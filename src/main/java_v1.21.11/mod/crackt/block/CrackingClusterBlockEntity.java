package mod.crackt.block;

import mod.crackt.CracktBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Stores which ore block state to render for the cracking cluster core.
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
	protected void saveAdditional(ValueOutput writer) {
		super.saveAdditional(writer);
		if (displayState != null) {
			writer.putInt("display_state", Block.getId(displayState));
		}
		if (!displayOffset.equals(Vec3.ZERO)) {
			writer.putDouble("off_x", displayOffset.x);
			writer.putDouble("off_y", displayOffset.y);
			writer.putDouble("off_z", displayOffset.z);
		}
	}

	@Override
	public void loadAdditional(ValueInput reader) {
		super.loadAdditional(reader);
		int id = reader.getIntOr("display_state", 0);
		displayState = id != 0 ? Block.stateById(id) : null;
		double ox = reader.getDoubleOr("off_x", 0.0);
		double oy = reader.getDoubleOr("off_y", 0.0);
		double oz = reader.getDoubleOr("off_z", 0.0);
		displayOffset = new Vec3(ox, oy, oz);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider lookupProvider) {
		return saveWithoutMetadata(lookupProvider);
	}
}
