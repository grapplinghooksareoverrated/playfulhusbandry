package net.apricotdev.playfulhusbandry.events;

import net.apricotdev.playfulhusbandry.PlayfulHusbandry;
import net.apricotdev.playfulhusbandry.genetics.AnimalGenetics;
import net.apricotdev.playfulhusbandry.genetics.AnimalGeneticsProvider;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;

@Mod.EventBusSubscriber(modid = PlayfulHusbandry.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AnimalForgeEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnimalForgeEvents.class);

    // Adds AnimalGenetics to all animals.
    @SubscribeEvent
    public static void onAttachCapabilitiesTamable(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Animal) {
            event.addCapability(
                    ResourceLocation.fromNamespaceAndPath(PlayfulHusbandry.MODID, "properties")
                    , new AnimalGeneticsProvider()
            );
            LOGGER.info("AnimalGenetics was added to " + event.getObject().getType().toShortString());
        }
    }

    // Intervene when any entity is about to drop items after death.
    // Used specifically to let AnimalGenetics modify drop quantities.
    @SubscribeEvent
    public static void onLivingDropsEvent(LivingDropsEvent event) {
        event.getEntity().getCapability(AnimalGeneticsProvider.ANIMAL_GENETICS).ifPresent( animalGenetics -> {

            if (animalGenetics.shouldNotDropLoot()) {
                event.setCanceled(true);
                return;
            }

            // WARNING: This code structure means that a single drop may be offset multiple times.
            var drops = event.getDrops();
            var tagOffsets = animalGenetics.getTagOffsets();
            for (ItemEntity drop : drops) {
                // Check if key matches a tag found in the offsets.
                for (TagKey<Item> tag : tagOffsets.keySet()) {
                    if (drop.getItem().is(tag)) {
                        // Apply offset
                        var offsetValue = tagOffsets.get(tag);
                        drop.getItem().setCount(drop.getItem().getCount() + offsetValue);
                    }
                }
            }

            LOGGER.info("Killed an entity that had an AnimalGenetics instance.");
        });
    }

    @SubscribeEvent
    public static void onBabySpawnEvent(BabyEntitySpawnEvent event){
        // Check that all three mobs have AnimalGenetics capability
        var parentA = event.getParentA();
        var parentB = event.getParentB();
        var child = event.getChild();
        if (child == null) return;
        var parentALazy = parentA.getCapability(AnimalGeneticsProvider.ANIMAL_GENETICS).resolve();
        var parentBLazy = parentB.getCapability(AnimalGeneticsProvider.ANIMAL_GENETICS).resolve();
        var childLazy =     child.getCapability(AnimalGeneticsProvider.ANIMAL_GENETICS).resolve();
        if (parentALazy.isPresent() && parentBLazy.isPresent() && childLazy.isPresent()) {
            var parentAGenetics = parentALazy.get();
            var parentBGenetics = parentBLazy.get();
            var childAGenetics = childLazy.get();
            childAGenetics.setAsNewborn(child, parentA.getId(), parentB.getId(), parentAGenetics, parentBGenetics);
        }
    }

}
