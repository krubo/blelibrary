package com.krubo.blelibrary.callback;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

/**
 * @author krubo
 * @created 2017/8/23
 */

public class BleMessageCallback {

    public void readCharacteristicResult(BluetoothGattCharacteristic characteristic, boolean isSuccess){};

    public void writeCharacteristicResult(BluetoothGattCharacteristic characteristic, boolean isSuccess){};

    public void readDescriptorResult(BluetoothGattDescriptor descriptor, boolean isSuccess){};

    public void writeDescriptorResult(BluetoothGattDescriptor descriptor, boolean isSuccess){};
}
