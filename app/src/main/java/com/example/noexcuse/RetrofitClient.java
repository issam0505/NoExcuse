package com.example.noexcuse;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // هاد الـ URL هو العنوان ديال الـ FastAPI في البيسي ديالك
    // 10.0.2.2 كيعوض localhost فاش كتكون كتخدم بـ Emulator ديال الأندرويد
    private static final String BASE_URL = "http://192.168.1.11:8000/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create()) // باش يحول JSON لـ Java Object
                    .build();
        }
        return retrofit;
    }
}