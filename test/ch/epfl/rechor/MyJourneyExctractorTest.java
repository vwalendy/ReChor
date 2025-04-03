package ch.epfl.rechor;

import ch.epfl.rechor.journey.*;
import ch.epfl.rechor.timetable.Stations;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.Trips;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MyJourneyExctractorTest {
    private static class FakeStations implements Stations {
        private final int size;
        FakeStations(int size) { this.size = size; }
        @Override public String name(int id) { return "Station" + id; }
        @Override public double longitude(int id) { return 0.0; }
        @Override public double latitude(int id) { return 0.0; }
        @Override public int size() { return size; }
    }

    private static class FakeTrips implements Trips {
        private final int size;
        FakeTrips(int size) { this.size = size; }
        @Override public int routeId(int id) { return id; }
        @Override public String destination(int id) { return "TripDest" + id; }
        @Override public int size() { return size; }
    }

    private static class FakeTimeTable implements TimeTable {
        private final Stations stations;
        private final Trips trips;
        FakeTimeTable(int stationSize, int tripSize) {
            this.stations = new FakeStations(stationSize);
            this.trips = new FakeTrips(tripSize);
        }
        @Override public Stations stations() { return stations; }
        @Override public Trips tripsFor(LocalDate date) { return trips; }
        @Override public ch.epfl.rechor.timetable.Platforms platforms() { return null; }
        @Override public ch.epfl.rechor.timetable.StationAliases stationAliases() { return null; }
        @Override public ch.epfl.rechor.timetable.Routes routes() { return null; }
        @Override public ch.epfl.rechor.timetable.Transfers transfers() { return null; }
        @Override public ch.epfl.rechor.timetable.Connections connectionsFor(LocalDate date) { return null; }
    }

    // Test 1 : Si la frontière pour la gare est vide, journeys() doit retourner une liste vide.
    @Test
    void testEmptyParetoFrontReturnsNoJourneys() {
        FakeTimeTable ft = new FakeTimeTable(3, 2);
        Profile.Builder pb = new Profile.Builder(ft, LocalDate.of(2025, 3, 18), 1);
        // Pour station 0, aucune frontière n'est définie → ParetoFront.EMPTY.
        Profile profile = pb.build();
        List<Journey> journeys = JourneyExtractor.journeys(profile, 0);
        assertTrue(journeys.isEmpty());
    }

    // Test 2 : Extraction d'un voyage sans heure de départ.
    @Test
    void testJourneyExtractionWithoutDeparture() {
        FakeTimeTable ft = new FakeTimeTable(3, 2);
        LocalDate date = LocalDate.of(2025, 3, 18);
        Profile.Builder pb = new Profile.Builder(ft, date, 1);
        ParetoFront.Builder stationBuilder = new ParetoFront.Builder();
        // Tuple sans heure de départ : arrMins=600, changes=5, payload=123.
        long tuple = PackedCriteria.pack(600, 5, 123);
        stationBuilder.add(tuple);
        pb.setForStation(0, stationBuilder);
        Profile profile = pb.build();
        List<Journey> journeys = JourneyExtractor.journeys(profile, 0);
        assertEquals(1, journeys.size());
        LocalDateTime expectedDep = LocalDateTime.of(date, LocalTime.MIDNIGHT);
        LocalDateTime expectedArr = expectedDep.plusMinutes(600);
        Journey journey = journeys.get(0);
        assertEquals(expectedDep, journey.depTime());
        assertEquals(expectedArr, journey.arrTime());
    }

    // Test 3 : Extraction d'un voyage avec heure de départ.
    @Test
    void testJourneyExtractionWithDeparture() {
        FakeTimeTable ft = new FakeTimeTable(3, 2);
        LocalDate date = LocalDate.of(2025, 3, 18);
        Profile.Builder pb = new Profile.Builder(ft, date, 1);
        ParetoFront.Builder stationBuilder = new ParetoFront.Builder();
        // Tuple avec heure de départ forcée à 100 minutes.
        long tuple = PackedCriteria.pack(700, 3, 456);
        long tupleWithDep = PackedCriteria.withDepMins(tuple, 100);
        stationBuilder.add(tupleWithDep);
        pb.setForStation(0, stationBuilder);
        Profile profile = pb.build();
        List<Journey> journeys = JourneyExtractor.journeys(profile, 0);
        assertEquals(1, journeys.size());
        LocalDateTime expectedDep = LocalDateTime.of(date, LocalTime.MIDNIGHT).plusMinutes(100);
        LocalDateTime expectedArr = LocalDateTime.of(date, LocalTime.MIDNIGHT).plusMinutes(700);
        Journey journey = journeys.get(0);
        assertEquals(expectedDep, journey.depTime());
        assertEquals(expectedArr, journey.arrTime());
    }

    // Test 4 : Vérifie que les voyages extraits sont triés par heure de départ puis par heure d'arrivée.
    @Test
    void testJourneysSortedByDepAndArrTime() {
        FakeTimeTable ft = new FakeTimeTable(3, 2);
        LocalDate date = LocalDate.of(2025, 3, 18);
        Profile.Builder pb = new Profile.Builder(ft, date, 1);
        ParetoFront.Builder stationBuilder = new ParetoFront.Builder();
        // Ajouter plusieurs tuples sans heure de départ.
        stationBuilder.add(PackedCriteria.pack(500, 4, 10)); // arrMins=500
        stationBuilder.add(PackedCriteria.pack(400, 5, 20)); // arrMins=400
        stationBuilder.add(PackedCriteria.pack(600, 3, 30)); // arrMins=600
        pb.setForStation(0, stationBuilder);
        Profile profile = pb.build();
        List<Journey> journeys = JourneyExtractor.journeys(profile, 0);
        assertEquals(3, journeys.size());
        // Tous ont depTime = midnight, donc triés par arrTime croissant.
        assertTrue(journeys.get(0).arrTime().isBefore(journeys.get(1).arrTime()));
        assertTrue(journeys.get(1).arrTime().isBefore(journeys.get(2).arrTime()));
    }


    @Test
    void testProfileBuilderDefaultFrontiers() {
        FakeTimeTable tt = new FakeTimeTable(5, 2);
        Profile.Builder pb = new Profile.Builder(tt, LocalDate.of(2025, 3, 18), 100);
        // Aucun builder n'est défini, donc forStation(i) devrait retourner null pour tout i.
        for (int i = 0; i < tt.stations().size(); i++) {
            assertNull(pb.forStation(i));
        }
    }

    @Test
    void testForStationOutOfRange() {
        FakeTimeTable tt = new FakeTimeTable(3, 2);
        Profile.Builder pb = new Profile.Builder(tt, LocalDate.of(2025, 3, 18), 50);
        assertThrows(IndexOutOfBoundsException.class, () -> pb.forStation(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> pb.forStation(3));
    }

    @Test
    void testSetAndGetForStation() {
        FakeTimeTable tt = new FakeTimeTable(4, 2);
        Profile.Builder pb = new Profile.Builder(tt, LocalDate.of(2025, 3, 18), 200);
        ParetoFront.Builder builder0 = new ParetoFront.Builder().add(100, 2, 10);
        ParetoFront.Builder builder2 = new ParetoFront.Builder().add(150, 3, 20);
        pb.setForStation(0, builder0);
        pb.setForStation(2, builder2);
        Profile profile = pb.build();
        List<ParetoFront> fronts = profile.stationFront();
        assertEquals(4, fronts.size());
        assertEquals(builder0.build().toString(), fronts.get(0).toString());
        assertEquals(ParetoFront.EMPTY.toString(), fronts.get(1).toString());
        assertEquals(builder2.build().toString(), fronts.get(2).toString());
        assertEquals(ParetoFront.EMPTY.toString(), fronts.get(3).toString());
    }

    @Test
    void testJourneyExtractionWithoutDeparture2() {
        FakeTimeTable tt = new FakeTimeTable(3, 2);
        LocalDate date = LocalDate.of(2025, 3, 18);
        Profile.Builder pb = new Profile.Builder(tt, date, 101);
        ParetoFront.Builder stationBuilder = new ParetoFront.Builder();
        // Tuple sans heure de départ: arrMins=600, changes=5, payload=123.
        long tuple = PackedCriteria.pack(600, 5, 123);
        stationBuilder.add(tuple);
        pb.setForStation(0, stationBuilder);
        Profile profile = pb.build();
        List<Journey> journeys = JourneyExtractor.journeys(profile, 0);
        assertEquals(1, journeys.size());
        LocalDateTime expectedDep = LocalDateTime.of(date, LocalTime.MIDNIGHT);
        LocalDateTime expectedArr = expectedDep.plusMinutes(600);
        Journey journey = journeys.get(0);
        assertEquals(expectedDep, journey.depTime());
        assertEquals(expectedArr, journey.arrTime());
    }

    @Test
    void testJourneyExtractionWithDeparture2() {
        FakeTimeTable tt = new FakeTimeTable(3, 2);
        LocalDate date = LocalDate.of(2025, 3, 18);
        Profile.Builder pb = new Profile.Builder(tt, date, 101);
        ParetoFront.Builder stationBuilder = new ParetoFront.Builder();
        // Tuple avec heure de départ forcée à 100 minutes.
        long tuple = PackedCriteria.pack(700, 3, 456);
        long tupleWithDep = PackedCriteria.withDepMins(tuple, 100);
        stationBuilder.add(tupleWithDep);
        pb.setForStation(0, stationBuilder);
        Profile profile = pb.build();
        List<Journey> journeys = JourneyExtractor.journeys(profile, 0);
        assertEquals(1, journeys.size());
        LocalDateTime expectedDep = LocalDateTime.of(date, LocalTime.MIDNIGHT).plusMinutes(100);
        LocalDateTime expectedArr = LocalDateTime.of(date, LocalTime.MIDNIGHT).plusMinutes(700);
        Journey journey = journeys.get(0);
        assertEquals(expectedDep, journey.depTime());
        assertEquals(expectedArr, journey.arrTime());
    }

    @Test
    void testJourneysSorting() {
        FakeTimeTable tt = new FakeTimeTable(3, 2);
        LocalDate date = LocalDate.of(2025, 3, 18);
        Profile.Builder pb = new Profile.Builder(tt, date, 101);
        ParetoFront.Builder stationBuilder = new ParetoFront.Builder();
        // Ajouter plusieurs tuples sans heure de départ.
        stationBuilder.add(PackedCriteria.pack(500, 4, 10));  // arrMins=500
        stationBuilder.add(PackedCriteria.pack(400, 5, 20));  // arrMins=400
        stationBuilder.add(PackedCriteria.pack(600, 3, 30));  // arrMins=600
        pb.setForStation(0, stationBuilder);
        Profile profile = pb.build();
        List<Journey> journeys = JourneyExtractor.journeys(profile, 0);
        assertEquals(3, journeys.size());
        // Comme tous les voyages n'ont pas d'heure de départ, ils partent à minuit et
        // sont triés par heure d'arrivée.
        assertTrue(journeys.get(0).arrTime().isBefore(journeys.get(1).arrTime()));
        assertTrue(journeys.get(1).arrTime().isBefore(journeys.get(2).arrTime()));
    }
}
