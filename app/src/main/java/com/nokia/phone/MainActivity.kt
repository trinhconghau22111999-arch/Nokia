package com.nokia.phone

import android.Manifest
import android.app.ActivityOptions
import android.graphics.Rect
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val LCD = Color(0xFFC7F0D8)
val INK = Color(0xFF1B2B1B)
val KEY = Color(0xFFD5DAE3)
val KEYTXT = Color(0xFF1B2230)
val MONO = FontFamily.Monospace

val MENU = listOf(
    "Ứng dụng" to "apps", "Danh bạ" to "contacts", "Tin nhắn" to "messages",
    "Đồng hồ" to "clock", "Rắn săn mồi" to "snake", "Cài đặt" to "settings"
)
val LETTERS = mapOf("2" to "ABC", "3" to "DEF", "4" to "GHI", "5" to "JKL",
    "6" to "MNO", "7" to "PQRS", "8" to "TUV", "9" to "WXYZ", "*" to "+", "0" to "_", "#" to "⇧")

data class AppInfo(val label: String, val pkg: String)

/** Tăng mỗi khi người dùng bấm nút Home (launcher chạy lại) -> về màn hình chờ. */
val homeTick = mutableIntStateOf(0)

class Snake {
    val w = 10; val h = 14
    var body by mutableStateOf(listOf(5 to 7, 4 to 7, 3 to 7))
    var dir by mutableStateOf(1 to 0)
    var food by mutableStateOf(8 to 3)
    var dead by mutableStateOf(false)
    var score by mutableIntStateOf(0)
    fun reset() { body = listOf(5 to 7, 4 to 7, 3 to 7); dir = 1 to 0; dead = false; score = 0 }
    fun turn(d: Pair<Int, Int>) { if (d.first != -dir.first || d.second != -dir.second) dir = d }
    fun step() {
        if (dead) return
        val hd = body[0]
        val n = ((hd.first + dir.first + w) % w) to ((hd.second + dir.second + h) % h)
        if (n in body) { dead = true; return }
        if (n == food) {
            score++
            body = listOf(n) + body
            food = (0 until w).random() to (0 until h).random()
        } else body = listOf(n) + body.dropLast(1)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        hideBars()
        setContent { Phone() }
    }

    private fun hideBars() {
        val c = WindowInsetsControllerCompat(window, window.decorView)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        c.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideBars()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homeTick.intValue++
    }
}

fun timeNow(): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

