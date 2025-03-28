package ro.srth.lbv2;

import org.junit.jupiter.api.Test;
import ro.srth.lbv2.util.FastFlags;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FastFlagTests {
    private final FastFlags flags;

    {
        try {
            flags = new FastFlags();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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
