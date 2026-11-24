package com.example.kaspikids;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Rule;
import org.junit.Test;

public class AnnouncementClientTest {
    @Rule public final MockWebServer server = new MockWebServer();

    private AnnouncementClient client(long readTimeoutMillis) {
        OkHttpClient http = new OkHttpClient.Builder()
                .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(false)
                .build();
        return new AnnouncementClient(http, server.url("/announcement.json"));
    }

    @Test public void parsesSuccessAndSendsTheExpectedGetRequest() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"message\":\"KADR\"}"));
        assertEquals("KADR", client(2000).fetch());
        assertEquals("GET /announcement.json HTTP/1.1", server.takeRequest(2, TimeUnit.SECONDS).getRequestLine());
    }

    @Test public void reports404() {
        server.enqueue(new MockResponse().setResponseCode(404));
        assertEquals("HTTP 404", assertThrows(IOException.class, () -> client(2000).fetch()).getMessage());
    }

    @Test public void reports500() {
        server.enqueue(new MockResponse().setResponseCode(500));
        assertEquals("HTTP 500", assertThrows(IOException.class, () -> client(2000).fetch()).getMessage());
    }

    @Test public void reportsReadTimeout() {
        server.enqueue(new MockResponse().setBody("{\"message\":\"KADR\"}")
                .setBodyDelay(500, TimeUnit.MILLISECONDS));
        assertThrows(SocketTimeoutException.class, () -> client(100).fetch());
    }

    @Test public void rejectsMalformedJson() {
        server.enqueue(new MockResponse().setBody("{broken"));
        assertEquals("Invalid announcement JSON",
                assertThrows(IOException.class, () -> client(2000).fetch()).getMessage());
    }

    @Test public void rejectsMissingMessage() {
        server.enqueue(new MockResponse().setBody("{}"));
        assertEquals("Missing announcement message",
                assertThrows(IOException.class, () -> client(2000).fetch()).getMessage());
    }
}