@Composable
fun Phone() {
    val ctx = LocalContext.current
    var screen by remember { mutableStateOf("home") }
    var sel by remember { mutableIntStateOf(0) }
    var dial by remember { mutableStateOf("") }
    var buzz by remember { mutableStateOf(true) }
    var now by remember { mutableStateOf(timeNow()) }
    var apps by remember { mutableStateOf(listOf<AppInfo>()) }
    var lcdRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val snake = remember { Snake() }
    val focus = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) { focus.requestFocus(); while (true) { now = timeNow(); delay(1000) } }
    LaunchedEffect(screen) { if (screen == "snake") while (true) { delay(170); snake.step() } }
    val tick = homeTick.intValue
    LaunchedEffect(tick) { if (tick > 0) { screen = "home"; sel = 0 } }

    fun freeformOn(): Boolean =
        ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT) ||
            Settings.Global.getInt(ctx.contentResolver, "enable_freeform_support", 0) != 0

    /** Mở app trong cửa sổ có kích thước đúng bằng vùng nội dung của màn hình LCD nhỏ (cần bật cửa sổ tự do). */
    fun launch(i: Intent, inLcd: Boolean = true) {
        val r = lcdRect
        if (!inLcd || r == null) {
            try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
            return
        }
        if (!freeformOn()) {
            Toast.makeText(ctx, "Máy chưa bật cửa sổ tự do. Vào Cài đặt > Cửa sổ nhỏ (dev)", Toast.LENGTH_LONG).show()
        }
        val opts = ActivityOptions.makeBasic()
        opts.setLaunchBounds(Rect(r.left.roundToInt(), r.top.roundToInt(), r.right.roundToInt(), r.bottom.roundToInt()))
        // Ép chế độ cửa sổ tự do (API ẩn, có thể không dùng được trên một số máy -> bỏ qua)
        try {
            ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType).invoke(opts, 5)
        } catch (_: Throwable) {}
        // MULTIPLE_TASK: tạo task mới để khung được áp dụng ngay cả khi app đang chạy toàn màn hình
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        try {
            ctx.startActivity(i, opts.toBundle())
        } catch (_: Throwable) {
            try { ctx.startActivity(i) } catch (_: Exception) {}
        }
    }
    fun dialScreen(n: String) = launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(n))))
    fun placeCall(n: String) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(n)))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { dialScreen(n) }
    }
    var pendingCall by remember { mutableStateOf("") }
    val callPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        val n = pendingCall
        pendingCall = ""
        if (n.isNotEmpty()) { if (ok) placeCall(n) else dialScreen(n) }
    }
    fun loadApps() {
        val pm = ctx.packageManager
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        apps = pm.queryIntentActivities(q, 0)
            .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .filter { it.pkg != ctx.packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }
    fun open(s: String) { screen = s; sel = 0; if (s == "snake") snake.reset(); if (s == "apps") loadApps() }
    fun back() { when (screen) { "home" -> {}; "menu" -> open("home"); else -> open("menu") } }

    fun press(k: String) {
        if (buzz) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val size = when (screen) { "menu" -> MENU.size; "apps" -> maxOf(apps.size, 1); "settings" -> 3; else -> 1 }
        when (k) {
            "UP", "LEFT" -> if (screen == "snake") snake.turn(if (k == "UP") 0 to -1 else -1 to 0)
                else sel = (sel - 1 + size) % size
            "DOWN", "RIGHT" -> if (screen == "snake") snake.turn(if (k == "DOWN") 0 to 1 else 1 to 0)
                else sel = (sel + 1) % size
            "OK", "SOFTL" -> when (screen) {
                "home" -> open("menu")
                "menu" -> when (val id = MENU[sel].second) {
                    "contacts" -> launch(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI))
                    "messages" -> launch(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING))
                    else -> open(id)
                }
                "apps" -> apps.getOrNull(sel)?.let { a ->
                    ctx.packageManager.getLaunchIntentForPackage(a.pkg)?.let { launch(it) }
                }
                "settings" -> when (sel) {
                    0 -> buzz = !buzz
                    1 -> launch(Intent(Settings.ACTION_HOME_SETTINGS))
                    else -> launch(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS), false)
                }
                "snake" -> if (snake.dead) snake.reset()
            }
            "SOFTR" -> if (screen == "home") { if (dial.isNotEmpty()) dial = dial.dropLast(1) } else back()
            "END" -> if (screen == "home") dial = "" else open("home")
            "CALL" -> {
                if (screen == "home" && dial.isNotEmpty()) {
                    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE)
                        == PackageManager.PERMISSION_GRANTED) placeCall(dial)
                    else { pendingCall = dial; callPerm.launch(Manifest.permission.CALL_PHONE) }
                }
            }
            else -> if (screen == "home") dial += k
                else if (screen == "snake") when (k) {
                    "2" -> snake.turn(0 to -1); "8" -> snake.turn(0 to 1)
                    "4" -> snake.turn(-1 to 0); "6" -> snake.turn(1 to 0)
                }
        }
    }

    val tapItem: (Int) -> Unit = { i -> sel = i; press("OK") }
    val scroll: (Int) -> Unit = { d -> press(if (d > 0) "DOWN" else "UP") }

    BackHandler { back() }

    val left = when (screen) {
        "home" -> "Menu"; "menu", "apps" -> "Chọn"; "settings" -> "Đổi"
        "snake" -> if (snake.dead) "Chơi lại" else ""; else -> ""
    }
    val right = when (screen) { "home" -> if (dial.isEmpty()) "" else "Xóa"; else -> "Về" }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Tỉ lệ gốc của màn hình điện thoại thật -> làm chuẩn cho màn hình LCD nhỏ
        val ratio = maxWidth / maxHeight
        Column(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF4469BC), Color(0xFF223C78))))
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 10.dp)
                .focusRequester(focus).focusable()
                .onKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val c = e.utf16CodePoint.toChar()
                    when {
                        e.key == Key.DirectionUp -> press("UP")
                        e.key == Key.DirectionDown -> press("DOWN")
                        e.key == Key.DirectionLeft -> press("LEFT")
                        e.key == Key.DirectionRight -> press("RIGHT")
                        e.key == Key.DirectionCenter || e.key == Key.Enter -> press("OK")
                        e.key == Key.Backspace -> press("SOFTR")
                        c.isDigit() || c == '*' || c == '#' -> press(c.toString())
                        else -> return@onKeyEvent false
                    }
                    true
                }
        ) {
            // Loa + logo (mỏng gọn)
            Box(Modifier.fillMaxWidth(), Alignment.Center) {
                Box(Modifier.width(70.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF111B36)))
            }
            Text("NOKIA", Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 5.dp), textAlign = TextAlign.Center,
                color = Color(0xFFE6ECFA), fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 4.sp)

            // Màn hình LCD nhỏ (phần duy nhất thay đổi)
            Box(Modifier.fillMaxWidth().weight(0.72f), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize()
                .clip(RoundedCornerShape(14.dp)).background(Color(0xFF111B36)).padding(7.dp)) {
                Column(Modifier.fillMaxSize().background(LCD)) {
                    Row(Modifier.fillMaxWidth().background(INK).padding(horizontal = 6.dp, vertical = 1.dp)) {
                        Text("▂▄▆", color = LCD, fontFamily = MONO, fontSize = 11.sp)
                        Spacer(Modifier.weight(1f))
                        Text(now.take(5), color = LCD, fontFamily = MONO, fontSize = 11.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("▮▮▮", color = LCD, fontFamily = MONO, fontSize = 11.sp)
                    }
                    Box(Modifier.weight(1f).fillMaxWidth().onGloballyPositioned { lcdRect = it.boundsInWindow() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)) {
                        when (screen) {
                            "home" -> Column(Modifier.fillMaxSize().clickable { press("OK") }, Arrangement.SpaceEvenly, Alignment.CenterHorizontally) {
                                Text("NOKIA", color = INK, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = MONO)
                                Text(now.take(5), color = INK, fontSize = 40.sp, fontWeight = FontWeight.Bold, fontFamily = MONO)
                                Text(dial, color = INK, fontSize = 20.sp, fontFamily = MONO, maxLines = 1, overflow = TextOverflow.Clip)
                            }
                            "menu" -> Lines(MENU.map { it.first }, sel, tapItem, scroll)
                            "apps" -> Lines(apps.map { it.label }.ifEmpty { listOf("(trống)") }, sel, tapItem, scroll)
                            "clock" -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Text(now, color = INK, fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = MONO)
                            }
                            "settings" -> Lines(listOf("Rung phím: " + if (buzz) "Bật" else "Tắt", "Chọn launcher", "Cửa sổ nhỏ (dev)"), sel, tapItem, scroll)
                            "snake" -> Column(Modifier.fillMaxSize()) {
                                Text(if (snake.dead) "Thua! Điểm: ${snake.score}" else "Điểm: ${snake.score}",
                                    color = INK, fontFamily = MONO, fontSize = 12.sp)
                                Canvas(Modifier.weight(1f).fillMaxWidth().pointerInput(Unit) {
                                    detectTapGestures { o ->
                                        if (snake.dead) snake.reset() else {
                                            val dx = o.x - size.width / 2f
                                            val dy = o.y - size.height / 2f
                                            if (abs(dx) > abs(dy)) snake.turn(if (dx > 0) 1 to 0 else -1 to 0)
                                            else snake.turn(if (dy > 0) 0 to 1 else 0 to -1)
                                        }
                                    }
                                }) {
                                    val c = minOf(size.width / snake.w, size.height / snake.h)
                                    val ox = (size.width - c * snake.w) / 2f
                                    val oy = (size.height - c * snake.h) / 2f
                                    snake.body.forEach { (x, y) -> drawRect(INK, Offset(ox + x * c, oy + y * c), Size(c - 2, c - 2)) }
                                    drawRect(INK, Offset(ox + snake.food.first * c + c / 4, oy + snake.food.second * c + c / 4), Size(c / 2, c / 2))
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Box(Modifier.weight(1f).clickable { press("SOFTL") }.padding(horizontal = 6.dp, vertical = 5.dp)) {
                            Text(left, color = INK, fontWeight = FontWeight.Bold, fontFamily = MONO, fontSize = 13.sp)
                        }
                        Box(Modifier.weight(1f).clickable { press("SOFTR") }.padding(horizontal = 6.dp, vertical = 5.dp),
                            contentAlignment = Alignment.CenterEnd) {
                            Text(right, color = INK, fontWeight = FontWeight.Bold, fontFamily = MONO, fontSize = 13.sp)
                        }
                    }
                }
            }
            }

            Spacer(Modifier.height(6.dp))
            Keys(::press)
        }
    }
}

