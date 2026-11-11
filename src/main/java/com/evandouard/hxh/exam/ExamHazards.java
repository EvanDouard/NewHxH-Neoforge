package com.evandouard.hxh.exam;

import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.Quest;
import com.evandouard.hxh.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Dangers et accessoires des épreuves de l'Examen : tempête sur le bateau de Dolle, chute dans la mer ou la
 * rivière, courant ascendant du mont Split, candidats et plaques de l'île de Zevil. Le brouillard de Numele est
 * côté client ({@code ExamFogClient}). Tout est serveur ; seul l'orage est envoyé au joueur concerné.
 */
public final class ExamHazards {
    private ExamHazards() {}

    /** Étiquette des candidats de Zevil ; {@link #TARGET_TAG} pour celui qui porte la plaque de la cible. */
    public static final String CANDIDATE_TAG = "hxh_candidate", TARGET_TAG = "hxh_target";
    /** Joueurs qui voient l'orage (à qui il faut renvoyer le beau temps). */
    private static final Set<UUID> STORM = new HashSet<>();

    /** Lieu de l'Examen où se trouve le joueur (le plus proche centre), ou null. */
    public static ExamSite siteAt(BlockPos pos) {
        int i = Math.round(pos.getX() / (float) ExamWorld.SPACING);
        ExamSite[] all = ExamSite.values();
        if (i < 0 || i >= all.length || pos.getZ() < -ExamWorld.SPACING / 2) return null;
        return all[i];
    }

