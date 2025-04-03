package ch.epfl.rechor.journey;

import ch.epfl.rechor.timetable.Connections;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.Trips;

import java.time.LocalDate;
import java.util.List;


public record Profile(
        TimeTable timeTable,
        LocalDate date,
        int arrStationId,
        List<ParetoFront> stationFront){

    public Profile{
        stationFront = List.copyOf(stationFront);
    }

    public Connections connections(){
        return timeTable.connectionsFor(date);
    }

    public Trips trips(){
        return timeTable.tripsFor(date);
    }


    public ParetoFront forStation(int stationId){
        if (stationId < 0 || stationId >= stationFront().size()){
            throw new IndexOutOfBoundsException();
        }
        return stationFront.get(stationId);
    }


    public static class Builder {

        private final TimeTable timeTable;
        private final LocalDate date;
        private final int arrStationId;

        private final ParetoFront.Builder[] stationFrontBuilders;
        private final ParetoFront.Builder[] tripFrontBuilders;

        public Builder(TimeTable timeTable, LocalDate date, int arrStationId) {
            this.timeTable = timeTable;
            this.date = date;
            this.arrStationId = arrStationId;

            this.stationFrontBuilders = new ParetoFront.Builder[timeTable.stations().size()];
            this.tripFrontBuilders = new ParetoFront.Builder[timeTable.tripsFor(date).size()];

        }

        public ParetoFront.Builder forStation(int stationId) {
            if (stationId < 0 || stationId >= stationFrontBuilders.length) {
                throw new IndexOutOfBoundsException();
            }
            return stationFrontBuilders[stationId];
        }

        public void setForStation(int stationId, ParetoFront.Builder builder) {
            if (stationId < 0 || stationId >= stationFrontBuilders.length) {
                throw new IndexOutOfBoundsException();
            }
            stationFrontBuilders[stationId] = builder;
        }

        public ParetoFront.Builder forTrip(int tripId){
            if (tripId < 0 || tripId >= stationFrontBuilders.length) {
                throw new IndexOutOfBoundsException();
            }
            return stationFrontBuilders[tripId];
        }

        public void setForTrip(int tripId, ParetoFront.Builder builder){
            if (tripId < 0 || tripId >= stationFrontBuilders.length) {
                throw new IndexOutOfBoundsException();
            }
            stationFrontBuilders[tripId] = builder;
        }

        public Profile build(){
            ParetoFront[] stationFronts = new ParetoFront[stationFrontBuilders.length];
            for (int i = 0; i < stationFrontBuilders.length; i++) {
                if (stationFrontBuilders[i] == null) {
                    stationFronts[i] = ParetoFront.EMPTY;
                } else {
                    stationFronts[i] = stationFrontBuilders[i].build();}
            }
            return new Profile(timeTable, date, arrStationId, List.of(stationFronts));
        }

    }


}
