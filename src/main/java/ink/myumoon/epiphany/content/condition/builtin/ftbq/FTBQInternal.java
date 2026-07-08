package ink.myumoon.epiphany.content.condition.builtin.ftbq;

import com.mojang.logging.LogUtils;
import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import dev.ftb.mods.ftbquests.quest.*;
import ink.myumoon.epiphany.api.EpiphanyManager;
import ink.myumoon.epiphany.api.ModuleManager;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Internal bridge to FTB Quests API. Package-private — only {@link FTBQHelper} calls here.
 * <p>
 * <b>CRITICAL:</b> This class must NOT be referenced from any other package.
 * The JVM only loads it when {@link FTBQHelper} enters its {@code if (LOADED)} branch,
 * guaranteeing that FTB Quests classes are on the classpath at that point.
 * <p>
 * Uses the stable public API ({@link FTBQuestsAPI}) rather than internal classes
 * like {@code ServerQuestFile} which may vary between Forge/NeoForge builds.
 */
final class FTBQInternal {

    private static final Logger LOGGER = LogUtils.getLogger();

    private FTBQInternal() {
    }

    @Nullable
    private static BaseQuestFile questFile() {
        try {
            return FTBQuestsAPI.api().getQuestFile(false);
        } catch (Exception e) {
            LOGGER.warn("FTBQInternal: failed to get quest file", e);
            return null;
        }
    }

    static boolean isQuestCompleted(ServerPlayer player, long questId) {
        BaseQuestFile file = questFile();
        if (file == null) return false;
        TeamData data = file.getTeamData(player).orElse(null);
        if (data == null) return false;
        Quest quest = file.getQuest(questId);
        if (quest == null) {
            LOGGER.warn("FTBQ quest {} not found for epiphany:ftbq_quest condition (player={})", questId, player.getGameProfile().getName());
            return false;
        }
        return data.isCompleted(quest);
    }

    static boolean isChapterStarted(ServerPlayer player, long chapterId) {
        BaseQuestFile file = questFile();
        if (file == null) return false;
        TeamData data = file.getTeamData(player).orElse(null);
        if (data == null) return false;
        Chapter chapter = file.getChapter(chapterId);
        if (chapter == null) {
            LOGGER.warn("FTBQ chapter {} not found for epiphany:ftbq_chapter_started condition (player={})", chapterId, player.getGameProfile().getName());
            return false;
        }
        return data.isStarted(chapter);
    }

    static boolean isChapterCompleted(ServerPlayer player, long chapterId) {
        BaseQuestFile file = questFile();
        if (file == null) return false;
        TeamData data = file.getTeamData(player).orElse(null);
        if (data == null) return false;
        Chapter chapter = file.getChapter(chapterId);
        if (chapter == null) {
            LOGGER.warn("FTBQ chapter {} not found for epiphany:ftbq_chapter_completed condition (player={})", chapterId, player.getGameProfile().getName());
            return false;
        }
        return data.isCompleted(chapter);
    }

    static int countCompletedQuestsWithTag(ServerPlayer player, String tag) {
        BaseQuestFile file = questFile();
        if (file == null) return 0;
        TeamData data = file.getTeamData(player).orElse(null);
        if (data == null) return 0;
        int[] count = {0};
        file.forAllQuests(quest -> {
            if (quest.hasTag(tag) && data.isCompleted(quest)) {
                count[0]++;
            }
        });
        return count[0];
    }

    // ============================================================
    // Architectury event listeners — instant auto-unlock on FTBQ completion
    // ============================================================

    static void registerEvents() {
        // Behavior ids for aptitude_source datapack entries (looked up by listener side)
        final var FTBQ_QUEST_COMPLETE =
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("epiphany", "ftbq_quest_complete");
        final var FTBQ_CHAPTER_COMPLETE =
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("epiphany", "ftbq_chapter_complete");

        ObjectCompletedEvent.QUEST.register(event -> {
            String code = event.getObject().getCodeString();
            // FTBQ code strings are uppercase hex (e.g. "6D031F64B5DEFA7C"); lowercase for ResourceLocation
            var target = net.minecraft.resources.ResourceLocation.tryParse(code.toLowerCase(java.util.Locale.ROOT));
            for (var player : event.getOnlineMembers()) {
                ModuleManager.checkAutoUnlock(player);
                EpiphanyManager.checkAutoUnlock(player);
                if (target != null) {
                    ink.myumoon.epiphany.api.AptitudeSourceManager.grant(player, FTBQ_QUEST_COMPLETE, target, null);
                }
            }
            return EventResult.pass();
        });

        ObjectCompletedEvent.CHAPTER.register(event -> {
            String code = event.getObject().getCodeString();
            var target = net.minecraft.resources.ResourceLocation.tryParse(code.toLowerCase(java.util.Locale.ROOT));
            for (var player : event.getOnlineMembers()) {
                ModuleManager.checkAutoUnlock(player);
                EpiphanyManager.checkAutoUnlock(player);
                if (target != null) {
                    ink.myumoon.epiphany.api.AptitudeSourceManager.grant(player, FTBQ_CHAPTER_COMPLETE, target, null);
                }
            }
            return EventResult.pass();
        });

        LOGGER.info("FTBQInternal: registered Architectury event listeners for quest/chapter completion");
    }
}
