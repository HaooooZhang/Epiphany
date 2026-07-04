package ink.myumoon.epiphany.registry;

import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

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
