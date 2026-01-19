/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.deity;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.ApiStatus;
import rpggods.RGRegistry;
import rpggods.data.perk.Perk;
import rpggods.data.perk.action.PerkAction;
import rpggods.data.perk.condition.PerkCondition;

import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Centralizes all offerings, sacrifices, and perks
 * to reduce expensive searches and sorts after data has been loaded.
 */
@Immutable
public class DeityContainer {

    /** The ResourceLocation ID **/
    private final ResourceLocation id;
    /** The Deity instance **/
    private final Deity deity;
    /** Map of Altar ID to Altar(s) **/
    private final Map<ResourceLocation, Altar> altarMap;
    /** Map of Item ID to Offering(s) **/
    private final Map<ResourceLocation, Map<ResourceLocation, Offering>> offeringMap;
    /** Map of Entity ID to Sacrifice(s) **/
    private final Map<ResourceLocation, Map<ResourceLocation, Sacrifice>> sacrificeMap;
    /** Map of Perk ID to Perk **/
    private final Map<ResourceLocation, Perk> perkMap;
    /** Map of Perk Condition Type to Perk(s). May contain multiple instances of the same Perk. **/
    private final Map<Codec<? extends PerkCondition>, Map<ResourceLocation, Perk>> perkByConditionMap;
    /** Map of Perk Action Type to Perk(s) **/
    private final Map<Codec<? extends PerkAction>, Map<ResourceLocation, Perk>> perkByActionMap;

    //// CONSTRUCTOR ////

    private DeityContainer(RegistryAccess registryAccess, ResourceLocation id) {
        // cache ID
        this.id = id;

        // load registries
        final Registry<Deity> deityRegistry = registryAccess.registryOrThrow(RGRegistry.Keys.DEITIES);
        final Registry<Altar> altarRegistry = registryAccess.registryOrThrow(RGRegistry.Keys.ALTARS);
        final Registry<Offering> offeringRegistry = registryAccess.registryOrThrow(RGRegistry.Keys.OFFERINGS);
        final Registry<Sacrifice> sacrificeRegistry = registryAccess.registryOrThrow(RGRegistry.Keys.SACRIFICES);
        final Registry<Perk> perkRegistry = registryAccess.registryOrThrow(RGRegistry.Keys.PERKS);

        // cache deity
        this.deity = deityRegistry
                .getOptional(id)
                .orElseThrow(() -> new IllegalStateException("[DeityContainer] Missing deity with ID \"" + id + "\""));

        // create map of altar ID to altar for this deity
        {
            Map<ResourceLocation, Altar> altarBuilder = new HashMap<>();
            for (Map.Entry<ResourceKey<Altar>, Altar> entry : altarRegistry.entrySet()) {
                // validate altar and add to builder
                if (Altar.isFor(entry, id)) {
                    altarBuilder.put(entry.getKey().location(), entry.getValue());
                }
            }
            // cache altars
            this.altarMap = Collections.unmodifiableMap(altarBuilder);
        }

        // create map of item ID to offering(s) for this deity
        {
            Map<ResourceLocation, Map<ResourceLocation, Offering>> offeringBuilder = new HashMap<>();
            for (Map.Entry<ResourceKey<Offering>, Offering> entry : offeringRegistry.entrySet()) {
                // validate offering and add to builder
                if (Offering.isFor(entry, id)) {
                    ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(entry.getValue().getOffering().getItem());
                    offeringBuilder.computeIfAbsent(itemId, r -> new HashMap<>())
                            .put(entry.getKey().location(), entry.getValue());
                }
            }
            // convert map values to unmodifiable lists
            offeringBuilder.replaceAll((key, value) -> Collections.unmodifiableMap(value));
            // cache offerings
            this.offeringMap = Collections.unmodifiableMap(offeringBuilder);
        }

        // create map of entity ID to sacrifice(s)
        {
            Map<ResourceLocation, Map<ResourceLocation, Sacrifice>> sacrificeBuilder = new HashMap<>();
            for (Map.Entry<ResourceKey<Sacrifice>, Sacrifice> entry : sacrificeRegistry.entrySet()) {
                // validate sacrifice and add to builder
                if (Sacrifice.isFor(entry, id)) {
                    ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entry.getValue().getEntity());
                    sacrificeBuilder.computeIfAbsent(entityId, r -> new HashMap<>())
                            .put(entry.getKey().location(), entry.getValue());
                }
            }
            // convert map values to unmodifiable lists
            sacrificeBuilder.replaceAll((key, value) -> Collections.unmodifiableMap(value));
            // cache sacrifices
            this.sacrificeMap = Collections.unmodifiableMap(sacrificeBuilder);
        }

