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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import rpggods.RGRegistry;
import rpggods.util.RGCodecUtils;

import javax.annotation.concurrent.Immutable;
import java.util.Optional;

@Immutable
public class Deity {

    /** Empty deity used as a fallback **/
    public static final Deity EMPTY = new Deity(
            new ResourceLocation("rpggods", "empty"),
            ItemStack.EMPTY,
            Gender.OTHER,
            false,
            false,
            0,
            0
    );

    public static final Codec<Deity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("name").forGetter(Deity::getId),
            RGCodecUtils.ITEM_OR_STACK_CODEC.optionalFieldOf("icon", ItemStack.EMPTY).forGetter(Deity::getIcon),
            Deity.Gender.CODEC.optionalFieldOf("gender", Gender.MALE).forGetter(Deity::getGender),
            Codec.BOOL.optionalFieldOf("unlocked", true).forGetter(Deity::isUnlocked),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(Deity::isEnabled),
            Codec.INT.optionalFieldOf("minlevel", -10).forGetter(Deity::getMinLevel),
            Codec.INT.optionalFieldOf("maxlevel", 10).forGetter(Deity::getMaxLevel)
    ).apply(instance, Deity::new));

    /** The deity ID, must be unique **/
    private final ResourceLocation id;
    /** The ItemStack icon in the favor GUI **/
    private final ItemStack icon;
    /** The gender to use for pronouns and models **/
    private final Deity.Gender gender;
    /** True if the deity is unlocked by default **/
    private final boolean unlocked;
    /** True if the deity is enabled by default **/
    private final boolean enabled;
    /** The minimum favor level **/
    private final int minLevel;
    /** The maximum favor level **/
    private final int maxLevel;
    /** The deity name **/
    private final Component name;

    public Deity(ResourceLocation id, ItemStack icon, Deity.Gender gender, boolean unlocked, boolean enabled, int minLevel, int maxLevel) {
        this.id = id;
        this.icon = icon;
        this.gender = gender;
        this.unlocked = unlocked;
        this.enabled = enabled;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.name = Component.translatable("deity." + id.getNamespace() + "." + id.getPath());
    }

    //// HELPER METHODS ////

    /**
     * @param registryAccess the registry access
     * @return the {@link Deity} registry
     */
    public static Registry<Deity> getRegistry(final RegistryAccess registryAccess) {
        return registryAccess.registryOrThrow(RGRegistry.Keys.DEITIES);
    }

    //// GETTERS ////

    /** @return The deity ID, must be unique **/
    public ResourceLocation getId() {
        return id;
    }

    /** @return the deity name **/
    public Component getName() {
        return this.name;
    }

    /** @return The ItemStack icon of the deity in the favor GUI **/
    public ItemStack getIcon() {
        return icon;
    }

    /** @return The gender to use for pronouns and models **/
    public Deity.Gender getGender() {
        return gender;
    }

    /** @return True if the deity is unlocked **/
    public boolean isUnlocked() {
        return unlocked;
    }

    /** @return True if the deity is enabled **/
    public boolean isEnabled() {
        return enabled;
    }

    /** @return The minimum favor level for this deity **/
    public int getMinLevel() {
        return minLevel;
    }

    /** @return The maximum favor level for this deity **/
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public String toString() {
        return "Deity{" +
                "id=" + id +
                ", unlocked=" + unlocked +
                ", enabled=" + enabled +
                ", minLevel=" + minLevel +
                ", maxLevel=" + maxLevel +
                '}';
    }

    public static enum Gender implements StringRepresentable {
        MALE("male"),
        FEMALE("female"),
        OTHER("other");

        public static final Codec<Gender> CODEC = StringRepresentable.fromEnum(Gender::values);

        private final String name;

        Gender(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
