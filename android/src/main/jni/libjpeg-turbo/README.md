# libjpeg-turbo for Android

This directory contains a trimmed and configured version of **libjpeg-turbo** for Android NDK build.

## 1. Source Origin
- **Version**: libjpeg-turbo 1.5.3
- **Source**: Retrieved from official GitHub release tarball (source code).

## 2. Build System
- **Toolchain**: Android NDK (`ndk-build`)
- **Configuration**: Uses `Android.mk` manually configured for Android environment.

## 3. Supported Architectures
- `armeabi-v7a` (with NEON)
- `arm64-v8a` (with NEON)
- `x86_64` (with SSE/AVX2)

*Note: Legacy `armeabi`, `mips`, and 32-bit `x86` architectures have been removed.*

## 4. Modifications & Cleanup
To reduce repository size and simplify the build process, the original source tree has been significantly trimmed:

### Removed
- **Build Scripts**: Removed `CMakeLists.txt`, `Makefile.am`, `configure.ac`, and all Autotools/CMake related files.
- **Documentation**: Removed `doc/`, man pages (`*.1`), and miscellaneous text files (`*.txt`, `*.md` except this one).
- **Wrappers & Tests**: Removed `java/` (JNI wrappers), `testimages/`, unit tests (`tjunittest.c`, `tjbench.c`), and CLI tools (`cjpeg.c`, `djpeg.c`, `jpegtran.c`).
- **Platform Support**: Removed Windows (`win/`) and other non-Android specific files.

### Configuration Changes
- **Directory Name**: Renamed from `libjpeg-turbo-1.5.0` to `libjpeg-turbo`.
- **Android.mk Refactoring**: 
  - Rewrote `Android.mk` to build the **Shared Library** (`libjpeg-turbo1500.so`) directly from source files.
  - Removed the intermediate Static Library build step.
  - Removed configuration blocks for unsupported architectures (`x86`, `mips`, `armeabi`).
- **Header Configuration**: 
  - Manually provided `jconfig.h` configured for standard Android NDK environment.

## 5. Usage
This library is linked by `libuvc` and `UVCCamera` native modules.

```makefile
# In dependent Android.mk
LOCAL_SHARED_LIBRARIES += jpeg-turbo1500
```
