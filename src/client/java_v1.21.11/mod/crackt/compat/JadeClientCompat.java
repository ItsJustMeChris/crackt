package mod.crackt.compat;

import mod.crackt.Crackt;
import mod.crackt.block.CrackingClusterBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

import java.util.List;

public class JadeClientCompat implements IBlockComponentProvider {
	@Override
	public net.minecraft.resources.Identifier getUid() {
		return net.minecraft.resources.Identifier.fromNamespaceAndPath(Crackt.MOD_ID, "loot_preview");
	}

	@Override
	public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
		LootPreviewUtil.TooltipData data = LootPreviewUtil.readTooltipData(accessor.getServerData());
		if (data.isEmpty()) {
			return;
		}

		if (accessor.getBlockEntity() instanceof CrackingClusterBlockEntity) {
			tooltip.clear();
			tooltip.add(IThemeHelper.get().title(Component.translatable("tooltip.crackt.cracked_block", Component.translatable(data.displayNameKey()))));
		}

		tooltip.add(Component.translatable("tooltip.crackt.x_out_of_y_cracks", data.hits(), data.requiredHits()));
		for (ItemStack stack : data.preview()) {
			tooltip.add(stack.getHoverName().copy().append(" x" + stack.getCount()));
		}
	}
}
