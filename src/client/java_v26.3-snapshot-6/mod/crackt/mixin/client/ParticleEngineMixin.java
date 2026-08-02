package mod.crackt.mixin.client;

import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 1.21.x does not need particle crack swapping; this stub satisfies mixin config.
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
}
