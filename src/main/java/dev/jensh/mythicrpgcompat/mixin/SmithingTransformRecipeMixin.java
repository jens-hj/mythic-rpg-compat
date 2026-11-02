package dev.jensh.mythicrpgcompat.mixin;

import dev.jensh.mythicrpgcompat.Infusion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** First-time infusion perks that can't be expressed as a static recipe result (an extra gem socket). */
@Mixin(SmithingTransformRecipe.class)
public abstract class SmithingTransformRecipeMixin {
	@Inject(method = "assemble(Lnet/minecraft/world/item/crafting/SmithingRecipeInput;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
	private void mythicRpgCompat$firstInfusion(SmithingRecipeInput input, CallbackInfoReturnable<ItemStack> cir) {
		Infusion.grantPerks(cir.getReturnValue());
	}
}
