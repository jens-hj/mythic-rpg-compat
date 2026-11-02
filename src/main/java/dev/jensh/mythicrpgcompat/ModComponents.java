package dev.jensh.mythicrpgcompat;

import net.fabricmc.fabric.api.item.v1.ItemComponentTooltipProviderRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModComponents {
	/** Which Mythic gem a piece of RPG gear has been infused with. */
	public static final DataComponentType<Gem> INFUSION = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			MythicRpgCompat.id("infusion"),
			DataComponentType.<Gem>builder().persistent(Gem.CODEC).networkSynchronized(Gem.STREAM_CODEC).build()
	);

	private ModComponents() {
	}

	public static void init() {
		ItemComponentTooltipProviderRegistry.addFirst(INFUSION);
	}
}
