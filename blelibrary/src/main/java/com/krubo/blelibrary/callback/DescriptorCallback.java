package com.krubo.blelibrary.callback;

import android.bluetooth.BluetoothGattDescriptor;

/**
 * @author krubo
 * @created 2017/8/23
 */

public interface DescriptorCallback {

    void readResult(BluetoothGattDescriptor descriptor, boolean isSuccess);

    void writeResult(BluetoothGattDescriptor descriptor, boolean isSuccess);
}
