package net.apricotdev.playfulhusbandry.genetics;

import com.mojang.logging.LogUtils;
import net.apricotdev.playfulhusbandry.Config;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Explosion;
import org.slf4j.Logger;

import java.util.*;


public class AnimalGenetics {
    // Tracks what drops' counts are intended to be offset.
    private Map<TagKey<Item>, Integer> tagOffsets = new HashMap<>();
    // Tracks how many times the animal's stats are changed.
    private int offsetsApplied = 0;
    private int maxSafeApplications = 5;
    private boolean doNotDropLoot = false;
    private boolean explodedAlready = false;
    private int parentAId;
    private int parentBId;
    private static int naturalParentId = -1;
    private boolean incestuousBirth;

    public AnimalGenetics() {
        parentAId = naturalParentId--;
        parentBId = naturalParentId--;
    }

    public boolean isProductOfIncest() {
        return incestuousBirth;
    }

    public int getParentAId() {
        return parentAId;
    }

    public int getParentBId() {
        return parentBId;
    }

    public Map<TagKey<Item>, Integer> getTagOffsets() {
        return tagOffsets;
    }

    public int getMaxSafeApplications() {
        return maxSafeApplications;
    }

    public int getOffsetsApplied() {
        return offsetsApplied;
    }

    public boolean shouldNotDropLoot() {
        return doNotDropLoot;
    }

    public boolean hasExploded() {
        return explodedAlready;
    }

    private static final Logger LOGGER = LogUtils.getLogger();


    public void applyOffset(TagKey<Item> tag, int magnitude, LivingEntity entity) {

        if (tagOffsets.containsKey(tag)) {
            var existingValue = tagOffsets.get(tag);
            tagOffsets.put(tag, existingValue+magnitude);
        }
        else {
            tagOffsets.put(tag, magnitude);
        }

        onOffsetApplied(entity);

    }

    private void onOffsetApplied(LivingEntity entity) {
        // Execute only on server side
        if (entity.level.isClientSide()) return;
        if (explodedAlready) return;
        offsetsApplied++;

        // (Maybe) Explode
        if (offsetsApplied > maxSafeApplications) {
            // Explosion probability increases based on how severely the max has been surpassed
            var random = new Random();
            int randomNumber = random.nextInt(101);
            int maxTolerableValue = 100;
            // 50% chance
            if (offsetsApplied == maxSafeApplications+1) {
                maxTolerableValue = 50;
            }
            // 25% chance
            else if (offsetsApplied == maxSafeApplications+2) {
                maxTolerableValue = 25;
            }
            // 1% chance
            else {
                maxTolerableValue = 1;
            }

            // Set explosion power based on entity
            var explosionPower = Config.animal_explosion_radius;
            // Make the chicken explode even bigger because why not lmao
            if ( entity instanceof Chicken) {
                explosionPower = Config.chicken_explosion_radius;
            }

            // Trigger explosion
            if (randomNumber > maxTolerableValue) {
                // Don't drop loot
                doNotDropLoot = true;
                // Cause an explosion
                entity.level.explode(
                        entity,
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        explosionPower,
                        false, // Don't set everything on fire lmao
                        Explosion.BlockInteraction.DESTROY
                );
                entity.hurt( DamageSource.explosion(entity), Float.MAX_VALUE );
                explodedAlready = true;
            }
        }
    }

