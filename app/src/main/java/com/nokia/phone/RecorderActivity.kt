package com.nokia.phone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * App con "Ghi âm": ghi, tạm dừng/tiếp tục, nghe lại, xóa. Dùng chung thư mục bản ghi với
 * màn hình Ghi âm cũ và với tính năng ghi âm cuộc gọi (Rec/Play trong Media.kt).
 *
 * Phím: OK/Menu = ghi hoặc nghe, ▲▼ chọn, # xóa, Về/Tắt = thoát.
 * Khi đang ghi: OK/Lưu = lưu, ▲▼ hoặc phím phải = tạm dừng/tiếp tục, phím Tắt = lưu và về danh sách.
 */
class RecorderActivity : LcdActivity() {
    private var screen by mutableStateOf("list")          // list | rec | confirm
    private var sel by mutableIntStateOf(0)
    private var recs by mutableStateOf(listOf<RecItem>())
    private var playing by mutableStateOf<String?>(null)
    private var tick by mutableIntStateOf(0)

    private val micPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) beginRec() else toast("Cần quyền Micro để ghi âm")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLcd(left = { leftLabel() }, right = { rightLabel() }) { RecLcd() }
        loadRecs()
    }

    override fun onStop() {
        super.onStop()
        // Rời màn hình thì lưu bản đang ghi (Android chặn micro khi app ở nền) và dừng phát
        if (screen == "rec" && Rec.active && !CallHub.recording) { Rec.stop(); screen = "list"; loadRecs() }
        Play.stop(); playing = null
    }

    private fun loadRecs() {
        Thread {
            val l = Rec.list(this)
            runOnUiThread { recs = l }
        }.start()
    }

    private fun playingRec(): RecItem? = recs.firstOrNull { it.file.absolutePath == playing }

    private fun leftLabel(): String = when (screen) {
        "rec" -> "Lưu"
        "confirm" -> "Có"
        else -> if (sel == 0) "Ghi" else if (playing != null && playing == recs.getOrNull(sel - 1)?.file?.absolutePath) "Dừng" else "Nghe"
    }

    private fun rightLabel(): String = when (screen) {
        "rec" -> if (Rec.paused) "Tiếp tục" else "Tạm dừng"
        "confirm" -> "Không"
        else -> "Thoát"
    }

    // ------------------------------------------------------------ hành động

    private fun startRec() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
            beginRec()
        else micPerm.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun beginRec() {
        Play.stop(); playing = null
        if (Rec.active) { toast(if (CallHub.recording) "Đang ghi âm cuộc gọi" else "Đang ghi âm"); return }
        if (Rec.start(this)) screen = "rec" else toast("Không ghi âm được")
    }

    private fun stopRec() {
        val f = Rec.stop()
        screen = "list"
        sel = if (f != null) 1 else 0
        if (f == null) toast("Bản ghi quá ngắn, không lưu")
        loadRecs()
    }

    private fun togglePause() {
        if (Rec.paused) Rec.resume() else if (!Rec.pause()) toast("Máy không hỗ trợ tạm dừng")
    }

    private fun togglePlay(i: Int) {
        val r = recs.getOrNull(i - 1) ?: return
        if (playing == r.file.absolutePath) { Play.stop(); playing = null }
        else if (Play.play(r.file) { playing = null }) playing = r.file.absolutePath
        else toast("Không phát được")
    }

    private fun doDelete() {
        val r = recs.getOrNull(sel - 1)
        if (r != null) {
            if (playing == r.file.absolutePath) { Play.stop(); playing = null }
            r.file.delete()
            toast("Đã xóa")
            sel = (sel - 1).coerceAtLeast(0)
            loadRecs()
        }
        screen = "list"
    }

    override fun onKey(k: String) {
        val n = recs.size + 1
        when (screen) {
            "rec" -> when (k) {
                "OK", "SOFTL", "END" -> stopRec()
                "SOFTR", "UP", "DOWN" -> togglePause()
            }
            "confirm" -> when (k) {
                "OK", "SOFTL" -> doDelete()
                "SOFTR", "END" -> screen = "list"
            }
            else -> when (k) {
                "UP", "LEFT" -> sel = (sel - 1 + n) % n
                "DOWN", "RIGHT" -> sel = (sel + 1) % n
                "OK", "SOFTL" -> if (sel == 0) startRec() else togglePlay(sel)
                "#" -> if (sel > 0) screen = "confirm"
                "SOFTR", "END" -> finish()
            }
        }
    }

    // ------------------------------------------------------------ giao diện LCD

    @Composable
    private fun RecLcd() {
        val t = tick
        LaunchedEffect(screen, playing) { while (screen == "rec" || playing != null) { tick++; delay(200) } }
        when (screen) {
            "rec" -> Column(Modifier.fillMaxSize()) {
                Head(if (Rec.paused) "‖ TẠM DỪNG" else if ((t / 3) % 2 == 0) "● ĐANG GHI ÂM" else "○ ĐANG GHI ÂM")
                Column(Modifier.weight(1f).fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterHorizontally) {
                    Text(mmss(Rec.elapsed()), color = INK, fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 56.sp)
                    val lv = if (Rec.paused) 0f else (Rec.level() / 32767f).coerceIn(0f, 1f)
                    val b = (Math.sqrt(lv.toDouble()) * 16).toInt().coerceIn(0, 16)
                    Text("█".repeat(b) + "░".repeat(16 - b), color = INK, fontFamily = MONO, fontSize = 18.sp)
                    Text("OK: lưu    ▲▼: tạm dừng", color = INK, fontFamily = MONO, fontSize = 13.sp)
                }
            }
            "confirm" -> Column(Modifier.fillMaxSize().padding(8.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                val r = recs.getOrNull(sel - 1)
                Text("Xóa bản ghi này?", color = INK, fontFamily = MONO, fontWeight = FontWeight.Bold,
                    fontSize = 20.sp, textAlign = TextAlign.Center)
                if (r != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(r.file.lastModified())) +
                        "  " + mmss(r.dur), color = INK, fontFamily = MONO, fontSize = 14.sp)
                }
            }
            else -> Column(Modifier.fillMaxSize()) {
                Head("GHI ÂM  (${recs.size})")
                val pr = playingRec()
                Hint(if (pr != null) progress(pr, t) else "OK: ghi/nghe   #: xóa")
                Spacer(Modifier.height(4.dp))
                Box(Modifier.weight(1f)) {
                    Lines(recLines(recs, playing, t), sel,
                        { i -> sel = i; onKey("OK") }, { d -> onKey(if (d > 0) "DOWN" else "UP") })
                }
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun progress(r: RecItem, tick: Int): String {
        val pos = Play.pos()
        val b = if (r.dur > 0) (pos * 10f / r.dur).toInt().coerceIn(0, 10) else 0
        return "▶ " + mmss(pos) + " " + "█".repeat(b) + "░".repeat(10 - b) + " " + mmss(r.dur)
    }
}
