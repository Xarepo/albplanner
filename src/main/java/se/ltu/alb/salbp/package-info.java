/**
 * This package contains code for solving the Arbitrary Assembly Line
 * Balancing Problem -- SALBP.
 * "Simple" refers to the configuration of the assembly line: it's a linear
 * sequence of fixed stations, and the work piece advances from one station to
 * the next at a fixed cycle time.  There are no assignments of workers or
 * resources/tools.  There is also no parallelism (disregarding of course the
 * parallelism inherent in the assembly line concept).
 * <p>
 * The SALB problem traditionally comes in two variants: type 1 and 2.
 * In type 1 planning, a target cycle time is given, and the goal is to minimize
 * the number of stations on the assembly line.
 * In type 2 planning, the stations are given, and the goal is to minimize the
 * cycle time.
 * <p>
 * Because both problems are almost identical, except for the goal, they can
 * both be represented by the same model. Hence this package contains the
 * shared model classes for the two versions.
 * Because the goals differ, so do the score calculations. However, the hard
 * constraints are different. So this package also contains some utilities for
 * computing scores, such as a collection of basic score features.
 *
 * <h2>SALBP-1</h2>
 * The {@code se.ltu.alb.salbp1} package contains code for solving SALBP-1 (the
 * Simple Assembly Line Balancing Problem type 1).
 * "Type 1" refers to the version of the problem where the cycle time is given,
 * and the goal is to distribute the work tasks to the smallest number of
 * stations possible.  In other words, the goal is to make the assembly line as
 * short as possible, in terms of the number of stations.
 * <p>
 * The only thing distinguishing SALBP-1 from the bin packing problem is that
 * tasks generally have dependencies on other tasks that need to be completed
 * before work can start on the dependent tasks. This restricts the possible
 * orders in which the tasks can be allocated to stations.
 *
 * @see se.ltu.alb.equip
 * @author Christoffer Fink
 */
package se.ltu.alb.salbp;
