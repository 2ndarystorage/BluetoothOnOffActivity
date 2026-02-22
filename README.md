# BTonoffActivity

Android の Bluetooth 設定画面へ遷移するためのサンプルアプリです。

## 概要

2種類の Bluetooth 設定画面への遷移方法を確認・比較するためのアプリです。

| ボタン | 遷移先 Intent |
|--------|--------------|
| BLUETOOTH_SETTINGS | `android.settings.BLUETOOTH_SETTINGS` |
| BLUETOOTH_DASHBOARD_SETTINGS | `android.settings.BLUETOOTH_DASHBOARD_SETTINGS` |

## 機能

- **BLUETOOTH_SETTINGS ボタン**
  標準の Bluetooth 設定画面を開きます。Android 12 以降で動作します。

- **BLUETOOTH_DASHBOARD_SETTINGS ボタン**
  Bluetooth ダッシュボード設定画面を開きます。
  `BLUETOOTH_CONNECT` パーミッションが必要です。未許可の場合はダイアログで許可を求めます。
  > Android 15 (API 35) 以降で利用可能な設定画面です。

## 動作環境

| 項目 | バージョン |
|------|-----------|
| 最小 SDK | Android 12 (API 31) |
| ターゲット SDK | Android 15 (API 35) |
| 言語 | Java |
| ビルドシステム | Gradle (Kotlin DSL) |

## パーミッション

```xml
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

## ビルド方法

```bash
./gradlew assembleDebug
```

ビルド済み APK は `app/build/outputs/apk/debug/app-debug.apk` に出力されます。
