# Status Monitor

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A lightweight Android client for [**Status**](https://github.com/dani3l0/Status), the simple system monitor for small Linux home servers. Add your server once, open the app, and see CPU, memory, storage, network and process information in real time.

> **Home-lab app.** Status Monitor is built for **personal home servers on a trusted local network** (mini PCs, Raspberry Pis, TV boxes, NAS boxes). It is **not designed for public internet exposure** and should not be used without a trusted network boundary.

---

## Screenshots

<!--
  Add your screenshots to docs/screenshots/ using the file names below
  (or change the paths). Recommended: PNG or WebP, portrait, ~1080 px wide.
-->

| Dashboard | CPU | Memory |
|:---:|:---:|:---:|
| ![Dashboard](/assets/Dashboard.png) | ![CPU details](/assets/cpu.png) | ![Memory details](/assets/ram.png) |

| Storage | Network | Add server |
|:---:|:---:|:---:|
| ![Storage details](/assets/storage.png) | ![Network details](/assets/net.png) | ![Add server dialog](/assets/add-server.png) |

| Dark theme | Light theme | Servers (Settings) |
|:---:|:---:|:---:|
| ![Dark theme](/assets/Dashboard.png) | ![Light theme](/assets/light.png) | ![Server list](/assets/settings.png) |

---

## Features

- **Real data only.** No demo mode and no sample values. The first launch asks you to add a server, and nothing is shown until you do.
- **One or many servers.** Add as many Status servers as you like from Settings. With a single server the app shows just that server; with several, it opens the one you last selected.
- **Dashboard** with gauges for CPU load and temperature, memory, storage and network, plus a System card (hostname, OS, uptime, load average, process count).
- **Detail screens** for CPU, Memory, Storage and Network with smooth live charts (drawn with Compose Canvas, no chart libraries).
- **Light and dark theme** with a toggle on every screen. The choice is saved, and the system theme is used on first launch.
- **Graceful with missing data.** Sensors that a device does not expose (for example CPU temperature or frequency on some ARM boards) show `N/A` or a short "not available" message instead of crashing the app.
- **Test connection** before saving, with the real error message if it fails.
- **Built for low-end phones.** Few dependencies, polling only while the app is visible, only the active server is polled, and a "Reduce animations" switch.

## Requirements

- An Android phone running **Android 7.0 (API 24) or newer**.
- A Linux server running **Status** (the `dani3l0/Status` project), reachable from your phone on the same network, for example `http://192.168.1.50:9090`.

## Setting up the server

Install and run Status on your server by following the [official instructions](https://github.com/dani3l0/Status). By default it listens on port `9090`. The app reads `GET /api/status`, so make sure the endpoint is reachable from your phone.

## Using the app

1. Open the app and tap **Add server**.
2. Choose **HTTP** or **HTTPS**, enter the **IP address or domain** and the **port** (default `9090`). The path and the display name are optional.
3. Tap **Test connection**, then **Save**.
4. To add, edit, delete or switch servers later, open **Settings, Servers**.

## Building from source

1. Install the latest stable **Android Studio**.
2. Clone this repository and open it in Android Studio.
3. Let Gradle sync, then run the `app` configuration on a device or emulator.
4. For a release build, use **Build, Generate Signed Bundle / APK** (R8 minification and resource shrinking are enabled).

Tech stack: Kotlin, Jetpack Compose (Material 3), Navigation Compose, ViewModel and StateFlow, DataStore, OkHttp, kotlinx.serialization.

## Intended use and security

Status Monitor is a **hobby project for home servers**. Please keep the following in mind:

- **Home and LAN use only.** Use it on a network you trust. Do not expose your Status server to the public internet (no port forwarding to it).
- **No authentication and no encryption by default.** Status itself has no login, and the app connects over plain HTTP unless you set up HTTPS yourself. Anyone who can reach the server's port can access the status data.
- **Not for enterprise or production environments.** There is no multi-user access control, audit logging, SLA, or hardening, and the app has not been security-reviewed for such use.
- **Your data stays with you.** The app is designed to talk only to the servers you add. It does not include ads or analytics, and stores its settings locally on your phone.
- **Provided as is, without warranty.** You are responsible for the security of your own network and devices.

If you need to reach your server from outside your home, put it behind a VPN (for example WireGuard or Tailscale) instead of opening ports to the internet.

## Troubleshooting

| Problem | What to check |
|---|---|
| "Cannot connect" when testing | Open `http://<server-ip>:<port>/api/status` in the phone's browser. Make sure the phone and server are on the same network, and turn off mobile data while testing. |
| Temperature or frequency shows `N/A` | The server's kernel does not expose these sensors (common on ARM TV boxes). This is normal. |
| Storage shows `0` or is missing | Status could not read the mounted filesystems. This can happen when it runs inside a chroot or container. |
| Network chart starts at zero | The rate is calculated from two consecutive readings, so it appears after the second update. |

## Credits

- [**Status**](https://github.com/dani3l0/Status) by dani3l0 is the server this app talks to. This project is an independent client and is **not affiliated with or endorsed by** its author.

## License

This project is licensed under the [MIT License](LICENSE).

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
