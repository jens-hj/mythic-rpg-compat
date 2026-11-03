package dev.jensh.mythicrpgcompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mythic's Jade aura adds its speed modifier with {@code addTransientModifier}, which throws if
 * the modifier is still on the entity when the aura is re-applied, crashing the server tick.
 * Add or update it instead, as vanilla effects do.
 */
@Pseudo
@Mixin(targets = "net.trique.mythicupgrades.effect.JadeAuraEffect")
public abstract class JadeAuraEffectMixin {
	@WrapOperation(method = "addAttributeModifiers", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;addTransientModifier(Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;)V"))
	private void mythicRpgCompat$addOrUpdate(AttributeInstance instance, AttributeModifier modifier, Operation<Void> original) {
		instance.addOrUpdateTransientModifier(modifier);
	}
}
