package se.ltu.alb.equip;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.IntStream;
import java.util.function.Function;
import static java.util.stream.Collectors.toList;
import static java.lang.String.format;
import ch.rfin.alb.AlbInstance;
import ch.rfin.alb.Alb;
import ch.rfin.util.Pair;
import ch.rfin.util.Pairs;
import static ch.rfin.util.Pair.pair;
import static ch.rfin.util.Misc.optional;
import static ch.rfin.util.Misc.optionalInt;
import static ch.rfin.util.Misc.get;
import static ch.rfin.util.Functions.max;
import static ch.rfin.util.Functions.incBy;
import static ch.rfin.util.Exceptions.illegalArg;
import static ch.rfin.util.Printing.printf;
import se.ltu.alb.equip.test.ProblemGenerator;

// TODO: This class needs a lot of cleanup. Maybe replace completely!

public class Util {

    public static Stream<AlbInstance> readAlbInstances(Stream<Pair<Integer,Integer>> instances) {
        return instances
            .map(nr -> {
                try {
                    return readInstance(nr._1, nr._2);
                } catch (Throwable e) {
                    return null; // XXX
                }
            })
            .map(AlbInstance::zeroBased);
    }

    // XXX: THIS IS ONLY FOR THE HUGE INSTANCES
    // For large, the indices are different!
    /**
     * Returns a list of the indices of the "hard 1/3" instances.
     * These are the intervals
     * [26,50], [101,125], [176,200], [251,275],
     * [326,350], [401,425], [476,500].
     */
    public static List<Integer> hardInstances() {
        List<Integer> indices = new ArrayList<>();
        // [26+75i, 50+75i], for i = 0,1,2,…,6
        for (int i = 0; i < 7; i++) {
            for (int j = 26; j < 51; j++) {
                indices.add(j + 75*i);
            }
        }
        return indices;
    }

    public static Stream<AlbInstance> readHardAlbInstances(int size, int count) {
        List<Integer> indices = hardInstances();
        final int step = 175 / count;
        Stream<Pair<Integer,Integer>> instances = Stream
            .iterate(0, incBy(step))
            .limit(count)
            .map(indices::get)
            .map(Pairs.pairWithFirst(size));
        return readAlbInstances(instances);
    }

    // XXX: Just as with difficult instances, these are probably different for large size.
    // These are the highest-index instances that are especially difficult.
    public static List<Integer> peskyInstances() {
        // 1, 4, 7, 10, 13, 16, ..., 478, ..., 499, ..., 517, 520, 523
        // 1, 2, 3,  4,  5,  6, ..., 160, ..., 167, ..., 173, 174, 175
        // (i-1)*3 + 1 = 3i - 3 + 1 = 3i - 2
        // 160..167 = 478..499 (probably 475..499)
        return IntStream.range(160, 167)
            .map(i -> (i-1) * 3 + 1)
            .boxed()
            .collect(toList());
    }

    public static Stream<AlbInstance> readPeskyAlbInstances(int size) {
        if (size != 1000) {
            throw new IllegalArgumentException("Only huge (n=1000) instances currently supported");
        }
        List<Integer> indices = peskyInstances();
        Stream<Pair<Integer,Integer>> instances = indices.stream()
            .map(Pairs.pairWithFirst(size));
        return readAlbInstances(instances);
    }

    public static Stream<AlbInstance> readSubset(int size, int count, List<Integer> indices) {
        final int step = indices.size() / count;
        Stream<Pair<Integer,Integer>> instances = Stream
            .iterate(0, incBy(step))
            .limit(count)
            .map(indices::get)
            .map(Pairs.pairWithFirst(size));
        return readAlbInstances(instances);
    }

    public static Stream<AlbInstance> readAlbInstances(int size, Stream<Integer> instances) {
        return readAlbInstances(instances.map(Pairs.pairWithFirst(size)));
    }

    public static AlbInstance readInstance(int size, int nr) throws Exception {
        final String base = "src/main/resources/data/n%1$d/instance_n=%1$d_%2$d.alb";
        String instanceFile = format(base, size, nr);
        return Alb.parseFile(instanceFile);
    }

    // We can control 4 or 5 different parameters:
    // - How many problems we want
    //   - How many original problems
    //   - How many variations of each
    // - Which originals to use
    // - How variations are produced (setting the converter)
    //  
    // We might want to read a large number of problems and simply pretend that
    // they are EqALB instances -- so only produce a single variation.
    // Or we might want to read a relatively small number and solve lots of
    // variations of each.
    // But we might want to be able to control *which* original problems to use.
    // Finally, we almost certainly want to be able to influence how they are
    // converted (what ProblemGenerator.Parameters are used).

    // Use case 1:
    // Directly and explicitly specify the instance numbers to read and convert,
    // and the number of variations to generate for each.
    // 
    // Use case 2:
    // Specify (implicitly) exactly the instances you want to convert, and how
    // many variations of each.
    // first, step, [originals or problems + variations]
    //
    // Use case 3:
    // [first], [step], originals, (assume variations=1)
    // If first and/or step are missing, they can be inferred from originals.

    // first + step ⇒ originals (sort of)
    //  
    // originals + variations ⇒ total problems
    //
    // originals + first ⇒ step
    //
    // originals + total ⇒ step, variations
    //
    // What we ultimately need is first, step, originals, variations.

    // TODO: maybe make it possible to read a random sample of arbitrary size
    // (rather than having to read a subsequence of 1..525).

