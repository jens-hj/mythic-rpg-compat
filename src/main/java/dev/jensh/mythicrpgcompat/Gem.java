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

/** A Mythic Upgrades gem that RPG gear can be infused with. Stored on stacks as the infusion component. */
public enum Gem implements StringRepresentable, TooltipProvider {
	AMETRINE("ametrine", 0x8422AE),
	AQUAMARINE("aquamarine", 0x057B9E),
	CITRINE("citrine", 0xDCB40A),
	JADE("jade", 0x1D8B30),
	PERIDOT("peridot", 0x61AD0F),
	RUBY("ruby", 0xA90C37),
	SAPPHIRE("sapphire", 0x0C46B2),
	TOPAZ("topaz", 0xD1480D);

	public static final Codec<Gem> CODEC = StringRepresentable.fromEnum(Gem::values);
	public static final IntFunction<Gem> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
	public static final StreamCodec<ByteBuf, Gem> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

	public static final String MYTHIC = "mythicupgrades";

	private final String name;
	private final int color;

	Gem(String name, int color) {
		this.name = name;
		this.color = color;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public int color() {
		return color;
	}

	/** The Mythic item standing in for an infused piece: the gem's armor piece for that slot, or its sword. */
	public Item surrogate(EquipmentSlot slot) {
		String piece = switch (slot) {
			case HEAD -> "helmet";
			case CHEST -> "chestplate";
			case LEGS -> "leggings";
			case FEET -> "boots";
			default -> "sword";
		};
		return mythicItem(piece);
	}

	public Item mythicItem(String piece) {
		Identifier id = Identifier.fromNamespaceAndPath(MYTHIC, name + "_" + piece);
		return BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
	}

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
		consumer.accept(Component.translatable(
				"tooltip.mythic_rpg_compat.infused",
				Component.translatable("gem.mythic_rpg_compat." + name).withColor(color)
		).withColor(0xAAAAAA));
		if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jewelry")) {
			consumer.accept(Component.translatable("tooltip.mythic_rpg_compat.extra_socket_mount").withColor(0xAAAAAA));
		}
	}
}
