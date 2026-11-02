package dev.jensh.mythicrpgcompat;

import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.spell_engine.entity.DamageSourceExtension;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;

/** Spell Engine lookups. Only call when spell_engine is loaded. */
final class SpellEngineCompat {
	private SpellEngineCompat() {
	}

	/** Damage types of the magic spell schools (arcane, fire, frost, ...), from every installed mod. */
	private static Set<ResourceKey<DamageType>> magicTypes;

	/**
	 * Damage from a magic spell school. Physical schools (weapon skills) deal vanilla attack damage
	 * and keep behaving like melee.
	 */
	static boolean isSpell(DamageSource source) {
		if (magicTypes == null) {
			magicTypes = SpellSchools.all().stream()
					.filter(SpellSchool::isMagicArchetype)
					.map(school -> school.damageType)
					.collect(Collectors.toUnmodifiableSet());
		}
		return source.typeHolder().unwrapKey().map(magicTypes::contains).orElse(false);
	}

	/** Splash or area victim, as opposed to the spell's direct target. */
	static boolean isIndirect(DamageSource source) {
		return source instanceof DamageSourceExtension extension && extension.isSpellIndirect();
	}
}
