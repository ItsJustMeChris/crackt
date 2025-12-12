package mod.crackt;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;

/**
 * Networking helpers for debug inspect overlay.
 */
public final class CracktNetworking {
	private static boolean payloadsRegistered = false;
	private CracktNetworking() {}

	public static void registerPayloads() {
		if (payloadsRegistered) return;
		payloadsRegistered = true;
		PayloadTypeRegistry.playS2C().register(CrackingClusterDisplay.ID, CrackingClusterDisplay.CODEC);
	}

	public static void registerServerReceivers() {
		// no-op for now
	}

	public static void syncCrackingCluster(ServerLevel level, BlockPos pos, BlockState state, net.minecraft.world.phys.Vec3 offset) {
		CrackingClusterDisplay payload = new CrackingClusterDisplay(pos, state, offset);
		for (ServerPlayer player : PlayerLookup.tracking(level, pos)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	public record CrackingClusterDisplay(BlockPos pos, BlockState state, net.minecraft.world.phys.Vec3 offset) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<CrackingClusterDisplay> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Crackt.MOD_ID, "cracking_cluster_display"));
		public static final StreamCodec<FriendlyByteBuf, CrackingClusterDisplay> CODEC = CustomPacketPayload.codec(CrackingClusterDisplay::write, CrackingClusterDisplay::new);

		public CrackingClusterDisplay(FriendlyByteBuf buf) {
			this(buf.readBlockPos(), Block.stateById(buf.readVarInt()), new net.minecraft.world.phys.Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
		}

		private void write(FriendlyByteBuf buf) {
			buf.writeBlockPos(pos);
			buf.writeVarInt(Block.getId(state));
			buf.writeDouble(offset.x);
			buf.writeDouble(offset.y);
			buf.writeDouble(offset.z);
		}

		@Override
		public CustomPacketPayload.Type<CrackingClusterDisplay> type() {
			return ID;
		}
	}
}
