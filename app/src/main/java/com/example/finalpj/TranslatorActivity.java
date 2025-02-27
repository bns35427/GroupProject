package com.example.finalpj;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.media3.common.util.Log;
import androidx.slidingpanelayout.widget.SlidingPaneLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TranslatorActivity extends AppCompatActivity {

    private SlidingPaneLayout slidingPaneLayout;
    private ListView itemListView;
    private FirebaseAuth mAuth=FirebaseAuth.getInstance();
    private FirebaseFirestore mStore = FirebaseFirestore.getInstance();

    // 음성 인식 시작 버튼
    // 음성 인식 결과를 보여줄 텍스트뷰
    private SpeechRecognizer speechRecognizer;
    String resultText;// SpeechRecognizer 객체
    ApiService apiService;
    Spinner spin1,spin2;
    String[] items={"한국어","English","नेपाली","Indonesia","Tiếng Việt","မြန်မာ","ខ្មែរ"};
    String[] langs={"한국어","영어","네팔어","인도네시아어","베트남어","버마어","크메르어"};
    String[] items2={"한국어","English","नेपाली","Indonesia","Tiếng Việt","မြန်မာ","ខ្មែរ"};

    ProgressBar loadingSpinner;
    String inputLanguage=null;
    String resultLanguage=null;
    private int lastSelectedPosition = 0;
    private int lastSelectedPosition2 = 0;
    TextView resultId;
    // 권한 요청 코드
    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final int REQUEST_CODE_PICK_IMAGE = 1;
    String inputLangType=null;
    String resultLangType=null;
    boolean recordFin=false;
    Map<String, Object> inputMap = new HashMap<>();
    String inputId;
    String userInputStr;
    ResultAdapter resultAdapter;
    ArrayList<String> itemNames= new ArrayList<>();;
    ArrayList<String> inputNames=new ArrayList<>();
    ArrayList<String> inputDb = new ArrayList<>();

    DrawerLayout drawerLayout;
    Button tabButton;
    FirebaseUser user = mAuth.getCurrentUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translator);

        ImageButton camera=findViewById(R.id.cameraId);
        spin1=findViewById(R.id.spinId);
        spin2=findViewById(R.id.spinId2);
        Button transBtn=findViewById(R.id.tranBtn);
        // ListView 및 Adapter 초기화
        ImageView tabButton=findViewById(R.id.tabButton);
        drawerLayout = findViewById(R.id.main);

        // Firestore에서 데이터 실시간 가져오기
        mStore.collection("Text")
                .document(user.getUid())
                .collection("inputs")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w("Firestore", "Listen failed.", error);
                        return;
                    }

                    // ✅ 데이터 초기화
                    itemNames.clear();
                    inputNames.clear();

                    // ✅ 실시간 데이터 업데이트
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            String inputText = document.getString("UserInputText");
                            String resultText = document.getString("TransText");

                            // ✅ 필드가 null이 아닐 때만 추가
                            if (inputText != null && resultText != null) {
                                itemNames.add("Input:"+inputText);
                                inputNames.add("Trans:"+resultText);
                            }
                        }
                    }
                    resultAdapter.notifyDataSetChanged();
                });


        ListView lv=findViewById(R.id.lv);
        resultAdapter=new ResultAdapter(this,itemNames,inputNames);
        lv.setAdapter(resultAdapter);

        // 🔥 탭 버튼을 클릭하면 슬라이드 메뉴가 나타남
        tabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    drawerLayout.openDrawer(GravityCompat.END);
                }
            }
        });
// ✅ Firestore에서 데이터 실시간 가져오기


        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(getApplicationContext(), TakePictureActivity.class);
                startActivity(intent);
            }
        });
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, items);
        ArrayAdapter<String> adapter2=new ArrayAdapter<>(this, R.layout.spinner_item, items2);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin1.setAdapter(adapter2);
        spin2.setAdapter(adapter);
        spin1.setSelection(lastSelectedPosition);
        spin1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                lastSelectedPosition = i;
                inputLanguage=langs[i];
                spin1.setSelection(lastSelectedPosition);
            }

            //한국어로 설정
