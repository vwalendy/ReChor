package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.Bits32_24_8;
import ch.epfl.rechor.timetable.Connections;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * @author Valentin Walendy (393413)
 * @author Ruben Lellouche (400288)
 *
 * Implémentation de l'interface Connections utilisant des buffers
 * pour stocker les données de connexions et leurs successeurs.
 */
public final class BufferedConnections implements Connections {

    private static final int DEP_STOP_ID   = 0;
    private static final int DEP_MINUTES   = 1;
    private static final int ARR_STOP_ID   = 2;
    private static final int ARR_MINUTES   = 3;
    private static final int TRIP_POS_ID   = 4;

    private static final Structure STRUCTURE_BUFFER = new Structure(
            Structure.field(DEP_STOP_ID, Structure.FieldType.U16),
            Structure.field(DEP_MINUTES, Structure.FieldType.U16),
            Structure.field(ARR_STOP_ID, Structure.FieldType.U16),
            Structure.field(ARR_MINUTES, Structure.FieldType.U16),
            Structure.field(TRIP_POS_ID, Structure.FieldType.S32)
    );

    private final StructuredBuffer buffer;
    private final IntBuffer      succBuffer;

    /**
     * Construit un BufferedConnections à partir des buffers donnés.
     *
     * @param bufData ByteBuffer des données de connexions
     * @param bufSucc ByteBuffer des indices de connexion suivante
     */
    public BufferedConnections(ByteBuffer bufData,ByteBuffer bufSucc) {
        this.buffer = new StructuredBuffer(STRUCTURE_BUFFER, bufData);
        this.succBuffer = bufSucc.asIntBuffer();
    }

    /**
     * Retourne l'identifiant de l'arrêt de départ pour la connexion spécifiée.
     *
     * @param id identifiant de la connexion
     * @return int identifiant de l'arrêt de départ
     */
    @Override
    public int depStopId(int id) {
        return buffer.getU16(DEP_STOP_ID, id);
    }

    /**
     * Retourne l'heure de départ en minutes après minuit.
     *
     * @param id identifiant de la connexion
     * @return int nombre de minutes depuis minuit
     */
    @Override
    public int depMins(int id) {
        return buffer.getU16(DEP_MINUTES, id);
    }

    /**
     * Retourne l'identifiant de l'arrêt d'arrivée pour la connexion spécifiée.
     *
     * @param id identifiant de la connexion
     * @return int identifiant de l'arrêt d'arrivée
     */
    @Override
    public int arrStopId(int id) {
        return buffer.getU16(ARR_STOP_ID, id);
    }

    /**
     * Retourne l'heure d'arrivée en minutes après minuit.
     *
     * @param id identifiant de la connexion
     * @return int nombre de minutes depuis minuit
     */
    @Override
    public int arrMins(int id) {
        return buffer.getU16(ARR_MINUTES, id);
    }

    /**
     * Retourne l'identifiant du trajet associé à la connexion spécifiée.
     *
     * @param id identifiant de la connexion
     * @return int identifiant du trajet (24 bits)
     */
    @Override
    public int tripId(int id) {
        int packed = buffer.getS32(TRIP_POS_ID, id);
        return Bits32_24_8.unpack24(packed);
    }

    /**
     * Retourne la position de la connexion dans son trajet.
     *
     * @param id identifiant de la connexion
     * @return int position dans le trajet (8 bits)
     */
    @Override
    public int tripPos(int id) {
        int packed = buffer.getS32(TRIP_POS_ID, id);
        return Bits32_24_8.unpack8(packed);
    }

    /**
     * Retourne l'identifiant de la connexion suivante.
     *
     * @param id identifiant de la connexion
     * @return int identifiant de la connexion suivante, ou -1 s'il n'existe pas
     */
    @Override
    public int nextConnectionId(int id) {
        return succBuffer.get(id);
    }

    /**
     * Retourne le nombre total de connexions stockées.
     *
     * @return int nombre de connexions
     */
    @Override
    public int size() {
        return buffer.size();
    }
}
