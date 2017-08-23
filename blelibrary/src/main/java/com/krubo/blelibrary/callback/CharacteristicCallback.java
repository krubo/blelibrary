package com.krubo.blelibrary.callback;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * @author krubo
 * @created 2017/8/23
 */

public interface CharacteristicCallback {

    void readResult(BluetoothGattCharacteristic characteristic, boolean isSuccess);

    void writeResult(BluetoothGattCharacteristic characteristic, boolean isSuccess);
}
