package com.example.tpalny.myapplication;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.google.api.client.http.GenericUrl;
import com.squareup.picasso.Picasso;


public class FullscreenSlideshow extends AppCompatActivity {


    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen_slideshow);


        mImageView = (ImageView) findViewById(R.id.my_image);

    }

    @Override
    protected void onResume() {
        super.onResume();
        while (Select_Folders.imagesList == null) {

        }
        String url = Select_Folders.imagesList.get(0);
        url = url.substring(0, url.length()-16);
        //mImageView.setImageURI(Uri.parse(gUrl.toString()));
        Picasso.with(getApplicationContext()).load(url).into(mImageView);
        /*while (true){
            if(Select_Folders.imagesList == null)continue;
            for(String url : Select_Folders.imagesList){
                Picasso.with(getApplicationContext()).load(url).into(mImageView);
                *//*try {
                    wait(1000);
                } catch (InterruptedException e) {
                    finish();
                }*//*
            }
        }*/
    }

    public void onImageClicked(View view) {
        finish();
    }


}
