/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import rpggods.RGRegistry;
import rpggods.data.deity.Deity;
import rpggods.data.favor.FavorLevel;
import rpggods.data.favor.FavorRange;
import rpggods.data.perk.action.PerkAction;
import rpggods.data.perk.condition.FalseCondition;
import rpggods.data.perk.condition.PerkCondition;
import rpggods.data.perk.condition.TrueCondition;
import rpggods.util.RGCodecUtils;

import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Immutable
public final class Perk {

    @Deprecated
    public static final Perk EMPTY = new Perk(PerkIcon.EMPTY, FalseCondition.INSTANCE,
            FavorRange.EMPTY, List.of(), 0.0F, "null", 1000L, Optional.empty());

    /**
     * @param registryAccess the registry access
     * @return the {@link Perk} registry
     */
    public static Registry<Perk> getRegistry(final RegistryAccess registryAccess) {
        return registryAccess.registryOrThrow(RGRegistry.Keys.PERKS);
    }

    public static final Codec<Perk> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PerkIcon.CODEC.optionalFieldOf("icon", PerkIcon.EMPTY).forGetter(Perk::getIcon),
            PerkCondition.DIRECT_CODEC.optionalFieldOf("condition", TrueCondition.INSTANCE).forGetter(Perk::getCondition),
            FavorRange.CODEC.optionalFieldOf("range", FavorRange.EMPTY).forGetter(Perk::getRange),
            RGCodecUtils.listOrElementCodec(PerkAction.DIRECT_CODEC).optionalFieldOf("action", List.of()).forGetter(Perk::getActions),
            Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(Perk::getChance),
            Codec.STRING.optionalFieldOf("cooldown_category", "").forGetter(Perk::getCategory),
            Codec.LONG.optionalFieldOf("cooldown", 600L).forGetter(Perk::getCooldown),
            Codec.BOOL.optionalFieldOf("positive").forGetter(Perk::getPositiveFlag)
    ).apply(instance, Perk::new));

    private final PerkIcon icon;
    private final PerkCondition condition;
    private final FavorRange range;
    private final List<PerkAction> actions;
    private final float chance;
    private final String category;
    private final long cooldown;
    private final Optional<Boolean> positiveFlag;
    private final boolean isPositive;

    public Perk(PerkIcon icon, PerkCondition condition, FavorRange range,
                List<PerkAction> actions, float chance, String category, long cooldown,
                Optional<Boolean> positiveFlag) {
        this.icon = icon;
        this.condition = condition;
        this.range = range;
        this.actions = actions;
        this.chance = chance;
        this.cooldown = cooldown;
        this.positiveFlag = positiveFlag;
        this.isPositive = positiveFlag.orElse(range.getMinLevel() >= 0);
        // determine category if not provided
        String tempCategory = category;
        if(null == tempCategory || tempCategory.isEmpty()) {
            // verify actions non-empty
            if(actions.isEmpty()) {
                tempCategory = "empty perk go brrrrr";
            } else {
                ResourceLocation actionTypeId = RGRegistry.PERK_ACTION_TYPES_SUPPLIER.get().getKey(actions.get(0).getCodec());
                tempCategory = actionTypeId.toString();
            }
        }
        this.category = tempCategory;
    }

    /**
     * @param entry the registry entry
     * @param deityId the deity ID
     * @return true if the given registry entry should be associated with the given deity
     */
    public static boolean isFor(final Map.Entry<ResourceKey<Perk>, Perk> entry, final ResourceLocation deityId) {
        // validate perk data
        final Perk perk = entry.getValue();
        if (FavorRange.EMPTY.equals(perk.getRange()) || perk.getActions().isEmpty()) {
            return false;
        }
        // validate resource location
        final ResourceLocation perkId = entry.getKey().location();
        if(!perkId.getNamespace().equals(deityId.getNamespace()) || !perkId.getPath().contains(deityId.getPath() + "/")) {
            return false;
        }
        // all checks passed
        return true;
    }

    //// GETTERS ////

    public PerkIcon getIcon() {
        return icon;
    }

    public PerkCondition getCondition() {
        return condition;
    }

    public FavorRange getRange() {
        return range;
    }

    public float getChance() {
        return chance;
    }

    /**
     * @param level the favor level to check
     * @return the percent chance to run this perk with added bonus, if any
     */
    public float getAdjustedChance(FavorLevel level) {
        return getChance() + level.getPerkBonus();
    }

    public String getCategory() {
        return category;
    }

    public long getCooldown() {
        return cooldown;
    }

    public Optional<Boolean> getPositiveFlag() {
        return positiveFlag;
    }

    public boolean isPositive() {
        return isPositive;
    }

    public List<PerkAction> getActions() {
        return actions;
    }

    public ResourceLocation getDeity() {
        return getRange().getDeity();
    }

    public Deity getDeity(final RegistryAccess registryAccess) {
        return Deity.getRegistry(registryAccess).get(getDeity());
    }

    @Override
    public String toString() {
        return "Perk{" +
                "icon=" + icon.getItem().getItem().toString() +
                ", conditions=" + condition +
                ", range=" + range +
                ", actions=" + actions +
                ", chance=" + chance +
                ", cooldown_category='" + category + '\'' +
                ", cooldown=" + cooldown +
                '}';
    }
}
