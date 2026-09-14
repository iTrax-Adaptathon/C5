package com.example.demo;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AppServer {

    // Simple in-memory thread-safe database for rapid demo/testing
    private static final List<Map<String, Object>> submissions = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, Double> eloStore = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        // Seed dummy data
        seedInitialData();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/api/submissions", new SubmissionsHandler());
        server.createContext("/api/challenges/", new ChallengeHandler());

        server.setExecutor(null);
        System.out.println("🚀 Backend API Server running on http://localhost:8080");
        server.start();
    }

    // --- Helper to handle CORS ---
    private static void setCorsHeaders(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        setCorsHeaders(exchange);
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    // --- 1. POST /api/submissions ---
    static class SubmissionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                // Quick JSON parsing extracted from request body
                String userId = extractJsonVal(body, "userId");
                String challengeId = extractJsonVal(body, "challengeId");
                String contentUrl = extractJsonVal(body, "contentUrl");
                int tier = Integer.parseInt(extractJsonValOrDefault(body, "verificationTier", "0"));

                Map<String, Object> entry = new HashMap<>();
                entry.put("id", "sub_" + (submissions.size() + 101));
                entry.put("user", userId.isEmpty() ? "anonymous" : userId);
                entry.put("challengeId", challengeId);
                entry.put("contentUrl", contentUrl);
                entry.put("tier", tier);
                entry.put("label", getTierLabel(tier));
                entry.put("score", "85.0%"); // Placeholder percentile rank logic trigger

                submissions.add(entry);
                eloStore.putIfAbsent((String) entry.get("id"), 1200.0);

                sendResponse(exchange, 201, "{\"status\":\"success\",\"message\":\"Entry recorded\"}");
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // --- Handles /api/challenges/:id/leaderboard, /pair, and /vote ---
    static class ChallengeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath(); // e.g., /api/challenges/5k-run/leaderboard
            String query = exchange.getRequestURI().getQuery();
            String method = exchange.getRequestMethod();

            // 2. GET /api/challenges/:id/leaderboard
            if (path.endsWith("/leaderboard") && "GET".equalsIgnoreCase(method)) {
                int minTier = 0;
                if (query != null && query.contains("min_tier=")) {
                    try {
                        minTier = Integer.parseInt(query.split("min_tier=")[1].split("&")[0]);
                    } catch (Exception ignored) {}
                }

                StringBuilder json = new StringBuilder("[");
                int count = 0;
                for (Map<String, Object> sub : submissions) {
                    int subTier = (int) sub.get("tier");
                    if (subTier >= minTier) {
                        if (count > 0) json.append(",");
                        json.append(String.format(
                            "{\"user\":\"%s\",\"score\":\"%s\",\"tier\":%d,\"label\":\"%s\"}",
                            sub.get("user"), sub.get("score"), subTier, sub.get("label")
                        ));
                        count++;
                    }
                }
                json.append("]");
                sendResponse(exchange, 200, json.toString());

            // 3. GET /api/challenges/:id/pair
            } else if (path.endsWith("/pair") && "GET".equalsIgnoreCase(method)) {
                if (submissions.size() < 2) {
                    sendResponse(exchange, 400, "{\"error\":\"Not enough submissions for arena\"}");
                    return;
                }
                List<Map<String, Object>> shuffled = new ArrayList<>(submissions);
                Collections.shuffle(shuffled);
                Map<String, Object> a = shuffled.get(0);
                Map<String, Object> b = shuffled.get(1);

                String response = String.format(
                    "{\"submissionA\":{\"id\":\"%s\",\"user\":\"%s\",\"url\":\"%s\",\"desc\":\"Submission A\"}," +
                     "\"submissionB\":{\"id\":\"%s\",\"user\":\"%s\",\"url\":\"%s\",\"desc\":\"Submission B\"}}",
                    a.get("id"), a.get("user"), a.get("contentUrl"),
                    b.get("id"), b.get("user"), b.get("contentUrl")
                );
                sendResponse(exchange, 200, response);

            // 4. POST /api/challenges/:id/vote
            } else if (path.endsWith("/vote") && "POST".equalsIgnoreCase(method)) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                String winnerId = extractJsonVal(body, "winnerId");

                // Recalibrate using scoring engine
                if (eloStore.containsKey(winnerId)) {
                    double currentElo = eloStore.get(winnerId);
                    eloStore.put(winnerId, currentElo + 16.0); // Simple Elo increment demo call
                }

                sendResponse(exchange, 200, "{\"status\":\"success\",\"winner\":\"" + winnerId + "\"}");
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        }
    }

    // --- Utilities ---
    private static String getTierLabel(int tier) {
        switch (tier) {
            case 4: return "Moderator";
            case 3: return "API Verified";
            case 2: return "Peer Verified";
            case 1: return "Evidence Attached";
            default: return "Self Reported";
        }
    }

    private static String extractJsonVal(String json, String key) {
        return extractJsonValOrDefault(json, key, "");
    }

    private static String extractJsonValOrDefault(String json, String key, String defaultVal) {
        if (!json.contains("\"" + key + "\"")) return defaultVal;
        try {
            String sub = json.split("\"" + key + "\"\\s*:\\s*")[1];
            if (sub.startsWith("\"")) {
                return sub.split("\"")[1];
            } else {
                return sub.split("[,}]")[0].trim();
            }
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private static void seedInitialData() {
        submissions.add(Map.of("id", "sub_101", "user", "dev_satoshi", "tier", 4, "label", "Moderator", "score", "99.2%", "contentUrl", "https://picsum.photos/seed/art1/400/300"));
        submissions.add(Map.of("id", "sub_102", "user", "quantum_coder", "tier", 2, "label", "Peer Verified", "score", "88.5%", "contentUrl", "https://picsum.photos/seed/art2/400/300"));
        submissions.add(Map.of("id", "sub_103", "user", "pixel_hunter", "tier", 2, "label", "Peer Verified", "score", "64.1%", "contentUrl", "https://picsum.photos/seed/art3/400/300"));
        submissions.add(Map.of("id", "sub_104", "user", "newbie_farmer", "tier", 0, "label", "Self Reported", "score", "42.0%", "contentUrl", "https://picsum.photos/seed/art4/400/300"));
        eloStore.put("sub_101", 1400.0);
        eloStore.put("sub_102", 1250.0);
        eloStore.put("sub_103", 1100.0);
        eloStore.put("sub_104", 1000.0);
    }
}
