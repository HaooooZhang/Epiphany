package ink.myumoon.epiphany.registry;

import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

// 待检查同步情况
public final class EpiphanyAttachmentTypes {

    public static final DeferredRegister<AttachmentType<?>> REGISTRY =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Epiphany.MODID);

    public static final Supplier<AttachmentType<PlayerEpiphanyData>> EPIPHANY_DATA =
            REGISTRY.register("epiphany_data", () ->
                    AttachmentType.builder(PlayerEpiphanyData::createDefault)
                            .serialize(PlayerEpiphanyData.CODEC)            // 通过 Codec 序列化
                            .copyOnDeath()                                  // clone
                            .sync(PlayerEpiphanyData.STREAM_CODEC)          // 我不知道能不能跑，文档说不行来着
                            .build()
            );

    private EpiphanyAttachmentTypes() {
    }
}
