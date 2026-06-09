package com.example.myapplication.controller;

import com.example.myapplication.utils.SupabaseConfig;

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

    public void getBooksPaged(String token, int limit, int offset, ApiCallback callback) {
        String url = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/books"
                + "?select=*"
                + "&limit=" + limit
                + "&offset=" + offset
                + "&order=created_at.desc";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();

        sendRequest(request, callback);
    }

    public void getBestSellerBooks(String token, int limit, ApiCallback callback) {
        String url = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/books"
                + "?select=*"
                + "&is_bestseller=eq.true"
                + "&limit=" + limit;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();

        sendRequest(request, callback);
    }

    public void getAudioBooksPaged(String token, int limit, int offset, ApiCallback callback) {
        String url = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/books"
                + "?select=*,audio_chapters!inner(id)"
                + "&limit=" + limit
                + "&offset=" + offset
                + "&order=created_at.desc";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();

        sendRequest(request, callback);
    }

    public void getAudioBestSellerBooks(String token, int limit, ApiCallback callback) {
        String url = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/books"
                + "?select=*,audio_chapters!inner(id)"
                + "&is_bestseller=eq.true"
                + "&limit=" + limit;

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

    public void loginWithGoogleToken(String googleIdToken, ApiCallback callback) {
        String url = SupabaseConfig.SUPABASE_URL + "/auth/v1/token?grant_type=id_token";

        try {
            JSONObject json = new JSONObject();
            json.put("provider", "google");
            json.put("id_token", googleIdToken);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            sendRequest(request, callback);

        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void getFirstChapterByBookId(String bookId, ApiCallback callback) {
        String url = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/audio_chapters?select=*&book_id=eq."
                + bookId
                + "&order=chapter_number.asc&limit=1";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY)
                .get()
                .build();

        sendRequest(request, callback);
    }

    public void searchBooks(String query, ApiCallback callback) {
        try {
            String safeQuery = java.net.URLEncoder.encode("*" + query + "*", "UTF-8");
            String url = SupabaseConfig.SUPABASE_URL 
                    + "/rest/v1/books"
                    + "?select=*"
                    + "&or=(title.ilike." + safeQuery + ",author.ilike." + safeQuery + ",category.ilike." + safeQuery + ")";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY)
                    .get()
                    .build();

            sendRequest(request, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void getBooksByCategory(String category, ApiCallback callback) {
        try {
            String safeCategory = java.net.URLEncoder.encode("*" + category + "*", "UTF-8");
            String url = SupabaseConfig.SUPABASE_URL
                    + "/rest/v1/books"
                    + "?select=*"
                    + "&category=ilike." + safeCategory;

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY)
                    .get()
                    .build();

            sendRequest(request, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void getProfile(String token, String userId, ApiCallback callback) {
        String url = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/profiles"
                + "?select=*"
                + "&id=eq." + userId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();

        sendRequest(request, callback);
    }

    public void updateProfile(String token, String userId, String fullName, String avatarUrl, ApiCallback callback) {
        String url = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/profiles"
                + "?id=eq." + userId;

        try {
            JSONObject json = new JSONObject();
            json.put("full_name", fullName);
            json.put("avatar_url", avatarUrl);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .patch(body)
                    .build();

            sendRequest(request, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void insertProfile(String token, String userId, String email, String fullName, String avatarUrl, String provider, ApiCallback callback) {
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/profiles";

        try {
            JSONObject json = new JSONObject();
            json.put("id", userId);
            json.put("email", email);
            json.put("full_name", fullName);
            json.put("avatar_url", avatarUrl);
            json.put("provider", provider);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .post(body)
                    .build();

            sendRequest(request, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void refreshSession(String refreshToken, ApiCallback callback) {
        String url = SupabaseConfig.SUPABASE_URL + "/auth/v1/token?grant_type=refresh_token";

        try {
            JSONObject json = new JSONObject();
            json.put("refresh_token", refreshToken);

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

    public void getChaptersByBookId(String bookId, ApiCallback callback) {
        String url = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/audio_chapters?select=*"
                + "&book_id=eq." + bookId
                + "&order=chapter_number.asc";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY)
                .get()
                .build();

        sendRequest(request, callback);
    }

    public void getBookTotalDuration(String bookId, ApiCallback callback) {
        String url = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/books?select=total_duration&id=eq." + bookId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY)
                .get()
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