        // create map of perk ID to perk
        {
            Map<ResourceLocation, Perk> perkBuilder = new HashMap<>();
            for (Map.Entry<ResourceKey<Perk>, Perk> entry : perkRegistry.entrySet()) {
                // validate perk and add to builder
                if (Perk.isFor(entry, id)) {
                    perkBuilder.put(entry.getKey().location(), entry.getValue());
                }
            }
            // cache perks
            this.perkMap = Collections.unmodifiableMap(perkBuilder);
        }

        // create map of perk condition type to perk(s)
        {
            Map<Codec<? extends PerkCondition>, Map<ResourceLocation, Perk>> perkByConditionBuilder = new IdentityHashMap<>();
            for(Map.Entry<ResourceLocation, Perk> entry : this.perkMap.entrySet()) {
                // get the single condition from the perk and add to builder
                PerkCondition condition = entry.getValue().getCondition();
                Codec<? extends PerkCondition> conditionType = condition.getCodec();
                perkByConditionBuilder.computeIfAbsent(conditionType, c -> new HashMap<>())
                        .put(entry.getKey(), entry.getValue());
            }
            // convert map values to unmodifiable lists
            perkByConditionBuilder.replaceAll((key, value) -> Collections.unmodifiableMap(value));
            // cache perk by condition map
            this.perkByConditionMap = Collections.unmodifiableMap(perkByConditionBuilder);
        }

