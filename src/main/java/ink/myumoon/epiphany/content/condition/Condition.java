package ink.myumoon.epiphany.content.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import ink.myumoon.epiphany.content.condition.logic.AlwaysCondition;
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
 * Extensible via the {@link EpiphanyRegistries#CONDITION_SERIALIZERS} registry
 * — other mods can register new condition types via {@code DeferredRegister}.
 */
public interface Condition {

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

    /**
     * Whether this condition is driven by external events rather than polling.
     * Event-driven conditions may be skipped by periodic auto-unlock checks
     * to avoid expensive queries. FTBQ conditions use Architectury event listeners.
     */
    default boolean isEventDriven() {
        return false;
    }
}
