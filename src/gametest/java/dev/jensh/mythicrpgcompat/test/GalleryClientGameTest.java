package dev.jensh.mythicrpgcompat.test;

import dev.jensh.mythicrpgcompat.Forge;
import dev.jensh.mythicrpgcompat.Gem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the Modrinth gallery:
 * <ul>
 *   <li>one picture per class armor set: every gem variant on armor stands, plus plain Netherite</li>
 *   <li>one picture per forge tier: every class set forged, around the tier's own Better Nether/End armor</li>
 *   <li>a wall of Ruby/Aeternium/Crystal RPG weapons infused with each gem</li>
 * </ul>
 * Only runs with MYTHIC_RPG_GALLERY=1 ./gradlew runClientGameTest; pictures go to build/run/clientGameTest/gallery.
 */
@SuppressWarnings("UnstableApiUsage")
public class GalleryClientGameTest implements FabricClientGameTest {
	static final boolean ENABLED = System.getenv("MYTHIC_RPG_GALLERY") != null;

	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final String[] RPG_SLOTS = {"head", "chest", "legs", "feet"};
	private static final String[] VANILLA_PIECES = {"helmet", "chestplate", "leggings", "boots"};
	private static final String TAG = "mythic_rpg_gallery";

	/** Armor set, what the stands hold (null for nothing), and what they hold when forged to Ruby / Aeternium. */
	private record Kit(String mod, String armor, String mainHand, String offHand, String[] ruby, String[] aeternium) {
	}

	private static String[] hands(String main, String off) {
		return new String[]{main, off};
	}

	private static final List<Kit> SETS = List.of(
			new Kit("archers", "netherite_ranger_armor", "netherite_longbow", null,
					hands("ruby_heavy_crossbow", null), hands("crystal_longbow", null)),
			new Kit("paladins", "netherite_crusader_armor", "netherite_claymore", "netherite_kite_shield",
					hands("ruby_claymore", "ruby_kite_shield"), hands("aeternium_claymore", "aeternium_kite_shield")),
			new Kit("paladins", "netherite_prior_robe", "netherite_holy_staff", null,
					hands("ruby_holy_staff", null), hands("aeternium_mace", null)),
			new Kit("wizards", "netherite_arcane_robe", "staff_netherite_arcane", null,
					hands("staff_netherite_arcane", null), hands("staff_crystal_arcane", null)),
			new Kit("wizards", "netherite_fire_robe", "staff_netherite_fire", null,
					hands("staff_ruby_fire", null), hands("staff_netherite_fire", null)),
			new Kit("wizards", "netherite_frost_robe", "staff_netherite_frost", null,
					hands("staff_netherite_frost", null), hands("staff_netherite_frost", null)),
			new Kit("rogues", "netherite_assassin_armor", "netherite_dagger", "netherite_dagger",
					hands("ruby_dagger", "ruby_dagger"), hands("aeternium_dagger", "aeternium_dagger")),
			new Kit("rogues", "netherite_berserker_armor", "netherite_double_axe", null,
					hands("ruby_double_axe", null), hands("aeternium_double_axe", null)));

	/** Rows of the weapon wall: RPG Ruby/Aeternium/Crystal weapons, one column per gem. */
	private static final String[][] WALL = {
			{"paladins", "ruby_claymore"}, {"rogues", "aeternium_glaive"}, {"archers", "crystal_longbow"}, {"wizards", "staff_ruby_fire"}};

	/** Back row (raised) x offsets, middle one is the reference piece. Front row sits in the gaps. */
	private static final int[] BACK = {-4, -2, 0, 2, 4};
	private static final int[] FRONT = {-3, -1, 1, 3};
	/** The weapon wall is built off to the side so the stage stays out of view. */
	private static final int WALL_OFFSET = 40;

	/** What one stand wears, and the pedestal block it stands on. */
	private interface Dress {
		void apply(MinecraftServer server, ArmorStand stand);
	}

