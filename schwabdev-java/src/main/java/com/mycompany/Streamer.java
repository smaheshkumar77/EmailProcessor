package com.mycompany;

import java.net.URI;
import java.net.http.*;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class Streamer {
    private WebSocket webSocket;

    public CompletableFuture<Void> start(String uri, Consumer<String> messageHandler) {
        HttpClient client = HttpClient.newHttpClient();
        return client.newWebSocketBuilder()
            .buildAsync(URI.create(uri), new WebSocket.Listener() {
                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    messageHandler.accept(data.toString());
                    return WebSocket.Listener.super.onText(webSocket, data, last);
                }
            })
            .thenAccept(ws -> this.webSocket = ws);
    }

    //public CompletableFuture<Void> stop() {
    public CompletableFuture<WebSocket> stop() {
        if (webSocket != null) {
            return webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Done");
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }
}
