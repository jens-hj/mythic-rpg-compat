package dev.jensh.mythicrpgcompat.mixin;

import dev.jensh.mythicrpgcompat.Visuals;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A forged piece infused with a gem (or the other way round) shows the gem's look, whichever came last. */
@Mixin(SmithingTransformRecipe.class)
public abstract class SmithingTransformRecipeMixin {
	@Inject(method = "assemble(Lnet/minecraft/world/item/crafting/SmithingRecipeInput;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
	private void mythicRpgCompat$fixVisuals(SmithingRecipeInput input, CallbackInfoReturnable<ItemStack> cir) {
		Visuals.apply(cir.getReturnValue());
	}
}
