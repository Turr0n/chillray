# Mando Aire

App Android mínima (≈17 KB) para encender y apagar aires acondicionados **Mundo Clima** y otros equipos de base Midea usando el emisor de infrarrojos del teléfono.

## Descarga

[MandoAire.apk](MandoAire.apk). Requiere Android 4.4 o superior y un teléfono con emisor IR (muchos Xiaomi, Redmi y POCO lo tienen).

## Funciones

- Encender y apagar
- Temperatura de 17 a 30 °C
- Modos: Frío, Seco, Auto, Calor y Ventilador
- Dos protocolos: **A (Coolix)**, el de los mandos Midea RG57/R51, y **B (Midea de 48 bits)**, el de los mandos RG66/R09C. Si el aire no responde con uno, prueba el otro.

## Compilar

Sin Gradle, con `aapt2`, `javac`, `d8` y `apksigner`. El script espera el JDK y el SDK en el directorio padre (`../jdk-*`, `../sdk`):

```sh
./build.sh
```

## Créditos

Los protocolos IR están portados de [IRremoteESP8266](https://github.com/crankyoldgit/IRremoteESP8266).
