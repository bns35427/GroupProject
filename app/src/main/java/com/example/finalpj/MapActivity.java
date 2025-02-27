package com.example.finalpj;

import static android.widget.Toast.LENGTH_SHORT;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.media3.common.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;

import org.checkerframework.checker.units.qual.A;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private FusedLocationProviderClient fusedLocationClient;
    private NaverMap mNaverMap;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String BASE_URL = "https://openapi.naver.com/";
    private static final String CLIENT_ID = "KfXy_Ysuq7btwrCpN9p9";  // 네이버 API Client ID 입력
    private static final String CLIENT_SECRET = "XQtsiiFEvk";  // 네이버 API Client Secret 입력
    private static final String GOOGLE_API_KEY = "AIzaSyBq48eJhRKDIO6zinAiTegmntHxG3G5cgw";
    private List<Marker> markers = new ArrayList<>();  // ✅ 마커 리스트 추가
    private ListViewAdapter lvAdapter;
    private ListView lv;



    boolean fac=true;
    ArrayList<String> selectFac;
    ArrayList<String> selectFacName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        //위치 정보 제공 동의
        checkPermissions();

        View bottomSheet = findViewById(R.id.bottom_sheet);
        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        //네이버 지도 SDK
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        NaverMapSdk.getInstance(this).setClient(new NaverMapSdk.NaverCloudPlatformClient("dmehfztcwm"));
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
        selectFac = new ArrayList<>();
        selectFacName=new ArrayList<>();
        lv=findViewById(R.id.lvId);
        lvAdapter=new ListViewAdapter(this,selectFacName);
        lv.setAdapter(lvAdapter);
        Button userBtn=findViewById(R.id.userBtn);
        userBtn.setOnClickListener(view ->{
            getCurrentLocationAndSearch("나");
        });
        Button restBtn = findViewById(R.id.restBtn);
        restBtn.setOnClickListener(view ->{
                clearAllMarkers();
                getCurrentLocationAndSearch("다문화");
                lvAdapter.notifyDataSetChanged();
        });
        Button hosBtn=findViewById(R.id.hosBtn);
        hosBtn.setOnClickListener(view -> {
            clearAllMarkers();
            getCurrentLocationAndSearch("대학 병원");
            lvAdapter.notifyDataSetChanged();
        });
        Button lawBtn=findViewById(R.id.cusBtn);
        Button checkBtn=findViewById(R.id.checkBtn);
        lawBtn.setOnClickListener(view -> {
            clearAllMarkers();
            getCurrentLocationAndSearch("상담소");
            lvAdapter.notifyDataSetChanged();
        });

        EditText inputId=findViewById(R.id.inputId);
        inputId.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    checkBtn.performClick(); // 버튼 클릭 실행
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(inputId.getWindowToken(), 0);
                    clearAllMarkers();
                    return true; // 이벤트 소비 (기본 엔터 동작 방지)
                }
                return false;
            }
        });
        checkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String inputText=inputId.getText().toString().trim();
                if(inputText.isEmpty()){
                    inputId.setError("다시 입력해주세오");
                    inputId.requestFocus();
                    return;
                }
                clearAllMarkers();
                getCurrentLocationAndSearch(inputText);
                lvAdapter.notifyDataSetChanged();
            }
        });
        lv.setOnItemClickListener((parent, view, position, id) -> {
            String selectedPlace = selectFac.get(position); // 선택된 아이템 이름 (시설명)
            NaverGeocoding.getCoordinates(selectedPlace, new NaverGeocoding.GeocodingCallback() {
                @Override
                public void onSuccess(double latitude, double longitude) {
                    moveCameraToLocation(latitude,longitude);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }

                @Override
                public void onFailure(String error) {

                }
            });

        });

// ✅ 초기 상태를 COLLAPSED로 설정 (중복 설정 제거)
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

// ✅ 적절한 peekHeight 값 설정 (터치 영역 확보)
        bottomSheetBehavior.setPeekHeight(300);

// ✅ 드래그 활성화
        bottomSheetBehavior.setDraggable(true);

// ✅ 완전히 숨겨지는 것 방지
        bottomSheetBehavior.setHideable(false);

