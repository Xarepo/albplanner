package se.ltu.alb.equip.test;

import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalDouble;
import java.util.function.Function;

import ch.rfin.util.Rng;
import ch.rfin.alb.AlbInstance;
import se.ltu.alb.salbp.algo.RandomType1;
import static ch.rfin.util.Printing.eprintf;
import static ch.rfin.util.Misc.get;
import static ch.rfin.util.Misc.optional;
import static ch.rfin.util.Misc.optionalInt;
import static ch.rfin.util.Misc.optionalDouble;

/*
 * Here's the basic strategy:
 * - Solve the problem in some straightforward way (RandomConstruction).
 * - Randomly distribute equipment among stations.
 * - Make some of the tasks dependent on one or more of the equipment available
 *   at their current station.
 * - Depending on the type of problem, we may reset the solution in the
 *   sense that equipment is no longer assigned to stations.
 *   (So that solving the problem then involves equipping stations.)
 *
 * However, there are many ways to implement that basic strategy.
 *
 * Method 1:
 * Given a fixed (finite) set of equipments, ...
 * - Select a subset of stations, one station per equipment.
 * - Equip each of those stations with one of the equipments.
 * - Make some of the tasks at each station depend on the equipment.
 *
 * Method 2:
 * Given a fixed number of stations that should have equipment, ...
 * - Select a subset of stations.
 * - Assign some number of equipments to each one.
 * - Make some of the tasks at each station depend on the equipment.
 *
 * Method 3:
 * Given a fixed number of tasks that should depend on equipment, ...
 * - Select a subset of tasks.
 * - Make them depend on equipment.
 * - Equip their stations with the necessary equipment.
 * - This is a pretty messy method, since, depending on how the tasks are
 *   paired up with equipment, we may end up with stations with lots of
 *   differnet kinds of equipment.
 *
 * Variables:
 * - How many / which equipments are there?
 * - How many stations should have equipments?
 * - How many tasks should have equipment requirements?
 * - What is the probability that a task depends on an equipment at its station?
 * - What is the probability that a station has an equipment?
 * - Can a station have more than one kind of equipment?
 * - Can a task depend on more than one kind of equipment?
 * - Can an equipment be used more than once?
 * - Do all equipments have to be used (at least once)?
 * - Number of stations / cycle time.
 *
 * We may either want to specify some fixed set of equipments (possibly
 * containing duplicates) that are to be distributed among stations (and
 * tasks), or we may want to specify some number of stations and/or tasks
 * that should have equipment.
 *
 * We may or may not allow stations to have more than one type of equipment,
 * or tasks to be dependent on more than one kind of equipment.
 *
 * We may or may not have a given number of stations or a given cycle time.
 * This would presumably be part of the original ALB problem instance.
 *
 * One can think of the problem from a station- (and/or task-) centeric
 * perspective, or from an equipment-centric perspective.
 * Either stations (and/or tasks) should have some number of equipment, or
 * some set of equipment should be distributed among stations/tasks.
 *
 * TODO:
 * Probabilities have the advantage of being independent of problem size!
 * However, that leaves the question of exactly how they are used.
 * One option is to loop through stations/tasks and flipping a (biased) coin
 * for each one to decide whether or not to give it some equipment.
 * Another option is to simply interpretet the probability as a proportion
 * and then fall back on min/max. In other words, derive min/max automatically
 * in a problem size neutral way. In that case, we might want to use a
 * min/max proportion parameter pair. For example, then we could say that we
 * want between 20 % and 35 % of the tasks to depend on equipment.
 */

/**
 * Adds equipment constraints to ALB problems.
 * This problem generator takes an ALB problem instance and converts it to an
 * EqALB instance. In other words, it adds equipment requirements.
 * The generator works by first generating a feasible solution to the problem,
 * thereby assigning tasks to stations. Then it distributes equipment among
 * stations and tasks such that the solution remains feasible. Hence we know
 * a feasible solution exists.
 * <p>
 * To allow only (at most) one type of equipment per station, set
 * {@link ProblemGenerator.Parameters#maxEquipmentPerStation()} to 1.
 * To force all tasks at a station to require <strong>some</strong> (at least
 * one) of the equipment types available at the station in the current
 * solution, set
 * {@link ProblemGenerator.Parameters#taskNeedsEquipmentAtStation()} to 1.0.
 * TODO: well, <strong>how many</strong> of them should it require?
 * @author Christoffer Fink
 */
