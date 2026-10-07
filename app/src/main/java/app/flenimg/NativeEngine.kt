package app.flenimg
object NativeEngine {
    init { try { System.loadLibrary("flen_engine") } catch (_: Throwable) {} }
    external fun nativeInfo(): String?
}