// ✅ ListView가 터치 이벤트를 BottomSheetBehavior에 전달하도록 설정
        lv.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(false);
            return false;
        });

// 상태 변경 리스너 추가
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                View expandedContent = findViewById(R.id.bottom_sheet);

                if (expandedContent == null) {
                    Log.e("DEBUG", "❌ expanded_content 찾을 수 없음!");
                    return;
                }

                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    expandedContent.setVisibility(View.VISIBLE);
                    Log.d("DEBUG", "✅ BottomSheet 상태: EXPANDED, 리스트뷰 유지됨");
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    // ✅ 리스트뷰를 숨기지 않음
                    expandedContent.setVisibility(View.VISIBLE);
                    Log.d("DEBUG", "✅ BottomSheet 상태: COLLAPSED, 리스트뷰 유지됨");
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // 슬라이드 애니메이션 효과 (선택 사항)
            }
        });

    }


    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        mNaverMap = naverMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                // ✅ 지도 카메라를 현재 위치로 이동
                LatLng currentLocation = new LatLng(latitude, longitude);
                CameraUpdate cameraUpdate = CameraUpdate.scrollTo(currentLocation);
                mNaverMap.moveCamera(cameraUpdate);
                // ✅ 현재 위치에 마커 추가
                Marker marker = new Marker();
                marker.setPosition(currentLocation);  // 마커 위치 설정
                marker.setMap(mNaverMap);  // 네이버 맵에 마커 추가
                naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_TRANSIT, true); // 실시간 대중교통 정보 표시
// 실시간 대중교통 정보 표시

            }
        });
