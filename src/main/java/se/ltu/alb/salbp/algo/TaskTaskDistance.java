package se.ltu.alb.salbp.algo;

import org.optaplanner.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import se.ltu.alb.salbp.model.Step;

/**
 * Compute the distance for a swap move as the distance between the station
 * the task would move from and the station it would move to.
 * That is, the distance is {@code d = |current station nr - new station nr|}.
 * This works exactly like {@link se.ltu.alb.salbp.algo.TaskStationDistance},
 * except the new station is the one the other task is currently assigned to.
 * @author Christoffer Fink
 */
public class TaskTaskDistance implements NearbyDistanceMeter<Step, Step> {

    @Override
    public double getNearbyDistance(Step origin, Step destination) {
        return Math.abs(origin.station().number - destination.station().number);
    }

}
