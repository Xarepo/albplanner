package se.ltu.alb;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import ch.rfin.util.Pair;
import ch.rfin.util.Pairs;

// TODO: should use @ParameterizedTest or @TestFactory

/**
 * Abstract base class for testing planners.
 * Need to override at least one of
 * {@link #problemConfigPairs()} or
 * {@link #problemInstances()} and/or
 * {@link #solverConfigs()}.
 * <p>
 * A test will run for each problem-config pair returned by
 * {@link #problemConfigPairs()}.
 * Additional problem-config pairs will automatically be generated
 * by forming the cross product of problem instances and solver configs returned by
 * {@link #problemInstances()} and {@link #solverConfigs()}.
 * If no solver configs are returned, then the problem instances will be solved
 * using {@link se.ltu.alb.Planner#solve(Object)}, i.e., using a default config
 * that is specific to the planner implementation.
 * <p>
 * It is also possible to include a {@code null} string among the configs to
 * force each instance to also be solved using the default config.
 * (If multiple {@code null} strings are included, the instances would be solved
 * multiple times using the default config.)
 * Similarly, if {@link #problemConfigPairs()} returns a pair where the config
 * is {@code null}, then that problem instance will be solved using the default
 * config.
 * @author Christoffer Fink
 */
public abstract class AbstractPlannerTest<P> {

    public abstract Planner<P> planner();

    /**
     * Return problem instances to be solved.
     * The default implementation returns an empty list.
     * This should never be {@code null}!
     */
    public List<P> problemInstances() {
        return List.of();
    }

    /**
     * Return solver configs (as XML resource).
     * For example, {@code "configs/simple.xml"} might be used to refer to
     * a config in {@code src/main/resources/configs/simple.xml}.
     * The default implementation returns an empty list.
     * This should never be {@code null}!
     * However, null Strings are allowed.
     */
    public List<String> solverConfigs() {
        return List.of();
    }

    /**
     * Solve each of these problem instances using the solver config it is
     * paired up with (given as an XML resource).
     * This should never be {@code null}!
     * However, null Strings (as the second item in a pair) are allowed.
     */
    public List<Pair<P,String>> problemConfigPairs() {
        return List.of();
    }

    private List<String> getConfigsOrSingleNull() {
        List<String> tmp = solverConfigs();
        if (tmp.isEmpty()) {
            tmp = new ArrayList<>();
            tmp.add(null);
        }
        return tmp;
    }

    private List<Pair<P,Optional<String>>> generateCases() {
        final List<Pair<P,String>> pairs = problemConfigPairs();
        final List<P> instances = problemInstances();
        final List<String> tmp = solverConfigs();
        final List<String> configs = getConfigsOrSingleNull();

        final List<Pair<P,Optional<String>>> results = new ArrayList<>();
        pairs.stream()
            .map(Pairs.map_2(Optional::ofNullable))
            .forEach(results::add);
        instances.stream()
            .flatMap(instance -> configs.stream()
                    .map(Optional::ofNullable)
                    .map(Pairs.pairWithFirst(instance)))
            .forEach(results::add);
        return results;
    }

    public abstract void checkSolution(P solution);

    public void test(Planner<P> planner, P problemInstance, String config) {
        final P solution = planner.solve(problemInstance, config);
        checkSolution(solution);
    }

    public void test(Planner<P> planner, P problemInstance) {
        final P solution = planner.solve(problemInstance);
        checkSolution(solution);
    }

    @Test
    public void test() {
        final Planner<P> planner = planner();
        for (final var x : generateCases()) {
            if (x._2.isPresent()) {
                test(planner, x._1, x._2.get());
            } else {
                test(planner, x._1);
            }
        }
    }

}
