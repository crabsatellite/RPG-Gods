/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.deity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import rpggods.RGRegistry;
import rpggods.util.altar.AltarItems;
import rpggods.util.altar.AltarPose;

import java.util.Map;
import java.util.Optional;

public class Altar {

    public static final ResourceLocation MATERIAL = new ResourceLocation("stone");

    @Deprecated
    public static final Altar EMPTY = new Altar(true, Optional.empty(), false, 0,
            AltarItems.EMPTY, MATERIAL, AltarPose.EMPTY, false);

    public static final Codec<Altar> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(Altar::isEnabled),
            Codec.STRING.optionalFieldOf("name").forGetter(Altar::getName),
            Codec.BOOL.optionalFieldOf("slim", false).forGetter(Altar::isSlim),
            Codec.INT.optionalFieldOf("light", 0).forGetter(Altar::getLightLevel),
            AltarItems.CODEC.optionalFieldOf("items", AltarItems.EMPTY).forGetter(Altar::getItems),
            ResourceLocation.CODEC.optionalFieldOf("material", MATERIAL).forGetter(Altar::getMaterial),
            AltarPose.CODEC.optionalFieldOf("pose", AltarPose.WALKING).forGetter(Altar::getPose),
            Codec.BOOL.optionalFieldOf("pose_locked", false).forGetter(Altar::isPoseLocked)
    ).apply(instance, Altar::new));

    private final boolean enabled;
    private final Optional<String> name;
    private final boolean slim;
    private final int lightLevel;
    private final AltarItems items;
    private final ResourceLocation material;
    private final AltarPose pose;
    private final boolean poseLocked;

    private final Optional<ResourceLocation> deity;

    //// CONSTRUCTOR ////

    public Altar(boolean enabled, Optional<String> name, boolean slim,
                 int lightLevel, AltarItems items, ResourceLocation material,
                 AltarPose pose, boolean poseLocked) {
        this.enabled = enabled;
        this.name = name;
        this.slim = slim;
        this.lightLevel = Math.max(0, Math.min(15, lightLevel));
        this.items = items;
        this.material = material;
        this.pose = pose;
        this.poseLocked = poseLocked;

        if(name.isPresent() && !name.get().isEmpty()) {
            this.deity = Optional.ofNullable(ResourceLocation.tryParse(name.get()));
        } else {
            this.deity = Optional.empty();
        }
    }

    //// HELPER METHODS ////

    /**
     * @param entry the registry entry
     * @param deityId the deity ID
     * @return true if the given registry entry should be associated with the given deity
     */
    public static boolean isFor(final Map.Entry<ResourceKey<Altar>, Altar> entry, final ResourceLocation deityId) {
        final Altar altar = entry.getValue();
        final Optional<ResourceLocation> oDeity = altar.getDeity();
        return altar.isEnabled() && oDeity.isPresent() && deityId.equals(oDeity.get());
    }

    /**
     * @param registryAccess the registry access
     * @return the {@link Altar} registry
     */
    public static Registry<Altar> getRegistry(final RegistryAccess registryAccess) {
        return registryAccess.registryOrThrow(RGRegistry.Keys.ALTARS);
    }

    //// GETTERS ////

    public boolean isEnabled() {
        return enabled;
    }

    public Optional<ResourceLocation> getDeity() {
        return deity;
    }

    public Optional<String> getName() { return name; }

    public boolean isSlim() {
        return slim;
    }

    /**
     * @return true if this altar uses a slim (female) model
     */
    public boolean isFemale() {
        return slim;
    }

    public int getLightLevel() {
        return lightLevel;
    }

    public AltarItems getItems() {
        return items;
    }

    public ResourceLocation getMaterial() {
        return material;
    }

    public AltarPose getPose() {
        return pose;
    }

    public boolean isPoseLocked() {
        return poseLocked;
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder("Deity:");
        b.append(" enabled[").append(enabled).append("]");
        b.append(" items[").append(items.toString()).append("]");
        b.append(" slim[").append(slim).append("]");
        b.append(" pose_locked[").append(poseLocked).append("]");
        return b.toString();
    }

    @Deprecated
    public static String createTranslationKey(final ResourceLocation deity) {
        return "deity." + deity.getNamespace() + "." + deity.getPath();
    }
}
