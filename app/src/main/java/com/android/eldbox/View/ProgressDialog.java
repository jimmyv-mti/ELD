package com.android.eldbox.View;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;

import com.android.eldbox.R;

public class ProgressDialog extends AlertDialog
{
    public ProgressDialog(Context context)
    {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progress_dialog_layout);
    }

}
