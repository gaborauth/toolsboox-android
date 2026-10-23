package com.toolsboox.ot

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.os.Parcel
import timber.log.Timber

/**
 * Viwoods AiPaper fast-ink backend.
 *
 * Background: true hardware fast-ink (the T1000 AutoDraw / `initWriting` path used by
 * Viwoods' own apps) is NOT reachable from a sideloaded app — `libpaintworker.so`
 * crashes in JNI_OnLoad on an `untrusted_app` SELinux context (it needs `/dev/t1000_spi`,
 * the display compositor and `/dev/input`, all system-only). See jdkruzr's RE notes:
 * github.com/jdkruzr/ViwoodsAppDev. That path needs root or a system/product-app install.
 *
 * What works WITHOUT root: switch the panel to the FAST e-ink waveform via the
 * `ENoteSetting` binder service (direct IBinder.transact from our process), so the app's
 * own SurfaceView stroke posts refresh quickly instead of using the slow GL16 reading
 * waveform. The app still renders the strokes itself (software). The AutoDraw enable
 * transacts are also fired in case a given firmware unit happens to honour them, but the
 * FAST waveform is what produces the visible speed-up.
 *
 * Every reflective/binder call is guarded; inert on non-Viwoods hardware. Gate on
 * [isAvailable] before constructing.
 */
class ViwoodsFastInk {

    private var enote: Any? = null          // android.os.enote.ENoteSetting.getInstance()
    private var appContext: Context? = null

    var attached: Boolean = false
        private set

    /**
     * True once [initWriting] has been activated — i.e. the native full-screen T1000 fast-ink
     * is rendering pen strokes in hardware. Only possible on the targetSdk-30 (viwoods) build.
     * When true the app must NOT software-draw the live stroke (the hardware does it).
     */
    var hardwareInk: Boolean = false
        private set

    private var initWritingDone = false

    /** Read a system property via reflection (hidden-API bypass is active). */
    private fun getProp(key: String): String = try {
        Class.forName("android.os.SystemProperties")
            .getMethod("get", String::class.java).invoke(null, key) as? String ?: ""
    } catch (_: Throwable) { "" }

