package mod.crackt;

import mod.crackt.block.CrackingClusterBlock;
import mod.crackt.block.CrackingClusterBlockEntity;
import mod.crackt.CracktBlocks;
import mod.crackt.CracktNetworking;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ore vein helper that lets a single ore "core" track cracking progress,
 * then collapses the connected vein when enough swings are applied.
 */
public final class OreCracker {
	private static final int MAX_ORES = 128;
	private static final BlockPos[] NEIGHBOR_OFFSETS = buildNeighborOffsets();
	private static final Map<SessionKey, Session> SESSIONS = new HashMap<>();
	private static final ThreadLocal<Boolean> PROCESSING = ThreadLocal.withInitial(() -> false);
	private static final TagKey<Block> COMMON_ORES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));

	private OreCracker() {}

	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register(OreCracker::beforeBreak);
		PlayerBlockBreakEvents.AFTER.register(OreCracker::afterBreak);
	}

	private static boolean beforeBreak(Level level, Player player, BlockPos pos, BlockState state, /* nullable */ Object blockEntity) {
		if (level.isClientSide()) return true;
		if (PROCESSING.get()) return true;
		if (player.isShiftKeyDown()) return true; // allow vanilla while crouching

		boolean isOre = isOreBlock(state);
		boolean isCluster = state.is(CracktBlocks.CRACKING_CLUSTER);

		if (!isOre && !isCluster) {
			Session existing = findSession(level, pos);
			if (existing != null) {
				SESSIONS.remove(existing.key());
			}
			return true;
		}
		ItemStack held = player.getMainHandItem();
		if (!held.is(ItemTags.PICKAXES)) {
			return true;
		}

		Session session = findSession(level, pos);
		if (session == null) {
			session = buildSession(level, pos, state, isCluster);
			if (session == null) {
				return true;
			}
			SESSIONS.put(session.key(), session);
		}

		applyDurabilityLoss(player, held, 1); // pay for the swing even if we don't finish
		if (held.isEmpty()) {
			return true;
		}

		session.recordAttempt();
		updateClusterVisual(level, session);
		if (!session.isComplete()) {
			return false; // cancel vanilla break; keep cracking with repeat swings
		}

		BlockPos basePos = session.key().base();
		BlockState baseOriginal = session.getOriginal(basePos);
		if (baseOriginal != null) {
			Block.dropResources(baseOriginal, level, basePos, level.getBlockEntity(basePos), player, player.getMainHandItem());
		}
		level.setBlock(basePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);

		PROCESSING.set(true);
		try {
			crackRemainingWithDurability(level, player, session, basePos, held);
		} finally {
			PROCESSING.set(false);
		}
		SESSIONS.remove(session.key());
		return false; // we've handled drops and breaking
	}

	private static void afterBreak(Level level, Player player, BlockPos pos, BlockState state, /* nullable */ Object blockEntity) {
		// No-op; handled entirely in beforeBreak.
	}

	private static Session buildSession(Level level, BlockPos origin, BlockState state, boolean fromClusterBlock) {
		BlockState originState = state;
		if (fromClusterBlock) {
			if (level.getBlockEntity(origin) instanceof CrackingClusterBlockEntity cluster) {
				originState = cluster.getDisplayOrDefault();
			} else {
				return null;
			}
		}

		if (!isOreBlock(originState)) {
			return null;
		}

		Map<BlockPos, BlockState> originals = scanOres(level, origin, originState.getBlock());
		if (originals.isEmpty()) {
			return null;
		}

		// Anchor progress to the block the player started breaking so visuals appear where expected.
		BlockPos base = origin;
		int requiredHits = computeRequiredCracks(originals.size());
		int clusterStages = computeClusterStages(originals.size());
		SessionKey key = new SessionKey(level.dimension(), base.immutable());
		Vec3 offset = computeOffset(originals.keySet(), base);
		// Make sure the base uses the original ore for visuals/drops
		if (!originals.containsKey(base)) {
			originals.put(base, originState);
		}
		return new Session(key, originals, requiredHits, clusterStages, offset);
	}

	private static Map<BlockPos, BlockState> scanOres(Level level, BlockPos origin, Block targetBlock) {
		Map<BlockPos, BlockState> originals = new HashMap<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		queue.add(origin);

		while (!queue.isEmpty() && originals.size() < MAX_ORES) {
			BlockPos current = queue.removeFirst();
			if (originals.containsKey(current)) continue;

			BlockState state = level.getBlockState(current);
			if (state.getBlock() != targetBlock) continue;
			originals.put(current, state);

			for (BlockPos offset : NEIGHBOR_OFFSETS) {
				BlockPos next = current.offset(offset);
				if (!originals.containsKey(next) && level.getBlockState(next).getBlock() == targetBlock) {
					queue.add(next);
				}
			}
		}

		return originals;
	}

	private static int crackRemainingWithDurability(Level level, Player player, Session session, BlockPos alreadyBroken, ItemStack tool) {
		int cracked = 0;

		for (Map.Entry<BlockPos, BlockState> entry : session.originals.entrySet()) {
			BlockPos pos = entry.getKey();
			if (pos.equals(alreadyBroken)) continue;

			if (!player.isCreative()) {
				if (tool.isEmpty()) break;
				applyDurabilityLoss(player, tool, 1);
				if (tool.isEmpty()) break;
			}

			BlockState original = entry.getValue();
			Block.dropResources(original, level, pos, level.getBlockEntity(pos), player, player.getMainHandItem());
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			cracked++;
		}

		return cracked;
	}

	private static void applyDurabilityLoss(Player player, ItemStack tool, int amount) {
		if (amount <= 0) return;
		if (player.isCreative()) return;
		if (tool.isEmpty()) return;
		tool.hurtAndBreak(amount, player, EquipmentSlot.MAINHAND);
	}

	private static Session findSession(Level level, BlockPos pos) {
		for (Session session : SESSIONS.values()) {
			if (!session.key().dimension().equals(level.dimension())) continue;
			if (session.contains(pos)) return session;
		}
		return null;
	}

	private static void updateClusterVisual(Level level, Session session) {
		if (level.isClientSide()) return;
		BlockPos base = session.key().base();
		BlockState original = session.getOriginal(base);
		if (original == null) {
			original = session.anyOriginal();
		}
		if (original == null) return;

		int stage = computeClusterStage(session);
		BlockState clusterState = CracktBlocks.CRACKING_CLUSTER.defaultBlockState()
			.setValue(CrackingClusterBlock.STAGES, session.clusterStages())
			.setValue(CrackingClusterBlock.STAGE, stage);

		BlockState previous = level.getBlockState(base);
		level.setBlock(base, clusterState, Block.UPDATE_CLIENTS);
		if (level.getBlockEntity(base) instanceof CrackingClusterBlockEntity cluster) {
			cluster.setDisplayState(original);
			cluster.setDisplayOffset(session.offset());
			cluster.setChanged();
			// sync to clients
			if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
				CracktNetworking.syncCrackingCluster(serverLevel, base, original, session.offset());
				level.sendBlockUpdated(base, previous, clusterState, Block.UPDATE_CLIENTS);
				level.blockEntityChanged(base);
			}
		}
	}

	public static int computeRequiredCracks(int oreCount) {
		double value = 0.2 * oreCount + 1.25 * Math.log1p(oreCount);
		int hits = (int) Math.ceil(value);
		return Mth.clamp(hits, 1, Math.min(oreCount, 24));
	}

	public static int computeClusterStages(int oreCount) {
		double value = 0.15 * oreCount + 1.2 * Math.log1p(oreCount);
		int stages = (int) Math.ceil(value);
		return Mth.clamp(stages, 2, CrackingClusterBlock.MAX_STAGE);
	}

	private static int computeClusterStage(Session session) {
		if (session.requiredHits == 0) return 0;
		double ratio = (double) session.hits() / (double) session.requiredHits;
		int stage = (int) Math.ceil(ratio * session.clusterStages());
		return Mth.clamp(stage, 1, session.clusterStages());
	}

	private static Vec3 computeOffset(java.util.Set<BlockPos> positions, BlockPos base) {
		if (positions.isEmpty()) return Vec3.ZERO;
		double cx = 0, cy = 0, cz = 0;
		int count = 0;
		for (BlockPos p : positions) {
			if (p.equals(base)) continue;
			cx += p.getX() + 0.5;
			cy += p.getY() + 0.5;
			cz += p.getZ() + 0.5;
			count++;
		}
		if (count == 0) return Vec3.ZERO;
		cx /= count; cy /= count; cz /= count;
		Vec3 center = new Vec3(cx, cy, cz);
		Vec3 baseCenter = new Vec3(base.getX() + 0.5, base.getY() + 0.5, base.getZ() + 0.5);
		Vec3 delta = center.subtract(baseCenter);
		double max = 0.35; // keep inside block bounds
		double len = delta.length();
		if (len < 1e-4) return Vec3.ZERO;
		if (len > max) {
			delta = delta.scale(max / len);
		}
		return delta;
	}

	private static boolean isOreBlock(BlockState state) {
		return state.is(COMMON_ORES)
			|| state.is(BlockTags.COAL_ORES)
			|| state.is(BlockTags.IRON_ORES)
			|| state.is(BlockTags.COPPER_ORES)
			|| state.is(BlockTags.GOLD_ORES)
			|| state.is(BlockTags.REDSTONE_ORES)
			|| state.is(BlockTags.DIAMOND_ORES)
			|| state.is(BlockTags.EMERALD_ORES)
			|| state.is(BlockTags.LAPIS_ORES);
	}

	private static BlockPos[] buildNeighborOffsets() {
		// Use 6-direction adjacency to stay within the vein and avoid diagonally-bridged caves.
		List<BlockPos> offsets = new ArrayList<>(6);
		offsets.add(new BlockPos(1, 0, 0));
		offsets.add(new BlockPos(-1, 0, 0));
		offsets.add(new BlockPos(0, 1, 0));
		offsets.add(new BlockPos(0, -1, 0));
		offsets.add(new BlockPos(0, 0, 1));
		offsets.add(new BlockPos(0, 0, -1));
		return offsets.toArray(BlockPos[]::new);
	}

	private record SessionKey(ResourceKey<Level> dimension, BlockPos base) {}

	private static final class Session {
		private final SessionKey key;
		private final Map<BlockPos, BlockState> originals;
		private final int requiredHits;
		private final int clusterStages;
		private final Vec3 offset;
		private int hits = 0;

		Session(SessionKey key, Map<BlockPos, BlockState> originals, int requiredHits, int clusterStages, Vec3 offset) {
			this.key = key;
			this.originals = originals;
			this.requiredHits = requiredHits;
			this.clusterStages = clusterStages;
			this.offset = offset;
		}

		SessionKey key() {
			return key;
		}

		boolean contains(BlockPos pos) {
			return pos.equals(key.base()) || originals.containsKey(pos);
		}

		void recordAttempt() {
			hits++;
		}

		boolean isComplete() {
			return hits >= requiredHits;
		}

		BlockState getOriginal(BlockPos pos) {
			return originals.get(pos);
		}

		BlockState anyOriginal() {
			return originals.values().stream().findFirst().orElse(null);
		}

		int hits() {
			return hits;
		}

		int clusterStages() {
			return clusterStages;
		}

		Vec3 offset() {
			return offset;
		}
	}
}
