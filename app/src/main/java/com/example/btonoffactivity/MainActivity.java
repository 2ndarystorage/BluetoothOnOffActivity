package com.example.btonoffactivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    /** BLE スキャン自動停止タイムアウト (ms) */
    private static final long BLE_SCAN_TIMEOUT_MS = 10_000;

    // Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;

    // UI
    private TextView tvBtStatus;
    private Button btnRequestEnable;
    private Button btnStartDiscovery;
    private Button btnBLEScan;

    // 機能1: ペアリング済みデバイスリスト
    private final List<String> pairedDevicesList = new ArrayList<>();
    private ArrayAdapter<String> pairedDevicesAdapter;

    // 機能4: Classic BT 探索デバイスリスト
    private final List<String> discoveredDevicesList = new ArrayList<>();
    private ArrayAdapter<String> discoveredDevicesAdapter;

    // 機能3: BLE スキャンデバイスリスト
    private final List<String> bleDevicesList = new ArrayList<>();
    private final Set<String> bleDevicesAddresses = new HashSet<>();
    private ArrayAdapter<String> bleDevicesAdapter;

    // 状態フラグ
    private boolean isDiscovering = false;
    private boolean isBleScanning = false;
    private final Handler bleStopHandler = new Handler(Looper.getMainLooper());

    // 機能5: Bluetooth 有効化リクエスト用 ActivityResultLauncher
    private ActivityResultLauncher<Intent> enableBtLauncher;

    /**
     * 機能2: Bluetooth 状態変化 + 機能4: デバイス発見 を受信する BroadcastReceiver
     */
    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                // 機能2: Bluetooth ON/OFF 状態変化をリアルタイム反映
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                updateBtStatusUI(state);

            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // 機能4: Classic BT 探索でデバイス発見
                BluetoothDevice device;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                } else {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                }
                if (device == null) return;

                String address = device.getAddress();
                String name = hasConnectPermission() ? device.getName() : null;
                String display = (name != null ? name : getString(R.string.device_unknown))
                        + "\n" + address;
                if (!discoveredDevicesList.contains(display)) {
                    discoveredDevicesList.add(display);
                    discoveredDevicesAdapter.notifyDataSetChanged();
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // 機能4: 探索完了
                isDiscovering = false;
                btnStartDiscovery.setText(R.string.btn_discovery_start);
                if (discoveredDevicesList.isEmpty()) {
                    discoveredDevicesList.add(getString(R.string.no_devices_found));
                    discoveredDevicesAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    /**
     * 機能3: BLE スキャン コールバック
     */
    private final ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String address = device.getAddress();
            if (bleDevicesAddresses.contains(address)) return;

            bleDevicesAddresses.add(address);
            String name = hasConnectPermission() ? device.getName() : null;
            int rssi = result.getRssi();
            String display = (name != null ? name : getString(R.string.device_unknown))
                    + "\n" + address + "  RSSI: " + rssi + " dBm";
            bleDevicesList.add(display);
            bleDevicesAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanFailed(int errorCode) {
            Toast.makeText(MainActivity.this,
                    getString(R.string.ble_scan_failed, errorCode), Toast.LENGTH_SHORT).show();
            isBleScanning = false;
            btnBLEScan.setText(R.string.btn_ble_scan_start);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Bluetooth アダプター初期化
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = (bluetoothManager != null) ? bluetoothManager.getAdapter() : null;

        // UI 参照取得
        tvBtStatus = findViewById(R.id.tvBtStatus);
        btnRequestEnable = findViewById(R.id.btnRequestEnable);
        btnStartDiscovery = findViewById(R.id.btnStartDiscovery);
        btnBLEScan = findViewById(R.id.btnBLEScan);

        ListView lvPairedDevices = findViewById(R.id.lvPairedDevices);
        ListView lvDiscoveredDevices = findViewById(R.id.lvDiscoveredDevices);
        ListView lvBleDevices = findViewById(R.id.lvBleDevices);

        // ListView アダプター設定
        pairedDevicesAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, pairedDevicesList);
        discoveredDevicesAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, discoveredDevicesList);
        bleDevicesAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, bleDevicesList);

        lvPairedDevices.setAdapter(pairedDevicesAdapter);
        lvDiscoveredDevices.setAdapter(discoveredDevicesAdapter);
        lvBleDevices.setAdapter(bleDevicesAdapter);

        // BroadcastReceiver 登録
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(btReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(btReceiver, filter);
        }

        // 機能5: Bluetooth 有効化リクエスト用ランチャー登録
        enableBtLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> updateBtStatus()
        );

        // ボタンリスナー設定

        // 機能5: Bluetooth を有効にするボタン
        btnRequestEnable.setOnClickListener(v -> requestBluetoothEnable());

        // 機能4: Classic BT 探索 開始/停止
        btnStartDiscovery.setOnClickListener(v -> toggleDiscovery());

        // 機能3: BLE スキャン 開始/停止
        btnBLEScan.setOnClickListener(v -> toggleBleScan());

        // 既存: Bluetooth 設定画面遷移
        findViewById(R.id.btnOpenBLUETOOTH_SETTINGS).setOnClickListener(v -> {
            startActivity(new Intent("android.settings.BLUETOOTH_SETTINGS"));
        });

        findViewById(R.id.btnOpenBLUETOOTH_DASHBOARD_SETTINGS).setOnClickListener(v -> {
            if (!hasConnectPermission()) {
                requestPermissions();
            } else {
                startActivity(new Intent("android.settings.BLUETOOTH_DASHBOARD_SETTINGS"));
            }
        });

        // 初期状態更新
        updateBtStatus();
    }

    // -------------------------------------------------------------------------
    // 機能2: Bluetooth 状態リアルタイム表示
    // -------------------------------------------------------------------------

    /** BT アダプターの現在状態を UI に反映 */
    private void updateBtStatus() {
        if (bluetoothAdapter == null) {
            tvBtStatus.setText(R.string.bt_not_supported);
            btnRequestEnable.setEnabled(false);
            return;
        }
        updateBtStatusUI(bluetoothAdapter.getState());
        loadPairedDevices();
    }

    private void updateBtStatusUI(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_ON:
                tvBtStatus.setText(R.string.bt_status_on);
                btnRequestEnable.setEnabled(false);
                loadPairedDevices();
                break;
            case BluetoothAdapter.STATE_OFF:
                tvBtStatus.setText(R.string.bt_status_off);
                btnRequestEnable.setEnabled(true);
                pairedDevicesList.clear();
                pairedDevicesAdapter.notifyDataSetChanged();
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                tvBtStatus.setText(R.string.bt_status_turning_on);
                btnRequestEnable.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                tvBtStatus.setText(R.string.bt_status_turning_off);
                btnRequestEnable.setEnabled(false);
                break;
            default:
                tvBtStatus.setText(R.string.bt_status_unknown);
                break;
        }
    }

    // -------------------------------------------------------------------------
    // 機能1: ペアリング済みデバイス一覧表示
    // -------------------------------------------------------------------------

    private void loadPairedDevices() {
        if (!hasConnectPermission()) return;
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) return;

        pairedDevicesList.clear();
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if (bondedDevices.isEmpty()) {
            pairedDevicesList.add(getString(R.string.no_paired_devices));
        } else {
            for (BluetoothDevice device : bondedDevices) {
                String name = device.getName();
                String address = device.getAddress();
                pairedDevicesList.add(
                        (name != null ? name : getString(R.string.device_unknown))
                                + "\n" + address);
            }
        }
        pairedDevicesAdapter.notifyDataSetChanged();
    }

    // -------------------------------------------------------------------------
    // 機能5: Bluetooth 有効化リクエスト
    // -------------------------------------------------------------------------

    private void requestBluetoothEnable() {
        if (!hasConnectPermission()) {
            requestPermissions();
            return;
        }
        enableBtLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
    }

    // -------------------------------------------------------------------------
    // 機能4: Classic Bluetooth デバイス探索
    // -------------------------------------------------------------------------

    private void toggleDiscovery() {
        if (!hasScanPermission() || !hasConnectPermission()) {
            requestPermissions();
            return;
        }
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, R.string.bt_please_enable, Toast.LENGTH_SHORT).show();
            return;
        }

        if (isDiscovering) {
            bluetoothAdapter.cancelDiscovery();
            isDiscovering = false;
            btnStartDiscovery.setText(R.string.btn_discovery_start);
        } else {
            discoveredDevicesList.clear();
            discoveredDevicesAdapter.notifyDataSetChanged();
            if (bluetoothAdapter.startDiscovery()) {
                isDiscovering = true;
                btnStartDiscovery.setText(R.string.btn_discovery_stop);
            } else {
                Toast.makeText(this, R.string.discovery_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // -------------------------------------------------------------------------
    // 機能3: BLE スキャン
    // -------------------------------------------------------------------------

    private void toggleBleScan() {
        if (!hasScanPermission()) {
            requestPermissions();
            return;
        }
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, R.string.bt_please_enable, Toast.LENGTH_SHORT).show();
            return;
        }

        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bleScanner == null) {
            Toast.makeText(this, R.string.ble_scanner_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        if (isBleScanning) {
            stopBleScan();
        } else {
            bleDevicesList.clear();
            bleDevicesAddresses.clear();
            bleDevicesAdapter.notifyDataSetChanged();
            bleScanner.startScan(bleScanCallback);
            isBleScanning = true;
            btnBLEScan.setText(R.string.btn_ble_scan_stop);
            // タイムアウト後に自動停止
            bleStopHandler.postDelayed(this::stopBleScan, BLE_SCAN_TIMEOUT_MS);
        }
    }

    private void stopBleScan() {
        if (bleScanner != null && isBleScanning && hasScanPermission()) {
            bleScanner.stopScan(bleScanCallback);
        }
        isBleScanning = false;
        btnBLEScan.setText(R.string.btn_ble_scan_start);
        bleStopHandler.removeCallbacksAndMessages(null);
    }

    // -------------------------------------------------------------------------
    // パーミッション関連
    // -------------------------------------------------------------------------

    private boolean hasConnectPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasScanPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                },
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            updateBtStatus();
        }
    }

    // -------------------------------------------------------------------------
    // ライフサイクル
    // -------------------------------------------------------------------------

    @Override
    protected void onResume() {
        super.onResume();
        updateBtStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(btReceiver);
        if (isDiscovering && bluetoothAdapter != null && hasScanPermission()) {
            bluetoothAdapter.cancelDiscovery();
        }
        if (isBleScanning) {
            stopBleScan();
        }
    }
}
