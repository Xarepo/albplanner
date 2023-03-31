package se.ltu.alb;

import ch.rfin.alb.AlbBuilder;
import se.ltu.alb.salbp.model.AssemblyPlan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.*;

public class SolverThread extends Thread {


    private JSONObject problemObject;


    public SolverThread(JSONObject problemObject) {
        this.problemObject = problemObject;
    }


    public void run() {
        List<Integer> taskTimes = SolverThread.castFromJSONArray(this.problemObject.getJSONArray("taskTimes"));
        List<List<Integer>> taskDependencies = new ArrayList<List<Integer>>();
        JSONArray tempTaskDependencies = this.problemObject.getJSONArray("taskDependencies");
        for (int i = 0 ; i < tempTaskDependencies.length(); i++) {
            JSONArray subArray = tempTaskDependencies.getJSONArray(i);
            List<Integer> addArray;
            try {
                if (subArray.length() == 0) {
                    addArray = new ArrayList<>();
                } else {
                    addArray = SolverThread.castFromJSONArray(subArray);
                }
                taskDependencies.add(addArray);
            } catch (Exception e) {
                // Handle the exception as appropriate
                System.err.println("Error adding subArray: " + subArray);
                e.printStackTrace();
            }
        }

        int cycleTime = this.problemObject.getInt("cycleTime");
        int noStations = this.problemObject.getInt("noStations");

        AssemblyPlan problem = SolverThread.usingAlbBuilder(taskTimes, taskDependencies, cycleTime, noStations);

        JSONObject taskAssignments;
        Integer finalCycleTime;
        Integer finalNumStations;
        if (cycleTime == 0) {
            SALBPlan2 planner = new SALBPlan2();
            AssemblyPlan solution = planner.solve(problem);
            finalCycleTime = solution.cycleTime().orElse(0);
            finalNumStations = solution.stations().size();
            taskAssignments = getResult(solution);
        } else {
            SALBPlan1 planner = new SALBPlan1();
            AssemblyPlan solution = planner.solve(problem);
            finalCycleTime = solution.cycleTime().orElse(0);
            finalNumStations = solution.stations().size();
            taskAssignments = getResult(solution);
        }

        try {
            URL url = new URL("http://albepdess-backend:5001/graphql");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
      
            String input = "{ \"query\": \"" +
                    "mutation{" +
                    "createSolution(" +
                        "problemId: \\\"" + this.problemObject.getString("problemId") + "\\\", " +
                        "taskAssignments: \\\"" + taskAssignments.toString().replace("\"", "\\\\\\\"") + "\\\", " +
                        "cycleTime: " + finalCycleTime + ", " +
                        "numStations: " + finalNumStations +
                        ") {" +
                            "__typename," +
                            "... on SolutionType {" +
                                "name" +
                            "}" +
                        "}" +
                    "}\" }";
            OutputStream os = conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));
            String output;
            System.out.println("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                System.out.println(output);
            }
            conn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static JSONObject getResult(AssemblyPlan solution) {
        JSONObject result = new JSONObject();
        for (final var task : solution.tasks()) {
            result.put(Integer.toString(task.id()), task.station().toString());
        }
        return result;
    }

    public static List<Integer> castFromJSONArray(JSONArray array) {
        List<Integer> result = new ArrayList<>(array.length());
        for (int i = 0 ; i < array.length(); i++) {
            result.add(array.getInt(i));
        }
        return result;
    }

    // This is probably the easiest/simplest method.
    public static AssemblyPlan usingAlbBuilder(List<Integer> taskTimes, List<List<Integer>> taskDependencies, int cycleTime, int stations) {
        var builder = AlbBuilder.albBuilder();

        // Loop over task IDs.
        for (int i = 0; i < taskTimes.size(); i++) {
            builder.taskTime(i, taskTimes.get(i));                  // set task time for task i
            builder.taskDependencies(i, taskDependencies.get(i));   // set dependencies for task i
        }
        if (cycleTime == 0) {
            // For a SALBP-2 instance, we would need a limit on the number of stations
            builder.stations(stations);
        } else if (stations == 0) {
            // For a SALBP-1 instance, we would need a limit on the cycle time
            builder.cycleTime(cycleTime);
        }

        // Build AlbInstance and convert to AssemblyPlan.
        return AssemblyPlan.fromAlb(builder.buildValid());
    }

}