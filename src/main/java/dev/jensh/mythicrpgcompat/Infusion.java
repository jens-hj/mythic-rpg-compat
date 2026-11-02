package dev.jensh.mythicrpgcompat;

import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;
import net.trique.mythicupgrades.item.MythicArmorMaterials;
import org.jspecify.annotations.Nullable;

/** Behaviour of stacks carrying the {@link ModComponents#INFUSION} component. */
public final class Infusion {
	private Infusion() {
	}

	public static @Nullable Gem gem(ItemStack stack) {
		return stack.isEmpty() ? null : stack.get(ModComponents.INFUSION);
	}

	/** Armor slot of the stack, or null for weapons and other held items. */
	public static @Nullable EquipmentSlot armorSlot(ItemStack stack) {
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable == null) {
			return null;
		}
		EquipmentSlot slot = equippable.slot();
		return slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR ? slot : null;
	}

	/**
	 * The item Mythic Upgrades should see in place of this stack's own item: infused armor
	 * becomes the gem's armor piece for its slot, any other infused item becomes the gem's sword.
	 */
	public static Item mythicView(ItemStack stack, Item original) {
		Gem gem = gem(stack);
		if (gem == null) {
			return original;
		}
		EquipmentSlot slot = armorSlot(stack);
		Item surrogate = gem.surrogate(slot == null ? EquipmentSlot.MAINHAND : slot);
		return surrogate == net.minecraft.world.item.Items.AIR ? original : surrogate;
	}

	/** Extra modifiers an infused armor piece grants: Mythic material stats on top of Netherite's. */
	public static void forEachBonus(ItemStack stack, EquipmentSlot querySlot, BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
		Gem gem = gem(stack);
		EquipmentSlot slot = armorSlot(stack);
		if (gem == null || slot == null || slot != querySlot) {
			return;
		}
		ArmorType type = switch (slot) {
			case HEAD -> ArmorType.HELMET;
			case CHEST -> ArmorType.CHESTPLATE;
			case LEGS -> ArmorType.LEGGINGS;
			default -> ArmorType.BOOTS;
		};
		ArmorMaterial mythic = material(gem);
		ArmorMaterial netherite = ArmorMaterials.NETHERITE;
		var id = MythicRpgCompat.id("infusion_" + slot.getName());

		double armor = mythic.defense().getOrDefault(type, 0) - netherite.defense().getOrDefault(type, 0);
		double toughness = mythic.toughness() - netherite.toughness();
		double knockback = mythic.knockbackResistance() - netherite.knockbackResistance();
		if (armor != 0) {
			consumer.accept(Attributes.ARMOR, new AttributeModifier(id, armor, AttributeModifier.Operation.ADD_VALUE));
		}
		if (toughness != 0) {
			consumer.accept(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, toughness, AttributeModifier.Operation.ADD_VALUE));
		}
		if (knockback != 0) {
			consumer.accept(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(id, knockback, AttributeModifier.Operation.ADD_VALUE));
		}
	}

	public static void forEachBonus(ItemStack stack, EquipmentSlotGroup group, BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
		EquipmentSlot slot = armorSlot(stack);
		if (slot != null && EquipmentSlotGroup.bySlot(slot).equals(group)) {
			forEachBonus(stack, slot, consumer);
		}
	}

	private static ArmorMaterial material(Gem gem) {
		return switch (gem) {
			case AMETRINE -> MythicArmorMaterials.AMETRINE;
			case AQUAMARINE -> MythicArmorMaterials.AQUAMARINE;
			case CITRINE -> MythicArmorMaterials.CITRINE;
			case JADE -> MythicArmorMaterials.JADE;
			case PERIDOT -> MythicArmorMaterials.PERIDOT;
			case RUBY -> MythicArmorMaterials.RUBY;
			case SAPPHIRE -> MythicArmorMaterials.SAPPHIRE;
			case TOPAZ -> MythicArmorMaterials.TOPAZ;
		};
	}
}
