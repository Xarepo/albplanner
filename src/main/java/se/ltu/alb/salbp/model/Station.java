package se.ltu.alb.salbp.model;

import org.optaplanner.core.api.domain.lookup.PlanningId;

/**
 * An assembly line station.
 * Stations are immutable.
 * @author Christoffer Fink
 */
public class Station implements Comparable<Station> {

    @PlanningId
    public final int number;

    private Station(final int number) {
        this.number = number;
    }

    public static Station station(final int number) {
        return new Station(number);
    }

    /**
     * The number of this station along the assembly line.
     * The reason stations have "numbers" rather than "IDs" is that they
     * carry information about their position in time/space.
     * Hence they can be used to compare stations to see which one
     * "comes before" another, and determine if task assignments satisfy
     * precedence constraints.
     */
    public int number() {
        return number;
    }

    @Override
    public int compareTo(final Station that) {
        return Integer.compare(this.number, that.number);
    }

    @Override
    public String toString() {
        return "station " + number;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (getClass() != o.getClass()) {
            return false;
        }
        final Station that = (Station) o;
        return this.number == that.number;
    }

    @Override
    public int hashCode() {
        return number;
    }

}
