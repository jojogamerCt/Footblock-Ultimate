package net.footblock.footblockultimate.block;

import net.footblock.footblockultimate.match.WorldCupMatchManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public final class GoalLineBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final WorldCupMatchManager.Side scoringSide;

    public GoalLineBlock(WorldCupMatchManager.Side scoringSide, BlockBehaviour.Properties properties) {
        super(properties);
        this.scoringSide = scoringSide;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    public boolean isScoringCrossing(BlockState state, BlockPos pos, Vec3 previousPosition, Vec3 currentPosition) {
        Direction facing = state.getValue(FACING);
        Vec3 normal = new Vec3(facing.getStepX(), 0.0, facing.getStepZ());
        Vec3 center = Vec3.atCenterOf(pos);
        double previousSide = previousPosition.subtract(center).dot(normal);
        double currentSide = currentPosition.subtract(center).dot(normal);

        // The texture arrow points along FACING. Only crossings in that direction count.
        return previousSide <= 0.0 && currentSide > 0.0 && currentSide - previousSide > 1.0E-4;
    }

    public WorldCupMatchManager.Side getScoringSide() {
        return this.scoringSide;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "block.footblockultimate.goal_line.tooltip",
                Component.translatable("message.footblockultimate.side." + this.scoringSide.name().toLowerCase())
                        .withStyle(this.scoringSide.color())
        ).withStyle(ChatFormatting.GRAY));
    }
}