    public void setAsNewborn(LivingEntity child, int parentAId, int parentBId, AnimalGenetics parentAGenetics, AnimalGenetics parentBGenetics) {

        var offsetsA = parentAGenetics.getTagOffsets();
        var offsetsB = parentBGenetics.getTagOffsets();
        var keys = new ArrayList<TagKey<Item>>();
        var newMap = new HashMap<TagKey<Item>, Integer>();

        // Add ancestry
        this.parentAId = parentAId;
        this.parentBId = parentBId;

        // Check for incest
        var parentalIncest =
                this.parentAId == parentBGenetics.getParentAId() || this.parentAId == parentBGenetics.getParentBId() ||
                        this.parentBId == parentAGenetics.getParentAId() || this.parentBId == parentAGenetics.getParentBId()
                ;
        var siblingIncest =
                parentAGenetics.getParentAId() == parentBGenetics.getParentAId() || // A-A
                        parentAGenetics.getParentAId() == parentBGenetics.getParentBId() || // A-B
                        parentAGenetics.getParentBId() == parentAGenetics.getParentAId() || // B-A
                        parentAGenetics.getParentBId() == parentBGenetics.getParentBId()    // B-B
                ;

        if (parentalIncest || siblingIncest) {
            incestuousBirth = true;
        }

        // Add keys from A
        for ( var key : offsetsA.keySet() ) {
            if (!keys.contains(key)) {
                keys.add(key);
            }
        }

        // Add keys from B
        for ( var key : offsetsB.keySet() ) {
            if (!keys.contains(key)) {
                keys.add(key);
            }
        }

        // Calculate averages
        for ( var key : keys ) {
            float offA = offsetsA.getOrDefault(key, 0);
            float offB = offsetsB.getOrDefault(key, 0);
            int avg = (int)Math.ceil( (offA+offB)/2 );
            newMap.put(key, avg);
        }

        // Mutate a randomly selected stat
        if (incestuousBirth && !newMap.isEmpty()) {
            // Mutate random stat
            var keysSize = keys.size();
            var random = new Random();
            var targetIndex = random.nextInt(keysSize);
            var targetKey = keys.get(targetIndex);
            var statDelta = random.nextInt(Config.incest_mutation_min, Config.incest_mutation_max+1);
            newMap.put( targetKey, newMap.get(targetKey)+statDelta );
        }

        // Take the lower of the two parents' max safe applications
        var maxA = parentAGenetics.getMaxSafeApplications();
        var maxB = parentBGenetics.getMaxSafeApplications();
        this.maxSafeApplications = Math.min(maxA, maxB);
        this.maxSafeApplications -= 1;
        if (this.maxSafeApplications < 0) {
            maxSafeApplications = 0;
        }

        // Record
        this.tagOffsets = newMap;

    }


    public void copyFrom(AnimalGenetics other) {
        tagOffsets.putAll(other.tagOffsets);
        offsetsApplied = other.offsetsApplied;
        doNotDropLoot = other.doNotDropLoot;
        explodedAlready = other.explodedAlready;
    }

    public void saveNBTData(CompoundTag tag) {
        var offsetsCompoundTag = new CompoundTag();
        for (var key : tagOffsets.keySet()) {
            offsetsCompoundTag.putInt(key.location().toString(), tagOffsets.get(key));
        }
        tag.put("TagOffsets", offsetsCompoundTag);
        tag.putInt("OffsetsApplied", offsetsApplied);
        tag.putBoolean("doNotDropLoot", doNotDropLoot);
        tag.putBoolean("explodedAlready", explodedAlready);
        tag.putBoolean("incestuousBirth", incestuousBirth);
    }

    public void loadNBTData(CompoundTag tag) {

        // Deserialize tags (String -> TagKey<Item>), store in tagOffsets 
        try {
            var serializedOffsets = (CompoundTag)tag.get("TagOffsets");
            for (String key : serializedOffsets.getAllKeys()) {
                TagKey<Item> keyAsTag = TagKey.create(
                        Registry.ITEM_REGISTRY,
                        ResourceLocation.parse(key)
                );
                tagOffsets.put( keyAsTag, serializedOffsets.getInt(key) );
            }
        }
        catch (Exception e) {
            LOGGER.warn(e.getMessage());
            tagOffsets.clear();
        }


        offsetsApplied = tag.getInt("OffsetsApplied");
        doNotDropLoot = tag.getBoolean("doNotDropLoot");
        explodedAlready = tag.getBoolean("explodedAlready");
        incestuousBirth = tag.getBoolean("incestuousBirth");
    }

}