package se.ltu.alb.equip.algo;

import org.optaplanner.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import se.ltu.alb.equip.model.Task;

/**
 * Compute the distance for a swap move as the distance between the station
 * the task would move from and the station it would move to.
 * That is, the distance is {@code d = |current station nr - new station nr|}.
 * This works exactly like {@link se.ltu.alb.equip.algo.TaskStationDistance},
 * except the new station is the one the other task is currently assigned to.
 * @author Christoffer Fink
 */
public class TaskTaskDistance implements NearbyDistanceMeter<Task, Task> {

    /**
     * Controls whether tasks with equipment dependencies are ignored.
     * In that case, the distance is 0, which should effectively disable
     * the distance-based filtering for such tasks.
     * <p>
     * This may or may not work better by eliminating the
     * (potential/hypothetical?) case where a task cannot be moved to a
     * compatible station because the next one is too far away. So it ends up
     * being eliminated by the filter because of the distance, and then closer
     * stations are eliminated because it is not compatible.
     * <p>
     * AFAIK, we cannot set custom properties from the XML config.
     * So this is just for experimentation and must be modified manually.
     */
    private static final boolean ignoreWithEquip = false;

    // We could also implement a NeverSwapDependentMoveFilter by adding a
    // large distance when one of the tasks depends on the other.
    // However, this hurts performance.
    @Override
    public double getNearbyDistance(Task origin, Task dest) {
        if (ignoreWithEquip && !origin.equipmentDependencies().isEmpty()) {
            return 0.0;
        }
        return Math.abs(origin.station().number - dest.station().number);
    }

}
