package mod.crackt.compat.jade;

import mod.crackt.block.CrackingClusterBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade entry point, wired up through the "jade" entrypoint in fabric.mod.json.
 *
 * <p>This half lives in the common source set because Jade resolves the entrypoint on
 * dedicated servers too, where it registers the server data provider. The tooltip itself
 * touches client-only classes, so it is reached reflectively and never loaded on a server.
 */
@WailaPlugin
public class CracktJadePlugin implements IWailaPlugin {
	private static final String CLIENT_HOOK = "mod.crackt.compat.jade.CracktJadeClient";

	@Override
	public void register(IWailaCommonRegistration registration) {
		registration.registerBlockDataProvider(CrackingClusterData.INSTANCE, CrackingClusterBlockEntity.class);
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		try {
			Class.forName(CLIENT_HOOK)
				.getMethod("register", IWailaClientRegistration.class)
				.invoke(null, registration);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to register Crackt's Jade providers", e);
		}
	}
}
