package dev.jensh.mythicrpgcompat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.Nullable;

/**
 * Keeps an upgraded stack's look in line with its upgrades: the item model and, for armor, the
 * armor_model_api texture. A piece that is both forged and infused gets the two-tone variant
 * {@code <gem>_<forge variant>} (forge colours on the dark shades, gem colours on the light ones),
 * whatever the order of smithing.
 */
public final class Visuals {
	private static final String ARMOR_API = "armor_model_api";
	private static final String[] ARMOR_SUFFIXES = {"_head", "_chest", "_legs", "_feet"};

	private Visuals() {
	}

	public static @Nullable String variant(ItemStack stack) {
		Gem gem = Infusion.gem(stack);
		Forge forge = Infusion.forge(stack);
		if (gem != null && forge != null) {
			return gem.getSerializedName() + "_" + forge.variant();
		}
		if (gem != null) {
			return gem.getSerializedName();
		}
		return forge == null ? null : forge.variant();
	}

	public static void apply(ItemStack stack) {
		String variant = variant(stack);
		if (variant == null) {
			return;
		}
		Identifier item = BuiltInRegistries.ITEM.getKey(stack.getItem());
		Identifier model = MythicRpgCompat.id(variant + "/" + item.getNamespace() + "/" + item.getPath());
		if (!model.equals(stack.get(DataComponents.ITEM_MODEL))) {
			stack.set(DataComponents.ITEM_MODEL, model);
		}
		if (Infusion.armorSlot(stack) == null) {
			return;
		}
		String set = armorSet(item.getPath());
		if (set == null) {
			return;
		}
		String texture = MythicRpgCompat.id("textures/armor/" + variant + "/" + item.getNamespace() + "/" + set + ".png").toString();
		CompoundTag current = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompoundOrEmpty(ARMOR_API);
		if (!texture.equals(current.getStringOr("texture", ""))) {
			CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
				CompoundTag api = tag.getCompoundOrEmpty(ARMOR_API);
				api.putString("texture", texture);
				tag.put(ARMOR_API, api);
			});
		}
	}

	/** {@code netherite_ranger_armor_chest} to {@code netherite_ranger_armor}, matching the generated texture paths. */
	private static @Nullable String armorSet(String path) {
		for (String suffix : ARMOR_SUFFIXES) {
			if (path.endsWith(suffix)) {
				return path.substring(0, path.length() - suffix.length());
			}
		}
		return null;
	}
}
