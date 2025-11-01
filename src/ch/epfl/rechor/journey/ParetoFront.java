package ch.epfl.rechor.journey;

import ch.epfl.rechor.FormatterFr;

import java.time.Duration;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * @author Valentin Walendy (393413)
 * @author Ruben Lellouche (400288)
 */
public class ParetoFront {

    public static final ParetoFront EMPTY = new ParetoFront(new long[0]);
    private final long[] criteria;

    /**
     * Private constructor initializing the Pareto criteria array.
     * Copies the input array to ensure immutability.
     */
    private ParetoFront(long[] criteria) {
        this.criteria = Arrays.copyOf(criteria, criteria.length);
    }

    /**
     * Returns the number of tuples in the Pareto front.
     */
    public int size() {
        return criteria.length;
    }

    /**
     * Retrieves a tuple matching given arrival minutes and changes count.
     * @throws NoSuchElementException if no matching tuple is found.
     */
    public long get(int arrMins, int changes) {
        for (long crit : criteria) {
            if (PackedCriteria.arrMins(crit) == arrMins
                    && PackedCriteria.changes(crit) == changes) {
                return crit;
            }
        }
        throw new NoSuchElementException();
    }

    /**
     * Applies the given action to each tuple in the Pareto front.
     */
    public void forEach(LongConsumer action) {
        Objects.requireNonNull(action);
        for (long crit : criteria) {
            action.accept(crit);
        }
    }

    /**
     * Builds a string representation listing departure (if any), arrival, and changes.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (long crit : criteria) {
            if (PackedCriteria.hasDepMins(crit)) {
                int dep = PackedCriteria.depMins(crit);
                sb.append("dep: ")
                        .append(FormatterFr.formatDuration(Duration.ofMinutes(dep)))
                        .append(", ");
            }
            int arr = PackedCriteria.arrMins(crit);
            sb.append("arr: ")
                    .append(FormatterFr.formatDuration(Duration.ofMinutes(arr)))
                    .append(", ");
            sb.append("Changes: ")
                    .append(PackedCriteria.changes(crit))
                    .append("\n");
        }
        return sb.toString();
    }

    /**
     * Builder class for constructing a Pareto front incrementally.
     */
    public static class Builder {
        private static final int INITIAL_CAPACITY = 2;
        private long[] front;
        private int size;

        /**
         * Initializes an empty builder.
         */
        public Builder() {
            this.front = new long[INITIAL_CAPACITY];
            this.size  = 0;
        }

        /**
         * Copy constructor.
         */
        public Builder(Builder that) {
            this.front = Arrays.copyOf(that.front, that.front.length);
            this.size  = that.size;
        }

        /**
         * Removes all tuples dominated by the given tuple.
         */
        private void compact(long tup) {
            for (int i = 0; i < size; i++) {
                if (PackedCriteria.dominatesOrIsEqual(tup, front[i])) {
                    System.arraycopy(front, i + 1, front, i, size - (i + 1));
                    size--;
                    i--;  // recheck at this index
                }
            }
        }

        /**
         * Adds a packed tuple, maintaining lexicographic order
         * and removing tuples dominated by the new one.
         */
        public Builder add(long tup) {
            // Skip if an existing tuple dominates
            for (int i = 0; i < size; i++) {
                if (PackedCriteria.dominatesOrIsEqual(front[i], tup)) {
                    return this;
                }
            }

            // Remove dominated tuples
            compact(tup);

            // Determine insertion position lexicographically
            int pos = 0;
            while (pos < size && front[pos] < tup) {
                pos++;
            }

            // Resize array if needed
            if (size == front.length) {
                front = Arrays.copyOf(front, front.length * 2);
            }

            // Shift elements to make room
            if (size > pos) {
                System.arraycopy(front, pos, front, pos + 1, size - pos);
            }

            // Insert and increment size
            front[pos] = tup;
            size++;
            return this;
        }

        /**
         * Packs parameters into a tuple and adds it to the builder.
         */
        public Builder add(int arrMins, int changes, int payload) {
            return add(PackedCriteria.pack(arrMins, changes, payload));
        }

        /**
         * Adds all tuples from another builder.
         */
        public Builder addAll(Builder that) {
            for (int i = 0; i < that.size; i++) {
                add(that.front[i]);
            }
            return this;
        }

        /**
         * Checks if all tuples of 'that' builder are dominated by this builder,
         * after forcing departure time to depMins if needed.
         */
        public boolean fullyDominates(Builder that, int depMins) {
            boolean thisNoDep = true;
            for (int i = 0; i < size; i++) {
                if (PackedCriteria.hasDepMins(front[i])) {
                    thisNoDep = false;
                    break;
                }
            }
            boolean thatNoDep = true;
            for (int i = 0; i < that.size; i++) {
                if (PackedCriteria.hasDepMins(that.front[i])) {
                    thatNoDep = false;
                    break;
                }
            }
            if (thisNoDep && thatNoDep) {
                if (depMins == 0) {
                    throw new IllegalArgumentException();
                }
                for (int i = 0; i < that.size; i++) {
                    long t2 = that.front[i];
                    boolean dom = false;
                    for (int j = 0; j < size; j++) {
                        long t1 = front[j];
                        if (PackedCriteria.arrMins(t1) <= PackedCriteria.arrMins(t2)
                                && PackedCriteria.changes(t1) <= PackedCriteria.changes(t2)) {
                            dom = true;
                            break;
                        }
                    }
                    if (!dom) {
                        return false;
                    }
                }
                return true;
            }
            for (int i = 0; i < that.size; i++) {
                long t2 = PackedCriteria.withDepMins(that.front[i], depMins);
                boolean dom = false;
                for (int j = 0; j < size; j++) {
                    long t1 = front[j];
                    long x1 = PackedCriteria.hasDepMins(t1)
                            ? t1
                            : PackedCriteria.withDepMins(t1, depMins);
                    if (PackedCriteria.dominatesOrIsEqual(x1, t2)) {
                        dom = true;
                        break;
                    }
                }
                if (!dom) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Applies the given action to each tuple under construction.
         */
        public void forEach(LongConsumer action) {
            Objects.requireNonNull(action);
            for (int i = 0; i < size; i++) {
                action.accept(front[i]);
            }
        }

        /**
         * Builds an immutable ParetoFront from the current tuples.
         */
        public ParetoFront build() {
            return new ParetoFront(Arrays.copyOf(front, size));
        }

        /**
         * Returns a textual representation of the builder's current front.
         */
        @Override
        public String toString() {
            return build().toString();
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public Builder clear() {
            size = 0;
            return this;
        }


    }
}
