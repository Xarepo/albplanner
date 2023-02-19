package se.ltu.alb.equip.algo;

import java.util.Comparator;
import se.ltu.alb.equip.model.Station;

// FIXME: need to check the relationship between "less" than and "stronger than"!

/**
 * Compare station strength based on their IDs.
 * Considers a station to be stronger if it appears earlier in the sequence.
 * @author Christoffer Fink
 */
public class StationIdComparator implements Comparator<Station> {

    /**
     * {@code s1 < s2} if {@code |number(s1)| < |number(s2)|}.
     */
    @Override
    public int compare(final Station s1, final Station s2) {
        return Integer.compare(s1.number(), s2.number());
    }

}
