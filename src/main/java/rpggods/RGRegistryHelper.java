/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.server.ServerLifecycleHooks;
import rpggods.data.deity.Altar;
import rpggods.data.deity.Deity;
import rpggods.data.deity.DeityContainer;
import rpggods.data.deity.Offering;
import rpggods.data.deity.Sacrifice;
import rpggods.data.perk.AffinityType;
import rpggods.data.perk.Perk;
import rpggods.data.perk.action.AffinityAction;
import rpggods.data.perk.action.PerkAction;
import net.minecraft.util.Tuple;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Helper class to access datapack registries.
 * Replaces the old static Map fields that were removed during the 1.20 migration.
 */
public final class RGRegistryHelper {

    private RGRegistryHelper() {}

    /**
     * Gets the registry access from the current context (server or client)
     * @return the registry access, or null if not available
     */
    @Nullable
    public static RegistryAccess getRegistryAccess() {
        // Try server first
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.registryAccess();
        }
        // Fall back to client
        return getClientRegistryAccess();
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    private static RegistryAccess getClientRegistryAccess() {
        if (Minecraft.getInstance().level != null) {
            return Minecraft.getInstance().level.registryAccess();
        }
        return null;
    }

    //// ALTAR METHODS ////

    /**
     * @param id the altar ID
     * @return the altar, or Altar.EMPTY if not found
     */
    public static Altar getAltar(ResourceLocation id) {
        RegistryAccess access = getRegistryAccess();
        if (access != null) {
            return Altar.getRegistry(access).getOptional(id).orElse(Altar.EMPTY);
        }
        return Altar.EMPTY;
    }

    /**
     * @return all altar IDs
     */
    public static Set<ResourceLocation> getAltarIds() {
        RegistryAccess access = getRegistryAccess();
        if (access != null) {
            return Altar.getRegistry(access).keySet();
        }
        return Collections.emptySet();
    }

    /**
     * @return all altar entries as a list of tuples (ID, Altar)
     */
    public static List<Tuple<ResourceLocation, Altar>> getAltarEntries() {
        RegistryAccess access = getRegistryAccess();
        if (access != null) {
            List<Tuple<ResourceLocation, Altar>> entries = new ArrayList<>();
            Altar.getRegistry(access).entrySet().forEach(entry -> {
                entries.add(new Tuple<>(entry.getKey().location(), entry.getValue()));
            });
            return entries;
        }
        return Collections.emptyList();
    }

    //// DEITY METHODS ////

    /**
     * @param id the deity ID
     * @return the deity, or Deity.EMPTY if not found
     */
    public static Deity getDeity(ResourceLocation id) {
        RegistryAccess access = getRegistryAccess();
        if (access != null) {
            return Deity.getRegistry(access).getOptional(id).orElse(Deity.EMPTY);
        }
        return Deity.EMPTY;
    }

    /**
     * @param id the deity ID
     * @return an optional containing the deity
     */
    public static Optional<Deity> getOptionalDeity(ResourceLocation id) {
        RegistryAccess access = getRegistryAccess();
        if (access != null) {
            return Deity.getRegistry(access).getOptional(id);
        }
        return Optional.empty();
    }

    /**
     * @return all deity IDs
     */
    public static Set<ResourceLocation> getDeityIds() {
        RegistryAccess access = getRegistryAccess();
        if (access != null) {
            return Deity.getRegistry(access).keySet();
        }
        return Collections.emptySet();
    }

    //// OFFERING METHODS ////

    /**
     * @param id the offering ID
     * @return the offering, or Offering.EMPTY if not found
     */
    public static Offering getOffering(ResourceLocation id) {
        RegistryAccess access = getRegistryAccess();
        if (access != null) {
            return Offering.getRegistry(access).getOptional(id).orElse(Offering.EMPTY);
        }
        return Offering.EMPTY;
    }

    /**
     * @param id the offering ID
     * @return an optional containing the offering
     */
    public static Optional<Offering> getOptionalOffering(ResourceLocation id) {
        RegistryAccess access = getRegistryAccess();
        if (access != null) {
            return Offering.getRegistry(access).getOptional(id);
        }
        return Optional.empty();
    }

