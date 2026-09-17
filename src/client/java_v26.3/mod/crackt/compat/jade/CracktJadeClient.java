package mod.crackt.compat.jade;

import mod.crackt.block.CrackingClusterBlock;
import snownee.jade.api.IWailaClientRegistration;

/**
 * Client-only half of the Jade integration. Invoked reflectively by CracktJadePlugin so
 * that a dedicated server never has to load these classes.
 */
public final class CracktJadeClient {
	private CracktJadeClient() {}

	public static void register(IWailaClientRegistration registration) {
		registration.registerBlockComponent(CrackingClusterProvider.INSTANCE, CrackingClusterBlock.class);
		registration.registerBlockIcon(CrackingClusterProvider.INSTANCE, CrackingClusterBlock.class);
		registration.registerBlockComponent(CrackingClusterModNameProvider.INSTANCE, CrackingClusterBlock.class);
	}
}
