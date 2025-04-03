package ch.epfl.rechor.journey;

import ch.epfl.rechor.Bits32_24_8;
import ch.epfl.rechor.timetable.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import static ch.epfl.rechor.FormatterFr.formatLeg;

/**
 * Classe utilitaire pour extraire et organiser les informations de voyage à partir du profil utilisateur.
 * Cette classe contient des méthodes permettant de transformer les données de profil en voyages (Journey) organisés.
 */
public final class JourneyExtractor {

    /**
     * Constructeur privé pour empêcher l'instanciation de cette classe utilitaire.
     */
    private JourneyExtractor() {
    }

    /**
     * Extrait la liste des voyages pour une station de départ donnée.
     * <p>
     * Cette méthode génère une liste de voyages à partir du profil d'un utilisateur et de l'identifiant
     * d'une station de départ. Elle trie ensuite les voyages par heure de départ et d'arrivée.
     *
     * @param profile      Le profil utilisateur contenant les informations de connexions et de trajets.
     * @param depStationId L'identifiant de la station de départ.
     * @return La liste triée de voyages (Journey).
     */
    public static List<Journey> journeys(Profile profile, int depStationId) {
        List<Journey> journeys = new ArrayList<>();
        ParetoFront front = profile.forStation(depStationId);
        front.forEach(packedCriteria -> {
            List<Journey.Leg> legs = extractLegs(profile, depStationId, packedCriteria);
            journeys.add(new Journey(legs));
        });

        journeys.sort(Comparator
                .comparing(Journey::depTime)
                .thenComparing(Journey::arrTime));

        return journeys;
    }

