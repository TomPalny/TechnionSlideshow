package com.example.tpalny.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class Select_Folders extends Activity {

    private Button slideShowButton;
    private boolean textWasSelected = false;
    private boolean picturesWereSelected = false;
    private TextView pictureSelectionText;
    private TextView textSelectionText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select__folders);
        slideShowButton = (Button) findViewById(R.id.Start_Slideshow_Button);
        slideShowButton.setEnabled(false);
        slideShowButton.setAlpha(.5f);
        pictureSelectionText = (TextView) findViewById(R.id.Selected_Image_Folder);
        textSelectionText = (TextView) findViewById(R.id.Selected_Text_Folder);


    }

    private static final int REQUEST_CODE_IMAGE_OPENER = 1;
    private static final int REQUEST_CODE_TEXT_OPENER = 2;


    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
        }

        switch (requestCode) {
            case REQUEST_CODE_IMAGE_OPENER:

                break;
            case REQUEST_CODE_TEXT_OPENER:

                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }

    }


    public void onPicturesSelectClicked(View view) {

    }

    public void onTextSelectClicked(View view) {

    }

    public void onPlaySlideshowClicked(View view) {

    }

    public void onClearPicturesClicked(View view) {
        picturesWereSelected = false;
        slideShowButton.setEnabled(false);
        slideShowButton.setAlpha(.5f);
        pictureSelectionText.setText("None Selected");
    }

    public void onClearTextClicked(View view) {
        textWasSelected = false;
        textSelectionText.setText("None Selected");
    }


}
