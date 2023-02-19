package se.ltu.alb.equip.algo;

import java.util.*;
import se.ltu.alb.equip.model.*;
import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.phase.custom.CustomPhaseCommand;
import static ch.rfin.util.Printing.printf;

// TODO: should probably show equipment

/**
 * A custom command that shows the current working solution.
 * Does not modify the solution in any way.
 * Shows a brief summary about the solution:
 * how many of the stations are used (have at least one task assigned);
 * the cycle time; and the sum of squared station times.
 * If the verbose flag is set, then the actual solution is also shown,
 * i.e., which tasks are assigned to which stations.
 * (This is very verbose for large instances!)
 *
 * @author Christoffer Fink
 */
public class ShowWorkingSolution implements CustomPhaseCommand<AssemblyPlan> {

    private boolean verbose = false;

    @Deprecated(forRemoval = true)
    public void setBrief(boolean brief) {
        this.verbose = !brief;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public static void showSolution(final AssemblyPlan plan, boolean verbose) {
        final int m = plan.stations().size();
        int[] stationTimes = new int[m];
        int[] stationLoads = new int[m];
        int totalTime = 0;
        for (final var task : plan.tasks()) {
            if (task.station() == null) {
                continue;
            }
            totalTime += task.time();
            final int num = task.station().number();
            stationTimes[num] += task.time();
            stationLoads[num]++;
        }
        int cycleTime = 0;
        long squaredTimes = 0;
        int stationsUsed = 0;
        for (int i = 0; i < m; i++) {
            final int stationTime = stationTimes[i];
            if (stationTime > cycleTime) {
                cycleTime = stationTime;
            }
            squaredTimes += 1L * stationTime * stationTime;
            final int stationLoad = stationLoads[i];
            if (stationLoad > 0) {
                stationsUsed++;
            }
            if (verbose) {
                printf("%3d: # = %2d, T = %5d", i+1, stationLoad, stationTime);
            }
        }
        printf("Stations: %3d/%3d, ct: %5d, ssst: %1.1e", stationsUsed, m, cycleTime, 1.0 * squaredTimes);
    }

    @Override
    public void changeWorkingSolution(final ScoreDirector<AssemblyPlan> scoreDirector) {
        AssemblyPlan plan = scoreDirector.getWorkingSolution();
        showSolution(plan, verbose);
    }

}