//        clearAllMarkers();
//        LatLng gyeongbokgung = new LatLng(lati, lon);
//
//        // ✅ 지도 카메라를 경복궁으로 이동
//        CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(gyeongbokgung, 14);
//        mNaverMap.moveCamera(cameraUpdate);
//
//        // ✅ 경복궁 위치에 마커 추가
//        Marker marker = new Marker();
//        marker.setPosition(gyeongbokgung);
//        marker.setCaptionText("경복궁");  // 마커에 텍스트 추가
//        marker.setMap(mNaverMap);
    }


    private void getCurrentLocationAndSearch(String facility) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                if(facility.equals("나")){
                    moveCameraToLocation(latitude, longitude);
                }
                else{
                    getAddressFromLocation(latitude,longitude ,facility);
                    moveCameraToLocation(latitude, longitude);
                }
            } else {
                Toast.makeText(this, "위치 정보를 가져올 수 없습니다.", LENGTH_SHORT).show();
            }
        });
    }

    private void getAddressFromLocation(double latitude, double longitude, String facility) {
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses == null || addresses.isEmpty()) {
                    Log.e("DEBUG", "❌ 주소를 찾을 수 없음");
                    return;
                }

                runOnUiThread(() -> {
                    Address address = addresses.get(0);
                    if (address == null) {
                        Log.e("DEBUG", "❌ Address 객체가 NULL임");
                        return;
                    }

                    String fullAddress = address.getAddressLine(0);
                    if (fullAddress == null || fullAddress.isEmpty()) {
                        Log.e("DEBUG", "❌ 주소 문자열이 NULL임");
                        return;
                    }

                    Log.d("Location", "현재 주소: " + fullAddress);

                    List<String> addressComponents = new ArrayList<>(Arrays.asList(fullAddress.split(" ")));

                    // ✅ 주소 단계를 줄여가면서 검색 시도
                    selectFac.clear();
                    selectFacName.clear();
                    for (int i = 0; i < 3; i++) {  // 최대 3회 반복
                        if (addressComponents.size() <= 1) break;  // 주소를 더 줄일 수 없으면 종료

                        String searchArea = String.join(" ", addressComponents);
                        boolean found = searchNearbyPlaces(searchArea, facility);

                        if (found) {
                            Log.d("DEBUG", "✅ " + searchArea + "에서 시설을 찾음");
                            break;
                        } else {
                            Log.d("DEBUG", "❌ " + searchArea + "에서 시설을 찾지 못함, 범위 축소");
                            addressComponents.remove(addressComponents.size() - 1); // 마지막 단어 삭제 후 다시 검색
                        }
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("DEBUG", "주소 검색 중 오류 발생: " + e.getMessage());
            }
        }).start();
    }



    private void moveCameraToLocation(double latitude, double longitude) {
        LatLng currentLocation = new LatLng(latitude, longitude);
        CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(currentLocation,12);
        mNaverMap.moveCamera(cameraUpdate);
    }

    private boolean searchNearbyPlaces(String address, String facility) {
        Log.d("DEBUG", "API 요청 시작: " + address + " " + facility);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        String searchQuery = address + " " + facility;
        Call<NaverSearchResponse> call = apiService.getSearchResults(
                CLIENT_ID, CLIENT_SECRET, searchQuery, 10, 1, "random");

        final boolean[] found = {false};  // 검색 성공 여부 저장

        call.enqueue(new Callback<NaverSearchResponse>() {
            @Override
            public void onResponse(Call<NaverSearchResponse> call, Response<NaverSearchResponse> response) {
                Log.d("DEBUG", "API 응답 코드: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    List<NaverSearchResponse.NaverItem> items = response.body().items;
                    if (items != null && !items.isEmpty()) {
                        found[0] = true;
                        Log.d("DEBUG", "✅ 검색 결과 개수: " + items.size());
                        runOnUiThread(() -> {
                            for (NaverSearchResponse.NaverItem item : items) {
                                String facilityName =  item.title.replaceAll("<[^>]*>", "");;
                                String facilityAddress = item.address;
                                Log.d("DEBUG", "시설명: " + facilityName + ", 주소: " + facilityAddress);
//                                addMarkerFromAddress(facilityAddress, facilityName);
                                selectFac.add(facilityAddress);
                                selectFacName.add(facilityName);
                                addMarkerFromAddress(facilityAddress,facilityName);
                            }
                            lvAdapter.notifyDataSetChanged();
                        });
                    } else {
                        Log.e("DEBUG", "❌ 검색 결과 없음");
                    }
                } else {
                    Log.e("DEBUG", "API 요청 실패! Response Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<NaverSearchResponse> call, Throwable t) {
                Log.e("DEBUG", "API 요청 오류 발생: " + t.getMessage());
            }
        });

        return found[0];  // 검색 성공 여부 반환
    }


    private void addMarkerFromAddress(String address,String name) {
        new Thread(() -> {
//            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
            //                List<Address> addresses = geocoder.getFromLocationName(address, 1);
//                List<Address> addresses=
//                if (addresses == null || addresses.isEmpty()) {
//                    Log.e("DEBUG", "❌ 주소 변환 실패: " + address);
//                    return;
//                }

            NaverGeocoding.getCoordinates(address, new NaverGeocoding.GeocodingCallback() {
                @Override
                public void onSuccess(double latitude, double longitude) {

                    runOnUiThread(() -> {
                        LatLng facilityLocation = new LatLng(latitude, longitude);
                        Marker marker = new Marker();
                        marker.setPosition(facilityLocation);
                        marker.setMap(mNaverMap);
                        markers.add(marker);
                        marker.setCaptionText(name);


                        // ✅ 시설 마커 추가 후에만 토스트 표시
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        }, 500); // 500ms (0.5초) 지연 후 실행
                    });
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(MapActivity.this, "마커 실패", LENGTH_SHORT).show();
                    System.out.println(error);
                }
            });
//                if (location == null) {
//                    Log.e("DEBUG", "❌ 변환된 주소가 NULL임: " + address);
//                    return;
//                }

        }).start();
    }


    private void clearAllMarkers() {
        for (Marker marker : markers) {
            marker.setMap(null);  // ✅ 네이버 지도에서 마커 제거
        }
        markers.clear();  // ✅ 리스트 초기화
    }

    private void getCoordinatesFromNaver(String address,String name) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://naveropenapi.apigw.ntruss.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService service = retrofit.create(ApiService.class);

        Call<NaverGeocodeResponse> call = service.getGeocode(
                "dmehfztcwm",   // 🔹 발급받은 Client ID 입력
                "1fHyDhLeOelursIdXNItKXNfR7RhbVLNshI0mVVr", // 🔹 발급받은 Client Secret 입력
                address
        );

        call.enqueue(new Callback<NaverGeocodeResponse>() {
            @Override
            public void onResponse(Call<NaverGeocodeResponse> call, Response<NaverGeocodeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<NaverGeocodeResponse.AddressItem> addresses = response.body().addresses;
                    if (!addresses.isEmpty()) {
                        double latitude = Double.parseDouble(addresses.get(0).latitude);
                        double longitude = Double.parseDouble(addresses.get(0).longitude);

                        Log.d("NAVER_API", "✅ 주소 변환 성공: " + address + " -> " + latitude + ", " + longitude);

                        // ✅ 지도에서 마커 추가
                        runOnUiThread(() -> {
                            addMarkerFromAddress(address,name);
                            LatLng targetLocation = new LatLng(latitude, longitude);
                            CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(targetLocation, 14); // 줌 레벨 14로 이동
                            mNaverMap.moveCamera(cameraUpdate);
                        });
                    } else {
                        Log.e("NAVER_API", "❌ 주소 변환 실패 (검색 결과 없음)");
                    }
                } else {
                    Log.e("NAVER_API", "❌ API 응답 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<NaverGeocodeResponse> call, Throwable t) {
                Log.e("NAVER_API", "❌ API 요청 오류 발생: " + t.getMessage());
            }
        });
    }



    public static class NaverSearchResponse {
        @SerializedName("items")
        public List<NaverItem> items;

        public static class NaverItem {
            @SerializedName("title")
            public String title;
            @SerializedName("link")
            public String link;
            @SerializedName("category")
            public String category;
            @SerializedName("description")
            public String description;
            @SerializedName("address")
            public String address;
        }
    }
    public static class NaverGeocodeResponse {
        @SerializedName("addresses")
        public List<AddressItem> addresses;

        public static class AddressItem {
            @SerializedName("roadAddress")
            public String roadAddress;

            @SerializedName("x")  // 경도 (Longitude)
            public String longitude;

            @SerializedName("y")  // 위도 (Latitude)
            public String latitude;
        }
    }



    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("DEBUG", "✅ 위치 권한 허용됨");
            } else {
                Log.e("DEBUG", "❌ 위치 권한 거부됨");
                Toast.makeText(this, "위치 권한이 필요합니다.", LENGTH_SHORT).show();
            }
        }
    }
    public static class NaverGeocoding {
        private static final String CLIENT_ID = "YOUR_NAVER_CLIENT_ID"; // 네이버 API Client ID
        private static final String CLIENT_SECRET = "YOUR_NAVER_CLIENT_SECRET"; // 네이버 API Client Secret

        public interface GeocodingCallback {
            void onSuccess(double latitude, double longitude);
            void onFailure(String error);
        }

        public static void getCoordinates(String address, GeocodingCallback callback) {
            new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    String query = params[0];
                    String apiUrl = "https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode?query=" + query;

                    try {
                        URL url = new URL(apiUrl);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setRequestProperty("X-NCP-APIGW-API-KEY-ID", "dmehfztcwm");
                        conn.setRequestProperty("X-NCP-APIGW-API-KEY", "1fHyDhLeOelursIdXNItKXNfR7RhbVLNshI0mVVr");

                        int responseCode = conn.getResponseCode();
                        if (responseCode == 200) { // 정상 응답
                            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            StringBuilder response = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) {
                                response.append(line);
                            }
                            br.close();
                            return response.toString();
                        } else {
                            return null;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(String result) {
                    if (result != null) {
                        try {
                            JSONObject jsonResponse = new JSONObject(result);
                            JSONArray addresses = jsonResponse.getJSONArray("addresses");
                            if (addresses.length() > 0) {
                                JSONObject firstAddress = addresses.getJSONObject(0);
                                double latitude = firstAddress.getDouble("y");
                                double longitude = firstAddress.getDouble("x");
                                callback.onSuccess(latitude, longitude);
                            } else {
                                callback.onFailure("주소를 찾을 수 없습니다.");
                            }
                        } catch (Exception e) {
                            callback.onFailure("JSON 파싱 오류: " + e.getMessage());
                        }
                    } else {
                        callback.onFailure("네이버 API 요청 실패");
                        System.out.println();
                    }
                }
            }.execute(address);
        }
    }

}
