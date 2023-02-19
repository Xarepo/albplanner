package se.ltu.alb;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import se.ltu.alb.salbp.model.AssemblyPlan;
import ch.rfin.alb.Alb;

public class SALBPlan1Test extends AbstractPlannerTest<AssemblyPlan> {

    @Override
    public SALBPlan1 planner() {
        return new SALBPlan1();
    }

    @Override
    public List<AssemblyPlan> problemInstances() {
        return List.of(
            AssemblyPlan.fromAlb(Alb.parseString(s1_n20_1_str))
        );
    }

    @Override
    public List<String> solverConfigs() {
        return List.of(
            "configs/salbp1/easy_ff_feasible.xml", // TODO: rename to simple or trivial
            "configs/salbp1/advanced.xml"
        );
    }

    @Override
    public void checkSolution(AssemblyPlan solution) {
        assertTrue(solution.score().isFeasible());
    }

    public static final String s1_n20_1_str =
        """
        <number of tasks>
        20

        <cycle time>
        1000

        <order strength>
        0,268


        <task times>
        1 142
        2 34
        3 140
        4 214
        5 121
        6 279
        7 50
        8 282
        9 129
        10 175
        11 97
        12 132
        13 107
        14 132
        15 69
        16 169
        17 73
        18 231
        19 120
        20 186

        <precedence relations>
        1,6
        2,7
        4,8
        5,9
        6,10
        7,11
        8,12
        10,13
        11,13
        12,14
        12,15
        13,16
        13,17
        13,18
        14,20
        15,19

        <end>
        """;

}
