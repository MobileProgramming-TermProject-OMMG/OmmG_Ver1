package com.example.lastommg;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostActivity extends AppCompatActivity {
    Uri imageUri, photoURI;
    String myUrl = "";
    String mCurrentPhotoPath, address;
    int flag;
    double latitude, longitude;
    UploadTask uploadTask;
    StorageReference storageReference;
    FirebaseAuth mAuth;
    private AlbumAdapter mAlbumAdapter;
    GeoPoint u_GeoPoint;


    LocationManager manager;
    //GPSListener gpsListener;

    ImageView close, image_added;
    TextView post;
    EditText description, locate;
    ImageButton gps;
    RecyclerView p_recyclerView;
    ItemAdapter itemAdapter;


    FirebaseFirestore db= FirebaseFirestore.getInstance();
    private static final int FROM_CAMERA = 1111;
    private static final int FROM_ALBUM = 2222;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        p_recyclerView = ((MainActivity) MainActivity.context_main).recyclerView;

        close = findViewById(R.id.close);
        image_added = findViewById(R.id.image_added);
        post = findViewById(R.id.post);
        description = findViewById(R.id.description);
        locate = findViewById(R.id.locate);
        gps = findViewById(R.id.btn_locate);

        u_GeoPoint = new GeoPoint(latitude, longitude);


        mAuth = FirebaseAuth.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        mAlbumAdapter = new AlbumAdapter();
        itemAdapter = new ItemAdapter();
        p_recyclerView.setAdapter(itemAdapter);



        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PostActivity.this, MainActivity.class));
                finish();
            }
        });

        post.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });


        gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        makeDialog();
    }

    private void makeDialog() {
        AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);

        alt_bld.setTitle("?????? ?????????").setCancelable(false).setPositiveButton("????????????", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.v("??????", "??????????????? > ???????????? ??????");
                flag = 0;
                takePhoto();
            }

        }).setNeutralButton("????????????", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int id) {
                Log.v("??????", "??????????????? > ???????????? ??????");
                flag = 1;
                getAlbum();
            }
        }).setNegativeButton("??????   ", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.v("??????", "??????????????? > ?????? ??????");
                dialog.cancel();
            }
        });

        AlertDialog alert = alt_bld.create();
        alert.show();
    }

    private void takePhoto() {
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            if (intent.resolveActivity(getPackageManager()) != null) {
                File photoFile = null;
                photoFile = createImageFile();

                if (photoFile != null) {
                    Uri providerURI = FileProvider.getUriForFile(this, getPackageName(), photoFile);
                    imageUri = providerURI;
                    intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, providerURI);
                    startActivityForResult(intent, FROM_CAMERA);
                }
            }
        } else {
            Log.v("??????", "??????????????? ?????? ?????????");
            return;
        }
    }

    private File createImageFile() {
        File imageFile = null;
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + ".jpg";
        File storageDir = new File(Environment.getExternalStorageDirectory() + "/Pictures", "ommg");

        if (!storageDir.exists()) {
            Log.i("mCurrentPhotoPath1", storageDir.toString());
            storageDir.mkdirs();
        }
        imageFile = new File(storageDir, imageFileName);
        mCurrentPhotoPath = imageFile.getAbsolutePath();

        return imageFile;
    }

    private void galleryAddPic() {
        Log.i("galleryAddPic", "Call");
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        // ?????? ????????? ?????? ????????? ?????????(?????? ????????? ???????????? ????????? ???????????? ??? ???)
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
        Toast.makeText(this, "????????? ????????? ?????????????????????.", Toast.LENGTH_SHORT).show();
    }

    private void getAlbum() {
        Log.i("getAlbum", "Call");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent, FROM_ALBUM);
    }

    private void uploadImage() {
        Uri file = null;
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        Timestamp timestamp=new Timestamp(new Date());
        String imageFileName = "JPEG" + timeStamp + "jpg";
        StorageReference filereference = storageReference.child("image/" + imageFileName);
        if (flag == 0) {
            //????????????
            file = Uri.fromFile(new File(mCurrentPhotoPath));
        } else if (flag == 1) {
            //????????????
            file = photoURI;
        }

        uploadTask = filereference.putFile(file);
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Posting");
        progressDialog.show();

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(PostActivity.this, "?????? ????????? ??????", Toast.LENGTH_SHORT).show();
                Log.v("??????", "?????? ????????? ??????");
                e.printStackTrace();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageReference = storage.getReference();
                StorageReference pathReference = storageReference.child("image");
                if (pathReference == null) {
                    Toast.makeText(PostActivity.this, "???????????? ????????? ????????????.", Toast.LENGTH_SHORT).show();
                } else {
                    StorageReference submitProfile = storageReference.child("image/" + imageFileName);
                    submitProfile.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            address = getCurrentAddress(u_GeoPoint.getLatitude(), u_GeoPoint.getLongitude());
                            Item item = new Item(0,mAuth.getUid(), imageFileName, uri.toString(), "010-9913-2992", u_GeoPoint, address, 0.0, timestamp);
                            itemAdapter.addItem(item);
                            if (item.getId().equals(mAuth.getUid())) {
                                mAlbumAdapter.addItem(item);
                                Log.i("aaaaaa", String.valueOf(mAlbumAdapter.getItemCount()));
                            }
                            Log.i("??????", imageFileName);
                            db.collection("items").document(imageFileName).set(item);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                        }
                    });
                }
                itemAdapter.notifyDataSetChanged();
                Toast.makeText(PostActivity.this, "?????? ???????????? ?????????????????????.", Toast.LENGTH_SHORT).show();
                p_recyclerView.startLayoutAnimation();
                progressDialog.dismiss();

            }
        });

//        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
//                if (task.isSuccessful()) {
//                    UploadTask.TaskSnapshot downloadUri = task.getResult();
//                    myUrl = downloadUri.toString();
//
//                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
//
//                    String postid = reference.push().getKey();
//
//                    HashMap<String, Object> hashMap = new HashMap<>();
//                    hashMap.put("postid", postid);
//                    hashMap.put("postimage", myUrl);
//                    hashMap.put("description", description.getText().toString());
//                    hashMap.put("publisher", FirebaseAuth.getInstance().getCurrentUser().getUid());
//
//                    reference.child(postid).setValue(hashMap);
//
//                    progressDialog.dismiss();
//
//                    //startActivity(new Intent(PostActivity.this, MainActivity.class));
//                    finish();
//                } else {
//                    Toast.makeText(PostActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
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

    private String getFileExtension(Uri uri) {
        android.webkit.MimeTypeMap.getSingleton();
        String extension = MimeTypeMap.getFileExtensionFromUrl(String.valueOf(uri));
        return extension;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case FROM_CAMERA:
                try {
                    Log.v("??????", "FROM_CAMERA ??????");
                    galleryAddPic();
                    image_added.setImageURI(imageUri);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;

            case FROM_ALBUM:
                //???????????? ????????????

                if (data.getData() != null) {
                    try {
                        photoURI = data.getData();
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoURI);
                        image_added.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                break;
        }
    }
}
