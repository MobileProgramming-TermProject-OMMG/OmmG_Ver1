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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class MainActivity extends FragmentActivity implements View.OnClickListener {
    public static final AuthorizationResult.RESULT_CODE SUCCESS = null;
    private BackPressCloseHandler backPressCloseHandler;
    private FirebaseAuth mAuth;
    private final String TAG ="????????????";
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    private ViewPager2 mPager;
    private FragmentStateAdapter pagerAdapter;
    private int num_page = 3;
    public RecyclerView recyclerView;
    private View yoon;
    private ItemAdapter itemAdapter;
    AlbumAdapter mAlbumAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

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
    public static Object context_main;
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
        context_main=this;


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
        //???????????????
        sortDistance.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                itemAdapter.setDistance(u_GeoPoint);
                itemAdapter.notifyDataSetChanged();
                recyclerView.startLayoutAnimation();
                int i=itemAdapter.getItemCount();
                Log.d("??????", Integer.toString(i));
            }
        });
        //?????????
        sortTime.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
              itemAdapter.sortTime();
              itemAdapter.notifyDataSetChanged();
              recyclerView.startLayoutAnimation();
            }
        });
        //???????????????
        mypageb.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(MainActivity.this, MypageActivity.class);
                startActivity(intent);
            }
        });

        //1??? ??? ?????? ??????(2?????? ?????????)
        firstView();
        secondView();
        //2??? ??? ???????????? ?????????
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


    public String getCurrentAddress(double latitude, double longitude) {

        //????????????... GPS??? ????????? ??????
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {

            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            //???????????? ??????
            Toast.makeText(this, "???????????? ????????? ????????????", Toast.LENGTH_LONG).show();
            return "???????????? ????????? ????????????";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "????????? GPS ??????", Toast.LENGTH_LONG).show();
            return "????????? GPS ??????";

        }


        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "?????? ?????????", Toast.LENGTH_LONG).show();
            return "?????? ?????????";

        }

        Address address = addresses.get(0);
        return address.getAddressLine(0).toString() + "\n";

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
        //?????? ??? ?????? ?????????
        //itemAdapter.removeAllItem();
        //?????? ????????? ??????

        //????????????
        swipeRefreshLayout = findViewById(R.id.swipe);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                itemAdapter.removeAllItem();
                itemAdapter.notifyDataSetChanged();
                recyclerView.startLayoutAnimation();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        //??????
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
        //152??????

        db.collection("items").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    for(QueryDocumentSnapshot document:task.getResult()){
                        Item item=document.toObject(Item.class);
                        itemAdapter.addItem(item);
                        Log.d("??????",document.getId()+"=>"+document.getData());
                    }
                }
                else
                {
                    Log.d("??????","??? ?????????",task.getException());
                }
            }
        });

    }


    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.plus:
                anim();
                Intent post = new Intent(getApplicationContext(), PostActivity.class);
                startActivity(post);
                //Toast.makeText(this, "plus", Toast.LENGTH_SHORT).show();
                break;
            case R.id.camera:
                anim();
                //Toast.makeText(this, "camera", Toast.LENGTH_SHORT).show();
                //captureCamera();
                break;
            case R.id.album:
                anim();
                //Toast.makeText(this, "album", Toast.LENGTH_SHORT).show();
                //getAlbum();
                break;
            case R.id.upload:
                anim();
                //Toast.makeText(this, "upload", Toast.LENGTH_SHORT).show();
                //loadAlbum();
                break;
        }
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

