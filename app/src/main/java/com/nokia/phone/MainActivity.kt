package com.nokia.phone

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val w = 14; val h = 8
    var body by mutableStateOf(listOf(5 to 4, 4 to 4, 3 to 4))
    var dir by mutableStateOf(1 to 0)
    var food by mutableStateOf(10 to 3)
    var dead by mutableStateOf(false)
    var score by mutableIntStateOf(0)
    fun reset() { body = listOf(5 to 4, 4 to 4, 3 to 4); dir = 1 to 0; dead = false; score = 0 }
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
    val snake = remember { Snake() }
    val focus = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) { focus.requestFocus(); while (true) { now = timeNow(); delay(1000) } }
    LaunchedEffect(screen) { if (screen == "snake") while (true) { delay(170); snake.step() } }
    val tick = homeTick.intValue
    LaunchedEffect(tick) { if (tick > 0) { screen = "home"; sel = 0 } }

    fun launch(i: Intent) {
        try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
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
        val size = when (screen) { "menu" -> MENU.size; "apps" -> maxOf(apps.size, 1); "settings" -> 2; else -> 1 }
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
                "settings" -> if (sel == 0) buzz = !buzz else launch(Intent(Settings.ACTION_HOME_SETTINGS))
                "snake" -> if (snake.dead) snake.reset()
            }
            "SOFTR" -> if (screen == "home") { if (dial.isNotEmpty()) dial = dial.dropLast(1) } else back()
            "END" -> if (screen == "home") dial = "" else open("home")
            "CALL" -> if (screen == "home" && dial.isNotEmpty())
                launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(dial))))
            else -> if (screen == "home") dial += k
                else if (screen == "snake") when (k) {
                    "2" -> snake.turn(0 to -1); "8" -> snake.turn(0 to 1)
                    "4" -> snake.turn(-1 to 0); "6" -> snake.turn(1 to 0)
                }
        }
    }

    BackHandler { back() }

    val left = when (screen) {
        "home" -> "Menu"; "menu", "apps" -> "Chọn"; "settings" -> "Đổi"
        "snake" -> if (snake.dead) "Chơi lại" else ""; else -> ""
    }
    val right = when (screen) { "home" -> if (dial.isEmpty()) "" else "Xóa"; else -> "Về" }

    Column(Modifier.fillMaxSize().background(Color(0xFF0E1420)).displayCutoutPadding().padding(8.dp)) {
        Column(
            Modifier.fillMaxSize()
                .clip(RoundedCornerShape(44.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF4469BC), Color(0xFF223C78))))
                .border(3.dp, Color(0xFF16254A), RoundedCornerShape(44.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp)
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
            // Loa + logo
            Box(Modifier.fillMaxWidth().weight(0.05f), Alignment.Center) {
                Box(Modifier.width(80.dp).height(7.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF111B36)))
            }
            Box(Modifier.fillMaxWidth().weight(0.05f), Alignment.Center) {
                Text("NOKIA", color = Color(0xFFE6ECFA), fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 4.sp)
            }

            // Màn hình LCD nhỏ (phần duy nhất thay đổi)
            Box(Modifier.fillMaxWidth().weight(0.30f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF111B36)).padding(7.dp)) {
                Column(Modifier.fillMaxSize().background(LCD)) {
                    Row(Modifier.fillMaxWidth().background(INK).padding(horizontal = 6.dp, vertical = 1.dp)) {
                        Text("▂▄▆", color = LCD, fontFamily = MONO, fontSize = 11.sp)
                        Spacer(Modifier.weight(1f))
                        Text(now.take(5), color = LCD, fontFamily = MONO, fontSize = 11.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("▮▮▮", color = LCD, fontFamily = MONO, fontSize = 11.sp)
                    }
                    Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)) {
                        when (screen) {
                            "home" -> Column(Modifier.fillMaxSize(), Arrangement.SpaceEvenly, Alignment.CenterHorizontally) {
                                Text("NOKIA", color = INK, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = MONO)
                                Text(now.take(5), color = INK, fontSize = 40.sp, fontWeight = FontWeight.Bold, fontFamily = MONO)
                                Text(dial, color = INK, fontSize = 20.sp, fontFamily = MONO, maxLines = 1, overflow = TextOverflow.Clip)
                            }
                            "menu" -> Lines(MENU.map { it.first }, sel)
                            "apps" -> Lines(apps.map { it.label }.ifEmpty { listOf("(trống)") }, sel)
                            "clock" -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Text(now, color = INK, fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = MONO)
                            }
                            "settings" -> Lines(listOf("Rung phím: " + if (buzz) "Bật" else "Tắt", "Chọn launcher"), sel)
                            "snake" -> Column(Modifier.fillMaxSize()) {
                                Text(if (snake.dead) "Thua! Điểm: ${snake.score}" else "Điểm: ${snake.score}",
                                    color = INK, fontFamily = MONO, fontSize = 12.sp)
                                Canvas(Modifier.weight(1f).fillMaxWidth()) {
                                    val c = minOf(size.width / snake.w, size.height / snake.h)
                                    snake.body.forEach { (x, y) -> drawRect(INK, Offset(x * c, y * c), Size(c - 2, c - 2)) }
                                    drawRect(INK, Offset(snake.food.first * c + c / 4, snake.food.second * c + c / 4), Size(c / 2, c / 2))
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(left, color = INK, fontWeight = FontWeight.Bold, fontFamily = MONO, fontSize = 13.sp)
                        Spacer(Modifier.weight(1f))
                        Text(right, color = INK, fontWeight = FontWeight.Bold, fontFamily = MONO, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Keys(::press)
        }
    }
}

@Composable
fun Lines(items: List<String>, sel: Int) {
    val visible = 5
    val start = (sel - visible / 2).coerceIn(0, maxOf(0, items.size - visible))
    Column(Modifier.fillMaxWidth()) {
        items.drop(start).take(visible).forEachIndexed { n, t ->
            val i = start + n
            Text(t, Modifier.fillMaxWidth().background(if (i == sel) INK else LCD).padding(horizontal = 6.dp, vertical = 4.dp),
                color = if (i == sel) LCD else INK, fontFamily = MONO, fontWeight = FontWeight.Bold,
                fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun K(label: String, mod: Modifier, sub: String = "", bg: Color = KEY, fg: Color = KEYTXT, size: Int = 18, onClick: () -> Unit) {
    Box(mod.padding(3.dp).clip(RoundedCornerShape(16.dp)).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = fg, fontWeight = FontWeight.Bold, fontSize = size.sp)
            if (sub.isNotEmpty()) Text(" $sub", color = fg, fontSize = 9.sp)
        }
    }
}

@Composable
fun ColumnScope.Keys(p: (String) -> Unit) {
    val dk = Color(0xFFB4BCCB)
    // Cụm điều hướng đối xứng: [Phím mềm T / Gọi] [D-pad] [Phím mềm P / Tắt]
    Row(Modifier.fillMaxWidth().weight(0.23f)) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            K("●", Modifier.weight(1f).fillMaxWidth(), size = 14) { p("SOFTL") }
            K("Gọi", Modifier.weight(1f).fillMaxWidth(), bg = Color(0xFF2E9E4F), fg = Color.White, size = 16) { p("CALL") }
        }
        Column(Modifier.weight(1.5f).fillMaxHeight()) {
            K("▲", Modifier.weight(1f).fillMaxWidth(), size = 14) { p("UP") }
            Row(Modifier.weight(1f).fillMaxWidth()) {
                K("◀", Modifier.weight(1f).fillMaxHeight(), size = 14) { p("LEFT") }
                K("OK", Modifier.weight(1.25f).fillMaxHeight(), bg = dk, size = 15) { p("OK") }
                K("▶", Modifier.weight(1f).fillMaxHeight(), size = 14) { p("RIGHT") }
            }
            K("▼", Modifier.weight(1f).fillMaxWidth(), size = 14) { p("DOWN") }
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            K("●", Modifier.weight(1f).fillMaxWidth(), size = 14) { p("SOFTR") }
            K("Tắt", Modifier.weight(1f).fillMaxWidth(), bg = Color(0xFFC0392B), fg = Color.White, size = 16) { p("END") }
        }
    }
    // Bàn phím số
    Column(Modifier.fillMaxWidth().weight(0.38f)) {
        listOf("123", "456", "789", "*0#").forEach { row ->
            Row(Modifier.weight(1f).fillMaxWidth()) {
                row.forEach { ch ->
                    val s = ch.toString()
                    K(s, Modifier.weight(1f).fillMaxHeight(), sub = LETTERS[s] ?: "", size = 22) { p(s) }
                }
            }
        }
    }
}
