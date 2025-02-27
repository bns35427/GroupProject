package com.example.finalpj;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.annotations.SerializedName;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TakePictureActivity extends AppCompatActivity {
    // UI 및 CameraX 관련 변수 선언
    private PreviewView previewView; // 카메라 프리뷰를 보여주는 뷰
    private ImageCapture imageCapture; // 사진 촬영 기능 제공
    private ExecutorService cameraExecutor;
    private ApiService apiService;
    TextView textId;

    String inputLanguage;
    String resultLanguage;

    private static final String PREFS_NAME = "LanguagePrefs";
    private static final String KEY_LANGUAGE = "selectedLanguage";


    View dark;
    Spinner spin1, spin2;

    private int lastSelectedPosition = 0;
    private int lastSelectedPosition2 = 0;
    ProgressBar loadingSpinner;

    String[] items={"한국어","English","नेपाली","Indonesia","Tiếng Việt","မြန်မာ","ខ្មែរ"};
    String[] langs={"한국어","영어","네팔어","인도네시아어","베트남어","버마어","크메르어"};
    String[] items2={"한국어","English","नेपाली","Indonesia","Tiếng Việt","မြန်မာ","ខ្មែរ"};

    private static final int REQUEST_CODE_PICK_IMAGE = 1;
    private ArrayList<String> textList = new ArrayList<>();
    private ArrayList<Double> xList = new ArrayList<>();
    private ArrayList<Double> yList = new ArrayList<>();

    Button captureButton;

    OverlayImageView capturedImage;
    ImageButton gallery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_picture);
        gallery = findViewById(R.id.gallery);

        capturedImage=findViewById(R.id.capturedImageView);
        textId=findViewById(R.id.textId);

//        spin1 = findViewById(R.id.spinId);
        spin2 = findViewById(R.id.spinId2);
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        // 레이아웃에서 뷰 연결
        previewView = findViewById(R.id.previewView);
        captureButton = findViewById(R.id.captureButton);
        String selectedImageUriStr = getIntent().getStringExtra("selectedImageUri");


        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item4, items);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this, R.layout.spinner_item4, items2);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spin1.setAdapter(adapter2);
        spin2.setAdapter(adapter);
