package dev.jensh.mythicrpgcompat.client.recolor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/** A netherite texture to recolour (e.g. {@code archers:item/netherite_longbow}) and its pre-netherite twin. */
public record RecolorEntry(Identifier texture, Optional<Identifier> twin) {
	public static final Codec<RecolorEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
			Identifier.CODEC.fieldOf("texture").forGetter(RecolorEntry::texture),
			Identifier.CODEC.optionalFieldOf("twin").forGetter(RecolorEntry::twin)
	).apply(i, RecolorEntry::new));

	/** Path of the recoloured copy: {@code mythic_rpg_compat:<kind>/<gem>/<ns>/<rest>}. */
	public Identifier recolored(String gem) {
		String path = texture.getPath();
		int slash = path.indexOf('/');
		return dev.jensh.mythicrpgcompat.MythicRpgCompat.id(
				path.substring(0, slash) + "/" + gem + "/" + texture.getNamespace() + path.substring(slash));
	}

	public static Identifier file(Identifier texture) {
		return texture.withPath(p -> "textures/" + p + ".png");
	}
}
