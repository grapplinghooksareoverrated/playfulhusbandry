package net.apricotdev.playfulhusbandry.genetics;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnimalGeneticsProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static Capability<AnimalGenetics> ANIMAL_GENETICS = CapabilityManager.get(new CapabilityToken<AnimalGenetics>() {});
    private AnimalGenetics animalGenetics = null;
    private final LazyOptional<AnimalGenetics> optional = LazyOptional.of(this::createAnimalGenetics);

    private AnimalGenetics createAnimalGenetics() {
        if (this.animalGenetics == null) {
            this.animalGenetics = new AnimalGenetics();
        }
        return this.animalGenetics;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ANIMAL_GENETICS) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createAnimalGenetics().saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createAnimalGenetics().loadNBTData(nbt);
    }
}
