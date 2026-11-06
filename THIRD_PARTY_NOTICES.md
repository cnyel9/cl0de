# Third-party notices

Cl0de's own code is MIT, see `LICENSE`. The app also ships third-party
components under their own licenses. Full license texts are in `licenses/`.

## Native binaries (app/src/main/jniLibs, prebuilt from Termux packages, unmodified)

- **PRoot** (`libproot.so`, `libproot-loader.so`): GNU GPL v2.0, text in
  `licenses/GPL-2.0.txt`. Corresponding source: the Termux build of PRoot,
  https://github.com/termux/proot with the package recipe in
  https://github.com/termux/termux-packages
- **libtalloc** (`libtalloc.so`): GNU LGPL v3.0, text in
  `licenses/LGPL-3.0.txt`. Upstream is part of the Samba project; the binary
  shipped here is the Termux package build (termux-packages, above).
- **libandroid-shmem** (`libandroid-shmem.so`): BSD-3-Clause.
  Source: https://github.com/termux/libandroid-shmem

## Fonts (app/src/main/res/font)

Inter, Source Serif 4, Unbounded and JetBrains Mono are licensed under the
SIL Open Font License 1.1, text in `licenses/OFL-1.1.txt`.

## CA certificate bundle

`app/src/main/assets/cacert.pem` is the Mozilla CA certificate bundle
(as extracted by the curl project from Mozilla's certdata). The bundle is
made available under MPL-2.0 terms by Mozilla; the file header inside the
bundle itself records its origin.

## Libraries compiled from Maven

AndroidX (Compose, activity, fragment), Kotlin and kotlinx libraries,
OkHttp/Okio and Coil are Apache-2.0, text in `licenses/Apache-2.0.txt`.
The Play services code scanner (`play-services-code-scanner`) is a
proprietary Google library used under Google's API terms; it is not open
source.

## Downloaded at first launch (not part of the APK)

Ubuntu Base 24.04 (from Canonical) and OpenCode 1.19.4 (MIT, from the
OpenCode project) are downloaded by the app on first run and keep their
own licenses.
