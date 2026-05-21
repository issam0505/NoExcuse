package com.example.noexcuse;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ── Users ────────────────────────────────────────────────────────────
    @POST("register_user")
    Call<Map<String, Object>> registerUser(@Body Map<String, Object> userData);

    @GET("get_user/{uid}")
    Call<Map<String, Object>> getUserData(@Path("uid") String uid);

    // ── Exercises ────────────────────────────────────────────────────────

    /**
     * Search exercises by muscle group.
     * FastAPI: GET /search_exercises?muscle=chest
     * Returns up to 20 results; WarmupActivity takes the first 6.
     */
    @GET("search_exercises")
    Call<List<Map<String, Object>>> searchExercises(
            @Query("muscle") String muscle
    );

    /**
     * Get a single exercise by ID.
     * FastAPI: GET /get_exercise/{ex_id}
     */
    @GET("get_exercise/{ex_id}")
    Call<Map<String, Object>> getExercise(
            @Path("ex_id") String exId
    );
    @POST("chat")
    Call<Map<String, Object>> chatWithAI(@Body Map<String, Object> requestBody);

}
