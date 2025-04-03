package ch.epfl.rechor.journey;

import static java.lang.Integer.toUnsignedLong;

/**
 * Classe utilitaire pour encoder et manipuler un critère de trajet sous forme d'un `long`.
 */
public final class PackedCriteria {

    /**
     * * @author Valentin Walendy (393413)
     *  * @author Ruben Lellouche (400288)
     * Constructeur privé pour empêcher l'instanciation.
     */
    private PackedCriteria() {
    }

    /**
     * Encode un critère de trajet.
     *
     * @param arrMins Heure d'arrivée (entre -240 et 2879).
     * @param changes Nombre de changements (entre 0 et 127).
     * @param payload Champ additionnel.
     * @return Un `long` contenant les données encodées.
     * @throws IllegalArgumentException Si une valeur est hors limites.
     */

    public static long pack(int arrMins, int changes, int payload) {
        if (arrMins < -240 || arrMins >= 2880) throw new IllegalArgumentException();
        if (changes < 0 || changes > 127) throw new IllegalArgumentException();
        //return ((long) arrMins << 39) | ((long) changes << 32) | toUnsignedLong(payload);
        return (((long) (arrMins + 240)) << 39) | (((long) changes) << 32) | toUnsignedLong(payload);
    }


    /**
     * Vérifie si le critère contient une heure de départ.
     */
    public static boolean hasDepMins(long criteria) {
        return (criteria >> 63) != 0;
    }

    /**
     * Extrait l'heure de départ.
     *
     * @throws IllegalArgumentException Si absente.
     */
    public static int depMins(long criteria) {
        if (!hasDepMins(criteria)) throw new IllegalArgumentException();
        //return (int) ((criteria >> 51) & 0xFFF);
        return (((int) (criteria >> 51)) & 0xFFF) - 240;

    }

    /**
     * Extrait l'heure d'arrivée.
     */
    public static int arrMins(long criteria) {

        return (((int) (criteria >> 39)) & 0xFFF) - 240;
    }

    /**
     * Extrait le nombre de changements.
     */
    public static int changes(long criteria) {
        return (int) ((criteria >> 32) & 0x7F);
    }

    /**
     * Extrait le champ additionnel (payload).
     */
    public static int payload(long criteria) {
        return (int) (criteria & 0xFFFFFFFF);
    }

    /**
     * Vérifie si un critère domine ou est égal à un autre.
     *
     * @throws IllegalArgumentException Si un critère a une heure de départ et pas l'autre.
     */
    public static boolean dominatesOrIsEqual(long criteria1, long criteria2) {
        if (hasDepMins(criteria1) != hasDepMins(criteria2)) throw new IllegalArgumentException();
        if (hasDepMins(criteria1) && depMins(criteria1) < depMins(criteria2)) return false;
        return arrMins(criteria1) <= arrMins(criteria2) && changes(criteria1) <= changes(criteria2);
    }

    /**
     * Supprime l'heure de départ.
     */
    public static long withoutDepMins(long criteria) {
        return criteria & ~(0x1FFFL << 51);
    }

    /**
     * Ajoute/modifie l'heure de départ.
     *
     * @throws IllegalArgumentException Si hors limites.
     */
    public static long withDepMins(long criteria, int depMins) {
        if (depMins < -240 || depMins >= 2880) throw new IllegalArgumentException();
        //return withoutDepMins(criteria) | ((long) depMins << 51) | (1L << 63);
        return withoutDepMins(criteria) | (((long) (depMins + 240)) << 51) | (1L << 63);

    }

    /**
     * Ajoute un changement.
     *
     * @throws IllegalArgumentException Si dépasse 127.
     */
    public static long withAdditionalChange(long criteria) {
        int newChanges = changes(criteria) + 1;
        if (newChanges > 127) throw new IllegalArgumentException();
        return (criteria & ~(0x7FL << 32)) | ((long) newChanges << 32);
    }

    /**
     * Modifie le champ additionnel (payload).
     */
    public static long withPayload(long criteria, int payload) {
        return (criteria & ~0xFFFFFFFFL) | toUnsignedLong(payload);
    }


}
