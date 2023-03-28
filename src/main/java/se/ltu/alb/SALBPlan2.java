package se.ltu.alb;

import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;
import se.ltu.alb.salbp.model.AssemblyPlan;

/**
 * Planner for solving SALBP-2 (Simple Assembly Line Balancing Problem type 2)
 * instances.
 * @see se.ltu.alb.salbp.model.AssemblyPlan#fromAlb(AlbInstance)
 * @author Christoffer Fink
 */
public class SALBPlan2 implements Planner<AssemblyPlan> {

    @Override
    public AssemblyPlan solve(AssemblyPlan problem) {
        return solve(problem, "configs/salbp2-config0.xml");
    }

    @Override
    public AssemblyPlan solve(AssemblyPlan problem, SolverConfig config) {
        return SolverFactory
            .<AssemblyPlan>create(config)
            .buildSolver()
            .solve(problem);
    }

}
