package ch.epfl.rechor.timetable.mapped;

import java.util.Objects;

/**
 * @author Valentin Walendy (393413)
 * @author Ruben Lellouche (400288)
 *
 * Représente la définition d'une structure de champs pour un buffer structuré.
 * Pré-calcul les offsets de chaque champ pour un accès rapide.
 */
public class Structure {

    /** Types de champs supportés. */
    public enum FieldType {
        U8, U16, S32;
    }

    /** Représente un champ avec son index et son type. */
    public record Field(int index, FieldType type) {
        public Field {
            Objects.requireNonNull(type);
        }
    }

    private final Field[] fields;
    private final int[] fieldOffsets;
    private final int totalSize;

    /**
     * Construit une Structure à partir d'une séquence de champs.
     * Vérifie l'index de chaque champ et pré-calcule les offsets ainsi que la taille totale.
     *
     * @param fields la liste des champs, dont l'index doit correspondre à leur position
     * @throws IllegalArgumentException si un index de champ est invalide
     */
    public Structure(Field... fields) {
        Objects.requireNonNull(fields);
        this.fields = new Field[fields.length];
        this.fieldOffsets = new int[fields.length];
        int offset = 0;
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            if (f.index() != i) {
                throw new IllegalArgumentException();
            }
            this.fields[i] = f;
            this.fieldOffsets[i] = offset;
            switch (f.type()) {
                case U8  -> offset += 1;
                case U16 -> offset += 2;
                case S32 -> offset += 4;
                default  -> throw new IllegalStateException();
            }
        }
        this.totalSize = offset;
    }

    /**
     * Renvoie la taille en octets d'un enregistrement.
     * @return la taille totale calculée
     */
    public int totalSize() {
        return totalSize;
    }

    /**
     * Renvoie l'offset (en octets) du début du champ pour un enregistrement donné.
     * @param fieldIndex   index du champ (0-based)
     * @param elementIndex index de l'enregistrement (>=0)
     * @return l'offset en octets
     * @throws IndexOutOfBoundsException si fieldIndex ou elementIndex est invalide
     */
    public int offset(int fieldIndex, int elementIndex) {
        if (fieldIndex < 0 || fieldIndex >= fieldOffsets.length) {
            throw new IndexOutOfBoundsException();
        }
        if (elementIndex < 0) {
            throw new IndexOutOfBoundsException();
        }
        return elementIndex * totalSize + fieldOffsets[fieldIndex];
    }

    /**
     * Crée un Field pour usage dans le constructeur.
     * @param index index du champ
     * @param type  type du champ
     * @return le Field correspondant
     */
    public static Field field(int index, FieldType type) {
        return new Field(index, type);
    }
}
