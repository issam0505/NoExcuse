package com.example.noexcuse;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    @POST("register_user")
    Call<Map<String, Object>> registerUser(@Body Map<String, Object> userData);
    @GET("get_user/{uid}")
    Call<Map<String, Object>> getUserData(@Path("uid") String uid);
}