package dev.jensh.mythicrpgcompat.client.recolor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;

/**
 * One recolour variant: a Mythic gem ({@code ruby}, palette strip) or a forge tier
 * ({@code forged_aeternium}, palette taken from a material texture of the installed mod).
 */
public record RecolorVariant(String key, Identifier palette, boolean strip, boolean forge) {
	public static final Codec<RecolorVariant> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("key").forGetter(RecolorVariant::key),
			Identifier.CODEC.fieldOf("palette").forGetter(RecolorVariant::palette),
			Codec.BOOL.optionalFieldOf("strip", false).forGetter(RecolorVariant::strip),
			Codec.BOOL.optionalFieldOf("forge", false).forGetter(RecolorVariant::forge)
	).apply(i, RecolorVariant::new));

	/** The palette, or null when the mod that provides it isn't installed. */
	public int @Nullable [] loadPalette(ResourceManager manager) {
		try {
			return strip ? GemRecolor.readPalette(manager, palette) : GemRecolor.paletteFromTexture(manager, palette);
		} catch (IOException e) {
			return null;
		}
	}
}
