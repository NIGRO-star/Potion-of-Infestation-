package com.infestationpotion;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.projectile.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Vanilla ignores armor stands for potions (ArmorStandEntity#isAffectedByPotions is false).
 * This class applies splash potions and lingering clouds to armor stands manually,
 * for every potion (vanilla and modded), and makes the Infested effect work on them.
 */
@Mod.EventBusSubscriber(modid = InfestationPotionMod.MODID)
public class ArmorStandEvents {

    /** Splash potions landing near armor stands + projectiles hitting infested armor stands. */
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Entity projectile = event.getEntity();
        if (projectile.level.isClientSide) {
            return;
        }
        RayTraceResult ray = event.getRayTraceResult();

        if (projectile instanceof PotionEntity) {
            if (ray.getType() != RayTraceResult.Type.MISS) {
                splashArmorStands((PotionEntity) projectile, ray);
            }
            return;
        }

        // Arrows, tridents, snowballs... hitting an infested armor stand
        if (ray.getType() == RayTraceResult.Type.ENTITY) {
            Entity hit = ((EntityRayTraceResult) ray).getEntity();
            if (hit instanceof ArmorStandEntity) {
                InfestationEvents.trySpawnSilverfish((ArmorStandEntity) hit);
            }
        }
    }

    /** A player punching an infested armor stand. */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Entity target = event.getTarget();
        if (target instanceof ArmorStandEntity) {
            InfestationEvents.trySpawnSilverfish((ArmorStandEntity) target);
        }
    }

    /** Lingering potion clouds affecting armor stands (checked once per second). */
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (!(entity instanceof ArmorStandEntity) || entity.level.isClientSide || entity.tickCount % 20 != 0) {
            return;
        }

        for (AreaEffectCloudEntity cloud : entity.level.getEntitiesOfClass(AreaEffectCloudEntity.class, entity.getBoundingBox())) {
            double dx = entity.getX() - cloud.getX();
            double dz = entity.getZ() - cloud.getZ();
            double radius = cloud.getRadius();
            if (dx * dx + dz * dz > radius * radius) {
                continue;
            }
            for (EffectInstance inst : cloud.getPotion().getEffects()) {
                Effect effect = inst.getEffect();
                if (effect.isInstantenous()) {
                    effect.applyInstantenousEffect(cloud, cloud.getOwner(), entity, inst.getAmplifier(), 0.5D);
                } else {
                    entity.addEffect(new EffectInstance(effect, inst.getDuration() / 4,
                            inst.getAmplifier(), inst.isAmbient(), inst.isVisible()));
                }
            }
        }
    }

    private static void splashArmorStands(PotionEntity potion, RayTraceResult ray) {
        ItemStack stack = potion.getItem();
        if (stack.getItem() == Items.LINGERING_POTION) {
            return; // lingering potions create a cloud, handled in onLivingUpdate
        }
        java.util.List<EffectInstance> effects = PotionUtils.getMobEffects(stack);
        if (effects.isEmpty()) {
            return;
        }

        Entity directHit = ray.getType() == RayTraceResult.Type.ENTITY
                ? ((EntityRayTraceResult) ray).getEntity() : null;
        AxisAlignedBB box = potion.getBoundingBox().inflate(4.0D, 2.0D, 4.0D);

        for (ArmorStandEntity stand : potion.level.getEntitiesOfClass(ArmorStandEntity.class, box)) {
            double distSq = potion.distanceToSqr(stand);
            if (distSq >= 16.0D) {
                continue;
            }
            double potency = stand == directHit ? 1.0D : 1.0D - Math.sqrt(distSq) / 4.0D;

            for (EffectInstance inst : effects) {
                Effect effect = inst.getEffect();
                if (effect.isInstantenous()) {
                    effect.applyInstantenousEffect(potion, potion.getOwner(), stand, inst.getAmplifier(), potency);
                } else {
                    int duration = (int) (potency * (double) inst.getDuration() + 0.5D);
                    if (duration > 20) {
                        stand.addEffect(new EffectInstance(effect, duration,
                                inst.getAmplifier(), inst.isAmbient(), inst.isVisible()));
                    }
                }
            }
        }
    }
}
