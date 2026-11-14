package com.evandouard.hxh.registry;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.block.ArenaBlock;
import com.evandouard.hxh.block.WaterDivinationBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HxHMod.MODID);

    public static final DeferredBlock<com.evandouard.hxh.block.HunterComputerBlock> HUNTER_COMPUTER = BLOCKS.register("hunter_computer",
            () -> new com.evandouard.hxh.block.HunterComputerBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f).sound(SoundType.METAL).noOcclusion().lightLevel(s -> 6)));
    public static final DeferredBlock<WaterDivinationBlock> WATER_DIVINATION = BLOCKS.register("water_divination",
            () -> new WaterDivinationBlock(BlockBehaviour.Properties.of()
                    .strength(0.3f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    /** Portail de la Tour Céleste, à poser dans le monde (en attendant la vraie tour). */
    public static final DeferredBlock<ArenaBlock> HEAVENS_ARENA_GATE = BLOCKS.register("heavens_arena_gate",
            () -> new ArenaBlock(ArenaBlock.Kind.GATE, BlockBehaviour.Properties.of()
                    .strength(3f).sound(SoundType.METAL).lightLevel(s -> 10)));
    public static final DeferredBlock<ArenaBlock> ARENA_ELEVATOR = BLOCKS.register("arena_elevator",
            () -> new ArenaBlock(ArenaBlock.Kind.ELEVATOR, BlockBehaviour.Properties.of()
                    .strength(-1f, 3600000f).sound(SoundType.METAL).noLootTable()));
    public static final DeferredBlock<ArenaBlock> ARENA_EXIT = BLOCKS.register("arena_exit",
            () -> new ArenaBlock(ArenaBlock.Kind.EXIT, BlockBehaviour.Properties.of()
                    .strength(-1f, 3600000f).sound(SoundType.WOOD).noLootTable()));

    // Repères pour les étages construits à la main (remplacés par de l'air quand la structure est posée)
    public static final DeferredBlock<Block> MARKER_PLAYER = marker("arena_marker_player");
    public static final DeferredBlock<Block> MARKER_OPPONENT = marker("arena_marker_opponent");
    /** Ancien repère « PNJ » (compris comme l'arbitre) ; remplacé par les deux suivants. */
    public static final DeferredBlock<Block> MARKER_NPC = marker("arena_marker_npc");
    public static final DeferredBlock<Block> MARKER_RECEPTIONIST = marker("arena_marker_receptionist");
    public static final DeferredBlock<Block> MARKER_REFEREE = marker("arena_marker_referee");
    public static final DeferredBlock<Block> MARKER_ARRIVAL = marker("arena_marker_arrival");

    /** Repère PNJ réglable (clic droit pour choisir le PNJ). */
    public static final DeferredBlock<com.evandouard.hxh.marker.MarkerBlock> MARKER_NPC_CHOICE = BLOCKS.register("marker_npc",
            () -> new com.evandouard.hxh.marker.MarkerBlock(com.evandouard.hxh.marker.MarkerBlock.Kind.NPC,
                    BlockBehaviour.Properties.of().strength(0.2f).sound(SoundType.WOOL).noLootTable()));
    /** Point de parcours numéroté (clic droit pour changer le numéro ; le plus grand = arrivée). */
    public static final DeferredBlock<com.evandouard.hxh.marker.MarkerBlock> MARKER_WAYPOINT = BLOCKS.register("marker_waypoint",
            () -> new com.evandouard.hxh.marker.MarkerBlock(com.evandouard.hxh.marker.MarkerBlock.Kind.WAYPOINT,
                    BlockBehaviour.Properties.of().strength(0.2f).sound(SoundType.WOOL).noLootTable()));
    /** Pierres de méditation I à V (sanctuaires de Volonté). */
    public static final java.util.List<DeferredBlock<com.evandouard.hxh.meditation.MeditationStoneBlock>> MEDITATION_STONES =
            java.util.stream.IntStream.rangeClosed(1, 5)
                    .mapToObj(t -> BLOCKS.register("meditation_stone_" + t,
                            () -> new com.evandouard.hxh.meditation.MeditationStoneBlock(t, BlockBehaviour.Properties.of()
                                    .strength(-1f, 3600000f).sound(SoundType.STONE).noOcclusion().lightLevel(s -> 3 + t))))
                    .toList();

    /** Faux mur de la Tour de Trick : clic droit = il s'efface quelques secondes. */
    public static final DeferredBlock<com.evandouard.hxh.block.IllusionWallBlock> ILLUSION_WALL = BLOCKS.register("illusion_wall",
            () -> new com.evandouard.hxh.block.IllusionWallBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f, 6f).sound(SoundType.STONE).noOcclusion()));

    /** Point d'apparition des proies d'une épreuve de chasse. */
    public static final DeferredBlock<Block> MARKER_PREY = marker("marker_prey");

    private static DeferredBlock<Block> marker(String name) {
        return BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.of()
                .strength(0.2f).sound(SoundType.WOOL).noLootTable()));
    }

    /** Tapis de course I à IV. */
    public static final java.util.List<DeferredBlock<com.evandouard.hxh.training.TreadmillBlock>> TREADMILLS =
            java.util.Arrays.stream(com.evandouard.hxh.training.TrainingTier.values())
                    .map(t -> BLOCKS.register("treadmill" + t.suffix(), () -> new com.evandouard.hxh.training.TreadmillBlock(t,
                            BlockBehaviour.Properties.of().strength(2f).sound(SoundType.METAL).noOcclusion())))
                    .toList();

    /** Mannequin cogneur : frappe ce qui se tient devant lui (Endurance, ou Vitesse en esquivant). */
    public static final DeferredBlock<com.evandouard.hxh.training.StrikingDummyBlock> STRIKING_DUMMY = BLOCKS.register("striking_dummy",
            () -> new com.evandouard.hxh.training.StrikingDummyBlock(BlockBehaviour.Properties.of()
                    .strength(2f).sound(SoundType.WOOD).noOcclusion()));

    /** Bain glacé : y rester entraîne la Volonté. */
    public static final DeferredBlock<com.evandouard.hxh.training.IceBathBlock> ICE_BATH = BLOCKS.register("ice_bath",
            () -> new com.evandouard.hxh.training.IceBathBlock(BlockBehaviour.Properties.of()
                    .strength(2f).sound(SoundType.METAL).noOcclusion()));

    private ModBlocks() {}
}
