package se.ltu.alb;

import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;
import se.ltu.alb.equip.model.AssemblyPlan;

/**
 * Planner for solving EqALBP-2a (Equipped Assembly Line Balancing Problem type
 * 2a, i.e., pure type 2) instances.
 * Type 2a or "pure type 2" means that all equipment has been pinned, and so
 * a complete assembly line exists. All that remains is distributing tasks
 * among the stations.
 * @see se.ltu.alb.equip.model.AssemblyPlan#fromAlb(AlbInstance)
 * @author Christoffer Fink
 */
public class EqALBPlan2a implements Planner<AssemblyPlan> {

    @Override
    public AssemblyPlan solve(AssemblyPlan problem) {
        return solve(problem, "configs/eqalbp2a-config0.xml");
    }

    @Override
    public AssemblyPlan solve(AssemblyPlan problem, SolverConfig config) {
        return SolverFactory
            .<AssemblyPlan>create(config)
            .buildSolver()
            .solve(problem);
    }

}
