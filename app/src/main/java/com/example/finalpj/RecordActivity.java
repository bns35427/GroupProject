//package com.example.finalpj;
//
//import android.Manifest;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.os.Bundle;
//import android.speech.RecognitionListener;
//import android.speech.RecognizerIntent;
//import android.speech.SpeechRecognizer;
//import android.widget.Button;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//
//import com.google.gson.annotations.SerializedName;
//
//import java.util.ArrayList;
//
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//
//public class RecordActivity extends AppCompatActivity {
//
//    // UI 요소 선언
//    private Button btnStartSpeech; // 음성 인식 시작 버튼
//    private TextView tvResult;     // 음성 인식 결과를 보여줄 텍스트뷰
//    private SpeechRecognizer speechRecognizer;
//    String resultText;// SpeechRecognizer 객체
//    ApiService apiService;
//
//    // 권한 요청 코드
//    private static final int PERMISSION_REQUEST_CODE = 101;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_record);
//
//        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
//        // 레이아웃 요소 연결
//        btnStartSpeech = findViewById(R.id.rcBtn);
//        tvResult = findViewById(R.id.result);
//
//        // 권한 확인
//        if (!hasAudioPermission()) {
//            requestAudioPermission();
//        }
//
//        // SpeechRecognizer 초기화
//        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
//
//        // 버튼 클릭 이벤트 설정
//        btnStartSpeech.setOnClickListener(v -> {
//            if (hasAudioPermission()) {
//                startSpeechRecognition();
//            } else {
//                Toast.makeText(this, "오디오 녹음 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    // 오디오 권한 확인
//    private boolean hasAudioPermission() {
//        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
//    }
//
//    // 오디오 권한 요청
//    private void requestAudioPermission() {
//        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
//    }
//
//    // 권한 요청 결과 처리
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == PERMISSION_REQUEST_CODE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(this, "권한 허용됨", Toast.LENGTH_SHORT).show();
//            } else {
//                Toast.makeText(this, "권한이 필요합니다.", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
//    // 음성 인식 시작
//    private void startSpeechRecognition() {
//        // RecognizerIntent 설정
//        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); // 자유로운 대화 방식
//        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
//        //한국어로 설정
////        태국어 (Thai)	th-TH	"th-TH"
////        인도네시아어 (Indonesian)	id-ID	"id-ID"
////        말레이어 (Malay)	ms-MY	"ms-MY"
////        베트남어 (Vietnamese)	vi-VN	"vi-VN"
////        필리핀어 (Filipino)	fil-PH	"fil-PH"
//        // SpeechRecognizer 리스너 설정
//        speechRecognizer.setRecognitionListener(new RecognitionListener() {
//            @Override
//            public void onReadyForSpeech(Bundle params) {
//                // 사용자가 말할 준비가 되었을 때 호출
//                Toast.makeText(RecordActivity.this, "말씀하세요...", Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            public void onBeginningOfSpeech() {
//                // 사용자가 말하기 시작했을 때 호출
//                tvResult.setText("듣고 있습니다...");
//            }
//
//            @Override
//            public void onRmsChanged(float rmsdB) {
//                // 음성 입력의 볼륨 레벨이 변경될 때 호출
//            }
//
//            @Override
//            public void onBufferReceived(byte[] buffer) {
//                // 음성 데이터를 버퍼로 받았을 때 호출
//            }
//
//            @Override
//            public void onEndOfSpeech() {
//                // 사용자가 말하기를 멈췄을 때 호출
//                tvResult.setText("음성 입력 종료...");
//
//            }
//
//            @Override
//            public void onError(int error) {
//                // 오류 발생 시 호출
//                String message;
//                switch (error) {
//                    case SpeechRecognizer.ERROR_NETWORK:
//                        message = "네트워크 오류";
//                        break;
//                    case SpeechRecognizer.ERROR_AUDIO:
//                        message = "오디오 오류";
//                        break;
//                    case SpeechRecognizer.ERROR_NO_MATCH:
//                        message = "인식된 내용이 없습니다.";
//                        break;
//                    default:
//                        message = "알 수 없는 오류: " + error;
//                        break;
//                }
//                Toast.makeText(RecordActivity.this, "오류 발생: " + message, Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            public void onResults(Bundle results) {
//                // 최종 음성 인식 결과를 받았을 때 호출
//                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
//                resultText=matches.get(0);
//                if (matches != null && !matches.isEmpty()) {
//                    // 첫 번째 결과를 텍스트뷰에 표시
//                    tvResult.setText(matches.get(0));
//                }
//                uploadRecordToServer(resultText,"ko_kr");
//            }
//
//            @Override
//            public void onPartialResults(Bundle partialResults) {
//                // 부분적인 음성 인식 결과를 받았을 때 호출
//            }
//
//            @Override
//            public void onEvent(int eventType, Bundle params) {
//                // 기타 이벤트 발생 시 호출
//            }
//        });
//
//        // 음성 인식 시작
//        speechRecognizer.startListening(intent);
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        // SpeechRecognizer 자원 해제
//        if (speechRecognizer != null) {
//            speechRecognizer.destroy();
//        }
//    }
//
//    private void uploadRecordToServer(String rcText,String lang) {
//
//        // 데이터 모델 생성
//        ApiService.RecordModel recordModel = new ApiService.RecordModel(rcText,lang);
//
//        // 서버에 데이터 전송
//        Call<Void> call = apiService.uploadRecord(recordModel);
//        call.enqueue(new Callback<Void>() {
//            @Override
//            public void onResponse(Call<Void> call, Response<Void> response) {
//                if (response.isSuccessful()) {
//                    Toast.makeText(RecordActivity.this, "음성 업로드 성공!", Toast.LENGTH_SHORT).show();
//                } else {
//                    Toast.makeText(RecordActivity.this, "음성 업로드 실패: " + response.code(), Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onFailure(Call<Void> call, Throwable t) {
//                Toast.makeText(RecordActivity.this, "통신 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//}