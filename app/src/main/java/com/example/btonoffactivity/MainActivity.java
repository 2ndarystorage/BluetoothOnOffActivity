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

        Button openBluetoothBtn = findViewById(R.id.btnOpenBluetooth);
        openBluetoothBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    // パーミッションが与えられていない場合はリクエスト
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                            BLUETOOTH_PERMISSION_REQUEST_CODE);
                } else {
                    // パーミッションがすでに許可されている場合は設定画面へ遷移
                    openBluetoothSettings();
                }
            }
        });
    }

    // Bluetooth設定画面に遷移するメソッド
    private void openBluetoothSettings() {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= 35) {
            // Android 15 (U) 以上 → ダッシュボード設定
            intent.setAction("android.settings.BLUETOOTH_DASHBOARD_SETTINGS");
        } else {
            // それ以下のバージョン → 従来のBluetooth設定画面
            intent.setAction("android.settings.BLUETOOTH_SETTINGS");
        }
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
                // 許可された場合は設定画面へ遷移
                openBluetoothSettings();
            }
        }
    }
}