public class ProblemGenerator {

    // TODO: Maybe specify distribution (uniform/gaussian) for some params?
    /* 
     * TODO: The difficulty with some min/max parameters is that we can't
     * know ahead of time what the limiting factors will be. And so setting
     * a min/max range and generating random numbers in that range may produce
     * values outside the true range. So we may have to explicitly get the
     * min/max limits and adjust them, ultimately ending up having to
     * generate the random numbers manually. So the parameter() method that
     * is supposed to be able to take care of that for us becomes useless.
     * Possible solution:
     * For each parameter() method (corresponding to a minParameter() and
     * maxParameter() pair), add a parameter(lower, upper) method that
     * allows a cap to be added from the outside.  If one of the limits is
     * missing, the one that is passed in can be used. If a limit exists,
     * the more limiting value takes precedence. This also solves the problem
     * of always having to set both limits for the parameter to be usable.
     * So what was the point of setting the min/max in the first place, if
     * we're just gonna pass in limits anyway?
     * Well, they can have slightly different semantics:
     * The min/max can specify the value we would ideally like to achieve,
     * while the passed-in limits can be a harder limit that specifies what is
     * actually possible to achieve. Since the range would only be made more
     * narrow, it would be consistent with the initial specification.
     */

    /*
     * Compared to the Problem Generator, this class is fairly dumb. It's up
     * to the generator to use the parameters appropriately, which includes
     * making sure that minima, maxima, and probabilities add up, in the sense
     * that once some random numbers have been generated, those values may
     * constrain future values.
     */

    /**
     * Problem Generator Parameters.
     * These allow flexible control of the values that are randomly generated,
     * but also the way in which problems are generated.
     * <p>
     * All parameters (more or less) have a minimum and maximum value.
     * When a parameter value is set explicitly, the min and max are
     * both set to this value.
     * The min and max values can also be set separately.
     * When getting the value of a parameter, either the min and max are
     * the same (as is the case when the parameter has been specified
     * explicitly), in which case that value is returned, or they are
     * different, in which case a number between the min and max is randomly
     * generated.
     * <p>
     * For example, to specify that exactly 7 pieces of equipment should
     * be generated (7 instances, not types), call
     * <pre>
     * param.equipments(7);
     * // ...
     * int equipments = param.equipments(); // 7
     * </pre>
     * To specify that the number of equipments should be between 3 and 9, call
     * <pre>
     * param.minEquipments(3).maxEquipments(9);
     * // ...
     * int equipments = param.equipments(); // [3,9]
     * </pre>
     * <p>
     * Note that the Rng parameter is not just for internal use but is a
     * parameter, like all the others, to be passed to and used by some problem
     * generator.
     * <p>
     * min/max equipment per station refers to those stations that have been
     * choosen to have any equipment. Hence min will typically be (at least) 1.
     * To make it apply to <strong>all</strong> stations, set
     * {@link #stationHasEquipment(double) stationHasEquipment(1.0)} or
     * {@link #minStationsWithEquipment(int) minStationsWithEquipment(number of stations}.
     */
    // TODO: this should be a general class that can be used for
    // all ALB variations. Or should it?
    public static class Parameters {
        // Used to generate values between min and max limits, but can also
        // be viewed as a parameter in its own right.
        private Rng rng;
        // Different types of equipment.
        private OptionalInt minEquipmentTypes = optionalInt();
        private OptionalInt maxEquipmentTypes = optionalInt();
        // Total equipment instances.
        private OptionalInt minEquipments = optionalInt();
        private OptionalInt maxEquipments = optionalInt();
        // Number of instances of each type.
        private OptionalInt minEquipmentPerType = optionalInt();
        private OptionalInt maxEquipmentPerType = optionalInt();
        // Number of stations with some (at least one) type of equipment.
        private OptionalInt minStationsWithEquipment = optionalInt();
        private OptionalInt maxStationsWithEquipment = optionalInt();
        // For a station with equipment, how many types does it have?
        private OptionalInt minEquipmentPerStation = optionalInt();
        private OptionalInt maxEquipmentPerStation = optionalInt();
        // Number of tasks that depend on some (at least one) type of equipment.
        private OptionalInt minTasksWithEquipment = optionalInt();
        private OptionalInt maxTasksWithEquipment = optionalInt();
        // For a task that depends on equipment, how many types does it depend on?
        private OptionalInt minEquipmentPerTask = optionalInt();
        private OptionalInt maxEquipmentPerTask = optionalInt();

