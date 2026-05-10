package mod.crackt.compat;

import mod.crackt.Crackt;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class JadeCompat implements IWailaPlugin {
	@Override
	public void register(IWailaCommonRegistration registrar) {
		registrar.registerBlockDataProvider(new JadeCommonProvider(), net.minecraft.world.level.block.Block.class);
	}

	@Override
	public void registerClient(IWailaClientRegistration registrar) {
		IBlockComponentProvider provider;
		try {
			provider = (IBlockComponentProvider) Class.forName("mod.crackt.compat.JadeClientCompat").getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Failed to load Jade client compat", exception);
		}
		registrar.registerBlockComponent(provider, net.minecraft.world.level.block.Block.class);
	}

	private static final class JadeCommonProvider implements snownee.jade.api.IServerDataProvider<snownee.jade.api.BlockAccessor> {
		@Override
		public void appendServerData(net.minecraft.nbt.CompoundTag data, snownee.jade.api.BlockAccessor accessor) {
			if (!(accessor.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) {
				return;
			}
			net.minecraft.world.level.block.state.BlockState state = accessor.getBlockState();
			if (!LootPreviewUtil.shouldShow(state)) {
				return;
			}
			LootPreviewUtil.writePreview(data, level, accessor.getPosition(), state, accessor.getPlayer());
		}

		@Override
		public net.minecraft.resources.Identifier getUid() {
			return net.minecraft.resources.Identifier.fromNamespaceAndPath(Crackt.MOD_ID, "loot_preview");
		}
	}
}
