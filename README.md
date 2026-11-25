# THPS4-Port-Android

**English** · [Bahasa Indonesia](README.id.md) · [Español](README.es.md) · [Português](README.pt.md)

Unofficial native Android port of **Tony Hawk's Pro Skater 4** (original Xbox, USA
release, title ID `41560017` / AV-023). The Xbox x86 code is statically recompiled to
C++ with [XboxXBE-Recomp](https://github.com/TGP482/XboxXBE-Recomp) and built for
arm64 — no emulator involved, the game runs as native code on the phone.

> [!NOTE]
> This project does not include any game assets. You need the files from your own
> legally acquired copy of the game (the USA `default.xbe` / disc image) to build
> or run it.

## Status

In active development. The game boots and plays end to end on real hardware, and
public builds are available from the
[releases repo](https://github.com/UnknuwProjects/THPS4-Android-Releases).
The Android app and the project tools are already here; the native code —
the recompiled game and the glue around it — follows with the **1.0.4**
release.

## How it works

- **Static recompilation** — the XBE is translated to C++ ahead of time, then
  compiled for arm64-v8a. Nothing is interpreted at runtime.
- **XDK 4928 support** — THPS4 was built with XDK 4928, which XboxXBE-Recomp did
  not cover. This project bootstrapped it: function signatures and the device
  layout were recovered from the game itself and verified field by field.
- **Rendering** — a reimplemented D3D8 layer on top of Vulkan 1.1, with optional
  custom GPU drivers (e.g. Turnip on Adreno).
- **The port lives inside the game** — graphics settings, language and credits
  sit in the game's own Options menu as *Port Settings*, in four languages:
  English, Bahasa Indonesia, Español and Português.
- **Online play** — the local 2-player mode and System Link run over the
  internet through a small community server, with a Wi-Fi fallback when no
  server is reachable.
- **Extras** — touch and gamepad controls with remapping, mod packs, custom
  soundtracks from your own WMA files, and a low-end preset with dynamic
  resolution for weaker phones.

## The app

| | |
|---|---|
| App name | **THPS4** |
| Package | `com.unknuw.thps4.unofficial` |
| Android | 10–16 (minSdk 29, targetSdk 36), arm64-v8a |

## Official builds

Official APKs are signed with one certificate. Check it before you trust a build
that claims to be this port:

```
SHA-256: 12474f52e9b6a461bceb2af6882a0543d8dc6e49fc8fcb06ac25de83489885bb
```

The game also verifies its own signature at startup and refuses to run when it
does not match.

## Credits

- [XboxXBE-Recomp](https://github.com/TGP482/XboxXBE-Recomp) by [**TGP482**](https://github.com/TGP482) — the
  recompiler and SDK runtime this port is built on
- [XboxXBERecomp](https://github.com/UnknuwProjects/XboxXBERecomp) by
  [**UNKNUW**](https://github.com/UnknuwProjects) — this project's fork,
  with XDK 4928 support and the runtime changes this port needs
- [XbSymbolDatabase](https://github.com/Cxbx-Reloaded/XbSymbolDatabase)
  (Cxbx-Reloaded) — OOVPA patterns used for cross-checking
- [SDL3](https://github.com/libsdl-org/SDL) — windowing, input and audio on Android
- [playit.gg](https://playit.gg) — the tunnel that hosts the community server

Support the project:
[Sociabuzz](https://sociabuzz.com/unknuw/tribe) ·
[Saweria](https://saweria.co/Unknuw)

Found a bug? Open an issue in this repo.

## License

This project is licensed under the **GNU General Public License v3.0** (GPL-3.0) —
see [LICENSE](LICENSE). The vendored XboxXBE-Recomp SDK keeps its own license.

*Unofficial fan project. Not affiliated with Activision or Neversoft.
Tony Hawk's Pro Skater 4 is a trademark of its respective owners.*
