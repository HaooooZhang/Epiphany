package ink.myumoon.epiphany.attachment;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.network.NetworkHooks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class EpiphanyPlayerDataTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        try (var loading = mockStatic(LoadingModList.class);
             var networkHooks = mockStatic(NetworkHooks.class)) {
            loading.when(LoadingModList::get).thenReturn(mock(LoadingModList.class));
            Bootstrap.bootStrap();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void invalidNbtReportsTheLoadedPlayerUuidAndPreservesProgress() throws ReflectiveOperationException {
        ServerPlayer player = mock(ServerPlayer.class);
        var event = new AttachCapabilitiesEvent<>(Entity.class, player);
        var attach = EpiphanyPlayerData.class.getDeclaredMethod("attach", AttachCapabilitiesEvent.class);
        attach.setAccessible(true);
        attach.invoke(null, event);
        var id = ResourceLocation.fromNamespaceAndPath("epiphany", "epiphany_data");
        var provider = (ICapabilitySerializable<Tag>) event.getCapabilities().get(id);

        UUID loadedId = UUID.fromString("e47b3aec-8c44-45bb-8979-1f50c53fb292");
        when(player.getUUID()).thenReturn(loadedId);
        var valid = assertInstanceOf(CompoundTag.class, provider.serializeNBT());
        valid.putLong("aptitude", 42L);
        provider.deserializeNBT(valid);
        assertEquals(valid, provider.serializeNBT());

        for (Tag invalid : List.of(new CompoundTag(), StringTag.valueOf("invalid"))) {
            var error = assertThrows(IllegalStateException.class, () -> provider.deserializeNBT(invalid));
            assertTrue(error.getMessage().contains(id.toString()));
            assertTrue(error.getMessage().contains(loadedId.toString()));
            assertInstanceOf(RuntimeException.class, error.getCause());
            assertEquals(valid, provider.serializeNBT());
        }
    }
}
