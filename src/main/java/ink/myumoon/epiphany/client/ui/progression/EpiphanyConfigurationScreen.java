package ink.myumoon.epiphany.client.ui.progression;

import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.config.CommonConfig;
import ink.myumoon.epiphany.network.EpiphanyNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;

/** Forge's Mods screen entry for Epiphany's two configuration files. */
public final class EpiphanyConfigurationScreen extends Screen {
    private final Screen parent;

    public EpiphanyConfigurationScreen(Screen parent) {
        super(text("title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = width / 2 - 105;
        int y = height / 2 - 35;
        var common = addRenderableWidget(Button.builder(text("section.epiphany.common.toml"),
                button -> minecraft.setScreen(new CommonScreen(this))).bounds(x, y, 210, 20).build());
        common.active = CommonConfig.SPEC.isLoaded()
                && (minecraft.getConnection() == null || minecraft.getSingleplayerServer() != null);
        if (!common.active) common.setTooltip(Tooltip.create(text("common.unavailable")));
        addRenderableWidget(Button.builder(text("section.epiphany.progression.server.toml"),
                button -> minecraft.setScreen(new CurveEditorScreen(this))).bounds(x, y + 25, 210, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE,
                button -> onClose()).bounds(x, y + 50, 210, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 58, 0xffffff);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private static Component text(String key) {
        return Component.translatable("epiphany.configuration." + key);
    }

    private static final class CommonScreen extends Screen {
        private static final List<ForgeConfigSpec.IntValue> NUMBERS = List.of(
                CommonConfig.MAX_EPIPHANY_SLOTS, CommonConfig.MODULE_SELECT_COST, CommonConfig.MAX_SELECTED_MODULES);
        private static final List<ForgeConfigSpec.BooleanValue> TOGGLES = List.of(
                CommonConfig.NOTIFY_INSIGHT_POINTS, CommonConfig.NOTIFY_MODULE_UNLOCK, CommonConfig.NOTIFY_EPIPHANY_UNLOCK);
        private final Screen parent;
        private final MinecraftServer server;
        private final String[] numbers = new String[NUMBERS.size()];
        private final boolean[] toggles = new boolean[TOGGLES.size()];
        private Button save;
        private Component error;
        private boolean saving;

        private CommonScreen(Screen parent) {
            super(text("section.epiphany.common.toml.title"));
            this.parent = parent;
            server = Minecraft.getInstance().getSingleplayerServer();
            for (int i = 0; i < numbers.length; i++) numbers[i] = NUMBERS.get(i).get().toString();
            for (int i = 0; i < toggles.length; i++) toggles[i] = TOGGLES.get(i).get();
        }

        @Override
        protected void init() {
            for (int i = 0; i < numbers.length; i++) {
                int index = i;
                var name = label(NUMBERS.get(i));
                var input = new EditBox(font, width / 2 + 69, rowY(i), 75, 20, name);
                input.setValue(numbers[i]);
                input.setTooltip(Tooltip.create(text(key(NUMBERS.get(i)) + ".tooltip")));
                input.setEditable(!saving);
                input.setResponder(value -> {
                    numbers[index] = value;
                    refreshSave();
                });
                addRenderableWidget(input);
            }
            for (int i = 0; i < toggles.length; i++) {
                int index = i;
                var setting = TOGGLES.get(i);
                var toggle = CycleButton.onOffBuilder(toggles[i]).displayOnlyValue()
                        .withTooltip(value -> Tooltip.create(text(key(setting) + ".tooltip")))
                        .create(width / 2 + 69, rowY(i + numbers.length), 75, 20, label(setting),
                                (button, value) -> toggles[index] = value);
                toggle.active = !saving;
                addRenderableWidget(toggle);
            }
            save = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE,
                    button -> save()).bounds(width / 2 - 94, height - 26, 91, 20).build());
            addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL,
                    button -> onClose()).bounds(width / 2 + 3, height - 26, 91, 20).build());
            refreshSave();
        }

        private int rowY(int index) {
            return Math.max(27, height / 2 - 73) + 22 * index;
        }

        private static String key(ForgeConfigSpec.ConfigValue<?> value) {
            return String.join(".", value.getPath());
        }

        private static Component label(ForgeConfigSpec.ConfigValue<?> value) {
            return text(key(value));
        }

        private void refreshSave() {
            if (save != null) {
                boolean valid = parsedNumbers() != null;
                save.active = !saving && valid;
                save.setTooltip(valid ? null : Tooltip.create(text("common.invalid")));
            }
        }

        private int[] parsedNumbers() {
            int[] parsed = new int[numbers.length];
            for (int i = 0; i < numbers.length; i++) {
                try {
                    parsed[i] = Integer.parseInt(numbers[i]);
                } catch (NumberFormatException ignored) {
                    return null;
                }
                ForgeConfigSpec.ValueSpec spec = CommonConfig.SPEC.getSpec().get(NUMBERS.get(i).getPath());
                if (!spec.test(parsed[i])) return null;
            }
            return parsed;
        }

        private void save() {
            int[] parsed = parsedNumbers();
            if (saving || parsed == null) return;
            boolean[] values = toggles.clone();
            saving = true;
            refreshSave();
            Runnable commit = () -> {
                RuntimeException failure = null;
                try {
                    if (server != null && server != ServerLifecycleHooks.getCurrentServer()) {
                        throw new IllegalStateException("The integrated server changed");
                    }
                    saveCommon(parsed, values);
                    if (server != null) EpiphanyNetwork.publishSelectionRules(server);
                } catch (RuntimeException exception) {
                    failure = exception;
                    Epiphany.LOGGER.error("Cannot save Epiphany common configuration", exception);
                }
                boolean success = failure == null;
                minecraft.execute(() -> {
                    saving = false;
                    if (minecraft.screen != this) return;
                    if (success) onClose();
                    else {
                        error = text("common.save_failed");
                        refreshSave();
                    }
                });
            };
            if (server != null) server.execute(commit);
            else if (minecraft.getConnection() == null) commit.run();
            else {
                saving = false;
                error = text("common.unavailable");
                refreshSave();
            }
        }

        private static void saveCommon(int[] parsed, boolean[] values) {
            var previousNumbers = NUMBERS.stream().map(ForgeConfigSpec.ConfigValue::get).toList();
            var previousToggles = TOGGLES.stream().map(ForgeConfigSpec.ConfigValue::get).toList();
            for (int i = 0; i < parsed.length; i++) NUMBERS.get(i).set(parsed[i]);
            for (int i = 0; i < values.length; i++) TOGGLES.get(i).set(values[i]);
            try {
                CommonConfig.SPEC.save();
            } catch (RuntimeException failure) {
                for (int i = 0; i < parsed.length; i++) NUMBERS.get(i).set(previousNumbers.get(i));
                for (int i = 0; i < values.length; i++) TOGGLES.get(i).set(previousToggles.get(i));
                throw failure;
            }
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            graphics.drawCenteredString(font, title, width / 2, rowY(0) - 20, 0xffffff);
            int x = width / 2 - 144;
            for (int i = 0; i < numbers.length; i++) {
                graphics.drawString(font, label(NUMBERS.get(i)), x, rowY(i) + 6, 0xffffff);
            }
            for (int i = 0; i < toggles.length; i++) {
                graphics.drawString(font, label(TOGGLES.get(i)), x, rowY(i + numbers.length) + 6, 0xffffff);
            }
            if (error != null) graphics.drawCenteredString(font, error, width / 2, height - 41, 0xff7777);
            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void onClose() {
            if (!saving) minecraft.setScreen(parent);
        }
    }
}

