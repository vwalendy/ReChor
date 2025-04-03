package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.List;

import static ch.epfl.rechor.timetable.mapped.BufferedStationAliases.STRUCTURE;

public record FileTimeTable(
    Path directory,
    List<String> stringTable,
    Stations stations,
    StationAliases stationAliases,
    Platforms platforms,
    Routes routes,
    Transfers transfers
        ) implements TimeTable {


    public static TimeTable in(Path directory) throws IOException{
        try {
            Path stringsPath = directory.resolve("strings.txt");
            List<String> strings = List.copyOf(Files.readAllLines(stringsPath, StandardCharsets.ISO_8859_1));

            ByteBuffer stationsBuffer = mapFile(directory.resolve("stations.bin"));
            Stations stations = new BufferedStations(strings, stationsBuffer);

            ByteBuffer aliasesBuffer = mapFile(directory.resolve("station-aliases.bin"));
            StationAliases stationAliases = new BufferedStationAliases(strings, aliasesBuffer);

            ByteBuffer platformsBuffer = mapFile(directory.resolve("platforms.bin"));
            Platforms platforms = new BufferedPlatforms(strings, platformsBuffer);

            ByteBuffer routesBuffer = mapFile(directory.resolve("routes.bin"));
            Routes routes = new BufferedRoutes(strings, routesBuffer);

            ByteBuffer transfersBuffer = mapFile(directory.resolve("transfers.bin"));
            Transfers transfers = new BufferedTransfers(transfersBuffer);

            return new FileTimeTable(directory, strings, stations, stationAliases, platforms, routes, transfers);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ByteBuffer mapFile(Path filePath) throws IOException {
        try (FileChannel fileChannel = FileChannel.open(filePath, StandardOpenOption.READ)) {
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
        }
    }


    @Override
    public Trips tripsFor(LocalDate date) {
        try {
            Path tripsPath = directory.resolve("trips_" + date + ".bin");
            ByteBuffer tripsBuffer = mapFile(tripsPath);
            return new BufferedTrips(stringTable, tripsBuffer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }


    }

    @Override
    public Connections connectionsFor(LocalDate date) {
        try {
            Path connectionsPath = directory.resolve("connections_" + date + ".bin");
            Path connectionsSuccPath = directory.resolve("connections-succ_" + date + ".bin");
            ByteBuffer connectionsBuffer = mapFile(connectionsPath);
            ByteBuffer connectionsSuccBuffer = mapFile(connectionsSuccPath);
            return new BufferedConnections(connectionsBuffer, connectionsSuccBuffer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
