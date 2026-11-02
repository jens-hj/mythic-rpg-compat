package dev.jensh.mythicrpgcompat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Material stats of another mod's armor, read from its items' default attribute modifiers so we
 * never touch that mod's classes. Used to give upgraded RPG armor the difference between, say,
 * an Aeternium chestplate and a Netherite one.
 */
public final class ReferenceStats {
	/** Armor, toughness, knockback resistance. */
	public record Base(double armor, double toughness, double knockback) {
		static final Base ZERO = new Base(0, 0, 0);

		Base minus(Base other) {
			return new Base(armor - other.armor, toughness - other.toughness, knockback - other.knockback);
		}

		/** Never make an upgrade worse than Netherite. */
		Base clampPositive() {
			return new Base(Math.max(0, armor), Math.max(0, toughness), Math.max(0, knockback));
		}
	}

	private static final Map<Item, Map<EquipmentSlot, Base>> CACHE = new ConcurrentHashMap<>();

	private ReferenceStats() {
	}

	/** {@code reference} minus the vanilla Netherite piece for {@code slot}, clamped at zero; zero when the reference is missing. */
	public static Base overNetherite(Item reference, EquipmentSlot slot) {
		if (reference == Items.AIR) {
			return Base.ZERO;
		}
		return of(reference, slot).minus(of(netherite(slot), slot)).clampPositive();
	}

	public static Base of(Item item, EquipmentSlot slot) {
		return CACHE.computeIfAbsent(item, i -> new ConcurrentHashMap<>()).computeIfAbsent(slot, s -> compute(item, s));
	}

	private static Base compute(Item item, EquipmentSlot slot) {
		EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(slot);
		double armor = 0, toughness = 0, knockback = 0;
		ItemAttributeModifiers modifiers = item.components().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
			if (!entry.slot().equals(group) || entry.modifier().operation() != AttributeModifier.Operation.ADD_VALUE) {
				continue;
			}
			Holder<Attribute> attribute = entry.attribute();
			double amount = entry.modifier().amount();
			if (attribute.equals(Attributes.ARMOR)) {
				armor += amount;
			} else if (attribute.equals(Attributes.ARMOR_TOUGHNESS)) {
				toughness += amount;
			} else if (attribute.equals(Attributes.KNOCKBACK_RESISTANCE)) {
				knockback += amount;
			}
		}
		return new Base(armor, toughness, knockback);
	}

	private static Item netherite(EquipmentSlot slot) {
		return switch (slot) {
			case HEAD -> Items.NETHERITE_HELMET;
			case CHEST -> Items.NETHERITE_CHESTPLATE;
			case LEGS -> Items.NETHERITE_LEGGINGS;
			default -> Items.NETHERITE_BOOTS;
		};
	}
}
