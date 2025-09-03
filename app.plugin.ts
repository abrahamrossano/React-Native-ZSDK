import { ConfigPlugin, withAndroidManifest, withInfoPlist } from "@expo/config-plugins";

const plugin: ConfigPlugin = (config) => {
  config = withInfoPlist(config, (c) => {
    c.modResults.NSBluetoothAlwaysUsageDescription =
      c.modResults.NSBluetoothAlwaysUsageDescription || "La app usa Bluetooth para imprimir en impresoras Zebra.";
    c.modResults.NSBluetoothPeripheralUsageDescription =
      c.modResults.NSBluetoothPeripheralUsageDescription || "Se requiere Bluetooth para descubrir e imprimir.";

    const existingUIBackgroundModes =
      Array.isArray(c.modResults.UIBackgroundModes) ? (c.modResults.UIBackgroundModes as string[]) : [];
    c.modResults.UIBackgroundModes = Array.from(
      new Set([...existingUIBackgroundModes, "bluetooth-central"])
    );

    const existingProtocols =
      Array.isArray(c.modResults.UISupportedExternalAccessoryProtocols)
        ? (c.modResults.UISupportedExternalAccessoryProtocols as string[])
        : [];
    c.modResults.UISupportedExternalAccessoryProtocols = Array.from(
      new Set([...existingProtocols, "com.zebra.rawport"])
    );
    return c;
  });

  config = withAndroidManifest(config, (c) => {
    const manifest = c.modResults;
    const permisos = [
      "android.permission.BLUETOOTH",
      "android.permission.BLUETOOTH_ADMIN",
      "android.permission.ACCESS_COARSE_LOCATION",
      "android.permission.ACCESS_FINE_LOCATION",
      "android.permission.BLUETOOTH_SCAN",
      "android.permission.BLUETOOTH_CONNECT"
    ];
    const existentes = manifest.manifest["uses-permission"] || [];
    const faltantes = permisos
      .filter((p) => !existentes.some((u: any) => u.$["android:name"] === p))
      .map((p) => ({ $: { "android:name": p } }));
    manifest.manifest["uses-permission"] = [...existentes, ...faltantes];
    return c;
  });

  return config;
};

export default plugin;