        /**
         * The probability that a task depends on the equipment that is
         * available at its assigned station.
         * If tasks are allowed to depend on more than one type of equipment,
         * this is applied to each equipment at the station.
         */
        private OptionalDouble taskNeedsEquipmentAtStation = optionalDouble();
        /**
         * The probability that a task depends on any equipment.
         * TODO: what if tasks are allowed to depend on more than one
         * type of equipment? Does that interact with this probability?
         */
        private OptionalDouble taskNeedsEquipment = optionalDouble();
        /**
         * The probability that a station has any equipment.
         * TODO: what if stations are allowed to be equipped with more than one
         * type of equipment? Does that interact with this probability?
         */
        private OptionalDouble stationHasEquipment = optionalDouble();
        // pinned ⇒ equipped
        // ¬equipped ⇒ ¬pinned
        // equipped ⇒ pinned ∨ ¬pinned
        // Type 1 ⇒ Pr[pinned] = 0.0  ∧  Pr[equipped] = [0,1]
        // Type 2 ⇒ Pr[pinned] = 1.0
        private OptionalDouble isPinned = optionalDouble();
        private OptionalDouble isEquipped = optionalDouble();
        private OptionalDouble fractionUnique = optionalDouble();

        /**
         * Problem type (1 for type 1; 2 for type 2; 0 for mixed).
         */
        private int problemType = 2;

        private Parameters(Parameters param) {
            rng = param.rng;
            minEquipmentTypes = param.minEquipmentTypes;
            maxEquipmentTypes = param.maxEquipmentTypes;
            minEquipments = param.minEquipments;
            maxEquipments = param.maxEquipments;
            minEquipmentPerType = param.minEquipmentPerType;
            maxEquipmentPerType = param.maxEquipmentPerType;
            minStationsWithEquipment = param.minStationsWithEquipment;
            maxStationsWithEquipment = param.maxStationsWithEquipment;
            minEquipmentPerStation = param.minEquipmentPerStation;
            maxEquipmentPerStation = param.maxEquipmentPerStation;
            minTasksWithEquipment = param.minTasksWithEquipment;
            maxTasksWithEquipment = param.maxTasksWithEquipment;
            minEquipmentPerTask = param.minEquipmentPerTask;
            maxEquipmentPerTask = param.maxEquipmentPerTask;
            taskNeedsEquipmentAtStation = param.taskNeedsEquipmentAtStation;
            taskNeedsEquipment = param.taskNeedsEquipment;
            stationHasEquipment = param.stationHasEquipment;
            isPinned = param.isPinned;
            isEquipped = param.isEquipped;
            fractionUnique = param.fractionUnique;
            problemType = param.problemType;
        }

        private Parameters() {
            this(new Rng());
            eprintf("Warning: creating RNG without seed. Not reproducible!");
        }

        private Parameters(final Rng rng) {
            this.rng = rng;
        }

        /** Instantiate the RNG to a random seed. */
        public static Parameters of() {
            return new Parameters();
        }

        /** Instantiate the RNG to the given seed. */
        public static Parameters of(final long seed) {
            return of(new Rng(seed));
        }

        /** Use the given RNG. */
        public static Parameters of(final Rng rng) {
            return new Parameters(rng);
        }

        /**
         * Copy the given Parameters object.
         * This is especially useful for making instance-specific tweaks
         * to the parameters without modifying the original parameters.
         * <p>
         * <strong>Note:</strong> the original and the new parameters object
         * will share a reference to the same Rng instance!
         */
        public static Parameters of(final Parameters param) {
            return new Parameters(param);
        }

        /** Get the random number generator. */
        public Rng rng() {
            return rng;
        }

