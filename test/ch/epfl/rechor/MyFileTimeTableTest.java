package ch.epfl.rechor;

import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.mapped.FileTimeTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MyFileTimeTableTest {

    @Test
    void testInReturnsValidTimeTable(@TempDir Path tempDir) throws IOException {
        // Écriture du fichier strings.txt minimal
        Files.write(tempDir.resolve("strings.txt"), "dummy\n".getBytes(StandardCharsets.ISO_8859_1));
        // Création de fichiers binaires vides
        Files.write(tempDir.resolve("stations.bin"), new byte[0]);
        Files.write(tempDir.resolve("station-aliases.bin"), new byte[0]);
        Files.write(tempDir.resolve("platforms.bin"), new byte[0]);
        Files.write(tempDir.resolve("routes.bin"), new byte[0]);
        Files.write(tempDir.resolve("transfers.bin"), new byte[0]);
        // Fichiers pour trips et connections pour une date donnée
        LocalDate date = LocalDate.of(2025, 3, 18);
        Files.write(tempDir.resolve("trips_" + date + ".bin"), new byte[0]);
        Files.write(tempDir.resolve("connections_" + date + ".bin"), new byte[0]);
        Files.write(tempDir.resolve("connections-succ_" + date + ".bin"), new byte[0]);

        TimeTable tt = FileTimeTable.in(tempDir);
        assertNotNull(tt);
    }

    @Test
    void testTripsForReturnsNonNull(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("strings.txt"), "dummy\n".getBytes(StandardCharsets.ISO_8859_1));
        Files.write(tempDir.resolve("stations.bin"), new byte[0]);
        Files.write(tempDir.resolve("station-aliases.bin"), new byte[0]);
        Files.write(tempDir.resolve("platforms.bin"), new byte[0]);
        Files.write(tempDir.resolve("routes.bin"), new byte[0]);
        Files.write(tempDir.resolve("transfers.bin"), new byte[0]);
        LocalDate date = LocalDate.of(2025, 3, 18);
        Files.write(tempDir.resolve("trips_" + date + ".bin"), new byte[0]);
        Files.write(tempDir.resolve("connections_" + date + ".bin"), new byte[0]);
        Files.write(tempDir.resolve("connections-succ_" + date + ".bin"), new byte[0]);

        TimeTable tt = FileTimeTable.in(tempDir);
        assertNotNull(tt.tripsFor(date));
    }

    @Test
    void testConnectionsForReturnsNonNull(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("strings.txt"), "dummy\n".getBytes(StandardCharsets.ISO_8859_1));
        Files.write(tempDir.resolve("stations.bin"), new byte[0]);
        Files.write(tempDir.resolve("station-aliases.bin"), new byte[0]);
        Files.write(tempDir.resolve("platforms.bin"), new byte[0]);
        Files.write(tempDir.resolve("routes.bin"), new byte[0]);
        Files.write(tempDir.resolve("transfers.bin"), new byte[0]);
        LocalDate date = LocalDate.of(2025, 3, 18);
        Files.write(tempDir.resolve("trips_" + date + ".bin"), new byte[0]);
        Files.write(tempDir.resolve("connections_" + date + ".bin"), new byte[0]);
        Files.write(tempDir.resolve("connections-succ_" + date + ".bin"), new byte[0]);

        TimeTable tt = FileTimeTable.in(tempDir);
        assertNotNull(tt.connectionsFor(date));
    }

    @Test
    void testInReturnsValidTimeTable2(@TempDir Path tempDir) throws IOException {
        // Créer des fichiers minimaux valides pour chaque donnée
        Files.write(tempDir.resolve("strings.txt"), List.of("Line1", "Line2"), StandardCharsets.ISO_8859_1);

        // Pour stations.bin, on crée un ByteBuffer vide ou avec quelques octets
        Files.write(tempDir.resolve("stations.bin"), new byte[]{});
        Files.write(tempDir.resolve("station-aliases.bin"), new byte[]{});
        Files.write(tempDir.resolve("platforms.bin"), new byte[]{});
        Files.write(tempDir.resolve("routes.bin"), new byte[]{});
        Files.write(tempDir.resolve("transfers.bin"), new byte[]{});

        // Pour trips et connections, créer des fichiers vides.
        LocalDate date = LocalDate.of(2025, 3, 18);
        Files.write(tempDir.resolve("trips_" + date + ".bin"), new byte[]{});
        Files.write(tempDir.resolve("connections_" + date + ".bin"), new byte[]{});
        Files.write(tempDir.resolve("connections-succ_" + date + ".bin"), new byte[]{});

        TimeTable tt = FileTimeTable.in(tempDir);
        assertNotNull(tt);
        assertNotNull(tt.tripsFor(date));
        assertNotNull(tt.connectionsFor(date));
    }

    @Test
    void testTripsForReturnsBufferedTrips(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("strings.txt"), List.of("Trip1", "Trip2"), StandardCharsets.ISO_8859_1);
        Files.write(tempDir.resolve("stations.bin"), new byte[]{});
        Files.write(tempDir.resolve("station-aliases.bin"), new byte[]{});
        Files.write(tempDir.resolve("platforms.bin"), new byte[]{});
        Files.write(tempDir.resolve("routes.bin"), new byte[]{});
        Files.write(tempDir.resolve("transfers.bin"), new byte[]{});
        LocalDate date = LocalDate.of(2025, 3, 18);
        // Créer un fichier trips minimal (non vide)
        byte[] tripsContent = new byte[8];
        Files.write(tempDir.resolve("trips_" + date + ".bin"), tripsContent);
        // Nécessaire pour connections
        Files.write(tempDir.resolve("connections_" + date + ".bin"), new byte[]{});
        Files.write(tempDir.resolve("connections-succ_" + date + ".bin"), new byte[]{});

        TimeTable tt = FileTimeTable.in(tempDir);
        assertNotNull(tt.tripsFor(date));
    }

    @Test
    void testConnectionsForReturnsBufferedConnections(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("strings.txt"), List.of("Conn1", "Conn2"), StandardCharsets.ISO_8859_1);
        Files.write(tempDir.resolve("stations.bin"), new byte[]{});
        Files.write(tempDir.resolve("station-aliases.bin"), new byte[]{});
        Files.write(tempDir.resolve("platforms.bin"), new byte[]{});
        Files.write(tempDir.resolve("routes.bin"), new byte[]{});
        Files.write(tempDir.resolve("transfers.bin"), new byte[]{});
        LocalDate date = LocalDate.of(2025, 3, 18);

        // Créer des fichiers connections avec au moins 12 octets (1 enregistrement complet)
        byte[] connContent = new byte[12];
        Files.write(tempDir.resolve("connections_" + date + ".bin"), connContent);

        // Créer un fichier connections-succ avec au moins 4 octets (pour 1 int)
        byte[] succContent = new byte[4];
        Files.write(tempDir.resolve("connections-succ_" + date + ".bin"), succContent);

        TimeTable tt = FileTimeTable.in(tempDir);
        assertNotNull(tt.connectionsFor(date));
    }

    @Test
    void testInThrowsUncheckedIOExceptionWhenFileMissing(@TempDir Path tempDir) {
        // Ne créer qu'un fichier strings.txt et rien d'autre.
        try {
            Files.write(tempDir.resolve("strings.txt"), List.of("Dummy"), StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            fail(e);
        }
        UncheckedIOException thrown = assertThrows(UncheckedIOException.class,
                () -> FileTimeTable.in(tempDir));
        assertNotNull(thrown);
    }

    @Test
    void testDirectoryAttributeSetCorrectly(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("strings.txt"), List.of("Dummy"), StandardCharsets.ISO_8859_1);
        Files.write(tempDir.resolve("stations.bin"), new byte[]{});
        Files.write(tempDir.resolve("station-aliases.bin"), new byte[]{});
        Files.write(tempDir.resolve("platforms.bin"), new byte[]{});
        Files.write(tempDir.resolve("routes.bin"), new byte[]{});
        Files.write(tempDir.resolve("transfers.bin"), new byte[]{});
        LocalDate date = LocalDate.of(2025, 3, 18);
        Files.write(tempDir.resolve("trips_" + date + ".bin"), new byte[]{});
        Files.write(tempDir.resolve("connections_" + date + ".bin"), new byte[]{});
        Files.write(tempDir.resolve("connections-succ_" + date + ".bin"), new byte[]{});
        TimeTable tt = FileTimeTable.in(tempDir);
        // On vérifie que le champ directory de FileTimeTable correspond à tempDir.
        assertTrue(tt.toString().contains(tempDir.toString()));
    }
}
