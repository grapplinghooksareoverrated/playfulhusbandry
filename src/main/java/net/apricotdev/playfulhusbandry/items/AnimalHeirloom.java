package net.apricotdev.playfulhusbandry.items;

import com.mojang.logging.LogUtils;
import net.apricotdev.playfulhusbandry.genetics.AnimalGeneticsProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

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


    @Override
    public InteractionResult interactLivingEntity(ItemStack p_41398_, Player p_41399_, LivingEntity p_41400_, InteractionHand p_41401_) {

        if (!p_41399_.level.isClientSide()) {
            if (p_41400_ instanceof Animal) {
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
