package ink.myumoon.epiphany.client;

import ink.myumoon.epiphany.attachment.EpiphanyPlayerState;
import ink.myumoon.epiphany.attachment.ModulePlayerState;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.content.*;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.Map;

@EventBusSubscriber(value = Dist.CLIENT)
public final class EpiphanyDebugHud {

    private EpiphanyDebugHud() {
    }

    @SubscribeEvent
    static void onRenderOverlay(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        PlayerEpiphanyData data = mc.player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        GuiGraphics g = event.getGuiGraphics();
        int x = 10;
        int y = 10;

        g.drawString(mc.font, "\u00a76\u00a7l=== Epiphany Debug ===", x, y, 0xFFFFFF);
        y += 14;

        // Player state
        g.drawString(mc.font, String.format("§eAptitude: \u00a7f%d  \u00a7ePoints: \u00a7f%d  \u00a7eSpent: \u00a7f%d",
                data.aptitude(), data.insightPoints(), data.totalInsightPointsSpent()), x, y, 0xFFFFFF);
        y += 12;
        g.drawString(mc.font, String.format("§eSlots: \u00a7f%d/%d (used)  \u00a7eMax: \u00a7f%d",
                data.usedEpiphanySlots(), data.epiphanySlots(),
                ink.myumoon.epiphany.Config.MAX_EPIPHANY_SLOTS.get()), x, y, 0xFFFFFF);
        y += 14;

        for (Map.Entry<ResourceLocation, ModulePlayerState> e : data.modules().entrySet()) {
            ModulePlayerState s = e.getValue();
            String st = (s.unlocked() ? "\u00a72U" : "\u00a77u")
                    + (s.selected() ? "\u00a72S" : "\u00a77s")
                    + (s.completed() ? "\u00a72C" : "\u00a77c");
            g.drawString(mc.font, "  " + st + "  " + e.getKey(), x, y, 0xFFFFFF);
            y += 10;
        }
        for (Map.Entry<ResourceLocation, EpiphanyPlayerState> e : data.epiphanies().entrySet()) {
            EpiphanyPlayerState s = e.getValue();
            String st = (s.unlocked() ? "\u00a72U" : "\u00a77u")
                    + (s.selected() ? "\u00a72S" : "\u00a77s");
            g.drawString(mc.font, "  " + st + "  " + e.getKey(), x, y, 0xFFFFFF);
            y += 10;
        }
        y += 4;

        // Registries
        if (mc.getConnection() == null) return;
        var access = mc.getConnection().registryAccess();
        if (access == null) return;

        Registry<ModuleData> modReg = access.registryOrThrow(EpiphanyRegistries.MODULE_REGISTRY_KEY);
        g.drawString(mc.font, "\u00a7d--- Reg Modules (" + modReg.size() + ") ---", x, y, 0xFFFFFF);
        y += 12;
        for (var e : modReg.entrySet()) {
            String init = e.getValue().initialState() == InitialState.SELECTABLE ? "\u00a7aSel" : "\u00a7cLck";
            g.drawString(mc.font, String.format("  \u00a77%s  %s  \u00a78(%d ins)", e.getKey().location(), init, e.getValue().insights().size()), x, y, 0xFFFFFF);
            y += 10;
        }
        y += 2;

        Registry<InsightData> insReg = access.registryOrThrow(EpiphanyRegistries.INSIGHT_REGISTRY_KEY);
        g.drawString(mc.font, "\u00a7d--- Reg Insights (" + insReg.size() + ") ---", x, y, 0xFFFFFF);
        y += 12;
        for (var e : insReg.entrySet()) {
            g.drawString(mc.font, String.format("  \u00a77%s  \u00a78(cost:%d)", e.getKey().location(), e.getValue().cost()), x, y, 0xFFFFFF);
            y += 10;
        }
        y += 2;

        Registry<EpiphanyData> epiReg = access.registryOrThrow(EpiphanyRegistries.EPIPHANY_REGISTRY_KEY);
        g.drawString(mc.font, "\u00a7d--- Reg Epiphanies (" + epiReg.size() + ") ---", x, y, 0xFFFFFF);
        y += 12;
        for (var e : epiReg.entrySet()) {
            String init = e.getValue().initialState() == InitialState.SELECTABLE ? "\u00a7aSel" : "\u00a7cLck";
            g.drawString(mc.font, String.format("  \u00a77%s  %s", e.getKey().location(), init), x, y, 0xFFFFFF);
            y += 10;
        }
        y += 2;

        Registry<PathData> pathReg = access.registryOrThrow(EpiphanyRegistries.PATH_REGISTRY_KEY);
        g.drawString(mc.font, "\u00a7d--- Reg Paths (" + pathReg.size() + ") ---", x, y, 0xFFFFFF);
        y += 12;
        for (var e : pathReg.entrySet()) {
            g.drawString(mc.font, "  \u00a77" + e.getKey().location(), x, y, 0xFFFFFF);
            y += 10;
        }
    }
}
