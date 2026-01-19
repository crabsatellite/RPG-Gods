/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.deity;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RGRegistry;
import rpggods.data.perk.condition.PerkCondition;
import rpggods.data.perk.condition.TrueCondition;
import rpggods.util.RGCodecUtils;

import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Immutable
public class Sacrifice {

    /** Empty sacrifice used as a fallback **/
    public static final Sacrifice EMPTY = new Sacrifice(
            EntityType.PIG, 0, 0, 0, 0,
            TrueCondition.INSTANCE,
            Optional.empty(), Optional.empty()
    );

    /**
     * @param registryAccess the registry access
     * @return the {@link Sacrifice} registry
     */
    public static Registry<Sacrifice> getRegistry(final RegistryAccess registryAccess) {
        return registryAccess.registryOrThrow(RGRegistry.Keys.SACRIFICES);
    }

    public static final Codec<Sacrifice> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ForgeRegistries.ENTITY_TYPES.getCodec().fieldOf("entity").forGetter(Sacrifice::getEntity),
            Codec.INT.optionalFieldOf("favor", 0).forGetter(Sacrifice::getFavor),
            Codec.INT.optionalFieldOf("maxuses", 16).forGetter(Sacrifice::getMaxUses),
            Codec.INT.optionalFieldOf("restocks", -1).forGetter(Sacrifice::getRestocks),
            Codec.INT.optionalFieldOf("cooldown", 12000).forGetter(Sacrifice::getCooldown),
            PerkCondition.DIRECT_CODEC.optionalFieldOf("condition", TrueCondition.INSTANCE).forGetter(Sacrifice::getCondition),
            ResourceLocation.CODEC.optionalFieldOf("function").forGetter(Sacrifice::getFunction),
            Codec.STRING.optionalFieldOf("translation_key").forGetter(Sacrifice::getFunctionTranslationKey)
    ).apply(instance, Sacrifice::new));

    private final EntityType<?> entity;
    private final int favor;
    private final int maxUses;
    private final int restocks;
    private final int cooldown;
    private final PerkCondition condition;
    private final Optional<ResourceLocation> function;
    private final Optional<String> functionTranslationKey;

    public Sacrifice(EntityType<?> entity, int favor, int maxUses, int restocks, int cooldown,
                     PerkCondition condition,
                     Optional<ResourceLocation> function, Optional<String> functionTranslationKey) {
        this.entity = entity;
        this.favor = favor;
        this.maxUses = maxUses;
        this.restocks = restocks;
        this.cooldown = cooldown;
        this.condition = condition;
        this.function = function;
        this.functionTranslationKey = functionTranslationKey;
    }

    //// HELPER METHODS ////

    /**
     * @param entry the registry entry
     * @param deityId the deity ID
     * @return true if the given registry entry should be associated with the given deity
     */
    public static boolean isFor(final Map.Entry<ResourceKey<Sacrifice>, Sacrifice> entry, final ResourceLocation deityId) {
        // validate sacrifice data
        final Sacrifice sacrifice = entry.getValue();
        if (sacrifice.getFavor() == 0 && sacrifice.getFunction().isEmpty()) {
            return false;
        }
        // validate resource location
        final ResourceLocation sacrificeId = entry.getKey().location();
        if(!sacrificeId.getNamespace().equals(deityId.getNamespace()) || !sacrificeId.getPath().contains(deityId.getPath() + "/")) {
            return false;
        }
        // all checks passed
        return true;
    }

    //// GETTERS ////

    public EntityType<?> getEntity() {
        return entity;
    }

    public int getFavor() {
        return favor;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public int getRestocks() {
        return restocks;
    }

    public int getCooldown() {
        return cooldown;
    }

    public PerkCondition getCondition() {
        return condition;
    }

    public Optional<ResourceLocation> getFunction() {
        return function;
    }

    public Optional<String> getFunctionTranslationKey() {
        return functionTranslationKey;
    }

    public Cooldown createCooldown() {
        return new Cooldown(this.maxUses, this.cooldown, this.restocks);
    }
}
