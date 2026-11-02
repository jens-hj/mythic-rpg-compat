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
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;
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

	/** How much infusion boosts an armor piece's own class attributes (ranged damage, spell power, ...). */
	public static final double CLASS_STAT_BOOST = 0.25;
	/**
	 * How much forging boosts them. Mirrors the RPG Series' own step from Netherite to Ruby or
	 * Aeternium weapons (e.g. claymore 11.5 to 13.0 damage, about 13%).
	 */
	public static final double FORGE_CLASS_STAT_BOOST = 0.15;
	/** Flat armor toughness each upgrade (infusion, forge) adds to an armor piece. */
	public static final double BONUS_TOUGHNESS = 1.0;

	public static @Nullable Forge forge(ItemStack stack) {
		return stack.isEmpty() ? null : stack.get(ModComponents.FORGED);
	}

	/**
	 * Extra modifiers an upgraded armor piece grants, per upgrade it carries (gem infusion and/or
	 * forge tier): the upgrade material's stats over Netherite, {@link #BONUS_TOUGHNESS}, and the
	 * piece's own class attributes boosted by {@link #CLASS_STAT_BOOST} / {@link #FORGE_CLASS_STAT_BOOST}.
	 */
	public static void forEachBonus(ItemStack stack, EquipmentSlot querySlot, BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
		EquipmentSlot slot = armorSlot(stack);
		if (slot == null || slot != querySlot) {
			return;
		}
		Gem gem = gem(stack);
		if (gem != null) {
			bonus(stack, slot, "infusion_", gem.surrogate(slot), CLASS_STAT_BOOST, consumer);
		}
		Forge forge = forge(stack);
		if (forge != null) {
			bonus(stack, slot, "forge_", forge.reference(slot), FORGE_CLASS_STAT_BOOST, consumer);
		}
	}

	private static void bonus(ItemStack stack, EquipmentSlot slot, String idPrefix, Item reference, double classBoost,
							  BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
		var id = MythicRpgCompat.id(idPrefix + slot.getName());
		ReferenceStats.Base delta = ReferenceStats.overNetherite(reference, slot);
		double toughness = delta.toughness() + BONUS_TOUGHNESS;
		if (delta.armor() != 0) {
			consumer.accept(Attributes.ARMOR, new AttributeModifier(id, delta.armor(), AttributeModifier.Operation.ADD_VALUE));
		}
		if (toughness != 0) {
			consumer.accept(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, toughness, AttributeModifier.Operation.ADD_VALUE));
		}
		if (delta.knockback() != 0) {
			consumer.accept(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(id, delta.knockback(), AttributeModifier.Operation.ADD_VALUE));
		}

		EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(slot);
		for (ItemAttributeModifiers.Entry entry : stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers()) {
			if (!entry.slot().equals(group) || BASE_ARMOR_STATS.contains(entry.attribute())) {
				continue;
			}
			AttributeModifier own = entry.modifier();
			consumer.accept(entry.attribute(), new AttributeModifier(id, own.amount() * classBoost, own.operation()));
		}
	}

	private static final java.util.Set<Holder<Attribute>> BASE_ARMOR_STATS =
			java.util.Set.of(Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS, Attributes.KNOCKBACK_RESISTANCE);

	/** How many extra Jewelry Socket Mounts an infused piece accepts beyond Jewelry's own limit. */
	public static final int EXTRA_SOCKET_MOUNTS = 1;

	/**
	 * Undo for 0.2.0, which added an empty gem socket on infusion instead of raising the mount
	 * limit: drop the marker and that socket if it is still empty.
	 */
	public static void revertLegacySocket(ItemStack stack) {
		if (!stack.has(ModComponents.PERKS_GRANTED)) {
			return;
		}
		stack.remove(ModComponents.PERKS_GRANTED);
		if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jewelry")) {
			JewelryCompat.removeTrailingEmptySocket(stack);
		}
	}

	public static void forEachBonus(ItemStack stack, EquipmentSlotGroup group, BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
		EquipmentSlot slot = armorSlot(stack);
		if (slot != null && EquipmentSlotGroup.bySlot(slot).equals(group)) {
			forEachBonus(stack, slot, consumer);
		}
	}
}
