# Flen.img

Fast, local image & video enhancement for Android.

## Stack
Kotlin + Jetpack Compose for Android UX; Rust for native orchestration; Android MediaCodec for hardware-accelerated video I/O; pluggable AI runtime for Android-native CPU/GPU/NPU execution.

## Product surface
Image/video input, URL input, 2x/4x/8x AI scaling, custom resolution, Super HD, HQ HEVC, HDR10, lossless/archive output, AI image/video presets, denoise, sharpen, deband, deblock, deinterlace, color controls, film grain and frame-rate processing.

## Performance
Kotlin UI -> Rust job engine -> hardware decode -> native enhancement -> hardware encode.

## Build
GitHub Actions builds arm64-v8a and x86_64 debug APKs.

The compatibility backend currently expects a local processing runtime on the device. The native engine is isolated so it can later be replaced with Android-native inference and media processing without redesigning the app.
