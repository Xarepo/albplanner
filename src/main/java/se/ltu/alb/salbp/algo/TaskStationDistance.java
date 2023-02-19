package se.ltu.alb.salbp.algo;

import org.optaplanner.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import se.ltu.alb.salbp.model.Step;
import se.ltu.alb.salbp.model.Station;

/**
 * Compute the distance for a change move as the distance between the station
 * the task would move from and the station it would move to.
 * That is, the distance is {@code d = |current station nr - new station nr|}.
 * @author Christoffer Fink
 */
public class TaskStationDistance implements NearbyDistanceMeter<Step, Station> {

    @Override
    public double getNearbyDistance(Step task, Station station) {
        return Math.abs(task.station().number - station.number);
    }

}
