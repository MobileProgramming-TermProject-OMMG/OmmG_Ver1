package com.example.lastommg;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MypageActivity extends AppCompatActivity implements AlbumAdapter.OnItemClickListener, View.OnClickListener, Serializable {

    private Context mContext;
    private RecyclerView my_album;
    private AlbumAdapter mAlbumAdapter;

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseAuth mAuth;
    private static final int PICK_FROM_CAMERA = 0;
    private static final int PICK_FROM_ALBUM = 1;
    private static final int CROP_FROM_CAMERA = 2;
    private Uri mImageCaptureUri;
    private RoundImageView mPressProfileImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mypage);
        mAlbumAdapter = new AlbumAdapter();
        mAuth = FirebaseAuth.getInstance();
    //????????? ????????? ?????????(????????????)
        mPressProfileImg = findViewById(R.id.round_profile_image);
        mPressProfileImg.setOnClickListener(this);

//        RoundImageView riv = findViewById(R.id.round_profile_image);
//        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.profile_img);
//        riv.setImageBitmap(bm);
    //????????? ?????? ?????????
        TextView nameSlot = findViewById(R.id.name);
        TextView emailSlot = findViewById(R.id.email);
        TextView introduction = findViewById(R.id.intro);
        nameSlot.setText("MinHyugi");
        emailSlot.setText("minhyuk9803@gmail.com");
        introduction.setText("Hi, im cute");
        //
        mContext = this;
    //?????? ?????? ?????????
        init();

        db.collection("items").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    for(QueryDocumentSnapshot document:task.getResult()){
                        Item item=document.toObject(Item.class);
                        if(item.getId().equals(mAuth.getUid())) {
                            mAlbumAdapter.addItem(item);
                        }
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
    //????????? ????????? ?????? methods/////////////////////////////////////////////////////////////////
    /**
     * ???????????? ????????? ????????????
     */
    private void doTakeAlbumAction()
    {
        // ?????? ??????
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent, PICK_FROM_ALBUM);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case CROP_FROM_CAMERA: {
                // ????????? ??? ????????? ???????????? ?????? ????????????.
                // ??????????????? ???????????? ?????????????????? ???????????? ?????? ?????????
                // ?????? ????????? ???????????????.
                final Bundle extras = data.getExtras();

                if (extras != null) {
                    Bitmap photo = extras.getParcelable("data");
                    mPressProfileImg.setImageBitmap(photo);
                }

                // ?????? ?????? ??????
                File f = new File(mImageCaptureUri.getPath());
                if (f.exists()) {
                    f.delete();
                }

                break;
            }

            case PICK_FROM_ALBUM: {
                // ????????? ????????? ???????????? ???????????? ??????  break?????? ???????????????.
                // ?????? ??????????????? ?????? ???????????? ????????? ??????????????? ????????????.

                mImageCaptureUri = data.getData();
            }

            case PICK_FROM_CAMERA: {


                // ???????????? ????????? ????????? ??????????????? ????????? ????????? ???????????????.
                // ????????? ????????? ?????? ????????????????????? ???????????? ?????????.

                Intent intent = new Intent("com.android.camera.action.CROP");
                intent.setDataAndType(mImageCaptureUri, "image/*");

                intent.putExtra("outputX", 90);
                intent.putExtra("outputY", 90);
                intent.putExtra("aspectX", 1);
                intent.putExtra("aspectY", 1);
                intent.putExtra("scale", true);
                intent.putExtra("return-data", true);
                startActivityForResult(intent, CROP_FROM_CAMERA);
                break;
            }
        }
    }
    //????????? ????????? ????????? ???
    @Override
    public void onClick(View view) {
        DialogInterface.OnClickListener albumListener = new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                doTakeAlbumAction();
            }
        };

        DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        };

        new AlertDialog.Builder(this)
                .setTitle("???????????? ????????? ??????")
                .setNeutralButton("????????????", albumListener)
                .setNegativeButton("??????", cancelListener)
                .show();
    }
    //?????? ?????? ????????? ?????????/////////////////////////////////////////////////////////////////////////////////////////

    private void init() {
        my_album = findViewById(R.id.my_album);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(mContext, 3);
        my_album.setLayoutManager(mLayoutManager);
        my_album.addItemDecoration(new ItemDecoration(this));
        mAlbumAdapter.setOnItemClickListener(this);
        my_album.setAdapter(mAlbumAdapter);
    }
    // ??? ???????????? ???????????? ????????????
    @Override
    public void onItemClick(View view, myItem item) {
        Intent intent = new Intent(this, DetailActivity.class);

        intent.putExtra("item", item);

        View thumbView = view.findViewById(R.id.img_thumb);
        Pair<View, String> pair_thumb = Pair.create(thumbView, thumbView.getTransitionName());
        ActivityOptions optionsCompat = ActivityOptions.makeSceneTransitionAnimation(this, pair_thumb);

        startActivity(intent, optionsCompat.toBundle());
    }



}

