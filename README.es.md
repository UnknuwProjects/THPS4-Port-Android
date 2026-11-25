# THPS4-Port-Android

[English](README.md) · [Bahasa Indonesia](README.id.md) · **Español** · [Português](README.pt.md)

Port no oficial de **Tony Hawk's Pro Skater 4** (Xbox original, versión USA,
title ID `41560017` / AV-023) para Android. El código x86 de Xbox se recompila
de forma estática a C++ con
[XboxXBE-Recomp](https://github.com/TGP482/XboxXBE-Recomp) y se compila para
arm64: sin emulador, el juego corre como código nativo en el teléfono.

> [!NOTE]
> Este proyecto no incluye ningún archivo del juego. Necesitas los archivos de
> tu propia copia del juego, adquirida legalmente (el `default.xbe` / la imagen
> de disco de la versión USA), para compilarlo o jugarlo.

## Estado

En desarrollo activo. El juego arranca y se puede jugar de principio a fin en
hardware real, y hay builds públicas en el
[repositorio de versiones](https://github.com/UnknuwProjects/THPS4-Android-Releases).
La app de Android y las herramientas del proyecto ya están aquí; el código
nativo — el juego recompilado y la capa que lo une — llegará con la versión
**1.0.4**.

## Cómo funciona

- **Recompilación estática**: el XBE se traduce a C++ de antemano y se compila
  para arm64-v8a. No se interpreta nada en tiempo de ejecución.
- **Soporte de XDK 4928**: THPS4 se creó con el XDK 4928, que XboxXBE-Recomp no
  cubría. Este proyecto lo puso en marcha: las firmas de funciones y el layout
  del dispositivo se recuperaron del propio juego y se verificaron campo a campo.
- **Renderizado**: una capa D3D8 reimplementada sobre Vulkan 1.1, con drivers
  de GPU personalizados opcionales (p. ej. Turnip en Adreno).
- **El port vive dentro del juego**: los ajustes gráficos, el idioma y los
  créditos están en el menú Options del propio juego como *Ajustes del Port*,
  en cuatro idiomas: inglés, Bahasa Indonesia, español y portugués.
- **Juego en línea**: el modo local de 2 jugadores y System Link funcionan por
  internet a través de un pequeño servidor comunitario, con respaldo por Wi-Fi
  cuando no hay servidor disponible.
- **Extras**: controles táctiles y de mando con reasignación, paquetes de mods,
  bandas sonoras personalizadas desde tus propios archivos WMA, y un ajuste
  para gamas bajas con resolución dinámica para teléfonos más modestos.

## La app

| | |
|---|---|
| Nombre | **THPS4** |
| Paquete | `com.unknuw.thps4.unofficial` |
| Android | 10–16 (minSdk 29, targetSdk 36), arm64-v8a |

## Builds oficiales

Los APK oficiales se firman con un único certificado. Compruébalo antes de
confiar en una build que se presente como este port:

```
SHA-256: 12474f52e9b6a461bceb2af6882a0543d8dc6e49fc8fcb06ac25de83489885bb
```

El juego también verifica su propia firma al arrancar y se niega a funcionar
si no coincide.

## Créditos

- [XboxXBE-Recomp](https://github.com/TGP482/XboxXBE-Recomp), de [**TGP482**](https://github.com/TGP482): el
  recompilador y el runtime SDK sobre los que se construye este port
- [XboxXBERecomp](https://github.com/UnknuwProjects/XboxXBERecomp), de
  [**UNKNUW**](https://github.com/UnknuwProjects): el fork de este
  proyecto, con soporte para XDK 4928 y los cambios de runtime que
  necesita este port
- [XbSymbolDatabase](https://github.com/Cxbx-Reloaded/XbSymbolDatabase)
  (Cxbx-Reloaded): patrones OOVPA usados para la verificación cruzada
- [SDL3](https://github.com/libsdl-org/SDL): ventanas, entrada y audio en Android
- [playit.gg](https://playit.gg): el túnel que aloja el servidor comunitario

Apoya el proyecto:
[Sociabuzz](https://sociabuzz.com/unknuw/tribe) ·
[Saweria](https://saweria.co/Unknuw)

¿Encontraste un error? Abre un issue en este repositorio.

## Licencia

Este proyecto se publica bajo la **GNU General Public License v3.0** (GPL-3.0);
consulta [LICENSE](LICENSE). El SDK de XboxXBE-Recomp incluido conserva su
propia licencia.

*Proyecto de fans no oficial. Sin afiliación con Activision ni Neversoft.
Tony Hawk's Pro Skater 4 es una marca de sus respectivos propietarios.*