        // create map of perk action type to perk(s)
        {
            Map<Codec<? extends PerkAction>, Map<ResourceLocation, Perk>> perkByActionBuilder = new IdentityHashMap<>();
            for(Map.Entry<ResourceLocation, Perk> entry : this.perkMap.entrySet()) {
                // iterate each condition in the perk and add to builder
                for(PerkAction action : entry.getValue().getActions()) {
                    Codec<? extends PerkAction> actionType = action.getCodec();
                    perkByActionBuilder.computeIfAbsent(actionType, c -> new HashMap<>())
                            .put(entry.getKey(), entry.getValue());
                }
            }
            // convert map values to unmodifiable lists
            perkByActionBuilder.replaceAll((key, value) -> Collections.unmodifiableMap(value));
            // cache perk by condition map
            this.perkByActionMap = Collections.unmodifiableMap(perkByActionBuilder);
        }
    }

    //// GETTERS ////

    public ResourceLocation getId() {
        return id;
    }

    public Map<ResourceLocation, Altar> getAltars() {
        return altarMap;
    }

    public Map<ResourceLocation, Map<ResourceLocation, Offering>> getOfferings() {
        return offeringMap;
    }

    public Map<ResourceLocation, Offering> getOfferingsByItem(final Item item) {
        final ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return offeringMap.getOrDefault(key, ImmutableMap.of());
    }

    public Map<ResourceLocation, Map<ResourceLocation, Sacrifice>> getSacrifices() {
        return sacrificeMap;
    }

    public Map<ResourceLocation, Sacrifice> getSacrificesByEntity(final EntityType<?> entityType) {
        final ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        return sacrificeMap.getOrDefault(key, ImmutableMap.of());
    }

    public Map<Codec<? extends PerkCondition>, Map<ResourceLocation, Perk>> getPerkByConditionMap() {
        return perkByConditionMap;
    }

    public Map<Codec<? extends PerkAction>, Map<ResourceLocation, Perk>> getPerkByActionMap() {
        return perkByActionMap;
    }

    public Map<ResourceLocation, Perk> getPerks() {
        return perkMap;
    }

    public Map<ResourceLocation, Perk> getPerksByCondition(final Codec<? extends PerkCondition> condition) {
        return perkByConditionMap.getOrDefault(condition, ImmutableMap.of());
    }

    public Map<ResourceLocation, Perk> getPerksByAction(final Codec<? extends PerkAction> action) {
        return perkByActionMap.getOrDefault(action, ImmutableMap.of());
    }

    public Deity getDeity() {
        return this.deity;
    }

    /**
     * @return an Optional containing the deity, never empty for properly constructed containers
     */
    public Optional<Deity> getOptionalDeity() {
        return Optional.ofNullable(this.deity);
    }

    public Component getName() {
        return this.deity.getName();
    }

    /**
     * Creates a display name component for the given deity/altar ID
     * @param id the ResourceLocation ID
     * @return a translatable component
     */
    public static Component createName(final ResourceLocation id) {
        return Component.translatable("deity." + id.getNamespace() + "." + id.getPath());
    }

    @Override
    public String toString() {
        int offerings = 0;
        for(Map<ResourceLocation, Offering> o : offeringMap.values()) {
            offerings += o.size();
        }
        int sacrifices = 0;
        for(Map<ResourceLocation, Sacrifice> s : sacrificeMap.values()) {
            sacrifices += s.size();
        }
        final StringBuilder sb = new StringBuilder("DeityHelper:");
        sb.append(" id[").append(id).append("]");
        sb.append(" altars[").append(altarMap.size()).append("]");
        sb.append(" offerings[").append(offerings).append("]");
        sb.append(" sacrifices[").append(sacrifices).append("]");
        sb.append(" perks[").append(perkMap.size()).append("]");
        return sb.toString();
    }

    //// REGISTRY ////

    private static final Map<ResourceLocation, DeityContainer> REGISTRY = new HashMap<>();
    private static final Map<ResourceLocation, DeityContainer> CLIENT_REGISTRY = new HashMap<>();

    /**
     * @param isClientSide true to use the client side registry, necessary for caching when using LAN servers
     * @return the {@link DeityContainer} registry
     */
    public static Map<ResourceLocation, DeityContainer> getRegistry(final boolean isClientSide) {
        if(isClientSide) {
            return CLIENT_REGISTRY;
        }
        return REGISTRY;
    }

    /**
     * @param registryAccess the registry access
     * @param id the {@link Deity} ID
     * @return the cached {@link DeityContainer}
     */
    public static DeityContainer getOrCreate(final RegistryAccess registryAccess, final ResourceLocation id) {
        // get existing entry
        final Map<ResourceLocation, DeityContainer> registry = getRegistry(EffectiveSide.get().isClient());
        final DeityContainer entry = registry.get(id);
        if(entry != null) {
            return entry;
        }
        // create new entry
        final DeityContainer container = new DeityContainer(registryAccess, id);
        registry.put(id, container);
        return container;
    }

    /**
     * Loads all values in the {@link Deity} registry and creates {@link DeityContainer}s for each one.
     * @param registryAccess the registry access
     */
    @ApiStatus.Internal
    public static void populate(final RegistryAccess registryAccess) {
        // load golem registry
        final Registry<Deity> registry = registryAccess.registryOrThrow(RGRegistry.Keys.DEITIES);
        // resolve golem containers when the server starts to avoid lag spikes later
        for(ResourceLocation id : registry.keySet()) {
            DeityContainer.getOrCreate(registryAccess, id);
        }
    }

    /**
     * Clears the {@link DeityContainer} registry
     */
    @ApiStatus.Internal
    public static void clearCache() {
        getRegistry(EffectiveSide.get().isClient()).clear();
    }
}
