import { NativeModules, Platform } from "react-native";

type TipoImpresora = { name?: string; friendlyName?: string; serial?: string; mac?: string; address?: string };

const { ZSDKModule } = NativeModules as any;

function asegurarModulo(): void {
    if (!ZSDKModule) {
        throw new Error("ZSDKModule no disponible. ¿Se enlazó el módulo nativo y se corrió prebuild?");
    }
}

export async function descubrirImpresoras(): Promise<TipoImpresora[]> {
    asegurarModulo();
    return new Promise((resolver, rechazar) => {
        ZSDKModule.zsdkPrinterDiscoveryBluetooth((error: string | null, json: string) => {
        if (error) { rechazar(new Error(String(error))); return; }
        try {
            const respuesta = JSON.parse(json || "[]");
            const normalizado: TipoImpresora[] = (Array.isArray(respuesta) ? respuesta : []).map((i: any) => ({
            name: i.name || i.friendlyName,
            friendlyName: i.friendlyName,
            serial: i.serial,
            mac: i.mac || i.address,
            address: i.address
            }));
            resolver(normalizado);
        } catch (e) { rechazar(e as Error); }
        });
    });
}

export async function escribirBluetooth(identificador: string, cpcl: string): Promise<string> {
    asegurarModulo();
    let respuesta: string = "";
    respuesta = await ZSDKModule.zsdkWriteBluetooth(identificador, cpcl);
    return respuesta;
}

export async function consultarBluetooth(identificador: string, comando: string): Promise<string> {
    asegurarModulo();
    if (typeof ZSDKModule.zsdkQueryBluetooth !== "function") {
        throw new Error("Falta zsdkQueryBluetooth en el módulo nativo.");
    }
    let respuesta: string = "";
    respuesta = await ZSDKModule.zsdkQueryBluetooth(identificador, comando);
    return respuesta;
}

export function obtenerIdentificador(impresora: TipoImpresora): string {
    if (Platform.OS === "ios") return String(impresora.serial || "");
    return String(impresora.mac || impresora.address || "");
}