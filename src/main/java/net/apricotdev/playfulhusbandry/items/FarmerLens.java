package net.apricotdev.playfulhusbandry.items;

import net.apricotdev.playfulhusbandry.genetics.AnimalGeneticsProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FarmerLens extends Item {
    public FarmerLens(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack p_41398_, Player p_41399_, LivingEntity p_41400_, InteractionHand p_41401_) {

        if (!p_41399_.level.isClientSide()) {
            if (p_41400_ instanceof Animal) {
                // Check if animal has animal genetics provider.
                p_41400_.getCapability(AnimalGeneticsProvider.ANIMAL_GENETICS).ifPresent(animalGenetics -> {
                    // Generate components
                    var headerOutput = Component.empty();
                    var safetyOutput = Component.empty();
                    var tagDetailOutput = Component.empty();
                    var incestOutput = Component.empty();

                    // Define header
                    headerOutput
                            .append("-- ")
                            .append(p_41400_.getName())
                            .append(" ------------")
                    ;

                    // Define safety
                    var offsetsApplied = animalGenetics.getOffsetsApplied();
                    var maxSafeApplications = animalGenetics.getMaxSafeApplications();

                    // Color # of offsets applied
                    var offsetsAppliedComponent = Component.empty().append(String.valueOf(offsetsApplied));
                    var warningComponent = Component.empty();
                    if (offsetsApplied > maxSafeApplications) {
                        offsetsAppliedComponent = offsetsAppliedComponent.withStyle(ChatFormatting.RED);
                        warningComponent = warningComponent
                                .append("\n")
                                .append(Component.translatable("message.playfulhusbandry.heirlooms_exceeded_warning"))
                                .withStyle(ChatFormatting.RED);
                    }

                    safetyOutput = safetyOutput
                            .append(offsetsAppliedComponent)
                            .append("/")
                            .append(String.valueOf(maxSafeApplications))
                            .append( Component.translatable("message.playfulhusbandry.heirlooms_applied") )
                            .append(warningComponent)
                    ;

                    // Define tag detail
                    var tagOffsets = animalGenetics.getTagOffsets();
                    var ctr = 0;
                    var size = tagOffsets.size();
                    for (TagKey<Item> key : tagOffsets.keySet()) {
                        // Adds plus sign to non negative values
                        Integer offsetMagnitude = tagOffsets.get(key);
                        String offsetDisplay = ( offsetMagnitude >= 0 ? "+" : "" ) + offsetMagnitude;
                        tagDetailOutput
                                // Include magnitude of offset
                                .append( Component.literal( offsetDisplay ) )
                                // Hyphen
                                .append( Component.literal( " to " ) )
                                // Include human-readable tag name
                                .append( Component.translatable(key.location().toString()) )
                        ;

                        // Quick and dirty way of adding newlines to all but the last item
                        if (ctr < size-1) {
                            tagDetailOutput.append("\n");
                        }
                        ctr++;

                    }

                    if ( animalGenetics.isProductOfIncest() ) {
                        incestOutput = incestOutput
                                .append("\n")
                                .append(Component.translatable("message.playfulhusbandry.incest_warning").withStyle(ChatFormatting.YELLOW))
                        ;
                    }



                    p_41399_.sendSystemMessage(
                            Component.empty()
                                    .append(headerOutput)
                                    .append("\n")
                                    .append(tagDetailOutput)
                                    .append("\n")
                                    .append(safetyOutput)
                                    .append(incestOutput)
                    );


                });
                return InteractionResult.SUCCESS;
            }
            else if ( p_41400_ instanceof WaterAnimal) {
                p_41399_.sendSystemMessage( Component.translatable("message.playfulhusbandry.farmer_lens.water_animal").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY) );
                return InteractionResult.FAIL;
            }
            else {
                p_41399_.sendSystemMessage( Component.translatable("message.playfulhusbandry.farmer_lens.invalid_target").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY) );
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.PASS;
    }


}
