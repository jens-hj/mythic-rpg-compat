package dev.jensh.mythicrpgcompat.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.jensh.mythicrpgcompat.Infusion;
import dev.jensh.mythicrpgcompat.RangedProcs;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mythic Upgrades detects its armor set effects and weapon on-hit effects by item identity.
 * Inside those handlers, make infused RPG gear report the matching Mythic item instead, and let
 * {@link RangedProcs} decide which ranged and spell hits count.
 */
@Mixin(value = LivingEntity.class, priority = 1500)
public abstract class MythicLivingEntityMixinSquared {
	private static final String MYTHIC_MIXIN = "net.trique.mythicupgrades.mixin.LivingEntityMixin";
	private static final String GET_DIRECT_ENTITY = "Lnet/minecraft/world/damagesource/DamageSource;getDirectEntity()Lnet/minecraft/world/entity/Entity;";
	private static final String GET_ITEM = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;";

	@TargetHandler(mixin = MYTHIC_MIXIN, name = "checkArmorEffects")
	@WrapOperation(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = GET_ITEM))
	private Item mythicRpgCompat$armorView(ItemStack stack, Operation<Item> original) {
		return Infusion.mythicView(stack, original.call(stack));
	}

	@TargetHandler(mixin = MYTHIC_MIXIN, name = "onHurt")
	@WrapOperation(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = GET_ITEM))
	private Item mythicRpgCompat$weaponView(ItemStack stack, Operation<Item> original) {
		return RangedProcs.weapon(Infusion.mythicView(stack, original.call(stack)));
	}

	/** Lets arrows from infused bows through Mythic's melee-only check, and limits spell procs. */
	@TargetHandler(mixin = MYTHIC_MIXIN, name = "onHurt")
	@WrapOperation(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = GET_DIRECT_ENTITY))
	private Entity mythicRpgCompat$directEntity(DamageSource source, Operation<Entity> original) {
		return RangedProcs.directEntity((LivingEntity) (Object) this, source, original.call(source));
	}
}
