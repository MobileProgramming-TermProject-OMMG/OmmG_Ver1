package com.example.lastommg;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    ArrayList<Item> items = new ArrayList<Item>();
    int lastPosition = -1;

    Context context;
    AlertDialog dialog;
    static String TAG = "Adapter";
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {

        Random random = new Random();
        if ((random.nextInt(100)) % 2 == 1) {
            LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
            View itemView = inflater.inflate(R.layout.item_layout_rl, viewGroup, false);
            context = viewGroup.getContext();
            return new ViewHolder(itemView);
        } else {
            LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
            View itemView = inflater.inflate(R.layout.item_layout_lr, viewGroup, false);
            context = viewGroup.getContext();
            return new ViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, @SuppressLint("RecyclerView") final int position) {
        Random random = new Random();
        if (viewHolder.getAdapterPosition() > lastPosition) {
            if ((random.nextInt(100)) % 2 == 1) {
                Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_row_rl);
                ((ViewHolder) viewHolder).itemView.startAnimation(animation);

                Item item = items.get(position);
                viewHolder.setItem(item);
            } else {
                Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_row_lr);
                ((ViewHolder) viewHolder).itemView.startAnimation(animation);

                Item item = items.get(position);
                viewHolder.setItem(item);
            }
        }

        viewHolder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupXml(items.get(position).getName(),items.get(position).getUri(), items.get(position).getPhoneNumber(),items.get(position).getAddress(),items.get(position).getDistance());
            }
        });
    }



    public void popupXml(String name, String uri, String phoneNumber, String address,Double distance) {
        //Log.d(TAG, "okay");
        Uri u=Uri.parse(uri);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.popup1, null);
        ImageView imageView = view.findViewById(R.id.imageView);
        TextView textView = view.findViewById(R.id.textView);
        Glide.with(context).load(u).into(imageView);

        textView.setTextSize(35);
        textView.setText(name + "\n");
        textView.append(phoneNumber + "\n");
        textView.append(address + "\n");
        textView.append(distance + "\n");

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("음식점정보").setView(view);
        builder.setNegativeButton("닫기", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        dialog = builder.create();
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItem(Item item) {
        items.add(0,item);
    }

    public void removeAllItem() {
        items.clear();
    }

    public void setDistance(GeoPoint B){
        int i=0;
        int j=getItemCount();
        double distance;
        GeoPoint A;
        Location locationA=new Location("Point A");
        Location locationB=new Location("Point B");
        locationB.setLatitude((B.getLatitude()));
        locationB.setLongitude((B.getLongitude()));
        for(i=0;i<j;i++)
        {
            A=items.get(i).getGeoPoint();
            locationA.setLatitude(A.getLatitude());
            locationA.setLongitude((A.getLongitude()));
            distance=locationA.distanceTo(locationB);
            items.get(i).distance=distance;
        }
        Collections.sort(items,new itemDistanceComparator());
    }


    public void sortTime(){
        items.sort(Comparator.comparing(Item::getTimestamp));
    }

    class itemDistanceComparator implements Comparator<Item> {
        @Override
        public int compare(Item i1, Item i2) {
            if(i1.distance>i2.distance){
                return 1;
            }
            else if(i1.distance<i2.distance){
                return -1;
            }
            return 0;
        }
    }



    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        RelativeLayout parentLayout;
        Uri u;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.b_image);
            parentLayout = itemView.findViewById(R.id.parentLayout);
        }

        public void setItem(Item item) {
            u=Uri.parse(item.getUri());
            Glide.with(context).load(u).into(imageView);
        }
    }
}