@Composable
fun Lines(items: List<String>, sel: Int, onTap: (Int) -> Unit, onScroll: (Int) -> Unit) {
    val visible = 9
    val start = (sel - visible / 2).coerceIn(0, maxOf(0, items.size - visible))
    Column(Modifier.fillMaxWidth().fillMaxHeight().pointerInput(Unit) {
        var acc = 0f
        detectVerticalDragGestures(onDragEnd = { acc = 0f }) { _, d ->
            acc += d
            if (acc < -45f) { onScroll(1); acc = 0f } else if (acc > 45f) { onScroll(-1); acc = 0f }
        }
    }) {
        items.drop(start).take(visible).forEachIndexed { n, t ->
            val i = start + n
            Text(t, Modifier.fillMaxWidth().clickable { onTap(i) }
                .background(if (i == sel) INK else LCD).padding(horizontal = 6.dp, vertical = 4.dp),
                color = if (i == sel) LCD else INK, fontFamily = MONO, fontWeight = FontWeight.Bold,
                fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

val GREEN = Color(0xFF2E9E4F)
val RED = Color(0xFFC0392B)
const val CALL_PATH = "M20.01 15.38c-1.23 0-2.42-.2-3.53-.56-.35-.12-.74-.03-1.01.24l-1.57 1.97c-2.83-1.35-5.48-3.9-6.89-6.83l1.95-1.66c.27-.28.35-.67.24-1.02-.37-1.11-.56-2.3-.56-3.53 0-.54-.45-.99-.99-.99H4.19C3.65 3 3 3.24 3 3.99 3 13.28 10.73 21 20.01 21c.71 0 .99-.63.99-1.18v-3.45c0-.54-.45-.99-.99-.99z"

/** Biểu tượng ống nghe điện thoại; hangUp = xoay 135° thành nút tắt máy. */
@Composable
fun PhoneIcon(color: Color, hangUp: Boolean, modifier: Modifier = Modifier) {
    val path = remember { PathParser().parsePathString(CALL_PATH).toPath() }
    Canvas(modifier) {
        val k = size.minDimension / 24f
        withTransform({
            if (hangUp) rotate(135f, Offset(size.width / 2f, size.height / 2f))
            scale(k, k, Offset.Zero)
        }) { drawPath(path, color) }
    }
}

@Composable
fun K(mod: Modifier, shape: Shape = RoundedCornerShape(16.dp), bg: Color = KEY,
      onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(mod.padding(3.dp).clip(shape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center) { content() }
}

@Composable
fun Glyph(t: String, size: Int = 18, fg: Color = KEYTXT) {
    Text(t, color = fg, fontWeight = FontWeight.Bold, fontSize = size.sp)
}

/** Phím số: chữ số lớn, các chữ cái đậm nằm ngay bên dưới. */
@Composable
fun NumKey(d: String, sub: String, mod: Modifier, onClick: () -> Unit) {
    K(mod, onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(d, color = KEYTXT, fontWeight = FontWeight.Black, fontSize = 26.sp)
            Text(if (sub.isEmpty()) " " else sub, color = KEYTXT, fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun ColumnScope.Keys(p: (String) -> Unit) {
    val dk = Color(0xFFB4BCCB)
    // Bố cục như Nokia thật: phím mềm (cao) ở trên, Gọi/Tắt (thấp) ở dưới, D-pad tròn ở giữa.
    BoxWithConstraints(Modifier.fillMaxWidth().weight(0.27f), contentAlignment = Alignment.Center) {
        val s = minOf(maxHeight, maxWidth * 0.48f)
        val c = s / 3
        Row(Modifier.fillMaxWidth().height(s)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                K(Modifier.fillMaxWidth().height(s * 0.336f), shape = RoundedCornerShape(16.dp), onClick = { p("SOFTL") }) { Glyph("●", 14) }
                K(Modifier.fillMaxWidth().height(s * 0.336f), shape = RoundedCornerShape(14.dp), onClick = { p("CALL") }) {
                    PhoneIcon(GREEN, false, Modifier.size(31.dp))
                }
            }
            Column(Modifier.width(s).fillMaxHeight()) {
                Row(Modifier.height(c)) {
                    Spacer(Modifier.size(c))
                    K(Modifier.size(c), shape = CircleShape, onClick = { p("UP") }) { Glyph("▲", 14) }
                    Spacer(Modifier.size(c))
                }
                Row(Modifier.height(c)) {
                    K(Modifier.size(c), shape = CircleShape, onClick = { p("LEFT") }) { Glyph("◀", 14) }
                    K(Modifier.size(c), shape = CircleShape, bg = dk, onClick = { p("OK") }) { Glyph("OK", 15) }
                    K(Modifier.size(c), shape = CircleShape, onClick = { p("RIGHT") }) { Glyph("▶", 14) }
                }
                Row(Modifier.height(c)) {
                    Spacer(Modifier.size(c))
                    K(Modifier.size(c), shape = CircleShape, onClick = { p("DOWN") }) { Glyph("▼", 14) }
                    Spacer(Modifier.size(c))
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                K(Modifier.fillMaxWidth().height(s * 0.336f), shape = RoundedCornerShape(16.dp), onClick = { p("SOFTR") }) { Glyph("●", 14) }
                K(Modifier.fillMaxWidth().height(s * 0.336f), shape = RoundedCornerShape(14.dp), onClick = { p("END") }) {
                    PhoneIcon(RED, true, Modifier.size(31.dp))
                }
            }
        }
    }
    // Bàn phím số
    Column(Modifier.fillMaxWidth().weight(0.40f)) {
        listOf("123", "456", "789", "*0#").forEach { row ->
            Row(Modifier.weight(1f).fillMaxWidth()) {
                row.forEach { ch ->
                    val s = ch.toString()
                    NumKey(s, LETTERS[s] ?: "", Modifier.weight(1f).fillMaxHeight()) { p(s) }
                }
            }
        }
    }
}
