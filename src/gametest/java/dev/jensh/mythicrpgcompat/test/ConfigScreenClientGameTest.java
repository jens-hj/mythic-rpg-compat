package dev.jensh.mythicrpgcompat.test;

import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.jensh.mythicrpgcompat.MythicParticles;
import dev.jensh.mythicrpgcompat.MythicParticles.Group;
import dev.jensh.mythicrpgcompat.client.config.ModMenuIntegration;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.Screen;

/** Opens the Mod Menu config screen, saves it untouched and checks the config survives as it was. */
public class ConfigScreenClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (GalleryClientGameTest.ENABLED) {
			return;
		}
		// Override on, and a factor above the slider's 200% range that must not be clamped by saving
		MythicParticles.set(0.5, Map.of(Group.JADE, 3.0, Group.RUBY, 0.25));
		MythicParticles.save();

		ModMenuApi api = new ModMenuIntegration();
		context.setScreen(() -> api.getModConfigScreenFactory().create(null));
		context.waitTicks(5);
		String screen = context.computeOnClient(mc -> mc.gui.screen() == null ? "null" : mc.gui.screen().getClass().getName());
		if (!screen.startsWith("me.shedaniel.clothconfig2")) {
			throw new AssertionError("Config screen is not a Cloth Config screen: " + screen);
		}
		context.takeScreenshot("config_screen");
		context.runOnClient(mc -> {
			try {
				Screen current = mc.gui.screen();
				current.getClass().getMethod("saveAll", boolean.class).invoke(current, false);
			} catch (ReflectiveOperationException e) {
				throw new AssertionError("Could not save the config screen", e);
			}
		});
		context.setScreen(() -> null);

		MythicParticles.set(null, Map.of());
		MythicParticles.load();
		String result = "override=" + MythicParticles.override()
				+ " jade=" + MythicParticles.ownFactor(Group.JADE)
				+ " ruby=" + MythicParticles.ownFactor(Group.RUBY)
				+ " topaz=" + MythicParticles.ownFactor(Group.TOPAZ);
		System.out.println("[config-screen] after save: " + result);
		MythicParticles.set(null, Map.of());
		MythicParticles.save();
		if (!result.equals("override=0.5 jade=3.0 ruby=0.25 topaz=1.0")) {
			throw new AssertionError("Config changed by saving the untouched screen: " + result);
		}
	}
}
