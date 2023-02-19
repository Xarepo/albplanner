package se.ltu.alb.equip.algo;

import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.phase.custom.CustomPhaseCommand;
import se.ltu.alb.equip.model.*;
import se.ltu.alb.equip.algo.State;
import ch.rfin.util.ImmutableBits;
import static ch.rfin.util.Printing.printf;
import static ch.rfin.util.Exceptions.assertion;

/**
 * A custom command that checks whether the current working solution is feasible.
 * If not feasible, some explanations are printed.
 * The feasibility check can optionally be turned into an assertion as well so
 * that a failed check throws an exception.
 * <p>
 * Does not modify the solution in any way.
 * <p>
 * <strong>Note:</strong> Currently does NOT check precedence constraints!
 * Only unassigned tasks and missing equipment are checked.
 *
 * @author Christoffer Fink
 */
public class CheckFeasibility implements CustomPhaseCommand<AssemblyPlan> {

    private boolean assertFeasible = false;
    private boolean verbose = false;

    /**
     * Control whether an exception is thrown if the solution is not feasible.
     */
    public void setAssertFeasible(boolean flag) {
        this.assertFeasible = flag;
    }

    /**
     * Control whether to be verbose.
     * The verbose mode only prnits a few extra lines.
     */
    public void setVerbose(boolean flag) {
        this.verbose = flag;
    }

    // FIXME: check task precedence!
    public static boolean checkFeasibility(final AssemblyPlan plan, boolean assertFeasible, boolean verbose) {
        if (verbose) {
            printf("Checking feasibility... (score: %s)", plan.score());
        }
        boolean feasible = true;
        final State state = State.init(plan);
        for (final var task : plan.tasks()) {
            final int taskId = task.id();
            final Station station = task.station();
            if (station == null) {
                feasible = false;
                printf("Task %d is unassigned!", taskId);
                continue;
            }
            final int stationNr = station.number();
            final var installed = state.stationToInstalledEquipments.getOrDefault(stationNr, ImmutableBits.empty());
            final var required = state.taskToEquipments.get(taskId);
            if (!required.subsetOf(installed)) {
                printf("Task %d (equip.req. %s) not compatible with station %d (installed %s)",
                        taskId, required, stationNr, installed);
                feasible = false;
            }
        }
        if (assertFeasible && !feasible) {
            assertion(" !! Solution not feasible!");
        }
        if (verbose) {
            printf("...OK");
        }
        return feasible;
    }

    @Override
    public void changeWorkingSolution(final ScoreDirector<AssemblyPlan> scoreDirector) {
        AssemblyPlan plan = scoreDirector.getWorkingSolution();
        checkFeasibility(plan, assertFeasible, verbose);
    }

}
