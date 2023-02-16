package yooga.app.service;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@ApplicationScoped
public class HttpService {
    
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();
    
    public Response get(String url) throws IOException {
        Request request = new Request.Builder()
            .url(url)
            .build();
        return client.newCall(request).execute();
    }

    public Response delete(String url) throws IOException {
        Request request = new Request.Builder()
            .url(url)
            .delete()
            .build();
        return client.newCall(request).execute();
    }

    public Response post(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .build();
        return client.newCall(request).execute();
    }

    public Response put(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
            .url(url)
            .put(body)
            .build();
        return client.newCall(request).execute();
    }
}
