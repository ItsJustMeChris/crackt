package mod.crackt;

import mod.crackt.block.CrackingClusterBlock;
import mod.crackt.block.CrackingClusterBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

/**
 * Central point for block and block entity registration.
 */
public final class CracktBlocks {
	public static final String CLUSTER_NAME = "cracking_cluster";
	private static final ResourceKey<Block> CLUSTER_KEY = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Crackt.MOD_ID, CLUSTER_NAME));

	public static final CrackingClusterBlock CRACKING_CLUSTER = new CrackingClusterBlock(
		BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)
			.setId(CLUSTER_KEY)
			.noOcclusion()
			.pushReaction(PushReaction.DESTROY)
	);

	public static final BlockEntityType<CrackingClusterBlockEntity> CRACKING_CLUSTER_ENTITY =
		FabricBlockEntityTypeBuilder.create(CrackingClusterBlockEntity::new, CRACKING_CLUSTER).build();

	private static boolean registered;

	private CracktBlocks() {}

	public static void register() {
		if (registered) return;
		registered = true;

		registerBlock(CLUSTER_NAME, CRACKING_CLUSTER);

		registerBlockEntity(CLUSTER_NAME, CRACKING_CLUSTER_ENTITY);
	}

	private static void registerBlock(String path, Block block) {
		Identifier id = Identifier.fromNamespaceAndPath(Crackt.MOD_ID, path);
		Registry.register(BuiltInRegistries.BLOCK, id, block);
	}

	private static <T extends BlockEntityType<?>> void registerBlockEntity(String path, T type) {
		Identifier id = Identifier.fromNamespaceAndPath(Crackt.MOD_ID, path);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, type);
	}
}
