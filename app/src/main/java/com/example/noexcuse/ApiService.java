package com.example.noexcuse;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("register_user")
    Call<Map<String, Object>> registerUser(@Body Map<String, Object> userData);
}