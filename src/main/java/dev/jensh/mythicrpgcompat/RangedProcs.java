package dev.jensh.mythicrpgcompat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.jspecify.annotations.Nullable;

/**
 * Decides which hits trigger a Mythic weapon gem effect besides melee. Mythic only procs when the
 * damage's direct entity is the attacker, using the attacker's main-hand item. Inside its handler:
 * <ul>
 *   <li>arrows fired from an infused bow or crossbow count as a hit by the attacker with the gem's
 *       sword, if the arrow is a crit (a fully drawn bow; crossbow bolts always are). Thrown weapons
 *       (no bow involved) always count;</li>
 *   <li>spell hits (already direct) proc only on the spell's direct target, not on splash or area
 *       victims, and at most once per target per {@link #SPELL_COOLDOWN_TICKS} per caster.</li>
 * </ul>
 */
public final class RangedProcs {
	public static final int SPELL_COOLDOWN_TICKS = 20;
	private static final boolean SPELL_ENGINE = FabricLoader.getInstance().isModLoaded("spell_engine");

	/** Gem of the projectile being handled; read by the weapon lookup right after the direct-entity check. */
	private static final ThreadLocal<Gem> PENDING = new ThreadLocal<>();
	/** victim -> caster -> game time of the last spell proc. Server thread only. */
	private static final Map<LivingEntity, Map<UUID, Long>> SPELL_PROCS = new WeakHashMap<>();

	private RangedProcs() {
	}

	/** Replaces Mythic's {@code source.getDirectEntity()} in its on-hurt handler. */
	public static @Nullable Entity directEntity(LivingEntity victim, DamageSource source, @Nullable Entity direct) {
		PENDING.remove();
		Entity attacker = source.getEntity();
		if (direct instanceof AbstractArrow arrow && attacker instanceof LivingEntity && arrow.getOwner() == attacker) {
			Gem gem = Infusion.gem(arrow.getWeaponItem());
			boolean fromLauncher = arrow.getWeaponItem().getItem() instanceof ProjectileWeaponItem;
			if (gem != null && (arrow.isCritArrow() || !fromLauncher)) {
				PENDING.set(gem);
				return attacker;
			}
			return direct;
		}
		if (SPELL_ENGINE && direct != null && direct == attacker && SpellEngineCompat.isSpell(source)) {
			return allowSpellProc(victim, attacker, SpellEngineCompat.isIndirect(source)) ? direct : null;
		}
		return direct;
	}

	private static boolean allowSpellProc(LivingEntity victim, Entity caster, boolean indirect) {
		if (indirect) {
			return false;
		}
		long now = victim.level().getGameTime();
		Map<UUID, Long> byCaster = SPELL_PROCS.computeIfAbsent(victim, v -> new HashMap<>());
		Long last = byCaster.get(caster.getUUID());
		if (last != null && now - last < SPELL_COOLDOWN_TICKS) {
			return false;
		}
		byCaster.put(caster.getUUID(), now);
		return true;
	}

	/** Replaces the weapon Mythic reads: the projectile's gem sword when a projectile hit was let through. */
	public static Item weapon(Item original) {
		Gem gem = PENDING.get();
		if (gem == null) {
			return original;
		}
		PENDING.remove();
		Item sword = gem.surrogate(EquipmentSlot.MAINHAND);
		return sword == Items.AIR ? original : sword;
	}
}
