package com.example.finalpj;

import android.widget.TextView;

import androidx.media3.common.FlagSet;
import androidx.media3.common.util.Log;

import com.google.firebase.firestore.auth.User;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {
    @Multipart
    @POST("/upload_image/")
    Call<Void> uploadImage(
            @Part MultipartBody.Part file,
            @Part("input") RequestBody input,
            @Part("result") RequestBody result
    );


    @POST("/upload_text/")
    Call<Void> uploadRecord(@Body RecordModel recordModel);

    class RecordModel {
        String result_text;
        String languageType;
        String sourceType;

        public RecordModel(String result_text, String sourceType,String languageType) {
            this.result_text = result_text;
            this.languageType = languageType;
            this.sourceType=sourceType;
        }
    }

    @POST("/agent/")
    Call<Void> uploadAgent(@Body AgentModel agentModel);

    class AgentModel {
        String user_text;

        public AgentModel(String user_text) {
            this.user_text = user_text;
        }
    }

    @GET("v1/search/local.json")
    Call<MapActivity.NaverSearchResponse> getSearchResults(
            @Header("X-Naver-Client-Id") String clientId,
            @Header("X-Naver-Client-Secret") String clientSecret,
            @Query("query") String query,
            @Query("display") int display,
            @Query("start") int start,
            @Query("sort") String sort
    );

    @GET("map-geocode/v2/geocode")
    Call<MapActivity.NaverGeocodeResponse> getGeocode(
            @Header("X-NCP-APIGW-API-KEY-ID") String clientId,
            @Header("X-NCP-APIGW-API-KEY") String clientSecret,
            @Query("query") String address
    );

    @GET("/get_translated_text")
        // FastAPI 서버의 GET 엔드포인트
    Call<TranslatorActivity.ApiResponse> getMessage();


    @GET("/get_agent_text")
    Call<AgentActivity.ApiResponse> getText(@Query("response") String response);

    @GET("/translated_results")
    Call<TakePictureActivity.TranslatedResultsResponse> getTranslatedResults();
}


