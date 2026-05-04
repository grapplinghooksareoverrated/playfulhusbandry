package net.apricotdev.playfulhusbandry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = PlayfulHusbandry.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue ANIMAL_EXPLOSION_RADIUS = BUILDER
            .comment("Size of explosion generated when animals are fed too many heirlooms.\nDoes not apply to chickens.")
            .defineInRange("animal_explosion_radius", 3, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue CHICKEN_EXPLOSION_RADIUS = BUILDER
            .comment("Size of explosion generated when chickens are fed too many heirlooms")
            .defineInRange("chicken_explosion_radius", 6, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue INCEST_MUTATION_MIN = BUILDER
            .comment("Minimum mutation value for incest")
            .defineInRange("incest_mutation_min", -3, Integer.MIN_VALUE, 0);

    public static final ForgeConfigSpec.IntValue INCEST_MUTATION_MAX = BUILDER
            .comment("Maximum mutation value for incest")
            .defineInRange("incest_mutation_max", 3, 0, Integer.MAX_VALUE);


    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int animal_explosion_radius;
    public static int chicken_explosion_radius;
    public static int incest_mutation_min;
    public static int incest_mutation_max;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        animal_explosion_radius = ANIMAL_EXPLOSION_RADIUS.get();
        chicken_explosion_radius = CHICKEN_EXPLOSION_RADIUS.get();
        incest_mutation_min = INCEST_MUTATION_MIN.get();
        incest_mutation_max = INCEST_MUTATION_MAX.get();
    }
}
