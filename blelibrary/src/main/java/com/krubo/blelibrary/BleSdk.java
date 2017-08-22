package com.krubo.blelibrary;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.krubo.blelibrary.callback.ConnectBleCallback;
import com.krubo.blelibrary.callback.ScanBleCallback;
import com.krubo.blelibrary.utils.LogUtil;

import java.util.UUID;

/**
 * @author krubo
 * @created 2017/8/22
 */

public class BleSdk {

    private static BleSdk bleSdk;
    private BluetoothAdapter bluetoothAdapter;
    private Context context;
    private boolean isScanning;
    private ScanBleCallback scanBleCallback;
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            LogUtil.d("scan bluetoothDevice mac: "+ bluetoothDevice.getAddress());
            if (scanBleCallback!=null){
                scanBleCallback.scan(bluetoothDevice);
            }
        }
    };
    private BluetoothGatt bluetoothGatt;
    private ConnectBleCallback connectBleCallback;
    private int connectState;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };
    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (!gatt.getDevice().getAddress().equals(bluetoothGatt.getDevice().getAddress())){
                return;
            }
            LogUtil.d("onConnectionStateChange status :"+status+" newState :"+newState);
            connectState = newState;
            if (newState == BluetoothProfile.STATE_CONNECTED){
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (connectBleCallback != null){
                            connectBleCallback.onConnected();
                        }
                    }
                });
            }else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                close();
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (connectBleCallback != null){
                            connectBleCallback.onDisconnected();
                        }
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (!gatt.getDevice().getAddress().equals(bluetoothGatt.getDevice().getAddress())){
                return;
            }
            LogUtil.d("onServicesDiscovered status :"+status);

        }

        private void runOnMainThread(Runnable runnable){
            if (Looper.myLooper() == Looper.getMainLooper()){
                runnable.run();
            }else{
                handler.post(runnable);
            }
        }
    };

    private BleSdk(Context context){
        this.context = context.getApplicationContext();
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    public static BleSdk getInstance(Context context){
        if (bleSdk == null){
            bleSdk = new BleSdk(context);
        }
        return bleSdk;
    }

    public boolean isSupportBle(){
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public boolean isSupportBluetooth(){
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
    }

    public boolean isOpen(){
        if (!isSupportBluetooth()){
            throw new IllegalArgumentException("no bluetooth hardware");
        }
        return bluetoothAdapter.isEnabled();
    }

    public boolean openBluetooth(){
        if (!isOpen()){
            return bluetoothAdapter.enable();
        }
        return true;
    }

    public boolean closeBluetooth(){
        if (isOpen()){
            return bluetoothAdapter.disable();
        }
        return true;
    }

    private void checkBle(){
        if (!isSupportBluetooth()){
            throw new IllegalArgumentException("no bluetooth hardware");
        }
        if (!isSupportBle()){
            throw new IllegalArgumentException("no bluetooth ble hardware");
        }
    }

    public void startBleScan(ScanBleCallback callback){
        checkBle();
        if (!isOpen()){
            return;
        }
        if (isScanning){
            return;
        }
        isScanning = true;
        scanBleCallback = callback;
        bluetoothAdapter.startLeScan(leScanCallback);
    }

    public void startBleScan(UUID[] serviceUuids, ScanBleCallback callback){
        checkBle();
        if (!isOpen()){
            return;
        }
        if (isScanning){
            return;
        }
        isScanning = true;
        scanBleCallback = callback;
        bluetoothAdapter.startLeScan(serviceUuids, leScanCallback);
    }

    public void stopBleScan(){
        checkBle();
        if (!isOpen()){
            return;
        }
        if (!isScanning){
            return;
        }
        isScanning = false;
        bluetoothAdapter.stopLeScan(leScanCallback);
    }

    public boolean isScanning() {
        return isScanning;
    }

    public void connect(Context context, BluetoothDevice device, boolean autoConnect, ConnectBleCallback callback){
        checkBle();
        if (!isOpen()){
            return;
        }
        if (connectState == BluetoothProfile.STATE_DISCONNECTED){
            connectBleCallback = callback;
            connectState = BluetoothProfile.STATE_CONNECTING;
            bluetoothGatt = device.connectGatt(context, autoConnect, gattCallback);
        }
    }

    public void disconnect(){
        checkBle();
        if (!isOpen()){
            return;
        }
        if (connectState == BluetoothProfile.STATE_CONNECTED){
            if (bluetoothGatt != null) {
                connectState = BluetoothProfile.STATE_DISCONNECTING;
                bluetoothGatt.disconnect();
            }else{
                connectState = BluetoothProfile.STATE_DISCONNECTED;
            }
        }else if(connectState == BluetoothProfile.STATE_CONNECTING){
            close();
        }
    }

    public void close(){
        checkBle();
        if (!isOpen()){
            return;
        }
        connectState = BluetoothProfile.STATE_DISCONNECTED;
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    public boolean isConnected(){
        return connectState == BluetoothProfile.STATE_CONNECTED;
    }
}
