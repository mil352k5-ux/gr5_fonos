package com.example.myapplication.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FonosApiManager {

    public interface ApiCallback {
        void onSuccess(String responseBody);
        void onError(String errorMessage);
    }

    private final OkHttpClient client;
    private final Handler mainHandler;

    public FonosApiManager(Context context) {
        client = new OkHttpClient();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void signUpWithEmail(String email, String password, ApiCallback callback) {
        String url = SupabaseConfig.SUPABASE_URL + "/auth/v1/signup";

        try {
            JSONObject json = new JSONObject();
            json.put("email", email);
            json.put("password", password);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            sendRequest(request, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void loginWithEmail(String email, String password, ApiCallback callback) {
        String url = SupabaseConfig.SUPABASE_URL + "/auth/v1/token?grant_type=password";

        try {
            JSONObject json = new JSONObject();
            json.put("email", email);
            json.put("password", password);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            sendRequest(request, callback);

        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void getBooks(String token, ApiCallback callback) {
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/books?select=*";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();

        sendRequest(request, callback);
    }

    public void saveLibraryProgress(String token, String userId, String bookId, int progressSeconds, ApiCallback callback) {
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/user_library";

        try {
            JSONObject json = new JSONObject();
            json.put("user_id", userId);
            json.put("book_id", bookId);
            json.put("progress_seconds", progressSeconds);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .post(body)
                    .build();

            sendRequest(request, callback);

        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void createProfile(String userId, String email, String fullName, String provider, ApiCallback callback) {
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/profiles";

        try {
            JSONObject json = new JSONObject();
            json.put("id", userId);
            json.put("email", email);
            json.put("full_name", fullName);
            json.put("provider", provider);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .post(body)
                    .build();

            sendRequest(request, callback);

        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void searchGoogleBooks(String query, ApiCallback callback) {
        String url =
                "https://www.googleapis.com/books/v1/volumes?q="
                        + query.replace(" ", "+")
                        + "&maxResults=20"
                        + "&key="
                        + SupabaseConfig.GOOGLE_BOOKS_API_KEY;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        sendRequest(request, callback);
    }

    public void insertBookToSupabase(String token, JSONObject book, ApiCallback callback) {
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/books";

        RequestBody body = RequestBody.create(
                book.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(body)
                .build();

        sendRequest(request, callback);
    }

    private void sendRequest(Request request, ApiCallback callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(result));
                } else {
                    mainHandler.post(() -> callback.onError(result));
                }
            }
        });
    }
}