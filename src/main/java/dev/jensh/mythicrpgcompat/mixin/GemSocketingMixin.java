package dev.jensh.mythicrpgcompat.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.jensh.mythicrpgcompat.Infusion;
import net.jewelry.gems.GemSocketing;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Infused gear accepts one more Jewelry Socket Mount than normal. Skipped when Jewelry isn't installed. */
@Pseudo
@Mixin(GemSocketing.class)
public abstract class GemSocketingMixin {
	@ModifyExpressionValue(
			method = "canMount",
			at = @At(value = "INVOKE", target = "Lnet/jewelry/gems/SocketMountComponent;maxMounted()I")
	)
	private static int mythicRpgCompat$extraMount(int maxMounted, ItemStack target) {
		return Infusion.gem(target) != null ? maxMounted + Infusion.EXTRA_SOCKET_MOUNTS : maxMounted;
	}
}
