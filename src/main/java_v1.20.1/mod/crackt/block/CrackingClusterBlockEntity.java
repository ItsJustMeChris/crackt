package mod.crackt.block;

import mod.crackt.CracktBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Stores which ore block state to render for the cracking cluster core.
 * 1.20.1 variant uses CompoundTag save/load APIs.
 */
public class CrackingClusterBlockEntity extends BlockEntity {
	private BlockState displayState;
	private Vec3 displayOffset = Vec3.ZERO;

	public CrackingClusterBlockEntity(BlockPos pos, BlockState state) {
		super(CracktBlocks.CRACKING_CLUSTER_ENTITY, pos, state);
	}

	public BlockState getDisplayState() {
		return displayState;
	}

	public BlockState getDisplayOrDefault() {
		return displayState != null ? displayState : defaultState();
	}

	public void setDisplayState(BlockState state) {
		this.displayState = state;
		setChanged();
	}

	public Vec3 getDisplayOffset() {
		return displayOffset;
	}

	public void setDisplayOffset(Vec3 offset) {
		this.displayOffset = offset;
		setChanged();
	}

	private BlockState defaultState() {
		return Blocks.IRON_ORE.defaultBlockState();
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (displayState != null) {
			tag.putInt("display_state", Block.getId(displayState));
		}
		if (!displayOffset.equals(Vec3.ZERO)) {
			tag.putDouble("off_x", displayOffset.x);
			tag.putDouble("off_y", displayOffset.y);
			tag.putDouble("off_z", displayOffset.z);
		}
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		int id = tag.contains("display_state") ? tag.getInt("display_state") : 0;
		displayState = id != 0 ? Block.stateById(id) : null;
		double ox = tag.contains("off_x") ? tag.getDouble("off_x") : 0.0;
		double oy = tag.contains("off_y") ? tag.getDouble("off_y") : 0.0;
		double oz = tag.contains("off_z") ? tag.getDouble("off_z") : 0.0;
		displayOffset = new Vec3(ox, oy, oz);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag() {
		return saveWithoutMetadata();
	}
}
