package com.desafiolgico.utils

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.core.content.FileProvider
import com.desafiolgico.BuildConfig
import java.io.File
import java.io.FileOutputStream

object ShareUtils {

    private const val CACHE_DIR = "share"
    private const val MIME_PNG = "image/png"

    fun shareViewAsImage(
        activity: Activity,
        viewToShare: View,
        chooserTitle: String = "Compartilhar"
    ) {
        val bitmap = captureViewSafe(viewToShare)
        val uri = saveToCacheAndGetUri(activity, bitmap)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = MIME_PNG
            putExtra(Intent.EXTRA_STREAM, uri)

            // ✅ ESSENCIAL para FileProvider
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            // ✅ alguns Androids só respeitam a permissão se tiver ClipData
            clipData = ClipData.newRawUri("placar", uri)
        }

        // ✅ garante permissão para TODOS os apps que podem receber (100% compat)
        val pm = activity.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 33) PackageManager.ResolveInfoFlags.of(0)
        else 0

        val resInfoList = if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(shareIntent, flags as PackageManager.ResolveInfoFlags)
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(shareIntent, flags as Int)
        }

        resInfoList.forEach { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            activity.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        activity.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }

    private fun captureViewSafe(view: View): Bitmap {
        // Se ainda não foi medido/layout, mede e faz layout na hora
        if (view.width <= 0 || view.height <= 0) {
            val wSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            val hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            view.measure(wSpec, hSpec)
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        }

        val w = view.width.coerceAtLeast(1)
        val h = view.height.coerceAtLeast(1)

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        view.draw(canvas)
        return bmp
    }

    private fun saveToCacheAndGetUri(activity: Activity, bitmap: Bitmap): Uri {
        val dir = File(activity.cacheDir, CACHE_DIR)
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "placar_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return FileProvider.getUriForFile(
            activity,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            file
        )
    }
}
