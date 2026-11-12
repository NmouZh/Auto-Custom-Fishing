package cafe.yamds.ui;

import cafe.yamds.config.AdvanceCurve;
import cafe.yamds.config.CurvePoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 可拖拽的提前量曲线编辑控件。
 *
 * <p>左键添加/拖动控制点，右键删除。横轴为速度（左 35ms → 右 0ms），
 * 纵轴为提前量（-1 到 3 格）。
 */
public final class CurveEditorWidget extends AbstractWidget {

    private static final int PADDING = 10;
    private static final int POINT_RADIUS = 4;

    private static final int GRID_COLOR = 0xFF404040;
    private static final int AXIS_COLOR = 0xFF808080;
    private static final int CURVE_COLOR = 0xFF00FF00;
    private static final int POINT_COLOR = 0xFFFFFFFF;
    private static final int POINT_HOVER_COLOR = 0xFFFFFF00;

    private static final double MIN_SPEED = 0.0;
    private static final double MAX_SPEED = 35.0;
    private static final double MIN_ADVANCE = -1.0;
    private static final double MAX_ADVANCE = 3.0;
    private static final double SNAP = 0.1;
    private static final double HIT_RADIUS = 2.0;

    private AdvanceCurve curve;
    private CurvePoint draggedPoint;

    public CurveEditorWidget(int x, int y, int width, int height, AdvanceCurve curve) {
        super(x, y, width, height, Component.literal("Curve Editor"));
        this.curve = curve;
    }

    public AdvanceCurve curve() {
        return curve;
    }

    public void setCurve(AdvanceCurve curve) {
        this.curve = curve;
        this.draggedPoint = null;
    }

    // ========== 坐标换算 ==========

    private int plotX() {
        return getX() + PADDING;
    }

    private int plotY() {
        return getY() + PADDING;
    }

    private int plotWidth() {
        return width - 2 * PADDING;
    }

    private int plotHeight() {
        return height - 2 * PADDING;
    }

    /** 横轴反向：35ms 在左，0ms 在右。 */
    private int screenX(double speedMs) {
        return plotX() + Math.round((float) ((MAX_SPEED - speedMs) / (MAX_SPEED - MIN_SPEED) * plotWidth()));
    }

    private int screenY(double advance) {
        return plotY() + Math.round((float) ((MAX_ADVANCE - advance) / (MAX_ADVANCE - MIN_ADVANCE) * plotHeight()));
    }

    private double speedAt(double mouseX) {
        double raw = MAX_SPEED - (mouseX - plotX()) / (double) plotWidth() * (MAX_SPEED - MIN_SPEED);
        return snapClamp(raw, MIN_SPEED, MAX_SPEED);
    }

    private double advanceAt(double mouseY) {
        double raw = MAX_ADVANCE - (mouseY - plotY()) / (double) plotHeight() * (MAX_ADVANCE - MIN_ADVANCE);
        return snapClamp(raw, MIN_ADVANCE, MAX_ADVANCE);
    }

    private static double snapClamp(double value, double min, double max) {
        double snapped = Math.round(value / SNAP) * SNAP;
        return Math.max(min, Math.min(max, snapped));
    }

