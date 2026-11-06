package ink.myumoon.epiphany.client.ui.progression;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import ink.myumoon.epiphany.config.ProgressionRules;

/** Fullscreen Java-only LDLib editor for the authoritative progression configuration. */
@OnlyIn(Dist.CLIENT)
public final class CurveEditorScreen extends ModularUIScreen {
    private final Screen parent;
    private final Editor editor;

    public CurveEditorScreen(Screen parent) {
        this(parent, CurveConfigSession.open());
    }

    CurveEditorScreen(Screen parent, CurveConfigSession session) {
        this(parent, new Editor(session));
    }

    private CurveEditorScreen(Screen parent, Editor editor) {
        super(editor.createUI(), tr("title"));
        this.parent = parent;
        this.editor = editor;
        editor.close = () -> { if (Minecraft.getInstance().screen == this) onClose(); };
    }

    @Override
    public void onClose() {
        if (editor.saving) return;
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable("epiphany.curve." + key, args);
    }

    private static final class Editor {
        // 4px padding + 16px title + 2px gap + five 16px rows with 2px gaps + 4px padding.
        private static final int PARAMETER_CARD_HEIGHT = 114;
        private static final int PARAMETER_FORM_HEIGHT = 88;
        private CurvePreviewDraft draft = new CurvePreviewDraft();
        private CurvePreviewDraft.Preview preview = draft.preview();
        private final CurveConfigSession session;
        private UIElement segmentList;
        private UIElement parameterBox;
        private final List<Button> segmentButtons = new ArrayList<>();
        private final Map<String, TextField> parameterFields = new HashMap<>();
        private static final IGuiTexture ERROR_BUTTON = SpriteTexture.of("epiphany:textures/gui/progression/error_button.png")
                .setSprite(0, 0, 13, 13).setBorder(4);
        private static final IGuiTexture ERROR_BUTTON_UNSELECTED = SpriteTexture.of("epiphany:textures/gui/progression/error_button_unclicked.png")
                .setSprite(0, 0, 13, 13).setBorder(4);
        private static final IGuiTexture ERROR_BUTTON_HOVER = SpriteTexture.of("epiphany:textures/gui/progression/error_button_light.png")
                .setSprite(0, 0, 13, 13).setBorder(4);
        private static final IGuiTexture ERROR_INPUT = SpriteTexture.of("epiphany:textures/gui/progression/error_input.png")
                .setSprite(0, 0, 14, 14).setBorder(4);
        private final Label readout = label(Component.empty());
        private final Label limitCaption = label(tr("limit"));
        private final Label parameterTitle = label(Component.empty());
        private final CurveChartElement chart = new CurveChartElement(this::selectPoint);
        private TextField limitField;
        private TextField pointField;
        private Button unlimitedButton;
        private Button removeButton;
        private Button addButton;
        private Button saveButton;
        private Button resetButton;
        private boolean saving;
        private String saveError;
        private boolean logarithmic;
        private boolean cumulative;
        private int point = 1;
        private Runnable close = () -> {};

        Editor(CurveConfigSession session) {
            this.session = session;
            if (session != null) {
                draft = new CurvePreviewDraft(session.rules);
                preview = draft.preview();
            }
        }

