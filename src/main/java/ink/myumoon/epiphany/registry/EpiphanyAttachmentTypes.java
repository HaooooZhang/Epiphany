package ink.myumoon.epiphany.registry;

import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Player-data attachment types for the Epiphany system.
 * <p>
 * {@code EPIPHANY_DATA} is configured with {@code .serialize() + .copyOnDeath() + .sync()}
 * — the standard NeoForge combo. {@code .sync(STREAM_CODEC)} is fully supported
 * (verified on LDLib2 2.2.26): it makes {@code player.getData(EPIPHANY_DATA)} return
 * the authoritative server snapshot on the client as well.
 */
public final class EpiphanyAttachmentTypes {

    public static final DeferredRegister<AttachmentType<?>> REGISTRY =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Epiphany.MODID);

    public static final Supplier<AttachmentType<PlayerEpiphanyData>> EPIPHANY_DATA =
            REGISTRY.register("epiphany_data", () ->
                    AttachmentType.builder(PlayerEpiphanyData::createDefault)
                            .serialize(PlayerEpiphanyData.CODEC)            // NBT persistence
                            .copyOnDeath()                                  // keep across deaths
                            .sync(PlayerEpiphanyData.STREAM_CODEC)          // S -> C automatic mirror
                            .build()
            );

    private EpiphanyAttachmentTypes() {
    }
}
