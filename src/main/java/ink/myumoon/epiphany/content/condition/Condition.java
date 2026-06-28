package ink.myumoon.epiphany.content.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import ink.myumoon.epiphany.util.DefaultedCodec;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Function;

/**
 * A polymorphic condition that can be evaluated against a player.
 * <p>
 * Uses NeoForge's dispatch pattern (same as {@code ICondition}):
 * the {@code "type"} field in JSON maps to a registry key,
 * which resolves to the corresponding {@link MapCodec} for deserialization.
 * <p>
 * Sealed hierarchy includes combinators (AND/OR/NOT), built-in checks
 * (advancement, always, never), and is extensible via the
 * {@link EpiphanyRegistries#CONDITION_SERIALIZERS} registry.
 */
public sealed interface Condition
        permits AndCondition, OrCondition, NotCondition,
                AdvancementCondition, AlwaysCondition, NeverCondition {

    Codec<Condition> CODEC = DefaultedCodec.registryDispatch(
            EpiphanyRegistries.CONDITION_SERIALIZERS,
            Condition::codec,
            Function.identity(),
            () -> AlwaysCondition.INSTANCE
    );

    /**
     * Returns the {@link MapCodec} that (de)serializes this concrete condition type.
     */
    MapCodec<? extends Condition> codec();

    /**
     * Evaluates this condition tree against the given player.
     *
     * @param player the server-side player to test
     * @return true if the condition is satisfied
     */
    boolean test(ServerPlayer player);
}
