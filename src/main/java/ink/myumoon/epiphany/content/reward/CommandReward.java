package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Executes a command as the player.
 * <p>
 * JSON: {@code {"type": "epiphany:command", "command": "say hello"}}
 */
public record CommandReward(String command) implements InsightReward, EpiphanyReward {

    public static final MapCodec<CommandReward> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.STRING.fieldOf("command").forGetter(CommandReward::command)
    ).apply(instance, CommandReward::new));

    @Override
    public MapCodec<CommandReward> codec() {
        return CODEC;
    }

    @Override
    public void apply(ServerPlayer player, ResourceLocation sourceId) {
        player.server.getCommands().performPrefixedCommand(
                player.createCommandSourceStack(), command);
    }

    @Override
    public void remove(ServerPlayer player, ResourceLocation sourceId) {
        // Cannot undo a command execution.
    }
}
