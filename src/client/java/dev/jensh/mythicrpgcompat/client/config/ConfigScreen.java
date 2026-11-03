package dev.jensh.mythicrpgcompat.client.config;

import dev.jensh.mythicrpgcompat.MythicParticles;
import dev.jensh.mythicrpgcompat.MythicParticles.Group;
import java.util.EnumMap;
import java.util.Map;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Cloth Config screen for {@code config/rpg_gear_upgrades.json}. Only load when Cloth Config is present. */
final class ConfigScreen {
	/** Sliders show factors as percentages; values above this in the file are kept unless changed. */
	private static final int MAX_PERCENT = 200;
	private static final String KEY = "config.mythic_rpg_compat.";

	private ConfigScreen() {
	}

	static Screen create(Screen parent) {
		MythicParticles.load();
		Double currentOverride = MythicParticles.override();
		boolean[] useOverride = {currentOverride != null};
		int[] overridePercent = {percent(currentOverride != null ? currentOverride : 1.0)};
		Map<Group, Double> factors = new EnumMap<>(Group.class);
		for (Group group : Group.values()) {
			factors.put(group, MythicParticles.ownFactor(group));
		}

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable(KEY + "title"))
				.setSavingRunnable(() -> {
					MythicParticles.set(useOverride[0] ? overridePercent[0] / 100.0 : null, factors);
					MythicParticles.save();
				});
		ConfigEntryBuilder entries = builder.entryBuilder();
		ConfigCategory particles = builder.getOrCreateCategory(Component.translatable(KEY + "particles"));

		particles.addEntry(entries.startTextDescription(Component.translatable(KEY + "particles.description")).build());
		particles.addEntry(entries.startBooleanToggle(Component.translatable(KEY + "particles.use_override"), useOverride[0])
				.setDefaultValue(false)
				.setTooltip(Component.translatable(KEY + "particles.use_override.tooltip"))
				.setSaveConsumer(value -> useOverride[0] = value)
				.build());
		particles.addEntry(slider(entries, Component.translatable(KEY + "particles.override"), overridePercent[0],
				Component.translatable(KEY + "particles.override.tooltip"), value -> overridePercent[0] = value));
		for (Group group : Group.values()) {
			double own = factors.get(group);
			Component name = Component.translatable(KEY + "particles.gem." + group.key());
			Component tooltip = Component.translatable(KEY + "particles.gem.tooltip", name);
			particles.addEntry(slider(entries, name, percent(own), tooltip, value -> {
				// Leave values outside the slider's range alone unless the slider was moved
				if (value != percent(own)) {
					factors.put(group, value / 100.0);
				}
			}));
		}
		return builder.build();
	}

	private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?> slider(ConfigEntryBuilder entries, Component name,
			int value, Component tooltip, java.util.function.IntConsumer save) {
		return entries.startIntSlider(name, value, 0, MAX_PERCENT)
				.setDefaultValue(100)
				.setTextGetter(percent -> Component.literal(percent + "%"))
				.setTooltip(tooltip)
				.setSaveConsumer(save::accept)
				.build();
	}

	private static int percent(double factor) {
		return (int) Math.round(Math.clamp(factor, 0, MAX_PERCENT / 100.0) * 100);
	}
}
