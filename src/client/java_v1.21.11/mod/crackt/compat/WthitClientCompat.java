package mod.crackt.compat;

import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.IWailaConfig;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.WailaConstants;
import mcp.mobius.waila.api.IWailaClientPlugin;
import mcp.mobius.waila.api.component.ItemComponent;
import mcp.mobius.waila.api.component.WrappedComponent;
import mod.crackt.ClientDisplayCache;
import mod.crackt.CracktBlocks;
import mod.crackt.block.CrackingClusterBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class WthitClientCompat implements IWailaClientPlugin, IBlockComponentProvider {
	private static final WthitClientCompat INSTANCE = new WthitClientCompat();

	@Override
	public void register(IClientRegistrar registrar) {
		registrar.body(INSTANCE, Block.class);
		registrar.head(INSTANCE, Block.class);
	}

	@Override
	public void appendHead(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
		if (!accessor.getBlockState().is(CracktBlocks.CRACKING_CLUSTER)) {
			return;
		}
		LootPreviewUtil.TooltipData data = LootPreviewUtil.readTooltipData(accessor.getData().raw());
		if (data.isEmpty() && accessor.getBlockEntity() instanceof CrackingClusterBlockEntity cluster) {
			data = new LootPreviewUtil.TooltipData(List.of(), cluster.getDisplayOrDefault().getBlock().getDescriptionId(), cluster.getHits(), cluster.getRequiredHits());
		}
		if (data.isEmpty()) {
			BlockState cached = ClientDisplayCache.get(accessor.getPosition());
			if (cached != null) {
				data = new LootPreviewUtil.TooltipData(List.of(), cached.getBlock().getDescriptionId(), 0, 0);
			}
		}
		if (data.isEmpty()) {
			return;
		}
		Component title = Component.translatable("tooltip.crackt.cracked_block", Component.translatable(data.displayNameKey()));
		tooltip.setLine(WailaConstants.OBJECT_NAME_TAG, IWailaConfig.get().getFormatter().blockName(title.getString()));
	}

	@Override
	public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
		LootPreviewUtil.TooltipData syncedData = LootPreviewUtil.readTooltipData(accessor.getData().raw());
		LootPreviewUtil.TooltipData data = syncedData;
		if (data.isEmpty()) {
			data = LootPreviewUtil.buildClientFallback(accessor.getLevel(), accessor.getPosition(), accessor.getBlockState());
		}
		if (data.isEmpty()) {
			return;
		}

		tooltip.addLine(new WrappedComponent(net.minecraft.network.chat.Component.translatable("tooltip.crackt.x_out_of_y_cracks", data.hits(), data.requiredHits())));
		for (ItemStack stack : syncedData.preview()) {
			tooltip.addLine().with(new ItemComponent(stack));
		}
	}
}
