package ink.myumoon.epiphany.content.reward;

import net.minecraft.resources.ResourceLocation;

/** Stable owner context for a reward invocation. */
public record RewardSource(
        String ownerKind,
        ResourceLocation ownerId,
    String rewardSlot,
    ResourceLocation legacyId
) {
    public RewardSource(String ownerKind, ResourceLocation ownerId, String rewardSlot) {
        this(ownerKind, ownerId, rewardSlot, ResourceLocation.fromNamespaceAndPath(
                ownerId.getNamespace(), ownerId.getPath() + "/" + rewardSlot));
    }
}