package cafe.yamds.detection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BarTableTest {

    @Test
    void successRatesMatchCustomFishingTables() {
        assertEquals(1.0, BarTable.byType(1).successRate(6));
        assertEquals(0.6, BarTable.byType(1).successRate(5));
        assertEquals(0.0, BarTable.byType(1).successRate(1));

        assertEquals(1.0, BarTable.byType(4).successRate(11));
        assertEquals(1.0, BarTable.byType(4).successRate(17));
        assertEquals(0.1, BarTable.byType(4).successRate(20));

        assertEquals(1.0, BarTable.byType(9).successRate(44));
        assertEquals(0.0, BarTable.byType(9).successRate(1));
    }

    @Test
    void maxPositionsMatchBarFamilies() {
        assertEquals(11, BarTable.byType(1).maxPosition());
        assertEquals(22, BarTable.byType(5).maxPosition());
        assertEquals(44, BarTable.byType(8).maxPosition());
    }

    @Test
    void outOfRangePositionsHaveNoSuccess() {
        assertEquals(0.0, BarTable.byType(1).successRate(0));
        assertEquals(0.0, BarTable.byType(1).successRate(12));
    }

    @Test
    void findsNextPerfectPosition() {
        BarTable.BarInfo bar1 = BarTable.byType(1);
        assertEquals(6, bar1.nextPerfectPosition(4, 5));
        assertEquals(6, bar1.nextPerfectPosition(6, 5));
        assertTrue(bar1.isPerfect(6));

        BarTable.BarInfo bar9 = BarTable.byType(9);
        assertEquals(44, bar9.nextPerfectPosition(40, 5));
        assertFalse(bar9.isPerfect(43));
    }

    @Test
    void unknownBarsFallBackToConservativeDefaults() {
        BarTable.BarInfo fallback = BarTable.byType(10);
        assertEquals(11, fallback.maxPosition());
        assertEquals(16, fallback.widthPerSection());
        for (int position = 1; position <= 11; position++) {
            assertEquals(0.0, fallback.successRate(position));
        }
    }
}
