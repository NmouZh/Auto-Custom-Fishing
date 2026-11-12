package cafe.yamds.ui;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Cloth Config 中的“打开曲线编辑器”按钮行（1.21.11 渲染 API）。
 */
public final class OpenCurveEditorEntry extends AbstractConfigListEntry<Void> {

    private static final int BUTTON_WIDTH = 150;
    private static final int HEIGHT = 20;

    private final int barNum;
    private Button button;

    public OpenCurveEditorEntry(int barNum) {
        super(Component.translatable("config.autofishing.curve.open", barNum), false);
        this.barNum = barNum;
    }

    @Override
    public Void getValue() {
        return null;
    }

    @Override
    public Optional<Void> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public void save() {
        // 按钮本身没有需要保存的值
    }

    @Override
    public int getItemHeight() {
        return HEIGHT;
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return button == null ? Collections.emptyList() : Collections.singletonList(button);
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return button == null ? Collections.emptyList() : Collections.singletonList(button);
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x,
                       int entryWidth, int entryHeight, int mouseX, int mouseY,
                       boolean isHovered, float delta) {
        int buttonX = x + entryWidth / 2 - BUTTON_WIDTH / 2;
        if (button == null) {
            button = Button.builder(
                            Component.translatable("config.autofishing.curve.open.button"),
                            clicked -> {
                                Minecraft minecraft = Minecraft.getInstance();
                                minecraft.setScreen(new CurveEditorScreen(minecraft.screen, barNum));
                            })
                    .bounds(buttonX, y, BUTTON_WIDTH, HEIGHT)
                    .build();
        } else {
            button.setX(buttonX);
            button.setY(y);
        }
        button.render(graphics, mouseX, mouseY, delta);
    }
}