	private record Spot(String pedestal, Dress dress) {
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ENABLED) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getConnection().waitForChunksRender();
			for (String cmd : new String[]{"time set 6000", "gamerule advance_time false", "weather clear",
					"gamerule advance_weather false", "gamerule spawn_mobs false", "gamemode spectator @a"}) {
				sp.getServer().runCommand(cmd);
			}
			BlockPos o = sp.getServer().computeOnServer(server -> player(server).blockPosition());
			run(sp, "fill %d %d %d %d %d %d minecraft:polished_blackstone", o.getX() - 8, o.getY() - 1, o.getZ() - 5, o.getX() + 8, o.getY() - 1, o.getZ() + 3);
			run(sp, "fill %d %d %d %d %d %d minecraft:polished_blackstone_bricks", o.getX() - 8, o.getY() - 1, o.getZ() - 5, o.getX() + 8, o.getY() - 1, o.getZ() - 5);

			context.runOnClient(mc -> {
				mc.options.fov().set(50);
				mc.options.bobView().set(false);
			});
			context.getInput().pressKey(options -> options.keyToggleGui);
			Path out = Path.of("gallery");

			// Gem variants of each class set.
			tp(sp, o.getX() + 0.5, o.getY() + 4.5, o.getZ() + 7.0, 11);
			Gem[] gems = Gem.values();
			for (Kit kit : SETS) {
				List<Spot> spots = new ArrayList<>();
				for (int i = 0; i < 9; i++) {
					Gem gem = gemAt(gems, i);
					spots.add(new Spot(gem == null ? "minecraft:netherite_block" : "mythicupgrades:" + gem.getSerializedName() + "_block",
							(server, stand) -> dress(server, stand, kit, gem, null, kit.mainHand(), kit.offHand())));
				}
				stage(context, sp, o, spots);
				shoot(context, out, kit.armor());
			}

			// Forge tiers: all class sets, around the tier's own armor.
			for (Forge forge : Forge.values()) {
				boolean ruby = forge == Forge.RUBY;
				String block = ruby ? "betternether:nether_ruby_block" : "betterend:aeternium_block";
				String mod = ruby ? "betternether" : "betterend";
				String material = ruby ? "flaming_ruby" : "aeternium";
				List<Spot> spots = new ArrayList<>();
				int kitIndex = 0;
				for (int i = 0; i < 9; i++) {
					if (i == 2) {
						spots.add(new Spot(block, (server, stand) -> {
							for (int s = 0; s < 4; s++) {
								stand.setItemSlot(ARMOR[s], InfusionClientGameTest.rpg(mod, material + "_" + VANILLA_PIECES[s]));
							}
							stand.setItemSlot(EquipmentSlot.MAINHAND, InfusionClientGameTest.rpg(mod, material + "_sword"));
						}));
						continue;
					}
					Kit kit = SETS.get(kitIndex++);
					String[] held = ruby ? kit.ruby() : kit.aeternium();
					spots.add(new Spot(block, (server, stand) -> dress(server, stand, kit, null, forge, held[0], held[1])));
				}
				stage(context, sp, o, spots);
				shoot(context, out, "forge_" + forge.getSerializedName());
			}

