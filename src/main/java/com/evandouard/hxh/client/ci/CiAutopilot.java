package com.evandouard.hxh.client.ci;

import com.evandouard.hxh.HxHMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Pilote automatique utilisé uniquement en CI (propriété système {@code hxh.ci.scenario}).
 * <p>
 * Crée un monde plat, joue le scénario ligne par ligne puis quitte le jeu.
 * Instructions du scénario :
 * <ul>
 *     <li>{@code wait <ticks>} : attend</li>
 *     <li>{@code cmd <commande sans />} : exécute une commande en tant que joueur</li>
 *     <li>{@code look <yaw> <pitch>} : oriente la caméra</li>
 *     <li>{@code shot <nom>} : capture d'écran dans screenshots/&lt;nom&gt;.png</li>
 *     <li>{@code log <texte>} : écrit dans le rapport</li>
 *     <li>{@code quit} : quitte le jeu</li>
 * </ul>
 * Le déroulé est écrit dans {@code ci-report.txt} à la racine du dossier de jeu.
 */
public final class CiAutopilot {
    private static final long TIMEOUT_MS = 10 * 60 * 1000L;

    private final List<String> lines;
    private final Path report;
    private final long startMs = System.currentTimeMillis();
    private int index = 0;
    private int waitTicks = 0;
    private int menuTicks = 0;
    private boolean worldRequested = false;
    private boolean worldLoggedIn = false;
    private boolean quitting = false;

    private CiAutopilot(List<String> lines, Path report) {
        this.lines = lines;
        this.report = report;
    }

    public static void initIfRequested() {
        String scenario = System.getProperty("hxh.ci.scenario");
        if (scenario == null || scenario.isBlank()) return;
        try {
            List<String> lines = Files.readAllLines(Path.of(scenario), StandardCharsets.UTF_8);
            Path report = Minecraft.getInstance() != null
                    ? Minecraft.getInstance().gameDirectory.toPath().resolve("ci-report.txt")
                    : Path.of("ci-report.txt");
            CiAutopilot pilot = new CiAutopilot(lines, report);
            Files.writeString(report, "", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            pilot.log("Scénario chargé : " + lines.size() + " lignes");
            NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, pilot::onTick);
        } catch (IOException e) {
            HxHMod.LOGGER.error("[HxH CI] Impossible de lire le scénario {}", scenario, e);
        }
    }

    private void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (quitting) return;

        if (System.currentTimeMillis() - startMs > TIMEOUT_MS) {
            log("ECHEC : délai dépassé");
            quit(mc);
            return;
        }

        // Étape 1 : attendre la fin du chargement puis créer le monde.
        if (mc.level == null || mc.player == null) {
            if (!worldRequested && mc.getOverlay() == null && mc.screen != null && ++menuTicks > 40) {
                worldRequested = true;
                log("Création du monde plat");
                createWorld(mc);
            }
            return;
        }

        if (!worldLoggedIn) {
            worldLoggedIn = true;
            log("Monde chargé, joueur " + mc.player.getName().getString());
        }

        // Le jeu se met en pause si la fenêtre perd le focus : on referme le menu.
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        while (index < lines.size()) {
            String raw = lines.get(index++).trim();
            if (raw.isEmpty() || raw.startsWith("#")) continue;
            if (execute(mc, raw)) return; // l'instruction consomme ce tick
        }
        log("Fin du scénario");
        quit(mc);
    }

    /** @return true si l'instruction doit laisser passer au moins un tick. */
    private boolean execute(Minecraft mc, String line) {
        String[] parts = line.split("\\s+", 2);
        String op = parts[0];
        String arg = parts.length > 1 ? parts[1] : "";
        try {
            switch (op) {
                case "wait" -> {
                    waitTicks = Integer.parseInt(arg);
                    return true;
                }
                case "cmd" -> {
                    String command = arg.startsWith("/") ? arg.substring(1) : arg;
                    mc.player.connection.sendCommand(command);
                    log("cmd /" + command);
                    return true;
                }
                case "look" -> {
                    String[] angles = arg.split("\\s+");
                    mc.player.setYRot(Float.parseFloat(angles[0]));
                    mc.player.setXRot(Float.parseFloat(angles[1]));
                    return true;
                }
                case "shot" -> {
                    String name = arg.isBlank() ? "shot_" + index : arg;
                    Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(),
                            msg -> log("capture " + name + " : " + msg.getString()));
                    return true;
                }
                case "log" -> {
                    log(arg);
                    return false;
                }
                case "quit" -> {
                    log("Fin du scénario (quit)");
                    quit(mc);
                    return true;
                }
                default -> {
                    log("ECHEC : instruction inconnue '" + line + "'");
                    return false;
                }
            }
        } catch (RuntimeException e) {
            log("ECHEC : '" + line + "' -> " + e);
            return false;
        }
    }

    private void createWorld(Minecraft mc) {
        LevelSettings settings = new LevelSettings("ci_world", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                true, new GameRules(), WorldDataConfiguration.DEFAULT);
        WorldOptions options = new WorldOptions(20261102L, false, false);
        mc.createWorldOpenFlows().createFreshLevel("ci_world", settings, options,
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                        .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                mc.screen);
    }

    private void quit(Minecraft mc) {
        quitting = true;
        mc.stop();
    }

    private void log(String message) {
        HxHMod.LOGGER.info("[HxH CI] {}", message);
        try {
            Files.writeString(report, message + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}