    //// SACRIFICE METHODS ////

    /**
     * @param id the sacrifice ID
     * @return the sacrifice, or Sacrifice.EMPTY if not found
     */
    public static Sacrifice getSacrifice(ResourceLocation id) {
        RegistryAccess access = getRegistryAccess();
        if (access != null) {
            return Sacrifice.getRegistry(access).getOptional(id).orElse(Sacrifice.EMPTY);
        }
        return Sacrifice.EMPTY;
    }

    /**
     * @param id the sacrifice ID
     * @return an optional containing the sacrifice
     */
    public static Optional<Sacrifice> getOptionalSacrifice(ResourceLocation id) {
        RegistryAccess access = getRegistryAccess();
        if (access != null) {
            return Sacrifice.getRegistry(access).getOptional(id);
        }
        return Optional.empty();
    }

    //// PERK METHODS ////

    /**
     * @param id the perk ID
     * @return the perk, or Perk.EMPTY if not found
     */
    public static Perk getPerk(ResourceLocation id) {
        RegistryAccess access = getRegistryAccess();
        if (access != null) {
            return Perk.getRegistry(access).getOptional(id).orElse(Perk.EMPTY);
        }
        return Perk.EMPTY;
    }

    /**
     * @param id the perk ID
     * @return an optional containing the perk
     */
    public static Optional<Perk> getOptionalPerk(ResourceLocation id) {
        RegistryAccess access = getRegistryAccess();
        if (access != null) {
            return Perk.getRegistry(access).getOptional(id);
        }
        return Optional.empty();
    }

    //// DEITY CONTAINER METHODS ////

    /**
     * @return all deity containers
     */
    public static Collection<DeityContainer> getDeityContainers() {
        RegistryAccess access = getRegistryAccess();
        if (access != null) {
            return DeityContainer.getRegistry(access.registryOrThrow(RGRegistry.Keys.DEITIES) != null).values();
        }
        return Collections.emptyList();
    }

    /**
     * @param id the deity ID
     * @return the deity container
     */
    public static DeityContainer getDeityContainer(ResourceLocation id) {
        RegistryAccess access = getRegistryAccess();
        if (access != null) {
            return DeityContainer.getOrCreate(access, id);
        }
        throw new IllegalStateException("Cannot get DeityContainer without registry access");
    }

    //// AFFINITY METHODS ////

    /**
     * Gets the affinity perk IDs for a given entity type and affinity type.
     * This replaces the old RPGGods.AFFINITY map lookup.
     * @param entityId the entity type ResourceLocation
     * @param affinityType the affinity type
     * @return list of perk IDs that have affinity actions for this entity
     */
    public static List<ResourceLocation> getAffinityPerkIds(ResourceLocation entityId, AffinityType affinityType) {
        RegistryAccess access = getRegistryAccess();
        if (access == null) {
            return Collections.emptyList();
        }
        List<ResourceLocation> result = new ArrayList<>();
        Perk.getRegistry(access).entrySet().forEach(entry -> {
            Perk perk = entry.getValue();
            for (PerkAction action : perk.getActions()) {
                if (action instanceof AffinityAction affinityAction) {
                    ResourceLocation actionEntityId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(affinityAction.getEntityType());
                    if (entityId.equals(actionEntityId) && affinityAction.getAffinityType() == affinityType) {
                        result.add(entry.getKey().location());
                        break;
                    }
                }
            }
        });
        return result;
    }

    /**
     * Gets the affinity map for a given entity type.
     * This replaces the old RPGGods.AFFINITY.getOrDefault(id, ImmutableMap.of()) lookup.
     * @param entityId the entity type ResourceLocation
     * @return map of affinity type to list of perk IDs
     */
    public static Map<AffinityType, List<ResourceLocation>> getAffinityMap(ResourceLocation entityId) {
        Map<AffinityType, List<ResourceLocation>> result = new HashMap<>();
        for (AffinityType type : AffinityType.values()) {
            List<ResourceLocation> perks = getAffinityPerkIds(entityId, type);
            if (!perks.isEmpty()) {
                result.put(type, perks);
            }
        }
        return result;
    }
}
