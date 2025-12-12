package mod.crackt;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import mod.crackt.block.CrackingClusterBlockEntity;
import mod.crackt.block.CrackingClusterRenderer;

public class CracktClient implements ClientModInitializer {
	private final java.util.Map<BlockPos, ClusterDisplay> pendingClusterDisplays = new java.util.HashMap<>();

	@Override
	public void onInitializeClient() {
		CracktNetworking.registerPayloads();
		BlockEntityRenderers.register(CracktBlocks.CRACKING_CLUSTER_ENTITY, CrackingClusterRenderer::new);

		ClientPlayNetworking.registerGlobalReceiver(CracktNetworking.CRACKING_CLUSTER_DISPLAY_ID, (client, handler, buf, responseSender) -> {
			handleClusterDisplay(client, buf);
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.level == null) {
				pendingClusterDisplays.clear();
				ClientDisplayCache.clear();
				return;
			}
			if (pendingClusterDisplays.isEmpty()) return;
			pendingClusterDisplays.entrySet().removeIf(entry -> {
				if (client.level.getBlockEntity(entry.getKey()) instanceof CrackingClusterBlockEntity cluster) {
					cluster.setDisplayState(entry.getValue().state());
					cluster.setDisplayOffset(entry.getValue().offset());
					ClientDisplayCache.put(entry.getKey(), entry.getValue().state());
					return true;
				}
				return false;
			});
		});
	}

	private void handleClusterDisplay(Minecraft client, FriendlyByteBuf buf) {
		BlockPos pos = buf.readBlockPos();
		BlockState state = Block.stateById(buf.readVarInt());
		Vec3 offset = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
		client.execute(() -> {
			if (client.level == null) return;
			ClientDisplayCache.put(pos, state);
			if (client.level.getBlockEntity(pos) instanceof CrackingClusterBlockEntity cluster) {
				cluster.setDisplayState(state);
				cluster.setDisplayOffset(offset);
			} else {
				pendingClusterDisplays.put(pos, new ClusterDisplay(state, offset));
			}
		});
	}

	private record ClusterDisplay(BlockState state, Vec3 offset) {}
}