        /** The minimum number of equipment instances to generate. */
        public OptionalInt minEquipments() {
            return minEquipments;
        }
        /** The maximum number of equipment instances to generate. */
        public OptionalInt maxEquipments() {
            return maxEquipments;
        }
        /** The minimum number of equipment types to generate. */
        public OptionalInt minEquipmentTypes() {
            return minEquipmentTypes;
        }
        /** The maximum number of equipment types to generate. */
        public OptionalInt maxEquipmentTypes() {
            return maxEquipmentTypes;
        }
        /** The minimum number of times a type of equipment is used. */
        public OptionalInt minEquipmentPerType() {
            return minEquipmentPerType;
        }
        /** The maximum number of times a type of equipment is used. */
        public OptionalInt maxEquipmentPerType() {
            return maxEquipmentPerType;
        }
        /** TODO. */
        public OptionalInt minStationsWithEquipment() {
            return minStationsWithEquipment;
        }
        /** TODO. */
        public OptionalInt maxStationsWithEquipment() {
            return maxStationsWithEquipment;
        }
        /** The minimum number of equipments a station must have. */
        public OptionalInt minEquipmentPerStation() {
            return minEquipmentPerStation;
        }
        /** The maximum number of equipments a station is allowed to have. */
        public OptionalInt maxEquipmentPerStation() {
            return maxEquipmentPerStation;
        }
        /** TODO. */
        public OptionalInt minTasksWithEquipment() {
            return minTasksWithEquipment;
        }
        /** TODO. */
        public OptionalInt maxTasksWithEquipment() {
            return maxTasksWithEquipment;
        }
        /** The minimum number of equipments a task must depend on. */
        public OptionalInt minEquipmentPerTask() {
            return minEquipmentPerTask;
        }
        /** The maximum number of equipments a task is allowed to depend on. */
        public OptionalInt maxEquipmentPerTask() {
            return maxEquipmentPerTask;
        }

        /** The minimum number of equipment instances to generate. */
        public Parameters minEquipments(int n) {
            minEquipments = optional(n);
            return this;
        }
        /** The maximum number of equipment instances to generate. */
        public Parameters maxEquipments(int n) {
            maxEquipments = optional(n);
            return this;
        }
        /** The minimum number of equipment types to generate. */
        public Parameters minEquipmentTypes(int n) {
            minEquipmentTypes = optional(n);
            return this;
        }
        /** The maximum number of equipment types to generate. */
        public Parameters maxEquipmentTypes(int n) {
            maxEquipmentTypes = optional(n);
            return this;
        }
        /** The minimum number of times a type of equipment is used. */
        public Parameters minEquipmentPerType(int n) {
            minEquipmentPerType = optional(n);
            return this;
        }
        /** The maximum number of times a type of equipment is used. */
        public Parameters maxEquipmentPerType(int n) {
            maxEquipmentPerType = optional(n);
            return this;
        }
        /** TODO. */
        public Parameters minStationsWithEquipment(int n) {
            minStationsWithEquipment = optional(n);
            return this;
        }
        /** TODO. */
        public Parameters maxStationsWithEquipment(int n) {
            maxStationsWithEquipment = optional(n);
            return this;
        }
        /** The minimum number of equipments a station must have. */
        public Parameters minEquipmentPerStation(int n) {
            minEquipmentPerStation = optional(n);
            return this;
        }
        /** The maximum number of equipments a station is allowed to have. */
        public Parameters maxEquipmentPerStation(int n) {
            maxEquipmentPerStation = optional(n);
            return this;
        }
        /** TODO. */
        public Parameters minTasksWithEquipment(int n) {
            minTasksWithEquipment = optional(n);
            return this;
        }
        /** TODO. */
        public Parameters maxTasksWithEquipment(int n) {
            maxTasksWithEquipment = optional(n);
            return this;
        }
        /** The minimum number of equipments a task must depend on. */
        public Parameters minEquipmentPerTask(int n) {
            minEquipmentPerTask = optional(n);
            return this;
        }
        /** The maximum number of equipments a task is allowed to depend on. */
        public Parameters maxEquipmentPerTask(int n) {
            maxEquipmentPerTask = optional(n);
            return this;
        }

