package se;

import ch.rfin.alb.Alb;
import se.ltu.alb.SALBPlan1;
import se.ltu.alb.salbp.model.AssemblyPlan;

import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class main {
    public static void main(String[] args) throws IOException {
        // AssemblyPlan plan = AssemblyPlan.fromAlb(Alb.parseString(s1_n20_1_str));

        SALBPlan1 planner = new SALBPlan1();

        // System.out.println("THE SOLUTION IS" + planner.solve(plan));

        HttpServer server = HttpServer.create(new InetSocketAddress(3001), 0);
        server.createContext("/salbp", (exchange -> {

            if (exchange.getRequestMethod().equals("POST")) {
                String plan = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
                String responseText = planner.solve(AssemblyPlan.fromAlb(Alb.parseString(plan))).toString();
                exchange.sendResponseHeaders(200, responseText.getBytes().length);
                OutputStream output = exchange.getResponseBody();
                output.write(responseText.getBytes());
                output.flush();
            } else {
                exchange.sendResponseHeaders(405, -1);// 405 Method Not Allowed
            }
            exchange.close();
        }));
        server.setExecutor(null); // creates a default executor
        server.start();
    }
}
