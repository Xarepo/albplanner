package se.ltu.alb;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.OptionalInt;
import static java.util.stream.Collectors.toList;
import ch.rfin.alb.Alb;
import ch.rfin.alb.AlbInstance;
import se.ltu.alb.equip.model.AssemblyPlan;
import se.ltu.alb.equip.test.GeneratorType2A;

public class EqALBPlan2aTest extends AbstractPlannerTest<AssemblyPlan> {

    @Override
    public EqALBPlan2a planner() {
        return new EqALBPlan2a();
    }

    @Override
    public List<AssemblyPlan> problemInstances() {
        var param = GeneratorType2A.defaultParameters(1234);
        var generator = GeneratorType2A.of(param);
        return List.of(
            SALBPlan1Test.s1_n20_1_str
        ).stream()
            .map(Alb::parseString)
            .map(AlbInstance::zeroBased)
            .map(generator::convert)
            .map(raw -> raw.cycleTime(OptionalInt.empty()))
            .map(AssemblyPlan::fromAlb)
            .collect(toList());
    }

    @Override
    public List<String> solverConfigs() {
        return List.of(
            "configs/eqalbp2a/easy_ff_feasible.xml", // TODO: rename to simple or trivial
            "configs/eqalbp2a/advanced.xml"
        );
    }

    @Override
    public void checkSolution(AssemblyPlan solution) {
        assertTrue(solution.score().isFeasible());
    }

}
