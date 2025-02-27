package com.example.finalpj;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    ApiService apiService;
    String userTextstr;
    ProgressBar loadingSpinner;

    Spinner spin1;
    String[] items={"한국어","English","नेपाली","Indonesia","Tiếng Việt","မြန်မာ","ខ្មែរ"};
    private int lastSelectedPosition = 0;
    private int lastSelectedPosition2 = 0;

    TextView text1,text2,text3;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mStore = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LinearLayout transLn=findViewById(R.id.transId);
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        transLn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(getApplicationContext(), TranslatorActivity.class);
                startActivity(intent);
            }
        });
        LinearLayout mapLn=findViewById(R.id.mapId);
        mapLn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(getApplicationContext(), MapActivity.class);
                startActivity(intent);
            }
        });



        EditText useredText=findViewById(R.id.userInputTextId);
        Button checkBtn=findViewById(R.id.checkBtn);
        useredText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    checkBtn.performClick(); // 버튼 클릭 실행
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(useredText.getWindowToken(), 0);
                    return true; // 이벤트 소비 (기본 엔터 동작 방지)
                }
                return false;
            }
        });
        checkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userTextstr=useredText.getText().toString().trim();
                UserToAgentUpload(userTextstr);


                }
        });

        mAuth=FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();

        spin1=findViewById(R.id.spinId);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item3, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin1.setAdapter(adapter);
        spin1.setSelection(lastSelectedPosition);
        spin1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                lastSelectedPosition = i;
                spin1.setSelection(lastSelectedPosition);
                // Firestore에서 데이터 실시간 가져오기
                mStore.collection("Tip").document(items[i]).get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                text1=findViewById(R.id.text1);
                                text2=findViewById(R.id.text2);
                                text3=findViewById(R.id.text3);
                                if (task.isSuccessful()) {
                                    // 🔥 DocumentSnapshot 가져오기
                                    DocumentSnapshot document = task.getResult();
                                    if (document.exists()) {
                                        // 🔥 필드명으로 데이터 가져오기
                                        String title = document.getString("text");
                                        String title1=document.getString("text1");
                                        String title2=document.getString("text2");// String 타입

                                        // 🔥 데이터 로그 확인
                                        Log.d("Firestore", "Title: " + title);

                                        // 🔥 가져온 데이터를 UI에 적용
                                        text1.setText(title);
                                        text2.setText(title1);
                                        text3.setText(title2);

                                    } else {
                                        Log.d("Firestore", "No such document");
                                    }
                                } else {
                                    Log.e("Firestore", "Failed to get document", task.getException());
                                }
                            }
                        });

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        ImageView imageView = findViewById(R.id.imageView);
        imageView.setOnClickListener(view -> {
            String url = "https://www.moel.go.kr/index.do"; // 이동할 웹사이트 URL
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });

    }

    private void UserToAgentUpload(String userInputText) {
        ApiService.AgentModel agentModel = new ApiService.AgentModel(userInputText);
        View dark=findViewById(R.id.dark_overlay);
        dark.setVisibility(View.VISIBLE);
        showLoading();

        Call<Void> call = apiService.uploadAgent(agentModel);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                hideLoading();
                dark.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Log.d("Agent", "성공");
                    Intent intent=new Intent(getApplicationContext(), AgentActivity.class);
                    startActivity(intent);

                } else {

                    Log.d("Agent", "실패: " + response.code());
                    Intent intent=new Intent(getApplicationContext(), AgentActivity.class);
                    startActivity(intent);
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
