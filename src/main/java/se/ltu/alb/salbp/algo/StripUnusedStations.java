package se.ltu.alb.salbp.algo;

import java.util.List;
import static java.util.stream.Collectors.toList;
import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.phase.custom.CustomPhaseCommand;
import se.ltu.alb.salbp.model.*;
import static ch.rfin.util.Printing.printf;

/**
 * Remove unused stations from the problem instance.
 *
 * @author Christoffer Fink
 */
public class StripUnusedStations implements CustomPhaseCommand<AssemblyPlan> {

    private boolean verbose = false;
    /**
     * Leave this many unused stations rather than removing all.
     * Defaults to 0 (remove all).
     */
    private int leave = 0;

    /**
     * Control whether to print out how many stations were removed.
     */
    public void setVerbose(boolean flag) {
        verbose = flag;
    }

    /**
     * Sets how many of the unused stations should be ignored and not removed.
     * By default, no stations are left, meaning all unused are removed.
     */
    public void setLeave(int stations) {
        this.leave = stations;
    }

    @Override
    public void changeWorkingSolution(final ScoreDirector<AssemblyPlan> scoreDirector) {
        AssemblyPlan plan = scoreDirector.getWorkingSolution();
        final int before = plan.stations().size();
        List<Station> stations = plan.stations();
        assert stations
            .stream()
            .sorted()
            .collect(toList())
            .equals(stations);
        int maxStation = -1;
        for (final var task : plan.tasks()) {
            Station station = task.station();
            if (station == null) {
                printf("Solution not initialized (task %d assigned to null station)",
                        task.id());
                printf("Aborting! (Leaving solution unchanged)");
                return;
            }
            maxStation = Math.max(maxStation, station.number());
        }
        int maxStationIndex = maxStation - 1;
        final int limit = maxStationIndex + leave;
        for (int i = stations.size() - 1; i > limit; i--) {
            Station station = stations.get(i);
            scoreDirector.beforeProblemFactRemoved(station);
            stations.remove(i);
            scoreDirector.afterProblemFactRemoved(station);
        }
        final int after = plan.stations().size();
        if (verbose) {
            printf("Removed %d stations (before: %d, after: %d)",
                    (before-after), before, after);
        }
    }

}
