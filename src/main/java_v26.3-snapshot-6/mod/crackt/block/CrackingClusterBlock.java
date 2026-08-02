package mod.crackt.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary ore "core" that shrinks as the vein cracking progresses.
 * Rendering is handled by a BER so we can reuse the original ore's model.
 */
public class CrackingClusterBlock extends Block implements EntityBlock {
	public static final int MAX_STAGE = 16;
	public static final float MIN_SCALE = 0.5f; // stop shrinking at 50% volume
	public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, MAX_STAGE);
	public static final IntegerProperty STAGES = IntegerProperty.create("stages", 1, MAX_STAGE);

	private static final Map<Integer, VoxelShape[]> SHAPES = new ConcurrentHashMap<>();
	private static final int DEFAULT_STAGES = 4;

	public CrackingClusterBlock(BlockBehaviour.Properties settings) {
		super(settings);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(STAGE, 0)
			.setValue(STAGES, DEFAULT_STAGES));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(STAGE, STAGES);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		// Let the BER handle visuals (including breaking overlay).
		return RenderShape.INVISIBLE;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapeWithOffset(state, level, pos);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapeWithOffset(state, level, pos);
	}

	@Override
	public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapeWithOffset(state, level, pos);
	}

	@Override
	public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		// Match iron ore hardness so vanilla break speed expectations stay intact.
		return Blocks.IRON_ORE.defaultBlockState().getDestroyProgress(player, level, pos);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CrackingClusterBlockEntity(pos, state);
	}

	public static float scaleFor(int stage, int totalStages) {
		int clampedStages = Mth.clamp(totalStages, 1, MAX_STAGE);
		int clampedStage = Mth.clamp(stage, 0, clampedStages);
		float t = (float) clampedStage / (float) clampedStages;
		return Mth.lerp(t, 1.0f, MIN_SCALE);
	}

	private static VoxelShape shapeFor(BlockState state) {
		int stages = state.getValue(STAGES);
		int stage = Math.min(state.getValue(STAGE), stages);
		VoxelShape[] cached = SHAPES.computeIfAbsent(stages, CrackingClusterBlock::buildShapes);
		return cached[Math.min(stage, cached.length - 1)];
	}

	private static VoxelShape shapeWithOffset(BlockState state, BlockGetter level, BlockPos pos) {
		VoxelShape base = shapeFor(state);
		if (level.getBlockEntity(pos) instanceof CrackingClusterBlockEntity cluster) {
			Vec3 off = cluster.getDisplayOffset();
			if (!off.equals(Vec3.ZERO)) {
				return base.move(off.x, off.y, off.z);
			}
		}
		return base;
	}

	private static VoxelShape[] buildShapes(int stages) {
		VoxelShape[] shapes = new VoxelShape[stages + 1];
		for (int i = 0; i <= stages; i++) {
			double scale = scaleFor(i, stages);
			double inset = (16.0 - 16.0 * scale) / 2.0;
			shapes[i] = Block.box(inset, inset, inset, 16.0 - inset, 16.0 - inset, 16.0 - inset);
		}
		return shapes;
	}
}