        /** The number of equipment instances to generate. */
        public Parameters equipments(int n) {
            return minEquipments(n).maxEquipments(n);
        }
        /** The number of equipment types to generate. */
        public Parameters equipmentTypes(int n) {
            return minEquipmentTypes(n).maxEquipmentTypes(n);
        }
        /** The number of times a type of equipment is used. */
        public Parameters equipmentPerType(int n) {
            return minEquipmentPerType(n).maxEquipmentPerType(n);
        }
        /** The number of stations with at least one type of equipment. */
        public Parameters stationsWithEquipment(int n) {
            return minStationsWithEquipment(n).maxStationsWithEquipment(n);
        }
        /** The number of tasks that depend on at least one type of equipment. */
        public Parameters tasksWithEquipment(int n) {
            return minTasksWithEquipment(n).maxTasksWithEquipment(n);
        }
        /** The number of equipments a station must have. */
        public Parameters equipmentPerStation(int n) {
            return minEquipmentPerStation(n).maxEquipmentPerStation(n);
        }
        /** The number of equipments a task must depend on. */
        public Parameters equipmentPerTask(int n) {
            return minEquipmentPerTask(n).maxEquipmentPerTask(n);
        }

        /**
         * TODO.
         */
        public Parameters taskNeedsEquipmentAtStation(double n) {
            assert n >= 0.0 && n <= 1.0;
            taskNeedsEquipmentAtStation = optional(n);
            return this;
        }
        /**
         * TODO.
         */
        public Parameters taskNeedsEquipment(double n) {
            assert n >= 0.0 && n <= 1.0;
            taskNeedsEquipment = optional(n);
            return this;
        }
        /**
         * TODO.
         */
        public Parameters stationHasEquipment(double n) {
            assert n >= 0.0 && n <= 1.0;
            stationHasEquipment = optional(n);
            return this;
        }
        /**
         * TODO.
         */
        public Parameters fractionUnique(double n) {
            assert n >= 0.0 && n <= 1.0;
            fractionUnique = optional(n);
            return this;
        }
        /**
         * The probability that an equipped equipment is pinned.
         * Note that this applies only to those equipments that are assigned
         * to some station.
         * May be interpreted as a per-equipment probability or as an overall
         * fraction.
         */
        public Parameters isPinned(double n) {
            assert n >= 0.0 && n <= 1.0;
            isPinned = optional(n);
            // TODO: inspect/modify problemType?
            return this;
        }
        /**
         * The probability that an equipment is equipped at a station.
         * May be interpreted as a per-equipment probability, or as a fraction.
         * TODO: comment on problem types (when does this come into play?).
         */
        public Parameters isEquipped(double n) {
            assert n >= 0.0 && n <= 1.0;
            isEquipped = optional(n);
            // TODO: inspect/modify problemType?
            return this;
        }

        // equipments() and equipmentTypes() should only be called once per
        // problem. Generating multiple per problem just doesn't make sense.
        /** The number of equipment instances to generate. */
        public int equipments() {
            return generate(get(minEquipments()), get(maxEquipments()));
        }
        /** The number of equipment types to generate. */
        public int equipmentTypes() {
            return generate(get(minEquipmentTypes()), get(maxEquipmentTypes()));
        }
        // These can be called multiple times per problem.
        // equipmentTypeUse() should probably be called for each equipment.
        // equipmentPerStation() and equipmentPerTask() should probably be called
        // for each station/task.
        /** The number of times a type of equipment is used. */
        public int equipmentPerType() {
            return generate(get(minEquipmentPerType()), get(maxEquipmentPerType()));
        }
        /** The number of stations with at least one type of equipment. */
        public int stationsWithEquipment() {
            return generate(get(minStationsWithEquipment), get(maxStationsWithEquipment));
        }
        /** The number of tasks that depend on at least one type of equipment. */
        public int tasksWithEquipment() {
            return generate(get(minTasksWithEquipment), get(maxTasksWithEquipment));
        }
        /** The number of equipments a station must have. */
        public int equipmentPerStation() {
            return generate(get(minEquipmentPerStation), get(maxEquipmentPerStation));
        }
        /** The number of equipments a task must depend on. */
        public int equipmentPerTask() {
            return generate(get(minEquipmentPerTask), get(maxEquipmentPerTask));
        }

