package ch.epfl.rechor.journey;

import ch.epfl.rechor.PackedRange;
import ch.epfl.rechor.timetable.Connections;
import ch.epfl.rechor.timetable.Routes;
import ch.epfl.rechor.timetable.Stations;
import ch.epfl.rechor.timetable.TimeTable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Valentin Walendy (393413)
 * @author Ruben Lellouche (400288)
 *
 * Extrait les voyages optimaux pour un profil et une gare de départ.
 */
public final class JourneyExtractor {

    private JourneyExtractor() {
        throw new UnsupportedOperationException();
    }

    /**
     * Extrait et retourne la liste des voyages optimaux triés par heure de départ et d'arrivée.
     *
     * @param profile      Profile itinéraire
     * @param depStationId int identifiant de la gare de départ
     * @return List<Journey> liste des voyages optimaux
     */
    public static List<Journey> journeys(Profile profile, int depStationId) {
        List<Journey> journeys = new ArrayList<>();

        ParetoFront front = profile.forStation(depStationId);
        front.forEach(packedCriteria -> {
            List<Journey.Leg> legs = extractLegs(profile, depStationId, packedCriteria);
            journeys.add(new Journey(legs));
        });

        journeys.sort(
                Comparator.comparing(Journey::depTime)
                        .thenComparing(Journey::arrTime)
        );

        return journeys;
    }


    /**
     * Extrait les étapes d'un voyage à partir d'un critère initial packagé.
     *
     * @param profile         Profile itinéraire
     * @param depStationId    int identifiant de la gare de départ
     * @param initialCriteria long critère initial packagé
     * @return List<Journey.Leg> liste des étapes du voyage
     */
    private static List<Journey.Leg> extractLegs(
            Profile profile,
            int depStationId,
            long initialCriteria
    ) {
        List<Journey.Leg> legs = new ArrayList<>();
        TimeTable timeTable   = profile.timeTable();
        Connections connections = profile.connections();

        int currentStopId  = depStationId;
        int finalArrMins   = PackedCriteria.arrMins(initialCriteria);
        int currentArrMins = 0;

        int payload        = PackedCriteria.payload(initialCriteria);
        int connId         = payload >> 8;
        int depStopId0     = connections.depStopId(connId);

        if (timeTable.stationId(depStopId0) != currentStopId) {
            addFootLeg(
                    connections.depMins(connId),
                    false,
                    currentStopId,
                    depStopId0,
                    timeTable,
                    profile,
                    legs
            );
        }

        for (int rem = PackedCriteria.changes(initialCriteria);
             rem >= 0;
             rem--
        ) {
            ParetoFront pf = profile.forStation(timeTable.stationId(currentStopId));
            long crit      = pf.get(finalArrMins, rem);

            payload   = PackedCriteria.payload(crit);
            connId    = payload >> 8;
            int skip  = payload & 0xFF;
            depStopId0 = connections.depStopId(connId);

            if (!legs.isEmpty() && legs.get(legs.size() - 1) instanceof Journey.Leg.Transport) {
                addFootLeg(
                        currentArrMins,
                        true,
                        currentStopId,
                        depStopId0,
                        timeTable,
                        profile,
                        legs
                );
            }

            connId = addTransportLeg(profile, timeTable, connId, skip, legs);

            currentStopId  = connections.arrStopId(connId);
            currentArrMins = connections.arrMins(connId);
        }

        if (timeTable.stationId(currentStopId) != profile.arrStationId()) {
            addFootLeg(
                    currentArrMins,
                    true,
                    currentStopId,
                    profile.arrStationId(),
                    timeTable,
                    profile,
                    legs
            );
        }

        return legs;
    }


