package cafe.yamds.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdvanceCurveTest {

    @Test
    void interpolatesLinearly() {
        AdvanceCurve curve = AdvanceCurve.of(List.of(
                new CurvePoint(0.0, 1.0),
                new CurvePoint(10.0, 0.0)
        ));

        assertEquals(1.0, curve.advanceAt(0.0), 1e-9);
        assertEquals(0.5, curve.advanceAt(5.0), 1e-9);
        assertEquals(0.0, curve.advanceAt(10.0), 1e-9);
    }

    @Test
    void clampsOutsideConfiguredRange() {
        AdvanceCurve curve = AdvanceCurve.of(List.of(
                new CurvePoint(5.0, 2.0),
                new CurvePoint(10.0, 0.0)
        ));

        assertEquals(2.0, curve.advanceAt(0.0), 1e-9);
        assertEquals(0.0, curve.advanceAt(35.0), 1e-9);
    }

    @Test
    void sortsPointsOnConstruction() {
        AdvanceCurve curve = AdvanceCurve.of(List.of(
                new CurvePoint(30.0, 3.0),
                new CurvePoint(0.0, 0.0)
        ));

        assertEquals(0.0, curve.points().get(0).speedMs(), 1e-9);
        assertEquals(30.0, curve.points().get(1).speedMs(), 1e-9);
    }

    @Test
    void clampsPointValuesToValidRanges() {
        AdvanceCurve curve = AdvanceCurve.of(List.of(
                new CurvePoint(-5.0, 99.0),
                new CurvePoint(99.0, -99.0)
        ));

        assertEquals(0.0, curve.points().get(0).speedMs(), 1e-9);
        assertEquals(AdvanceCurve.MAX_ADVANCE, curve.points().get(0).advanceAmount(), 1e-9);
        assertEquals(AdvanceCurve.MAX_SPEED, curve.points().get(1).speedMs(), 1e-9);
        assertEquals(AdvanceCurve.MIN_ADVANCE, curve.points().get(1).advanceAmount(), 1e-9);
    }

    @Test
    void addsAndRemovesPointsImmutably() {
        AdvanceCurve base = AdvanceCurve.of(List.of(
                new CurvePoint(0.0, 1.0),
                new CurvePoint(35.0, 0.0)
        ));

        AdvanceCurve withPoint = base.withPoint(10.0, 2.0);
        assertEquals(3, withPoint.points().size());
        assertEquals(2, base.points().size());

        AdvanceCurve withoutPoint = withPoint.withoutPoint(new CurvePoint(10.0, 2.0));
        assertEquals(2, withoutPoint.points().size());
    }

    @Test
    void neverDropsBelowTwoPoints() {
        AdvanceCurve base = AdvanceCurve.of(List.of(
                new CurvePoint(0.0, 1.0),
                new CurvePoint(35.0, 0.0)
        ));

        assertEquals(2, base.withoutPoint(new CurvePoint(0.0, 1.0)).points().size());
    }

    @Test
    void findsNearestPointWithinThreshold() {
        AdvanceCurve curve = AdvanceCurve.of(List.of(
                new CurvePoint(0.0, 1.0),
                new CurvePoint(10.0, 0.0)
        ));

        CurvePoint nearest = curve.nearest(10.1, 0.05, 0.5);
        assertEquals(10.0, nearest.speedMs(), 1e-9);

        assertNull(curve.nearest(20.0, -1.0, 0.5));
    }

    @Test
    void fallsBackToDefaultsWhenInsufficientData() {
        assertEquals(2, AdvanceCurve.of(List.of()).points().size());
        assertEquals(2, AdvanceCurve.of(null).points().size());
        assertThrows(UnsupportedOperationException.class, () -> AdvanceCurve.of(List.of(
                new CurvePoint(0.0, 1.0),
                new CurvePoint(10.0, 2.0)
        )).points().add(new CurvePoint(1.0, 1.0)));
    }
}