        // FIXME: how to deal with optional double?
        // On the one hand, it's nice to be able to randomize a probability
        // if missing, but we may also need some way of knowing whether
        // the parameter has been specified!

        /** TODO. */
        public OptionalDouble taskNeedsEquipmentAtStation() {
            return taskNeedsEquipmentAtStation;
        }

        /** TODO. */
        public OptionalDouble taskNeedsEquipment() {
            return taskNeedsEquipment;
        }

        /** TODO. */
        public OptionalDouble stationHasEquipment() {
            return stationHasEquipment;
        }

        /** TODO. */
        public OptionalDouble fractionUnique() {
            return fractionUnique;
        }

        // pinned ⇒ equipped
        // ¬equipped ⇒ ¬pinned
        // equipped ⇒ pinned ∨ ¬pinned
        // Type 1 ⇒ Pr[pinned] = 0.0  ∧  Pr[equipped] = [0,1]
        // Type 2 ⇒ Pr[pinned] = 1.0  ∧  Pr[equipped] = 1.0

        /**
         * Problem type (1 for type 1; 2 for type 2; 0 for mixed).
         * Also sets isPinned and isEquipped accordingly.
         * TODO: what does that mean?
         */
        public Parameters problemType(int type) {
            assert type >= 0 && type <= 2;
            problemType = type;
            if (type == 1) {
                return isPinned(0);
            } else if (type == 2) {
                return isPinned(1).isEquipped(1);
            }
            return this;
        }

        // TODO: throw exceptions (we want this to work even when assertions
        // are not enabled).
        public boolean valid() {
            assert !(maxEquipmentPerType.isPresent()
                && maxEquipmentTypes.isPresent()
                && minEquipments.isPresent())
                || get(maxEquipmentPerType) * get(maxEquipmentTypes) < get(minEquipments);
            // These two assertions assume that a station cannot have more than
            // one instance of the same type of equipment.
            if (has(minEquipmentPerStation) && has(maxEquipmentTypes)) {
                assert get(minEquipmentPerStation) <= get(maxEquipmentTypes);
            }
            if (has(minEquipmentTypes) && has(maxStationsWithEquipment)) {
                assert get(minEquipmentTypes) <= get(maxStationsWithEquipment);
            }
            return true;
        }


        /**
         * Use the random number generator to generate a value between min and
         * max (inclusive).
         */
        public int generate(final int min, final int max) {
            if (min == max) {
                return min;
            }
            return rng.nextIntInclusive(min, max);
        }

        private boolean has(final OptionalInt n) {
            return n.isPresent();
        }

    }

    protected Parameters param;
    protected AlbInstance original;
    private Function<AlbInstance, Map<Integer,Integer>> solver;
    protected Rng rng;

    public ProblemGenerator(Parameters param) {
        this.param = param;
        this.rng = param.rng();
    }

    public ProblemGenerator params(Parameters params) {
        this.param = params;
        return this;
    }

    /**
     * Set the solver used to produce a solution to the original problem.
     * Setting this is optional.
     */
    public ProblemGenerator solver(Function<AlbInstance, Map<Integer,Integer>> f) {
        this.solver = f;
        return this;
    }

    /**
     * Generate EqALB problems based on this initial ALB problem.
     */
    public ProblemGenerator seedProblem(AlbInstance problem) {
        if (!problem.isZeroBased()) {
            this.original = problem.zeroBased();
        } else {
            this.original = problem;
        }
        return this;
    }

    public AlbInstance generate() {
        assert original != null;
        return convert(original);
    }

    public AlbInstance convert(AlbInstance alb) {
        return convert(alb, solve(alb));
    }

    public AlbInstance convert(AlbInstance alb, Map<Integer,Integer> solution) {
        // TODO: don't hard-code using Type2A!
        return GeneratorType2A.of(param).convert(alb, solution);    // XXX
    }

    // task ↦ station
    protected Map<Integer,Integer> solve(AlbInstance alb) {
        if (solver == null) {
            solver = solver();
        }
        return solver.apply(alb);
    }

    /** Make a default solver. */
    private Function<AlbInstance, Map<Integer,Integer>> solver() {
        var solver = new RandomType1()
            .seed(rng.nextLong());
        return solver::randomFeasible;
    }

}
