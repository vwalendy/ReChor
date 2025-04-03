package ch.epfl.rechor;

import ch.epfl.rechor.journey.ParetoFront;
import ch.epfl.rechor.journey.Profile;
import ch.epfl.rechor.timetable.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MyProfileTest {
    private static class FakeStations implements Stations {
        private final int size;
        FakeStations(int size) { this.size = size; }
        @Override public String name(int id) { return "Station" + id; }
        @Override public double longitude(int id) { return 0; }
        @Override public double latitude(int id) { return 0; }
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
        @Override public Platforms platforms() { return null; }
        @Override public Stations stations() { return stations; }
        @Override public StationAliases stationAliases() { return null; }
        @Override public Routes routes() { return null; }
        @Override public Transfers transfers() { return null; }
        @Override public Trips tripsFor(LocalDate date) { return trips; }
        @Override public Connections connectionsFor(LocalDate date) { return new Connections() {
            @Override public int depStopId(int id) { return 0; }
            @Override public int depMins(int id) { return 0; }
            @Override public int arrStopId(int id) { return 0; }
            @Override public int arrMins(int id) { return 0; }
            @Override public int tripId(int id) { return 0; }
            @Override public int tripPos(int id) { return 0; }
            @Override public int nextConnectionId(int id) { return 0; }
            @Override public int size() { return 0; }
        }; }
    }

    @Test
    void testForStationValid() {
        FakeTimeTable ft = new FakeTimeTable(3, 2);
        Profile.Builder pb = new Profile.Builder(ft, LocalDate.of(2025, 3, 18), 1);
        ParetoFront.Builder station0Builder = new ParetoFront.Builder();
        station0Builder.add(100, 2, 50);
        pb.setForStation(0, station0Builder);
        ParetoFront.Builder station2Builder = new ParetoFront.Builder();
        station2Builder.add(150, 3, 75);
        pb.setForStation(2, station2Builder);
        Profile profile = pb.build();
        assertEquals(ParetoFront.EMPTY, profile.forStation(1));
        assertEquals(station0Builder.build().toString(), profile.forStation(0).toString());
        assertEquals(station2Builder.build().toString(), profile.forStation(2).toString());
    }

    @Test
    void testForStationInvalid() {
        FakeTimeTable ft = new FakeTimeTable(3, 2);
        Profile.Builder pb = new Profile.Builder(ft, LocalDate.of(2025, 3, 18), 1);
        assertThrows(IndexOutOfBoundsException.class, () -> pb.forStation(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> pb.forStation(3));
    }

    @Test
    void testConnectionsAndTrips() {
        FakeTimeTable ft = new FakeTimeTable(3, 2);
        Profile.Builder pb = new Profile.Builder(ft, LocalDate.of(2025, 3, 18), 1);
        Profile profile = pb.build();
        assertNotNull(profile.connections());
        assertNotNull(profile.trips());
    }

    @Test
    void testBuildImmutability() {
        FakeTimeTable ft = new FakeTimeTable(2, 1);
        Profile.Builder pb = new Profile.Builder(ft, LocalDate.of(2025, 3, 18), 1);
        ParetoFront.Builder builder0 = new ParetoFront.Builder();
        builder0.add(200, 4, 100);
        pb.setForStation(0, builder0);
        Profile profile = pb.build();
        builder0.add(210, 3, 110);
        String expected = new ParetoFront.Builder().add(200, 4, 100).build().toString();
        assertEquals(expected, profile.forStation(0).toString());
    }

    @Test
    void testProfileBuildWithEmptyBuilders() {
        FakeTimeTable ft = new FakeTimeTable(4, 2);
        Profile.Builder pb = new Profile.Builder(ft, LocalDate.of(2025, 3, 18), 2);
        pb.setForStation(1, new ParetoFront.Builder().add(120, 2, 10));
        Profile profile = pb.build();
        List<ParetoFront> fronts = profile.stationFront();
        assertEquals(4, fronts.size());
        assertEquals(ParetoFront.EMPTY, fronts.get(0));
        assertEquals(ParetoFront.EMPTY, fronts.get(2));
        assertEquals(ParetoFront.EMPTY, fronts.get(3));
        assertNotEquals(ParetoFront.EMPTY, fronts.get(1));
    }


    @Test
    void testForStationValid2() {
        FakeTimeTable tt = new FakeTimeTable(3, 2);
        Profile.Builder pb = new Profile.Builder(tt, LocalDate.of(2025, 3, 18), 1);
        ParetoFront.Builder b0 = new ParetoFront.Builder();
        b0.add(100, 2, 10);
        pb.setForStation(0, b0);
        ParetoFront.Builder b2 = new ParetoFront.Builder();
        b2.add(150, 3, 20);
        pb.setForStation(2, b2);
        Profile profile = pb.build();
        assertEquals(ParetoFront.EMPTY, profile.forStation(1));
        assertEquals(b0.build().toString(), profile.forStation(0).toString());
        assertEquals(b2.build().toString(), profile.forStation(2).toString());
    }

    @Test
    void testForStationInvalid2() {
        FakeTimeTable tt = new FakeTimeTable(3, 2);
        Profile.Builder pb = new Profile.Builder(tt, LocalDate.of(2025, 3, 18), 1);
        assertThrows(IndexOutOfBoundsException.class, () -> pb.forStation(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> pb.forStation(3));
    }

    @Test
    void testForTripInvalidIndex() {
        FakeTimeTable tt = new FakeTimeTable(3, 4);
        Profile.Builder pb = new Profile.Builder(tt, LocalDate.of(2025, 3, 18), 1);
        assertThrows(IndexOutOfBoundsException.class, () -> pb.forTrip(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> pb.forTrip(tt.tripsFor(LocalDate.of(2025, 3, 18)).size()));
    }

    @Test
    void testForTripReturnsNullWhenNotSet() {
        FakeTimeTable tt = new FakeTimeTable(3, 4);
        Profile.Builder pb = new Profile.Builder(tt, LocalDate.of(2025, 3, 18), 1);
        assertNull(pb.forTrip(0));
    }

    @Test
    void testSetForTripInvalidIndex() {
        FakeTimeTable tt = new FakeTimeTable(3, 4);
        Profile.Builder pb = new Profile.Builder(tt, LocalDate.of(2025, 3, 18), 1);
        ParetoFront.Builder dummy = new ParetoFront.Builder();
        assertThrows(IndexOutOfBoundsException.class, () -> pb.setForTrip(-1, dummy));
        assertThrows(IndexOutOfBoundsException.class, () -> pb.setForTrip(5, dummy));
    }

    @Test
    void testBuildProfileAttributes() {
        FakeTimeTable tt = new FakeTimeTable(3, 2);
        LocalDate date = LocalDate.of(2025, 3, 18);
        int arrStationId = 1;
        Profile.Builder pb = new Profile.Builder(tt, date, arrStationId);
        ParetoFront.Builder b0 = new ParetoFront.Builder();
        b0.add(100, 2, 10);
        pb.setForStation(0, b0);
        pb.setForStation(2, new ParetoFront.Builder().add(120, 3, 20));
        Profile profile = pb.build();
        assertEquals(tt, profile.timeTable());
        assertEquals(date, profile.date());
        assertEquals(arrStationId, profile.arrStationId());
        List<ParetoFront> fronts = profile.stationFront();
        assertEquals(3, fronts.size());
        assertEquals(b0.build().toString(), fronts.get(0).toString());
        assertEquals(ParetoFront.EMPTY.toString(), fronts.get(1).toString());
        assertNotEquals(ParetoFront.EMPTY.toString(), fronts.get(2).toString());
    }

    @Test
    void testImmutabilityOfProfileFront() {
        FakeTimeTable tt = new FakeTimeTable(2, 1);
        Profile.Builder pb = new Profile.Builder(tt, LocalDate.of(2025, 3, 18), 0);
        ParetoFront.Builder b0 = new ParetoFront.Builder();
        b0.add(200, 4, 100);
        pb.setForStation(0, b0);
        Profile profile = pb.build();
        b0.add(210, 3, 110);
        assertEquals(new ParetoFront.Builder().add(200, 4, 100).build().toString(),
                profile.forStation(0).toString());
    }

    @Test
    void testProfileBuildWithEmptyBuilders2() {
        FakeTimeTable tt = new FakeTimeTable(4, 2);
        Profile.Builder pb = new Profile.Builder(tt, LocalDate.of(2025, 3, 18), 2);
        pb.setForStation(1, new ParetoFront.Builder().add(120, 2, 10));
        Profile profile = pb.build();
        List<ParetoFront> fronts = profile.stationFront();
        assertEquals(4, fronts.size());
        assertEquals(ParetoFront.EMPTY, fronts.get(0));
        assertEquals(ParetoFront.EMPTY, fronts.get(2));
        assertEquals(ParetoFront.EMPTY, fronts.get(3));
        assertNotEquals(ParetoFront.EMPTY, fronts.get(1));
    }
}
