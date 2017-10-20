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

import com.krubo.blelibrary.callback.BleMessageCallback;
import com.krubo.blelibrary.callback.ConnectBleCallback;
import com.krubo.blelibrary.callback.ScanBleCallback;
import com.krubo.blelibrary.utils.LogUtil;

import java.util.HashMap;
import java.util.Map;
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
    private Map<String, BluetoothGatt> bluetoothGattMap = new HashMap<>();
    private Map<String, ConnectBleCallback> connectBleCallbackMap = new HashMap<>();
    private Map<String, BleMessageCallback> bleMessageCallbackMap = new HashMap<>();
    private Map<String, Integer> connectStateMap = new HashMap<>();
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    private void setBluetoothGatt(BluetoothGatt gatt){
        if (bluetoothGattMap == null){
            bluetoothGattMap = new HashMap<>();
        }
        if (gatt == null || gatt.getDevice()==null){
            return;
        }
        bluetoothGattMap.put(gatt.getDevice().getAddress(), gatt);
    }

    public BluetoothGatt getBluetoothGatt(String address){
        if (bluetoothGattMap == null){
            bluetoothGattMap = new HashMap<>();
        }
        if (address == null || address.length() == 0){
            return null;
        }
        if (bluetoothGattMap.containsKey(address)){
            return bluetoothGattMap.get(address);
        }
        return null;
    }

    private void setConnectCallback(String address, ConnectBleCallback callback){
        if (connectBleCallbackMap == null){
            connectBleCallbackMap = new HashMap<>();
        }
        if (callback == null || address == null || address.length() == 0){
            return;
        }
        connectBleCallbackMap.put(address, callback);
    }

    private ConnectBleCallback getConnectBleCallback(String address){
        if (connectBleCallbackMap == null){
            connectBleCallbackMap = new HashMap<>();
        }
        if (address == null || address.length() == 0){
            return null;
        }
        if (connectBleCallbackMap.containsKey(address)){
            return connectBleCallbackMap.get(address);
        }
        return null;
    }

    private void setConnectState(String address, int state){
        if (connectStateMap == null){
            connectStateMap = new HashMap<>();
        }
        if (address == null || address.length() == 0){
            return;
        }
        connectStateMap.put(address, state);
    }

    public int getConnectState(String address){
        if (connectStateMap == null){
            connectStateMap = new HashMap<>();
        }
        if (address == null || address.length() == 0){
            return -1;
        }
        if (connectStateMap.containsKey(address)){
            return connectStateMap.get(address);
        }
        return -1;
    }

    public void setBleMessageCallback(String address, BleMessageCallback callback){
        if (bleMessageCallbackMap == null){
            bleMessageCallbackMap = new HashMap<>();
        }
        if (callback == null || address == null || address.length() == 0){
            return;
        }
        bleMessageCallbackMap.put(address, callback);
    }

    private BleMessageCallback getBleMessageCallback(String address){
        if (bleMessageCallbackMap == null){
            bleMessageCallbackMap = new HashMap<>();
        }
        if (address == null || address.length() == 0){
            return null;
        }
        if (bleMessageCallbackMap.containsKey(address)){
            return bleMessageCallbackMap.get(address);
        }
        return null;
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            String address = gatt.getDevice().getAddress();
            final ConnectBleCallback connectBleCallback = getConnectBleCallback(address);
            LogUtil.d("onConnectionStateChange status :"+status+" newState :"+newState+" address : "+ address);
            setConnectState(address, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED){
                gatt.discoverServices();
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (connectBleCallback != null){
                            connectBleCallback.onConnected();
                        }
                    }
                });
            }else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                close(address);
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
            String address = gatt.getDevice().getAddress();
            LogUtil.d("onServicesDiscovered status :"+status+" address : "+ address);
            final ConnectBleCallback connectBleCallback = getConnectBleCallback(address);
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
            String address = gatt.getDevice().getAddress();
            final BleMessageCallback bleMessageCallback = getBleMessageCallback(address);
            LogUtil.d("onCharacteristicRead status :"+status+" address : "+ address);
            if (bleMessageCallback != null){
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        bleMessageCallback.readCharacteristicResult(characteristic, status == BluetoothGatt.GATT_SUCCESS);
                    }
                });
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            String address = gatt.getDevice().getAddress();
            final BleMessageCallback bleMessageCallback = getBleMessageCallback(address);
            LogUtil.d("onCharacteristicWrite status :"+status+" address : "+ address);
            if (bleMessageCallback != null){
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        bleMessageCallback.writeCharacteristicResult(characteristic, status == BluetoothGatt.GATT_SUCCESS);
                    }
                });
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            String address = gatt.getDevice().getAddress();
            LogUtil.d("onCharacteristicChanged"+" address : "+ address);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            String address = gatt.getDevice().getAddress();
            final BleMessageCallback bleMessageCallback = getBleMessageCallback(address);
            LogUtil.d("onDescriptorRead status :" + status+" address : "+ address);
            if (bleMessageCallback != null){
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        bleMessageCallback.readDescriptorResult(descriptor, status == BluetoothGatt.GATT_SUCCESS);
                    }
                });
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            String address = gatt.getDevice().getAddress();
            final BleMessageCallback bleMessageCallback = getBleMessageCallback(address);
            LogUtil.d("onDescriptorWrite status :" + status+" address : "+ address);
            if (bleMessageCallback != null){
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        bleMessageCallback.writeDescriptorResult(descriptor, status == BluetoothGatt.GATT_SUCCESS);
                    }
                });
            }
        }
