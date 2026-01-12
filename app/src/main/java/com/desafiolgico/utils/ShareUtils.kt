package com.desafiolgico.utils

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.core.content.FileProvider
import com.desafiolgico.BuildConfig
import java.io.File
import java.io.FileOutputStream

object ShareUtils {

    fun shareViewAsImage(
        activity: Activity,
        viewToShare: View,
        chooserTitle: String = "Compartilhar"
    ) {
        val bitmap = captureViewSafe(viewToShare)
        val uri = saveToCacheAndGetUri(activity, bitmap)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)

            // ✅ ESSENCIAL para FileProvider
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            // ✅ alguns Androids só respeitam a permissão se tiver ClipData
            clipData = ClipData.newRawUri("placar", uri)
        }

        // ✅ (Opcional, mas resolve 100%): garante permissão para todos os apps que podem receber
        val resInfoList = activity.packageManager.queryIntentActivities(
            shareIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            activity.grantUriPermission(
                packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        val chooser = Intent.createChooser(shareIntent, chooserTitle)
        activity.startActivity(chooser)
    }

    private fun captureViewSafe(view: View): Bitmap {
        // Se ainda não foi medido/layout, mede na hora
        if (view.width == 0 || view.height == 0) {
            val wSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            val hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            view.measure(wSpec, hSpec)
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        }

        val b = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        view.draw(c)
        return b
    }

    private fun saveToCacheAndGetUri(activity: Activity, bitmap: Bitmap) =
        run {
            val dir = File(activity.cacheDir, "share")
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, "placar_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            FileProvider.getUriForFile(
                activity,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file
            )
        }
}
