package cafe.yamds.ui;

import cafe.yamds.AutoFishingClient;
import cafe.yamds.ModRuntime;
import cafe.yamds.config.AdvanceCurve;
import cafe.yamds.config.AutoFishConfig;
import cafe.yamds.config.CurvePoint;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;

/**
 * 单个 bar 的提前量曲线编辑器（1.21.11 渲染 API）。
 */
public final class CurveEditorScreen extends Screen {

    private final Screen parent;
    private final int barNum;
    private final AdvanceCurve initialCurve;
    private CurveEditorWidget curveWidget;

    public CurveEditorScreen(Screen parent, int barNum) {
        super(Component.literal("Bar " + barNum + " 提前量曲线编辑器"));
        this.parent = parent;
        this.barNum = barNum;

        ModRuntime runtime = Objects.requireNonNull(
                AutoFishingClient.runtime(), "Auto Fishing runtime not initialized");
        List<CurvePoint> points = runtime.configStore().config().barCurves.get(barNum);
        this.initialCurve = AdvanceCurve.of(points);
    }

    @Override
    protected void init() {
        super.init();

        // init() 会因窗口缩放等原因重复调用，保留用户尚未保存的编辑。
        AdvanceCurve curve = curveWidget != null ? curveWidget.curve() : initialCurve;

        int editorWidth = Math.min(600, width - 40);
        int editorHeight = Math.min(400, height - 100);
        int editorX = (width - editorWidth) / 2;
        int editorY = 40;

        curveWidget = new CurveEditorWidget(editorX, editorY, editorWidth, editorHeight, curve);
        addRenderableWidget(curveWidget);

        int buttonY = editorY + editorHeight + 10;

        addRenderableWidget(Button.builder(
                Component.literal("重置为默认"),
                button -> curveWidget.setCurve(AdvanceCurve.of(AutoFishConfig.defaultCurveFor(barNum)))
        ).bounds(width / 2 - 210, buttonY, 100, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("保存"),
                button -> save()
        ).bounds(width / 2 - 105, buttonY, 100, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("取消"),
                button -> minecraft.setScreen(parent)
        ).bounds(width / 2 + 5, buttonY, 100, 20).build());
    }

    private void save() {
        ModRuntime runtime = Objects.requireNonNull(
                AutoFishingClient.runtime(), "Auto Fishing runtime not initialized");
        runtime.configStore().config().barCurves.put(barNum, curveWidget.curve().points());
        runtime.configStore().save();
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                "左键：添加/拖动点 | 右键：删除点 | 横轴：速度(ms) | 纵轴：提前量(格)",
                width / 2, height - 30, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
