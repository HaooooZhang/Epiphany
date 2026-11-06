package ink.myumoon.epiphany.client.ui.progression;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.gui.util.UISoundUtils;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.LoadingModList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.mock;

class CurveEditorScreenTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        try (var loading = mockStatic(LoadingModList.class)) {
            loading.when(LoadingModList::get).thenReturn(mock(LoadingModList.class));
            Bootstrap.bootStrap();
        }
        LayoutProperties.init();
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildsEditorAndHandlesSelectorsEmptyPreviewAndEveryFunction() {
        // Use real LDLib components. Only disable client rendering/font access;
        // Selector's setupDialog/setValue lifecycle must run unmocked.
        try (var environment = mockStatic(LDLib2.class); var mods = mockStatic(ModList.class)) {
            mods.when(ModList::get).thenReturn(mock(ModList.class));
            environment.when(() -> LDLib2.id(anyString())).thenAnswer(call ->
                    ResourceLocation.fromNamespaceAndPath("ldlib2", call.getArgument(0)));
            var screen = assertDoesNotThrow(() -> new CurveEditorScreen(null, (CurveConfigSession) null));
            for (var function : CurvePreviewDraft.Function.values()) {
                var selector = (Selector<CurvePreviewDraft.Function>) findSelector(screen.modularUI.ui.rootElement);
                assertDoesNotThrow(() -> selector.setSelected(null, false));
                assertDoesNotThrow(() -> selector.setSelected(function));
                var replacement = (Selector<CurvePreviewDraft.Function>) findSelector(screen.modularUI.ui.rootElement);
                assertEquals(function, replacement.getValue());
            }
        }
    }

    private static Selector<?> findSelector(UIElement root) {
        if (root instanceof Selector<?> selector) return selector;
        for (var child : root.getChildren()) {
            var found = findSelector(child);
            if (found != null) return found;
        }
        return null;
    }

    @Test
    @SuppressWarnings("unchecked")
    void keepsCardsAndControlsSeparateAtMinecraftGuiSizes() {
        try (var environment = mockStatic(LDLib2.class); var mods = mockStatic(ModList.class)) {
            mods.when(ModList::get).thenReturn(mock(ModList.class));
            environment.when(() -> LDLib2.id(anyString())).thenAnswer(call ->
                    ResourceLocation.fromNamespaceAndPath("ldlib2", call.getArgument(0)));
            var screen = new CurveEditorScreen(null, (CurveConfigSession) null);
            for (int[] size : new int[][] {{320, 240}, {480, 270}, {854, 480}}) {
                screen.modularUI.init(size[0], size[1]);
                var ui = screen.modularUI;
                var segments = ui.getElementsById("curve-segments").getFirst();
                var parameters = ui.getElementsById("curve-parameters").getFirst();
                var graph = ui.getElementsById("curve-preview").getFirst();
                assertTrue(segments.getSizeWidth() > 0);
                assertTrue(segments.getSizeHeight() >= 52, "list must keep room for its controls");
                assertEquals(114, parameters.getSizeHeight(), 0.01f);
                assertSame(segments.getStyle().backgroundTexture(), parameters.getStyle().backgroundTexture());
                assertSame(segments.getStyle().backgroundTexture(), graph.getStyle().backgroundTexture());
                assertTrue(segments.getPositionY() + segments.getSizeHeight() <= parameters.getPositionY());
                assertTrue(segments.getPositionX() + segments.getSizeWidth() <= graph.getPositionX());
                assertTrue(graph.getSizeWidth() >= 140);
                assertTrue(graph.getPositionY() + graph.getSizeHeight() <= size[1]);
                var limits = ui.getElementsById("curve-limits").getFirst();
                assertSame(segments.getStyle().backgroundTexture(), limits.getStyle().backgroundTexture());
                assertEquals(graph.getPositionX(), limits.getPositionX(), 0.01f);
                assertEquals(ui.getElementsById("curve-preview-title").getFirst().getPositionX(),
                        limits.getChildren().getFirst().getPositionX(), 0.01f);
                assertEquals(limits.getPositionY(), segments.getPositionY(), 0.01f);
                assertTrue(limits.getPositionY() + limits.getSizeHeight() <= graph.getPositionY());
                for (var function : CurvePreviewDraft.Function.values()) {
                    var selector = (Selector<CurvePreviewDraft.Function>) findSelector(ui.ui.rootElement);
                    selector.setSelected(function);
                    relayout(screen);
                    assertEquals(114, parameters.getSizeHeight(), 0.01f,
                            function + " must keep the parameter card height stable");
                    assertTrue(parameters.getPositionY() + parameters.getSizeHeight() <= size[1]);
                    var form = ui.getElementsById("curve-parameter-fields").getFirst();
                    assertFalse(form instanceof ScrollerView);
                    for (var field : form.getChildren()) {
                        assertTrue(field.getPositionY() >= parameters.getContentY());
                        assertTrue(field.getPositionY() + field.getSizeHeight()
                                <= parameters.getContentY() + parameters.getContentHeight() + 0.01f,
                                function + " must show every parameter without scrolling");
                    }
                }
                var parameterForm = ui.getElementsById("curve-parameter-fields").getFirst();
                var segmentScroll = (ScrollerView) segments.getChildren().getLast();
                // Exercise an overflowing list and the same incremental layout
                // path used by wheel/scrollbar changes, without reinitializing UI.
                for (int i = 0; i < 32; i++) {
                    segmentScroll.addScrollViewChild(new Button().layout(l -> l.widthPercent(100).height(18)));
                }
                relayout(screen);
                assertTrue(segmentScroll.getContainerHeight() > segmentScroll.viewPort.getContentHeight());
                for (float offset : new float[] {0.5f, 1f, 0f}) {
                    segmentScroll.verticalScroller.setValue(offset);
                    relayout(screen);
                    assertTrue(parameterForm.getSizeWidth() >= parameters.getContentWidth() - 1,
                            "parameter form collapsed: " + parameterForm.getSizeWidth()
                                    + " / " + parameters.getContentWidth());
                    for (var field : parameterForm.getChildren()) {
                        assertTrue(field.getSizeWidth() >= parameterForm.getSizeWidth() - 1);
                        if (field instanceof Selector<?>) continue;
                        assertTrue(field.getChildren().getFirst().getSizeWidth() > 30, "parameter label collapsed");
                    }
                }
                for (String id : new String[] {"curve-header", "curve-limits", "curve-inspector", "curve-preview-header"}) {
                    var row = ui.getElementsById(id).getFirst();
                    float end = row.getContentX();
                    for (var child : row.getChildren()) {
                        assertTrue(child.getSizeWidth() > 0, id + " must allocate width to labels too");
                        assertTrue(child.getPositionX() >= end - 0.01f, id + " overlaps");
                        end = child.getPositionX() + child.getSizeWidth();
                        assertTrue(end <= row.getContentX() + row.getContentWidth() + 0.01f, id + " overflows");
                    }
                }
            }
        }
    }

    @Test
    void invalidInputHighlightsOnlyItsFieldAndClearsAfterCorrection() {
        try (var environment = mockStatic(LDLib2.class); var mods = mockStatic(ModList.class)) {
            mods.when(ModList::get).thenReturn(mock(ModList.class));
            environment.when(() -> LDLib2.id(anyString())).thenAnswer(call ->
                    ResourceLocation.fromNamespaceAndPath("ldlib2", call.getArgument(0)));
            var screen = new CurveEditorScreen(null, (CurveConfigSession) null);
            screen.modularUI.init(480, 270);
            var ui = screen.modularUI;
            assertTrue(ui.getElementsById("curve-error").isEmpty());
            var base = (TextField) ui.getElementsById("curve-parameter-base").getFirst();
            var slope = (TextField) ui.getElementsById("curve-parameter-slope").getFirst();
            var segments = ui.getElementsById("curve-segments").getFirst();
            var segmentScroll = (ScrollerView) segments.getChildren().getLast();
            var segmentButton = (Button) segmentScroll.viewContainer.getChildren().getFirst();
            assertSame(Sprites.RECT_DARK, segmentButton.getButtonStyle().baseTexture());
            assertSame(Sprites.RECT_DARK, segmentButton.getButtonStyle().hoverTexture());
            assertSame(Sprites.RECT_DARK, segmentButton.getButtonStyle().pressedTexture());
            var unselectedButton = (Button) segmentScroll.viewContainer.getChildren().get(1);
            assertSame(Sprites.RECT, unselectedButton.getButtonStyle().baseTexture());
            assertSame(Sprites.RECT_LIGHT, unselectedButton.getButtonStyle().hoverTexture());
            assertSame(Sprites.RECT_DARK, unselectedButton.getButtonStyle().pressedTexture());
            base.setText("");
            relayout(screen);
            assertFalse(base.getStyle().tooltips().isEmpty());
            assertEquals("epiphany:textures/gui/progression/error_input.png",
                    assertInstanceOf(SpriteTexture.class, base.getStyle().backgroundTexture()).getImageLocation().toString());
            assertSame(IGuiTexture.EMPTY, base.getTextFieldStyle().focusOverlay());
            assertEquals("epiphany:textures/gui/progression/error_button.png",
                    assertInstanceOf(SpriteTexture.class, segmentButton.getButtonStyle().baseTexture()).getImageLocation().toString());
            assertSame(segmentButton.getButtonStyle().baseTexture(), segmentButton.getButtonStyle().hoverTexture());
            assertSame(segmentButton.getButtonStyle().baseTexture(), segmentButton.getButtonStyle().pressedTexture());
            assertTrue(slope.getStyle().tooltips().isEmpty());
            base.setText("12");
            relayout(screen);
            assertTrue(base.getStyle().tooltips().isEmpty());
            assertSame(Sprites.RECT_RD_SOLID, base.getStyle().backgroundTexture());
            assertSame(Sprites.RECT_RD_T_SOLID, base.getTextFieldStyle().focusOverlay());
            var limit = (TextField) ui.getElementsById("curve-limit-input").getFirst();
            limit.setText("-1");
            relayout(screen);
            assertFalse(limit.getStyle().tooltips().isEmpty());
            assertEquals("epiphany:textures/gui/progression/error_input.png",
                    assertInstanceOf(SpriteTexture.class, limit.getStyle().backgroundTexture()).getImageLocation().toString());
        }
    }

    @Test
    void keepsAllErrorsVisibleAcrossSegmentSelectionAndIndependentCorrections() {
        try (var environment = mockStatic(LDLib2.class); var mods = mockStatic(ModList.class);
             var sounds = mockStatic(UISoundUtils.class)) {
            mods.when(ModList::get).thenReturn(mock(ModList.class));
            environment.when(() -> LDLib2.id(anyString())).thenAnswer(call ->
                    ResourceLocation.fromNamespaceAndPath("ldlib2", call.getArgument(0)));
            var screen = new CurveEditorScreen(null, (CurveConfigSession) null);
            screen.modularUI.init(480, 270);
            input(screen, "curve-limit-input").setText("-1");
            input(screen, "curve-point-input").setText("bad");
            input(screen, "curve-parameter-base").setText("");
            input(screen, "curve-parameter-slope").setText("-1");
            relayout(screen);
            for (String id : new String[] {"curve-limit-input", "curve-point-input", "curve-parameter-base", "curve-parameter-slope"})
                assertFalse(input(screen, id).getStyle().tooltips().isEmpty(), id);

            clickSegment(screen, 1);
            input(screen, "curve-parameter-start").setText("1");
            input(screen, "curve-parameter-base").setText("0");
            input(screen, "curve-parameter-slope").setText("bad");
            relayout(screen);
            var first = (Button) screen.modularUI.getElementsById("curve-segment-0").getFirst();
            assertEquals(2, first.getStyle().tooltips().tooltips().length);
            assertEquals("error_button_unclicked.png", textureName(first.getButtonStyle().baseTexture()));
            assertEquals("error_button_light.png", textureName(first.getButtonStyle().hoverTexture()));
            assertEquals("error_button.png", textureName(first.getButtonStyle().pressedTexture()));
            var selected = (Button) screen.modularUI.getElementsById("curve-segment-1").getFirst();
            assertEquals(3, selected.getStyle().tooltips().tooltips().length);
            assertEquals("error_button.png", textureName(selected.getButtonStyle().baseTexture()));
            assertSame(selected.getButtonStyle().baseTexture(), selected.getButtonStyle().hoverTexture());
            for (String id : new String[] {"curve-parameter-start", "curve-parameter-base", "curve-parameter-slope"})
                assertFalse(input(screen, id).getStyle().tooltips().isEmpty(), id);

            input(screen, "curve-parameter-start").setText("21");
            input(screen, "curve-parameter-base").setText("30");
            input(screen, "curve-parameter-slope").setText("3");
            input(screen, "curve-limit-input").setText("100");
            relayout(screen);
            assertTrue(selected.getStyle().tooltips().isEmpty());
            assertFalse(first.getStyle().tooltips().isEmpty());
            assertFalse(input(screen, "curve-point-input").getStyle().tooltips().isEmpty());

            clickSegment(screen, 0);
            assertFalse(input(screen, "curve-parameter-base").getStyle().tooltips().isEmpty());
            assertFalse(input(screen, "curve-parameter-slope").getStyle().tooltips().isEmpty());
            input(screen, "curve-parameter-base").setText("10");
            relayout(screen);
            assertTrue(input(screen, "curve-parameter-base").getStyle().tooltips().isEmpty());
            assertFalse(input(screen, "curve-parameter-slope").getStyle().tooltips().isEmpty());
            input(screen, "curve-parameter-slope").setText("1");
            input(screen, "curve-point-input").setText("20");
            relayout(screen);
            assertTrue(input(screen, "curve-point-input").getStyle().tooltips().isEmpty());
            assertTrue(screen.modularUI.getElementsById("curve-segment-0").getFirst().getStyle().tooltips().isEmpty());
        }
    }

    private static TextField input(CurveEditorScreen screen, String id) {
        return (TextField) screen.modularUI.getElementsById(id).getFirst();
    }

    private static String textureName(IGuiTexture texture) {
        var path = assertInstanceOf(SpriteTexture.class, texture).getImageLocation().getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static void clickSegment(CurveEditorScreen screen, int index) {
        var event = UIEvent.create(UIEvents.MOUSE_DOWN);
        event.target = screen.modularUI.getElementsById("curve-segment-" + index).getFirst();
        event.button = 0;
        UIEventDispatcher.dispatchEvent(event, false, false, false);
        relayout(screen);
    }

    @Test
    void selectorTextKeepsItsWidthWhenResizingWithoutRebuildingTheForm() {
        try (var environment = mockStatic(LDLib2.class); var mods = mockStatic(ModList.class)) {
            mods.when(ModList::get).thenReturn(mock(ModList.class));
            environment.when(() -> LDLib2.id(anyString())).thenAnswer(call ->
                    ResourceLocation.fromNamespaceAndPath("ldlib2", call.getArgument(0)));
            var screen = new CurveEditorScreen(null, (CurveConfigSession) null);
            var selector = findSelector(screen.modularUI.ui.rootElement);
            for (int[] size : new int[][] {{480, 270}, {320, 240}, {854, 480}, {480, 270}, {320, 240}}) {
                screen.modularUI.init(size[0], size[1]);
                for (int pass = 0; pass < 3; pass++) {
                    relayout(screen);
                    var label = selector.preview.getChildren().getFirst();
                    assertTrue(label.getContentWidth() >= selector.preview.getContentWidth() - 1,
                            "selector text collapsed after resize: " + label.getContentWidth()
                                    + " / " + selector.preview.getContentWidth());
                    assertTrue(label.getContentWidth() > 50);
                }
            }
        }
    }

    private static void relayout(CurveEditorScreen screen) {
        try {
            var calculate = screen.modularUI.getClass().getDeclaredMethod("calculateStyleAndLayout");
            calculate.setAccessible(true);
            calculate.invoke(screen.modularUI);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    @Test
    void loadsRealRulesWithoutDoublePrecisionLossAndKeepsRemoteParametersReadOnly() {
        try (var environment = mockStatic(LDLib2.class); var mods = mockStatic(ModList.class)) {
            mods.when(ModList::get).thenReturn(mock(ModList.class));
            environment.when(() -> LDLib2.id(anyString())).thenAnswer(call ->
                    ResourceLocation.fromNamespaceAndPath("ldlib2", call.getArgument(0)));
            var rules = new ink.myumoon.epiphany.config.ProgressionRules(20, 9007199254740993L, 1);
            var local = new CurveEditorScreen(null, new CurveConfigSession(rules, java.nio.file.Path.of("unused.toml"), null, null, null));
            local.modularUI.init(320, 240);
            assertEquals("9007199254740993", input(local, "curve-parameter-base").getValue());
            assertEquals("20", input(local, "curve-limit-input").getValue());
            assertEquals(1, local.modularUI.getElementsById("curve-segment-0").size());
            assertTrue(local.modularUI.getElementsById("curve-save").getFirst().isActive());
            input(local, "curve-parameter-base").setText("");
            assertFalse(local.modularUI.getElementsById("curve-save").getFirst().isActive());
            input(local, "curve-parameter-base").setText("10");
            assertTrue(local.modularUI.getElementsById("curve-save").getFirst().isActive());

            var remote = new CurveEditorScreen(null, new CurveConfigSession(rules, null, null, null, null));
            remote.modularUI.init(320, 240);
            assertFalse(input(remote, "curve-parameter-base").isActive());
            assertFalse(input(remote, "curve-limit-input").isActive());
            assertFalse(findSelector(remote.modularUI.ui.rootElement).isActive());
            assertFalse(remote.modularUI.getElementsById("curve-save").getFirst().isActive());
            assertTrue(input(remote, "curve-point-input").isActive());
        }
    }

    @Test
    void saveFailureKeepsDraftAndRestoresTheParameterForm() {
        try (var environment = mockStatic(LDLib2.class); var mods = mockStatic(ModList.class);
             var sounds = mockStatic(UISoundUtils.class)) {
            mods.when(ModList::get).thenReturn(mock(ModList.class));
            environment.when(() -> LDLib2.id(anyString())).thenAnswer(call ->
                    ResourceLocation.fromNamespaceAndPath("ldlib2", call.getArgument(0)));
            var rules = new ink.myumoon.epiphany.config.ProgressionRules(20, 10, 1);
            var session = org.mockito.Mockito.spy(new CurveConfigSession(rules, java.nio.file.Path.of("unused.toml"), null, null, null));
            var callback = new java.util.concurrent.atomic.AtomicReference<java.util.function.Consumer<String>>();
            org.mockito.Mockito.doAnswer(call -> { callback.set(call.getArgument(1)); return null; })
                    .when(session).save(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
            var screen = new CurveEditorScreen(null, session);
            screen.modularUI.init(480, 270);
            input(screen, "curve-parameter-base").setText("25");
            var event = UIEvent.create(UIEvents.MOUSE_DOWN);
            event.target = screen.modularUI.getElementsById("curve-save").getFirst();
            event.button = 0;
            UIEventDispatcher.dispatchEvent(event, false, false, false);
            assertNotNull(callback.get());
            assertFalse(input(screen, "curve-parameter-base").isActive());
            callback.get().accept("disk failure");
            relayout(screen);
            assertEquals("25", input(screen, "curve-parameter-base").getValue());
            assertTrue(input(screen, "curve-parameter-base").isActive());
            assertTrue(screen.modularUI.getElementsById("curve-parameter-fields").getFirst().isActive());
            assertFalse(screen.modularUI.getElementsById("curve-save").getFirst().getStyle().tooltips().isEmpty());
        }
    }

}
