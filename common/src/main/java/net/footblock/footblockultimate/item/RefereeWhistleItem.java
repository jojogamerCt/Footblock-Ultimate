package net.footblock.footblockultimate.item;

import net.footblock.footblockultimate.entity.FootballEntity;
import net.footblock.footblockultimate.match.WorldCupMatchManager;
import net.footblock.footblockultimate.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class RefereeWhistleItem extends Item {
    private static final double RANGE = 48.0;

    public RefereeWhistleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level instanceof ServerLevel serverLevel) {
            AABB area = player.getBoundingBox().inflate(RANGE);
            List<FootballEntity> footballs = serverLevel.getEntitiesOfClass(
                    FootballEntity.class,
                    area,
                    football -> football.distanceToSqr(player) <= RANGE * RANGE
            );
            for (FootballEntity football : footballs) {
                football.stopForWhistle();
            }

            if (player.isCrouching() && canResetMatch(player)) {
                WorldCupMatchManager.resetMatch(serverLevel, player, footballs.size());
            } else {
                WorldCupMatchManager.stopPlay(serverLevel, player, footballs.size());
                if (player.isCrouching()) {
                    WorldCupMatchManager.denyReset(player);
                }
            }

            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.REFEREE_WHISTLE.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
            player.getCooldowns().addCooldown(this, 40);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static boolean canResetMatch(Player player) {
        return player.hasPermissions(2)
                || player.getTags().contains("footblock_referee")
                || (player.getTeam() != null && player.getTeam().getName().equalsIgnoreCase("referee"));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.footblockultimate.referee_whistle.tooltip.use")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.footblockultimate.referee_whistle.tooltip.reset")
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
