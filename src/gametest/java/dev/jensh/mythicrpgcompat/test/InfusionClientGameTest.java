package dev.jensh.mythicrpgcompat.test;

import dev.jensh.mythicrpgcompat.Gem;
import dev.jensh.mythicrpgcompat.ModComponents;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;

/** End-to-end checks: recipes, stats, Mythic effects, textures. Run with ./gradlew runClientGameTest. */
@SuppressWarnings("UnstableApiUsage")
public class InfusionClientGameTest implements FabricClientGameTest {
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final String[] RPG_SLOTS = {"head", "chest", "legs", "feet"};
	private static final String[] MYTHIC_PIECES = {"helmet", "chestplate", "leggings", "boots"};

	private final List<String> failures = new ArrayList<>();

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getConnection().waitForChunksRender();
			sp.getServer().runCommand("time set noon");
			sp.getServer().runCommand("weather clear");
			sp.getServer().runCommand("gamerule spawn_mobs false");

			sp.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();

				// Recipes produce infused stacks, and re-gemming replaces the gem.
				ItemStack ruby = infuse(server, "archers", "netherite_ranger_armor_chest", Gem.RUBY, null);
				check(ruby.get(ModComponents.INFUSION) == Gem.RUBY, "ruby infusion component");
				check(String.valueOf(ruby.get(DataComponents.ITEM_MODEL)).equals("mythic_rpg_compat:ruby/archers/netherite_ranger_armor_chest"), "item_model, was " + ruby.get(DataComponents.ITEM_MODEL));
				ItemStack topaz = infuse(server, "archers", "netherite_ranger_armor_chest", Gem.TOPAZ, ruby);
				check(topaz.get(ModComponents.INFUSION) == Gem.TOPAZ, "re-gem to topaz");

