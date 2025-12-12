package mod.crackt;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Simple S2C packet for cracking cluster display sync for 1.20.1 (pre-CustomPacketPayload).
 */
public final class CracktNetworking {
	public static final ResourceLocation CRACKING_CLUSTER_DISPLAY_ID = new ResourceLocation(Crackt.MOD_ID, "cracking_cluster_display");
	private CracktNetworking() {}

	public static void registerPayloads() {
		// no static payload registry in this version
	}

	public static void registerServerReceivers() {
		// no-op for now; cluster display sync is sent proactively from the ore cracker
	}

	public static void syncCrackingCluster(ServerLevel level, BlockPos pos, BlockState state, Vec3 offset) {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		buf.writeBlockPos(pos);
		buf.writeVarInt(Block.getId(state));
		buf.writeDouble(offset.x);
		buf.writeDouble(offset.y);
		buf.writeDouble(offset.z);
		for (ServerPlayer player : PlayerLookup.tracking(level, pos)) {
			ServerPlayNetworking.send(player, CRACKING_CLUSTER_DISPLAY_ID, new FriendlyByteBuf(buf.copy()));
		}
		buf.release();
	}
}
