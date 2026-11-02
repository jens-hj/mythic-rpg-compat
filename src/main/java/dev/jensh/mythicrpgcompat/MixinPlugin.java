package dev.jensh.mythicrpgcompat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Skips mixins into optional mods that aren't installed. */
public final class MixinPlugin implements IMixinConfigPlugin {
	private static final Map<String, String> REQUIRED_MOD = Map.of(
			"MythicLivingEntityMixinSquared", "mythicupgrades",
			"MythicParticlesMixin", "mythicupgrades",
			"NecoiumParticlesMixin", "mythicupgrades",
			"GemSocketingMixin", "jewelry");

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		String mod = REQUIRED_MOD.get(mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1));
		return mod == null || FabricLoader.getInstance().isModLoaded(mod);
	}

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
