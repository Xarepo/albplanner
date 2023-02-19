package ch.rfin.alb;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AlbParserTest {

    @Test
    public void parsing_s1_n20_1() {
        final AlbInstance instance = AlbStrings.s1_n20_1_inst;
        final AlbInstance parsed = Alb.parseString(AlbStrings.s1_n20_1_str);
        assertEquals(instance.tasks(), parsed.tasks());
        assertEquals(instance.cycleTime(), parsed.cycleTime());
        assertEquals(instance.taskTimes(), parsed.taskTimes());
        assertEquals(instance.taskDependencies(), parsed.taskDependencies());
        // XXX: This works because the property is null!
        // Does it even make sense to check this!?
        assertEquals(instance.<String>prop("<order strength>"), parsed.<String>prop("<order strength>"));
    }

}
