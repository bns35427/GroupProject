package com.example.finalpj;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit;
//    private static final String BASE_URL = "https://firebase-api-5lbf4u4nsa-uc.a.run.app/";
//    private  static  final String BASE_URL="https://us-central1-dbdb-96e12.cloudfunctions.net/firebase_api/";
    private  static final String BASE_URL="http://172.30.16.141:8080/";
//    private  static  final String BASE_URL="http://192.168.45.5:8000/";
//    private static final String BASE_URL="https://bootcamp-final-306119718275.asia-northeast3.run.app/";

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(120, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .writeTimeout(120, TimeUnit.SECONDS)
                    .build();
            retrofit = new Retrofit.Builder()
                    .client(client)  // Timeout 적용
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
