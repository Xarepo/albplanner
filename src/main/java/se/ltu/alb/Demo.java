package se.ltu.alb;

import java.util.*;
import ch.rfin.alb.AlbInstance;
import ch.rfin.alb.AlbBuilder;
import se.ltu.alb.salbp.model.AssemblyPlan;
import se.ltu.alb.salbp.model.Step;
import se.ltu.alb.salbp.model.Station;

/**
 * Examples of how to instantiate a problem and use a Planner to solve it.
 * @author Christoffer Fink
 */
public class Demo {

    private static List<Integer> taskTimes = List.of(
        123, 234, 345, 456, 567,    // task times for 0-4
        321, 432, 543, 654, 765     // task times for 5-9
    );
    private static List<List<Integer>> taskDependencies = List.of(
        List.of(),          // task 0 does not depend on any tasks
        List.of(),          // task 1 does not depend on any tasks
        List.of(0, 1),      // task 2 depends on 0 and 1
        List.of(2),         // task 3 depends on 2
        List.of(2),         // task 4 depends on 2
        List.of(2),         // task 5 depends on 2
        List.of(3, 4),      // task 6 depends on 3 and 4
        List.of(5),         // task 7 depends on 5
        List.of(7),         // task 8 depends on 7
        List.of(6, 8)       // task 9 depends on 6 and 8
    );
    private static int ntasks = taskTimes.size();


    // Solve a problem.
    public static void main(String[] args) throws Exception {
        SALBPlan1 planner = new SALBPlan1();

        //AssemblyPlan problem = usingAssemblyPlanDirectly();
        //AssemblyPlan problem = usingAlbInstanceDirectly();
        AssemblyPlan problem = usingAlbBuilder_alt();

        // This uses the default config (whatever that is), which currently
        // runs for 1 minute.
        //problem = planner.solve(problem);
        // These specify an XML config. Both finish in negligible time.
        problem = planner.solve(problem, "configs/salbp1-config3-first_feasible.xml");
        //problem = planner.solve(problem, "configs/salbp1-config9-unimproved.xml");
        System.out.println("Score: " + problem.score());


        // Now solve SALBP-2.

        SALBPlan2 planner2 = new SALBPlan2();
        problem = usingAlbBuilder();
        problem = planner.solve(problem, "configs/salbp2-bf.xml");
        System.out.println("Score: " + problem.score());
    }


    // NOTE: Builds a type-2 instance!
    // This is probably the easiest/simplest method.
    public static AssemblyPlan usingAlbBuilder() {
        var builder = AlbBuilder.albBuilder();

        // Loop over task IDs.
        for (int i = 0; i < taskTimes.size(); i++) {
            builder.taskTime(i, taskTimes.get(i));                  // set task time for task i
            builder.taskDependencies(i, taskDependencies.get(i));   // set dependencies for task i
        }

        // For a SALBP-1 instance, we would need a limit on the cycle time
        //builder.cycleTime(1234);
        // For a SALBP-2 instance, we would need a limit on the number of stations
        builder.stations(10);

        // Build AlbInstance and convert to AssemblyPlan.
        return AssemblyPlan.fromAlb(builder.buildValid());
    }


    // Slightly different version.
    public static AssemblyPlan usingAlbBuilder_alt() {
        var builder = AlbBuilder.albBuilder();

        // Loop over task IDs.
        for (int i = 0; i < ntasks; i++) {
            var deps = taskDependencies.get(i);
            for (final var dep : deps) {
                builder.taskDependency(i, dep);   // add one dependency for task i
            }
        }

        // Put task times in a map.
        Map<Integer, Integer> taskTimesMap = new HashMap<>();
        for (int i = 0; i < ntasks; i++) {
            taskTimesMap.put(i, taskTimes.get(i));
        }
        builder.taskTimes(taskTimesMap);    // set all task times

        // For a SALBP-1 instance, we would need a limit on the cycle time
        builder.cycleTime(1234);
        // For a SALBP-2 instance, we would need a limit on the number of stations
        //builder.stations(10);

        // Build AlbInstance and convert to AssemblyPlan.
        return AssemblyPlan.fromAlb(builder.buildValid());
    }


    public static AssemblyPlan usingAlbInstanceDirectly() {
        // Put task times in a map.
        Map<Integer, Integer> taskTimesMap = new HashMap<>();
        for (int i = 0; i < ntasks; i++) {
            taskTimesMap.put(i, taskTimes.get(i));
        }
        // Put dependencise in a map.
        Map<Integer, Collection<Integer>> taskDependenciesMap = new HashMap<>();
        for (int i = 0; i < ntasks; i++) {
            taskDependenciesMap.put(i, taskDependencies.get(i));
        }

        // Make an ALB instance and set fields directly.
        var alb = AlbInstance.albInstance()
            .tasks(ntasks)
            .cycleTime(1234)
            .taskTimes(taskTimesMap)
            .taskDependencies(taskDependenciesMap)
            .validatedOrFail(); // Optional
        return AssemblyPlan.fromAlb(alb);
    }


    // This works relatively painlessly because the tasks are numbered such
    // that i < j ⇒ i precedes j. So when creating task i, all tasks it depends
    // on have already been created.
    public static AssemblyPlan usingAssemblyPlanDirectly() {
        // Create all the tasks (Step objects).
        List<Step> tasks = new ArrayList<>();
        for (int i = 0; i < ntasks; i++) {
            var dependencies = new ArrayList<Step>();
            for (final int id : taskDependencies.get(i)) {
                dependencies.add(tasks.get(id));
            }
            // Create instance (assign to station = null)
            tasks.add(Step.step(i, dependencies, taskTimes.get(i), null));
        }
        // Create the stations. Assuming this is SALBP-1 and we use n as an
        // upper bound. (Don't know how many stations we are allowed to use
        // or might need, but we will never need more than n.)
        List<Station> stations = new ArrayList<>();
        for (int i = 0; i < ntasks; i++) {
            stations.add(Station.station(i));
        }
        // Now we can create the AssemblyPlan object.
        var plan = new AssemblyPlan(tasks, stations);
        // Still need to set the cycle time.
        plan.cycleTime(1234);

        return plan;
    }

}
