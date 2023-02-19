package se.ltu.alb;

import org.optaplanner.core.config.solver.SolverConfig;

/**
 * A Planner solves problem instances.
 * @author Christoffer Fink
 */
public interface Planner<P> {

    /**
     * Solve the given problem instance (using some default config).
     * The default config may be hard-coded or determined dynamically based on
     * the properties problem instance, depending on the implementation of the
     * planner.
     */
    public P solve(P problemInstance);

    /**
     * Solve the given problem instance using the Solver Config.
     */
    public P solve(P problemInstance, SolverConfig config);

    /**
     * Solve the given problem instance using the given XML config.
     * The config is a resource available on the classpath, not an arbitrary
     * file path.
     */
    public default P solve(P problemInstance, String configXmlRes) {
        return solve(problemInstance, SolverConfig.createFromXmlResource(configXmlRes));
    }

}
