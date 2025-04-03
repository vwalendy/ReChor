package ch.epfl.rechor;

import ch.epfl.rechor.journey.PackedCriteria;
import ch.epfl.rechor.journey.ParetoFront;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

import static ch.epfl.rechor.journey.PackedCriteria.withDepMins;
import static org.junit.jupiter.api.Assertions.*;

public class MyParetoFrontTest {

    @Test
    void limiteDeClear(){
        ParetoFront.Builder builder = new ParetoFront.Builder();

        builder.add(700,2,3);
        builder.clear();
        assertEquals(builder=new ParetoFront.Builder(),builder);
    }
    @Test
    void limiteSize(){
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(700);
        assertEquals(false,builder.isEmpty());
    }

    @Test
    public void testEmptyParetoFront() {
        ParetoFront emptyFront = ParetoFront.EMPTY;
        assertEquals(0, emptyFront.size(), "Un ParetoFront vide doit avoir une taille de 0.");
        assertThrows(NoSuchElementException.class, () -> emptyFront.get(10, 2),
                "Chercher un élément inexistant doit lever une exception.");
    }

    @Test
    public void testBuilderIsInitiallyEmpty() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        assertTrue(builder.isEmpty(), "Un builder vide doit être considéré comme vide.");
    }

    @Test
    public void testBuilderAddAndBuild() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(600, 5, 123456);
        builder.add(700, 3, 654321);

        ParetoFront front = builder.build();
        assertEquals(2, front.size(), "Le ParetoFront doit contenir 2 éléments.");

        // Vérifier que les éléments sont bien récupérés
        long expected1 = PackedCriteria.pack(600, 5, 123456);
        long expected2 = PackedCriteria.pack(700, 3, 654321);

        assertEquals(expected1, front.get(600, 5), "L'élément (600, 5) doit être récupérable.");
        assertEquals(expected2, front.get(700, 3), "L'élément (700, 3) doit être récupérable.");

        // Vérifier qu'un élément inexistant lève une exception
        assertThrows(NoSuchElementException.class, () -> front.get(500, 2),
                "Chercher un élément inexistant doit lever une exception.");
    }

    @Test
    public void testBuilderDoesNotModifyParetoFrontAfterBuild() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(600, 5, 123456);
        ParetoFront front = builder.build();

        // Ajouter un autre élément dans le builder après la construction
        builder.add(700, 3, 654321);

        // Le ParetoFront ne doit pas être affecté
        assertEquals(1, front.size(), "Un ParetoFront construit doit être immuable.");
    }

    @Test
    public void testBuilderClear() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(600, 5, 123456);
        assertFalse(builder.isEmpty(), "Le builder ne doit pas être vide après ajout.");

        builder.clear();
        assertTrue(builder.isEmpty(), "Le builder doit être vide après clear().");
    }

    @Test
    public void testForEach() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(600, 5, 123456);
        builder.add(700, 3, 654321);

        ParetoFront front = builder.build();

        AtomicLong count = new AtomicLong(0);
        front.forEach(criterion -> count.incrementAndGet());

        assertEquals(2, count.get(), "forEach() doit itérer sur tous les éléments du ParetoFront.");
    }

    @Test
    public void testToString() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(600, 5, 123456);
        builder.add(700, 3, 654321);
        ParetoFront front = builder.build();

        String result = front.toString();
        assertTrue(result.contains("arr :"), "toString() doit contenir 'arr :' pour chaque élément.");
        assertTrue(result.contains("Changes :"), "toString() doit contenir 'Changes :' pour chaque élément.");
    }

    @Test
    public void test1FullyDominates() {
        ParetoFront.Builder builder1 = new ParetoFront.Builder();
        builder1.add(600, 5, 123456);
        builder1.add(700, 3, 654321);

        ParetoFront.Builder builder2 = new ParetoFront.Builder();
        builder2.add(800, 6, 222222);  // Moins bon, donc dominé

        assertTrue(builder1.fullyDominates(builder2, 600),
                "Le premier builder doit dominer complètement le second.");
    }

    @Test
    public void test1BuilderCopyConstructor() {
        ParetoFront.Builder original = new ParetoFront.Builder();
        original.add(600, 5, 123456);
        ParetoFront.Builder copy = new ParetoFront.Builder(original);

        assertEquals(original.toString(), copy.toString(),
                "Le constructeur de copie doit créer une copie identique.");
    }
    private ParetoFront.Builder builder;

    @BeforeEach
    void setUp() {
        builder = new ParetoFront.Builder();
    }

    @Test
    void testParetoFrontEmpty() {
        assertEquals(0, ParetoFront.EMPTY.size());
        assertThrows(NoSuchElementException.class, () -> ParetoFront.EMPTY.get(10, 1));
    }

    @Test
    void testBuilderInitiallyEmpty() {
        assertTrue(builder.isEmpty());
    }

    @Test
    void testBuilderAddSingleElement() {
        builder.add(600, 5, 123456);
        assertEquals(1, builder.build().size());
    }

    @Test
    void testBuilderAddDuplicateElements() {
        builder.add(600, 5, 123456);
        builder.add(600, 5, 123456);
        assertEquals(1, builder.build().size(), "Duplicate elements should not increase size");
    }

    @Test
    void testBuilderAddDominatedElement() {
        builder.add(600, 5, 123456);
        builder.add(700, 6, 654321); // Dominated
        assertEquals(1, builder.build().size(), "Dominated elements should not be added");
    }

    @Test
    void testBuilderRemovesDominatedElements() {
        builder.add(700, 3, 654321);
        builder.add(600, 2, 999999); // Should remove the first element
        assertEquals(1, builder.build().size(), "Weaker elements should be removed");
    }

    @Test
    void testBuilderHandlesLargeInput() {
        for (int i = 0; i < 1000; i++) {
            builder.add(i, i % 10, i * 1000);
        }
        assertTrue(builder.build().size() > 0, "Builder should handle large inputs correctly");
    }

    @Test
    void test1BuilderClear() {
        builder.add(600, 5, 123456);
        builder.clear();
        assertTrue(builder.isEmpty());
    }

    @Test
    void testBuilderCopyConstructor() {
        builder.add(600, 5, 123456);
        ParetoFront.Builder copy = new ParetoFront.Builder(builder);
        assertEquals(builder.build().size(), copy.build().size(), "Copy should have the same size");
    }

    @Test
    void testFullyDominates() {
        builder.add(600, 5, 123456);
        ParetoFront.Builder other = new ParetoFront.Builder();
        other.add(700, 6, 654321);
        assertTrue(builder.fullyDominates(other, 500), "All elements in 'other' should be dominated");
    }

    @Test
    void test1ForEach() {
        builder.add(600, 5, 123456);
        builder.add(700, 3, 654321);
        ParetoFront front = builder.build();
        AtomicInteger count = new AtomicInteger();
        front.forEach((LongConsumer) l -> count.incrementAndGet());
        assertEquals(2, count.get(), "forEach should iterate over all elements");
    }

    //=============================================

    // On utilisera ici des critères sans heure de départ via PackedCriteria.pack(arrMins, changes, payload)
    // Pour la simplicité, nous définissons quelques tuples exemples :
    // tuple1: (arrMins=480 (8h00), changes=3, payload=100)
    // tuple2: (arrMins=480, changes=4, payload=200) -> dominé par tuple1 (même arr, plus de changements)
    // tuple3: (arrMins=481, changes=2, payload=300) -> incomparable avec tuple1
    // tuple4: (arrMins=482, changes=1, payload=400) -> incomparable avec tuple1 et tuple3
    // tuple5: (arrMins=483, changes=1, payload=500) -> dominé par tuple4
    // tuple6: (arrMins=484, changes=0, payload=600) -> incomparable avec les autres

    private long tuple1, tuple2, tuple3, tuple4, tuple5, tuple6;

    @BeforeEach
    void setUp1() {
        tuple1 = PackedCriteria.pack(480, 3, 100);
        tuple2 = PackedCriteria.pack(480, 4, 200);
        tuple3 = PackedCriteria.pack(481, 2, 300);
        tuple4 = PackedCriteria.pack(482, 1, 400);
        tuple5 = PackedCriteria.pack(483, 1, 500);
        tuple6 = PackedCriteria.pack(484, 0, 600);
    }

    @Test
    void testEmptyParetoFront1() {
        ParetoFront pf = ParetoFront.EMPTY;
        assertEquals(0, pf.size());
    }

    @Test
    void testBuilderAddWithoutDeparture() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        // Ajout de tuple1
        builder.add(tuple1);
        assertEquals(1, builder.build().size());

        // Ajout de tuple2, qui est dominé par tuple1, ne doit pas être ajouté
        builder.add(tuple2);
        ParetoFront pf = builder.build();
        assertEquals(1, pf.size());
        ParetoFront finalPf = pf;
        assertThrows(NoSuchElementException.class, () -> finalPf.get(480, 4));

        // Ajout de tuple3 (incomparable avec tuple1) → frontière doit contenir tuple1 et tuple3
        builder.add(tuple3);
        pf = builder.build();
        assertEquals(2, pf.size());
        assertEquals(tuple1, pf.get(480, 3));
        assertEquals(tuple3, pf.get(481, 2));

        // Ajout de tuple4 (incomparable avec tuple1 et tuple3) → frontière devient tuple1, tuple3, tuple4
        builder.add(tuple4);
        pf = builder.build();
        assertEquals(3, pf.size());

        // Ajout de tuple5, dominé par tuple4, ne doit pas être ajouté
        builder.add(tuple5);
        pf = builder.build();
        assertEquals(3, pf.size());

        // Ajout de tuple6 (incomparable avec les autres) → frontière devient 4 éléments
        builder.add(tuple6);
        pf = builder.build();
        assertEquals(4, pf.size());
    }

    @Test
    void testBuilderOrdering() {
        // Ajoutons les tuples dans un ordre aléatoire et vérifions l'ordre lexicographique (ordre naturel des longs)
        ParetoFront.Builder builder = new ParetoFront.Builder();
        List<Long> input = new ArrayList<>();
        input.add(tuple3);
        input.add(tuple1);
        input.add(tuple6);
        input.add(tuple4);
        // tuple2 est dominé et tuple5 dominé par tuple4, ils ne seront pas ajoutés
        for (Long t : input) {
            builder.add(t);
        }
        ParetoFront pf = builder.build();
        List<Long> ordered = new ArrayList<>();
        pf.forEach(ordered::add);
        // Vérifions que les valeurs sont dans l'ordre non décroissant (ordre naturel sur long)
        for (int i = 1; i < ordered.size(); i++) {
            assertTrue(ordered.get(i - 1) <= ordered.get(i));
        }
    }

    @Test
    void testBuilderAddAllAndClear() {
        ParetoFront.Builder builder1 = new ParetoFront.Builder();
        builder1.add(tuple1);
        builder1.add(tuple3);

        ParetoFront.Builder builder2 = new ParetoFront.Builder();
        builder2.add(tuple4);
        builder2.add(tuple6);

        builder1.addAll(builder2);
        ParetoFront pf = builder1.build();
        // On attend 4 tuples au total
        assertEquals(4, pf.size());

        // Test de clear
        builder1.clear();
        assertTrue(builder1.isEmpty());
    }


    @Test
    void testImmutabilityAfterBuild() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(tuple1);
        ParetoFront pf1 = builder.build();
        int sizePf1 = pf1.size();
        // Ajout d'un nouveau tuple dans le builder après build()
        builder.add(tuple3);
        ParetoFront pf2 = builder.build();
        // pf1 ne doit pas être modifié
        assertEquals(sizePf1, pf1.size());
        // pf2 doit avoir plus d'éléments
        assertTrue(pf2.size() > sizePf1);
    }

    @Test
    void testGetMethodAndToString() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(tuple1);
        builder.add(tuple3);
        builder.add(tuple4);
        ParetoFront pf = builder.build();
        // Test de la méthode get()
        long found = pf.get(480, 3);
        assertEquals(tuple1, found);
        // Test que get lance une exception pour un tuple absent
        assertThrows(NoSuchElementException.class, () -> pf.get(490, 5));
        // Test de toString() : on s'assure que la chaîne contient au moins le mot "arr=" indiquant l'heure d'arrivée
        String str = pf.toString();
        assertNotNull(str);
        assertTrue(str.contains("arr :"));
    }

    @Test
    void testBuilderAddUsingTripleParameters() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        // Utilisation de l'overload add(arrMins, changes, payload)
        builder.add(480, 3, 100); // correspond à tuple1
        ParetoFront pf = builder.build();
        assertEquals(1, pf.size());
        long retrieved = pf.get(480, 3);
        assertEquals(tuple1, retrieved);
    }

    @Test
    void testBuilderForEach() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(tuple1);
        builder.add(tuple3);
        List<Long> list = new ArrayList<>();
        builder.build().forEach(list::add);
        // Vérifie que le nombre d'éléments parcourus est égal à la taille du front
        assertEquals(builder.build().size(), list.size());
    }

    //=============================================================================


    @Test
    void testAddToEmptyFront() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        long tuple = PackedCriteria.pack(480, 3, 0);
        builder.add(tuple);
        ParetoFront front = builder.build();
        assertEquals(1, front.size());
        assertEquals(tuple, front.get(480, 3));
    }

    @Test
    void testAddDominatedTuple() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        long tuple4 = PackedCriteria.pack(480, 3, 0);
        builder.add(tuple4);

        long tuple1 = PackedCriteria.pack(480, 4, 0); // Dominé par 480,3
        builder.add(tuple1);
        assertEquals(1, builder.build().size());

        long tuple5 = PackedCriteria.pack(481, 3, 0); // Non dominé
        builder.add(tuple5);
        assertEquals(1, builder.build().size());
    }

    @Test
    void testAddDominatingTuple() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        long tuple1 = PackedCriteria.pack(480, 4, 0);
        long tuple2 = PackedCriteria.pack(481, 2, 0);
        long tuple3 = PackedCriteria.pack(482, 1, 0);
        long tuple4 = PackedCriteria.pack(480, 3, 0); // Domine 480,4

        builder.add(tuple1);
        builder.add(tuple2);
        builder.add(tuple3);
        builder.add(tuple4);
        ParetoFront front = builder.build();

        assertEquals(3, front.size());
        List<Long> tuples = new ArrayList<>();
        front.forEach(tuples::add);
        assertArrayEquals(new long[] {
                PackedCriteria.pack(480, 3, 0),
                PackedCriteria.pack(481, 2, 0),
                PackedCriteria.pack(482, 1, 0)
        }, tuples.stream().mapToLong(Long::longValue).toArray());
    }

    @Test
    void testAddNonDominatedTuple() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        long tuple4 = PackedCriteria.pack(480, 3, 0);
        long tuple3 = PackedCriteria.pack(482, 1, 0);
        long tuple2 = PackedCriteria.pack(481, 2, 0);

        builder.add(tuple4);
        builder.add(tuple3);
        builder.add(tuple2);
        ParetoFront front = builder.build();

        assertEquals(3, front.size());
        List<Long> tuples = new ArrayList<>();
        front.forEach(tuples::add);
        assertArrayEquals(new long[] {
                PackedCriteria.pack(480, 3, 0),
                PackedCriteria.pack(481, 2, 0),
                PackedCriteria.pack(482, 1, 0)
        }, tuples.stream().mapToLong(Long::longValue).toArray());
    }

    @Test
    void testAddDuplicateTuple() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        long tuple4 = PackedCriteria.pack(480, 3, 0);
        builder.add(tuple4);
        builder.add(tuple4); // Duplicata exact
        assertEquals(1, builder.build().size());

        long tuple6 = PackedCriteria.pack(480, 3, 5); // Même arrMins et changes, payload différent
        builder.add(tuple6);
        assertEquals(1, builder.build().size());
    }

    @Test
    void testAddBoundaryValues() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(PackedCriteria.pack(-240, 0, 0));   // Min arrMins
        builder.add(PackedCriteria.pack(2879, 127, 0)); // Max arrMins et changes
        builder.add(PackedCriteria.pack(1440, 64, 0));  // Milieu
        ParetoFront front = builder.build();
        assertEquals(1, front.size());

    }

    @Test
    void testFullyDominates12() {
        ParetoFront.Builder builder1 = new ParetoFront.Builder();
        long tuple4 = PackedCriteria.pack(480, 3, 0);
        long tuple2 = PackedCriteria.pack(481, 2, 0);
        builder1.add(withDepMins(tuple4, 0));
        builder1.add(withDepMins(tuple2, 0));

        ParetoFront.Builder builder2 = new ParetoFront.Builder();
        long tuple1 = PackedCriteria.pack(480, 4, 0);
        long tuple5 = PackedCriteria.pack(481, 3, 0);
        builder2.add(tuple1);
        builder2.add(tuple5);

        assertTrue(builder1.fullyDominates(builder2, 0));
    }
    @Test
    void testWhenLengtsIsLessThan2() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(PackedCriteria.pack(480, 3, 0));
        assertEquals(1, builder.build().size());
    }
    @Test
    void testWithPayLoadInAction(){
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(PackedCriteria.pack(480, 3, 10000000));
        builder.add(PackedCriteria.pack(480, 3, 4));
        assertEquals(1, builder.build().size());
    }
    @Test
    void testGet(){
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(PackedCriteria.pack(480, 3, 0));
        ParetoFront front = builder.build();
        assertEquals(PackedCriteria.pack(480,3,0), front.get(480,3));
        builder.add(PackedCriteria.pack(484, 2, 1));
        front = builder.build();
        assertEquals(PackedCriteria.pack(484,2,1), front.get(484,2));
        ParetoFront finalFront = front;
        assertThrows(NoSuchElementException.class, () -> finalFront.get(999, 9));
    }
    @Test
    void testforEach(){
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(480, 3, 0);
        builder.add(484, 2, 1);
        ParetoFront front = builder.build();

        // Liste pour stocker les valeurs parcourues
        List<Long> collected = new ArrayList<>();
        front.forEach(collected::add);

        // Vérification que tous les éléments sont présents et dans le bon ordre
        assertEquals(List.of(
                PackedCriteria.pack(480, 3, 0),
                PackedCriteria.pack(484, 2, 1)
        ), collected);
    }
    @Test
    void testAddAll() {

        // Création du premier Builder avec deux tuples
        ParetoFront.Builder builder1 = new ParetoFront.Builder();
        builder1.add(480, 3, 0);
        builder1.add(484, 2, 1);

        // Création du second Builder avec deux autres tuples
        ParetoFront.Builder builder2 = new ParetoFront.Builder();
        builder2.add(479, 4, 2);  // Ajouté car non dominé
        builder2.add(482, 3, 0);  // Ajouté car non dominé

        // Ajout de builder2 dans builder1
        builder1.addAll(builder2);

        // Construction du ParetoFront final
        ParetoFront front = builder1.build();

        // Liste pour stocker les valeurs parcourues
        List<Long> collected = new ArrayList<>();
        front.forEach(collected::add);

        // Vérification que tous les éléments sont présents et dans le bon ordre
        assertEquals(List.of(
                PackedCriteria.pack(479, 4, 2),
                PackedCriteria.pack(480, 3, 0),
                PackedCriteria.pack(484, 2, 1)
        ), collected);
    }
    @Test
    void testAddMethod() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(480, 3, 93479823);
        builder.add(480, 4, 3094830);
        builder.add(484, 1, 923847987);
        builder.add(481, 2, 984309238);
        builder.add(482, 1, 9898347);
        builder.add(483, 0, 9898347);
        ParetoFront paretoFront = builder.build();
        long[] expected = new long[4];
        expected[0] = PackedCriteria.pack(480, 3, 93479823);
        expected[1] = PackedCriteria.pack(481, 2, 984309238);
        expected[2] = PackedCriteria.pack(482, 1, 9898347);
        expected[3] = PackedCriteria.pack(483, 0, 9898347);
        //assertArrayEquals(expected, paretoFront.bordersTuple);
    }

    @Test
    void testFullyDominatesMethod() {
        // Constructeur du récepteur (l'objet qui appelle fullyDominates)
        ParetoFront.Builder receiverBuilder = new ParetoFront.Builder();
        receiverBuilder.add(withDepMins(PackedCriteria.pack(480, 2, 1), 480)); // 8h00, 2 changements
        receiverBuilder.add(withDepMins(PackedCriteria.pack(485, 1, 2), 480)); // 8h05, 1 changement
        receiverBuilder.add(withDepMins(PackedCriteria.pack(490, 0, 3), 480)); // 8h10, 0 changement

        // Constructeur de l'objet "that"
        ParetoFront.Builder thatBuilder = new ParetoFront.Builder();
        thatBuilder.add(481, 3, 4); // 8h01, 3 changements
        thatBuilder.add(486, 2, 5); // 8h06, 2 changements
        thatBuilder.add(491, 1, 6); // 8h11, 1 changement

        // Test avec depMins = 480 (8h00)
        assertTrue(receiverBuilder.fullyDominates(thatBuilder, 480));

        // Ajout d'un tuple non dominé à "that"
        thatBuilder.add(479, 0, 7); // 7h59, 0 changement

        // Test avec depMins = 480 (8h00) après l'ajout
        assertFalse(receiverBuilder.fullyDominates(thatBuilder, 480));

        // Test avec un "that" vide
        ParetoFront.Builder emptyBuilder = new ParetoFront.Builder();
        assertTrue(receiverBuilder.fullyDominates(emptyBuilder, 480));

        // Test avec un récepteur vide
        ParetoFront.Builder emptyReceiverBuilder = new ParetoFront.Builder();
        assertFalse(emptyReceiverBuilder.fullyDominates(thatBuilder, 480));
    }
    @Test
    void testFullyDominatesWithEqualTuples() {
        ParetoFront.Builder receiverBuilder = new ParetoFront.Builder();
        receiverBuilder.add(withDepMins(PackedCriteria.pack(480, 2, 1), 480)); // 8h00, 2 changements
        receiverBuilder.add(withDepMins(PackedCriteria.pack(485, 1, 2), 480)); // 8h05, 1 changement
        receiverBuilder.add(withDepMins(PackedCriteria.pack(490, 0, 3), 480));

        ParetoFront.Builder thatBuilder = new ParetoFront.Builder();
        thatBuilder.add(PackedCriteria.pack(480, 2, 1)); // 8h00, 2 changements
        thatBuilder.add(PackedCriteria.pack(485, 1, 2)); // 8h05, 1 changement
        thatBuilder.add(PackedCriteria.pack(490, 0, 3));

        assertTrue(receiverBuilder.fullyDominates(thatBuilder, 480));
    }
    @Test
    void builderAddToEmptyFront() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        long tuple1 = PackedCriteria.pack(480, 3, 0);
        long tuple2 = PackedCriteria.pack(480, 4, 0);// 8h00, 2 changements
        long tuple3 = PackedCriteria.pack(484, 1, 0);// 8h00, 2 changements
        long tuple4 = PackedCriteria.pack(481, 2, 0);// 8h00, 2 changements
        long tuple5 = PackedCriteria.pack(482, 1, 0);// 8h00, 2 changements
        long tuple6 = PackedCriteria.pack(483, 0, 0);// 8h00, 2 changements

        System.out.println(builder);
        builder.add(tuple1);
        System.out.println(builder);
        builder.add(tuple2);
        System.out.println(builder);
        builder.add(tuple3);
        System.out.println(builder);
        builder.add(tuple4);
        System.out.println(builder);
        builder.add(tuple5);
        System.out.println(builder);
        builder.add(tuple6);
        System.out.println(builder);

        ParetoFront front = builder.build();
        assertEquals(4, front.size());
        assertEquals(tuple1, front.get(480, 3));
        assertEquals(tuple4, front.get(481, 2));
        assertEquals(tuple5, front.get(482, 1));
        assertEquals(tuple6, front.get(483, 0));

    }

    @Test
    void builderAddDominatedTupleDoesNothing() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        long tuple1 = PackedCriteria.pack(480, 5, 0);
        long tuple2 = PackedCriteria.pack(400, 20, 0);
        long tuple3 = PackedCriteria.pack(480, 10, 0);

        builder.add(tuple1);
        builder.add(tuple2);
        builder.add(tuple3);

        ParetoFront pareto = builder.build();
        assertEquals(2, pareto.size());
        assertTrue(pareto.get(480, 5) == tuple1);
    }

    @Test
    void builderAddDominatingTupleRemovesDominated() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        long tuple1 = PackedCriteria.pack(500, 3, 0); // Dominé
        long tuple2 = PackedCriteria.pack(480, 2, 0); // Domine

        builder.add(tuple1);
        builder.add(tuple2);

        ParetoFront front = builder.build();
        assertEquals(1, front.size());
        assertEquals(tuple2, front.get(480, 2));
    }

    @Test
    void builderAddNonDominatedTuplesRetainsBoth() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        long tuple1 = PackedCriteria.pack(480, 2, 0); // Arrivée tôt, plus de changements
        long tuple2 = PackedCriteria.pack(490, 1, 0); // Arrivée plus tard, moins de changements

        builder.add(tuple1);
        builder.add(tuple2);

        assertEquals(2, builder.build().size());
    }

    @Test
    void fullyDominatesReturnsTrueWhenAllDominated() {
        ParetoFront.Builder builder1 = new ParetoFront.Builder();

        long tuple1 = PackedCriteria.pack(480, 2, 0);
        long tuple2 = PackedCriteria.pack(490, 1, 0);
        tuple1 = withDepMins(tuple1, 100);
        tuple2 = withDepMins(tuple2, 100);

        builder1.add(tuple1); // Domine tout
        builder1.add(tuple2);

        System.out.println(builder1);

        ParetoFront.Builder builder2 = new ParetoFront.Builder();
        builder2.add(PackedCriteria.pack(500, 2, 0));
        builder2.add(PackedCriteria.pack(510, 3, 0));

        System.out.println(builder2);

        assertTrue(builder1.fullyDominates(builder2, 100));
    }
    @Test
    void builderResizesInternalArrayWhenFull() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        for (int i = 0; i < 126; i++) {
            builder.add(PackedCriteria.pack(480 + i, 127-i, 0));
        }

        assertTrue(builder.build().size() > 0);
        assertTrue(builder.build().size() == 126);

    }

    @Test
    void getThrowsForNonExistentCriteria() {
        ParetoFront front = ParetoFront.EMPTY;
        assertThrows(NoSuchElementException.class, () -> front.get(0, 0));
    }

    @Test
    void builderAddsEdgeCaseValues() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        long minTuple = PackedCriteria.pack(-240, 127, 0);
        long maxTuple = PackedCriteria.pack(2879, 0, 0);

        builder.add(minTuple);
        builder.add(maxTuple);

        assertEquals(2, builder.build().size());
    }

    @Test
    void builderMaintainsLexOrderAfterAdditions() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(PackedCriteria.pack(500, 2, 0));
        builder.add(PackedCriteria.pack(480, 3, 0));
        builder.add(PackedCriteria.pack(490, 1, 0));

        List<Long> tuples = new ArrayList<>();
        builder.forEach(tuples::add);



        assertTrue(tuples.get(0) < tuples.get(1));
    }
    @Test
    void payloadDoesNotAffectDominanceButIsPreserved() {
        long tuple1 = PackedCriteria.pack(480, 2, 0xCAFE_BABE);
        long tuple2 = PackedCriteria.pack(480, 2, 0xDEAD_BEEF);

        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(tuple1);
        builder.add(tuple2); // Doit être ignoré car dominé/égal

        ParetoFront front = builder.build();
        assertEquals(1, front.size());
        assertEquals(0xCAFE_BABE, PackedCriteria.payload(front.get(480, 2)));
    }

    @Test
    void withPayloadUpdatesOnlyPayload() {
        long original = PackedCriteria.pack(600, 3, 0);
        long modified = PackedCriteria.withPayload(original, 0x1234_5678);

        assertEquals(0x1234_5678, PackedCriteria.payload(modified));
        assertEquals(PackedCriteria.arrMins(original), PackedCriteria.arrMins(modified));
        assertEquals(PackedCriteria.changes(original), PackedCriteria.changes(modified));
    }

    @Test
    void boundaryArrivalTimesAreHandled() {
        long minArrival = PackedCriteria.pack(-240, 127, 0); // 4h avant minuit
        long maxArrival = PackedCriteria.pack(2879, 0, 0); // 47h59

        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(minArrival);
        builder.add(maxArrival);

        ParetoFront front = builder.build();
        assertEquals(2, front.size());
        assertThrows(IllegalArgumentException.class, () -> PackedCriteria.pack(-241, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> PackedCriteria.pack(2880, 0, 0));
    }

    @Test
    void boundaryChangesAreHandled() {
        long zeroChanges = PackedCriteria.pack(480, 0, 0);
        long maxChanges = PackedCriteria.pack(480, 127, 0);

        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(zeroChanges);
        builder.add(maxChanges);

        assertEquals(1, builder.build().size());
        assertThrows(IllegalArgumentException.class, () -> PackedCriteria.pack(480, 128, 0));
    }
    @Test
    void mixingDepAndNoDepThrowsOnDominanceCheck() {
        long noDep = PackedCriteria.pack(480, 2, 0);
        long withDep = withDepMins(noDep, 300); // 5h

        ParetoFront.Builder b1 = new ParetoFront.Builder();
        b1.add(noDep);

        ParetoFront.Builder b2 = new ParetoFront.Builder();
        b2.add(withDep);

        assertThrows(IllegalArgumentException.class, () ->
                PackedCriteria.dominatesOrIsEqual(noDep, withDep));
    }
    @Test
    void addingDepTimeToNonDepTuple() {
        long noDep = PackedCriteria.pack(600, 1, 0);
        long withDep = withDepMins(noDep, 720); // 12h

        assertTrue(PackedCriteria.hasDepMins(withDep));
        assertEquals(720, PackedCriteria.depMins(withDep));
        assertEquals(600, PackedCriteria.arrMins(withDep));
    }

    @Test
    void fullyDominatesConsidersDepTimeAdjustment() {
        // Builder1 : tuples sans heure de départ
        ParetoFront.Builder b1 = new ParetoFront.Builder();
        b1.add(PackedCriteria.pack(480, 2, 0)); // Devient (720, 480, 2)
        b1.add(PackedCriteria.pack(600, 1, 0));

        // Builder2 : tuples à dominer
        ParetoFront.Builder b2 = new ParetoFront.Builder();
        b2.add(PackedCriteria.pack(500, 3, 0));
        b2.add(PackedCriteria.pack(650, 2, 0));

        assertThrows(IllegalArgumentException.class, () -> b1.fullyDominates(b2, 0)); // 720 = départ à 12h
    }

    @Test
    void fullyDominatesFailsWhenOneTupleNotDominated() {
        ParetoFront.Builder b1 = new ParetoFront.Builder();
        b1.add(withDepMins(PackedCriteria.pack(600, 2, 0), 720));

        ParetoFront.Builder b2 = new ParetoFront.Builder();
        b2.add(PackedCriteria.pack(500, 1, 0));

        assertFalse(b1.fullyDominates(b2, 720));
    }

    @Test
    void frontRemainsImmutableAfterBuilderChanges() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(PackedCriteria.pack(480, 1, 0));
        ParetoFront front1 = builder.build();

        builder.add(PackedCriteria.pack(600, 0, 0));
        ParetoFront front2 = builder.build();

        assertEquals(1, front1.size());
        assertEquals(2, front2.size());
    }

    @Test
    void sameCriteriaDifferentPayloadDoNotDuplicate() {
        long t1 = PackedCriteria.pack(480, 2, 0xAAAA);
        long t2 = PackedCriteria.pack(480, 2, 0xBBBB);

        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(t1);
        builder.add(t2);

        assertEquals(1, builder.build().size());
        assertEquals(0xAAAA, PackedCriteria.payload(builder.build().get(480, 2)));
    }



}