//        태국어 (Thai)	th-TH	"th-TH"
//        인도네시아어 (Indonesian)	id-ID	"id-ID"
//        말레이어 (Malay)	ms-MY	"ms-MY"
//        베트남어 (Vietnamese)	vi-VN	"vi-VN"
//        필리핀어 (Filipino)	fil-PH	"fil-PH"

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spin2.setSelection(lastSelectedPosition2);
        spin2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                resultLanguage=langs[i];
                lastSelectedPosition2 = i;

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        ImageButton recordId=findViewById(R.id.recordId);
        // 레이아웃 요소 연결
        resultId= findViewById(R.id.resultId);

        // 권한 확인
        if (!hasAudioPermission()) {
            requestAudioPermission();
        }

        // SpeechRecognizer 초기화
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        // 버튼 클릭 이벤트 설정
        recordId.setOnClickListener(v -> {
            if (hasAudioPermission()) {
                startSpeechRecognition();
            } else {
                Toast.makeText(this, "You need permission to record audio.", Toast.LENGTH_SHORT).show();
            }
        });
        EditText userInput=findViewById(R.id.userInput);
        Button tranBtn=findViewById(R.id.tranBtn);
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        // 데이터 저장 (transBtn 클릭 시)
        tranBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userInputStr = userInput.getText().toString().trim();
                if (userInputStr.isEmpty()) {
                    userInput.setError("Input Text!");
                    userInput.requestFocus();
                    return;
                }

                // ✅ 고유한 inputId 생성
                String formattedNow = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                inputId = user.getUid() + formattedNow;
                userInput.setText("");

                uploadRecordToServer(userInputStr,inputLanguage,resultLanguage);


            }
        });


    }

    public static class ResultAdapter extends BaseAdapter {
        Context context;
        ArrayList<String> result;
        ArrayList<String> input;
        public ResultAdapter(Context context, ArrayList<String> result,ArrayList<String> input) {
            this.context=context;
            this.result = result;
            this.input=input;
        }

        @Override
        public int getCount() {
            return result.size();
        }

        @Override
        public Object getItem(int i) {
            return result.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                view = inflater.inflate(R.layout.lvitem2, viewGroup, false);
            }
            // ✅ 올바른 findViewById 위치 (view에서 호출)
            TextView resultTv = view.findViewById(R.id.textId);
            TextView inputTv=view.findViewById(R.id.inputId);
            resultTv.setText(result.get(i));
            inputTv.setText(input.get(i));
            return view;
        }
    }


    // 오디오 권한 확인
    private boolean hasAudioPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    // 오디오 권한 요청
    private void requestAudioPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
    }

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "Permission is required.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 음성 인식 시작
    private void startSpeechRecognition() {
        // RecognizerIntent 설정
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");

        // 애니메이션 뷰 가져오기
        LottieAnimationView micAnimation = findViewById(R.id.micAnimation);
        View dark=findViewById(R.id.dark_overlay);


        // SpeechRecognizer 리스너 설정
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Toast.makeText(TranslatorActivity.this, "Speeking. . .", Toast.LENGTH_SHORT).show();
                dark.setVisibility(View.VISIBLE);
                micAnimation.setVisibility(View.VISIBLE); // 애니메이션 보이게
                micAnimation.playAnimation(); // 애니메이션 재생
            }

            @Override
            public void onBeginningOfSpeech() {
//                resultId.setText("듣고 있습니다...");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // 음성 입력의 볼륨 레벨이 변경될 때 호출
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // 음성 데이터를 버퍼로 받았을 때 호출
            }

            @Override
            public void onEndOfSpeech() {
//                resultId.setText("음성 입력 종료...");
                micAnimation.cancelAnimation(); // 애니메이션 정지
                dark.setVisibility(View.GONE);
                micAnimation.setVisibility(View.GONE); // 애니메이션 숨김
            }

            @Override
            public void onError(int error) {
                String message;
                switch (error) {
                    case SpeechRecognizer.ERROR_NETWORK:
                        message = "네트워크 오류";
                        break;
                    case SpeechRecognizer.ERROR_AUDIO:
                        message = "오디오 오류";
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        message = "인식된 내용이 없습니다.";
                        break;
                    default:
                        message = "알 수 없는 오류: " + error;
                        break;
                }
                Toast.makeText(TranslatorActivity.this, "오류 발생: " + message, Toast.LENGTH_SHORT).show();
                micAnimation.cancelAnimation(); // 오류 발생 시 애니메이션 정지
                micAnimation.setVisibility(View.GONE); // 애니메이션 숨김
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                resultText = matches.get(0);
                EditText useres=findViewById(R.id.userInput);
                if (matches != null && !matches.isEmpty()) {
                    useres.setText(matches.get(0));
                }
                micAnimation.cancelAnimation(); // 음성 인식이 끝나면 애니메이션 정지
                micAnimation.setVisibility(View.GONE); // 애니메이션 숨김
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // 부분적인 음성 인식 결과를 받았을 때 호출
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // 기타 이벤트 발생 시 호출
            }
        });

        // 음성 인식 시작
        speechRecognizer.startListening(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // SpeechRecognizer 자원 해제
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    private void uploadRecordToServer(String rcText,String input,String langs) {
        if(input.equals("English")){
            input="영어";
        }
        // 데이터 모델 생성
        ApiService.RecordModel recordModel = new ApiService.RecordModel(rcText,input,langs);

        // 서버에 데이터 전송
        Call<Void> call = apiService.uploadRecord(recordModel);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(TranslatorActivity.this, "업로드 성공!", Toast.LENGTH_SHORT).show();
                    System.out.println(call.request().body());
                    fetchMessage();
                } else {
                    Toast.makeText(TranslatorActivity.this, "업로드 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.d("MAINERROR", response.errorBody().toString());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(TranslatorActivity.this, "통신 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public class ApiResponse {
        private String message;

        public String getMessage() {
            return message;
        }
    }
    private void fetchMessage() {
        Call<ApiResponse> call = apiService.getMessage();
        TextView result=findViewById(R.id.resultId);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful()) {
                    ApiResponse apiResponse = response.body();
                    hideLoading();

                    if (apiResponse != null) {
                        inputMap.put("UserInputText",userInputStr);
                        inputMap.put("TransText",apiResponse.getMessage());
                        Log.d("Retrofit", "Message: " + apiResponse.getMessage());
                        result.setText(apiResponse.getMessage());
                        // ✅ Firestore에 저장
                        mStore.collection("Text")
                                .document(user.getUid())
                                .collection("inputs")
                                .document(inputId)
                                .set(inputMap)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("Firestore", "InputText 저장 성공");
                                })
                                .addOnFailureListener(e -> {
                                    Log.w("Firestore", "InputText 저장 실패", e);
                                });

                    }
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

    private void showLoading() {
        loadingSpinner=findViewById(R.id.loadingSpinner);
        loadingSpinner.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        loadingSpinner=findViewById(R.id.loadingSpinner);
        loadingSpinner.setVisibility(View.GONE);
    }

}