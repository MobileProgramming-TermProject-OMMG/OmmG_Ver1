package com.example.lastommg;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

public class DetailActivity extends AppCompatActivity implements View.OnClickListener {

    private Context mContext;
    private myItem item;

    private ImageView btn_back;
    private ImageView img_thumb;
    private TextView txt_title;
    private TextView txt_artist;
    private TextView txt_date;
    private TextView txt_type;
    private TextView txt_introduce;
    private RecyclerView rcc_song;

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        setInit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        mContext = this;
        getData();
    }

    private void getData() {
        Intent intent = getIntent();
        item = (myItem) intent.getSerializableExtra("item");
    }



    private void setInit() {

        btn_back = (ImageView) findViewById(R.id.btn_back);
        btn_back.setOnClickListener(this);
        img_thumb = (ImageView) findViewById(R.id.img_thumb);
        Glide.with(mContext)
                .load(item.getUri())
                .into(img_thumb);

        txt_title = (TextView) findViewById(R.id.txt_title);
        txt_title.setText(item.getName());
        txt_artist = (TextView) findViewById(R.id.txt_artist);
        txt_artist.setText(item.getAddress());
        txt_date = (TextView) findViewById(R.id.txt_date);
        txt_date.setText(item.getPhoneNumber());
        txt_type = (TextView) findViewById(R.id.txt_type);
        txt_type.setText(item.getName());
        txt_introduce = (TextView) findViewById(R.id.txt_introduce);
        txt_introduce.setText(item.getPhoneNumber());
        rcc_song = (RecyclerView) findViewById(R.id.rcc_song);
        RecyclerView.LayoutManager mLayoutManager =
                new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false);
        rcc_song.setLayoutManager(mLayoutManager);
        rcc_song.setNestedScrollingEnabled(false);
//        SongAdapter mSongAdapter = new SongAdapter(mContext, albumVO.getSong());
//        rcc_song.setAdapter(mSongAdapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_back:
                supportFinishAfterTransition();
                break;
        }
    }
}