package ink.myumoon.epiphany.client.ui.progression;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.TreeSet;
import java.util.function.IntConsumer;

/** Bounded, per-pixel preview rendering, following the main UI's custom UIElement pattern. */
final class CurveChartElement extends UIElement {
    static final int[] COLORS = {0xFFDFBE85, 0xFF8DBDC3, 0xFF9BACDB, 0xFFC4A5CF};
    private CurvePreviewDraft.Preview preview;
    private int point = 1;
    private boolean logarithmic;
    private boolean cumulative;
    private final IntConsumer onPointSelected;

    CurveChartElement(IntConsumer onPointSelected) {
        this.onPointSelected = onPointSelected;
        layout(l -> l.flex(1).minHeight(48).minWidth(0));
        style(s -> s.background(new ColorRectTexture(0xFF2F2A34)));
        addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button != 0 || preview == null || preview.end() == 0) return;
            double fraction = (event.x - getContentX() - 32) / Math.max(1, getContentWidth() - 40);
            onPointSelected.accept((int) Math.max(1, Math.min(preview.end(), Math.round(fraction * preview.end()))));
        });
    }

    void setPreview(CurvePreviewDraft.Preview preview, int point, boolean logarithmic, boolean cumulative) {
        this.preview = preview;
        this.point = point;
        this.logarithmic = logarithmic;
        this.cumulative = cumulative;
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        if (preview == null) return;
        float x = getContentX(), y = getContentY(), width = getContentWidth(), height = getContentHeight();
        if (width < 50 || height < 35) return;
        if (preview.end() == 0) {
            text(context, Component.translatable("epiphany.curve.empty"), x + 5, y + height / 2, 0xFFAAAAAA);
            return;
        }
        float left = x + 32, top = y + 12, w = width - 40, h = height - 29;
        int samples = Math.max(2, Math.min(600, (int) w));
        var points = new TreeSet<Integer>();
        points.add(1);
        points.add(preview.end());
        for (int i = 0; i <= samples; i++) points.add(Math.max(1, (int) ((long) preview.end() * i / samples)));
        for (var rule : preview.rules()) {
            if (rule.start() <= preview.end()) {
                points.add(rule.start());
                if (rule.start() > 1) points.add(rule.start() - 1);
            }
        }
        double max = 1;
        for (int k : points) {
            double cost = cost(k);
            if (representable(cost)) max = Math.max(max, cost);
        }
        for (var rule : preview.rules()) {
            if (!cumulative && rule.start() <= preview.end() && rule.function() == CurvePreviewDraft.Function.CONVERGENT)
                max = Math.max(max, rule.value(CurvePreviewDraft.Parameter.TARGET));
        }
        double ceiling = transform(max) * 1.12;
        for (int i = 0; i <= 3; i++) {
            float gy = top + h - h * i / 3;
            rect(context, left, gy, w, 0.5f, 0xFF394047);
            double tick = ceiling * i / 3;
            text(context, Component.literal(shortNumber(logarithmic ? Math.expm1(tick) : tick)), x + 1, gy - 3, 0xFFAAAAAA);
        }
        for (int i = 0; i <= 2; i++) {
            int tick = (int) ((long) preview.end() * i / 2);
            String value = shortNumber(tick);
            float offset = i == 2 ? value.length() * 4 : 0;
            text(context, Component.literal(value), left + w * i / 2 - offset, top + h + 5, 0xFFAAAAAA);
        }
        for (int i = 0; i < preview.rules().size(); i++) {
            var rule = preview.rules().get(i);
            if (rule.start() > preview.end()) break;
            int end = i + 1 < preview.rules().size() ? Math.min(preview.end(), preview.rules().get(i + 1).start() - 1) : preview.end();
            int color = COLORS[i % COLORS.length];
            if (!cumulative && rule.function() == CurvePreviewDraft.Function.CONVERGENT) {
                float gy = (float) (top + h - transform(rule.value(CurvePreviewDraft.Parameter.TARGET)) / ceiling * h);
                for (float dash = px(rule.start(), left, w); dash < px(end, left, w); dash += 6)
                    rect(context, dash, gy, Math.min(3, px(end, left, w) - dash), 0.5f, color);
            }
            float previousX = 0, previousY = 0;
            boolean drawing = false;
            for (int k : points.subSet(rule.start(), true, end, true)) {
                double cost = cost(k);
                if (!representable(cost)) { drawing = false; continue; }
                float cx = px(k, left, w), cy = (float) (top + h - transform(cost) / ceiling * h);
                if (drawing) line(context, previousX, previousY, cx, cy, color);
                else rect(context, cx - 1, cy - 1, 2, 2, color);
                previousX = cx; previousY = cy; drawing = true;
            }
        }
        double cost = cost(point);
        float cx = px(point, left, w);
        rect(context, cx, top, 0.5f, h, 0xFF777777);
        if (representable(cost)) rect(context, cx - 1.5f, (float) (top + h - transform(cost) / ceiling * h) - 1.5f, 3, 3, 0xFFFFFFFF);
    }

    private double cost(int point) {
        return cumulative ? preview.cumulativeCost(point) : preview.rule(point).cost(point);
    }

    private float px(int k, float left, float width) {
        return left + (float) ((double) k / preview.end() * width);
    }

    private double transform(double value) {
        return logarithmic ? Math.log1p(value) : value;
    }

    static boolean representable(double value) {
        return Double.isFinite(value) && value > 0 && value <= 0x1p63;
    }

    static String shortNumber(double value) {
        if (value >= 1_000_000) return String.format(Locale.ROOT, "%.1e", value);
        if (value >= 10_000) return String.format(Locale.ROOT, "%.1fk", value / 1_000);
        return String.format(Locale.ROOT, "%.0f", value);
    }

    private static void line(GUIContext context, float x1, float y1, float x2, float y2, int color) {
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1))));
        for (int i = 0; i <= steps; i++) rect(context, x1 + (x2 - x1) * i / steps, y1 + (y2 - y1) * i / steps, 1, 1, color);
    }

    private static void rect(GUIContext context, float x, float y, float w, float h, int color) {
        context.drawTexture(new ColorRectTexture(color), x, y, w, h);
    }

    private static void text(GUIContext context, Component text, float x, float y, int color) {
        context.graphics.pose().pushPose();
        context.graphics.pose().translate(x, y, 0);
        context.graphics.pose().scale(0.75f, 0.75f, 1);
        context.graphics.drawString(context.mc.font, text, 0, 0, color, false);
        context.graphics.pose().popPose();
    }
}