    /**
     * Extrait les "legs" (étapes de voyage) d'un profil utilisateur et d'un critère donné.
     * <p>
     * Cette méthode analyse un critère d'optimisation pour déterminer les différentes étapes (legs)
     * d'un voyage, en tenant compte des arrêts intermédiaires, des changements et des connexions de transport.
     *
     * @param profile        Le profil utilisateur contenant les informations nécessaires pour extraire les étapes.
     * @param stationId      L'identifiant de la station de départ.
     * @param packedCriteria Le critère d'optimisation pour la recherche du voyage.
     * @return Une liste de "legs" représentant les différentes étapes du voyage.
     */
    private static List<Journey.Leg> extractLegs(Profile profile, int stationId, long packedCriteria) {
        List<Journey.Leg> legs = new ArrayList<>();
        TimeTable timeTable = profile.timeTable();
        Connections connections = profile.connections();
        Trips trips = profile.trips();
        Routes routes = timeTable.routes();

        int changesRemain = PackedCriteria.changes(packedCriteria);
        int targetArrMinutes = PackedCriteria.arrMins(packedCriteria);

        int firstConnectionId = Bits32_24_8.unpack24(PackedCriteria.payload(packedCriteria));
        int skipStops = Bits32_24_8.unpack8(PackedCriteria.payload(packedCriteria));

        int firstConnectionDepStopId = connections.depStopId(firstConnectionId);
        if (stationId != timeTable.stationId(firstConnectionDepStopId)) {
            int transMin;
            if (!timeTable.isStationId(firstConnectionDepStopId)) {
                firstConnectionDepStopId = timeTable.platforms().stationId(firstConnectionDepStopId - profile.timeTable().stations().size());
            }

            transMin = timeTable.transfers().minutesBetween(stationId, firstConnectionDepStopId);

            Stop depStop = createStop(timeTable, stationId);
            Stop arrStop = createStop(timeTable, connections.depStopId(firstConnectionId));

            LocalDateTime depTime = timeToDateTime(PackedCriteria.depMins(packedCriteria), profile.date());
            LocalDateTime arrTime = depTime.plusMinutes(transMin);

            Journey.Leg.Foot footLeg = new Journey.Leg.Foot(depStop, depTime, arrStop, arrTime);
            legs.add(footLeg);
        }

        int currentConnectionId = firstConnectionId;
        LocalDateTime arrTime = timeToDateTime(connections.arrMins(currentConnectionId), profile.date());
        while (changesRemain >= 0) {
            int tripId = connections.tripId(currentConnectionId);
            int routeId = trips.routeId(tripId);

            int depStopId = connections.depStopId(currentConnectionId);
            int arrStopId = connections.depStopId(currentConnectionId);
            List<Journey.Leg.IntermediateStop> intermediateStops = new ArrayList<>();

            for (int i = 0; i < skipStops; i++) {
                int nextConnectionId = connections.nextConnectionId(currentConnectionId);
                arrStopId = connections.arrStopId(currentConnectionId);
                Stop intermediateStop = createStop(timeTable, arrStopId);
                LocalDateTime intermediateArrTime = timeToDateTime(
                        connections.arrMins(currentConnectionId), profile.date());
                LocalDateTime intermediateDepTime = timeToDateTime(
                        connections.depMins(nextConnectionId), profile.date());
                intermediateStops.add(new Journey.Leg.IntermediateStop(
                        intermediateStop, intermediateArrTime, intermediateDepTime));
                currentConnectionId = nextConnectionId;
            }


            arrStopId = connections.arrStopId(currentConnectionId);
            int arrStationId = timeTable.stationId(arrStopId);

            Stop depStop = createStop(timeTable, depStopId);
            Stop arrStop = createStop(timeTable, arrStopId);

            LocalDateTime depTime = timeToDateTime(connections.depMins(firstConnectionId), profile.date());
            arrTime = timeToDateTime(connections.arrMins(currentConnectionId), profile.date());
            Vehicle vehicle = routes.vehicle(routeId);
            String routeName = routes.name(routeId);
            String destination = trips.destination(tripId);

            Journey.Leg.Transport transportLeg = new Journey.Leg.Transport(
                    depStop, depTime, arrStop, arrTime, intermediateStops,
                    vehicle, routeName, destination);

            legs.add(transportLeg);

            ParetoFront arrStationFront = profile.forStation(arrStationId);

            long nextPackedCriteria;
            if (--changesRemain >= 0) {
                nextPackedCriteria = arrStationFront.get(targetArrMinutes, changesRemain);
                int nextConnectionId = Bits32_24_8.unpack24(PackedCriteria.payload(nextPackedCriteria));
                skipStops = Bits32_24_8.unpack8(PackedCriteria.payload(nextPackedCriteria));

                int nextConnectionDepStopId = connections.depStopId(nextConnectionId);
                int nextConnectionStationId = nextConnectionDepStopId;

                if (!timeTable.isStationId(nextConnectionStationId)) {
                    nextConnectionStationId = timeTable.platforms().stationId(nextConnectionStationId - profile.timeTable().stations().size());
                }
                int transferMinutes;
                transferMinutes = timeTable.transfers().minutesBetween(arrStationId, nextConnectionStationId);

                Stop transferDepStop = createStop(timeTable, arrStopId);
                Stop transferArrStop = createStop(timeTable, nextConnectionDepStopId);

                LocalDateTime transferDepTime = arrTime;
                LocalDateTime transferArrTime = transferDepTime.plusMinutes(transferMinutes);

                Journey.Leg.Foot footLeg = new Journey.Leg.Foot(transferDepStop, transferDepTime,
                        transferArrStop, transferArrTime);
                legs.add(footLeg);

                currentConnectionId = nextConnectionId;
            }
        }
        int arrivalId = connections.arrStopId(currentConnectionId);
        if (timeTable.isPlatformId(arrivalId))
            arrivalId = timeTable.platforms().stationId(timeTable.stationId(arrivalId));
        if (arrivalId != profile.arrStationId()) {
            int transferMinutes;

            System.out.println("Recherche transfert entre : " + profile.arrStationId() + " et " + arrivalId);

            try {
                transferMinutes = timeTable.transfers().minutesBetween(profile.arrStationId(), arrivalId);
            } catch (NoSuchElementException e) {
                System.err.println("Pas de transfert direct entre " + profile.arrStationId() + " et " + arrivalId);
                transferMinutes = -1; // ou une autre valeur par défaut
            }


            Stop transferDepStop = createStop(timeTable, connections.arrStopId(currentConnectionId));
            Stop transferArrStop = createStop(timeTable, profile.arrStationId());

            LocalDateTime transferDepTime = arrTime;
            LocalDateTime transferArrTime = transferDepTime.plusMinutes(transferMinutes);

            Journey.Leg.Foot footLeg = new Journey.Leg.Foot(transferDepStop, transferDepTime,
                    transferArrStop, transferArrTime);
            legs.add(footLeg);
        }
        for (Journey.Leg l : legs) {
            switch (l) {
                case Journey.Leg.Foot fl -> System.out.println(formatLeg(fl));
                case Journey.Leg.Transport tl -> {
                    System.out.println(formatLeg(tl));
                    for (Journey.Leg.IntermediateStop is : tl.intermediateStops()) {
                        System.out.println(is);
                    }
                }
            }
        }


        return legs;
    }

    /**
     * Crée un objet `Stop` à partir de l'identifiant d'un arrêt dans un `TimeTable`.
     * <p>
     * Cette méthode récupère les informations relatives à une station de transport (nom, plateforme,
     * coordonnées) à partir de l'ID d'un arrêt.
     *
     * @param timeTable Le tableau des horaires de transport.
     * @param stopId    L'identifiant de l'arrêt.
     * @return Un objet `Stop` contenant les informations de la station.
     */
    private static Stop createStop(TimeTable timeTable, int stopId) {
        int stationId = timeTable.stationId(stopId);
        Stations stations = timeTable.stations();

        return new Stop(
                stations.name(stationId),
                timeTable.platformName(stopId),
                stations.longitude(stationId),
                stations.latitude(stationId));
    }

    /**
     * Convertit les minutes depuis minuit en un objet `LocalDateTime` correspondant à une date donnée.
     * <p>
     * Cette méthode utilise une date de base et ajoute le nombre de minutes depuis minuit pour obtenir
     * un objet `LocalDateTime`.
     *
     * @param minutesSinceMidnight Le nombre de minutes écoulées depuis minuit.
     * @param date                 La date à laquelle se réfère l'heure.
     * @return Un objet `LocalDateTime` représentant l'heure correspondant aux minutes depuis minuit.
     */
    private static LocalDateTime timeToDateTime(int minutesSinceMidnight, LocalDate date) {
        return date.atStartOfDay().plusMinutes(minutesSinceMidnight);
    }
}
