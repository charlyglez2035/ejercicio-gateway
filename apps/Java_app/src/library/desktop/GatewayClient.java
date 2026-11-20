package library.desktop;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.UUID;

/**
 * Cliente HTTP que habla SOLO con el Reverse Proxy / API Gateway nginx (puerto 80).
 * Nunca usa los puertos internos 5000-5005: nginx enruta /api/<servicio>/<ruta>.
 */
public final class GatewayClient {

    /** Resultado de una peticion, con lo necesario para el log de evidencias. */
    public static final class Result {
        public final String method;
        public final String url;
        public final int status;          // 0 = sin respuesta (red/timeout)
        public final String body;
        public final String requestId;    // X-Request-ID que devuelve nginx
        public final long millis;
        public final boolean sentJwt;
        public final String networkError;

        Result(String method, String url, int status, String body, String requestId,
               long millis, boolean sentJwt, String networkError) {
            this.method = method;
            this.url = url;
            this.status = status;
            this.body = body;
            this.requestId = requestId;
            this.millis = millis;
            this.sentJwt = sentJwt;
            this.networkError = networkError;
        }

        public boolean ok() {
            return status >= 200 && status < 300;
        }

        public Object json() {
            return Json.parse(body);
        }
    }

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private volatile String gatewayUrl;
    private volatile String accessToken;

    public GatewayClient(String gatewayUrl) {
        setGatewayUrl(gatewayUrl);
    }

    public void setGatewayUrl(String url) {
        URI uri = URI.create(url.trim());
        if (uri.getScheme() == null || uri.getHost() == null
                || !(uri.getScheme().equals("http") || uri.getScheme().equals("https"))) {
            throw new IllegalArgumentException("URL de gateway invalida: " + url);
        }
        this.gatewayUrl = url.trim().replaceAll("/+$", "");
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public void setAccessToken(String token) {
        this.accessToken = token;
    }

    public boolean hasToken() {
        return accessToken != null;
    }

    public Result get(String path, boolean withJwt) {
        return send("GET", path, null, withJwt);
    }

    public Result post(String path, String jsonBody, boolean withJwt) {
        return send("POST", path, jsonBody, withJwt);
    }

    private Result send(String method, String path, String jsonBody, boolean withJwt) {
        String url = gatewayUrl + path + (path.contains("?") ? "&" : "?") + "format=json";
        String clientRequestId = UUID.randomUUID().toString();
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("User-Agent", "library-java-desktop/1.0")
                .header("X-Client-Request-ID", clientRequestId);
        String token = accessToken;
        boolean sentJwt = withJwt && token != null;
        if (sentJwt) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (jsonBody != null) {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        long start = System.nanoTime();
        try {
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            long ms = (System.nanoTime() - start) / 1_000_000;
            String requestId = response.headers().firstValue("X-Request-ID").orElse("-");
            return new Result(method, url, response.statusCode(), response.body(), requestId, ms, sentJwt, null);
        } catch (HttpTimeoutException ex) {
            return failure(method, url, start, sentJwt, "Tiempo agotado: el gateway no respondio.");
        } catch (IOException ex) {
            return failure(method, url, start, sentJwt,
                    "No se pudo conectar con el gateway (" + ex.getClass().getSimpleName() + "). "
                            + "Revisa la IP de la VM y que nginx este activo en el puerto 80.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return failure(method, url, start, sentJwt, "Peticion interrumpida.");
        }
    }

    private static Result failure(String method, String url, long start, boolean sentJwt, String message) {
        long ms = (System.nanoTime() - start) / 1_000_000;
        return new Result(method, url, 0, "", "-", ms, sentJwt, message);
    }
}