    // TODO: move most of this functionality out to a more general (not
    // EqALB-specific) Util and make this Reader delegate and only convert.
    public static class Reader {
        public static final List<Integer> supportedSizes = List.of(
                20, 50, 100, 1000);
        public static final int minInstanceNumber = 1;
        public static final int maxInstanceNumber = 525;
        public static final int defaultVariations = 1;
        public int size = 100;  // 20, 50, 100, 1000
        public int first = 1;   // 1-525
        public OptionalInt step = optionalInt();    // step size
        public OptionalInt originalProblems = optionalInt();   // <= 525
        public OptionalInt totalProblems = optionalInt();
        public OptionalInt variations = optionalInt();  // how many random versions of each?
        private Function<AlbInstance,AlbInstance> converter = null;

        public static Reader reader() {
            return new Reader();
        }

        public static Reader of(Function<AlbInstance,AlbInstance> f) {
            return reader().converter(f);
        }

        /**
         * Returns a stream of EqALB problems.
         */
        public Stream<AlbInstance> read() {
            assert valid();
            final int variations = variations();
            final int total = total();
            printf("reading problems... %d variations each, %d total",
                    variations, total);
            return readOriginals()
                .flatMap(alb ->
                        Stream.generate(() -> converter.apply(alb))
                        .limit(variations())
                )
                .limit(total());
        }

        /**
         * Returns a stream of plain (unconverted) ALB problems.
         */
        public Stream<AlbInstance> readOriginals() {
            var instances = indices()
                .map(i -> pair(size, i))
                .peek(alb -> printf("read original instance %s", alb));
            return readAlbInstances(instances);
        }

        public Reader converter(Function<AlbInstance,AlbInstance> f) {
            this.converter = f;
            return this;
        }

        /**
         * Sets the total number of problems to read/generate.
         */
        public Reader totalProblems(int n) {
            this.totalProblems = optional(n);
            return this;
        }

        /**
         * Sets the number of original problems to read.
         */
        public Reader originalProblems(int n) {
            assert n >= 1 && n <= maxInstanceNumber;
            this.originalProblems = optional(n);
            return this;
        }

        /**
         * Sets the number of variations that should be generated for each
         * original problem that is read.
         */
        public Reader variations(int n) {
            this.variations = optional(n);
            return this;
        }

        /** Sets the first problem instance. */
        public Reader first(int n) {
            this.first = n;
            return this;
        }

        /**
         * Sets the interval between instances.
         * For example, setting first to 7 and step to 5 would read instances
         * 7, 12, 17, 22, etc.
         */
        public Reader step(int n) {
            assert n < 525;
            this.step = optional(n);
            return this;
        }

        /**
         * Sets the problem size.
         * The size must be in {20, 50, 100, 1000}.
         */
        public Reader size(int size) {
            if (!List.of(20, 50, 100, 1000).contains(size)) {
                illegalArg("size (%d) not in {20, 50, 100, 1000}", size);
            }
            this.size = size;
            return this;
        }

        /**
         * Set problem size to small (n=20).
         * @see #size(int)
         */
        public Reader small() {
            return size(20);
        }

        /**
         * Set problem size to medium (n=50).
         * @see #size(int)
         */
        public Reader medium() {
            return size(50);
        }

        /**
         * Set problem size to large (n=100).
         * @see #size(int)
         */
        public Reader large() {
            return size(100);
        }

        /**
         * Set problem size to very large (n=1000).
         * @see #size(int)
         */
        public Reader veryLarge() {
            return size(1000);
        }

        private Stream<Integer> indices() {
            final int step = step();
            final int limit = originals();
            printf("returning indices for first=%d, step=%d, originals=%d",
                    first, step, limit);
            return IntStream.iterate(
                    first,
                    i -> true,
                    i -> i + step)
                .takeWhile(i -> i <= maxInstanceNumber)
                .limit(originals())
                .boxed();
        }

        // Can infer steps from first and originals.
        private int step() {
            if (step.isPresent()) {
                return get(step);
            }
            final int originals = originals();
            return max(1, (maxInstanceNumber-first+1) / originals);
        }

        // Can only infer oritinals if we have steps or total AND variations.
        private int originals() {
            if (originalProblems.isPresent()) {
                return get(originalProblems);
            }
            // first + step ⇒ originals
            if (step.isPresent()) {
                return (maxInstanceNumber - first) / get(step) + 1;
            }
            // total + variations ⇒ originals
            assert totalProblems.isPresent() && variations.isPresent();
            final int total = get(totalProblems);
            final int variations = get(this.variations);
            return (total + variations - 1) / variations; // ⌈total/variations⌉
        }

        // Can only infer variations if we can infer originals AND total.
        private int variations() {
            if (variations.isPresent()) {
                return get(variations);
            }
            return total() / originals();
        }

        // Can only infer total if we can infer originals AND variations.
        private int total() {
            if (totalProblems.isPresent()) {
                return get(totalProblems);
            }
            return originals() * variations();
        }

        // step OR originals OR (total AND variations)
        // total OR (originals AND variations)
        // variations OR (total AND originals)
        private boolean valid() {
            assert step.isPresent()
                || originalProblems.isPresent()
                || (totalProblems.isPresent() && variations.isPresent());
            assert totalProblems.isPresent()
                || (originalProblems.isPresent() && variations.isPresent());
            assert variations.isPresent()
                || (totalProblems.isPresent() && originalProblems.isPresent());
            assert total() >= variations();
            int step = step();
            assert step >= 1 && step <= maxInstanceNumber;
            int originals = originals();
            assert originals > 0 && originals <= maxInstanceNumber;
            return true;
        }

    }

}
