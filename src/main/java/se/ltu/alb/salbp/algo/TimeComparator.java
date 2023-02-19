package se.ltu.alb.salbp.algo;

import java.util.Comparator;
import se.ltu.alb.salbp.model.Step;

/**
 * Compare tasks based on their time requirements.
 * Considers a task to be smaller if it takes less time.
 * @author Christoffer Fink
 */
public class TimeComparator implements Comparator<Step> {

    /**
     * {@code s1 < s2} if {@code time(s1) < time(s2)}.
     */
    @Override
    public int compare(final Step s1, final Step s2) {
        return Integer.compare(s1.time(), s2.time());
    }

}