    // ========== 渲染 ==========

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000);

        drawGrid(graphics);
        drawAxes(graphics);
        drawCurve(graphics);
        drawPoints(graphics, mouseX, mouseY);
        drawLabels(graphics);
    }

    private void drawGrid(GuiGraphicsExtractor graphics) {
        int x = plotX();
        int y = plotY();

        // 垂直网格：7 等分，每份 5ms
        for (int i = 0; i <= 7; i++) {
            int gridX = x + plotWidth() - (int) (i * plotWidth() / 7.0);
            graphics.fill(gridX, y, gridX + 1, y + plotHeight(), GRID_COLOR);
        }

        // 水平网格：8 等分，每份 0.5 格
        for (int i = 0; i <= 8; i++) {
            int gridY = y + plotHeight() - (int) (i * plotHeight() / 8.0);
            graphics.fill(x, gridY, x + plotWidth(), gridY + 1, GRID_COLOR);
        }
    }

    private void drawAxes(GuiGraphicsExtractor graphics) {
        int x = plotX();
        int y = plotY();
        graphics.fill(x, y + plotHeight(), x + plotWidth(), y + plotHeight() + 1, AXIS_COLOR);
        graphics.fill(x, y, x + 1, y + plotHeight(), AXIS_COLOR);
    }

    private void drawCurve(GuiGraphicsExtractor graphics) {
        int previousX = -1;
        int previousY = -1;

        for (int i = 0; i <= 100; i++) {
            double speed = MAX_SPEED - (MAX_SPEED - MIN_SPEED) * i / 100.0;
            int curveX = screenX(speed);
            int curveY = screenY(curve.advanceAt(speed));

            if (previousX >= 0) {
                drawLine(graphics, previousX, previousY, curveX, curveY, CURVE_COLOR);
            }
            previousX = curveX;
            previousY = curveY;
        }
    }

    private void drawPoints(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (CurvePoint point : curve.points()) {
            int pointX = screenX(point.speedMs());
            int pointY = screenY(point.advanceAmount());
            boolean hovered = distanceSquared(mouseX, mouseY, pointX, pointY)
                    < (POINT_RADIUS * 2) * (POINT_RADIUS * 2);

            int color = hovered ? POINT_HOVER_COLOR : POINT_COLOR;
            graphics.fill(pointX - POINT_RADIUS, pointY - POINT_RADIUS,
                    pointX + POINT_RADIUS, pointY + POINT_RADIUS, color);

            if (hovered) {
                String label = String.format("%.1fms, %.1f", point.speedMs(), point.advanceAmount());
                graphics.text(Minecraft.getInstance().font, label, pointX + 8, pointY - 4, 0xFFFFFFFF);
            }
        }
    }

    private void drawLabels(GuiGraphicsExtractor graphics) {
        Font font = Minecraft.getInstance().font;
        int x = plotX();
        int y = plotY();

        graphics.text(font, "35ms", x - 15, y + plotHeight() + 5, 0xFFFFFFFF);
        graphics.text(font, "0ms", x + plotWidth() - 15, y + plotHeight() + 5, 0xFFFFFFFF);
        graphics.text(font, "速度 (ms/section)", x + plotWidth() / 2 - 40, y + plotHeight() + 15, 0xFFFFFFFF);

        graphics.text(font, "3", x - 15, y - 5, 0xFFFFFFFF);
        graphics.text(font, "1", x - 15, y + plotHeight() / 2 - 5, 0xFFFFFFFF);
        graphics.text(font, "-1", x - 20, y + plotHeight() - 5, 0xFFFFFFFF);
    }

    private static void drawLine(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int stepX = x1 < x2 ? 1 : -1;
        int stepY = y1 < y2 ? 1 : -1;
        int error = dx - dy;

        while (true) {
            graphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) {
                return;
            }
            int doubled = 2 * error;
            if (doubled > -dy) {
                error -= dy;
                x1 += stepX;
            }
            if (doubled < dx) {
                error += dx;
                y1 += stepY;
            }
        }
    }

    private static long distanceSquared(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.round(dx * dx + dy * dy);
    }

    // ========== 输入 ==========

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }

        int button = event.buttonInfo().button();
        double speed = speedAt(mouseX);
        double advance = advanceAt(mouseY);

        if (button == 0) {
            CurvePoint nearest = curve.nearest(speed, advance, HIT_RADIUS);
            if (nearest != null) {
                draggedPoint = nearest;
            } else {
                curve = curve.withPoint(speed, advance);
            }
            return true;
        }

        if (button == 1) {
            CurvePoint nearest = curve.nearest(speed, advance, HIT_RADIUS);
            if (nearest != null) {
                curve = curve.withoutPoint(nearest);
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggedPoint == null) {
            return false;
        }

        double speed = speedAt(event.x());
        double advance = advanceAt(event.y());

        // AdvanceCurve 不可变：删掉旧点、加入新点，并记住新点用于继续拖拽。
        curve = curve.withoutPoint(draggedPoint).withPoint(speed, advance);
        draggedPoint = new CurvePoint(speed, advance);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.buttonInfo().button() == 0 && draggedPoint != null) {
            draggedPoint = null;
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.literal("Advance Curve Editor"));
    }
}