//        spin1.setSelection(lastSelectedPosition);
//        spin1.setSelection(lastSelectedPosition);
//        spin1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//                lastSelectedPosition = i;
//                inputLanguage=langs[i];
//                spin1.setSelection(lastSelectedPosition);
//            }
//
//            //한국어로 설정
////        태국어 (Thai)	th-TH	"th-TH"
////        인도네시아어 (Indonesian)	id-ID	"id-ID"
////        말레이어 (Malay)	ms-MY	"ms-MY"
////        베트남어 (Vietnamese)	vi-VN	"vi-VN"
////        필리핀어 (Filipino)	fil-PH	"fil-PH"
//
//            @Override
//            public void onNothingSelected(AdapterView<?> adapterView) {
//
//            }
//        });
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        resultLanguage = prefs.getString(KEY_LANGUAGE, "한국어"); // 기본값은 한국어

        spin2.setSelection(lastSelectedPosition2);
        spin2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                resultLanguage = langs[i];
                lastSelectedPosition2 = i;

                // 🔥 언어 선택 시 바로 저장
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_LANGUAGE, resultLanguage);
                editor.apply();

                Log.d("LanguageSelection", "선택된 언어: " + resultLanguage);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                resultLanguage = "한국어"; // 기본값 설정
            }
        });



        // 카메라 권한 요청
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            // 권한이 허용된 경우 카메라 초기화
            startCamera();
        } else {
            // 권한 요청
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        }

        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();

            }
        });
        if (selectedImageUriStr != null) {
            Uri selectedImageUri = Uri.parse(selectedImageUriStr);
            System.out.println(selectedImageUri);
            // 🔥 PreviewView 숨기기
            previewView.setVisibility(View.GONE);
            textId.setVisibility(View.GONE);
            spin2.setVisibility(View.GONE);
            gallery.setVisibility(View.GONE);
            captureButton.setVisibility(View.GONE);
            System.out.println(resultLanguage);
            // 🔥 ImageView에 선택한 이미지 표시
            capturedImage.setVisibility(View.VISIBLE);
            capturedImage.setImageURI(selectedImageUri);
            getBytesFromUri(selectedImageUri);
        }

        // 사진 촬영 버튼 클릭 리스너
        captureButton.setOnClickListener(v -> {
            if (imageCapture == null) {
                // imageCapture가 초기화되지 않은 경우 에러 메시지 출력
                Toast.makeText(this, "카메라가 초기화되지 않았습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            // 사진 촬영 후 갤러리에 저장
            imageCapture.takePicture(ContextCompat.getMainExecutor(this),
                    new ImageCapture.OnImageCapturedCallback() {
                        @Override
                        public void onCaptureSuccess(@NonNull ImageProxy image) {
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            byte[] imageData = new byte[buffer.remaining()];
                            buffer.get(imageData);

                            // 갤러리에 저장
                            textId.setVisibility(View.GONE);
                            spin2.setVisibility(View.GONE);
                            gallery.setVisibility(View.GONE);
                            captureButton.setVisibility(View.GONE);
                            savePhotoToGallery(imageData);
                            // 서버에 업로드

//
                            image.close();
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            // 촬영 실패 시 에러 메시지 출력
                            Toast.makeText(TakePictureActivity.this, "사진 촬영 실패: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        resultLanguage = prefs.getString(KEY_LANGUAGE, "한국어");
        Log.d("LanguageRestore", "복원된 언어: " + resultLanguage);

        // Spinner에 복원된 언어 설정
        int position = java.util.Arrays.asList(langs).indexOf(resultLanguage);
        if (position >= 0) {
            spin2.setSelection(position);
        }
    }



    // 카메라 작업을 위한 스레드 초기화

    // 카메라를 초기화하고 화면에 프리뷰를 표시하는 메서드
    private void startCamera() {
        // CameraX의 ProcessCameraProvider 초기화
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // CameraProvider 가져오기
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 카메라 설정
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA; // 후면 카메라 선택
                androidx.camera.core.Preview preview = new androidx.camera.core.Preview.Builder().build(); // 프리뷰 설정
                preview.setSurfaceProvider(previewView.getSurfaceProvider()); // 프리뷰를 PreviewView에 연결

                // ImageCapture 설정 (사진 촬영 기능)
                imageCapture = new ImageCapture.Builder().build();

                // CameraX에 바인딩 (라이프사이클과 연결)
                cameraProvider.unbindAll(); // 이전 카메라 연결 해제
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (Exception e) {
                // 카메라 초기화 실패 시 에러 메시지 출력
                Toast.makeText(this, "카메라 초기화 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d("CameraCamera", e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // 권한 요청 결과를 처리하는 메서드
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // 카메라 권한이 허용된 경우 카메라 초기화
            startCamera();
        } else {
            // 권한이 거부된 경우 앱 종료
            Toast.makeText(this, "카메라 권한이 필요합니다. 앱을 종료합니다.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // 촬영된 이미지를 갤러리에 저장하는 메서드
    private void savePhotoToGallery(byte[] imageData) {
        ContentResolver resolver = getContentResolver();
        ContentValues contentValues = new ContentValues();
        // 사진의 파일 이름 및 메타데이터 설정
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "photo_" + System.currentTimeMillis() + ".jpg"); // 파일 이름
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg"); // 파일 형식
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES); // 저장 위치 (갤러리)

        // MediaStore에 파일 삽입
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        try {
            if (uri != null) {
                // 이미지 데이터를 저장
                OutputStream outputStream = resolver.openOutputStream(uri);
                if (outputStream != null) {
                    outputStream.write(imageData);
                    outputStream.close();
                    uploadImageToServer(imageData,inputLanguage,resultLanguage);
                    // 저장 완료 메시지
                    Toast.makeText(this, "사진이 갤러리에 저장되었습니다.", Toast.LENGTH_SHORT).show();
                    previewView.setVisibility(View.GONE);
                    capturedImage.setVisibility(View.VISIBLE);
                    capturedImage.setImageURI(uri);

                }

            }
        } catch (Exception e) {
            // 저장 실패 시 에러 메시지 출력
            Toast.makeText(this, "사진 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            System.out.println(e.getMessage());
        }
    }

    // 액티비티 종료 시 카메라 스레드 종료
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }

    private void getBytesFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e("TakePictureActivity", "InputStream is null");
                return;
            }

            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }

            byte[] imageData = byteBuffer.toByteArray();
            if (imageData == null || imageData.length == 0) {
                Log.e("TakePictureActivity", "ImageData is null or empty");
                Toast.makeText(this, "이미지 데이터가 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            System.out.println(imageData);

            uploadImageToServer(imageData, "한국어", resultLanguage);

        } catch (IOException e) {
            Log.e("TakePictureActivity", "IOException 발생: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "이미지 처리 실패", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Log.e("TakePictureActivity", "SecurityException 발생: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }


    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                // 🔥 저장된 언어 불러오기
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                resultLanguage = prefs.getString(KEY_LANGUAGE, "한국어");
                Log.d("LanguageOnImagePick", "이미지 선택 시 언어: " + resultLanguage);

                try {
                    // 🔥 선택한 이미지 URI를 TakePictureActivity로 전달
                    Intent intent = new Intent(getApplicationContext(), TakePictureActivity.class);
                    intent.putExtra("selectedImageUri", selectedImageUri.toString());
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "이미지 처리 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public static class Coordinates {
        @SerializedName("x")
        private int x;

        @SerializedName("y")
        private int y;

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }

    public class TranslatedResultsResponse {
        @SerializedName("translated_text_data")
        private TranslatedTextData translatedTextData;

        public TranslatedTextData getTranslatedTextData() {
            return translatedTextData;
        }
    }

    public class TranslatedTextData {
        private List<TextItem> text;
        private List<TableItem> table;
        private List<ListItem> list;

        public List<TextItem> getText() {
            return text;
        }

        public List<TableItem> getTable() {
            return table;
        }

        public List<ListItem> getList() {
            return list;
        }
    }

    public class TextItem {
        private String content;
        private String translated;
        private double x;
        private double y;

        public String getContent() {
            return content;
        }

        public String getTranslated() {
            return translated;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }

    public class ListItem {
        private String name;
        private String translated;
        private double x;
        private double y;

        public String getName() {
            return name;
        }

        public String getTranslated() {
            return translated;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }

    public class TableItem {
        private List<String> headers;
        private List<TableRow> rows;

        public Collection<? extends String> getHeaders() {
            return headers;
        }

        public List<TableRow> getRows() {
            return rows;
        }
    }

    public class TableRow {
        private List<String> data;
        private Coordinates coordinates;

        public List<String> getData() {
            return data;
        }

        public Coordinates getCoordinates() {
            return coordinates;
        }
    }

    private void fetchTranslatedResults() {
        Call<TranslatedResultsResponse> call = apiService.getTranslatedResults();
        call.enqueue(new Callback<TranslatedResultsResponse>() {
            @Override
            public void onResponse(Call<TranslatedResultsResponse> call, Response<TranslatedResultsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TranslatedTextData data = response.body().getTranslatedTextData();

                    // 🔥 기존 데이터 삭제 (중복 방지)
                    textList.clear();
                    xList.clear();
                    yList.clear();

                    // Text 항목 추가
                    List<TextItem> textItems = data.getText();
                    if (textItems != null) {
                        for (TextItem item : textItems) {
                            String translatedText = item.getTranslated();
                            // 🔥 중복 체크 후 추가
                            if (!textList.contains(translatedText)) {
                                textList.add(translatedText);
                                xList.add(item.getX());  // 🔥 x 좌표 저장
                                yList.add(item.getY());  // 🔥 y 좌표 저장
                            }
                            Log.d("TranslatedResults", "Text: " + item.getContent() +
                                    " -> " + translatedText +
                                    " (x:" + item.getX() + ", y:" + item.getY() + ")");
                        }
                    }

                    // List 항목 추가
                    List<ListItem> listItems = data.getList();
                    if (listItems != null) {
                        for (ListItem item : listItems) {
                            String translatedList = item.getTranslated();
                            if (!textList.contains(translatedList)) {
                                textList.add(translatedList);
                                xList.add(item.getX());  // 🔥 x 좌표 저장
                                yList.add(item.getY());  // 🔥 y 좌표 저장
                            }
                            Log.d("TranslatedResults", "List: " + item.getName() +
                                    " -> " + translatedList +
                                    " (x:" + item.getX() + ", y:" + item.getY() + ")");
                        }
                    }

                    // Table 항목 추가
                    List<TableItem> tableItems = data.getTable();
                    if (tableItems != null) {
                        for (TableItem table : tableItems) {
                            // Table Header 추가
                            textList.addAll(table.getHeaders());
                            for (String header : table.getHeaders()) {
                                xList.add(0.0);  // 🔥 Header의 x 좌표 (임의값)
                                yList.add(0.0);  // 🔥 Header의 y 좌표 (임의값)
                            }
                            Log.d("TranslatedResults", "Table Headers: " + table.getHeaders());

                            // Table Row 추가
                            for (TableRow row : table.getRows()) {
                                List<String> rowData = row.getData();
                                textList.addAll(rowData);
                                for (String data1  : rowData) {
                                    xList.add(row.getCoordinates().getX());  // 🔥 x 좌표 저장
                                    yList.add(row.getCoordinates().getY());  // 🔥 y 좌표 저장
                                }
                                Log.d("TranslatedResults", "Row Data: " + rowData +
                                        " Coordinates: (x:" + row.getCoordinates().getX() +
                                        ", y:" + row.getCoordinates().getY() + ")");
                            }
                        }
                    }

                } else {
                    Log.e("TranslatedResults", "Response Error: " + response.errorBody());
                }
//                ArrayList<PointF> pointList = new ArrayList<>();
//                for (int i = 0; i < xList.size(); i++) {
//                    float x = xList.get(i).floatValue(); // Double -> float 변환
//                    float y = yList.get(i).floatValue(); // Double -> float 변환
//                    PointF point = new PointF(x, y);
//                    pointList.add(point);
//                }
                removeDuplicates();

            }

            @Override
            public void onFailure(Call<TranslatedResultsResponse> call, Throwable t) {
                Log.e("TranslatedResults", "Network request failed", t);
            }
        });
    }


    // 서버로 이미지 업로드
    private void uploadImageToServer(byte[] imageData, String input, String result) {

        if (imageData == null || imageData.length == 0) {
            Log.e("Upload Error", "ImageData is null or empty");
            Toast.makeText(this, "이미지 데이터가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (result == null || result.isEmpty()) {
            Log.e("Upload Error", "Result Language is null or empty");
            Toast.makeText(this, "번역할 언어를 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        System.out.println("성공");
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        RequestBody requestBody = RequestBody.Companion.create(MediaType.parse("image/jpeg"), imageData);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", "image.jpg", requestBody);

        RequestBody inputText = RequestBody.create(MediaType.parse("text/plain"), "한국어");
        RequestBody resultText = RequestBody.create(MediaType.parse("text/plain"), resultLanguage);

        Call<Void> call = apiService.uploadImage(body, inputText, resultText);
        showLoading();
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    hideLoading();
                    Toast.makeText(TakePictureActivity.this, "이미지 업로드 성공!", Toast.LENGTH_SHORT).show();
                    hideLoading();
                    fetchTranslatedResults();
                } else try {
                    String errorResponse = response.errorBody().string();
                    Log.e("Upload Error", "서버 응답 실패: " + errorResponse);
                    Toast.makeText(TakePictureActivity.this, "업로드 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(TakePictureActivity.this, "업로드 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔥 중복 제거 및 정렬 메서드
    private void removeDuplicates() {
        LinkedHashSet<String> set = new LinkedHashSet<>(textList);
        ArrayList<String> newTextList = new ArrayList<>(set);
        ArrayList<Double> newXList = new ArrayList<>();
        ArrayList<Double> newYList = new ArrayList<>();

        for (String text : newTextList) {
            int index = textList.indexOf(text);
            if (index != -1) {
                newXList.add(xList.get(index));
                newYList.add(yList.get(index));
            }
        }

        textList.clear();
        textList.addAll(newTextList);

        xList.clear();
        xList.addAll(newXList);

        yList.clear();
        yList.addAll(newYList);

        Log.d("DuplicateRemoval", "중복 제거 후 textList: " + textList);
        Log.d("DuplicateRemoval", "중복 제거 후 xList: " + xList);
        Log.d("DuplicateRemoval", "중복 제거 후 yList: " + yList);
        OverlayImageView capturedImage =   findViewById(R.id.capturedImageView);
        ArrayList<PointF> pointList = new ArrayList<>();
        for (int i = 0; i < xList.size(); i++) {
            float x = xList.get(i).floatValue(); // Double -> float 변환
            float y = yList.get(i).floatValue(); // Double -> float 변환
            PointF point = new PointF(x, y);
            pointList.add(point);
        }
        capturedImage.setOverlayTexts(textList, pointList);
    }


    // 🔥 이미지 데이터 비동기 로드
    // 🔥 이미지 데이터 비동기 로드 및 서버 전송
//    private void loadImageDataAsync(Uri uri) {
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        executor.execute(() -> {
//            try {
//                // 🔥 백그라운드 스레드에서 이미지 데이터 로드
//                byte[] imageData = getBytesFromUri(uri);
//
//                // 🔥 getBytesFromUri()가 완전히 종료된 후에 실행
//                if (imageData != null && imageData.length > 0) {
//                    // 🔥 메인 스레드에서 서버로 전송
//                    runOnUiThread(() -> uploadImageToServer(imageData, inputLanguage, resultLanguage));
//                } else {
//                    runOnUiThread(() -> Toast.makeText(TakePictureActivity.this, "이미지 데이터가 없습니다.", Toast.LENGTH_SHORT).show());
//                    Log.e("TakePictureActivity", "loadImageDataAsync: imageData가 null이거나 비어 있습니다.");
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//                runOnUiThread(() -> Toast.makeText(TakePictureActivity.this, "이미지 처리 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
//            }
//        });
//    }




    private void showLoading() {
        loadingSpinner=findViewById(R.id.loadingSpinner);
        loadingSpinner.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        loadingSpinner=findViewById(R.id.loadingSpinner);
        loadingSpinner.setVisibility(View.GONE);
    }

}
