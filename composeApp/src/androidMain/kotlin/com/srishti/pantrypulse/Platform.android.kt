package com.srishti.pantrypulse

import android.os.Build
import com.srishti.pantrypulse.model.Platform

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()