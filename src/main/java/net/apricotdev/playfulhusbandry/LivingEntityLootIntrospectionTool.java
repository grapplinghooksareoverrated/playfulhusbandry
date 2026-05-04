package net.apricotdev.playfulhusbandry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Objects;

public class LivingEntityLootIntrospectionTool {

    public static Logger LOGGER = LogManager.getLogger();

    private static ArrayList<ItemStack> extractItemsFromLootTableJsonRecursive(MinecraftServer server, String namespace, String path) {
        // Declare variable to return
        ArrayList<ItemStack> ret = new ArrayList<>();
        try {
            // Get loot table file
            ResourceLocation fileLocation = ResourceLocation.fromNamespaceAndPath(namespace, "loot_tables/"+path+".json");
            Resource resource = server.getResourceManager()
                    .getResource(fileLocation)
                    .orElseThrow(() -> new FileNotFoundException(fileLocation.toString()));
            BufferedReader reader = resource.openAsReader();
            // Iterate through all LootPools of this Entity.
            JsonElement fileRootElement = JsonParser.parseReader(reader);
            // Double check if the element is a JsonObject. If not, return early.
            if (!fileRootElement.isJsonObject()) {
                LOGGER.warn("The file {} couldn't be converted to a JsonObject. Maybe it's corrupted?", fileLocation.toString());
                return new ArrayList<>();
            }
            JsonObject fileRoot = fileRootElement.getAsJsonObject();
            JsonElement poolsElement = fileRoot.get("pools");
            // Double check if the pools element is a JsonArray.
            // If not, exit early.
            if (poolsElement == null || !poolsElement.isJsonArray()) {
                return new ArrayList<>();
            }
            JsonArray pools = poolsElement.getAsJsonArray();
            // Extract items from each pool.
            for (JsonElement pool : pools) {
                JsonArray entries = pool.getAsJsonObject().get("entries").getAsJsonArray();
                for (JsonElement entry : entries) {
                    // Reverse engineer an ItemStack from each given item.
                    var entryObject = entry.getAsJsonObject();
                    var entryType = entryObject.get("type");
                    var entryName = entryObject.get("name");

                    ResourceLocation entryTypeLocation = ResourceLocation.tryParse(entryType.getAsString());
                    ResourceLocation entryNameLocation = ResourceLocation.tryParse(entryName.getAsString());

                    if (entryTypeLocation == null || entryNameLocation == null) {
                        LOGGER.warn("Malformed JSON avoided.");
                        continue;
                    }

                    // Simplest case: Just add the item.
                    if (Objects.equals(entryTypeLocation.getPath(), "item")) {
                        Item item = ForgeRegistries.ITEMS.getValue(entryNameLocation);
                        ItemStack itemStack = new ItemStack(item);
                        ret.add(itemStack);
                    }
                    // Tricky case: We've got a loot table in a loot table. Extract.
                    else if (Objects.equals(entryTypeLocation.getPath(), "loot_table")) {
                        ret.addAll(extractItemsFromLootTableJsonRecursive(
                                server,
                                entryNameLocation.getNamespace(),
                                entryNameLocation.getPath())
                        );
                    }
                }
            }

        }
        catch (Exception e) {
            LOGGER.error("Shit's fucked");
        }

        return ret;
    }


    // Retrieve all possible drops for the given entity.
    public static ArrayList<ItemStack> getAllDrops(LivingEntity entity) {
        ResourceLocation id = entity.getLootTable();
        return extractItemsFromLootTableJsonRecursive(entity.getServer(), id.getNamespace(), id.getPath());
    }

    // Retrieve all drops of a given entity that correspond to the given tag.
    public static ArrayList<ItemStack> getDropsForTag(LivingEntity entity, TagKey<Item> tag) {
        ArrayList<ItemStack> allStacks = getAllDrops(entity);
        ArrayList<ItemStack> ret = new ArrayList<>();
        for (ItemStack stack : allStacks) {
            if (stack.is(tag)) {
                ret.add(stack);
            }
        }
        return ret;
    }

    public static boolean hasDropsForTag(LivingEntity entity, TagKey<Item> tag) {
        return !getDropsForTag(entity, tag).isEmpty();
    }


}
