package dev.jensh.mythicrpgcompat;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

/**
 * A post-netherite tier RPG class armor can be forged to, matching the RPG Series' own Ruby
 * (Better Nether) and Aeternium (Better End) weapons. Stored on stacks as the forged component.
 */
public enum Forge implements StringRepresentable, TooltipProvider {
	/** Stats over Netherite taken from Better Nether's netherite-tier Fireruby armor. */
	RUBY("ruby", 0xD3263B, "betternether", "flaming_ruby"),
	/** Stats over Netherite taken from Better End's Aeternium armor. */
	AETERNIUM("aeternium", 0x2AA79B, "betterend", "aeternium");

	public static final Codec<Forge> CODEC = StringRepresentable.fromEnum(Forge::values);
	public static final IntFunction<Forge> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
	public static final StreamCodec<ByteBuf, Forge> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

	private final String name;
	private final int color;
	private final String mod;
	private final String referenceMaterial;

	Forge(String name, int color, String mod, String referenceMaterial) {
		this.name = name;
		this.color = color;
		this.mod = mod;
		this.referenceMaterial = referenceMaterial;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	/** Recolour variant key, also used in model and texture paths. */
	public String variant() {
		return "forged_" + name;
	}

	/** The other mod's armor piece this tier's stats are read from, or air if that mod is missing. */
	public Item reference(EquipmentSlot slot) {
		String piece = switch (slot) {
			case HEAD -> "helmet";
			case CHEST -> "chestplate";
			case LEGS -> "leggings";
			default -> "boots";
		};
		return BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(mod, referenceMaterial + "_" + piece)).orElse(Items.AIR);
	}

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
		consumer.accept(Component.translatable(
				"tooltip.mythic_rpg_compat.forged",
				Component.translatable("forge.mythic_rpg_compat." + name).withColor(color)
		).withColor(0xAAAAAA));
	}
}
