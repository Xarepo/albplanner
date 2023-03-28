package se.ltu.alb;

import ch.rfin.alb.Alb;
import ch.rfin.alb.AlbBuilder;
import ch.rfin.alb.AlbInstance;
import se.ltu.alb.salbp.model.AssemblyPlan;

import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.json.*;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Entering API");

        SALBPlan1 planner = new SALBPlan1();
        
        HttpServer server = HttpServer.create(new InetSocketAddress(3001), 0);
        server.createContext("/salbp", (exchange -> {

            if (exchange.getRequestMethod().equals("POST")) {
                try {
                String plan = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
                JSONObject jsonObject = new JSONObject(plan);
                List<Integer> taskTimes = Main.castFromJSONArray(jsonObject.getJSONArray("taskTimes"));
                List<List<Integer>> taskDependencies = new ArrayList<List<Integer>>();
                JSONArray tempTaskDependencies = jsonObject.getJSONArray("taskDependencies");
                for (int i = 0 ; i < tempTaskDependencies.length(); i++) {
                    JSONArray subArray = tempTaskDependencies.getJSONArray(i);
                    List<Integer> addArray;
                    try {
                        if (subArray.length() == 0) {
                            addArray = new ArrayList<>();
                        } else {
                            addArray = Main.castFromJSONArray(subArray);
                        }
                        taskDependencies.add(addArray);
                    } catch (Exception e) {
                        // Handle the exception as appropriate
                        System.err.println("Error adding subArray: " + subArray);
                        e.printStackTrace();
                    }
                }

                AssemblyPlan problem = Main.usingAlbBuilder(taskTimes, taskDependencies, jsonObject.getInt("cycleTime"), jsonObject.getInt("noStations"));

                JSONObject responseText = getResult(planner.solve(problem));

                exchange.sendResponseHeaders(200, responseText.toString().getBytes().length);
                OutputStream output = exchange.getResponseBody();
                output.write(responseText.toString().getBytes());
                output.flush();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            } else {
                exchange.sendResponseHeaders(405, -1);// 405 Method Not Allowed
            }
            exchange.close();
        }));
        server.setExecutor(null); // creates a default executor
        server.start();
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