package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.PackedRange;
import ch.epfl.rechor.timetable.Transfers;

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

/**
 * @author Valentin Walendy (393413)
 * @author Ruben Lellouche (400288)
 *
 * Représente une collection de transferts entre stations sous forme de buffer structuré.
 * Cette implémentation offre un accès rapide aux informations de transfert via un index pré-calculé.
 */
public final class BufferedTransfers implements Transfers {

    private static final int DEP_STATION_ID   = 0;
    private static final int ARR_STATION_ID   = 1;
    private static final int TRANSFER_MINUTES = 2;

    private static final Structure STRUCTURE = new Structure(
            Structure.field(DEP_STATION_ID, Structure.FieldType.U16),
            Structure.field(ARR_STATION_ID, Structure.FieldType.U16),
            Structure.field(TRANSFER_MINUTES, Structure.FieldType.U8)
    );

    private final StructuredBuffer buffer;
    private final int[] arrivingAtTable;

    /**
     * Construit un BufferedTransfers à partir d'un ByteBuffer brut.
     * Pré-calcule la table arrivingAtTable pour une lecture rapide.
     *
     * @param buffer Le ByteBuffer contenant les données de tous les transferts.
     */
    public BufferedTransfers(ByteBuffer buffer) {
        this.buffer = new StructuredBuffer(STRUCTURE, buffer);

        int n = this.buffer.size();

        if (n == 0) {
            this.arrivingAtTable = new int[0];
            return;
        }

        int maxArrId = -1;
        for (int i = 0; i < n; i++) {
            int arrId = this.buffer.getU16(ARR_STATION_ID, i);
            if (arrId > maxArrId) {
                maxArrId = arrId;
            }
        }

        this.arrivingAtTable = new int[maxArrId + 1];

        int i = 0;
        while (i < n) {
            int currentArr = this.buffer.getU16(ARR_STATION_ID, i);
            int start      = i;

            while (i < n
                    && this.buffer.getU16(ARR_STATION_ID, i) == currentArr) {
                i++;
            }

            int end = i;
            int packedInterval = PackedRange.pack(start, end);
            arrivingAtTable[currentArr] = packedInterval;
        }
    }

    /** Retourne la station de départ pour le transfert donné. */
    @Override
    public int depStationId(int id) {
        return buffer.getU16(DEP_STATION_ID, id);
    }

    /** Retourne la durée en minutes pour le transfert donné. */
    @Override
    public int minutes(int id) {
        return buffer.getU8(TRANSFER_MINUTES, id);
    }

    /** Retourne l'intervalle d'indices des transferts arrivant à la station spécifiée. */
    @Override
    public int arrivingAt(int stationId) {
        return arrivingAtTable[stationId];
    }

    /**
     * Retourne la durée du transfert entre deux stations. Lance une exception si aucun
     * transfert n'existe.
     */
    @Override
    public int minutesBetween(int depStationId, int arrStationId) {
        int range = arrivingAt(arrStationId);

        for (int idx = PackedRange.startInclusive(range);
             idx < PackedRange.endExclusive(range);
             idx++) {
            if (depStationId(idx) == depStationId) {
                return minutes(idx);
            }
        }

        throw new NoSuchElementException();
    }

    /** Retourne le nombre total de transferts stockés. */
    @Override
    public int size() {
        return buffer.size();
    }
}
