package ink.myumoon.epiphany.content.condition.builtin.ftbq;

import com.mojang.logging.LogUtils;
import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import dev.ftb.mods.ftbquests.events.ObjectStartedEvent;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbquests.quest.*;
import ink.myumoon.epiphany.api.AptitudeSourceManager;
import ink.myumoon.epiphany.api.EpiphanyManager;
import ink.myumoon.epiphany.api.ModuleManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.FakePlayer;

import java.util.Locale;
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
        return FTBQuestsAPI.api().getQuestFile(false);
    }

    @Nullable
    private static TeamData teamData(BaseQuestFile file, ServerPlayer player) {
        var teams = FTBTeamsAPI.api();
        if (!teams.isManagerLoaded()) return null;
        return teams.getManager().getTeamForPlayer(player)
                .map(Team::getId).map(file::getNullableTeamData).orElse(null);
    }

    static boolean isQuestCompleted(ServerPlayer player, long questId) {
        BaseQuestFile file = questFile();
        if (file == null) return false;
        TeamData data = teamData(file, player);
        if (data == null) return false;
        Quest quest = file.getQuest(questId);
        return quest != null && data.isCompleted(quest);
    }

    static boolean isChapterStarted(ServerPlayer player, long chapterId) {
        BaseQuestFile file = questFile();
        if (file == null) return false;
        TeamData data = teamData(file, player);
        if (data == null) return false;
        Chapter chapter = file.getChapter(chapterId);
        return chapter != null && data.isStarted(chapter);
    }

    static boolean isChapterCompleted(ServerPlayer player, long chapterId) {
        BaseQuestFile file = questFile();
        if (file == null) return false;
        TeamData data = teamData(file, player);
        if (data == null) return false;
        Chapter chapter = file.getChapter(chapterId);
        return chapter != null && data.isCompleted(chapter);
    }

    static int countCompletedQuestsWithTag(ServerPlayer player, String tag) {
        BaseQuestFile file = questFile();
        if (file == null) return 0;
        TeamData data = teamData(file, player);
        if (data == null) return 0;
        int[] count = {0};
        file.forAllQuests(quest -> {
            if (quest.hasTag(tag) && data.isCompleted(quest)) {
                count[0]++;
            }
        });
        return count[0];
    }

    private static final ResourceLocation QUEST_COMPLETE =
            ResourceLocation.fromNamespaceAndPath("epiphany", "ftbq_quest_complete");
    private static final ResourceLocation CHAPTER_COMPLETE =
            ResourceLocation.fromNamespaceAndPath("epiphany", "ftbq_chapter_complete");
    private static boolean registered;

    static synchronized void registerEvents() {
        if (registered) return;
        ObjectStartedEvent.CHAPTER.register(event -> {
            for (var player : event.getOnlineMembers()) {
                if (canHandle(player)) checkUnlocks(player);
            }
            return EventResult.pass();
        });
        ObjectCompletedEvent.QUEST.register(event ->
                onCompleted(event.getOnlineMembers(), event.getObject().getCodeString(), QUEST_COMPLETE));
        ObjectCompletedEvent.CHAPTER.register(event ->
                onCompleted(event.getOnlineMembers(), event.getObject().getCodeString(), CHAPTER_COMPLETE));
        registered = true;
        LOGGER.info("Registered Epiphany FTB Quests chapter-start and completion bridges");
    }

    private static EventResult onCompleted(Iterable<ServerPlayer> players, String code, ResourceLocation behavior) {
        var target = ResourceLocation.tryParse(code.toLowerCase(Locale.ROOT));
        for (var player : players) {
            if (!canHandle(player)) continue;
            checkUnlocks(player);
            if (target != null) AptitudeSourceManager.grant(player, behavior, target, null);
        }
        return EventResult.pass();
    }

    private static boolean canHandle(ServerPlayer player) {
        return player.isAlive() && !(player instanceof FakePlayer);
    }

    private static void checkUnlocks(ServerPlayer player) {
        ModuleManager.checkAutoUnlock(player);
        EpiphanyManager.checkAutoUnlock(player);
    }
}
