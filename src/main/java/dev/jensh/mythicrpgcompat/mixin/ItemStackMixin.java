package dev.jensh.mythicrpgcompat.mixin;

import dev.jensh.mythicrpgcompat.Infusion;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.apache.commons.lang3.function.TriConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds Mythic armor stats to infused pieces, on top of their own (config-driven) modifiers. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	/** Upgrade gear infused before one-time perks existed. Server side only; the change syncs to clients. */
	@Inject(method = "inventoryTick", at = @At("HEAD"))
	private void mythicRpgCompat$grantPerks(Level level, Entity owner, @Nullable EquipmentSlot slot, CallbackInfo ci) {
		if (!level.isClientSide()) {
			Infusion.grantPerks((ItemStack) (Object) this);
		}
	}

	@Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V", at = @At("TAIL"))
	private void mythicRpgCompat$addBonus(EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> consumer, CallbackInfo ci) {
		Infusion.forEachBonus((ItemStack) (Object) this, slot, consumer);
	}

	@Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V", at = @At("TAIL"))
	private void mythicRpgCompat$addBonusGroup(EquipmentSlotGroup group, TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> consumer, CallbackInfo ci) {
		Infusion.forEachBonus((ItemStack) (Object) this, group,
				(attribute, modifier) -> consumer.accept(attribute, modifier, ItemAttributeModifiers.Display.attributeModifiers()));
	}
}
