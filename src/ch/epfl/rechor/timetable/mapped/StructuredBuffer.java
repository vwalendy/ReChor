package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.Preconditions;
import java.nio.ByteBuffer;

/**
 * @author Valentin Walendy (393413)
 * @author Ruben Lellouche (400288)
 *
 * Représente une vue structurée sur un ByteBuffer selon le format défini
 * par une instance de Structure. Fournit des accès typés aux champs non signés
 * et signés.
 */
public class StructuredBuffer {

    private final Structure structure;
    private final ByteBuffer buffer;

    /**
     * Construit un StructuredBuffer avec la Structure et le ByteBuffer fournis.
     * Vérifie que la capacité du buffer est un multiple de la taille totale
     * de la structure.
     *
     * @param structure     la Structure décrivant la disposition des enregistrements
     * @param buffer        le ByteBuffer contenant les données brutes
     * @throws IllegalArgumentException si buffer.capacity() n'est pas un multiple de structure.totalSize()
     */
    public StructuredBuffer(Structure structure, ByteBuffer buffer) {
        Preconditions.checkArgument(buffer.capacity() % structure.totalSize() == 0);
        this.structure = structure;
        this.buffer = buffer;
    }

    /**
     * Renvoie le nombre d'enregistrements stockés dans le buffer.
     *
     * @return le nombre d'éléments
     */
    public int size() {
        return buffer.capacity() / structure.totalSize();
    }

    /**
     * Lit et renvoie une valeur non signée sur 8 bits (U8)
     * à partir du champ et de l'enregistrement spécifiés.
     *
     * @param fieldIndex    l'indice du champ dans la structure (0-based)
     * @param elementIndex  l'indice de l'enregistrement dans le buffer (0-based)
     * @return la valeur U8, convertie en int non signé
     */
    public int getU8(int fieldIndex, int elementIndex) {
        int offset = structure.offset(fieldIndex, elementIndex);
        return Byte.toUnsignedInt(buffer.get(offset));
    }

    /**
     * Lit et renvoie une valeur non signée sur 16 bits (U16)
     * à partir du champ et de l'enregistrement spécifiés.
     *
     * @param fieldIndex    l'indice du champ dans la structure (0-based)
     * @param elementIndex  l'indice de l'enregistrement dans le buffer (0-based)
     * @return la valeur U16, convertie en int non signé
     */
    public int getU16(int fieldIndex, int elementIndex) {
        int offset = structure.offset(fieldIndex, elementIndex);
        return Short.toUnsignedInt(buffer.getShort(offset));
    }

    /**
     * Lit et renvoie une valeur signée sur 32 bits (S32)
     * à partir du champ et de l'enregistrement spécifiés.
     *
     * @param fieldIndex    l'indice du champ dans la structure (0-based)
     * @param elementIndex  l'indice de l'enregistrement dans le buffer (0-based)
     * @return la valeur S32 en int signé
     */
    public int getS32(int fieldIndex, int elementIndex) {
        int offset = structure.offset(fieldIndex, elementIndex);
        return buffer.getInt(offset);
    }
}
