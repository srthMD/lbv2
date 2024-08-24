package ro.srth.lbv2;

import org.junit.jupiter.api.Test;
import ro.srth.lbv2.util.FastFlags;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FastFlagTests {
    private final FastFlags flags = new FastFlags();

    @Test
    void intTest() {
        var value = (Integer) flags.query("FIntTestFlag");
        assertEquals(2, value);
    }

    @Test
    void stringTest() {
        var value = (String) flags.query("FIntTestFlag");
        assertEquals("Hello", value);
    }
}
