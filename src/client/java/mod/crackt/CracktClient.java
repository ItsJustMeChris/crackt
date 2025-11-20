package mod.crackt;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import mod.crackt.block.CrackingClusterBlockEntity;
import mod.crackt.block.CrackingClusterRenderer;
import mod.crackt.ClientDisplayCache;

@SuppressWarnings("deprecation")
public class CracktClient implements ClientModInitializer {
	private final java.util.Map<net.minecraft.core.BlockPos, ClusterDisplay> pendingClusterDisplays = new java.util.HashMap<>();

	@Override
	public void onInitializeClient() {
		CracktNetworking.registerPayloads();
		BlockEntityRenderers.register(CracktBlocks.CRACKING_CLUSTER_ENTITY, CrackingClusterRenderer::new);

		ClientPlayNetworking.registerGlobalReceiver(CracktNetworking.CrackingClusterDisplay.ID, (payload, context) -> {
			context.client().execute(() -> {
				if (context.client().level == null) return;
				ClientDisplayCache.put(payload.pos(), payload.state());
				if (context.client().level.getBlockEntity(payload.pos()) instanceof CrackingClusterBlockEntity cluster) {
					cluster.setDisplayState(payload.state());
					cluster.setDisplayOffset(payload.offset());
					return;
				}
				pendingClusterDisplays.put(payload.pos(), new ClusterDisplay(payload.state(), payload.offset()));
			});
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.level == null) {
				pendingClusterDisplays.clear();
				ClientDisplayCache.clear();
				return;
			}
			if (pendingClusterDisplays.isEmpty()) return;
			pendingClusterDisplays.entrySet().removeIf(entry -> {
				var be = client.level.getBlockEntity(entry.getKey());
				if (be instanceof CrackingClusterBlockEntity cluster) {
					cluster.setDisplayState(entry.getValue().state());
					cluster.setDisplayOffset(entry.getValue().offset());
					ClientDisplayCache.put(entry.getKey(), entry.getValue().state());
					return true;
				}
				return false;
			});
		});
	}

	private record ClusterDisplay(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.phys.Vec3 offset) {}
}
