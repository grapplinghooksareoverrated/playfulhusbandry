package net.apricotdev.playfulhusbandry.events;

import net.apricotdev.playfulhusbandry.PlayfulHusbandry;
import net.apricotdev.playfulhusbandry.genetics.AnimalGenetics;
import net.apricotdev.playfulhusbandry.genetics.AnimalGeneticsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = PlayfulHusbandry.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AnimalModEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnimalForgeEvents.class);

    // Register AnimalGenetics class so it is treated as a capability.
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(AnimalGenetics.class);
        LOGGER.info("Registered AnimalGenetics class.");
    }

}