    /**
     * Ajoute une étape de transport avec arrêts intermédiaires.
     *
     * @param profile      Profile itinéraire
     * @param timeTable    TimeTable horaire
     * @param connectionId int ID de la connexion courante
     * @param numStops     int nombre d'arrêts intermédiaires
     * @param legs         List<Journey.Leg> liste des étapes
     * @return int nouvel ID de connexion
     */
    private static int addTransportLeg(
            Profile profile,
            TimeTable timeTable,
            int connectionId,
            int numStops,
            List<Journey.Leg> legs
    ) {
        Connections connections = profile.connections();
        Stations stations       = timeTable.stations();
        Routes routes           = timeTable.routes();

        int tripIndex   = connections.tripId(connectionId);
        int depStop0    = connections.depStopId(connectionId);
        int initDepMins = connections.depMins(connectionId);

        List<Journey.Leg.IntermediateStop> intermediates = new ArrayList<>();
        for (int i = 0; i < numStops; i++) {
            int arrM = connections.arrMins(connectionId);
            connectionId = connections.nextConnectionId(connectionId);

            intermediates.add(
                    new Journey.Leg.IntermediateStop(
                            buildStop(connections.depStopId(connectionId), timeTable, stations),
                            convertTime(arrM, profile.date()),
                            convertTime(connections.depMins(connectionId), profile.date())
                    )
            );
        }

        int finalStop = connections.arrStopId(connectionId);
        Journey.Leg.Transport tLeg = new Journey.Leg.Transport(
                buildStop(depStop0, timeTable, stations),
                convertTime(initDepMins, profile.date()),
                buildStop(finalStop, timeTable, stations),
                convertTime(connections.arrMins(connectionId), profile.date()),
                intermediates,
                routes.vehicle(profile.trips().routeId(tripIndex)),
                routes.name(profile.trips().routeId(tripIndex)),
                profile.trips().destination(tripIndex)
        );

        legs.add(tLeg);
        return connectionId;
    }


    /**
     * Construit un objet Stop à partir d'un identifiant d'arrêt.
     *
     * @param stopId    int ID de l'arrêt
     * @param timeTable TimeTable horaire
     * @param stations  Stations disponibles
     * @return Stop instance du Stop
     */
    private static Stop buildStop(
            int stopId,
            TimeTable timeTable,
            Stations stations
    ) {
        int stId = timeTable.stationId(stopId);
        return new Stop(
                stations.name(stId),
                timeTable.platformName(stopId),
                stations.longitude(stId),
                stations.latitude(stId)
        );
    }


    /**
     * Convertit un nombre de minutes en LocalDateTime sur une date donnée.
     *
     * @param mins int nombre de minutes
     * @param date LocalDate date de référence
     * @return LocalDateTime instant converti
     */
    private static LocalDateTime convertTime(int mins, LocalDate date) {
        return LocalDateTime.of(date, LocalTime.MIN).plusMinutes(mins);
    }


    /**
     * Ajoute une étape à pied (Foot) au voyage selon le transfert.
     *
     * @param mins         int temps de référence
     * @param isDepMins    boolean vrai si temps de départ
     * @param depStopId    int ID arrêt départ
     * @param arrStopId    int ID arrêt arrivée
     * @param timeTable    TimeTable horaire
     * @param profile      Profile itinéraire
     * @param legs         List<Journey.Leg> étapes du voyage
     */
    private static void addFootLeg(
            int mins,
            boolean isDepMins,
            int depStopId,
            int arrStopId,
            TimeTable timeTable,
            Profile profile,
            List<Journey.Leg> legs
    ) {
        int depSt = timeTable.stationId(depStopId);
        int arrSt = timeTable.stationId(arrStopId);
        int view  = timeTable.transfers().arrivingAt(arrSt);

        for (int i = PackedRange.startInclusive(view);
             i < PackedRange.endExclusive(view);
             i++) {

            if (timeTable.transfers().depStationId(i) == depSt) {
                int d, a;
                if (isDepMins) {
                    d = mins;
                    a = mins + timeTable.transfers().minutes(i);
                } else {
                    d = mins - timeTable.transfers().minutes(i);
                    a = mins;
                }

                legs.add(new Journey.Leg.Foot(
                        buildStop(depStopId, timeTable, timeTable.stations()),
                        convertTime(d, profile.date()),
                        buildStop(arrStopId, timeTable, timeTable.stations()),
                        convertTime(a, profile.date())
                ));

                break;
            }
        }
    }
}