			// Weapon wall.
			clearStands(sp);
			BlockPos w = o.offset(WALL_OFFSET, 0, 0);
			buildWall(sp, w);
			tp(sp, w.getX(), w.getY() + 3.5, w.getZ() + 5.0, 0);
			context.waitTicks(40);
			sp.getConnection().waitForChunksRender();
			shoot(context, out, "weapons");
		}
	}

	/** Spot index 0..4 = back row (2 = middle, the reference), 5..8 = front row. Gems 0..3 back, 4..7 front. */
	private static Gem gemAt(Gem[] gems, int spot) {
		if (spot == 2) {
			return null;
		}
		return gems[spot < 2 ? spot : spot - 1];
	}

	private static void stage(ClientGameTestContext context, TestSingleplayerContext sp, BlockPos o, List<Spot> spots) {
		int x = o.getX(), y = o.getY(), z = o.getZ();
		for (int i = 0; i < BACK.length; i++) {
			run(sp, "fill %d %d %d %d %d %d minecraft:polished_blackstone_bricks", x + BACK[i], y, z - 3, x + BACK[i], y + 1, z - 3);
			run(sp, "setblock %d %d %d %s", x + BACK[i], y + 2, z - 3, spots.get(i).pedestal());
		}
		for (int i = 0; i < FRONT.length; i++) {
			run(sp, "setblock %d %d %d %s", x + FRONT[i], y, z, spots.get(5 + i).pedestal());
		}
		clearStands(sp);
		sp.getServer().runOnServer(server -> {
			ServerLevel level = player(server).level();
			for (int i = 0; i < 9; i++) {
				boolean back = i < 5;
				double sx = x + (back ? BACK[i] : FRONT[i - 5]) + 0.5;
				double sy = y + (back ? 3 : 1);
				double sz = z + (back ? -3 : 0) + 0.5;
				ArmorStand stand = new ArmorStand(level, sx, sy, sz);
				float yaw = (float) Math.toDegrees(Math.atan2(-((x + 0.5) - sx), (z + 7.0) - sz));
				stand.setYRot(yaw);
				stand.setYHeadRot(yaw);
				stand.setYBodyRot(yaw);
				stand.setNoGravity(true);
				stand.setShowArms(true);
				stand.setNoBasePlate(true);
				stand.addTag(TAG);
				spots.get(i).dress().apply(server, stand);
				level.addFreshEntity(stand);
			}
		});
		context.waitTicks(40);
	}

	private static void dress(MinecraftServer server, ArmorStand stand, Kit kit, Gem gem, Forge forge, String main, String off) {
		for (int s = 0; s < 4; s++) {
			stand.setItemSlot(ARMOR[s], item(server, kit.mod(), kit.armor() + "_" + RPG_SLOTS[s], gem, forge));
		}
		// Weapons only take gems; forged stands hold the RPG Series' own Ruby/Aeternium weapon.
		stand.setItemSlot(EquipmentSlot.MAINHAND, item(server, kit.mod(), main, gem, null));
		if (off != null) {
			stand.setItemSlot(EquipmentSlot.OFFHAND, item(server, kit.mod(), off, gem, null));
		}
	}

	private static ItemStack item(MinecraftServer server, String mod, String id, Gem gem, Forge forge) {
		if (forge != null) {
			return InfusionClientGameTest.forge(server, mod, id, forge, null);
		}
		return gem == null ? InfusionClientGameTest.rpg(mod, id) : InfusionClientGameTest.infuse(server, mod, id, gem, null);
	}

	private static void buildWall(TestSingleplayerContext sp, BlockPos w) {
		int x = w.getX(), y = w.getY(), z = w.getZ();
		Gem[] gems = Gem.values();
		run(sp, "fill %d %d %d %d %d %d minecraft:polished_blackstone", x - 6, y - 1, z - 3, x + 5, y - 1, z + 6);
		run(sp, "fill %d %d %d %d %d %d minecraft:polished_blackstone_bricks", x - 6, y, z - 3, x + 5, y + 6, z - 3);
		for (int c = 0; c < gems.length; c++) {
			run(sp, "setblock %d %d %d mythicupgrades:%s_block", x - 4 + c, y + 5, z - 3, gems[c].getSerializedName());
		}
		sp.getServer().runOnServer(server -> {
			ServerLevel level = player(server).level();
			for (int r = 0; r < WALL.length; r++) {
				for (int c = 0; c < gems.length; c++) {
					BlockPos pos = new BlockPos(x - 4 + c, y + 4 - r, z - 2);
					ItemFrame frame = new ItemFrame(level, pos, Direction.SOUTH);
					frame.setItem(InfusionClientGameTest.infuse(server, WALL[r][0], WALL[r][1], gems[c], null), false);
					frame.setInvisible(true);
					frame.addTag(TAG);
					level.addFreshEntity(frame);
				}
			}
		});
	}

	private static void clearStands(TestSingleplayerContext sp) {
		sp.getServer().runOnServer(server -> player(server).level()
				.getEntities(EntityTypes.ARMOR_STAND, e -> e.entityTags().contains(TAG)).forEach(e -> e.discard()));
	}

	private static void tp(TestSingleplayerContext sp, double x, double eyeY, double z, float pitch) {
		run(sp, "tp @a %.2f %.2f %.2f 180 %.1f", x, eyeY - 1.62, z, pitch);
	}

	private static void shoot(ClientGameTestContext context, Path out, String name) {
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withSize(2560, 1440).withDestinationDir(out));
	}

	private static net.minecraft.server.level.ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void run(TestSingleplayerContext sp, String fmt, Object... args) {
		sp.getServer().runCommand(String.format(java.util.Locale.ROOT, fmt, args));
	}
}
