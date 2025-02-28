package com.chari6268.mycontacts;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.InsetDrawable;

import java.util.Objects;

public class CustomLoading {
    private final Activity activity;
    private AlertDialog dialog;
    private int flag;
    public CustomLoading(Activity activity) {
        this.activity = activity;
        flag = 0;
    }

    public void load(){
        if(flag == 1){
            flag = 0;
            dismisss();
        }

        flag = 1;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setView(activity.getLayoutInflater().inflate(R.layout.library_custom_loading_dialog,null))
                .setCancelable(false);
        dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        ColorDrawable back = new ColorDrawable(Color.TRANSPARENT);
        InsetDrawable inset = new InsetDrawable(back, 80);
        dialog.getWindow().setBackgroundDrawable(inset);
        dialog.show();
    }


    public void dismisss(){
        if(flag!=0)
            dialog.dismiss();
        flag = 0;

    }
}