				// Weapon on-hit: infused dagger behaves like the gem's sword.
				for (Gem gem : Gem.values()) {
					Map<String, Integer> real = hitAndRecord(level, player, new ItemStack(gem.mythicItem("sword")));
					Map<String, Integer> infused = hitAndRecord(level, player, infuse(server, "rogues", "netherite_dagger", gem, null));
					log("%s weapon on-hit target effects: mythic=%s infused=%s", gem, real, infused);
					check(real.keySet().equals(infused.keySet()), gem + " weapon on-hit effects match");
				}
				player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			});

			// Stats: +1 toughness, class attributes x1.25, vanilla armor unchanged.
			equip(sp, (server, i) -> i == 1 ? rpg("archers", "netherite_ranger_armor_chest") : ItemStack.EMPTY);
			context.waitTicks(5);
			double[] plain = sp.getServer().computeOnServer(server -> stats(player(server)));
			equip(sp, (server, i) -> i == 1 ? infuse(server, "archers", "netherite_ranger_armor_chest", Gem.RUBY, null) : ItemStack.EMPTY);
			context.waitTicks(5);
			double[] infused = sp.getServer().computeOnServer(server -> stats(player(server)));
			log("armor/toughness plain=%s infused=%s", java.util.Arrays.toString(plain), java.util.Arrays.toString(infused));
			check(infused[0] == plain[0] && infused[1] == plain[1] + 1, "infused chest: same armor, +1 toughness");
			sp.getServer().runOnServer(server -> {
				Map<String, Double> plainMods = modifierSums(rpg("archers", "netherite_ranger_armor_chest"));
				Map<String, Double> infusedMods = modifierSums(infuse(server, "archers", "netherite_ranger_armor_chest", Gem.RUBY, null));
				log("chest modifiers plain=%s infused=%s", plainMods, infusedMods);
				String ranged = "ranged_weapon:damage";
				check(plainMods.containsKey(ranged) && Math.abs(infusedMods.get(ranged) - plainMods.get(ranged) * 1.25) < 1e-6, "ranged damage x1.25");
			});

			// Jewelry: plain piece takes 1 Socket Mount, infused piece takes 2.
			sp.getServer().runOnServer(server -> {
				ItemStack mountItem = rpg("jewelry", "socket_mount");
				int plainMounts = mountsAccepted(rpg("archers", "netherite_ranger_armor_chest"), mountItem);
				int infusedMounts = mountsAccepted(infuse(server, "archers", "netherite_ranger_armor_chest", Gem.RUBY, null), mountItem);
				ItemStack fresh = infuse(server, "archers", "netherite_ranger_armor_chest", Gem.RUBY, null);
				log("socket mounts accepted plain=%d infused=%d, sockets right after infusion=%d", plainMounts, infusedMounts, sockets(fresh));
				check(plainMounts >= 1 && infusedMounts == plainMounts + 1, "infused gear accepts one extra socket mount");
				check(sockets(fresh) == sockets(rpg("archers", "netherite_ranger_armor_chest")), "infusion itself adds no socket");
			});

			// Gear that 0.2.0 auto-socketed gets that empty socket removed again.
			int legacyBase = sp.getServer().computeOnServer(server -> {
				ItemStack legacy = rpg("archers", "netherite_ranger_armor_legs");
				legacy.set(ModComponents.INFUSION, Gem.JADE);
				legacy.set(ModComponents.PERKS_GRANTED, net.minecraft.util.Unit.INSTANCE);
				var current = legacy.get(net.jewelry.gems.GemComponents.SOCKETS);
				var list = new ArrayList<>(current == null ? List.<net.jewelry.gems.SocketsComponent.Socket>of() : current.sockets());
				list.add(net.jewelry.gems.SocketsComponent.Socket.EMPTY);
				legacy.set(net.jewelry.gems.GemComponents.SOCKETS, new net.jewelry.gems.SocketsComponent(list));
				player(server).getInventory().setItem(20, legacy);
				return sockets(rpg("archers", "netherite_ranger_armor_legs"));
			});
			context.waitTicks(10);
			int[] legacyAfter = sp.getServer().computeOnServer(server -> {
				ItemStack s1 = player(server).getInventory().getItem(20);
				return new int[]{sockets(s1), s1.has(ModComponents.PERKS_GRANTED) ? 1 : 0};
			});
			log("legacy sockets base=%d after=%d marker=%d", legacyBase, legacyAfter[0], legacyAfter[1]);
			check(legacyAfter[0] == legacyBase && legacyAfter[1] == 0, "0.2.0 auto-socket reverted");

			// Armor set effects: infused ranger set == real Mythic set of the same gem.
			for (Gem gem : new Gem[]{Gem.RUBY, Gem.SAPPHIRE, Gem.JADE, Gem.TOPAZ}) {
				Map<String, Integer> real = wearAndRecord(context, sp, (server, i) -> new ItemStack(gem.mythicItem(MYTHIC_PIECES[i])));
				Map<String, Integer> inf = wearAndRecord(context, sp, (server, i) -> infuse(server, "archers", "netherite_ranger_armor_" + RPG_SLOTS[i], gem, null));
				log("%s armor effects: mythic=%s infused=%s", gem, real, inf);
				check(!real.isEmpty() && real.equals(inf), gem + " armor effects match");
			}
			// Mixed set: two infused pieces count as two levels.
			Map<String, Integer> half = wearAndRecord(context, sp, (server, i) -> i < 2
					? infuse(server, "archers", "netherite_ranger_armor_" + RPG_SLOTS[i], Gem.RUBY, null)
					: rpg("archers", "netherite_ranger_armor_" + RPG_SLOTS[i]));
			Map<String, Integer> halfReal = wearAndRecord(context, sp, (server, i) -> i < 2 ? new ItemStack(Gem.RUBY.mythicItem(MYTHIC_PIECES[i])) : ItemStack.EMPTY);
			log("half ruby set: mythic=%s infused=%s", halfReal, half);
			check(half.equals(halfReal), "partial set matches");

			// Client: tooltip line, item icon sprite and worn-armor texture exist.
			context.runOnClient(mc -> {
				ItemStack stack = infuseClientCopy(mc.level.registryAccess(), Gem.RUBY);
				List<Component> lines = stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL);
				log("tooltip: %s", lines.stream().map(Component::getString).toList());
				check(lines.stream().anyMatch(l -> l.getString().contains("Infused: Ruby")), "tooltip shows infusion");

				var atlas = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS);
				for (String id : new String[]{"item/ruby/archers/netherite_ranger_armor_chest", "item/sapphire/rogues/netherite_dagger", "item/topaz/archers/netherite_longbow_pulling_2"}) {
					Identifier sprite = Identifier.fromNamespaceAndPath("mythic_rpg_compat", id);
					check(atlas.getSprite(sprite) != atlas.missingSprite(), "sprite " + sprite);
				}
				Identifier armorTex = Identifier.fromNamespaceAndPath("mythic_rpg_compat", "textures/armor/ruby/archers/netherite_ranger_armor.png");
				check(mc.getTextureManager().getTexture(armorTex) instanceof DynamicTexture, "armor texture " + armorTex);
			});

			// Screenshots for visual review.
			for (Gem gem : new Gem[]{Gem.RUBY, Gem.SAPPHIRE, Gem.CITRINE}) {
				sp.getServer().runOnServer(server -> {
					ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
					for (int i = 0; i < 4; i++) {
						player.setItemSlot(ARMOR[i], infuse(server, "archers", "netherite_ranger_armor_" + RPG_SLOTS[i], gem, null));
					}
					player.setItemSlot(EquipmentSlot.MAINHAND, infuse(server, "paladins", "netherite_claymore", gem, null));
					player.setItemSlot(EquipmentSlot.OFFHAND, infuse(server, "rogues", "netherite_dagger", gem, null));
					player.setYRot(0);
				});
				context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
				context.waitTicks(20);
				context.takeScreenshot("ranger_" + gem.getSerializedName());
			}
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			sp.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				int slot = 9;
				for (Gem gem : Gem.values()) {
					player.getInventory().setItem(slot++, infuse(server, "paladins", "netherite_crusader_armor_chest", gem, null));
					player.getInventory().setItem(slot++, infuse(server, "archers", "netherite_heavy_crossbow", gem, null));
					player.getInventory().setItem(slot++, infuse(server, "wizards", "staff_netherite_fire", gem, null));
				}
			});
			context.setScreen(() -> new InventoryScreen(context.computeOnClient(mc -> mc.player)));
			context.waitTicks(10);
			context.takeScreenshot("inventory");
			context.setScreen(() -> null);
		}

		if (!failures.isEmpty()) {
			throw new AssertionError("Failed checks:\n - " + String.join("\n - ", failures));
		}
		log("all checks passed");
	}

	private static ItemStack rpg(String mod, String item) {
		return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(mod, item)));
	}

	private static ItemStack infuse(net.minecraft.server.MinecraftServer server, String mod, String item, Gem gem, ItemStack base) {
		var key = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("mythic_rpg_compat",
				"infuse/" + mod + "/" + item + "_" + gem.getSerializedName()));
		Recipe<?> recipe = server.getRecipeManager().byKey(key).orElseThrow(() -> new AssertionError("missing recipe " + key)).value();
		var input = new SmithingRecipeInput(
				rpg("mythicupgrades", "mythic_upgrade_smithing_template"),
				base == null ? rpg(mod, item) : base,
				rpg("mythicupgrades", gem.getSerializedName() + "_ingot"));
		SmithingRecipe smithing = (SmithingRecipe) recipe;
		if (!smithing.matches(input, null)) {
			throw new AssertionError("recipe does not match " + key);
		}
		return smithing.assemble(input);
	}

	private static ItemStack infuseClientCopy(net.minecraft.core.HolderLookup.Provider registries, Gem gem) {
		ItemStack stack = rpg("archers", "netherite_ranger_armor_chest");
		stack.set(ModComponents.INFUSION, gem);
		return stack;
	}

	private interface Piece {
		ItemStack get(net.minecraft.server.MinecraftServer server, int index);
	}

	private static ServerPlayer player(net.minecraft.server.MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void equip(TestSingleplayerContext sp, Piece piece) {
		sp.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.removeAllEffects();
			for (int i = 0; i < 4; i++) {
				player.setItemSlot(ARMOR[i], piece.get(server, i));
			}
		});
	}

	private static double[] stats(ServerPlayer player) {
		return new double[]{player.getAttributeValue(Attributes.ARMOR), player.getAttributeValue(Attributes.ARMOR_TOUGHNESS)};
	}

	private static Map<String, Double> modifierSums(ItemStack stack) {
		Map<String, Double> sums = new TreeMap<>();
		stack.forEachModifier(EquipmentSlot.CHEST, (attribute, modifier) -> sums.merge(attribute.getRegisteredName(), modifier.amount(), Double::sum));
		return sums;
	}

	private static int mountsAccepted(ItemStack target, ItemStack mountItem) {
		int n = 0;
		while (n < 10 && net.jewelry.gems.GemSocketing.canMount(target, mountItem, false)) {
			target = net.jewelry.gems.GemSocketing.mount(target, mountItem, false);
			n++;
		}
		return n;
	}

	private static int sockets(ItemStack stack) {
		var sockets = stack.get(net.jewelry.gems.GemComponents.SOCKETS);
		return sockets == null ? 0 : sockets.count();
	}

	private static Map<String, Integer> wearAndRecord(ClientGameTestContext context, TestSingleplayerContext sp, Piece piece) {
		equip(sp, piece);
		context.waitTicks(40);
		Map<String, Integer> effects = sp.getServer().computeOnServer(server -> effects(player(server)));
		equip(sp, (server, i) -> ItemStack.EMPTY);
		context.waitTicks(5);
		return effects;
	}

	private static Map<String, Integer> hitAndRecord(ServerLevel level, ServerPlayer player, ItemStack weapon) {
		player.setItemSlot(EquipmentSlot.MAINHAND, weapon);
		Map<String, Integer> effects = new TreeMap<>();
		// Fresh target per hit (avoids hurt cooldown); several hits so chance-based procs show up.
		for (int i = 0; i < 20; i++) {
			var zombie = EntityTypes.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			zombie.setPos(player.getX() + 2, player.getY(), player.getZ());
			zombie.setNoAi(true);
			level.addFreshEntity(zombie);
			zombie.hurtServer(level, player.damageSources().playerAttack(player), 1.0F);
			effects.putAll(effects(zombie));
			zombie.discard();
		}
		return effects;
	}

	private static Map<String, Integer> effects(LivingEntity entity) {
		Map<String, Integer> map = new TreeMap<>();
		for (MobEffectInstance e : entity.getActiveEffects()) {
			map.put(e.getEffect().getRegisteredName(), e.getAmplifier());
		}
		return map;
	}

	private void check(boolean ok, String what) {
		log("%s %s", ok ? "PASS" : "FAIL", what);
		if (!ok) {
			failures.add(what);
		}
	}

	private static void log(String fmt, Object... args) {
		System.out.println("[mythic_rpg_compat test] " + String.format(fmt, args));
	}
}
