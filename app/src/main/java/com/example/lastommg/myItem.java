package com.example.lastommg;

import android.net.Uri;
import android.os.Parcelable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.io.Serializable;

public class myItem implements Serializable {
    String id;
    String uri;
    String name;
    String phoneNumber;
    Double lat;
    Double lon;
    String address;
    double distance=0.0;
    Timestamp timestamp;
    public myItem()  {

    }

    public myItem(String id,String name, String uri, String phoneNumber,Double lat,Double lon,String address,double distance)  {
        // this.resId = resId;
        this.id=id;
        this.name = name;
        this.uri=uri;
        this.phoneNumber = phoneNumber;
        this.lat=lat;
        this.lon=lon;
        this.address=address;
        this.distance=distance;
    }

    public String getId() {
        return id;
    }
    public void setId() {
        this.name = id;
    }
    public String getName() {
        return name;
    }
    public void setName() {
        this.name = name;
    }
    public String getUri(){
        return uri;
    }
    public void setUri(){
        this.uri=uri;
    }
    public String getPhoneNumber() {
        return phoneNumber;
    }
    public void setPhoneNumber() {
        this.phoneNumber = phoneNumber;
    }
    public Double getLat()
    {
        return lat;
    }
    public void setLat()
    {
        this.lat=lat;
    }
    public Double getLon()
    {
        return lon;
    }
    public void setLon()
    {
        this.lon=lon;
    }
    public String getAddress(){
        return address;
    }
    public void setAddress(){
        this.address=address;
    }
    public double getDistance(){
        return distance;
    }
    public void setDistance(){
        this.distance=distance;
    }


}