package mod.crackt.compat;

import mcp.mobius.waila.api.ICommonRegistrar;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import mcp.mobius.waila.api.IWailaCommonPlugin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

@SuppressWarnings({"rawtypes", "unchecked"})
public class WthitCommonCompat implements IWailaCommonPlugin, IDataProvider {
	private static final WthitCommonCompat INSTANCE = new WthitCommonCompat();

	@Override
	public void register(ICommonRegistrar registrar) {
		registrar.blockData(INSTANCE, Block.class);
	}

	@Override
	public void appendData(IDataWriter data, IServerAccessor accessor, IPluginConfig config) {
		if (!(accessor.getLevel() instanceof ServerLevel level)) {
			return;
		}
		if (!(accessor.getHitResult() instanceof BlockHitResult hitResult)) {
			return;
		}
		BlockState state = level.getBlockState(hitResult.getBlockPos());
		if (!LootPreviewUtil.shouldShow(state)) {
			return;
		}
		LootPreviewUtil.writePreview(data.raw(), level, hitResult.getBlockPos(), state, accessor.getPlayer());
	}
}
