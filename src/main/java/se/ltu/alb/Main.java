package se.ltu.alb;

import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.json.*;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Entering API");
        
        HttpServer server = HttpServer.create(new InetSocketAddress(3001), 0);
        server.createContext("/salbp", (exchange -> {

            if (exchange.getRequestMethod().equals("POST")) {
                try {
                    String plan = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
                    SolverThread thread = new SolverThread(new JSONObject(plan));
                    
                    thread.start();

                    exchange.sendResponseHeaders(200, "Started solving".getBytes().length);
                    OutputStream output = exchange.getResponseBody();
                    output.write("Started solving".getBytes());
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

}