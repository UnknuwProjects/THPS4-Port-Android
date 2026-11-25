# THPS4-Port-Android

[English](README.md) · **Bahasa Indonesia** · [Español](README.es.md) · [Português](README.pt.md)

Port Android tidak resmi dari **Tony Hawk's Pro Skater 4** (Xbox versi USA,
title ID `41560017` / AV-023). Kode x86 Xbox-nya dikompilasi ulang secara statis
menjadi C++ dengan [XboxXBE-Recomp](https://github.com/TGP482/XboxXBE-Recomp)
lalu dibangun untuk arm64 — tanpa emulator, game berjalan sebagai kode native
di HP.

> [!NOTE]
> Proyek ini tidak menyertakan berkas game apa pun. Kamu perlu berkas dari
> salinan game milikmu sendiri yang diperoleh secara legal (`default.xbe` /
> image disc versi USA) untuk membangun atau menjalankannya.

## Status

Masih dikembangkan secara aktif. Game sudah bisa dimainkan dari awal sampai
akhir di perangkat sungguhan, dan build publik tersedia di
[repo rilis](https://github.com/UnknuwProjects/THPS4-Android-Releases).
Aplikasi Android dan tools proyek sudah ada di sini; kode native — game hasil
rekompilasi dan penghubungnya — menyusul bersama rilis **1.0.4**.

## Cara kerja

- **Rekompilasi statis** — XBE diterjemahkan menjadi C++ sejak awal, lalu
  dikompilasi untuk arm64-v8a. Tidak ada yang diinterpretasikan saat game jalan.
- **Dukungan XDK 4928** — THPS4 dibangun dengan XDK 4928 yang belum didukung
  XboxXBE-Recomp. Proyek ini yang mem-bootstrap-nya: signature fungsi dan layout
  perangkat dipulihkan dari game itu sendiri dan diverifikasi satu per satu.
- **Render** — lapisan D3D8 yang ditulis ulang di atas Vulkan 1.1, dengan opsi
  driver GPU custom (mis. Turnip di Adreno).
- **Port menyatu dengan game** — pengaturan grafis, bahasa, dan credit berada
  di menu Options bawaan game sebagai *Pengaturan Port*, dalam empat bahasa:
  Inggris, Bahasa Indonesia, Español, dan Português.
- **Main online** — mode 2 pemain lokal dan System Link berjalan lewat internet
  melalui server komunitas kecil, dengan fallback Wi-Fi saat server tidak
  terjangkau.
- **Tambahan** — kontrol sentuh dan gamepad yang bisa di-remap, paket mod,
  soundtrack custom dari berkas WMA milikmu, dan preset low-end dengan resolusi
  dinamis untuk HP kelas bawah.

## Aplikasi

| | |
|---|---|
| Nama aplikasi | **THPS4** |
| Paket | `com.unknuw.thps4.unofficial` |
| Android | 10–16 (minSdk 29, targetSdk 36), arm64-v8a |

## Build resmi

APK resmi ditandatangani dengan satu sertifikat. Periksa sebelum memercayai
build yang mengaku sebagai port ini:

```
SHA-256: 12474f52e9b6a461bceb2af6882a0543d8dc6e49fc8fcb06ac25de83489885bb
```

Game juga memeriksa tanda tangannya sendiri saat start dan menolak berjalan
kalau tidak cocok.

## Credit

- [XboxXBE-Recomp](https://github.com/TGP482/XboxXBE-Recomp) oleh [**TGP482**](https://github.com/TGP482) —
  recompiler dan runtime SDK yang menjadi dasar port ini
- [XboxXBERecomp](https://github.com/UnknuwProjects/XboxXBERecomp) oleh
  [**UNKNUW**](https://github.com/UnknuwProjects) — fork proyek ini,
  dengan dukungan XDK 4928 dan perubahan runtime yang dibutuhkan port ini
- [XbSymbolDatabase](https://github.com/Cxbx-Reloaded/XbSymbolDatabase)
  (Cxbx-Reloaded) — pola OOVPA untuk pemeriksaan silang
- [SDL3](https://github.com/libsdl-org/SDL) — windowing, input, dan audio di Android
- [playit.gg](https://playit.gg) — tunnel untuk server komunitas

Dukung proyek ini:
[Sociabuzz](https://sociabuzz.com/unknuw/tribe) ·
[Saweria](https://saweria.co/Unknuw)

Nemu bug? Buka issue di repo ini.

## Lisensi

Proyek ini berlisensi **GNU General Public License v3.0** (GPL-3.0) —
lihat [LICENSE](LICENSE). SDK XboxXBE-Recomp yang disertakan mempertahankan
lisensinya sendiri.

*Proyek fan tidak resmi. Tidak berafiliasi dengan Activision atau Neversoft.
Tony Hawk's Pro Skater 4 adalah merek dagang pemiliknya masing-masing.*
