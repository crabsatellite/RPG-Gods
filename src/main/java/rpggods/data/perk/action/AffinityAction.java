/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.data.perk.AffinityType;
import rpggods.data.tameable.ITameable;

public class AffinityAction extends PerkAction {

    public static final Codec<AffinityAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(ForgeRegistries.ENTITY_TYPES.getCodec().fieldOf("entity").forGetter(o -> o.entityType))
            .and(AffinityType.CODEC.fieldOf("affinity").forGetter(o -> o.affinity))
            .apply(instance, AffinityAction::new));

    private final EntityType<?> entityType;
    private final AffinityType affinity;

    public AffinityAction(boolean isHidden, EntityType<?> entityType, AffinityType affinity) {
        super(isHidden);
        this.entityType = entityType;
        this.affinity = affinity;
    }

    @Override
    public boolean apply(PerkActionContext context) {
        // only handles the TAME affinity type.
        // other affinity types are handled elsewhere.
        if(context.getEntity().isPresent() && affinity == AffinityType.TAME) {
            final Entity entity = context.getEntity().get();
            LazyOptional<ITameable> tameable = entity.getCapability(RPGGods.TAMEABLE);
            if(tameable.isPresent()) {
                if(tameable.orElse(null).setTamedBy(context.getPlayer())) {
                    // set custom name to prevent despawn
                    if(!entity.hasCustomName()) {
                        entity.setCustomName(entity.getDisplayName());
                    }
                    // spawn particles
                    Vec3 pos = entity.getEyePosition(1.0F);
                    context.getLevel().sendParticles(ParticleTypes.HEART, pos.x, pos.y, pos.z, 10, 0.5D, 0.5D, 0.5D, 0);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Component createDescription(RegistryAccess registryAccess) {
        Component entityComponent = entityType.getDescription();
        return Component.translatable(PREFIX + "affinity" + SUFFIX + "." + affinity.getSerializedName(), entityComponent);
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.AFFINITY.get();
    }

    public EntityType<?> getEntityType() {
        return entityType;
    }

    public AffinityType getAffinityType() {
        return affinity;
    }
}