    companion object {
        private const val ENOTE_CLASS = "android.os.enote.ENoteSetting"
        private const val IFACE_TOKEN = "android.os.enote.IENoteSetting"
        private const val SERVICE_NAME = "ENoteSetting"

        const val MODE_FAST = 4
        const val MODE_GL16 = 3
        const val MODE_GC = 17

        // Transaction codes from IENoteSetting.Stub (jdkruzr's decompilation).
        private const val TXN_SET_AUTODRAW_ENABLE = 20
        private const val TXN_SET_AUTODRAW_TOOLTYPE = 21
        private const val TXN_SET_AUTODRAW_PENWIDTH = 23
        private const val TXN_ADD_AUTODRAW_RECT = 24
        private const val TXN_SET_ALL_REGION_UNAUTODRAW = 28
        private const val TXN_STOP_HANDWRITE_INTERCEPT = 29
        private const val TXN_SET_PICTURE_MODE = 13

        private var bypassDone = false

        /** Lift hidden-API enforcement process-wide (needed at high targetSdk). Idempotent. */
        private fun ensureHiddenApiBypass() {
            if (bypassDone) return
            bypassDone = true
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
            try {
                val classClass = Class::class.java
                val forName = classClass.getDeclaredMethod("forName", String::class.java)
                val getDeclaredMethod = classClass.getDeclaredMethod(
                    "getDeclaredMethod", String::class.java, arrayOf<Class<*>>()::class.java
                )
                val vmRuntimeClass = forName.invoke(null, "dalvik.system.VMRuntime") as Class<*>
                val getRuntime = getDeclaredMethod.invoke(
                    vmRuntimeClass, "getRuntime", arrayOfNulls<Class<*>>(0)
                ) as java.lang.reflect.Method
                val setExemptions = getDeclaredMethod.invoke(
                    vmRuntimeClass, "setHiddenApiExemptions", arrayOf<Class<*>>(arrayOf<String>()::class.java)
                ) as java.lang.reflect.Method
                // "L" is the prefix for every Ljava-style signature → exempts everything.
                setExemptions.invoke(getRuntime.invoke(null), arrayOf("L"))
            } catch (t: Throwable) {
                Timber.w(t, "ViwoodsFastInk hidden-API bypass failed")
            }
        }

        /** True only on Viwoods AiPaper hardware (ENoteSetting present). Cached. */
        val isAvailable: Boolean by lazy {
            ensureHiddenApiBypass()
            try {
                Class.forName(ENOTE_CLASS)
                Timber.i("ViwoodsFastInk available on ${Build.MANUFACTURER}/${Build.MODEL}")
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    /** Obtain the ENoteSetting singleton and bind the app context. */
    fun attach(context: Context) {
        if (!isAvailable || attached) return
        appContext = context.applicationContext
        try {
            enote = Class.forName(ENOTE_CLASS).getMethod("getInstance").invoke(null)
        } catch (t: Throwable) {
            Timber.w(t, "ViwoodsFastInk getInstance failed")
            return
        }
        reflect("setApplicationContext", arrayOf(Context::class.java), appContext)
        attached = true
    }

    /**
     * Activate fast ink. [w]x[h] are the full-screen dimensions.
     *
     * When [useInitWriting] is true (the targetSdk-30 viwoods build, which runs in the
     * untrusted_app_30 SELinux domain), `initWriting()` loads libpaintworker.so and the
     * T1000 renders pen strokes natively, full-screen — true instant hardware ink. This is
     * the confirmed working path; it is NEVER called on the standard build (targetSdk 36),
     * where it would crash in JNI_OnLoad.
     *
     * Otherwise we fall back to setting the FAST waveform so the app's own software stroke
     * posts refresh faster than the GL16 reading waveform. Idempotent.
     */
    fun enable(w: Int, h: Int, useInitWriting: Boolean, penMin: Int = 1, penMax: Int = 3) {
        if (enote == null) return
        if (useInitWriting && !initWritingDone) {
            initWritingDone = true
            // The native fast-ink layer only works if the system FocusMonitor service is
            // enabled (persist.sys.focusmonitor.config=1). It's factory-set on stock units
            // but wiped by a bootloader-unlock/factory-reset. Without it, initWriting()
            // returns OK but WritingSurface::init fails (lock error:-22) and nothing paints —
            // so only activate the hardware path when the prerequisite is actually present.
            // Otherwise fall through to the FAST-waveform + software-render path.
            if (getProp("persist.sys.focusmonitor.config") == "1") {
                val ok = reflect("initWriting", arrayOf()) != null
                reflect("setWritingEnabled", arrayOf(Boolean::class.javaPrimitiveType!!), true)
                hardwareInk = ok
                Timber.i("ViwoodsFastInk initWriting ${if (ok) "OK — hardware ink active" else "failed"}")
            } else {
                Timber.w("ViwoodsFastInk: focusmonitor.config not set — using software fallback")
            }
        }
        transact(TXN_SET_PICTURE_MODE, MODE_FAST)
            ?: reflect("setPictureMode", arrayOf(Int::class.javaPrimitiveType!!), MODE_FAST)
        if (!hardwareInk) {
            // Binder-only AutoDraw attempt (no-op on most firmware); software render is the
            // real fallback. Skipped when hardware ink is active to avoid double-drawing.
            transact(TXN_SET_AUTODRAW_ENABLE, 1)
            transact(TXN_SET_ALL_REGION_UNAUTODRAW, 0)
            transact(TXN_SET_AUTODRAW_TOOLTYPE, 2)
            transact(TXN_SET_AUTODRAW_PENWIDTH, penMin, penMax)
            transactRect(TXN_ADD_AUTODRAW_RECT, Rect(0, 0, w, h))
        }
    }

    /** Signal stroke start to the native layer (quality-redraw bookkeeping). Hardware path only. */
    fun onStrokeStart() {
        if (hardwareInk) reflect("onWritingStart", arrayOf())
    }

    /**
     * Re-assert the FAST waveform for the software path. The panel reverts to the GL16
     * reading waveform on full-page redraws (e.g. swiping between the day and notes/journal
     * pages), so FAST set once at surface creation isn't enough — call this on each pen-down
     * so every page writes quickly. No-op on the hardware path (the native layer owns refresh).
     */
    fun reassertFastMode() {
        if (enote == null || hardwareInk) return
        transact(TXN_SET_PICTURE_MODE, MODE_FAST)
            ?: reflect("setPictureMode", arrayOf(Int::class.javaPrimitiveType!!), MODE_FAST)
    }

    /** Signal stroke end — triggers the native quality redraw. Hardware path only. */
    fun onStrokeEnd() {
        if (hardwareInk) reflect("onWritingEnd", arrayOf())
    }

    /** Tear down fast ink and return the panel to the reading (GL16) waveform. Call from onPause. */
    fun disable() {
        if (enote == null) return
        if (hardwareInk) {
            reflect("setWritingEnabled", arrayOf(Boolean::class.javaPrimitiveType!!), false)
            reflect("exitWriting", arrayOf())
            hardwareInk = false
            initWritingDone = false
        } else {
            transact(TXN_SET_AUTODRAW_ENABLE, 0)
            transact(TXN_SET_ALL_REGION_UNAUTODRAW, 1)
            transact(TXN_STOP_HANDWRITE_INTERCEPT)
        }
        transact(TXN_SET_PICTURE_MODE, MODE_GL16)
            ?: reflect("setPictureMode", arrayOf(Int::class.javaPrimitiveType!!), MODE_GL16)
    }

    // === reflection on the ENoteSetting wrapper ===

    private fun reflect(name: String, types: Array<Class<*>>, vararg args: Any?): String? {
        val e = enote ?: return null
        return try {
            e.javaClass.getMethod(name, *types).apply { isAccessible = true }.invoke(e, *args)
            "ok"
        } catch (t: Throwable) {
            Timber.w(t, "ViwoodsFastInk.reflect($name)")
            null
        }
    }

    // === direct IBinder.transact (correct PID) ===

    private fun serviceBinder(): IBinder? = try {
        Class.forName("android.os.ServiceManager")
            .getMethod("getService", String::class.java).invoke(null, SERVICE_NAME) as? IBinder
    } catch (t: Throwable) {
        Timber.w(t, "ViwoodsFastInk.serviceBinder")
        null
    }

    /** transact with zero or more int args. Returns "ok"/error string, or null if unavailable. */
    private fun transact(code: Int, vararg ints: Int): String? {
        val binder = serviceBinder() ?: return null
        val data = Parcel.obtain(); val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(IFACE_TOKEN)
            ints.forEach { data.writeInt(it) }
            binder.transact(code, data, reply, 0)
            reply.readException()
            "ok"
        } catch (t: Throwable) {
            Timber.w(t, "ViwoodsFastInk.transact($code)")
            null
        } finally {
            data.recycle(); reply.recycle()
        }
    }

    private fun transactRect(code: Int, rect: Rect): String? {
        val binder = serviceBinder() ?: return null
        val data = Parcel.obtain(); val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(IFACE_TOKEN)
            data.writeInt(1)            // non-null marker (writeTypedObject)
            rect.writeToParcel(data, 0)
            binder.transact(code, data, reply, 0)
            reply.readException()
            "ok"
        } catch (t: Throwable) {
            Timber.w(t, "ViwoodsFastInk.transactRect($code)")
            null
        } finally {
            data.recycle(); reply.recycle()
        }
    }
}
