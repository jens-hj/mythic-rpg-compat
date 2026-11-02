package dev.jensh.mythicrpgcompat.test;

import dev.jensh.mythicrpgcompat.Gem;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;

/**
 * Renders one gallery picture per armor set: every gem variant on armor stands, plus plain Netherite.
 * Only runs with MYTHIC_RPG_GALLERY=1 ./gradlew runClientGameTest; pictures go to build/run/clientGameTest/gallery.
 */
@SuppressWarnings("UnstableApiUsage")
public class GalleryClientGameTest implements FabricClientGameTest {
	static final boolean ENABLED = System.getenv("MYTHIC_RPG_GALLERY") != null;

	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final String[] RPG_SLOTS = {"head", "chest", "legs", "feet"};
	private static final String TAG = "mythic_rpg_gallery";

	/** Armor set, plus what the stands hold (null for nothing). */
	private record Kit(String mod, String armor, String mainHand, String offHand) {
	}

	private static final List<Kit> SETS = List.of(
			new Kit("archers", "netherite_ranger_armor", "netherite_longbow", null),
			new Kit("paladins", "netherite_crusader_armor", "netherite_claymore", "netherite_kite_shield"),
			new Kit("paladins", "netherite_prior_robe", "netherite_holy_staff", null),
			new Kit("wizards", "netherite_arcane_robe", "staff_netherite_arcane", null),
			new Kit("wizards", "netherite_fire_robe", "staff_netherite_fire", null),
			new Kit("wizards", "netherite_frost_robe", "staff_netherite_frost", null),
			new Kit("rogues", "netherite_assassin_armor", "netherite_dagger", "netherite_dagger"),
			new Kit("rogues", "netherite_berserker_armor", "netherite_double_axe", null));

	/** Back row (raised): x offsets, Netherite reference in the middle. Front row sits in the gaps. */
	private static final int[] BACK = {-4, -2, 0, 2, 4};
	private static final int[] FRONT = {-3, -1, 1, 3};

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
			buildStage(sp, o);

			context.runOnClient(mc -> {
				mc.options.fov().set(50);
				mc.options.bobView().set(false);
			});
			context.getInput().pressKey(options -> options.keyToggleGui);
			sp.getServer().runCommand(String.format("tp @a %.2f %.2f %.2f 180 11", o.getX() + 0.5, o.getY() + 4.5 - 1.62, o.getZ() + 7.0));
			context.waitTicks(20);
			sp.getConnection().waitForChunksRender();

			Path out = Path.of("gallery");
			for (Kit kit : SETS) {
				sp.getServer().runOnServer(server -> spawnStands(server, o, kit));
				context.waitTicks(40);
				context.takeScreenshot(TestScreenshotOptions.of(kit.armor())
						.disableCounterPrefix()
						.withSize(2560, 1440)
						.withDestinationDir(out));
			}
		}
	}

	private static void buildStage(TestSingleplayerContext sp, BlockPos o) {
		int x = o.getX(), y = o.getY(), z = o.getZ();
		run(sp, "fill %d %d %d %d %d %d minecraft:polished_blackstone", x - 8, y - 1, z - 5, x + 8, y - 1, z + 3);
		run(sp, "fill %d %d %d %d %d %d minecraft:polished_blackstone_bricks", x - 8, y - 1, z - 5, x + 8, y - 1, z - 5);
		Gem[] gems = Gem.values();
		for (int i = 0; i < BACK.length; i++) {
			String top = BACK[i] == 0 ? "minecraft:netherite_block" : "mythicupgrades:" + backGem(gems, i).getSerializedName() + "_block";
			run(sp, "fill %d %d %d %d %d %d minecraft:polished_blackstone_bricks", x + BACK[i], y, z - 3, x + BACK[i], y + 1, z - 3);
			run(sp, "setblock %d %d %d %s", x + BACK[i], y + 2, z - 3, top);
		}
		for (int i = 0; i < FRONT.length; i++) {
			run(sp, "setblock %d %d %d mythicupgrades:%s_block", x + FRONT[i], y, z, gems[4 + i].getSerializedName());
		}
	}

	/** Gems 0..3 go in the back row around the Netherite reference, 4..7 in the front row. */
	private static Gem backGem(Gem[] gems, int backIndex) {
		return gems[backIndex < 2 ? backIndex : backIndex - 1];
	}

	private static void spawnStands(MinecraftServer server, BlockPos o, Kit kit) {
		ServerLevel level = player(server).level();
		level.getEntities(net.minecraft.world.entity.EntityTypes.ARMOR_STAND, e -> e.entityTags().contains(TAG)).forEach(e -> e.discard());
		Gem[] gems = Gem.values();
		for (int i = 0; i < BACK.length; i++) {
			Gem gem = BACK[i] == 0 ? null : backGem(gems, i);
			spawn(server, level, kit, gem, o.getX() + BACK[i] + 0.5, o.getY() + 3, o.getZ() - 3 + 0.5, o);
		}
		for (int i = 0; i < FRONT.length; i++) {
			spawn(server, level, kit, gems[4 + i], o.getX() + FRONT[i] + 0.5, o.getY() + 1, o.getZ() + 0.5, o);
		}
	}

	private static void spawn(MinecraftServer server, ServerLevel level, Kit kit, Gem gem, double x, double y, double z, BlockPos o) {
		ArmorStand stand = new ArmorStand(level, x, y, z);
		double dx = (o.getX() + 0.5) - x;
		double dz = (o.getZ() + 7.0) - z;
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		stand.setYRot(yaw);
		stand.setYHeadRot(yaw);
		stand.setYBodyRot(yaw);
		stand.setNoGravity(true);
		stand.setShowArms(true);
		stand.setNoBasePlate(true);
		stand.addTag(TAG);
		for (int i = 0; i < 4; i++) {
			stand.setItemSlot(ARMOR[i], item(server, kit.mod(), kit.armor() + "_" + RPG_SLOTS[i], gem));
		}
		stand.setItemSlot(EquipmentSlot.MAINHAND, item(server, kit.mod(), kit.mainHand(), gem));
		if (kit.offHand() != null) {
			stand.setItemSlot(EquipmentSlot.OFFHAND, item(server, kit.mod(), kit.offHand(), gem));
		}
		level.addFreshEntity(stand);
	}

	private static ItemStack item(MinecraftServer server, String mod, String id, Gem gem) {
		return gem == null ? InfusionClientGameTest.rpg(mod, id) : InfusionClientGameTest.infuse(server, mod, id, gem, null);
	}

	private static net.minecraft.server.level.ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void run(TestSingleplayerContext sp, String fmt, Object... args) {
		sp.getServer().runCommand(String.format(fmt, args));
	}
}
