package se.ltu.alb.equip;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Function;
import ch.rfin.alb.AlbInstance;

public class ReaderTest {

    Function<AlbInstance,AlbInstance> nop = Function.identity();

    // TODO: Invalid conditions to test:
    // - total < variations
    // - total < originals
    // - originals > 525 (whether explicitly or implicitly specified)
    // - etc ...

    @Test
    public void read_converted_total_variations() {
        read_converted_total_variations(5, 1);
        read_converted_total_variations(5, 2);
        read_converted_total_variations(5, 5);
    }

    private void read_converted_total_variations(int total, int variations) {
        var reader = Util.Reader.of(nop)
            .totalProblems(total)
            .variations(variations);
        // Note: can't count distinct because of the NOP converter.
        // (All variations will be the same.)
        var instances = reader.read().count();
        assertEquals(total, instances);
    }

    @Test
    public void readOriginals_first_step_originals() {
        readOriginals_first_step_originals(1, 1, 1);
        readOriginals_first_step_originals(1, 1, 10);
        readOriginals_first_step_originals(1, 50, 10);
        readOriginals_first_step_originals(25, 100, 5);
        readOriginals_first_step_originals(525, 1, 1);
        readOriginals_first_step_originals(524, 1, 2);
        readOriginals_first_step_originals(523, 2, 1);
        // XXX: These fail silently by returning fewer instances than requested.
        readOriginals_first_step_originals(525, 2, 2, 1);
        readOriginals_first_step_originals(526, 2, 2, 0);
    }

    private void readOriginals_first_step_originals(int first, int step, int orig) {
        readOriginals_first_step_originals(first, step, orig, orig);
    }

    private void readOriginals_first_step_originals(int first, int step, int orig, int expected) {
        var reader = Util.Reader.reader()
            .first(first)
            .step(step)
            .originalProblems(orig);
        var instances = reader.readOriginals().distinct().count();
        assertEquals(expected, instances);
    }

    @Test
    public void readOriginals_first_originals() {
        readOriginals_first_originals(1, 1);
        readOriginals_first_originals(1, 10);
        readOriginals_first_originals(25, 5);
        readOriginals_first_originals(525, 1);
        readOriginals_first_originals(524, 2);
        readOriginals_first_originals(523, 1);
        readOriginals_first_originals(1, 525);
    }

    private void readOriginals_first_originals(int first, int orig) {
        readOriginals_first_originals(first, orig, orig);
    }

    private void readOriginals_first_originals(int first, int orig, int expected) {
        var reader = Util.Reader.reader()
            .first(first)
            .originalProblems(orig);
        var instances = reader.readOriginals().distinct().count();
        assertEquals(orig, instances);
    }

    private void assertReadOriginalsCount(Util.Reader reader, int expected) {
        var count = reader.readOriginals().distinct().count();
        assertEquals(expected, count);
    }

}
