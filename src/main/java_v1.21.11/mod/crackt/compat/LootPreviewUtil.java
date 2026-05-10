package mod.crackt.compat;

import mod.crackt.CracktBlocks;
import mod.crackt.OreCracker;
import mod.crackt.block.CrackingClusterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LootPreviewUtil {
	private static final String PREVIEW_KEY = "crackt_loot_preview";
	private static final String DISPLAY_NAME_KEY = "display_name";
	private static final String HITS_KEY = "hits";
	private static final String REQUIRED_HITS_KEY = "required_hits";
	private static final String ITEM_KEY = "item";
	private static final String COUNT_KEY = "count";

	private LootPreviewUtil() {}

	public static boolean shouldShow(BlockState state) {
		return OreCracker.isPreviewTarget(state);
	}

	public static void writePreview(CompoundTag data, ServerLevel level, BlockPos pos, BlockState state, Player player) {
		OreCracker.PreviewData previewData = OreCracker.getPreviewData(level, pos, state);
		if (previewData.isEmpty()) {
			return;
		}

		data.putString(DISPLAY_NAME_KEY, previewData.displayState().getBlock().getDescriptionId());
		data.putInt(HITS_KEY, previewData.hits());
		data.putInt(REQUIRED_HITS_KEY, previewData.requiredHits());

		List<ItemStack> preview = collectPreview(level, previewData, player);

		ListTag entries = new ListTag();
		for (ItemStack stack : preview) {
			if (stack.isEmpty()) {
				continue;
			}
			CompoundTag entry = new CompoundTag();
			entry.putString(ITEM_KEY, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
			entry.putInt(COUNT_KEY, stack.getCount());
			entries.add(entry);
		}
		if (!entries.isEmpty()) {
			data.put(PREVIEW_KEY, entries);
		}
	}

	public static TooltipData readTooltipData(CompoundTag data) {
		ListTag entries = data.getListOrEmpty(PREVIEW_KEY);
		List<ItemStack> preview = new ArrayList<>(entries.size());
		for (Tag tag : entries) {
			if (!(tag instanceof CompoundTag entry)) {
				continue;
			}
			String rawId = entry.getString(ITEM_KEY).orElse(null);
			Identifier id = rawId == null ? null : Identifier.tryParse(rawId);
			if (id == null) {
				continue;
			}
			Item item = BuiltInRegistries.ITEM.getValue(id);
			int count = entry.getIntOr(COUNT_KEY, 0);
			if (item == Items.AIR || count <= 0) {
				continue;
			}
			preview.add(new ItemStack(item, count));
		}
		String displayNameKey = data.getStringOr(DISPLAY_NAME_KEY, Blocks.IRON_ORE.getDescriptionId());
		int hits = data.getIntOr(HITS_KEY, 0);
		int requiredHits = data.getIntOr(REQUIRED_HITS_KEY, 0);
		if (preview.isEmpty() && requiredHits <= 0) {
			return TooltipData.EMPTY;
		}
		return new TooltipData(preview, displayNameKey, hits, requiredHits);
	}

	public static TooltipData buildClientFallback(Level level, BlockPos pos, BlockState state) {
		Map<BlockPos, BlockState> originals = scanConnectedPreview(level, pos, state);
		if (originals.isEmpty()) {
			return TooltipData.EMPTY;
		}

		Block targetBlock = resolveTargetBlock(level, pos, state);
		CrackingClusterBlockEntity cluster = findConnectedCluster(level, pos, targetBlock);
		Map<Item, Integer> totals = new HashMap<>();
		for (BlockState original : originals.values()) {
			Item item = original.getBlock().asItem();
			if (item == Items.AIR) {
				continue;
			}
			totals.merge(item, 1, Integer::sum);
		}

		List<ItemStack> preview = totals.entrySet().stream()
			.sorted(Comparator.comparing(entry -> BuiltInRegistries.ITEM.getKey(entry.getKey()).toString()))
			.map(entry -> new ItemStack(entry.getKey(), entry.getValue()))
			.toList();

		int hits = cluster != null ? cluster.getHits() : 0;
		int requiredHits = cluster != null && cluster.getRequiredHits() > 0 ? cluster.getRequiredHits() : OreCracker.computeRequiredCracks(originals.size());
		String displayName = cluster != null ? cluster.getDisplayOrDefault().getBlock().getDescriptionId() : targetBlock.getDescriptionId();
		return new TooltipData(preview, displayName, hits, requiredHits);
	}

	private static Map<BlockPos, BlockState> scanConnectedPreview(Level level, BlockPos origin, BlockState state) {
		Block targetBlock = resolveTargetBlock(level, origin, state);
		if (!OreCracker.isPreviewTarget(state)) {
			return Map.of();
		}

		Map<BlockPos, BlockState> originals = new HashMap<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		queue.add(origin);
		while (!queue.isEmpty()) {
			BlockPos current = queue.removeFirst();
			if (originals.containsKey(current)) {
				continue;
			}

			BlockState currentState = level.getBlockState(current);
			BlockState original = originalStateForPreview(level, currentState, current);
			if (original == null || original.getBlock() != targetBlock) {
				continue;
			}
			originals.put(current.immutable(), original);

			for (BlockPos offset : neighborOffsets()) {
				BlockPos next = current.offset(offset);
				if (!originals.containsKey(next)) {
					queue.add(next);
				}
			}
		}
		return originals;
	}

	private static Block resolveTargetBlock(Level level, BlockPos pos, BlockState state) {
		if (state.is(CracktBlocks.CRACKING_CLUSTER) && level.getBlockEntity(pos) instanceof CrackingClusterBlockEntity cluster) {
			return cluster.getDisplayOrDefault().getBlock();
		}
		return state.getBlock();
	}

	private static CrackingClusterBlockEntity findConnectedCluster(Level level, BlockPos origin, Block targetBlock) {
		for (Map.Entry<BlockPos, BlockState> entry : scanConnectedPreview(level, origin, level.getBlockState(origin)).entrySet()) {
			if (level.getBlockEntity(entry.getKey()) instanceof CrackingClusterBlockEntity cluster && cluster.getDisplayOrDefault().getBlock() == targetBlock) {
				return cluster;
			}
		}
		return null;
	}

	private static BlockState originalStateForPreview(Level level, BlockState state, BlockPos pos) {
		if (state.is(CracktBlocks.CRACKING_CLUSTER)) {
			if (level.getBlockEntity(pos) instanceof CrackingClusterBlockEntity cluster) {
				return cluster.getDisplayOrDefault();
			}
			return null;
		}
		return OreCracker.isPreviewTarget(state) ? state : null;
	}

	private static BlockPos[] neighborOffsets() {
		return new BlockPos[] {
			new BlockPos(1, 0, 0),
			new BlockPos(-1, 0, 0),
			new BlockPos(0, 1, 0),
			new BlockPos(0, -1, 0),
			new BlockPos(0, 0, 1),
			new BlockPos(0, 0, -1)
		};
	}

	private static List<ItemStack> collectPreview(ServerLevel level, OreCracker.PreviewData previewData, Player player) {
		Map<Item, Integer> totals = new HashMap<>();
		ItemStack tool = player.getMainHandItem();
		for (Map.Entry<BlockPos, BlockState> entry : previewData.originals().entrySet()) {
			for (ItemStack drop : Block.getDrops(entry.getValue(), level, entry.getKey(), getLootBlockEntity(level, entry.getKey(), entry.getValue()), player, tool)) {
				if (drop.isEmpty()) {
					continue;
				}
				totals.merge(drop.getItem(), drop.getCount(), Integer::sum);
			}
		}

		return totals.entrySet().stream()
			.sorted(Comparator.comparing(entry -> BuiltInRegistries.ITEM.getKey(entry.getKey()).toString()))
			.map(entry -> new ItemStack(entry.getKey(), entry.getValue()))
			.toList();
	}

	private static BlockEntity getLootBlockEntity(ServerLevel level, BlockPos pos, BlockState originalState) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof CrackingClusterBlockEntity && !originalState.is(CracktBlocks.CRACKING_CLUSTER)) {
			return null;
		}
		return blockEntity;
	}

	public record TooltipData(List<ItemStack> preview, String displayNameKey, int hits, int requiredHits) {
		private static final TooltipData EMPTY = new TooltipData(List.of(), Blocks.AIR.getDescriptionId(), 0, 0);

		public boolean isEmpty() {
			return preview.isEmpty() && requiredHits <= 0;
		}
	}
}
