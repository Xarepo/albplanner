package se.ltu.alb.equip.algo;

import java.util.*;
import org.optaplanner.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import org.optaplanner.core.impl.heuristic.selector.move.generic.ChangeMove;
import org.optaplanner.core.api.score.director.ScoreDirector;
import se.ltu.alb.equip.model.*;
import ch.rfin.util.Bits;
import ch.rfin.util.ImmutableBits;
import static ch.rfin.util.Exceptions.assertion;

/**
 * Value selection filter that only allows tasks to be assigned to stations 
 * with the required equipment.
 * <p>
 * For performance reasons, some problem facts are cached the first time
 * the {@code accept()} method is called.
 * This includes necessarily fixed facts such as the the equipment required by
 * tasks.  But it also includes how stations are equipped, which only remains
 * fixed when all equipment is pinned.
 * So <strong>this filter only works for pure type 2 problem!</strong>
 * @author Christoffer Fink
 */
public class CompatibleStationFilter implements SelectionFilter<AssemblyPlan, ChangeMove<AssemblyPlan>> {

    private Map<Integer, Bits> equipmentDependencies = new HashMap<>();
    private Map<Integer, Bits> stationEquipment = new HashMap<>();
    private boolean initialized = false;

    /**
     * Caches both equipment requirements and installed equipment.
     */
    private void init(AssemblyPlan plan) {
        if (initialized) {
            return;
        }
        for (final var task : plan.tasks()) {
            equipmentDependencies.put(task.id(), Bits.bits(task.equipmentDependencies()));
        }
        for (final var equip : plan.equipment()) {
            assertion(equip.pinned(), "This filter can only be used with pinned equipment");
            final var station = equip.station();
            if (station == null) {
                continue;
            }
            final var equippedAt = stationEquipment.getOrDefault(station.number(), ImmutableBits.empty());
            stationEquipment.put(station.number(), equippedAt.set(equip.type()));
        }
        initialized = true;
    }

    @Override
    public boolean accept(ScoreDirector<AssemblyPlan> director, ChangeMove<AssemblyPlan> move) {
        return accept(director.getWorkingSolution(), move);
    }

    public boolean accept(AssemblyPlan plan, ChangeMove<AssemblyPlan> move) {
        init(plan);
        final Task task = (Task) move.getEntity();
        if (task.equipmentDependencies().isEmpty()) {
            return true;
        }
        final Station station = (Station) move.getToPlanningValue();
        final var accept = equipmentDependencies.get(task.id())
            .subsetOf(stationEquipment.getOrDefault(station.number(), ImmutableBits.empty()));
        return accept;
    }

}
