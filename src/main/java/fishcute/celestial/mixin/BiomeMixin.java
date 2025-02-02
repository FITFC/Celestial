package fishcute.celestial.mixin;

import fishcute.celestial.sky.CelestialSky;
import fishcute.celestial.util.Util;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public class BiomeMixin {
    @Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true)
    private void getFogColor(CallbackInfoReturnable<Integer> info) {
        if (CelestialSky.doesDimensionHaveCustomSky()) {
            info.setReturnValue(Util.getDecimal(CelestialSky.getDimensionRenderInfo().environment.fogColor.storedColor));
        }
    }
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void getSkyColor(CallbackInfoReturnable<Integer> info) {
        if (CelestialSky.doesDimensionHaveCustomSky()) {
            info.setReturnValue(Util.getDecimal(CelestialSky.getDimensionRenderInfo().environment.skyColor.storedColor));
        }
    }
}
