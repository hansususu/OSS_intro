package com.example.weatherforecastapplication;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        if (savedInstanceState == null) {
            DetailFragment fragment = new DetailFragment();
            if (getIntent().hasExtra("data")){
                Bundle arguments = new Bundle();
                arguments.putString("data", getIntent().getStringExtra("data"));
                fragment.setArguments(arguments);
            }
            getSupportFragmentManager().beginTransaction().add(R.id.detail_container, fragment).commit();
        }

    }

}
