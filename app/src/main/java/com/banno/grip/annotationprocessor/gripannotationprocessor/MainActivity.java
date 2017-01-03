package com.banno.grip.annotationprocessor.gripannotationprocessor;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.banno.annotations.example.LandOfGators;
import com.banno.grip.annotationprocessor.processor.GripAnnotationProcessor;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GripAnnotationProcessor what;

    }
}
