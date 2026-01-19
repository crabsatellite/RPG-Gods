/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.deity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.util.RGCodecUtils;

import javax.annotation.concurrent.Immutable;
import java.util.Map;
import java.util.Optional;

@Immutable
public class Offering {
    
    public static final String TAG_NBT = "nbt";

    /** Empty offering used as a fallback **/
    public static final Offering EMPTY = new Offering(
            ItemStack.EMPTY, 0, 0, 0, 0,
            Optional.empty(), Integer.MIN_VALUE, Integer.MAX_VALUE,
            Optional.empty(), Optional.empty()
    );

    /**
     * @param registryAccess the registry access
     * @return the {@link Offering} registry
     */
    public static Registry<Offering> getRegistry(final RegistryAccess registryAccess) {
        return registryAccess.registryOrThrow(RGRegistry.Keys.OFFERINGS);
    }
    
    public static final Codec<Offering> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RGCodecUtils.ITEM_OR_STACK_CODEC.optionalFieldOf("item", ItemStack.EMPTY).forGetter(Offering::getOffering),
            Codec.INT.optionalFieldOf("favor", 0).forGetter(Offering::getFavor),
            Codec.INT.optionalFieldOf("maxuses", 16).forGetter(Offering::getMaxUses),
            Codec.INT.optionalFieldOf("restocks", -1).forGetter(Offering::getRestocks),
            Codec.INT.optionalFieldOf("cooldown", 12000).forGetter(Offering::getCooldown),
            RGCodecUtils.ITEM_OR_STACK_CODEC.optionalFieldOf("trade").forGetter(Offering::getResult),
            Codec.INT.optionalFieldOf("minlevel", Integer.MIN_VALUE).forGetter(Offering::getMinLevel),
            Codec.INT.optionalFieldOf("maxlevel", Integer.MAX_VALUE).forGetter(Offering::getMaxLevel),
            ResourceLocation.CODEC.optionalFieldOf("function").forGetter(Offering::getFunction),
            Codec.STRING.optionalFieldOf("function_text").forGetter(Offering::getFunctionTranslationKey)
    ).apply(instance, Offering::new));

    // TODO use Ingredient instead of a single ItemStack
    private final ItemStack offering;
    private final int favor;
    private final Optional<ItemStack> result;
    private final int minLevel;
    private final int maxLevel;
    private final Optional<ResourceLocation> function;
    private final Optional<String> functionTranslationKey;
    private final int maxUses;
    private final int restocks;
    private final int cooldown;

    public Offering(ItemStack offering, int favor, int maxUses,
                    int restocks, int cooldown, Optional<ItemStack> result,
                    int tradeMinLevel, int tradeMaxLevel,
                    Optional<ResourceLocation> function, Optional<String> functionTranslationKey) {
        this.offering = withNbtStringAsTag(offering);
        this.favor = favor;
        this.restocks = restocks;
        this.maxUses = maxUses;
        this.cooldown = cooldown;
        this.result = result.map(Offering::withNbtStringAsTag);
        this.minLevel = Math.min(tradeMinLevel, tradeMaxLevel);
        this.maxLevel = Math.max(tradeMinLevel, tradeMaxLevel);
        this.function = function;
        this.functionTranslationKey = functionTranslationKey;
    }

    //// HELPER METHODS ////

    /**
     * Attempts to parse a {@link CompoundTag} from a String stored in the {@link ItemStack} 
     * and merges the tag back into the {@link ItemStack}
     * @param itemStack the item stack to modify
     * @return true if the item stack was modified
     * @see CompoundTag#merge(CompoundTag) 
     * @see TagParser#parseTag(String) 
     */
    private static ItemStack withNbtStringAsTag(final ItemStack itemStack) {
        // validate tag exists
        if(!itemStack.hasTag() || !itemStack.getTag().contains(TAG_NBT, Tag.TAG_STRING)) {
            return itemStack;
        }
        // parse tag from the string
        try {
            String nbtString = itemStack.getTag().getString(TAG_NBT);
            CompoundTag tag = TagParser.parseTag(nbtString);
            // merge with existing tag, if any
            tag = itemStack.getOrCreateTag().merge(tag);
            // remove string
            tag.remove(TAG_NBT);
            // update itemstack
            itemStack.setTag(tag);
        } catch (CommandSyntaxException e) {
            RPGGods.LOGGER.error("[Offering] Failed to parse NBT from String:\n" + e.getMessage());
        }
        return itemStack;
    }

    /**
     * @param entry the registry entry
     * @param deityId the deity ID
     * @return true if the given registry entry should be associated with the given deity
     */
    public static boolean isFor(final Map.Entry<ResourceKey<Offering>, Offering> entry, final ResourceLocation deityId) {
        // validate offering data
        final Offering offering = entry.getValue();
        if (offering.getFavor() == 0 && offering.getFunction().isEmpty() && offering.getResult().isEmpty()) {
            return false;
        }
        // validate resource location
        final ResourceLocation offeringId = entry.getKey().location();
        if(!offeringId.getNamespace().equals(deityId.getNamespace()) || !offeringId.getPath().contains(deityId.getPath() + "/")) {
            return false;
        }
        // all checks passed
        return true;
    }

    /**
     * Checks the given item stack to see if this offering can accept it.
     * Examines item, count, and enchantments
     *
     * @param offering another item stack
     * @return true if the given ItemStack matches the one in this offering
     */
    public boolean matches(ItemStack offering) {
        // check item and stack size
        if (!ItemStack.isSameItem(this.offering, offering) || offering.getCount() < this.offering.getCount()) {
            return false;
        }
        // check tag only when offering has a tag
        if (this.offering.hasTag() && !this.offering.getTag().isEmpty() && !NbtUtils.compareNbt(this.offering.getTag(), offering.getTag(), true)) {
            return false;
        }
        return true;
    }

    /**
     * ItemStack aware version of {@link #getResult()} that allows
     * the trade item to copy NBT data and enchantments from the offering item.
     *
     * @param offering the offering item
     * @return the trade item (or empty if there is no trade)
     */
    public Optional<ItemStack> getResult(final ItemStack offering) {
        final Optional<ItemStack> oResult = getResult();
        // special handling of trade when same item and NBT is present in the result
        if (oResult.isPresent() && oResult.get().hasTag() && ItemStack.isSameItem(offering, oResult.get())) {
            // create copy of offering item with correct count
            ItemStack tradeItem = offering.copy();
            tradeItem.setCount(oResult.get().getCount());
            // merge tags
            CompoundTag tradeItemTag = tradeItem.getOrCreateTag().merge(oResult.get().getTag());
            tradeItem.setTag(tradeItemTag);
            // merge damage
            if (offering.isDamageableItem() && offering.isDamaged()) {
                tradeItem.setDamageValue(offering.getDamageValue());
            }
            return Optional.of(tradeItem);
        }
        return oResult;
    }

    //// GETTERS ////

    public ItemStack getOffering() {
        return offering;
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

    /**
     * @return a copy of the result item stack, if any
     * @see #getResult(ItemStack)
     */
    public Optional<ItemStack> getResult() {
        return result.map(ItemStack::copy);
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
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

    public boolean hasLevelRange() {
        return hasMinLevel() || hasMaxLevel();
    }

    public boolean hasMinLevel() {
        return minLevel > Integer.MIN_VALUE;
    }

    public boolean hasMaxLevel() {
        return maxLevel < Integer.MAX_VALUE;
    }
}
