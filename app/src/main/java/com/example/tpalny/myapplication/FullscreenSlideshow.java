package com.example.tpalny.myapplication;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.google.api.client.http.GenericUrl;
import com.squareup.picasso.Picasso;

import android.net.Uri;


public class FullscreenSlideshow extends AppCompatActivity {


    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen_slideshow);


        imageView = (ImageView) findViewById(R.id.my_image);

    }

    @Override
    protected void onResume() {
        super.onResume();
        while(Select_Folders.imagesList == null) {

        }
        String url = Select_Folders.imagesList.get(0);
        url = url.substring(0, url.length() - 16);
        imageView.setImageURI(Uri.parse(url));
        //Picasso.with(getApplicationContext()).load(url).into(imageView);
        /*while (true){
            if(Select_Folders.imagesList == null)continue;
            for(String url : Select_Folders.imagesList){
                Picasso.with(getApplicationContext()).load(url).into(imageView);
                *//*try {
                    wait(1000);
                } catch (InterruptedException e) {
                    finish();
                }*//*
            }
        }*/
    }

}