//
        private void runOnMainThread(Runnable runnable){
            if (Looper.myLooper() == Looper.getMainLooper()){
                runnable.run();
            }else{
                handler.post(runnable);
            }
        }
    };

    private BleSdk(){

    }

    public void init(Context context){
        this.context = context.getApplicationContext();
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    public static BleSdk getInstance(){
        if (bleSdk == null){
            bleSdk = new BleSdk();
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

    public void connect(BluetoothDevice device, boolean autoConnect, ConnectBleCallback callback){
        if (device==null){
            return;
        }
        String address = device.getAddress();
        setConnectCallback(address, callback);
        checkBle();
        if (!isOpen()){
            if (callback != null) {
                callback.onDisconnected();
            }
            return;
        }
        if (getConnectState(address) == BluetoothProfile.STATE_DISCONNECTED){
            setConnectState(address, BluetoothProfile.STATE_CONNECTING);
            setBluetoothGatt(device.connectGatt(context, autoConnect, gattCallback));
        }
    }

    public void disconnect(String address){
        if (address == null || address.length() == 0){
            return;
        }
        checkBle();
        if (!isOpen()){
            return;
        }
        BluetoothGatt bluetoothGatt = bluetoothGattMap.get(address);
        if (getConnectState(address) == BluetoothProfile.STATE_CONNECTED){
            if (bluetoothGatt != null) {
                setConnectState(address, BluetoothProfile.STATE_DISCONNECTING);
                bluetoothGatt.disconnect();
            }else{
                setConnectState(address, BluetoothProfile.STATE_DISCONNECTED);
            }
        }else if(getConnectState(address) == BluetoothProfile.STATE_CONNECTING){
            close(address);
        }
    }

    public void close(String address){
        if (address == null || address.length() == 0){
            return;
        }
        checkBle();
        if (!isOpen()){
            return;
        }
        setConnectState(address, BluetoothProfile.STATE_DISCONNECTED);
        BluetoothGatt bluetoothGatt = bluetoothGattMap.get(address);
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
        }
    }

    public boolean isConnected(String address){
        if (address == null || address.length() == 0){
            return false;
        }
        return getConnectState(address) == BluetoothProfile.STATE_CONNECTED;
    }

    private void setFailCallback(String address, boolean isCharacteristic, boolean isRead){
        BleMessageCallback bleMessageCallback = getBleMessageCallback(address);
        if (bleMessageCallback == null) {
            return;
        }
        if (isCharacteristic) {
            if (isRead){
                bleMessageCallback.readCharacteristicResult(null, true);
            }else{
                bleMessageCallback.writeCharacteristicResult(null, false);
            }
        }else{
            if (isRead){
                bleMessageCallback.readDescriptorResult(null, false);
            }else{
                bleMessageCallback.writeDescriptorResult(null, false);
            }
        }
    }

    private BluetoothGattCharacteristic initCharacteristic(String address, UUID serviceUuid, UUID characteristicUuid,
                                                           boolean isCharacteristic, boolean isRead){
        BluetoothGattService service = getBluetoothGatt(address).getService(serviceUuid);
        if (service == null){
            setFailCallback(address, isCharacteristic, isRead);
            return null;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
        if (characteristic == null){
            setFailCallback(address, isCharacteristic, isRead);
        }
        return characteristic;
    }

    private BluetoothGattDescriptor initDescriptor(String address, UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid,
                                                   boolean isCharacteristic, boolean isRead){
        BluetoothGattService service = getBluetoothGatt(address).getService(serviceUuid);
        if (service == null){
            setFailCallback(address, isCharacteristic, isRead);
            return null;
        }
        BluetoothGattCharacteristic characteristic = initCharacteristic(address, serviceUuid, characteristicUuid,
                isCharacteristic, isRead);
        if (characteristic == null){
            return null;
        }
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorUuid);
        if (descriptor == null){
            setFailCallback(address, isCharacteristic, isRead);
        }
        return descriptor;
    }

    public boolean writeCharacteristic(String address, UUID serviceUuid, UUID characteristicUuid, byte[] data){
        checkBle();
        if (!isOpen()){
            setFailCallback(address, true, false);
            return false;
        }
        if (getBluetoothGatt(address) == null || data == null || serviceUuid == null || characteristicUuid == null){
            setFailCallback(address, true, false);
            return false;
        }
        BluetoothGattCharacteristic characteristic = initCharacteristic(address, serviceUuid, characteristicUuid, true, false);
        if (characteristic == null){
            return false;
        }
        characteristic.setValue(data);
        return getBluetoothGatt(address).writeCharacteristic(characteristic);
    }

    public boolean readCharacteristic(String address, UUID serviceUuid, UUID characteristicUuid){
        checkBle();
        if (!isOpen()){
            setFailCallback(address, true, true);
            return false;
        }
        if (getBluetoothGatt(address) == null || serviceUuid == null || characteristicUuid == null){
            setFailCallback(address, true, true);
            return false;
        }
        BluetoothGattCharacteristic characteristic = initCharacteristic(address, serviceUuid, characteristicUuid, true, true);
        if (characteristic == null){
            return false;
        }
        return getBluetoothGatt(address).readCharacteristic(characteristic);
    }

    public boolean writeDescriptor(String address, UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid, byte[] data){
        checkBle();
        if (!isOpen()){
            setFailCallback(address, false, false);
            return false;
        }
        if (getBluetoothGatt(address) == null || data == null || serviceUuid == null || characteristicUuid == null || descriptorUuid == null){
            setFailCallback(address, false, false);
            return false;
        }
        BluetoothGattDescriptor descriptor = initDescriptor(address, serviceUuid, characteristicUuid, descriptorUuid, false, false);
        if (descriptor == null){
            return false;
        }
        descriptor.setValue(data);
        return getBluetoothGatt(address).writeDescriptor(descriptor);
    }

    public boolean readDescriptor(String address, UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid){
        checkBle();
        if (!isOpen()){
            setFailCallback(address, false, true);
            return false;
        }
        if (getBluetoothGatt(address) == null || serviceUuid == null || characteristicUuid == null || descriptorUuid == null){
            setFailCallback(address, false, true);
            return false;
        }
        BluetoothGattDescriptor descriptor = initDescriptor(address, serviceUuid, characteristicUuid, descriptorUuid, false, true);
        if (descriptor == null){
            return false;
        }
        return getBluetoothGatt(address).readDescriptor(descriptor);
    }
}
