package dev.jensh.mythicrpgcompat.client.recolor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * A texture to recolour (e.g. {@code archers:item/netherite_longbow}) and the twin used to mask it.
 *
 * <p>{@code mode} "grey" (default): netherite textures, recolour grey netherite pixels that differ from
 * the pre-netherite twin. "diff": Ruby/Aeternium/Crystal textures, recolour every pixel that differs
 * from the netherite twin. {@code forge}: also build forge variants (class armor only).
 */
public record RecolorEntry(Identifier texture, Optional<Identifier> twin, String mode, boolean forge) {
	public static final Codec<RecolorEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
			Identifier.CODEC.fieldOf("texture").forGetter(RecolorEntry::texture),
			Identifier.CODEC.optionalFieldOf("twin").forGetter(RecolorEntry::twin),
			Codec.STRING.optionalFieldOf("mode", "grey").forGetter(RecolorEntry::mode),
			Codec.BOOL.optionalFieldOf("forge", false).forGetter(RecolorEntry::forge)
	).apply(i, RecolorEntry::new));

	public boolean diffMode() {
		return "diff".equals(mode);
	}

	public boolean accepts(RecolorVariant variant) {
		return forge || !variant.forge();
	}

	/** Path of the recoloured copy: {@code mythic_rpg_compat:<kind>/<variant>/<ns>/<rest>}. */
	public Identifier recolored(String variant) {
		String path = texture.getPath();
		int slash = path.indexOf('/');
		return dev.jensh.mythicrpgcompat.MythicRpgCompat.id(
				path.substring(0, slash) + "/" + variant + "/" + texture.getNamespace() + path.substring(slash));
	}

	public static Identifier file(Identifier texture) {
		return texture.withPath(p -> "textures/" + p + ".png");
	}
}
