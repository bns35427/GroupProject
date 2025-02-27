package com.example.finalpj;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.util.Log;

import com.google.gson.annotations.SerializedName;

import org.w3c.dom.Text;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AgentActivity extends AppCompatActivity {
    ApiService apiService;
    ProgressBar loadingSpinner;
    Button checkBtn;
    Integer[] resultIds = {R.id.result_1, R.id.result_2, R.id.result_3};
    EditText userInputText;
    TextView result1,result2,result3;
    String userInputStr;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agent);
        userInputText=findViewById(R.id.editText);
        checkBtn=findViewById(R.id.checkBtn);
        result1=findViewById(R.id.result_1);
        result2=findViewById(R.id.result_2);
        result3=findViewById(R.id.result_3);
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        for(int i=0;i<3;i++){
            fetchMessage(i);
        }
        userInputText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    checkBtn.performClick(); // 버튼 클릭 실행
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(userInputText.getWindowToken(), 0);
                    return true; // 이벤트 소비 (기본 엔터 동작 방지)
                }
                return false;
            }
        });
        checkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userInputStr=userInputText.getText().toString().trim();
                result1.setText("");
                result2.setText("");
                result3.setText("");
                UserToAgentUpload(userInputStr);
                userInputText.setText("");


            }
        });
        
    }
    public class ApiResponse {
        @SerializedName("status")
        private String status;
        @SerializedName("text1")
        private String text1;
        @SerializedName("text2")
        private String text2;
        @SerializedName("text3")
        private String text3;

        public String getStatus(){return status;}
        public String getText1(){return text1;}
        public String getText2(){return text2;}
        public String getText3(){return text3;}


    }
    private void fetchMessage(int position) {
        Call<ApiResponse> call = apiService.getText("1");

        TextView res=findViewById(resultIds[position]);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful()) {
                    ApiResponse apiResponse = response.body();
                    String resultText=null;
                    if(position==0){
                        resultText=apiResponse.getText1();
                    } else if (position==1) {
                        resultText= apiResponse.getText2();
                    }
                    else
                        resultText= apiResponse.getText3();
                    res.setText(resultText);
                } else {
                    Log.e("Retrofit", "Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e("Retrofit", "Failed: " + t.getMessage());
            }
        });
    }
    private void UserToAgentUpload(String userInputText) {
        ApiService.AgentModel agentModel = new ApiService.AgentModel(userInputText);
        showLoading();
        Call<Void> call = apiService.uploadAgent(agentModel);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                hideLoading();
                if (response.isSuccessful()) {
                    Log.d("Agent", "성공");
                    for(int i=0;i<3;i++){
                        fetchMessage(i);
                    }

                } else {

                    Log.d("Agent", "실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.d("Agent", "실패: " + t.getMessage());
            }
        });

    }
    private void showLoading() {
        loadingSpinner=findViewById(R.id.loadingSpinner);
        loadingSpinner.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        loadingSpinner=findViewById(R.id.loadingSpinner);
        loadingSpinner.setVisibility(View.GONE);
    }
}
