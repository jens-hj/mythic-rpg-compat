package dev.jensh.mythicrpgcompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLongArray;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/**
 * Scales the particles Mythic Upgrades spawns for its gem effects, per effect group, from
 * {@code config/rpg_gear_upgrades.json}:
 * <pre>
 * "mythic_particles": { "override": null, "jade": 0.25, "topaz": 1.0, ... }
 * </pre>
 * A factor of 1 keeps Mythic's amount, 0 hides them, 0.25 keeps about a quarter. When
 * {@code override} is a number it applies to every group instead of the individual factors.
 * Particles are spawned by the server, so on a multiplayer server this is the server's config.
 */
public final class MythicParticles {
	/** Particle groups: one per gem that has particles (Sapphire has none), plus Necoium. */
	public enum Group {
		AMETRINE, AQUAMARINE, CITRINE, JADE, PERIDOT, RUBY, TOPAZ, NECOIUM;

		String key() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("rpg_gear_upgrades.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().disableHtmlEscaping().create();
	private static final String SECTION = "mythic_particles";

	private static @Nullable Double override;
	private static final Map<Group, Double> FACTORS = new EnumMap<>(Group.class);
	/** Particles sent and dropped per group, for tests and debugging. */
	private static final AtomicLongArray SENT = new AtomicLongArray(Group.values().length);
	private static final AtomicLongArray DROPPED = new AtomicLongArray(Group.values().length);

	private MythicParticles() {
	}

	public static double factor(Group group) {
		return override != null ? override : FACTORS.getOrDefault(group, 1.0);
	}

	public static void set(@Nullable Double override, Map<Group, Double> factors) {
		MythicParticles.override = override == null ? null : Math.max(0, override);
		FACTORS.clear();
		factors.forEach((group, factor) -> FACTORS.put(group, Math.max(0, factor)));
	}

	/**
	 * How many of {@code count} particles to actually send. Fractions round randomly so small
	 * bursts (often 1 particle per call) thin out on average instead of all or nothing. A count of 0
	 * means "one particle with a direction" in vanilla, so it is kept or dropped as a whole.
	 */
	public static int scale(Group group, int count) {
		double factor = factor(group);
		int scaled;
		if (factor == 1.0) {
			scaled = count;
		} else if (count == 0) {
			scaled = ThreadLocalRandom.current().nextDouble() < factor ? 0 : -1;
		} else {
			double exact = count * factor;
			scaled = (int) exact + (ThreadLocalRandom.current().nextDouble() < exact - (int) exact ? 1 : 0);
			if (scaled == 0) {
				scaled = -1;
			}
		}
		if (scaled < 0) {
			DROPPED.incrementAndGet(group.ordinal());
		} else {
			SENT.incrementAndGet(group.ordinal());
		}
		return scaled;
	}

	/** Target descriptor of {@code ServerLevel.sendParticles}, wrapped in Mythic's particle helpers. */
	public static final String SEND_PARTICLES =
			"Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I";

	/** Body of the sendParticles wrappers: sends the scaled amount, or nothing. */
	public static int send(Group group, ServerLevel level, ParticleOptions particle, double x, double y, double z,
						   int count, double dx, double dy, double dz, double speed, Operation<Integer> original) {
		int scaled = scale(group, count);
		return scaled < 0 ? 0 : original.call(level, particle, x, y, z, scaled, dx, dy, dz, speed);
	}

	public static long sent(Group group) {
		return SENT.get(group.ordinal());
	}

	public static long dropped(Group group) {
		return DROPPED.get(group.ordinal());
	}

	private static @Nullable Double number(JsonObject section, String key, @Nullable Double fallback) {
		try {
			return section.has(key) && !section.get(key).isJsonNull() ? section.get(key).getAsDouble() : fallback;
		} catch (RuntimeException e) {
			MythicRpgCompat.LOGGER.warn("{}: {} is not a number, using {}", FILE, key, fallback);
			return fallback;
		}
	}

	/** Reads the config, adding any missing keys with their defaults. */
	public static void load() {
		JsonObject root = new JsonObject();
		if (Files.exists(FILE)) {
			try (Reader reader = Files.newBufferedReader(FILE)) {
				root = GSON.fromJson(reader, JsonObject.class);
			} catch (IOException | RuntimeException e) {
				MythicRpgCompat.LOGGER.warn("Could not read {}, using defaults", FILE, e);
				root = new JsonObject();
			}
		}
		JsonObject section = root.has(SECTION) && root.get(SECTION).isJsonObject() ? root.getAsJsonObject(SECTION) : new JsonObject();
		section.addProperty("_comment", "Amount of Mythic Upgrades gem effect particles: 1 = Mythic's default, 0 = none, 0.25 = a quarter. "
				+ "Set override to a number to use it for every gem instead of the per-gem values. Server-side.");
		Double overrideValue = number(section, "override", null);
		if (!section.has("override")) {
			section.add("override", null);
		}
		Map<Group, Double> factors = new EnumMap<>(Group.class);
		for (Group group : Group.values()) {
			if (!section.has(group.key())) {
				section.addProperty(group.key(), 1.0);
			}
			factors.put(group, number(section, group.key(), 1.0));
		}
		set(overrideValue, factors);
		root.add(SECTION, section);
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE)) {
				GSON.toJson(root, writer);
			}
		} catch (IOException e) {
			MythicRpgCompat.LOGGER.warn("Could not write {}", FILE, e);
		}
	}
}
