package se.ltu.alb.salbp.algo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import ch.rfin.alb.AlbInstance;

public class RandomType1Test {

    @Test
    public void trivial() {
        var solver = new RandomType1().seed(321);
        var problem = AlbInstance.albInstance(1, Map.of(0, 123))
            .taskDependencies(Map.of(0, List.of()))
            .cycleTime(1000);
        var solution = solver.randomFeasible(problem);
        assertEquals(solution, Map.of(0, 0));
    }

}
