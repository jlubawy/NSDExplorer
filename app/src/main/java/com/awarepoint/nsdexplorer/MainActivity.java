package com.awarepoint.nsdexplorer;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final String NSD_SERVICE_NAME = "NSDExplorer";
    private static final String NSD_SERVICE_TYPE = "_custom._tcp."; // must have service on top of TCP or else we get an internal error
    private static final int NSD_SERVICE_PORT = 80;

    private TextView txtServices;

    private NsdManager nsdManager;
    private final NsdServiceInfo nsdExplorerServiceInfo = new NsdServiceInfo();
    private boolean isResolving = false;

    private final NsdManager.RegistrationListener registrationListener = new NsdManager.RegistrationListener() {
        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "onRegistrationFailed: " + errorCode);
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "onUnregistrationFailed: " + errorCode);
        }

        @Override
        public void onServiceRegistered(NsdServiceInfo serviceInfo) {
            Log.i(TAG, "onServiceRegistered: " + serviceInfo.toString());
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            Log.i(TAG, "onServiceUnregistered: " + serviceInfo.toString());
        }
    };

    private final NsdManager.ResolveListener resolveListener = new NsdManager.ResolveListener() {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "onResolveFailed: " + errorCode);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.i(TAG, "onServiceResolved: " + serviceInfo.toString());

            final String msg = "[RESOLVED] " + serviceInfo.toString();

            Log.i(TAG, msg);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txtServices.append(msg + "\n");
                }
            });
        }
    };

    private final NsdManager.DiscoveryListener discoveryListener = new NsdManager.DiscoveryListener() {
        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "onStartDiscoveryFailed: " + errorCode);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "onStopDiscoveryFailed: " + errorCode);
        }

        @Override
        public void onDiscoveryStarted(String serviceType) {
            Log.i(TAG, "onDiscoveryStarted");
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.i(TAG, "onDiscoveryStopped");
        }

        @Override
        public void onServiceFound(NsdServiceInfo serviceInfo) {
            Log.i(TAG, "onServiceFound: " + serviceInfo.toString());

            // Log all found services
            final String msg = "[FOUND] " + serviceInfo.toString();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txtServices.append(msg + "\n");
                }
            });

            if (serviceInfo.getServiceName().equals(NSD_SERVICE_NAME)) {
                Log.i(TAG, msg);

                if (!isResolving) {
                    nsdManager.resolveService(serviceInfo, resolveListener);
                    isResolving = true;
                }
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo serviceInfo) {
            Log.i(TAG, "onServiceLost: " + serviceInfo.toString());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate");

        txtServices = (TextView) findViewById(R.id.txtServices);

        nsdManager = (NsdManager) getSystemService(NSD_SERVICE);

        // Get the device's hostname
        final InetAddress deviceHostname = getDeviceHostname();

        // Create the service info
        nsdExplorerServiceInfo.setServiceName(NSD_SERVICE_NAME);
        nsdExplorerServiceInfo.setServiceType(NSD_SERVICE_TYPE);
        nsdExplorerServiceInfo.setHost(deviceHostname);
        nsdExplorerServiceInfo.setPort(NSD_SERVICE_PORT);

        // Register the service
        nsdManager.registerService(nsdExplorerServiceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        stopDiscovery();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onPause");
        startDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        nsdManager.unregisterService(registrationListener);
    }

    private void startDiscovery() {
        isResolving = false;
        txtServices.setText("");
        nsdManager.discoverServices(NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    private void stopDiscovery() {
        nsdManager.stopServiceDiscovery(discoveryListener);
    }

    private InetAddress getDeviceHostname() {
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress inetAddress : Collections.list(networkInterface.getInetAddresses())) {
                    if (!inetAddress.isLoopbackAddress()) {
                        final String hostAddress = inetAddress.getHostAddress();
                        final boolean isIPv4 = hostAddress.indexOf(':') < 0;

                        Log.d(TAG, "getDeviceHostname: checking " + hostAddress);

                        if (isIPv4) {
                            Log.d(TAG, "getDeviceHostname: found " + inetAddress.toString());
                            return inetAddress;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return null;
    }
}
