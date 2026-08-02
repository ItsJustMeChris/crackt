package mod.crackt.compat.jade;

import mod.crackt.Crackt;
import mod.crackt.OreCracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Ships the live swing count to the client. Only the server knows it: progress lives in
 * OreCracker's session map, not on the block entity.
 *
 * <p>Shares its UID with CrackingClusterProvider so Jade's config toggle covers both halves.
 */
public enum CrackingClusterData implements IServerDataProvider<BlockAccessor> {
	INSTANCE;

	static final String KEY_HITS = "CracktHits";
	static final String KEY_REQUIRED = "CracktRequired";

	private static final ResourceLocation UID = new ResourceLocation(Crackt.MOD_ID, "cracking_cluster");

	@Override
	public void appendServerData(CompoundTag data, BlockAccessor accessor) {
		OreCracker.Progress progress = OreCracker.progressAt(accessor.getLevel(), accessor.getPosition());
		if (progress == null) {
			return;
		}
		data.putInt(KEY_HITS, progress.hits());
		data.putInt(KEY_REQUIRED, progress.requiredHits());
	}

	@Override
	public ResourceLocation getUid() {
		return UID;
	}
}
