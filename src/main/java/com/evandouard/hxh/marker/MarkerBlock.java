package com.evandouard.hxh.marker;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Repères réglables pour les structures :
 * <ul>
 *     <li>{@link Kind#NPC} : un PNJ (clic droit = PNJ suivant, accroupi = précédent) ;</li>
 *     <li>{@link Kind#WAYPOINT} : point de parcours numéroté (clic droit = +1, accroupi = −1),
 *         le plus grand numéro est l'arrivée de l'épreuve.</li>
 * </ul>
 * Une étiquette flottante au-dessus du bloc montre son réglage.
 */
public class MarkerBlock extends BaseEntityBlock {
    public enum Kind { NPC, WAYPOINT }

    /** PNJ proposés, dans l'ordre du clic droit. */
    public static final List<String> NPCS = List.of("navigator", "cook", "beans", "satotz", "buhara", "menchi", "netero",
            "captain", "quiz_granny", "fake_satotz", "lippo", "tonpa", "kiriko_wife", "husband_1", "husband_2", "hotel_manager", "passeur", "receptionist", "referee", "guichetier", "archeologue", "commissaire_priseur", "exorciste", "majordome_zoldyck", "hunter_gourmet", "cheadle", "lutteur");

    private final Kind kind;

    public MarkerBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(p -> new MarkerBlock(kind, p));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        MarkerBlockEntity be = new MarkerBlockEntity(pos, state);
        if (kind == Kind.NPC) be.setNpcSilently(NPCS.get(0));
        return be;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof MarkerBlockEntity be)) return InteractionResult.SUCCESS;
        int step = player.isShiftKeyDown() ? -1 : 1;
        if (kind == Kind.NPC) {
            // PNJ de base, puis ceux déclarés dans les quêtes ("npcs")
            List<String> all = new java.util.ArrayList<>(NPCS);
            for (String id : com.evandouard.hxh.quest.QuestRegistry.npcIds()) if (!all.contains(id)) all.add(id);
            int i = Math.max(0, all.indexOf(be.npc()));
            String next = all.get(Math.floorMod(i + step, all.size()));
            be.setNpc(next);
            player.displayClientMessage(Component.translatable("message.hxh.marker.npc",
                    Component.translatable("npc.hxh." + next)).withStyle(ChatFormatting.AQUA), true);
        } else {
            be.setIndex(be.index() + step);
            player.displayClientMessage(Component.translatable("message.hxh.marker.waypoint", be.index())
                    .withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(kind == Kind.NPC ? "tooltip.hxh.marker.npc" : "tooltip.hxh.marker.waypoint")
                .withStyle(ChatFormatting.GRAY));
    }
}
