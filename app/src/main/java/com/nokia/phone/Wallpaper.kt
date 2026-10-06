package com.nokia.phone

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File

/** Hình nền do người dùng chọn cho màn hình khóa (khung hiển thị phía trên). Lưu bản thu nhỏ trong bộ nhớ riêng của app. */
object Wallpaper {
    private fun file(ctx: Context) = File(ctx.filesDir, "lock_wallpaper.jpg")

    fun load(ctx: Context): Bitmap? = try {
        val f = file(ctx)
        if (f.exists()) BitmapFactory.decodeFile(f.path) else null
    } catch (_: Throwable) { null }

    /** Đọc ảnh từ uri (thu nhỏ, xoay đúng chiều), lưu lại và trả về bitmap; lỗi thì trả null. */
    fun save(ctx: Context, u: Uri, maxDim: Int = 1280): Bitmap? = try {
        val cr = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var s = 1
        while (bounds.outWidth / (s * 2) >= maxDim || bounds.outHeight / (s * 2) >= maxDim) s *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = s }
        val b = cr.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, opts) }
        val rot: Float = try {
            cr.openInputStream(u)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (_: Throwable) { 0f }
        val out = if (b != null && rot != 0f)
            Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(rot) }, true) else b
        if (out != null) file(ctx).outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        out
    } catch (_: Throwable) { null }
}
