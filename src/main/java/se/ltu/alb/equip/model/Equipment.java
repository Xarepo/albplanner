package se.ltu.alb.equip.model;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.entity.PlanningPin;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.api.domain.solution.ProblemFactProperty;

/**
 * An individual piece of equipment.
 * Each equipment has one type, but there may be many equipments of the same type.
 * An equipment is pinned to a station if and only if the move cost is -1.
 * @author Christoffer Fink
 */
@PlanningEntity
public class Equipment {

    // Problem facts.
    private int id;
    private int type;
    private int price;
    private int moveCost;
    private Station initiallyAt;
    private boolean pinned;   // Maybe non-final in the future.
    @PlanningVariable(valueRangeProviderRefs = {"stations"}, nullable = true)
    private Station station;
    // Temporary OptaPlanner bug workaround.
    public boolean dummy = false;

    /** OptaPlanner needs a no-arg constructor. */
    private Equipment() {
    }

    private Equipment(int id, int type, int price, int moveCost, Station station) {
        this.id = id;
        this.type = type;
        this.price = price;
        this.moveCost = moveCost;
        this.initiallyAt = station;
        this.station = station;
        this.pinned = moveCost <= -1;
    }

    public static Equipment dummy(int id, int type, Station station) {
        var eq = unpinned(id, type, 0, station);
        eq.dummy = true;
        return eq;
    }

    /**
     * Make an equipment that is pinned to the given station.
     * Because the equipment is pinned to a station, it is assumed to exist.
     * So the price defaults to 0. (No need to buy something you already own.)
     * More generally, we could say that the price is X, but it's a known
     * constant and is not affected by planning. So including it in any
     * calculations is not meaningful.
     */
    public static Equipment pinned(int id, int type, Station station) {
        var eq = new Equipment(id, type, 0, -1, station);
        assert eq.pinned();
        return eq;
    }

    /**
     * Make a movable equipment that is initially equipped at the given station.
     * Since the equipment is assigned to a station (although unpinned), the
     * equipment must already exist, just like the pinned case.
     * So the price defaults to 0.
     * However, since it is not pinned, it can be moved, and we expect some
     * (possibly 0) move cost.
     */
    public static Equipment unpinned(int id, int type, int moveCost, Station station) {
        assert moveCost >= 0 : "Move cost must be non-negative";
        var eq = new Equipment(id, type, 0, moveCost, station);
        assert !eq.pinned();
        return eq;
    }

    /**
     * Make an equipment that has not yet been equipped at any station.
     * The equipment may or may not already exist, and hence may or may not
     * need to be purchased. So the price can be any non-negative value.
     * Since the equippment is not installed at any station, it does not
     * need to be moved from one station to another, and so the move cost
     * is zero.
     * <p>
     * Any initial installation cost will have to be included in the price.
     */
    public static Equipment unequipped(int id, int type, int price) {
        assert price >= 0 : "Price must be non-negative";
        var eq = new Equipment(id, type, price, 0, null);
        assert !eq.pinned();
        return eq;
    }

    /**
     * The station that is equipped with this equipment.
     * For some problems, some equipment may be permanently equipped at a fixed
     * station.
     */
    public Station equippedAt() {
        return station();
    }

    public Station station() {
        return station;
    }

    public Equipment equipAt(Station station) {
        return station(station);
    }

    public Equipment station(Station station) {
        assert !pinned || (station == initiallyAt);
        this.station = station;
        return this;
    }

    /**
     * The ID identifying this particular equipment.
     */
    @ProblemFactProperty
    @PlanningId
    public int id() {
        return id;
    }

    /**
     * The equipment type is what determines which stations satisfy the
     * equipment requirements of tasks. Tasks depend on types of equipment
     * being available, not on individual equipment (instances).
     */
    @ProblemFactProperty
    public int type() {
        return type;
    }

    /**
     * The literal price to initially acquire the equipment.
     * This is only relevant for type-1 planning.
     */
    @ProblemFactProperty
    public int price() {
        return price;
    }

    /**
     * If movable (not pinned), the cost associated with moving this equipment
     * to a different station.
     * The cost need not necessarily represent a monetary value. It could be
     * something more abstract such as time or "effort".
     * <p>
     * This is used to support a mix between type 1 and type 2 planning, or
     * simply type 1 re-planning, where an assembly line is not build from
     * scratch, and efficiency gains from modifying the line must be traded off
     * against the cost.
     * <p>
     * The moveCost is -1 if and only if the equipment is pinned.
     * <p>
     * <em>Note that this is not intended for real-time planning.</em>
     */
    @ProblemFactProperty
    public int moveCost() {
        return moveCost;
    }

    /**
     * The station (if any) where this equipment is equipped.
     */
    @ProblemFactProperty
    public Station initiallyAt() {
        return initiallyAt;
    }

    /**
     * A pinned equipment is fixed to a certain station.
     * The equipment is pinned if and only if the moveCost is -1.
     */
    @ProblemFactProperty
    @PlanningPin
    public boolean pinned() {
        return pinned;
    }


    // Note: CANNOT do this == obj, because OptaPlanner clones entities!
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Equipment other = (Equipment) obj;
        return this.id == other.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return String
            .format("Equipment(id=%d, type=%d, at=%s, pinned=%s)",
                    id, type, station, pinned);
    }

}
