package com.infestationpotion;

import java.util.Random;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.SilverfishEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = InfestationPotionMod.MODID)
public class InfestationEvents {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (entity.level.isClientSide || event.getAmount() <= 0.0F) {
            return;
        }
        if (!entity.hasEffect(ModEffects.INFESTED.get())) {
            return;
        }

        Random rand = entity.getRandom();
        if (rand.nextDouble() >= InfestationConfig.SPAWN_CHANCE.get()) {
            return;
        }

        int count = 1 + rand.nextInt(InfestationConfig.MAX_SILVERFISH.get());
        for (int i = 0; i < count; i++) {
            SilverfishEntity silverfish = EntityType.SILVERFISH.create(entity.level);
            if (silverfish == null) {
                continue;
            }
            double x = entity.getX() + (rand.nextDouble() - 0.5D) * entity.getBbWidth();
            double z = entity.getZ() + (rand.nextDouble() - 0.5D) * entity.getBbWidth();
            silverfish.moveTo(x, entity.getY(), z, rand.nextFloat() * 360.0F, 0.0F);
            entity.level.addFreshEntity(silverfish);
        }
    }
}
