package net.apricotdev.playfulhusbandry.items;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.apricotdev.playfulhusbandry.LivingEntityLootIntrospectionTool;
import net.apricotdev.playfulhusbandry.Config;
import net.apricotdev.playfulhusbandry.genetics.AnimalGeneticsProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;

public class AnimalHeirloom extends Item {
    public AnimalHeirloom(Properties properties) {
        super(properties);
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    private TagKey<Item> tag;
    private final int magnitude = 1;

    public AnimalHeirloom(Properties properties, TagKey<Item> tag) {
        super(properties);
        this.tag = tag;
    }

    // This function checks if it's theoretically possible
    // for the mob to drop an item that matches this heirloom tag

    private boolean entityDropsItemOfTag(LivingEntity entity) {
        try {
            // Load loot table JSON file for given entity
            ResourceLocation id = entity.getLootTable();
            ResourceLocation fileLocation = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "loot_tables/"+id.getPath()+".json");
            Resource resource = entity.getServer().getResourceManager()
                    .getResource(fileLocation)
                    .orElseThrow(() -> new FileNotFoundException(fileLocation.toString()));
            BufferedReader reader = resource.openAsReader();
            // Iterate through all LootPools of this Entity.
            JsonElement fileRootElement = JsonParser.parseReader(reader);
            // Double check if the element is a JsonObject. If not, return early.
            if (!fileRootElement.isJsonObject()) {
                LOGGER.warn("The file {} couldn't be converted to a JsonObject. Maybe it's corrupted?", fileLocation.toString());
                return false;
            }
            JsonObject fileRoot = fileRootElement.getAsJsonObject();
            JsonElement poolsElement = fileRoot.get("pools");
            // Double check if the pools element is a JsonArray.
            if (poolsElement == null || !poolsElement.isJsonArray()) {
                return false;
            }
            JsonArray pools = poolsElement.getAsJsonArray();
            for (JsonElement pool : pools) {
                JsonArray entries = pool.getAsJsonObject().get("entries").getAsJsonArray();
                for (JsonElement entry : entries) {
                    // Reverse engineer an ItemStack from each given item.
                    var entryObject = entry.getAsJsonObject();
                    var fullName = entryObject.get("name").getAsString(); // NOTE: formatted as "namespace:item"
                    ResourceLocation itemId = ResourceLocation.tryParse(fullName);
                    Item item = ForgeRegistries.ITEMS.getValue(itemId);
                    ItemStack itemStack = new ItemStack(item);
                    // Check if the given itemStack matches this Heirloom's tag. If so, then return true.
                    if (itemStack.is(tag)) {
                        return true;
                    }
                }
            }
        }
        catch (Exception e) {
            LOGGER.warn(e.getMessage());
        }
        return false;
    }


    @Override
    public InteractionResult interactLivingEntity(ItemStack p_41398_, Player p_41399_, LivingEntity p_41400_, InteractionHand p_41401_) {
        if (!p_41399_.level.isClientSide()) {
            if (p_41400_ instanceof Animal) {

                // Verify that the animal's loot table contains at least one item corresponding to this heirloom's tag.
                // If "heirloom_loot_check" is set to false, this is ignored.
                if (Config.heirloom_loot_check && !LivingEntityLootIntrospectionTool.hasDropsForTag(p_41400_, tag)) {
                    p_41399_.sendSystemMessage(
                            Component.translatable("item.playfulhusbandry.heirloom.does_nothing_for_given_animal")
                                    .withStyle(ChatFormatting.ITALIC)
                                    .withStyle(ChatFormatting.GRAY)
                    );
                    return InteractionResult.FAIL;
                }

                // If animal contains an AnimalGenetics provider, the code is executed.
                p_41400_.getCapability(AnimalGeneticsProvider.ANIMAL_GENETICS).ifPresent(animalGenetics -> {
                    // Apply offset
                    var oldOffsetsApplied = animalGenetics.getOffsetsApplied();
                    animalGenetics.applyOffset(this.tag, magnitude, p_41400_);
                    var offsetsDelta = animalGenetics.getOffsetsApplied() - oldOffsetsApplied;
                    // If animal didn't explode, then announce status.
                    if (!animalGenetics.hasExploded()) {
                        // Broadcast action
                        var offsetsApplied = animalGenetics.getOffsetsApplied();
                        var maxSafeApplications = animalGenetics.getMaxSafeApplications();
                        var signage = (offsetsDelta <= 0 ? "" : "+");
                        var outputComponent = Component.empty()
                                .append(Component.translatable("message.playfulhusbandry.give_hierloom_0"))
                                .append(getName(p_41398_))
                                .append(Component.translatable("message.playfulhusbandry.give_hierloom_1"))
                                .append(p_41400_.getName());

                        // Extra feedback
                        if (offsetsApplied == maxSafeApplications) {
                            outputComponent = outputComponent
                                    .append("\n")
                                    .append(
                                            Component.translatable("message.playfulhusbandry.heirlooms_limit_met")
                                                    .withStyle(style -> style.withColor(TextColor.fromRgb(0x87e9ff)))
                                    );
                        } else if (offsetsApplied > maxSafeApplications) {
                            outputComponent = outputComponent
                                    .append("\n")
                                    .append(
                                            Component.translatable("message.playfulhusbandry.heirlooms_exceeded_warning")
                                                    .withStyle(ChatFormatting.RED)
                                    );
                        }
                        p_41399_.sendSystemMessage(
                                outputComponent
                        );
                    }

                });
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}
