package se.ltu.alb.equip.algo;

import java.util.Comparator;
import se.ltu.alb.equip.model.Task;

/**
 * Compare tasks based on their time requirements.
 * Considers a task to be smaller (easier) if it takes less time.
 * @author Christoffer Fink
 */
public class TimeComparator implements Comparator<Task> {

    /**
     * {@code s1 < s2} if {@code time(s1) < time(s2)}.
     */
    @Override
    public int compare(final Task t1, final Task t2) {
        return Integer.compare(t1.time(), t2.time());
    }

}
