package com.krubo.blelibrary.callback;

/**
 * @author krubo
 * @created 2017/8/22
 */

public interface ConnectBleCallback {

    void onConnected();

    void onDisconnected();

    void onServicesDiscoveredSuccess();

    void onServicesDiscoveredFail();
}
