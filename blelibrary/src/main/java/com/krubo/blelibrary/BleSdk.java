package com.krubo.blelibrary;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.krubo.blelibrary.callback.CharacteristicCallback;
import com.krubo.blelibrary.callback.ConnectBleCallback;
import com.krubo.blelibrary.callback.DescriptorCallback;
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
    private CharacteristicCallback characteristicCallback;
    private DescriptorCallback descriptorCallback;
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
                bluetoothGatt.discoverServices();
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
        public void onServicesDiscovered(BluetoothGatt gatt, final int status) {
            super.onServicesDiscovered(gatt, status);
            if (!gatt.getDevice().getAddress().equals(bluetoothGatt.getDevice().getAddress())){
                return;
            }
            LogUtil.d("onServicesDiscovered status :"+status);
            if (connectBleCallback != null){
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (status == BluetoothGatt.GATT_SUCCESS){
                            connectBleCallback.onServicesDiscoveredSuccess();
                        }else{
                            connectBleCallback.onServicesDiscoveredFail();
                        }
                    }
                });
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (!gatt.getDevice().getAddress().equals(bluetoothGatt.getDevice().getAddress())){
                return;
            }
            LogUtil.d("onCharacteristicRead status :"+status);
            if (characteristicCallback != null){
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        characteristicCallback.readResult(characteristic, status == BluetoothGatt.GATT_SUCCESS);
                        characteristicCallback = null;
                    }
                });
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (!gatt.getDevice().getAddress().equals(bluetoothGatt.getDevice().getAddress())){
                return;
            }
            LogUtil.d("onCharacteristicWrite status :"+status);
            if (characteristicCallback != null){
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        characteristicCallback.writeResult(characteristic, status == BluetoothGatt.GATT_SUCCESS);
                        characteristicCallback = null;
                    }
                });
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (!gatt.getDevice().getAddress().equals(bluetoothGatt.getDevice().getAddress())){
                return;
            }
            LogUtil.d("onCharacteristicChanged");
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            if (!gatt.getDevice().getAddress().equals(bluetoothGatt.getDevice().getAddress())){
                return;
            }
            LogUtil.d("onDescriptorRead status :" + status);
            if (descriptorCallback != null){
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        descriptorCallback.readResult(descriptor, status == BluetoothGatt.GATT_SUCCESS);
                        descriptorCallback = null;
                    }
                });
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (!gatt.getDevice().getAddress().equals(bluetoothGatt.getDevice().getAddress())){
                return;
            }
            LogUtil.d("onDescriptorWrite status :" + status);
            if (descriptorCallback != null){
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        descriptorCallback.writeResult(descriptor, status == BluetoothGatt.GATT_SUCCESS);
                        descriptorCallback = null;
                    }
                });
            }
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
        scanBleCallback = null;
    }

    public boolean isScanning() {
        return isScanning;
    }

    public void connect(Context context, BluetoothDevice device, boolean autoConnect, ConnectBleCallback callback){
        connectBleCallback = callback;
        checkBle();
        if (!isOpen()){
            if (connectBleCallback != null) {
                callback.onDisconnected();
            }
            return;
        }
        if (connectState == BluetoothProfile.STATE_DISCONNECTED){
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
        connectBleCallback = null;
    }

    public boolean isConnected(){
        return connectState == BluetoothProfile.STATE_CONNECTED;
    }

    private void setFailCallback(boolean isCharacteristic, boolean isRead){
        if (isCharacteristic) {
            if (characteristicCallback != null) {
                if (isRead){
                    characteristicCallback.readResult(null, true);
                }else{
                    characteristicCallback.writeResult(null, false);
                }
                characteristicCallback = null;
            }
        }else{
            if (descriptorCallback != null){
                if (isRead){
                    descriptorCallback.readResult(null, false);
                }else{
                    descriptorCallback.writeResult(null, false);
                }
                descriptorCallback = null;
            }
        }
    }

    private BluetoothGattCharacteristic initCharacteristic(UUID serviceUuid, UUID characteristicUuid,
                                                           boolean isCharacteristic, boolean isRead){
        BluetoothGattService service = bluetoothGatt.getService(serviceUuid);
        if (service == null){
            setFailCallback(isCharacteristic, isRead);
            return null;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
        if (characteristic == null){
            setFailCallback(isCharacteristic, isRead);
        }
        return characteristic;
    }

    private BluetoothGattDescriptor initDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid,
                                                   boolean isCharacteristic, boolean isRead){
        BluetoothGattService service = bluetoothGatt.getService(serviceUuid);
        if (service == null){
            setFailCallback(isCharacteristic, isRead);
            return null;
        }
        BluetoothGattCharacteristic characteristic = initCharacteristic(serviceUuid, characteristicUuid,
                isCharacteristic, isRead);
        if (characteristic == null){
            return null;
        }
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorUuid);
        if (descriptor == null){
            setFailCallback(isCharacteristic, isRead);
        }
        return descriptor;
    }

    public boolean writeCharacteristic(UUID serviceUuid, UUID characteristicUuid, byte[] data, CharacteristicCallback callback){
        characteristicCallback = callback;
        checkBle();
        if (!isOpen()){
            setFailCallback(true, false);
            return false;
        }
        if (bluetoothGatt == null || data == null || serviceUuid == null || characteristicUuid == null){
            setFailCallback(true, false);
            return false;
        }
        BluetoothGattCharacteristic characteristic = initCharacteristic(serviceUuid, characteristicUuid, true, false);
        if (characteristic == null){
            return false;
        }
        characteristic.setValue(data);
        return bluetoothGatt.writeCharacteristic(characteristic);
    }

    public boolean readCharacteristic(UUID serviceUuid, UUID characteristicUuid, CharacteristicCallback callback){
        characteristicCallback = callback;
        checkBle();
        if (!isOpen()){
            setFailCallback(true, true);
            return false;
        }
        if (bluetoothGatt == null || serviceUuid == null || characteristicUuid == null){
            setFailCallback(true, true);
            return false;
        }
        BluetoothGattCharacteristic characteristic = initCharacteristic(serviceUuid, characteristicUuid, true, true);
        if (characteristic == null){
            return false;
        }
        return bluetoothGatt.readCharacteristic(characteristic);
    }

    public boolean writeDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid, byte[] data, DescriptorCallback callback){
        descriptorCallback = callback;
        checkBle();
        if (!isOpen()){
            setFailCallback(false, false);
            return false;
        }
        if (bluetoothGatt == null || data == null || serviceUuid == null || characteristicUuid == null || descriptorUuid == null){
            setFailCallback(false, false);
            return false;
        }
        BluetoothGattDescriptor descriptor = initDescriptor(serviceUuid, characteristicUuid, descriptorUuid, false, false);
        if (descriptor == null){
            return false;
        }
        descriptor.setValue(data);
        return bluetoothGatt.writeDescriptor(descriptor);
    }

    public boolean readDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid, DescriptorCallback callback){
        descriptorCallback = callback;
        checkBle();
        if (!isOpen()){
            setFailCallback(false, true);
            return false;
        }
        if (bluetoothGatt == null || serviceUuid == null || characteristicUuid == null || descriptorUuid == null){
            setFailCallback(false, true);
            return false;
        }
        BluetoothGattDescriptor descriptor = initDescriptor(serviceUuid, characteristicUuid, descriptorUuid, false, true);
        if (descriptor == null){
            return false;
        }
        return bluetoothGatt.readDescriptor(descriptor);
    }
}
