package ch.epfl.rechor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Mytest32_24_8test {

    @Test
    void testValidCases() {
        int packed1 = Bits32_24_8.pack(0, 0);
        assertEquals(0, Bits32_24_8.unpack24(packed1));
        assertEquals(0, Bits32_24_8.unpack8(packed1));
    }
    @Test
    void testValidCases2() {
        int packed2 = Bits32_24_8.pack(0xFFFFFF, 255);
        assertEquals(0xFFFFFF, Bits32_24_8.unpack24(packed2));
        assertEquals(255, Bits32_24_8.unpack8(packed2));
    }
    @Test
    void testValidCases3() {
        int packed3 = Bits32_24_8.pack(0xFFFFFF, 0);
        assertEquals(0xFFFFFF, Bits32_24_8.unpack24(packed3));
        assertEquals(0, Bits32_24_8.unpack8(packed3));
    }
    @Test
    void testValidCases4(){
    int packed4 = Bits32_24_8.pack(0, 255);
    assertEquals(0, Bits32_24_8.unpack24(packed4));
    assertEquals(255, Bits32_24_8.unpack8(packed4));
}
}
