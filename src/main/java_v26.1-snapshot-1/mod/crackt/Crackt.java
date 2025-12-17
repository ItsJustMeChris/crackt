package mod.crackt;

import net.fabricmc.api.ModInitializer;

public class Crackt implements ModInitializer {
	public static final String MOD_ID = "crackt";

	@Override
	public void onInitialize() {
		CracktBlocks.register();
		CracktNetworking.registerPayloads();
		OreCracker.register();
		CracktNetworking.registerServerReceivers();
	}
}
