package cafe.yamds.detection;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiDecoderTest {

    @Test
    void decodesPositionsForEveryBarFamily() {
        assertEquals(6, decode(1, 6).position());
        assertEquals(22, decode(4, 22).position());
        assertEquals(17, decode(5, 17).position());
        assertEquals(44, decode(7, 44).position());
        assertEquals(21, decode(8, 21).position());
    }

    @Test
    void decodesBarType() {
        assertEquals(1, decode(1, 1).barType());
        assertEquals(9, decode(9, 44).barType());
    }

    @Test
    void clampsPositionToBarBounds() {
        // progress 超出范围时夹到 1..maxPosition
        UiFrame farRight = UiDecoder.decodeFrame(TestUiFrames.frameWithProgress(1, 400)).orElseThrow();
        assertEquals(11, farRight.position());

        UiFrame farLeft = UiDecoder.decodeFrame(TestUiFrames.frameWithProgress(1, -20)).orElseThrow();
        assertEquals(1, farLeft.position());
    }

    @Test
    void rejectsFrameWithoutTwoOffsetGroups() {
        List<TextPart> oneGroup = List.of(
                new TextPart(String.valueOf(TestUiFrames.barChar(1)), TestUiFrames.FONT_BAR),
                new TextPart("", TestUiFrames.FONT_OFFSET)
        );
        assertTrue(UiDecoder.decodeFrame(oneGroup).isEmpty());
    }

    @Test
    void rejectsFrameWithoutBarGlyph() {
        List<TextPart> noBar = List.of(
                new TextPart("hello", "minecraft:default"),
                new TextPart("", TestUiFrames.FONT_OFFSET),
                new TextPart("", TestUiFrames.FONT_OFFSET)
        );
        assertTrue(UiDecoder.decodeFrame(noBar).isEmpty());
    }

    @Test
    void ignoresUnrelatedFonts() {
        List<TextPart> unrelated = List.of(new TextPart("progress", "minecraft:default"));
        assertTrue(UiDecoder.decodeFrame(unrelated).isEmpty());
    }

    @Test
    void supportsUnknownBarTypesWithConservativeFallback() {
        Optional<UiFrame> rainbow = UiDecoder.decodeFrame(TestUiFrames.frame(10, 5));
        assertTrue(rainbow.isPresent());
        assertEquals(10, rainbow.get().barType());
        assertEquals(5, rainbow.get().position());
    }

    @Test
    void detectsGameHintCaseInsensitively() {
        assertTrue(UiDecoder.hintsGame("Fishing Progress"));
        assertTrue(UiDecoder.hintsGame("progress: 0.5"));
        assertFalse(UiDecoder.hintsGame("something else"));
        assertFalse(UiDecoder.hintsGame(null));
    }

    @Test
    void knowsProtocolGlyphs() {
        assertEquals(-183, TestUiFrames.encode(-183).chars()
                .map(c -> UiDecoder.offsetForChar((char) c)).sum());
        assertEquals(0, UiDecoder.barTypeForChar('뀁'));
        assertEquals(4, UiDecoder.barTypeForChar('뀅'));
        assertEquals(11, UiDecoder.barTypeForChar('뀌'));
        assertEquals(-1, UiDecoder.barTypeForChar('x'));
    }

    private static UiFrame decode(int barNum, int position) {
        return UiDecoder.decodeFrame(TestUiFrames.frame(barNum, position)).orElseThrow();
    }
}
