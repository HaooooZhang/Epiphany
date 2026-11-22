package ink.myumoon.epiphany.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;

/** Datapack text uses vanilla JSON. The login registries carry that JSON as a string inside NBT. */
public final class ComponentCodec {
    public static final Codec<Component> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<Component, T>> decode(DynamicOps<T> ops, T input) {
            return codecFor(ops).decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(Component value, DynamicOps<T> ops, T prefix) {
            return codecFor(ops).encode(value, ops, prefix);
        }
    };

    private static Codec<Component> codecFor(DynamicOps<?> ops) {
        // Routing component JSON through NbtOps otherwise converts style booleans to numbers.
        return ops.empty() instanceof Tag ? ExtraCodecs.FLAT_COMPONENT : ExtraCodecs.COMPONENT;
    }

    private ComponentCodec() {
    }
}
