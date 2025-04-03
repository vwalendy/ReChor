package ch.epfl.rechor;

import ch.epfl.rechor.timetable.mapped.Structure;
import ch.epfl.rechor.timetable.mapped.StructuredBuffer;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.HexFormat;

import static ch.epfl.rechor.timetable.mapped.Structure.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MyStructureBufferedTest {

    // Pour les tests, on définit une structure composée de trois champs :
    // - champ 0 : U8 (1 octet)
    // - champ 1 : U16 (2 octets)
    // - champ 2 : S32 (4 octets)
    // totalSize = 1 + 2 + 4 = 7 octets par élément.
    private static final Structure STRUCTURE = new Structure(
            field(0, Structure.FieldType.U8),
            field(1, Structure.FieldType.U16),
            field(2, Structure.FieldType.S32)
    );

    @Test
    void testConstructorInvalidBufferCapacity() {
        // On crée un ByteBuffer dont la capacité n'est pas un multiple de 7.
        ByteBuffer buffer = ByteBuffer.allocate(10);
        assertThrows(IllegalArgumentException.class, () -> {
            new StructuredBuffer(STRUCTURE2, buffer);
        });
    }

    @Test
    void testSize() {
        // On crée un ByteBuffer de capacité 21 (3 éléments de 7 octets).
        ByteBuffer buffer = ByteBuffer.allocate(21);
        StructuredBuffer sb = new StructuredBuffer(STRUCTURE2, buffer);
        assertEquals(3, sb.size());
    }

    @Test
    void testGetValuesSingleElement() {
        // On construit un ByteBuffer pour un seul élément (7 octets).
        // Pour l'élément 0, on va écrire :
        // - U8 : 0xFF (255 en décimal)
        // - U16 : 0x1234 (4660 en décimal)
        // - S32 : 42 (valeur positive)
        ByteBuffer buffer = ByteBuffer.allocate(7);
        buffer.put((byte) 0xFF);      // U8
        buffer.putShort((short) 0x1234);  // U16
        buffer.putInt(42);            // S32
        buffer.flip();

        StructuredBuffer sb = new StructuredBuffer(STRUCTURE2, buffer);
        assertEquals(255, sb.getU8(0, 0));
        assertEquals(4660, sb.getU16(1, 0));
        assertEquals(42, sb.getS32(2, 0));
    }

    @Test
    void testGetValuesMultipleElements() {
        // On construit un ByteBuffer pour 2 éléments (2 * 7 = 14 octets).
        // Pour l'élément 0, on écrit :
        // - U8 : 100
        // - U16 : 2000
        // - S32 : -300
        //
        // Pour l'élément 1, on écrit :
        // - U8 : 50
        // - U16 : 65535 (max unsigned short)
        // - S32 : 123456789
        ByteBuffer buffer = ByteBuffer.allocate(14);
        // Écriture de l'élément 0
        buffer.put((byte) 100);           // U8
        buffer.putShort((short) 2000);      // U16
        buffer.putInt(-300);              // S32
        // Écriture de l'élément 1
        buffer.put((byte) 50);            // U8
        buffer.putShort((short) 65535);    // U16 : 65535 devient -1 en short, mais getU16 doit l'interpréter en unsigned
        buffer.putInt(123456789);         // S32
        buffer.flip();

        StructuredBuffer sb = new StructuredBuffer(STRUCTURE2, buffer);
        // Vérification de l'élément 0
        assertEquals(100, sb.getU8(0, 0));
        assertEquals(2000, sb.getU16(1, 0));
        assertEquals(-300, sb.getS32(2, 0));

        // Vérification de l'élément 1
        assertEquals(50, sb.getU8(0, 1));
        assertEquals(65535, sb.getU16(1, 1));
        assertEquals(123456789, sb.getS32(2, 1));
    }

    @Test
    void testIndexOutOfBoundsForFieldIndex() {
        // On crée un ByteBuffer pour un élément.
        ByteBuffer buffer = ByteBuffer.allocate(7);
        // Remplissage quelconque
        buffer.put(new byte[7]).flip();
        StructuredBuffer sb = new StructuredBuffer(STRUCTURE2, buffer);

        // La structure possède 3 champs (indices 0, 1 et 2) donc index 3 devrait lever une exception.
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getU8(3, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getU16(3, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getS32(3, 0));
    }

    @Test
    void testIndexOutOfBoundsForElementIndex() {
        // On crée un ByteBuffer pour 2 éléments (14 octets).
        ByteBuffer buffer = ByteBuffer.allocate(14);
        buffer.put(new byte[14]).flip();
        StructuredBuffer sb = new StructuredBuffer(STRUCTURE2, buffer);

        // La méthode offset de Structure (et donc getX) lèvera une exception pour un élément négatif ou supérieur ou égal à size.
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getU8(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getU16(1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getS32(2, 100));
    }

    @Test
    void testLargeBufferAccess() {
        // Test avec un grand nombre d'éléments pour vérifier la robustesse.
        int numberOfElements = 1_000;
        int capacity = numberOfElements * STRUCTURE2.totalSize();
        ByteBuffer buffer = ByteBuffer.allocate(capacity);

        // Pour simplifier, on écrit pour chaque élément :
        // U8 : (byte) elementIndex, U16 : (short) (elementIndex + 1000), S32 : elementIndex * 2
        for (int i = 0; i < numberOfElements; i++) {
            buffer.put((byte) i);                         // U8
            buffer.putShort((short) (i + 1000));            // U16
            buffer.putInt(i * 2);                           // S32
        }
        buffer.flip();
        StructuredBuffer sb = new StructuredBuffer(STRUCTURE2, buffer);

        assertEquals(numberOfElements, sb.size());
        // Vérification sur quelques éléments
        for (int i : new int[]{0, 10, numberOfElements - 1}) {
            assertEquals(Byte.toUnsignedInt((byte) i), sb.getU8(0, i));
            assertEquals(Short.toUnsignedInt((short) (i + 1000)), sb.getU16(1, i));
            assertEquals(i * 2, sb.getS32(2, i));
        }
    }

    // On utilise la même structure de test : 1 octet pour U8, 2 pour U16, 4 pour S32, soit 7 octets par élément.
    private static final Structure STRUCTURE2 = new Structure(
            field(0, Structure.FieldType.U8),
            field(1, Structure.FieldType.U16),
            field(2, Structure.FieldType.S32)
    );

    @Test
    void testEmptyBuffer() {
        // Un ByteBuffer vide est accepté car 0 % totalSize == 0.
        ByteBuffer buffer = ByteBuffer.allocate(0);
        StructuredBuffer sb = new StructuredBuffer(STRUCTURE2, buffer);
        assertEquals(0, sb.size());

        // Accéder à un élément inexistant doit lever une IndexOutOfBoundsException.
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getU8(0, 0));
    }

    @Test
    void testNegativeFieldIndex() {
        // Création d'un buffer avec un élément valide.
        ByteBuffer buffer = ByteBuffer.allocate(7);
        // Remplissage arbitraire
        buffer.put(new byte[7]).flip();
        StructuredBuffer sb = new StructuredBuffer(STRUCTURE2, buffer);

        // Un fieldIndex négatif doit lever une IndexOutOfBoundsException.
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getU8(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getU16(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getS32(-1, 0));
    }

    @Test
    void testNegativeElementIndex() {
        // Buffer avec 2 éléments (14 octets)
        ByteBuffer buffer = ByteBuffer.allocate(14);
        buffer.put(new byte[14]).flip();
        StructuredBuffer sb = new StructuredBuffer(STRUCTURE2, buffer);

        // Un élément négatif doit lever une exception.
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getU8(0, -5));
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getU16(1, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getS32(2, -10));
    }

    @Test
    void testLastValidElement() {
        // Création d'un buffer pour 5 éléments.
        int nbElements = 5;
        ByteBuffer buffer = ByteBuffer.allocate(nbElements * STRUCTURE2.totalSize());

        // Remplissage : pour chaque élément, on écrit des valeurs simples en fonction de l'indice.
        // Pour l'élément i :
        //  - U8 : i
        //  - U16 : i + 10
        //  - S32 : i * 100
        for (int i = 0; i < nbElements; i++) {
            buffer.put((byte) i);                    // U8
            buffer.putShort((short) (i + 10));         // U16
            buffer.putInt(i * 100);                    // S32
        }
        buffer.flip();
        StructuredBuffer sb = new StructuredBuffer(STRUCTURE2, buffer);

        // Vérifier que l'élément d'indice nbElements-1 est accessible et correct.
        int lastIndex = nbElements - 1;
        assertEquals(lastIndex, sb.getU8(0, lastIndex));
        assertEquals(lastIndex + 10, sb.getU16(1, lastIndex));
        assertEquals(lastIndex * 100, sb.getS32(2, lastIndex));

        // Vérifier qu'un accès à l'élément nbElements (hors bornes) lève une exception.
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getU8(0, nbElements));
    }

    @Test
    void testValueBoundaries() {
        // Test avec des valeurs limites pour chaque type.
        // Pour U8, tester 0 et 255.
        // Pour U16, tester 0 et 65535.
        // Pour S32, tester Integer.MIN_VALUE et Integer.MAX_VALUE.
        ByteBuffer buffer = ByteBuffer.allocate(2 * STRUCTURE2.totalSize());
        // Premier élément
        buffer.put((byte) 0);               // U8 = 0
        buffer.putShort((short) 0);          // U16 = 0
        buffer.putInt(Integer.MIN_VALUE);    // S32 = Integer.MIN_VALUE

        // Second élément
        buffer.put((byte) 0xFF);             // U8 = 255
        buffer.putShort((short) 0xFFFF);       // U16 = 65535 (0xFFFF, interprété en unsigned)
        buffer.putInt(Integer.MAX_VALUE);    // S32 = Integer.MAX_VALUE
        buffer.flip();

        StructuredBuffer sb = new StructuredBuffer(STRUCTURE2, buffer);

        // Premier élément
        assertEquals(0, sb.getU8(0, 0));
        assertEquals(0, sb.getU16(1, 0));
        assertEquals(Integer.MIN_VALUE, sb.getS32(2, 0));

        // Second élément
        assertEquals(255, sb.getU8(0, 1));
        assertEquals(65535, sb.getU16(1, 1));
        assertEquals(Integer.MAX_VALUE, sb.getS32(2, 1));
    }

    @Test
    void testBufferModificationAfterCreation() {
        // On crée un buffer pour un élément et on teste que la lecture reflète bien les modifications.
        ByteBuffer buffer = ByteBuffer.allocate(7);
        // On remplit initialement avec des zéros.
        for (int i = 0; i < 7; i++) {
            buffer.put((byte) 0);
        }
        buffer.flip();
        StructuredBuffer sb = new StructuredBuffer(STRUCTURE2, buffer);

        // Initialement, toutes les valeurs doivent être nulles.
        assertEquals(0, sb.getU8(0, 0));
        assertEquals(0, sb.getU16(1, 0));
        assertEquals(0, sb.getS32(2, 0));

        // Modification directe du ByteBuffer : on remplace les 7 octets par de nouvelles valeurs.
        buffer.rewind();
        buffer.put((byte) 123);               // U8
        buffer.putShort((short) 4567);         // U16
        buffer.putInt(-890);                  // S32
        buffer.flip();

        // La lecture doit refléter les nouvelles valeurs.
        assertEquals(123, sb.getU8(0, 0));
        assertEquals(4567, sb.getU16(1, 0));
        assertEquals(-890, sb.getS32(2, 0));
    }


    @Test
    void sizeU16() {
        Structure a = new Structure(field(0, Structure.FieldType.U16));
        HexFormat hexFormat = HexFormat.ofDelimiter(" ");
        byte[] bytes = hexFormat.parseHex("00 05 00 04 00 02 00 03");
        ByteBuffer b = ByteBuffer.wrap(bytes);
        StructuredBuffer c = new StructuredBuffer(a, b);
        assertEquals(4, c.size());
    }

    @Test
    void sizeS32() {
        Structure a = new Structure(field(0, Structure.FieldType.S32));
        HexFormat hexFormat = HexFormat.ofDelimiter(" ");
        byte[] bytes = hexFormat.parseHex("00 05 00 04 00 02 00 03 00 00 01 23 AF 57 00 90");
        ByteBuffer b = ByteBuffer.wrap(bytes);
        StructuredBuffer c = new StructuredBuffer(a, b);
        assertEquals(4, c.size());
    }

    @Test
    void sizeU8() {
        Structure a = new Structure(field(0, Structure.FieldType.U8));
        HexFormat hexFormat = HexFormat.ofDelimiter(" ");
        byte[] bytes = hexFormat.parseHex("00 05 00 04 00 02 00 03 00 00 01 23 AF 57 00 90");
        ByteBuffer b = ByteBuffer.wrap(bytes);
        StructuredBuffer c = new StructuredBuffer(a, b);
        assertEquals(16, c.size());
    }

    @Test
    void ConstructeurException() {
        Structure a = new Structure(field(0, Structure.FieldType.S32));
        HexFormat hexFormat = HexFormat.ofDelimiter(" ");
        byte[] bytes = hexFormat.parseHex("00 05 00 04 00 02 00 03 00 00 01 23 AF 57 00");
        ByteBuffer b = ByteBuffer.wrap(bytes);
        assertThrows(IllegalArgumentException.class, () -> {
            new StructuredBuffer(a, b);
        });

    }

    @Test
    void getU8() {
        Structure a = new Structure(field(0, Structure.FieldType.S32),
                field(1, Structure.FieldType.U8));
        HexFormat hexFormat = HexFormat.ofDelimiter(" ");
        byte[] bytes = hexFormat.parseHex("00 05 00 04 00 02 00 03 00 00 01 23 AF 57 00");
        ByteBuffer b = ByteBuffer.wrap(bytes);
        StructuredBuffer c = new StructuredBuffer(a, b);
        assertEquals(00, c.getU8(1, 0));
    }

    @Test
    void getU16() {
        Structure a = new Structure(field(0, Structure.FieldType.U16),
                field(1, Structure.FieldType.U8));
        HexFormat hexFormat = HexFormat.ofDelimiter(" ");
        byte[] bytes = hexFormat.parseHex("00 05 00 04 00 02 00 03 00 00 01 23 AF 57 00");
        ByteBuffer b = ByteBuffer.wrap(bytes);
        StructuredBuffer c = new StructuredBuffer(a, b);
        assertEquals(0x0001, c.getU16(0, 3));
    }

    @Test
    void getS32() {
        Structure a = new Structure(field(0, Structure.FieldType.S32),
                field(1, Structure.FieldType.U8));
        HexFormat hexFormat = HexFormat.ofDelimiter(" ");
        byte[] bytes = hexFormat.parseHex("00 05 00 04 00 02 00 03 00 00 01 23 AF 57 00");
        ByteBuffer b = ByteBuffer.wrap(bytes);
        StructuredBuffer c = new StructuredBuffer(a, b);
        assertEquals(0x00050004, c.getS32(0, 0));
    }


    @Test
    void getU8Illleagal1() {
        Structure a = new Structure(field(0, Structure.FieldType.S32),
                field(1, Structure.FieldType.U8));
        HexFormat hexFormat = HexFormat.ofDelimiter(" ");
        byte[] bytes = hexFormat.parseHex("00 05 00 04 00 02 00 03 00 00 01 23 AF 57 00");
        ByteBuffer b = ByteBuffer.wrap(bytes);
        StructuredBuffer c = new StructuredBuffer(a, b);
        assertThrows(IndexOutOfBoundsException.class, () -> {
            c.getU8(1, 20);
        });
    }

    @Test
    void getU16Illegal1() {
        Structure a = new Structure(field(0, Structure.FieldType.U16),
                field(1, Structure.FieldType.U8));
        HexFormat hexFormat = HexFormat.ofDelimiter(" ");
        byte[] bytes = hexFormat.parseHex("00 05 00 04 00 02 00 03 00 00 01 23 AF 57 00");
        ByteBuffer b = ByteBuffer.wrap(bytes);
        StructuredBuffer c = new StructuredBuffer(a, b);
        assertThrows(IndexOutOfBoundsException.class, () -> {
            c.getU8(0, 20);
        });
    }

    @Test
    void getS32Illegal1() {
        Structure a = new Structure(field(0, Structure.FieldType.S32),
                field(1, Structure.FieldType.U8));
        HexFormat hexFormat = HexFormat.ofDelimiter(" ");
        byte[] bytes = hexFormat.parseHex("00 05 00 04 00 02 00 03 00 00 01 23 AF 57 00");
        ByteBuffer b = ByteBuffer.wrap(bytes);
        StructuredBuffer c = new StructuredBuffer(a, b);
        assertThrows(IndexOutOfBoundsException.class, () -> {
            c.getS32(0, 20);
        });
    }

    @Test
    void getU8Illleagal2() {
        Structure a = new Structure(field(0, Structure.FieldType.S32),
                field(1, Structure.FieldType.U8));
        HexFormat hexFormat = HexFormat.ofDelimiter(" ");
        byte[] bytes = hexFormat.parseHex("00 05 00 04 00 02 00 03 00 00 01 23 AF 57 00");
        ByteBuffer b = ByteBuffer.wrap(bytes);
        StructuredBuffer c = new StructuredBuffer(a, b);
        assertThrows(IndexOutOfBoundsException.class, () -> {
            c.getU8(1, -1);
        });
    }

    @Test
    void getU16Illegal2() {
        Structure a = new Structure(field(0, Structure.FieldType.U16),
                field(1, Structure.FieldType.U8));
        HexFormat hexFormat = HexFormat.ofDelimiter(" ");
        byte[] bytes = hexFormat.parseHex("00 05 00 04 00 02 00 03 00 00 01 23 AF 57 00");
        ByteBuffer b = ByteBuffer.wrap(bytes);
        StructuredBuffer c = new StructuredBuffer(a, b);
        assertThrows(IndexOutOfBoundsException.class, () -> {
            c.getU16(0, -1);
        });
    }

    @Test
    void getS32Illegal2() {
        Structure a = new Structure(field(0, Structure.FieldType.S32),
                field(1, Structure.FieldType.U8));
        HexFormat hexFormat = HexFormat.ofDelimiter(" ");
        byte[] bytes = hexFormat.parseHex("00 05 00 04 00 02 00 03 00 00 01 23 AF 57 00");
        ByteBuffer b = ByteBuffer.wrap(bytes);
        StructuredBuffer c = new StructuredBuffer(a, b);
        assertThrows(IndexOutOfBoundsException.class, () -> {
            c.getS32(0, -1);
        });
    }

    @Test
    void getU8Illleagal3() {
        Structure a = new Structure(field(0, Structure.FieldType.S32),
                field(1, Structure.FieldType.U8));
        HexFormat hexFormat = HexFormat.ofDelimiter(" ");
        byte[] bytes = hexFormat.parseHex("00 05 00 04 00 02 00 03 00 00 01 23 AF 57 00");
        ByteBuffer b = ByteBuffer.wrap(bytes);
        StructuredBuffer c = new StructuredBuffer(a, b);
        assertThrows(IndexOutOfBoundsException.class, () -> {
            c.getU8(-1, 1);
        });
    }

    @Test
    void getU16Illegal3() {
        Structure a = new Structure(field(0, Structure.FieldType.U16),
                field(1, Structure.FieldType.U8));
        HexFormat hexFormat = HexFormat.ofDelimiter(" ");
        byte[] bytes = hexFormat.parseHex("00 05 00 04 00 02 00 03 00 00 01 23 AF 57 00");
        ByteBuffer b = ByteBuffer.wrap(bytes);
        StructuredBuffer c = new StructuredBuffer(a, b);
        assertThrows(IndexOutOfBoundsException.class, () -> {
            c.getU16(-1, 1);
        });
    }

    @Test
    void getS32Illegal3() {
        Structure a = new Structure(field(0, Structure.FieldType.S32),
                field(1, Structure.FieldType.U8));
        HexFormat hexFormat = HexFormat.ofDelimiter(" ");
        byte[] bytes = hexFormat.parseHex("00 05 00 04 00 02 00 03 00 00 01 23 AF 57 00");
        ByteBuffer b = ByteBuffer.wrap(bytes);
        StructuredBuffer c = new StructuredBuffer(a, b);
        assertThrows(IndexOutOfBoundsException.class, () -> {
            c.getS32(-1, 2);
        });
    }

    @Test
    void testStructuredBuffer() {
        Structure structure = new Structure(
                field(0, Structure.FieldType.U16),
                field(1, Structure.FieldType.S32)
        );

        byte[] bytes = {0x00, 0x01, 0x00, 0x00, 0x00, 0x02, 0x00, 0x03, 0x00, 0x00, 0x00, 0x04};
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        StructuredBuffer structuredBuffer = new StructuredBuffer(structure, buffer);

        assertEquals(2, structuredBuffer.size());
        assertEquals(1, structuredBuffer.getU16(0, 0));
        assertEquals(2, structuredBuffer.getS32(1, 0));
        assertEquals(3, structuredBuffer.getU16(0, 1));
        assertEquals(4, structuredBuffer.getS32(1, 1));
    }
}
