package com.android.eldbox;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;


/*
*原本是用来做电子日志界面的，然而没有需求，就没有做
* Originally used to do the electronic log interface, but there is no demand, no to do.
*/
public class LoggingActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logging);
    }
}
