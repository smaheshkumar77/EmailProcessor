package com.mycompany.transform;





import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Streamer {
    private final String streamUri;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private WebSocket webSocket;
    private ExecutorService executor;
    private Consumer<JsonNode> messageHandler;

    private volatile boolean closed = false;

    public Streamer(String streamUri, TokenService tokenManager, Consumer<JsonNode> messageHandler) {
        this.streamUri = streamUri;
        this.tokenService = tokenManager;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
        this.messageHandler = messageHandler;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public CompletableFuture<Void> connect() throws Exception {
        String token = tokenService.getAccessToken();
        URI uriWithToken = new URI(streamUri + "?access_token=" + token);  // or however Schwab wants to pass auth

        CompletableFuture<Void> cf = httpClient.newWebSocketBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .buildAsync(uriWithToken, new Listener() {
                @Override
                public void onOpen(WebSocket ws) {
                    webSocket = ws;
                    System.out.println("WebSocket opened");
                    Listener.super.onOpen(ws);  
                }

                @Override
                public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                    // handle text frame
                    try {
                        JsonNode node = objectMapper.readTree(data.toString());
                        messageHandler.accept(node);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return Listener.super.onText(ws, data, last);
                }

                @Override
                public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
                    // if Schwab ever sends binary frames; could skip or handle
                    return Listener.super.onBinary(ws, data, last);
                }

                @Override
                public CompletionStage<?> onPing(WebSocket ws, ByteBuffer message) {
                    ws.sendPong(message);
                    return Listener.super.onPing(ws, message);
                }

                @Override
                public CompletionStage<?> onError(WebSocket ws, Throwable error) {
                    error.printStackTrace();
                    // maybe attempt reconnect?
                    return Listener.super.onError(ws, error);
                }

                @Override
                public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
                    System.out.println("WebSocket closed, code=" + statusCode + ", reason=" + reason);
                    closed = true;
                    return Listener.super.onClose(ws, statusCode, reason);
                }
            })
            .thenAccept(ws -> {
                // maybe send subscription message
                sendSubscription();
            });

        return cf;
    }

    public void sendText(String message) {
        if (webSocket != null) {
            webSocket.sendText(message, true);
        } else {
            throw new IllegalStateException("WebSocket is not open");
        }
    }

    private void sendSubscription() {
        // Build the subscription JSON, according to Schwab API
        // For example:
        Map<String, Object> sub = Map.of(
            "type", "subscribe",
            "channels", new String[]{"quotes", "trades"}  // example
        );
        try {
            String msg = objectMapper.writeValueAsString(sub);
            sendText(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CompletableFuture<Void> disconnect() {
        if (webSocket != null) {
            return webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Bye")
                .thenRun(() -> {
                    closed = true;
                });
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    public boolean isClosed() {
        return closed;
    }

    // Optionally: reconnect logic
    public void reconnectWithBackoff(long initialDelayMillis, int maxRetries) {
        executor.submit(() -> {
            long delay = initialDelayMillis;
            int attempts = 0;
            while (!closed && attempts < maxRetries) {
                try {
                    connect().join();
                    if (!closed) {
                        // success, exit loop
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                attempts++;
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                delay *= 2; // exponential backoff
            }
        });
    }
}