        ModularUI createUI() {
            UIElement root = column();
            root.setId("curve-root");
            root.layout(l -> l.widthPercent(100).heightPercent(100).paddingAll(4).gapAll(4));
            root.style(s -> s.background(new ColorRectTexture(0xFF242129)));

            var header = row();
            header.setId("curve-header");
            header.layout(l -> l.height(18).flexShrink(0));
            var title = label(tr("title"));
            title.layout(l -> l.width(0).flex(1));
            saveButton = button(tr("save"), 38, this::save);
            saveButton.setId("curve-save");
            resetButton = button(tr("reset"), 44, this::reset);
            header.addChildren(title, resetButton, saveButton,
                    button(Component.translatable("gui.cancel"), 38, () -> { if (!saving) close.run(); }));
            root.addChild(header);

            var limits = card("curve-limits");
            limits.layout(l -> l.flexDirection(FlexDirection.ROW).gapAll(4).height(24).flexShrink(0));
            limitCaption.layout(l -> l.width(46));
            limitField = input(draft.unlimited ? draft.previewEnd : draft.limit, value -> {
                if (draft.unlimited) draft.previewEnd = value;
                else draft.limit = value;
                refresh();
            });
            limitField.layout(l -> l.width(0).flex(1).minWidth(30));
            limitField.setId("curve-limit-input");
            unlimitedButton = button(tr(draft.unlimited ? "unlimited" : "finite"), 42, () -> {
                draft.unlimited = !draft.unlimited;
                limitField.setText(draft.unlimited ? draft.previewEnd : draft.limit, false);
                unlimitedButton.setText(tr(draft.unlimited ? "unlimited" : "finite"));
                limitCaption.setText(tr(draft.unlimited ? "preview_points" : "limit"));
                refresh();
            });
            limits.addChildren(limitCaption, limitField, unlimitedButton);
            limitCaption.setText(tr(draft.unlimited ? "preview_points" : "limit"));

            var body = row();
            body.setId("curve-body");
            body.layout(l -> l.flex(1).minHeight(0));
            var sidebar = column();
            sidebar.setId("curve-sidebar");
            sidebar.layout(l -> l.widthPercent(40).minWidth(125).maxWidth(210).minHeight(0).flexShrink(0).gapAll(4));

            var segments = card("curve-segments");
            segments.layout(l -> l.flex(1).minHeight(52));
            var tools = row();
            tools.layout(l -> l.height(16).flexShrink(0));
            var segmentTitle = label(tr("segments"));
            segmentTitle.layout(l -> l.width(0).flex(1));
            addButton = button(Component.literal("+"), 18, this::addSegment);
            removeButton = button(Component.literal("−"), 18, this::removeSegment);
            tools.addChildren(segmentTitle, addButton, removeButton);
            var segmentScroll = scroll();
            segmentList = segmentScroll.viewContainer;
            segments.addChildren(tools, segmentScroll);

            var parameters = card("curve-parameters");
            parameters.layout(l -> l.height(PARAMETER_CARD_HEIGHT).flexShrink(0));
            parameters.addChild(parameterTitle);
            parameterBox = column();
            parameterBox.setId("curve-parameter-fields");
            parameterBox.layout(l -> l.widthPercent(100).height(PARAMETER_FORM_HEIGHT).flexShrink(0).gapAll(2));
            parameters.addChild(parameterBox);
            sidebar.addChildren(segments, parameters);
            body.addChild(sidebar);

            var previewColumn = column();
            previewColumn.layout(l -> l.flex(1).minWidth(0).minHeight(0).gapAll(4));
            previewColumn.addChild(limits);
            var graphPanel = card("curve-preview");
            graphPanel.layout(l -> l.flex(1).minWidth(0).minHeight(0));
            var graphHeader = row();
            graphHeader.setId("curve-preview-header");
            graphHeader.layout(l -> l.height(16).flexShrink(0));
            var graphTitle = label(tr("preview"));
            graphTitle.setId("curve-preview-title");
            graphTitle.layout(l -> l.width(0).flex(1));
            Button scale = button(tr("linear_axis"), 44, () -> {});
            scale.setOnClick(event -> {
                logarithmic = !logarithmic;
                scale.setText(tr(logarithmic ? "log_axis" : "linear_axis"));
                refresh();
            });
            Button mode = button(tr("single"), 44, () -> {});
            mode.setId("curve-preview-mode");
            mode.setOnClick(event -> {
                cumulative = !cumulative;
                mode.setText(tr(cumulative ? "cumulative" : "single"));
                renderPreview();
            });
            graphHeader.addChildren(graphTitle, mode, scale);
            graphPanel.addChildren(graphHeader, chart);
            var inspector = row();
            inspector.setId("curve-inspector");
            inspector.layout(l -> l.height(16).flexShrink(0));
            var pointCaption = label(tr("point"));
            pointCaption.layout(l -> l.width(24));
            pointField = input("1", value -> refresh());
            pointField.setId("curve-point-input");
            pointField.layout(l -> l.width(44));
            readout.layout(l -> l.width(0).flex(1));
            inspector.addChildren(pointCaption, pointField, readout);
            graphPanel.addChild(inspector);
            previewColumn.addChild(graphPanel);
            body.addChild(previewColumn);
            root.addChild(body);
            rebuildEditor();
            return ModularUI.of(UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP)));
        }

        private void rebuildEditor() {
            segmentList.clearAllChildren();
            segmentButtons.clear();
            for (int i = 0; i < draft.segments.size(); i++) {
                final int index = i;
                var button = button(Component.empty(), 0, () -> {
                    draft.selected = index;
                    rebuildEditor();
                });
                button.layout(l -> l.widthPercent(100));
                button.setId("curve-segment-" + index);
                button.textStyle(s -> s.textColor(CurveChartElement.COLORS[index % CurveChartElement.COLORS.length]));
                segmentList.addChild(button);
                segmentButtons.add(button);
            }
            parameterBox.clearAllChildren();
            parameterBox.setActive(true);
            parameterFields.clear();
            var segment = draft.segments.get(draft.selected);
            parameterTitle.setText(tr("parameters", draft.selected + 1));
            TextField start = input(segment.start, value -> { segment.start = value; refresh(); });
            if (draft.selected == 0) start.disabled();
            start.setId("curve-parameter-start");
            parameterFields.put("start", start);
            parameterBox.addChild(field(tr("start"), start));
            var selector = new Selector<CurvePreviewDraft.Function>();
            selector.setCandidates(List.of(CurvePreviewDraft.Function.values()));
            selector.setSelected(segment.function, false);
            // LDLib rebuilds the preview while installing a provider, including for
            // an empty selection. Set the initial value first and accept that null.
            selector.setCandidateUIProvider(function -> label(function == null ? Component.empty() : function.title()));
            selector.layout(l -> l.height(16).widthPercent(100).flexShrink(0));
            // A percentage-sized label inside LDLib's flex preview can collapse
            // during resize. Bind it to the resolved preview width instead.
            selector.preview.layout(l -> l.width(0).minWidth(0).flex(1));
            selector.preview.addEventListener(UIEvents.LAYOUT_CHANGED, event -> {
                float width = selector.preview.getContentWidth();
                if (width <= 0) return;
                for (var child : selector.preview.getChildren()) child.layout(l -> l.width(width));
            });
            selector.setOnValueChanged(function -> {
                if (function == null) return;
                segment.function = function;
                rebuildEditor();
            });
            parameterBox.addChild(selector);
            for (var parameter : segment.function.parameters) {
                var input = input(segment.values.get(parameter), value -> {
                    segment.values.put(parameter, value);
                    refresh();
                });
                input.setId("curve-parameter-" + parameter.key);
                parameterFields.put(parameter.key, input);
                parameterBox.addChild(field(tr("parameter." + parameter.key), input));
            }
            if (saving || (session != null && !session.writable())) disableTree(parameterBox);
            refresh();
        }

        private static void disableTree(UIElement element) {
            element.setActive(false);
            element.getChildren().forEach(Editor::disableTree);
        }

        private void refresh() {
            var validation = draft.validate();
            if (validation.valid()) preview = validation.preview();
            var errors = new ArrayList<>(validation.errors());
            if (validation.valid()) {
                for (int i = 0; i < validation.preview().rules().size(); i++) {
                    var rule = validation.preview().rules().get(i);
                    long last = i + 1 < validation.preview().rules().size()
                            ? validation.preview().rules().get(i + 1).start() - 2L : (long) validation.end() - 1;
                    if (draft.unlimited || validation.end() <= rule.evaluator().from()) continue;
                    try { rule.evaluator().required(Math.min(last, (long) validation.end() - 1)); }
                    catch (ArithmeticException exception) {
                        for (var parameter : rule.function().parameters)
                            errors.add(new CurvePreviewDraft.InputError(i, parameter.key, "epiphany.curve.error.price"));
                    }
                }
            }
            markError(limitField, messages(errors, -1, "limit"));
            parameterFields.forEach((key, field) -> markError(field, messages(errors, draft.selected, key)));
            for (int i = 0; i < segmentButtons.size(); i++) {
                var segment = draft.segments.get(i);
                String end = i + 1 < draft.segments.size() ? previous(draft.segments.get(i + 1).start) : "∞";
                segmentButtons.get(i).setText(Component.literal(segment.start + "–" + end + " ").append(segment.function.title()));
            }
            for (int i = 0; i < segmentButtons.size(); i++) {
                final boolean selected = i == draft.selected;
                final int index = i;
                var segmentErrors = errors.stream().filter(error -> error.segment() == index).toList();
                final boolean invalid = !segmentErrors.isEmpty();
                // Keep every interaction state in the same square GDP texture
                // family. Button's defaults use the rounded RECT_RD family.
                segmentButtons.get(i).buttonStyle(s -> s.baseTexture(invalid ? selected ? ERROR_BUTTON : ERROR_BUTTON_UNSELECTED
                        : selected ? Sprites.RECT_DARK : Sprites.RECT)
                        .hoverTexture(invalid ? selected ? ERROR_BUTTON : ERROR_BUTTON_HOVER
                                : selected ? Sprites.RECT_DARK : Sprites.RECT_LIGHT)
                        .pressedTexture(invalid ? ERROR_BUTTON : Sprites.RECT_DARK));
                Component[] tooltip = segmentErrors.stream().map(error ->
                        tr(error.field().equals("start") ? "start" : "parameter." + error.field()).copy()
                                .append(": ").append(Component.translatable(error.message()))).toArray(Component[]::new);
                segmentButtons.get(i).style(s -> s.tooltips(tooltip));
            }
            boolean writable = session == null || session.writable();
            resetButton.setActive(writable && !saving);
            removeButton.setActive(writable && draft.selected > 0 && !saving);
            addButton.setActive(writable && draft.segments.size() < 32 && !saving);
            unlimitedButton.setActive(writable && !saving);
            limitField.setActive((writable || draft.unlimited) && !saving);
            saveButton.setActive(writable && errors.isEmpty() && !saving);
            Component[] saveTooltip = saveError != null ? new Component[]{tr("save_failed", Component.translatable(saveError))}
                    : session != null && session.error != null ? new Component[]{tr("load_failed", Component.translatable(session.error))}
                    : new Component[0];
            saveButton.style(s -> s.tooltips(saveTooltip));
            boolean saveInvalid = saveError != null || (session != null && session.error != null);
            saveButton.buttonStyle(s -> s.baseTexture(saveInvalid ? ERROR_BUTTON_UNSELECTED : Sprites.RECT_RD)
                    .hoverTexture(saveInvalid ? ERROR_BUTTON_HOVER : Sprites.RECT_RD_LIGHT)
                    .pressedTexture(saveInvalid ? ERROR_BUTTON : Sprites.RECT_RD_DARK));
            validatePoint(validation.end());
            renderPreview();
        }

        private void selectPoint(int value) {
            point = Math.max(1, Math.min(Math.max(1, preview.end()), value));
            pointField.setText(Integer.toString(point), false);
            refresh();
        }

        private void validatePoint(Integer end) {
            pointField.setActive(end == null || end > 0);
            if (end != null && end == 0) {
                markError(pointField);
                return;
            }
            try {
                int requested = Integer.parseInt(pointField.getValue());
                if (requested < 1 || (end != null && requested > end)) throw new NumberFormatException();
                point = requested;
                markError(pointField);
            } catch (NumberFormatException exception) {
                markError(pointField, end == null ? tr("error.integer") : tr("error.point", end));
            }
        }

        private void renderPreview() {
            point = Math.max(1, Math.min(Math.max(1, preview.end()), point));
            chart.setPreview(preview, point, logarithmic, cumulative);
            double cost = cumulative ? preview.cumulativeCost(point) : preview.rule(point).cost(point);
            readout.setText(preview.end() == 0 ? tr("empty") : tr("readout",
                    CurveChartElement.representable(cost) ? CurveChartElement.shortNumber(cost) : tr("overflow").getString()));
            Component exact;
            try { exact = Component.literal(cumulative ? "≈ " + cost : Long.toString(preview.rule(point).evaluator().required(point - 1L))); }
            catch (ArithmeticException exception) { exact = tr("overflow"); }
            Component tooltip = exact;
            readout.style(s -> s.tooltips(tooltip));
        }

        private static Component[] messages(List<CurvePreviewDraft.InputError> errors, int segment, String field) {
            return errors.stream().filter(error -> error.segment() == segment && error.field().equals(field))
                    .map(CurvePreviewDraft.InputError::message).distinct().map(Component::translatable).toArray(Component[]::new);
        }

        private static void markError(TextField field, Component... messages) {
            boolean invalid = messages.length > 0;
            field.style(s -> s.background(invalid ? ERROR_INPUT : Sprites.RECT_RD_SOLID).tooltips(messages));
            field.textFieldStyle(s -> s.focusOverlay(!invalid ? Sprites.RECT_RD_T_SOLID
                    : IGuiTexture.EMPTY));
        }

        private void addSegment() {
            if (draft.segments.size() >= 32) return;
            try {
                var current = draft.preview();
                int last = current.rules().getLast().start();
                if (last == Integer.MAX_VALUE) return;
                int start = last < current.end() ? (int) (((long) last + current.end()) / 2 + 1) : last + 1;
                draft.segments.add(new CurvePreviewDraft.Segment(start, CurvePreviewDraft.Function.LINEAR, "10", "1"));
                draft.selected = draft.segments.size() - 1;
                rebuildEditor();
            } catch (IllegalArgumentException ignored) { refresh(); }
        }

        private void removeSegment() {
            if (draft.selected == 0) return;
            draft.segments.remove(draft.selected--);
            rebuildEditor();
        }

        private void reset() {
            draft = session == null ? new CurvePreviewDraft() : new CurvePreviewDraft(ProgressionRules.DEFAULT);
            saveError = null;
            point = 1;
            pointField.setText("1", false);
            limitField.setText(draft.unlimited ? draft.previewEnd : draft.limit, false);
            unlimitedButton.setText(tr(draft.unlimited ? "unlimited" : "finite"));
            limitCaption.setText(tr(draft.unlimited ? "preview_points" : "limit"));
            rebuildEditor();
        }

        private void save() {
            if (session == null || !session.writable() || saving) return;
            var rules = draft.toRules();
            saving = true;
            disableTree(parameterBox);
            refresh();
            session.save(rules, failure -> {
                saving = false;
                saveError = failure;
                if (failure == null) close.run();
                else rebuildEditor();
            });
        }

        private static String previous(String raw) {
            try { return Integer.toString(Math.subtractExact(Integer.parseInt(raw), 1)); }
            catch (IllegalArgumentException | ArithmeticException exception) { return "?"; }
        }

        private static UIElement field(Component name, TextField input) {
            var field = row();
            field.layout(l -> l.widthPercent(100).height(16).flexShrink(0));
            var caption = label(name);
            caption.layout(l -> l.width(0).flex(1).minWidth(0));
            input.layout(l -> l.widthPercent(48).minWidth(42));
            return field.addChildren(caption, input);
        }

        private static TextField input(String value, Consumer<String> change) {
            var field = new TextField();
            field.setText(value, false);
            field.setTextResponder(change);
            field.layout(l -> l.height(16).flexShrink(0).minWidth(0));
            field.textFieldStyle(s -> s.fontSize(8));
            return field;
        }

        private static Button button(Component title, int width, Runnable action) {
            var button = new Button();
            button.setText(title);
            button.layout(l -> l.width(width).height(16).flexShrink(0));
            button.text.layout(l -> l.width(0).flex(1).minWidth(0).marginHorizontal(0));
            button.textStyle(s -> s.fontSize(8).adaptiveWidth(false).textWrap(TextWrap.HIDE));
            button.setOnClick(event -> action.run());
            return button;
        }

        private static Label label(Component title) {
            var label = new Label();
            label.setText(title);
            label.layout(l -> l.widthPercent(100).height(16).minWidth(0).flexShrink(0));
            label.textStyle(s -> s.fontSize(8).adaptiveWidth(false).textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE).textShadow(true));
            return label;
        }

        private static UIElement card(String id) {
            var panel = column();
            panel.setId(id);
            panel.layout(l -> l.paddingAll(4).gapAll(2).minWidth(0));
            panel.style(s -> s.background(Sprites.RECT_DARK));
            return panel;
        }

        private static ScrollerView scroll() {
            var scroll = new ScrollerView();
            scroll.layout(l -> l.widthPercent(100).flex(1).minHeight(0).minWidth(0));
            scroll.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL).adaptiveWidth(false).adaptiveHeight(false));
            scroll.viewPort.layout(l -> l.paddingAll(0));
            scroll.viewPort.style(s -> s.background(IGuiTexture.EMPTY));
            scroll.viewContainer.layout(l -> l.gapAll(2));
            // Percentage widths in LDLib's scroll wrapper can collapse to the
            // form's intrinsic width during incremental Taffy layout. Bind both
            // content layers to the viewport's measured width instead.
            scroll.viewPort.addEventListener(UIEvents.LAYOUT_CHANGED, event -> {
                float width = scroll.viewPort.getContentWidth();
                if (width <= 0) return;
                scroll.viewContainer.getParent().layout(l -> l.width(width));
                scroll.viewContainer.layout(l -> l.width(width));
            });
            return scroll;
        }

        private static UIElement column() {
            return new UIElement().layout(l -> l.flexDirection(FlexDirection.COLUMN).gapAll(3));
        }

        private static UIElement row() {
            return new UIElement().layout(l -> l.flexDirection(FlexDirection.ROW).gapAll(4));
        }
    }
}
