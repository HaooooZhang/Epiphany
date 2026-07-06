package ink.myumoon.epiphany.content.condition.builtin.ftbq;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/**
 * Public entry point for all FTB Quests condition checks.
 * <p>
 * All FTBQ API calls are isolated in the package-private {@link FTBQInternal} class.
 * This class only references {@link FTBQInternal} after verifying that the
 * {@code ftbquests} mod is loaded, preventing {@link NoClassDefFoundError}
 * when FTB Quests is not installed.
 * <p>
 * Use via <b>compileOnly</b> dependency — FTBQ classes are available at compile time
 * but the JVM only loads {@link FTBQInternal} if {@code isLoaded()} returns true.
 */
public final class FTBQHelper {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean LOADED = ModList.get().isLoaded("ftbquests");

    static {
        if (LOADED) {
            LOGGER.info("FTBQHelper: ftbquests mod detected — FTBQ conditions enabled");
        }
    }

    private FTBQHelper() {
    }

    /**
     * Registers Architectury event listeners for FTB Quests progress events.
     * Must be called during mod init if FTBQ is present.
     */
    public static void init() {
        if (LOADED) {
            FTBQInternal.registerEvents();
        }
    }

    /**
     * Parses an FTBQ hex ID string (e.g. {@code "6D031F64B5DEFA7C"}) to a long.
     * Returns 0 on malformed input.
     */
    private static long parseHex(String hex) {
        try {
            return Long.parseUnsignedLong(hex, 16);
        } catch (NumberFormatException e) {
            LOGGER.warn("FTBQHelper: invalid hex id '{}'", hex);
            return 0L;
        }
    }

    public static boolean isQuestCompleted(ServerPlayer player, String questHex) {
        if (!LOADED) return false;
        return FTBQInternal.isQuestCompleted(player, parseHex(questHex));
    }

    public static boolean isChapterStarted(ServerPlayer player, String chapterHex) {
        if (!LOADED) return false;
        return FTBQInternal.isChapterStarted(player, parseHex(chapterHex));
    }

    public static boolean isChapterCompleted(ServerPlayer player, String chapterHex) {
        if (!LOADED) return false;
        return FTBQInternal.isChapterCompleted(player, parseHex(chapterHex));
    }

    public static int countCompletedQuestsWithTag(ServerPlayer player, String tag) {
        if (!LOADED) return 0;
        return FTBQInternal.countCompletedQuestsWithTag(player, tag);
    }
}