    /** Chaque tick, pour un joueur dans la dimension de l'Examen. Renvoie true si l'épreuve vient d'être ratée. */
    public static boolean tick(ServerPlayer player, Quest.Step step, QuestData d) {
        ExamSite site = siteAt(player.blockPosition());
        storm(player, "storm".equals(step.hazard()) && site == ExamSite.BOAT);
        if (site == null) return false;
        BlockPos c = ExamWorld.centerAt(player.blockPosition());
        int instance = ExamWorld.instanceAt(player.blockPosition());
        ServerLevel level = player.serverLevel();
        switch (site) {
            case BOAT -> {
                // Passé par-dessus bord : l'équipage repêche le candidat
                if (player.getY() < ExamWorld.FLOOR_Y - 1.5 && player.isInWater()) {
                    // Par-dessus bord pendant la tempête : le capitaine débarque le candidat, épreuve ratée
                    if ("storm".equals(step.hazard())) {
                        player.sendSystemMessage(Component.translatable("exam.hxh.overboard_fail").withStyle(ChatFormatting.RED));
                        storm(player, false);
                        com.evandouard.hxh.quest.QuestManager.failExam(player);
                        return true;
                    }
                    rescue(player, Vec3.atBottomCenterOf(c.offset(0, 0, 3)), "exam.hxh.overboard");
                }
                if ("storm".equals(step.hazard())) {
                    if (stormTick(player, level, c)) return true;
                }
            }
            case AIRSHIP -> {
                // Pause avant le jeu de Netero : on s'y soigne et on y mange
                if (step.id().equals("ball_intro") && player.tickCount % 40 == 0) {
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.REGENERATION, 60, 1, false, false, true));
                    player.getFoodData().eat(2, 0.6f);
                }
            }
            case ZABAN -> {
                if (step.id().equals("register") && zabanLift(player, level, c, instance)) return false;
            }
            case MOUNT_SPLIT -> mountSplit(player, level, c, instance);
            case ISLAND_HOTEL -> {
                if (player.tickCount % 200 == 0) restockCoins(level, c, instance);
                if (player.tickCount % 4 == 0) pinCoins(level, c);
                if ("wave".equals(step.hazard())) wave(player, level, c, d);
            }
            case BISKA -> {
                if (player.tickCount % 200 == 0) restockSalmon(level, c);
            }
            case ZEVIL -> {
                if (player.tickCount % 200 == 0 && step.type().equals("points")) restockCandidates(level, c, instance);
                if (player.getY() < ExamWorld.FLOOR_Y - 1.5 && player.isInWater()
                        && Math.abs(player.getZ() - c.getZ()) > 46) rescue(player, Vec3.atBottomCenterOf(c.offset(0, 0, 48)), "exam.hxh.overboard");
            }
            default -> {}
        }
        return false;
    }

    // ------------------------------------------------------------------ Tempête

    private static void storm(ServerPlayer player, boolean on) {
        boolean had = STORM.contains(player.getUUID());
        if (on == had) return;
        if (on) {
            STORM.add(player.getUUID());
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 1f));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 1f));
            player.sendSystemMessage(Component.translatable("exam.hxh.storm").withStyle(ChatFormatting.AQUA));
        } else {
            STORM.remove(player.getUUID());
            STORMS.remove(player.getUUID());
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 0f));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 0f));
        }
    }

    /** Salle de Zaban qui descend : profondeur atteinte par joueur (absent = pas encore partie). */
    private static final java.util.Map<UUID, Integer> LIFT = new java.util.HashMap<>();

    /**
     * Le bon plat commandé, la « salle » descend : le candidat s'assoit sur la plateforme (ou saute dans le puits), elle
     * s'enfonce d'un bloc toutes les 4 ticks, et en bas, il arrive au hall souterrain. @return true une fois arrivé
     */
    private static boolean zabanLift(ServerPlayer player, ServerLevel level, BlockPos c, int instance) {
        double dx = player.getX() - (c.getX() + 0.5), dz = player.getZ() - (c.getZ() + ExamPlaceholders.ZABAN_LIFT_Z + 0.5);
        boolean onLift = Math.abs(dx) <= 1.6 && Math.abs(dz) <= 1.6;
        Integer depth = LIFT.get(player.getUUID());
        if (depth == null) {
            if (!onLift) {
                if (player.tickCount % 60 == 0) player.displayClientMessage(
                        Component.translatable("exam.hxh.zaban_lift_hint").withStyle(ChatFormatting.YELLOW), true);
                return false;
            }
            LIFT.put(player.getUUID(), 0);
            player.sendSystemMessage(Component.translatable("exam.hxh.zaban_lift").withStyle(ChatFormatting.GOLD));
            return false;
        }
        boolean arrived = depth >= ExamPlaceholders.ZABAN_SHAFT - 1 || player.getY() < ExamWorld.FLOOR_Y - ExamPlaceholders.ZABAN_SHAFT + 1;
        if (!arrived && player.tickCount % 4 == 0) {
            LIFT.put(player.getUUID(), depth + 1);
            ExamPlaceholders.zabanLift(level, c, depth + 1);
        }
        if (!arrived) return false;
        LIFT.remove(player.getUUID());
        ExamPlaceholders.zabanLift(level, c, 0); // la salle remonte pour le suivant
        for (int y = 1; y < ExamPlaceholders.ZABAN_SHAFT; y++) // efface les planchers laissés en chemin
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                BlockPos p = c.offset(x, -1 - y, ExamPlaceholders.ZABAN_LIFT_Z + z);
                if (y < ExamPlaceholders.ZABAN_SHAFT) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
        com.evandouard.hxh.quest.QuestData d = com.evandouard.hxh.quest.QuestManager.data(player);
        d.markExamVisited(ExamSite.HALL.id(), d.examInstance(ExamSite.HALL.id()));
        ExamTrips.sendTo(player, ExamSite.HALL, d.examInstance(ExamSite.HALL.id()));
        return true;
    }

    /** Tempête par joueur : prochaine rafale, et grosse vague annoncée (tick d'impact, abri à rejoindre). */
    private static final class Storm {
        int t, nextGust = 40, waveAt = -1, nextWave = 200, shelter;
    }

    private static final java.util.Map<UUID, Storm> STORMS = new java.util.HashMap<>();
    /** Délai pour rejoindre l'abri après l'annonce de la grosse vague (ticks), rayon de l'abri. */
    static final int WAVE_WARNING = 120;
    static final double SHELTER_RADIUS = 2.2;

    /**
     * Pendant la tempête : rafales à intervalles irréguliers (le roulis), et de temps en temps la grosse vague —
     * « abritez-vous ! », un abri du pont s'allume, il faut y être avant qu'elle frappe, sinon elle balaie le candidat
     * par-dessus bord. Le pont se soulève en vague à chaque déferlante. @return true si l'épreuve est ratée
     */
    private static boolean stormTick(ServerPlayer player, ServerLevel level, BlockPos c) {
        Storm st = STORMS.computeIfAbsent(player.getUUID(), u -> new Storm());
        st.t++;
        var rnd = player.getRandom();
        if (st.t >= st.nextGust) {
            gust(player);
            com.evandouard.hxh.fx.NenFx.quake(level, player.position(), 3f);
            st.nextGust = st.t + 25 + rnd.nextInt(70); // secousses irrégulières
        }
        if (st.waveAt < 0 && st.t >= st.nextWave) {
            st.shelter = rnd.nextInt(ExamPlaceholders.BOAT_SHELTERS.length);
            st.waveAt = st.t + WAVE_WARNING;
            player.sendSystemMessage(Component.translatable("exam.hxh.big_wave").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        }
        if (st.waveAt >= 0) {
            int[] o = ExamPlaceholders.BOAT_SHELTERS[st.shelter];
            Vec3 zone = Vec3.atBottomCenterOf(c.offset(o[0], o[1], o[2]));
            if (st.t % 10 == 0) // l'abri en surbrillance
                com.evandouard.hxh.fx.NenFx.shockwaveAt(level, zone.add(0, 0.1, 0), (float) SHELTER_RADIUS, 0xFFFFD040);
            if (st.t % 20 == 0) player.displayClientMessage(Component.translatable("exam.hxh.big_wave_timer",
                    Math.max(0, (st.waveAt - st.t) / 20)).withStyle(ChatFormatting.YELLOW), true);
            if (st.t >= st.waveAt) {
                st.waveAt = -1;
                st.nextWave = st.t + 300 + rnd.nextInt(200);
                // la déferlante traverse le pont d'un bord à l'autre
                for (int k = 0; k <= 4; k++) {
                    final int row = k;
                    com.evandouard.hxh.hatsu.NenTasks.later(k * 2, () -> {
                        for (int z = -14; z <= 14; z += 5)
                            com.evandouard.hxh.fx.NenFx.quake(level, new Vec3(c.getX() - 4 + row * 2 + 0.5, c.getY(), c.getZ() + z + 0.5), 3.5f);
                    });
                }
                boolean safe = player.position().distanceTo(zone) <= SHELTER_RADIUS + 0.5;
                if (safe) {
                    player.sendSystemMessage(Component.translatable("exam.hxh.big_wave_safe").withStyle(ChatFormatting.GREEN));
                } else {
                    player.sendSystemMessage(Component.translatable("exam.hxh.big_wave_hit").withStyle(ChatFormatting.RED));
                    double side = rnd.nextBoolean() ? 1 : -1;
                    player.push(side * 1.6, 0.6, 0);
                    player.hurtMarked = true;
                }
            }
        }
        return false;
    }

    /** Une rafale : poussée latérale qu'on encaisse en se baissant (sneak) ; le roulis secoue le pont. */
    private static void gust(ServerPlayer player) {
        if (player.isShiftKeyDown()) return;
        double side = player.getRandom().nextBoolean() ? 1 : -1;
        player.push(side * 0.55, 0.18, (player.getRandom().nextDouble() - 0.5) * 0.3);
        player.hurtMarked = true;
    }

    private static void rescue(ServerPlayer player, Vec3 to, String key) {
        player.teleportTo(to.x, to.y, to.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.hurtMarked = true;
        player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    // ------------------------------------------------------------------ Mont Split

    /**
     * La faille : on saute, on s'accroche aux fils (toiles), on prend un œuf, et le courant d'air remonte celui qui en
     * tient un. Sans œuf, on finit dans la rivière du fond, et on remonte au bord.
     */
    private static void mountSplit(ServerPlayer player, ServerLevel level, BlockPos c, int instance) {
        boolean inGap = Math.abs(player.getX() - (c.getX() + 0.5)) <= ExamPlaceholders.RAVINE_HALF + 0.7;
        if (!inGap) return;
        double y = player.getY();
        boolean egg = player.getInventory().countItem(ModItems.SPIDER_EAGLE_EGG.get()) > 0;
        if (egg && y < ExamWorld.FLOOR_Y + 1.5) {
            // Le courant ascendant : il soulève, et pousse vers le bord ouest (là où attend Menchi)
            Vec3 v = player.getDeltaMovement();
            double toRim = y > ExamWorld.FLOOR_Y - 0.5 ? -0.35 : 0;
            player.setDeltaMovement(toRim != 0 ? toRim : v.x * 0.5, 0.75, v.z * 0.5);
            if (level.getBlockState(player.blockPosition()).is(Blocks.COBWEB)) player.teleportTo(player.getX(), y + 1.2, player.getZ());
            player.resetFallDistance();
            player.hurtMarked = true;
            // le courant d'air : traits de vent sous le candidat (pas de particules vanilla)
            if (player.tickCount % 10 == 0) com.evandouard.hxh.fx.NenFx.slashAt(level, player.position().add(0, -0.6, 0), 1.0f, 0xDDEEFF);
            return;
        }
        if (y < ExamWorld.FLOOR_Y - ExamPlaceholders.RAVINE_DEPTH + 4) {
            rescue(player, Vec3.atBottomCenterOf(c.offset(-6, 0, 0)), "exam.hxh.river");
        }
        if (player.tickCount % 200 == 0) restockEggs(level, c, instance);
    }

    private static void restockEggs(ServerLevel level, BlockPos c, int instance) {
        ExamWorld.Site site = ExamWorld.cached(ExamSite.MOUNT_SPLIT, instance);
        if (site == null) return;
        int eggs = level.getEntitiesOfClass(ItemEntity.class, new AABB(c).inflate(20, 40, 20),
                e -> e.getItem().is(ModItems.SPIDER_EAGLE_EGG.get())).size();
        if (eggs < 2) placeEggs(level, site);
    }

    private static void placeEggs(ServerLevel level, ExamWorld.Site site) {
        for (BlockPos p : site.prey()) {
            ItemEntity egg = new ItemEntity(level, p.getX() + 0.5, p.getY() + 0.1, p.getZ() + 0.5,
                    new ItemStack(ModItems.SPIDER_EAGLE_EGG.get()), 0, 0, 0);
            egg.setNoGravity(true);
            egg.setUnlimitedLifetime();
            level.addFreshEntity(egg);
        }
    }

    // ------------------------------------------------------------------ Île-hôtel

    private static void placeCoins(ServerLevel level, ExamWorld.Site site) {
        for (BlockPos p : site.prey()) {
            ItemEntity coin = new ItemEntity(level, p.getX() + 0.5, p.getY() + 0.1, p.getZ() + 0.5,
                    new ItemStack(ModItems.SUNKEN_COIN.get()), 0, 0, 0);
            coin.setNoGravity(true);
            coin.setUnlimitedLifetime();
            coin.getPersistentData().putLong(COIN_SPOT, p.asLong());
            level.addFreshEntity(coin);
        }
    }

    private static final String COIN_SPOT = "hxh_coin_spot";

    /** L'eau fait remonter les objets : les pièces sont tenues sur le pont de l'épave. */
    private static void pinCoins(ServerLevel level, BlockPos c) {
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(c).inflate(42, 16, 48),
                e -> e.getPersistentData().contains(COIN_SPOT))) {
            BlockPos p = BlockPos.of(e.getPersistentData().getLong(COIN_SPOT));
            e.setPos(p.getX() + 0.5, p.getY() + 0.05, p.getZ() + 0.5);
            e.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static void restockCoins(ServerLevel level, BlockPos c, int instance) {
        ExamWorld.Site site = ExamWorld.cached(ExamSite.ISLAND_HOTEL, instance);
        if (site == null) return;
        int coins = level.getEntitiesOfClass(ItemEntity.class, new AABB(c).inflate(30, 16, 40),
                e -> e.getItem().is(ModItems.SUNKEN_COIN.get())).size();
        if (coins < 2) placeCoins(level, site);
    }

    /**
     * La vague géante et les canons : toutes les 4 s une lame venue de la mer (sud) pousse vers l'intérieur, sauf qui
     * s'accroche (accroupi) ; toutes les 2,5 s un boulet tombe près du candidat (onde de choc, dégâts s'il est trop
     * près). Emporté à la mer : retour sur la plage et le compte repart de zéro.
     */
    private static void wave(ServerPlayer player, ServerLevel level, BlockPos c, QuestData d) {
        // au premier instant, tout le monde sur la plage face à la mer (l'hôtel ne protège de rien)
        if (d.progress() == 0 && !level.canSeeSky(player.blockPosition().above())) {
            BlockPos beach = c.offset(0, 0, 10);
            player.teleportTo(beach.getX() + 0.5, beach.getY(), beach.getZ() + 0.5);
            player.setYRot(0f);
            player.hurtMarked = true;
        }
        if (player.tickCount % 100 == 0) {
            // La lame arrive de la mer (sud) et balaie toute l'île jusqu'au nord : un front de blocs qui se soulèvent
            player.sendSystemMessage(Component.translatable("exam.hxh.wave").withStyle(ChatFormatting.AQUA));
            int south = c.getZ() + 22, north = c.getZ() - 18;
            int rows = (south - north) / 4;
            int hitRow = Mth.clamp((int) Math.round((south - player.getZ()) / 4.0), 0, rows);
            for (int k = 0; k <= rows; k++) {
                final int row = k;
                double z = south - k * 4 + 0.5;
                com.evandouard.hxh.hatsu.NenTasks.later(k * 3, () -> {
                    for (int x = -20; x <= 20; x += 6)
                        com.evandouard.hxh.fx.NenFx.quake(level, new Vec3(c.getX() + x + 0.5, c.getY(), z), 4.5f);
                    if (row == 0) com.evandouard.hxh.fx.NenFx.shockwaveAt(level, new Vec3(c.getX() + 0.5, c.getY() + 0.2, z), 8f, 0x66AAFF);
                });
            }
            // quand le front passe sur le candidat : il est emporté vers l'intérieur, sauf accroupi
            com.evandouard.hxh.hatsu.NenTasks.later(hitRow * 3, () -> {
                if (!player.isAlive() || player.isShiftKeyDown()) return;
                player.push((player.getRandom().nextDouble() - 0.5) * 0.4, 0.4, -1.2);
                player.hurtMarked = true;
            });
        }
        if (player.tickCount % 50 == 25) {
            double a = player.getRandom().nextDouble() * Math.PI * 2, dist = 1 + player.getRandom().nextDouble() * 4;
            Vec3 hit = player.position().add(Math.cos(a) * dist, 0, Math.sin(a) * dist);
            com.evandouard.hxh.fx.NenFx.impactAt(level, hit.add(0, 0.5, 0), 1.6f, 0xFFAA44);
            com.evandouard.hxh.fx.NenFx.quake(level, hit, 2.5f);
            if (hit.distanceTo(player.position()) < 2.5) player.hurt(level.damageSources().explosion(null, null), 4f);
        }
        if (player.isInWater() && player.getY() < ExamWorld.FLOOR_Y - 1.5) {
            rescue(player, Vec3.atBottomCenterOf(c.offset(0, 0, 4)), "exam.hxh.wave_swept");
            d.setProgress(0);
        }
    }

    // ------------------------------------------------------------------ Zevil

    /** Points de plaques : sa propre plaque et celle de sa cible valent 3, les autres 1. */
    public static int platePoints(ServerPlayer player) {
        var inv = player.getInventory();
        return Math.min(1, inv.countItem(ModItems.EXAM_BADGE.get())) * 3
                + Math.min(1, inv.countItem(ModItems.TARGET_PLATE.get())) * 3
                + inv.countItem(ModItems.EXAM_PLATE.get());
    }

    private static final int[] NUMBERS = {16, 80, 118, 191, 294, 362, 371, 384};

    /** Un candidat de Zevil (vindicateur) qui garde sa plaque en main gauche ; elle tombe à sa défaite. */
    static ExamCandidate candidate(ServerLevel level, BlockPos p, boolean target, int n) {
        ExamCandidate v = com.evandouard.hxh.registry.ModEntities.EXAM_CANDIDATE.get().create(level);
        if (v == null) return null;
        v.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0);
        v.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(target ? ModItems.TARGET_PLATE.get() : ModItems.EXAM_PLATE.get()));
        v.setDropChance(EquipmentSlot.OFFHAND, 2f);
        v.setCustomName(Component.translatable(target ? "exam.hxh.candidate_target" : "exam.hxh.candidate", NUMBERS[n % NUMBERS.length]));
        v.setPersistenceRequired();
        v.addTag(CANDIDATE_TAG);
        if (target) v.addTag(TARGET_TAG);
        level.addFreshEntity(v);
        return v;
    }

    /** Pour les tests : un candidat de Zevil. */
    public static ExamCandidate candidateForTests(ServerLevel level, BlockPos p, boolean target) {
        return candidate(level, p, target, 0);
    }

    private static void restockCandidates(ServerLevel level, BlockPos c, int instance) {
        ExamWorld.Site site = ExamWorld.cached(ExamSite.ZEVIL, instance);
        if (site == null) return;
        var around = level.getEntitiesOfClass(ExamCandidate.class, new AABB(c).inflate(56), v -> v.getTags().contains(CANDIDATE_TAG));
        int max = Math.max(4, Math.min(8, site.prey().size()));
        if (around.size() >= max / 2) return;
        boolean hasTarget = around.stream().anyMatch(v -> v.getTags().contains(TARGET_TAG));
        for (int i = 0; i < site.prey().size() && around.size() + i < max; i++)
            candidate(level, site.prey().get(i), !hasTarget && i == 0, i + level.getRandom().nextInt(4));
    }

    // ------------------------------------------------------------------ Biska (saumons de Menchi)

    private static void restockSalmon(ServerLevel level, BlockPos c) {
        BlockPos pond = c.offset(11, -2, -7);
        if (level.getEntitiesOfClass(Salmon.class, new AABB(pond).inflate(8)).size() >= 2) return;
        for (int i = 0; i < 3; i++) {
            Salmon s = EntityType.SALMON.create(level);
            if (s == null) continue;
            s.moveTo(pond.getX() + 0.5 + i - 1, pond.getY(), pond.getZ() + 0.5, 0, 0);
            s.setPersistenceRequired();
            level.addFreshEntity(s);
        }
    }

    /** Ce qui peuple un lieu à sa pose : œufs, candidats, poissons. */
    static void populate(ServerLevel level, ExamSite site, ExamWorld.Site built, BlockPos center) {
        switch (site) {
            case MOUNT_SPLIT -> placeEggs(level, built);
            case ISLAND_HOTEL -> placeCoins(level, built);
            case ZEVIL -> {
                for (int i = 0; i < built.prey().size(); i++) candidate(level, built.prey().get(i), i == 0, i);
            }
            case BISKA -> restockSalmon(level, center);
            default -> {}
        }
    }
}
