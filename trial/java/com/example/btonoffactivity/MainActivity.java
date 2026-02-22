package com.example.btonoffactivity;

import android.os.Bundle;
import android.os.Build;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.Manifest;
import android.view.View;
import android.widget.Button;

import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1; // リクエストコード

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // BLUETOOTH_SETTINGSボタンを押下した場合
        Button openBluetoothSettingsBtn = findViewById(R.id.btnOpenBLUETOOTH_SETTINGS);
        openBluetoothSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // BLUETOOTH_SETTINGS画面へ遷移
                Intent intent = new Intent();
                intent.setAction("android.settings.BLUETOOTH_SETTINGS");
                startActivity(intent);
            }
        });

        // BLUETOOTH_DASHBOARD_SETTINGSボタンを押下した場合
        // Android 15 (U) 以上のみBLUETOOTH_DASHBOARD_SETTINGSに遷移可能
        Button openBluetoothDashboardSettingsBtn = findViewById(R.id.btnOpenBLUETOOTH_DASHBOARD_SETTINGS);
        openBluetoothDashboardSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    // パーミッションが与えられていない場合はリクエスト
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                            BLUETOOTH_PERMISSION_REQUEST_CODE);
                } else {
                    // パーミッションがすでに許可されている場合はBLUETOOTH_DASHBOARD_SETTINGS画面へ遷移
                    openBluetoothSettings();
                }
            }
        });
    }

    // Bluetooth設定画面に遷移するメソッド
    private void openBluetoothSettings() {
        // BLUETOOTH_DASHBOARD_SETTINGS画面へ遷移
        Intent intent = new Intent();
        intent.setAction("android.settings.BLUETOOTH_DASHBOARD_SETTINGS");
        startActivity(intent);
   }

    // パーミッションリクエストの結果を処理するメソッド
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // リクエストコードを確認
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            // パーミッションが許可されたかどうかを確認
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 許可された場合はBLUETOOTH_DASHBOARD_SETTINGS画面へ遷移
                openBluetoothSettings();
            }
        }
    }
}