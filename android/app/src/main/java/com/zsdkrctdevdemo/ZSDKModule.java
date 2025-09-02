package com.zsdkrctdevdemo;

import androidx.annotation.NonNull;
import android.util.Log;

import com.facebook.react.bridge.*;
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.discovery.BluetoothDiscoverer;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveredPrinterBluetooth;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ZSDKModule extends ReactContextBaseJavaModule {
    private static final String TAG = "ZSDKModule";
    private final ReactApplicationContext contexto;

    public ZSDKModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.contexto = reactContext;
    }

    @NonNull
    @Override
    public String getName() {
        return "ZSDKModule";
    }

    // Descubrir impresoras Bluetooth clásico
    @ReactMethod
    public void zsdkPrinterDiscoveryBluetooth(Callback callback) {
        new Thread(() -> {
            try {
                List<JSONObject> lista = new ArrayList<>();
                BluetoothDiscoverer.findPrinters(contexto, new DiscoveryHandler() {
                    @Override public void foundPrinter(DiscoveredPrinter printer) {
                        try {
                            JSONObject obj = new JSONObject();
                            obj.put("address", printer.address); // MAC
                            if (printer instanceof DiscoveredPrinterBluetooth) {
                                obj.put("friendlyName", ((DiscoveredPrinterBluetooth) printer).friendlyName);
                            }
                            lista.add(obj);
                        } catch (Exception ignore) {}
                    }
                    @Override public void discoveryFinished() {
                        JSONArray arr = new JSONArray(lista);
                        callback.invoke(null, arr.toString());
                    }
                    @Override public void discoveryError(String message) {
                        callback.invoke("discoveryError: " + message, "[]");
                    }
                });
            } catch (Exception e) {
                callback.invoke("exception: " + e.getMessage(), "[]");
            }
        }).start();
    }

    // Escribir datos (CPCL/ZPL) por Bluetooth MAC
    @ReactMethod
    public void zsdkWriteBluetooth(String mac, String data, Promise promise) {
        new Thread(() -> {
            Connection conn = null;
            try {
                conn = new BluetoothConnection(mac);
                conn.open();
                conn.write(data.getBytes(StandardCharsets.UTF_8));
                promise.resolve("Impresión enviada");
            } catch (Exception e) {
                promise.reject("print_error", e.getMessage(), e);
            } finally {
                try { if (conn != null) conn.close(); } catch (Exception ignore) {}
            }
        }).start();
    }

    // Enviar comando y leer respuesta (p.ej. ! U1 getvar "file.dir")
    @ReactMethod
    public void zsdkQueryBluetooth(String mac, String data, Promise promise) {
        new Thread(() -> {
            Connection conn = null;
            try {
                conn = new BluetoothConnection(mac);
                conn.open();
                conn.write(data.getBytes(StandardCharsets.UTF_8));

                // Espera breve para que la impresora responda
                Thread.sleep(250);

                // Lee hasta 4 KB o termina por timeout
                byte[] acumulado = new byte[0];
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < 1500) {
                    try {
                        byte[] chunk = conn.read(); // Link-OS SDK: devuelve null/[] si no hay datos
                        if (chunk != null && chunk.length > 0) {
                            byte[] nuevo = new byte[acumulado.length + chunk.length];
                            System.arraycopy(acumulado, 0, nuevo, 0, acumulado.length);
                            System.arraycopy(chunk, 0, nuevo, acumulado.length, chunk.length);
                            acumulado = nuevo;
                            if (acumulado.length >= 4096) break;
                        } else {
                            break;
                        }
                    } catch (ConnectionException e) {
                        break;
                    }
                }

                String respuesta = new String(acumulado, StandardCharsets.UTF_8);
                promise.resolve(respuesta);
            } catch (Exception e) {
                promise.reject("query_error", e.getMessage(), e);
            } finally {
                try { if (conn != null) conn.close(); } catch (Exception ignore) {}
            }
        }).start();
    }
}