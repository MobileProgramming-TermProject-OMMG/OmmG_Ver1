package com.example.lastommg;

import static android.view.View.GONE;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.ktx.Firebase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.kakao.auth.authorization.AuthorizationResult;
import com.kakao.network.ErrorResult;
import com.kakao.sdk.user.UserApiClient;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;
import com.kakao.usermgmt.callback.UnLinkResponseCallback;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class MainActivity extends FragmentActivity implements View.OnClickListener {
    public static final AuthorizationResult.RESULT_CODE SUCCESS = null;
    private BackPressCloseHandler backPressCloseHandler;
    private FirebaseAuth mAuth;
    private final String TAG ="로그아웃";
    FirebaseFirestore db = FirebaseFirestore.getInstance();


    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    private ViewPager2 mPager;
    private FragmentStateAdapter pagerAdapter;
    private int num_page = 3;
    private RecyclerView recyclerView;
    private View yoon;
    private ItemAdapter itemAdapter;
    private AlbumAdapter mAlbumAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final int MULTIPLE_PERMISSIONS = 1111;
    private static final int REQUEST_TAKE_PHOTO = 2222;
    private static final int REQUEST_TAKE_ALBUM = 3333;
    private static final int REQUEST_IMAGE_CROP = 4444;
    private static final int REQUEST_UPLOAD = 5555;
    private Animation fab_open, fab_close;
    private Boolean isFabOpen = false;
    private FloatingActionButton plus, camera, album, upload;
    private FirebaseStorage storage;

    //ImageView iv_view;
    String address;
    String mCurrentPhotoPath;
    double latitude;
    double longitude;
    private GpsTracker gpsTracker;
    GeoPoint u_GeoPoint;
    Uri imageUri;
    Uri photoURI, albumURI;

    AccessToken accessToken = AccessToken.getCurrentAccessToken();
    boolean isLoggedIn = accessToken != null && !accessToken.isExpired();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAlbumAdapter = new AlbumAdapter();
        gpsTracker = new GpsTracker(MainActivity.this);
        latitude = gpsTracker.getLatitude();
        longitude = gpsTracker.getLongitude();
        u_GeoPoint = new GeoPoint(latitude, longitude);
        yoon = findViewById(R.id.plus);

        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        } else {

            checkRunTimePermission();
        }

        mAuth = FirebaseAuth.getInstance();

        backPressCloseHandler = new BackPressCloseHandler(this);
        Button sortDistance = findViewById((R.id.sortdistance));
        Button sortTime = findViewById((R.id.sorttime));
        Button logoutb = findViewById((R.id.logoutbt));
        Button loginb = findViewById((R.id.loginbt));
        Button mypageb = findViewById(R.id.mypage_button);
        logoutb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                signOut();
                logoutb.setVisibility(GONE);
                loginb.setVisibility(View.VISIBLE);
            }
        });

        loginb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
        //거리순정렬
        sortDistance.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                itemAdapter.setDistance(u_GeoPoint);
                itemAdapter.notifyDataSetChanged();
                recyclerView.startLayoutAnimation();
            }
        });
        //시간순
        sortTime.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
              itemAdapter.sortTime();
              itemAdapter.notifyDataSetChanged();
              recyclerView.startLayoutAnimation();
            }
        });
        //마이페이지
        mypageb.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(MainActivity.this, MypageActivity.class);
                startActivity(intent);
            }
        });

        //1번 탭 미리 표시(2번도 활성화)
        firstView();
        secondView();
        //2번 탭 구성요소 가리기
        yoon.setVisibility((View.INVISIBLE));
        recyclerView.setVisibility(View.INVISIBLE);
        swipeRefreshLayout.setVisibility((View.INVISIBLE));
        //Tab implant
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                changeView(pos);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // do nothing
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // do nothing
            }
        });
    }

    //GPS
    void checkRunTimePermission() {

        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);


        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {

            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)


            // 3.  위치 값을 가져올 수 있음


        } else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, REQUIRED_PERMISSIONS[0])) {

                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Toast.makeText(MainActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);


            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }

        }

    }


    public String getCurrentAddress(double latitude, double longitude) {

        //지오코더... GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {

            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";

        }


        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";

        }

        Address address = addresses.get(0);
        return address.getAddressLine(0).toString() + "\n";

    }


    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }


    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    private void signOut() {
        mAuth.signOut();
        if (isLoggedIn == true) {
            LoginManager.getInstance().logOut();
        }
        UserManagement.getInstance().requestLogout(new LogoutResponseCallback() {
            @Override
            public void onCompleteLogout() {
                FirebaseAuth.getInstance().signOut();
                Log.d(TAG,"kakao logout");

            }
        });
    }

    public void onBackPressed() {
        //super.onBackPressed();
        backPressCloseHandler.onBackPressed();
    }


    private void changeView(int index) {
        switch (index) {
            case 0:
                mPager.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.INVISIBLE);
                yoon.setVisibility((View.INVISIBLE));
                swipeRefreshLayout.setVisibility((View.INVISIBLE));
                break;
            case 1:
                recyclerView.setVisibility(View.VISIBLE);
                yoon.setVisibility((View.VISIBLE));
                swipeRefreshLayout.setVisibility((View.VISIBLE));
                mPager.setVisibility(View.INVISIBLE);
                itemAdapter.notifyDataSetChanged();
                recyclerView.startLayoutAnimation();
                break;
        }
    }

    private void firstView() {
        //ViewPager2
        mPager = findViewById(R.id.viewpager);
        //Adapter
        pagerAdapter = new MyAdapter(this, num_page);
        mPager.setAdapter(pagerAdapter);
        //ViewPager Setting
        mPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
        mPager.setCurrentItem(1000);
        mPager.setOffscreenPageLimit(3);
        //Change in position -> change in view page
        mPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                if (positionOffsetPixels == 0) {
                    mPager.setCurrentItem(position);
                }
            }
        });
    }

    private void secondView() {
        recyclerView = findViewById(R.id.recycler_view);
        itemAdapter = new ItemAdapter();
        recyclerView.setAdapter(itemAdapter);
        //조회 전 화면 클리어
        //itemAdapter.removeAllItem();
        //샘플 데이터 생성

        //새로고침
        swipeRefreshLayout = findViewById(R.id.swipe);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                itemAdapter.notifyDataSetChanged();
                recyclerView.startLayoutAnimation();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        //윤수
        fab_open = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);

        plus = (FloatingActionButton) findViewById(R.id.plus);
        camera = (FloatingActionButton) findViewById(R.id.camera);
        album = (FloatingActionButton) findViewById(R.id.album);
        upload = (FloatingActionButton) findViewById(R.id.upload);
        //iv_view = (ImageView) findViewById(R.id.iv_view);

        storage = FirebaseStorage.getInstance();

        plus.setOnClickListener((View.OnClickListener) this);
        camera.setOnClickListener((View.OnClickListener) this);
        album.setOnClickListener((View.OnClickListener) this);
        upload.setOnClickListener((View.OnClickListener) this);
        checkPermission();
        //152까지

        db.collection("items").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    for(QueryDocumentSnapshot document:task.getResult()){
                        Item item=document.toObject(Item.class);
                        itemAdapter.addItem(item);
                        Log.d("확인",document.getId()+"=>"+document.getData());
                    }
                }
                else
                {
                    Log.d("실패","응 실패야",task.getException());
                }
            }
        });

    }

    private void checkPermission() {
        int result;
        List<String> permissionList = new ArrayList<>();
        for (String pm : PERMISSIONS) {
            result = ContextCompat.checkSelfPermission(this, pm);
            if (result != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(pm);
            }
        }

        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), MULTIPLE_PERMISSIONS);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.length == REQUIRED_PERMISSIONS.length) {
            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면

            boolean check_result = true;


            // 모든 퍼미션을 허용했는지 체크합니다.

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }


            if (check_result) {

                //위치 값을 가져올 수 있음
                ;
            } else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {

                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show();
                    finish();


                } else {

                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ", Toast.LENGTH_LONG).show();

                }
            }
        }
        if (requestCode == MULTIPLE_PERMISSIONS) {
            if (grantResults.length > 0) {
                for (int i = 0; i < permissions.length; i++) {
                    if (permissions[i].equals(this.PERMISSIONS[0])) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            if ((ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSIONS[0]))) {
                                new AlertDialog.Builder(this)
                                        .setTitle("알림")
                                        .setMessage("카메라 권한이 거부되었습니다. 사용을 원하시면 설정에서 해당 권한을 직접 허용하셔야 합니다.")
                                        .setNeutralButton("설정", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                intent.setData(Uri.parse("package:" + getPackageName()));
                                                startActivity(intent);
                                            }
                                        })
                                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                finish();
                                            }
                                        })
                                        .setCancelable(false)
                                        .create()
                                        .show();
                            }
                        }
                    } else if (permissions[i].equals(this.PERMISSIONS[1])) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            if ((ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSIONS[1]))) {
                                new AlertDialog.Builder(this)
                                        .setTitle("알림")
                                        .setMessage("저장소 권한이 거부되었습니다. 사용을 원하시면 설정에서 해당 권한을 직접 허용하셔야 합니다.")
                                        .setNeutralButton("설정", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                intent.setData(Uri.parse("package:" + getPackageName()));
                                                startActivity(intent);
                                            }
                                        })
                                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                finish();
                                            }
                                        })
                                        .setCancelable(false)
                                        .create()
                                        .show();
                            }
                        }
                    } else if (permissions[i].equals(this.PERMISSIONS[2])) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            if ((ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSIONS[2]))) {
                                new AlertDialog.Builder(this)
                                        .setTitle("알림")
                                        .setMessage("위치 권한이 거부되었습니다. 사용을 원하시면 설정에서 해당 권한을 직접 허용하셔야 합니다.")
                                        .setNeutralButton("설정", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                intent.setData(Uri.parse("package:" + getPackageName()));
                                                startActivity(intent);
                                            }
                                        })
                                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                finish();
                                            }
                                        })
                                        .setCancelable(false)
                                        .create()
                                        .show();
                            }
                        } else if (permissions[i].equals(this.PERMISSIONS[3])) {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                if ((ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSIONS[3]))) {
                                    new AlertDialog.Builder(this)
                                            .setTitle("알림")
                                            .setMessage("위치 권한이 거부되었습니다. 사용을 원하시면 설정에서 해당 권한을 직접 허용하셔야 합니다.")
                                            .setNeutralButton("설정", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                    intent.setData(Uri.parse("package:" + getPackageName()));
                                                    startActivity(intent);
                                                }
                                            })
                                            .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    finish();
                                                }
                                            })
                                            .setCancelable(false)
                                            .create()
                                            .show();
                                }
                            }
                        }
                    }
                    return;
                }
            }
        }
    }


    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.plus:
                anim();
                //Toast.makeText(this, "plus", Toast.LENGTH_SHORT).show();
                break;
            case R.id.camera:
                anim();
                //Toast.makeText(this, "camera", Toast.LENGTH_SHORT).show();
                captureCamera();
                break;
            case R.id.album:
                anim();
                //Toast.makeText(this, "album", Toast.LENGTH_SHORT).show();
                getAlbum();
                break;
            case R.id.upload:
                anim();
                //Toast.makeText(this, "upload", Toast.LENGTH_SHORT).show();
                loadAlbum();
                break;
        }
    }

    private void captureCamera() {
        String state = Environment.getExternalStorageState();
        // 외장 메모리 검사
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    Log.e("captureCamera Error", ex.toString());
                }
                if (photoFile != null) {
                    // getUriForFile의 두 번째 인자는 Manifest provider의 authorites와 일치해야 함

                    Uri providerURI = FileProvider.getUriForFile(this, getPackageName(), photoFile);
                    imageUri = providerURI;

                    // 인텐트에 전달할 때는 FileProvier의 Return값인 content://로만!!, providerURI의 값에 카메라 데이터를 넣어 보냄
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, providerURI);

                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                }
            }
        } else {
            Toast.makeText(this, "저장공간이 접근 불가능한 기기입니다", Toast.LENGTH_SHORT).show();
        }
    }

    public File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + ".jpg";
        File imageFile = null;
        File storageDir = new File(Environment.getExternalStorageDirectory() + "/Pictures", "ommg");

        if (!storageDir.exists()) {
            Log.i("mCurrentPhotoPath1", storageDir.toString());
            storageDir.mkdirs();
        }
        imageFile = new File(storageDir, imageFileName);
        mCurrentPhotoPath = imageFile.getAbsolutePath();

        return imageFile;
    }

    //CROP 용
    private void getAlbum() {
        Log.i("getAlbum", "Call");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent, REQUEST_TAKE_ALBUM);
    }

    //UPLOAD 용
    private void loadAlbum() {
        Log.i("loadAlbum", "Call");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent, REQUEST_UPLOAD);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        Log.i("REQUEST_TAKE_PHOTO", "OK");
                        galleryAddPic();

                        //iv_view.setImageURI(imageUri);
                    } catch (Exception e) {
                        Log.e("REQUEST_TAKE_PHOTO", e.toString());
                    }
                } else {
                    Toast.makeText(MainActivity.this, "촬영을 취소하였습니다.", Toast.LENGTH_SHORT).show();
                }
                break;

            case GPS_ENABLE_REQUEST_CODE: //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
                        checkRunTimePermission();
                        return;
                    }
                }
                break;
            case REQUEST_TAKE_ALBUM:
                if (resultCode == Activity.RESULT_OK) {
                    if (data.getData() != null) {
                        try {
                            File albumFile = null;
                            albumFile = createImageFile();
                            photoURI = data.getData();
                            albumURI = Uri.fromFile(albumFile);
                            cropImage();
                        } catch (Exception e) {
                            Log.e("TAKE_ALBUM_SINGLE ERROR", e.toString());
                        }
                    }
                }
                break;

            case REQUEST_IMAGE_CROP:
                if (resultCode == Activity.RESULT_OK) {
                    galleryAddPic();
                    //iv_view.setImageURI(albumURI);
                }
                break;

            case REQUEST_UPLOAD:
                if (resultCode == Activity.RESULT_OK) {
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
                    Timestamp timestamp=new Timestamp(new Date());
                    String imageFileName = "JPEG_" + timeStamp + ".jpg";
                    final String[] email = new String[1];
                    Uri file = data.getData();
                    StorageReference storageRef = storage.getReference();
                    StorageReference riversRef = storageRef.child("image/" + imageFileName);
                    UploadTask uploadTask = riversRef.putFile(file);

                    try {
                        InputStream in = getContentResolver().openInputStream(data.getData());
                        Bitmap img = BitmapFactory.decodeStream(in);
                        in.close();
                        //iv_view.setImageBitmap(img);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MainActivity.this, "사진 업로드가 실패하였습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            FirebaseStorage storage = FirebaseStorage.getInstance();
                            StorageReference storageReference = storage.getReference();
                            StorageReference pathReference = storageReference.child("image");
                            if (pathReference == null) {
                                Toast.makeText(MainActivity.this, "저장소에사진이없습니다.", Toast.LENGTH_SHORT).show();
                            } else {
                                StorageReference submitProfile = storageReference.child("image/" + imageFileName);
                                submitProfile.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        address=getCurrentAddress(u_GeoPoint.getLatitude(), u_GeoPoint.getLongitude());
                                        u_GeoPoint = new GeoPoint(latitude, longitude);
                                        Item item = new Item(mAuth.getUid(),imageFileName, uri.toString(), "010-9913-2992", u_GeoPoint,address,0.0,timestamp);
                                        itemAdapter.addItem(item);
                                        if(item.getId().equals(mAuth.getUid())) {
                                            mAlbumAdapter.addItem(item);
                                            Log.i("aaaaaa", String.valueOf(mAlbumAdapter.getItemCount()));
                                        }
                                        Log.i("확인", item.getGeoPoint().toString());
                                        db.collection("items").document(imageFileName).set(item);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                    }
                                });
                            }
                            itemAdapter.notifyDataSetChanged();
                            Toast.makeText(MainActivity.this, "사진 업로드가 성공하였습니다.", Toast.LENGTH_SHORT).show();
                            recyclerView.startLayoutAnimation();
                        }
                    });
                }
                break;
        }
    }

    private void galleryAddPic() {
        Log.i("galleryAddPic", "Call");
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        // 해당 경로에 있는 파일을 객체화(새로 파일을 만든다는 것으로 이해하면 안 됨)
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
        Toast.makeText(this, "사진이 앨범에 저장되었습니다.", Toast.LENGTH_SHORT).show();
    }

    // 카메라 전용 크랍
    public void cropImage() {
        Log.i("cropImage", "Call");
        Log.i("cropImage", "photoURI : " + photoURI + " / albumURI : " + albumURI);

        Intent cropIntent = new Intent("com.android.camera.action.CROP");

        // 50x50픽셀미만은 편집할 수 없다는 문구 처리 + 갤러리, 포토 둘다 호환하는 방법
        cropIntent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        cropIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        cropIntent.setDataAndType(photoURI, "image/*");
        //cropIntent.putExtra("outputX", 200); // crop한 이미지의 x축 크기, 결과물의 크기
        //cropIntent.putExtra("outputY", 200); // crop한 이미지의 y축 크기
        cropIntent.putExtra("aspectX", 1); // crop 박스의 x축 비율, 1&1이면 정사각형
        cropIntent.putExtra("aspectY", 1); // crop 박스의 y축 비율
        cropIntent.putExtra("scale", true);
        cropIntent.putExtra("output", albumURI); // 크랍된 이미지를 해당 경로에 저장
        startActivityForResult(cropIntent, REQUEST_IMAGE_CROP);
    }

    private void anim() {
        if (isFabOpen) {
            camera.startAnimation(fab_close);
            camera.setClickable(false);
            album.startAnimation(fab_close);
            album.setClickable(false);
            upload.startAnimation(fab_close);
            upload.setClickable(false);
            isFabOpen = false;
        } else {
            camera.startAnimation(fab_open);
            camera.setClickable(true);
            album.startAnimation(fab_open);
            album.setClickable(true);
            upload.startAnimation(fab_open);
            upload.setClickable(true);
            isFabOpen = true;
        }
    }
}

