package ch.rfin.alb;

import java.util.List;
import java.util.Set;
import java.util.Collection;
import static ch.rfin.util.Pairs.enumerate;
import static ch.rfin.util.Pairs.pairsToMap;
import static ch.rfin.util.Pairs.map_1;

/**
 * Contains the contents of ALB files as Strings.
 * @author Christoffer Fink
 */
public class AlbStrings {

    /**
     * This is instance 1 from the small (n=20) instances of the
     * "standard dataset" (by Otto et al).
     */
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

    public static final AlbInstance s1_n20_1_inst = Alb
        .instance()
        .tasks(20)
        .cycleTime(1000)
        .taskTimes(enumerate(List.of(
            142, 34, 140, 214, 121, 279, 50, 282, 129, 175,
            97, 132, 107, 132, 69, 169, 73, 231, 120, 186))
            .map(map_1(i -> i+1))   // Count from 1 instead of 0.
            .collect(pairsToMap()))
        .taskDependencies(enumerate(List.<Collection<Integer>>of(
            Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),              // 1-5
            Set.of(1), Set.of(2), Set.of(4), Set.of(5), Set.of(6),         // 6-10
            Set.of(7), Set.of(8), Set.of(10, 11), Set.of(12), Set.of(12),  // 11-15
            Set.of(13), Set.of(13), Set.of(13), Set.of(15), Set.of(14)))   // 16-20
            .map(map_1(i -> i+1))   // Count from 1 instead of 0.
            .collect(pairsToMap()));

}
