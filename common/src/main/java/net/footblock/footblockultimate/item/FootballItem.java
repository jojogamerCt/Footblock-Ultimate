package net.footblock.footblockultimate.item;

import net.footblock.footblockultimate.entity.FootballEntity;
import net.footblock.footblockultimate.entity.FootballVariant;
import net.footblock.footblockultimate.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class FootballItem extends Item {
    private final FootballVariant variant;

    public FootballItem(FootballVariant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos spawnPos = clickedPos.relative(face);

        FootballEntity football = ModEntities.FOOTBALL.get().create(level);
        if (football != null) {
            double x = spawnPos.getX() + 0.5;
            double y = spawnPos.getY() + 0.1;
            double z = spawnPos.getZ() + 0.5;

            football.moveTo(x, y, z, 0.0f, 0.0f);
            football.setVariant(this.variant);
            level.addFreshEntity(football);

            level.playSound(null, x, y, z, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);

            Player player = context.getPlayer();
            if (player != null && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (this.variant == FootballVariant.WORLD_CUP_2026) {
            tooltip.add(Component.translatable("item.footblockultimate.world_cup_2026_ball.tooltip")
                    .withStyle(ChatFormatting.AQUA));
        }
    }
}
