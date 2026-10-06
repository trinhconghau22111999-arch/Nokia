package com.nokia.phone

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.SystemClock
import androidx.compose.ui.graphics.Path
import android.app.ActivityOptions
import android.graphics.Rect
import android.content.Intent
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

import android.graphics.Bitmap
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val LCD = Color(0xFFC7F0D8)
val INK = Color(0xFF1B2B1B)
val KEY = Color(0xFFD5DAE3)
val KEYTXT = Color(0xFF1B2230)
val MONO = FontFamily.Monospace

val MENU = listOf(
    "Ứng dụng" to "apps", "Danh bạ" to "contacts", "Tin nhắn" to "messages",
    "Zalo" to "zalo", "YouTube" to "youtube",
    "Đồng hồ" to "clock", "Lịch" to "calendar", "Ghi âm" to "recorder",
    "Máy tính" to "calc", "Máy ảnh" to "camera", "Thư viện" to "gallery",
    "Rắn săn mồi" to "snake", "Cài đặt" to "settings"
)
val LETTERS = mapOf("2" to "ABC", "3" to "DEF", "4" to "GHI", "5" to "JKL",
    "6" to "MNO", "7" to "PQRS", "8" to "TUV", "9" to "WXYZ", "*" to "+", "0" to "_", "#" to "⇧")

data class AppInfo(val label: String, val pkg: String)

/** Tăng mỗi khi người dùng bấm nút Home (launcher chạy lại) -> về màn hình chờ. */
val homeTick = mutableIntStateOf(0)

/** true khi MainActivity (màn hình LCD) đang nằm trong chế độ chia đôi màn hình. */
val inSplit = mutableStateOf(false)

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
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        hideBars()
        inSplit.value = isInMultiWindowMode
        setContent { Phone() }
    }

    private fun hideBars() {
        val c = WindowInsetsControllerCompat(window, window.decorView)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        c.hide(WindowInsetsCompat.Type.navigationBars())
        c.show(WindowInsetsCompat.Type.statusBars())
        c.isAppearanceLightStatusBars = true // nền LCD sáng -> icon tối
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideBars()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("menu", false)) NokiaState.openMenu = true
        homeTick.intValue++
    }

    override fun onResume() {
        super.onResume()
        NokiaState.lcdResumed = true
        Sound.applyPending(this)
        NokiaAccessibilityService.instance?.hideCursor()
        if (inSplit.value) ensureKeypad() else autoSplit()
    }

    override fun onPause() {
        NokiaState.lcdResumed = false
        super.onPause()
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        inSplit.value = isInMultiWindowMode
        if (isInMultiWindowMode) ensureKeypad() else {
            NokiaState.keypad?.finish()
            NokiaAccessibilityService.instance?.hideCursor()
        }
    }

    /** Vào app lần đầu: nếu đã bật dịch vụ Trợ năng thì tự chia đôi màn hình. */
    private fun autoSplit() {
        val svc = NokiaAccessibilityService.instance ?: return
        if (NokiaState.splitTried) return
        NokiaState.splitTried = true
        Handler(Looper.getMainLooper()).postDelayed({ svc.toggleSplit() }, 500)
    }

    /** Đã chia đôi mà chưa có bàn phím -> mở bàn phím Nokia ở nửa còn lại (nửa dưới). */
    private fun ensureKeypad() {
        if (NokiaState.keypadAlive) return
        Handler(Looper.getMainLooper()).postDelayed({
            if (inSplit.value && !NokiaState.keypadAlive) {
                try {
                    startActivity(Intent(this, KeypadActivity::class.java).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
                } catch (_: Exception) {}
            }
        }, 700)
    }
}

fun dateNow(): String = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.getDefault()).format(Date())

fun timeNow(): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

/** Lịch tháng (tuần bắt đầu từ thứ 2), mỗi ô có ngày dương và ngày âm bên dưới. offset = số tháng lệch so với tháng hiện tại. */
@Composable
fun MonthCalendar(offset: Int) {
    val first = remember(offset) {
        Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, offset) }
    }
    val today = Calendar.getInstance()
    val year = first.get(Calendar.YEAR)
    val month = first.get(Calendar.MONTH) + 1
    val sameMonth = year == today.get(Calendar.YEAR) && month == today.get(Calendar.MONTH) + 1
    val days = first.getActualMaximum(Calendar.DAY_OF_MONTH)
    val lead = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val rows = (lead + days + 6) / 7
    val lunar = remember(offset) { (1..days).map { Lunar.fromSolar(it, month, year) } }
    val footer = if (sameMonth) lunar[today.get(Calendar.DAY_OF_MONTH) - 1].let {
        "Hôm nay âm lịch: ${it.day}/${it.month}${if (it.leap) " (nhuận)" else ""} năm ${Lunar.canChi(it.year)}"
    } else "Năm ${Lunar.canChi(lunar[14].year)}"
    Column(Modifier.fillMaxSize()) {
        Text("◀  Tháng $month/$year  ▶",
            Modifier.fillMaxWidth().padding(vertical = 2.dp), textAlign = TextAlign.Center,
            color = INK, fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Row(Modifier.fillMaxWidth()) {
            listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = INK,
                    fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth().weight(1f)) {
                for (c in 0 until 7) {
                    val d = r * 7 + c - lead + 1
                    val isToday = sameMonth && d == today.get(Calendar.DAY_OF_MONTH)
                    val fg = if (isToday) LCD else INK
                    Box(Modifier.weight(1f).fillMaxHeight().padding(1.dp)
                        .background(if (isToday) INK else Color.Transparent),
                        contentAlignment = Alignment.Center) {
                        if (d in 1..days) {
                            val l = lunar[d - 1]
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(d.toString(), color = fg, fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text(if (l.day == 1) "1/${l.month}" else l.day.toString(), color = fg,
                                    fontFamily = MONO, fontSize = 10.sp, maxLines = 1,
                                    fontWeight = if (l.day == 1) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }
        Text(footer, Modifier.fillMaxWidth().padding(vertical = 3.dp), textAlign = TextAlign.Center,
            color = INK, fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
fun FieldBox(text: String, selected: Boolean, size: Int, onClick: () -> Unit) {
    Text(text, Modifier.clickable(onClick = onClick)
        .background(if (selected) INK else LCD).padding(horizontal = 10.dp, vertical = 4.dp),
        color = if (selected) LCD else INK, fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = size.sp)
}

val VOL = listOf(
    "Nhạc chuông" to AudioManager.STREAM_RING,
    "Báo thức" to AudioManager.STREAM_ALARM,
    "Phương tiện" to AudioManager.STREAM_MUSIC
)

fun volLine(ctx: Context, name: String, stream: Int): String {
    val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val max = maxOf(am.getStreamMaxVolume(stream), 1)
    val cur = am.getStreamVolume(stream)
    val f = Math.round(cur * 10f / max)
    return name.padEnd(12) + "█".repeat(f) + "░".repeat(10 - f)
}

fun isBtOn(ctx: Context): Boolean = try {
    ((ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter)?.isEnabled == true
} catch (_: Exception) { false }

/** Màn hình khóa mặc định: hình nền che toàn bộ khung hiển thị, bấm Menu rồi * để mở khóa. */
@Composable
fun LockScreen(time: String, date: String, armed: Boolean) {
    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { } }) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF8FD3F4), Color(0xFFDDF4E4))))
            drawCircle(Color(0xFFFFE9A8), radius = w * 0.13f, center = Offset(w * 0.8f, h * 0.30f))
            val far = Path().apply {
                moveTo(0f, h * 0.74f)
                quadraticBezierTo(w * 0.25f, h * 0.58f, w * 0.55f, h * 0.74f)
                quadraticBezierTo(w * 0.8f, h * 0.86f, w, h * 0.70f)
                lineTo(w, h); lineTo(0f, h); close()
            }
            drawPath(far, Color(0xFF7CCB93))
            val near = Path().apply {
                moveTo(0f, h * 0.86f)
                quadraticBezierTo(w * 0.35f, h * 0.74f, w * 0.7f, h * 0.88f)
                quadraticBezierTo(w * 0.88f, h * 0.94f, w, h * 0.84f)
                lineTo(w, h); lineTo(0f, h); close()
            }
            drawPath(near, Color(0xFF3F9E63))
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(time, color = INK, fontSize = 80.sp, fontWeight = FontWeight.Bold, fontFamily = MONO)
            Text(date, color = INK, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = MONO, maxLines = 1)
        }
        Text(if (armed) "Bấm  *  để mở khóa" else "Bấm  Menu  rồi bấm  *",
            Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp),
            color = INK, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = MONO)
    }
}

@Composable
fun Phone() {
    val ctx = LocalContext.current
    var screen by remember { mutableStateOf("home") }
    var sel by remember { mutableIntStateOf(0) }
    var dial by remember { mutableStateOf("") }
    var buzz by remember { mutableStateOf(true) }
    var now by remember { mutableStateOf(timeNow()) }
    var apps by remember { mutableStateOf(listOf<AppInfo>()) }
    var monthOffset by remember { mutableIntStateOf(0) }
    var alarms by remember { mutableStateOf(AlarmStore.load(ctx)) }
    var locked by remember { mutableStateOf(true) }
    var lockAt by remember { mutableLongStateOf(0L) }
    var btOn by remember { mutableStateOf(false) }
    var volTick by remember { mutableIntStateOf(0) }
    var ringName by remember { mutableStateOf("") }
    var alarmName by remember { mutableStateOf("") }
    var editIdx by remember { mutableIntStateOf(0) }
    var editField by remember { mutableIntStateOf(0) }
    var eh by remember { mutableIntStateOf(7) }
    var em by remember { mutableIntStateOf(0) }
    var eon by remember { mutableStateOf(true) }
    var lcdRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val scope = rememberCoroutineScope()
    var contacts by remember { mutableStateOf(listOf<Contact>()) }
    var smsAll by remember { mutableStateOf(listOf<Sms>()) }
    var threads by remember { mutableStateOf(listOf<SmsThread>()) }
    var threadMsgs by remember { mutableStateOf(listOf<Sms>()) }
    var curKey by remember { mutableStateOf("") }
    var curAddr by remember { mutableStateOf("") }
    var curName by remember { mutableStateOf("") }
    var mv by remember { mutableIntStateOf(0) }
    var cStage by remember { mutableIntStateOf(0) }
    var cTo by remember { mutableStateOf("") }
    var cName by remember { mutableStateOf("") }
    var cFrom by remember { mutableStateOf("new") }
    var cPick by remember { mutableIntStateOf(-1) }
    val entry = remember { TextEntry() }
    val calc = remember { CalcState() }
    val cam = remember { CamHolder() }
    var camFront by remember { mutableStateOf(false) }
    var camOk by remember { mutableStateOf(false) }
    var gIds by remember { mutableStateOf(listOf<Long>()) }
    var gIdx by remember { mutableIntStateOf(0) }
    var gBmp by remember { mutableStateOf<Bitmap?>(null) }
    var gOk by remember { mutableStateOf(false) }
    var gLoaded by remember { mutableStateOf(false) }
    var recs by remember { mutableStateOf(listOf<RecItem>()) }
    var playing by remember { mutableStateOf<String?>(null) }
    var recTick by remember { mutableIntStateOf(0) }
    var recording by remember { mutableStateOf(false) }
    var permThen by remember { mutableStateOf<(() -> Unit)?>(null) }
    var permMust by remember { mutableStateOf(listOf<String>()) }
    val cLines = remember(contacts) {
        val cnt = contacts.groupingBy { it.name }.eachCount()
        contacts.map { if ((cnt[it.name] ?: 0) > 1) it.name + "  " + it.number else it.name }
    }
    val snake = remember { Snake() }
    val focus = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) { focus.requestFocus(); while (true) { now = timeNow(); delay(1000) } }
    LaunchedEffect(screen) { if (screen == "snake") while (true) { delay(170); snake.step() } }
    LaunchedEffect(lockAt) { if (lockAt != 0L) { delay(4000); lockAt = 0L } }
    LaunchedEffect(screen) { if (screen == "settings") while (true) { btOn = isBtOn(ctx); delay(600) } }
    LaunchedEffect(gIdx, gIds, screen) {
        if (screen == "gallery" && gIds.isNotEmpty()) {
            gBmp = null
            gBmp = withContext(Dispatchers.IO) { Gallery.bitmap(ctx, gIds[gIdx.coerceIn(0, gIds.lastIndex)]) }
        }
    }
    LaunchedEffect(playing, recording, screen) {
        if (screen == "recorder" || screen == "recording") while (playing != null || recording) { recTick++; delay(250) }
    }
    DisposableEffect(screen) {
        val s = screen
        onDispose {
            if (s == "recording" && Rec.active) { Rec.stop(); recording = false }
            if (s == "recorder") { Play.stop(); playing = null }
        }
    }
    DisposableEffect(Unit) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { locked = true; lockAt = 0L }
        }
        ctx.registerReceiver(r, IntentFilter(Intent.ACTION_SCREEN_OFF))
        onDispose { try { ctx.unregisterReceiver(r) } catch (_: Exception) {} }
    }
    val tick = homeTick.intValue
    LaunchedEffect(tick) { if (tick > 0) { screen = if (NokiaState.openMenu) "menu" else "home"; NokiaState.openMenu = false; sel = 0 } }

    fun freeformOn(): Boolean =
        ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT) ||
            Settings.Global.getInt(ctx.contentResolver, "enable_freeform_support", 0) != 0

    /** Mở app trong cửa sổ có kích thước đúng bằng vùng nội dung của màn hình LCD nhỏ (cần bật cửa sổ tự do). */
    fun launch(i: Intent, inLcd: Boolean = true) {
        val r = lcdRect
        if (!inLcd || r == null || inSplit.value) {
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
    fun showDiag() {
        val r = lcdRect
        val msg = "${Build.MANUFACTURER} ${Build.MODEL} | Android ${Build.VERSION.SDK_INT}\n" +
            "cua so tu do: " + freeformOn() + "\n" +
            "khung LCD: " + (r?.let { "${it.left.roundToInt()},${it.top.roundToInt()} - ${it.right.roundToInt()},${it.bottom.roundToInt()}" } ?: "null")
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
    }
    fun toggleSplit() {
        val svc = NokiaAccessibilityService.instance
        if (svc == null) Toast.makeText(ctx, "Hãy bật dịch vụ Trợ năng Nokia Phone trước", Toast.LENGTH_LONG).show()
        else { NokiaState.splitTried = true; svc.toggleSplit() }
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
    fun refreshSoundNames() { ringName = Sound.ringtoneTitle(ctx); alarmName = Sound.alarmTitle(ctx) }

    val ringPick = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            val uri = Sound.picked(r.data)
            if (!Sound.setRingtone(ctx, uri)) {
                Toast.makeText(ctx, "Hãy cấp quyền \"Sửa đổi cài đặt hệ thống\" cho Nokia Phone", Toast.LENGTH_LONG).show()
                try {
                    ctx.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + ctx.packageName))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Exception) {}
            }
            refreshSoundNames()
        }
    }
    val alarmPick = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            val uri = Sound.picked(r.data)
            if (uri == Settings.System.DEFAULT_ALARM_ALERT_URI) AlarmStore.resetAlarmSound(ctx) else AlarmStore.setAlarmSound(ctx, uri)
            refreshSoundNames()
        }
    }
    val btPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        Toast.makeText(ctx, if (ok) "Đã cấp quyền, bấm lại để đổi Bluetooth" else "Cần quyền Bluetooth để bật/tắt", Toast.LENGTH_SHORT).show()
    }
    val btEnable = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { btOn = isBtOn(ctx) }

    @Suppress("DEPRECATION")
    fun toggleBt() {
        val ad: BluetoothAdapter? = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (ad == null) { Toast.makeText(ctx, "Máy không có Bluetooth", Toast.LENGTH_SHORT).show(); return }
        if (Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            btPerm.launch(Manifest.permission.BLUETOOTH_CONNECT); return
        }
        try {
            if (!ad.isEnabled) {
                if (!(Build.VERSION.SDK_INT < 33 && ad.enable())) btEnable.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } else if (!(Build.VERSION.SDK_INT < 33 && ad.disable())) {
                Toast.makeText(ctx, "Android 13+ không cho app tự tắt Bluetooth, hãy gạt tắt trong màn hình này", Toast.LENGTH_LONG).show()
                try { ctx.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
            }
        } catch (_: SecurityException) {}
        btOn = isBtOn(ctx)
    }

    fun adjustVol(d: Int, wrap: Boolean = false) {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val st = VOL[sel.coerceIn(0, VOL.lastIndex)].second
        val max = am.getStreamMaxVolume(st)
        val cur = am.getStreamVolume(st)
        val n = if (wrap && cur >= max) 0 else (cur + d).coerceIn(0, max)
        try { am.setStreamVolume(st, n, AudioManager.FLAG_PLAY_SOUND) } catch (_: SecurityException) {
            Toast.makeText(ctx, "Hãy tắt chế độ Không làm phiền để chỉnh âm lượng", Toast.LENGTH_SHORT).show()
        }
        volTick++
    }

    /** Khôi phục cài đặt gốc = xóa toàn bộ dữ liệu của riêng app này (báo thức, nhạc báo thức, quyền đã cấp). */
    fun doReset() {
        alarms = emptyList()
        AlarmStore.schedule(ctx, emptyList())
        val ok = try {
            (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
        } catch (_: Exception) { false }
        if (!ok) {
            ctx.getSharedPreferences("alarms", Context.MODE_PRIVATE).edit().clear().apply()
            ctx.getSharedPreferences("sound", Context.MODE_PRIVATE).edit().clear().apply()
            ctx.getSharedPreferences("sms_sent", Context.MODE_PRIVATE).edit().clear().apply()
            locked = true; lockAt = 0L; screen = "home"; sel = 0; dial = ""
        }
    }

    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun startEdit(i: Int) {
        editIdx = i
        val a = alarms.getOrNull(i) ?: Alarm(7, 0, true)
        eh = a.h; em = a.m; eon = a.on; editField = 0
        screen = "alarmEdit"
    }
    fun commitAlarms(list: List<Alarm>) {
        alarms = list
        AlarmStore.save(ctx, list)
        AlarmStore.schedule(ctx, list)
        screen = "clock"
        sel = editIdx.coerceIn(0, list.size)
    }
    fun saveAlarm() {
        val l = alarms.toMutableList()
        val a = Alarm(eh, em, eon)
        if (editIdx < l.size) l[editIdx] = a else l.add(a)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        commitAlarms(l)
    }
    fun deleteAlarm() {
        val l = alarms.toMutableList()
        if (editIdx in l.indices) l.removeAt(editIdx)
        commitAlarms(l)
    }
    fun editMove(k: String) {
        val fields = if (editIdx < alarms.size) 4 else 3
        when (k) {
            "LEFT" -> editField = (editField - 1 + fields) % fields
            "RIGHT" -> editField = (editField + 1) % fields
            else -> {
                val d = if (k == "UP") 1 else -1
                when (editField) {
                    0 -> eh = (eh + d + 24) % 24
                    1 -> em = (em + d + 60) % 60
                    2 -> eon = !eon
                }
            }
        }
    }
    fun granted(p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    val multiPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        val t = permThen
        permThen = null
        if (t != null) {
            if (permMust.all { granted(it) }) t()
            else Toast.makeText(ctx, "Cần cấp quyền để dùng chức năng này. Nếu máy không hiện hộp thoại: " +
                "Cài đặt máy > Ứng dụng > Nokia Phone > Quyền (Android 13+: bấm ⋮ > Cho phép cài đặt bị hạn chế)",
                Toast.LENGTH_LONG).show()
        }
    }
    /** Xin quyền (nếu thiếu) rồi chạy then; must = các quyền bắt buộc, phần còn lại là tùy chọn. */
    fun need(perms: List<String>, must: List<String> = perms, then: () -> Unit) {
        val miss = perms.filter { !granted(it) }
        if (miss.isEmpty()) then() else { permThen = then; permMust = must; multiPerm.launch(miss.toTypedArray()) }
    }
    val imgPerms = if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.READ_MEDIA_IMAGES)
        else listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    val camPerms = if (Build.VERSION.SDK_INT < 29) listOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        else listOf(Manifest.permission.CAMERA)

    fun loadContacts() { scope.launch { contacts = withContext(Dispatchers.IO) { Contacts.load(ctx) } } }
    fun loadRecs() { scope.launch { recs = withContext(Dispatchers.IO) { Rec.list(ctx) } } }
    fun loadSms(after: (() -> Unit)? = null) {
        scope.launch {
            val hasC = granted(Manifest.permission.READ_CONTACTS)
            val (all, cs) = withContext(Dispatchers.IO) {
                val c = if (hasC) Contacts.load(ctx) else emptyList()
                SmsRepo.loadAll(ctx) to c
            }
            smsAll = all
            if (cs.isNotEmpty()) contacts = cs
            threads = buildThreads(all, if (cs.isNotEmpty()) cs else contacts)
            if (curKey.isNotEmpty()) threadMsgs = all.filter { addrKey(it.addr) == curKey }
            after?.invoke()
        }
    }

    fun launchPkg(pkg: String, label: String) {
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg)
        if (i != null) launch(i) else {
            Toast.makeText(ctx, "Chưa cài $label, đang mở CH Play", Toast.LENGTH_SHORT).show()
            try {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {}
        }
    }

    /** Gọi ngay, không xác nhận, không qua màn hình quay số của app. */
    fun callNow(n: String) {
        if (n.isEmpty()) return
        if (granted(Manifest.permission.CALL_PHONE)) placeCall(n)
        else { pendingCall = n; callPerm.launch(Manifest.permission.CALL_PHONE) }
    }

    fun openThread(key: String) {
        curKey = key
        val t = threads.firstOrNull { it.key == key }
        curAddr = t?.addr ?: ""
        curName = t?.name ?: curAddr
        threadMsgs = smsAll.filter { addrKey(it.addr) == key }
        screen = "thread"; sel = 0
    }
    fun startCompose(to: String, name: String, from: String) {
        cTo = to; cName = name; cFrom = from; cPick = -1
        entry.reset()
        cStage = if (to.isEmpty()) 0 else 1
        screen = "compose"; sel = 0
        if (to.isEmpty() && contacts.isEmpty() && granted(Manifest.permission.READ_CONTACTS)) loadContacts()
    }
    fun doSend() {
        val body = entry.text.trim()
        if (body.isEmpty() || cTo.isEmpty()) {
            Toast.makeText(ctx, "Chưa có nội dung", Toast.LENGTH_SHORT).show(); return
        }
        need(listOf(Manifest.permission.SEND_SMS)) {
            if (SmsRepo.send(ctx, cTo, body)) {
                Toast.makeText(ctx, "Đã gửi", Toast.LENGTH_SHORT).show()
                val key = addrKey(cTo)
                loadSms { openThread(key) }
            } else Toast.makeText(ctx, "Gửi không được", Toast.LENGTH_SHORT).show()
        }
    }
    fun pickContact(d: Int) {
        if (contacts.isEmpty()) return
        cPick = if (cPick < 0) (if (d > 0) 0 else contacts.size - 1) else (cPick + d + contacts.size) % contacts.size
        cTo = contacts[cPick].number; cName = contacts[cPick].name
    }
    fun jumpTo(k: String) {
        val g = LETTERS[k]?.lowercase() ?: return
        val idx = contacts.indices.filter { i -> contacts[i].key.firstOrNull()?.let { ch -> ch in g } == true }
        if (idx.isNotEmpty()) sel = idx.firstOrNull { it > sel } ?: idx.first()
    }

    fun startRecording() {
        need(listOf(Manifest.permission.RECORD_AUDIO)) {
            Play.stop(); playing = null
            if (Rec.start(ctx)) { recording = true; screen = "recording"; sel = 0 }
            else Toast.makeText(ctx, "Không ghi âm được", Toast.LENGTH_SHORT).show()
        }
    }
    fun stopRecording() {
        val f = Rec.stop()
        recording = false
        loadRecs()
        screen = "recorder"
        sel = if (f != null) 1 else 0
    }
    fun togglePlay(i: Int) {
        val r = recs.getOrNull(i - 1) ?: return
        if (playing == r.file.absolutePath) { Play.stop(); playing = null }
        else if (Play.play(r.file) { playing = null }) playing = r.file.absolutePath
        else Toast.makeText(ctx, "Không phát được", Toast.LENGTH_SHORT).show()
    }
    fun deleteRec(i: Int) {
        val r = recs.getOrNull(i - 1) ?: return
        if (playing == r.file.absolutePath) { Play.stop(); playing = null }
        r.file.delete()
        loadRecs()
        sel = (sel - 1).coerceAtLeast(0)
        Toast.makeText(ctx, "Đã xóa", Toast.LENGTH_SHORT).show()
    }

    fun open(s: String) {
        screen = s; sel = 0; monthOffset = 0
        when (s) {
            "snake" -> snake.reset()
            "apps" -> loadApps()
            "sound" -> refreshSoundNames()
            "calc" -> calc.clear()
            "recorder" -> loadRecs()
            "contacts" -> need(listOf(Manifest.permission.READ_CONTACTS)) { loadContacts() }
            "messages" -> {
                curKey = ""
                need(listOf(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.READ_CONTACTS),
                    listOf(Manifest.permission.READ_SMS)) { loadSms() }
            }
            "camera" -> { camOk = false; camFront = false; need(camPerms) { camOk = true } }
            "gallery" -> {
                gOk = false; gLoaded = false; gIds = emptyList(); gIdx = 0; gBmp = null
                need(imgPerms) {
                    gOk = true
                    scope.launch { gIds = withContext(Dispatchers.IO) { Gallery.ids(ctx) }; gLoaded = true }
                }
            }
        }
    }
    fun back() {
        when (screen) {
            "home" -> {}
            "menu" -> open("home")
            "alarmEdit" -> screen = "clock"
            "sound", "reset" -> open("settings")
            "volume" -> open("sound")
            "thread" -> open("messages")
            "msgview" -> screen = "thread"
            "compose" -> if (cStage == 1 && cFrom == "new") cStage = 0
                else if (cFrom == "thread") screen = "thread" else open("messages")
            "recording" -> stopRecording()
            else -> open("menu")
        }
    }

    fun press(k: String) {
        if (buzz) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (locked) {   // Menu rồi * để mở khóa
            val t = SystemClock.uptimeMillis()
            if (k == "SOFTL") lockAt = t
            else if (k == "*" && lockAt != 0L && t - lockAt < 4000) { locked = false; lockAt = 0L }
            else lockAt = 0L
            return
        }
        val size = when (screen) { "menu" -> MENU.size; "apps" -> maxOf(apps.size, 1); "settings" -> 4; "sound", "volume" -> 3; "clock" -> alarms.size + 1
            "contacts" -> maxOf(contacts.size, 1); "messages" -> threads.size + 1
            "thread" -> threadMsgs.size + 1; "msgview" -> maxOf(threadMsgs.size, 1); "recorder" -> recs.size + 1
            else -> 1 }
        when (k) {
            "UP", "LEFT" -> when (screen) {
                "snake" -> snake.turn(if (k == "UP") 0 to -1 else -1 to 0)
                "calendar" -> monthOffset--
                "volume" -> if (k == "LEFT") adjustVol(-1) else sel = (sel - 1 + size) % size
                "alarmEdit" -> editMove(k)
                "calc" -> calc.op(if (k == "UP") '+' else '×')
                "gallery" -> if (gIds.isNotEmpty()) gIdx = (gIdx - 1 + gIds.size) % gIds.size
                "camera" -> if (k == "LEFT") camFront = !camFront
                "msgview" -> mv = (mv - 1 + size) % size
                "compose" -> if (cStage == 0) pickContact(-1)
                else -> sel = (sel - 1 + size) % size
            }
            "DOWN", "RIGHT" -> when (screen) {
                "snake" -> snake.turn(if (k == "DOWN") 0 to 1 else 1 to 0)
                "calendar" -> monthOffset++
                "volume" -> if (k == "RIGHT") adjustVol(1) else sel = (sel + 1) % size
                "alarmEdit" -> editMove(k)
                "calc" -> calc.op(if (k == "DOWN") '−' else '÷')
                "gallery" -> if (gIds.isNotEmpty()) gIdx = (gIdx + 1) % gIds.size
                "camera" -> if (k == "RIGHT") camFront = !camFront
                "msgview" -> mv = (mv + 1) % size
                "compose" -> if (cStage == 0) pickContact(1)
                else -> sel = (sel + 1) % size
            }
            "OK", "SOFTL" -> when (screen) {
                "home" -> open("menu")
                "menu" -> when (val id = MENU[sel].second) {
                    "zalo" -> launchPkg("com.zing.zalo", "Zalo")
                    "youtube" -> launchPkg("com.google.android.youtube", "YouTube")
                    else -> open(id)
                }
                "apps" -> apps.getOrNull(sel)?.let { a ->
                    ctx.packageManager.getLaunchIntentForPackage(a.pkg)?.let { launch(it) }
                }
                "settings" -> when (sel) {
                    0 -> launch(Intent(Settings.ACTION_HOME_SETTINGS))
                    1 -> open("sound")
                    2 -> toggleBt()
                    else -> open("reset")
                }
                "sound" -> when (sel) {
                    0 -> ringPick.launch(Sound.pickerIntent(ctx, RingtoneManager.TYPE_RINGTONE))
                    1 -> open("volume")
                    else -> alarmPick.launch(Sound.pickerIntent(ctx, RingtoneManager.TYPE_ALARM))
                }
                "volume" -> adjustVol(1, true)
                "reset" -> doReset()
                "snake" -> if (snake.dead) snake.reset()
                "clock" -> startEdit(sel)
                "calendar" -> monthOffset = 0
                "alarmEdit" -> if (editField == 3 && editIdx < alarms.size) deleteAlarm() else saveAlarm()
                "contacts" -> contacts.getOrNull(sel)?.let { callNow(it.number) }
                "messages" -> if (sel == 0) startCompose("", "", "new") else threads.getOrNull(sel - 1)?.let { openThread(it.key) }
                "thread" -> if (sel == 0) startCompose(curAddr, curName, "thread") else { mv = sel - 1; screen = "msgview" }
                "msgview" -> startCompose(curAddr, curName, "thread")
                "compose" -> if (cStage == 0) { if (cTo.isNotEmpty()) cStage = 1 }
                    else if (k == "SOFTL") doSend() else entry.newline()
                "calc" -> if (k == "OK") calc.compute() else calc.back()
                "camera" -> if (camOk) takePhoto(ctx, cam)
                "recorder" -> if (sel == 0) startRecording() else togglePlay(sel)
                "recording" -> stopRecording()
            }
            "SOFTR" -> if (screen == "home") { if (dial.isNotEmpty()) dial = dial.dropLast(1) }
                else if (screen == "compose" && cStage == 0 && cTo.isNotEmpty()) { cTo = cTo.dropLast(1); cName = "" }
                else if (screen == "compose" && cStage == 1 && entry.text.isNotEmpty()) entry.backspace()
                else back()
            "END" -> if (screen == "home") dial = "" else open("home")
            "CALL" -> when (screen) {
                "home" -> if (dial.isNotEmpty()) callNow(dial)
                "contacts" -> contacts.getOrNull(sel)?.let { callNow(it.number) }
                "thread", "msgview" -> callNow(dialable(curAddr))
            }
            else -> when (screen) {
                "home" -> dial += k
                "snake" -> when (k) {
                    "2" -> snake.turn(0 to -1); "8" -> snake.turn(0 to 1)
                    "4" -> snake.turn(-1 to 0); "6" -> snake.turn(1 to 0)
                }
                "calc" -> when (k) {
                    "*" -> calc.comma()
                    "#" -> calc.clear()
                    else -> if (k.length == 1 && k[0].isDigit()) calc.digit(k)
                }
                "contacts" -> jumpTo(k)
                "compose" -> if (cStage == 0) {
                    if (k[0].isDigit()) { cTo += k; cName = "" } else if (k == "*" && cTo.isEmpty()) cTo = "+"
                } else entry.key(k, SystemClock.uptimeMillis())
                "recorder" -> if (k == "#" && sel > 0) deleteRec(sel)
            }
        }
    }

    val tapItem: (Int) -> Unit = { i -> sel = i; press("OK") }
    val scroll: (Int) -> Unit = { d -> press(if (d > 0) "DOWN" else "UP") }

    SideEffect { NokiaState.listener = { k -> press(k) } }
    BackHandler { if (!locked) back() }

    val left = when (screen) {
        "home" -> "Menu"; "menu", "apps" -> "Chọn"; "settings", "sound" -> "Chọn"; "reset" -> "Có"
        "snake" -> if (snake.dead) "Chơi lại" else ""
        "clock" -> "Sửa"; "calendar" -> "Hôm nay"
        "contacts" -> "Gọi"; "messages", "thread" -> "Chọn"; "msgview" -> "Trả lời"
        "compose" -> if (cStage == 1) "Gửi" else "Tiếp"
        "calc" -> "Xóa"; "camera" -> "Chụp"; "recording" -> "Lưu"
        "recorder" -> if (sel == 0) "Ghi" else if (playing != null && playing == recs.getOrNull(sel - 1)?.file?.absolutePath) "Dừng" else "Nghe"
        "alarmEdit" -> if (editField == 3 && editIdx < alarms.size) "Xóa" else "Lưu"
        else -> ""
    }
    val right = when (screen) {
        "home" -> if (dial.isEmpty()) "" else "Xóa"
        "reset" -> "Không"
        "compose" -> if ((cStage == 0 && cTo.isNotEmpty()) || (cStage == 1 && entry.text.isNotEmpty())) "Xóa" else "Về"
        else -> "Về"
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Tỉ lệ gốc của màn hình điện thoại thật -> làm chuẩn cho màn hình LCD nhỏ
        val ratio = maxWidth / maxHeight
        Column(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF4469BC), Color(0xFF223C78))))
                .padding(bottom = 10.dp)
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
            // Màn hình LCD nhỏ (phần duy nhất thay đổi)
            Box(Modifier.fillMaxWidth().weight(0.72f).background(LCD), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().background(LCD).statusBarsPadding()) {
                    Box(Modifier.weight(1f).fillMaxWidth().onGloballyPositioned { lcdRect = it.boundsInWindow() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)) {
                        when (screen) {
                            "home" -> Column(Modifier.fillMaxSize().clickable { press("OK") }, Arrangement.SpaceEvenly, Alignment.CenterHorizontally) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(now.take(5), color = INK, fontSize = 80.sp, fontWeight = FontWeight.Bold, fontFamily = MONO)
                                    Text(dateNow(), color = INK, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = MONO,
                                        maxLines = 1, overflow = TextOverflow.Clip)
                                }
                                Text(dial, color = INK, fontSize = 32.sp, fontFamily = MONO, maxLines = 1, overflow = TextOverflow.Clip)
                            }
                            "menu" -> Lines(MENU.map { it.first }, sel, tapItem, scroll)
                            "apps" -> Lines(apps.map { it.label }.ifEmpty { listOf("(trống)") }, sel, tapItem, scroll)
                            "clock" -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(now, Modifier.padding(top = 16.dp), color = INK, fontSize = 52.sp, fontWeight = FontWeight.Bold, fontFamily = MONO)
                                Text(dateNow(), color = INK, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = MONO)
                                Spacer(Modifier.height(14.dp))
                                Text("BÁO THỨC", Modifier.fillMaxWidth().background(INK).padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = LCD, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = MONO)
                                Box(Modifier.weight(1f).fillMaxWidth()) {
                                    Lines(alarms.map { "%02d:%02d   %s".format(it.h, it.m, if (it.on) "BẬT" else "TẮT") } + "+ Thêm báo thức",
                                        sel, tapItem, scroll)
                                }
                            }
                            "alarmEdit" -> Column(Modifier.fillMaxSize(), Arrangement.SpaceEvenly, Alignment.CenterHorizontally) {
                                Text(if (editIdx < alarms.size) "Sửa báo thức" else "Báo thức mới",
                                    color = INK, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = MONO)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    FieldBox("%02d".format(eh), editField == 0, 56) { if (editField == 0) editMove("UP") else editField = 0 }
                                    Text(":", color = INK, fontSize = 56.sp, fontWeight = FontWeight.Bold, fontFamily = MONO)
                                    FieldBox("%02d".format(em), editField == 1, 56) { if (editField == 1) editMove("UP") else editField = 1 }
                                }
                                FieldBox(if (eon) "Báo thức: BẬT" else "Báo thức: TẮT", editField == 2, 18) { if (editField == 2) editMove("UP") else editField = 2 }
                                if (editIdx < alarms.size) FieldBox("Xóa báo thức", editField == 3, 18) { if (editField == 3) deleteAlarm() else editField = 3 }
                                Text("◀ ▶ chọn ô    ▲ ▼ đổi giá trị", color = INK, fontSize = 12.sp, fontFamily = MONO)
                            }
                            "calendar" -> MonthCalendar(monthOffset)
                            "settings" -> Lines(listOf("Chọn launcher", "Cài đặt âm thanh",
                                "Bluetooth: " + if (btOn) "BẬT" else "TẮT", "Khôi phục cài đặt gốc"), sel, tapItem, scroll)
                            "sound" -> Lines(listOf("Nhạc chuông: $ringName", "Âm lượng", "Nhạc báo thức: $alarmName"), sel, tapItem, scroll)
                            "volume" -> Lines(VOL.map { (n, st) -> volTick.let { volLine(ctx, n, st) } }, sel, tapItem, scroll)
                            "reset" -> Column(Modifier.fillMaxSize().padding(8.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                                Text("Khôi phục cài đặt gốc?", color = INK, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                    fontFamily = MONO, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(10.dp))
                                Text("Xóa toàn bộ dữ liệu của riêng app Nokia Phone (báo thức, nhạc báo thức, quyền đã cấp). Máy của bạn không bị ảnh hưởng.",
                                    color = INK, fontSize = 14.sp, fontFamily = MONO, textAlign = TextAlign.Center)
                            }
                            "contacts" -> if (contacts.isEmpty())
                                Msg(if (granted(Manifest.permission.READ_CONTACTS)) "Danh bạ trống" else "Cần quyền Danh bạ")
                                else Lines(cLines, sel, tapItem, scroll)
                            "messages" -> Lines(listOf("+ Soạn tin mới") + threads.map {
                                (if (it.last.sent) "→ " else "") + it.name + ": " + it.last.body.replace('\n', ' ')
                            }, sel, tapItem, scroll)
                            "thread" -> Column(Modifier.fillMaxSize()) {
                                Head(curName)
                                Box(Modifier.weight(1f)) {
                                    Lines(listOf("+ Trả lời") + threadMsgs.map {
                                        (if (it.sent) "→ " else "← ") + it.body.replace('\n', ' ')
                                    }, sel, tapItem, scroll)
                                }
                            }
                            "msgview" -> MsgView(curName, threadMsgs.getOrNull(mv), mv, threadMsgs.size)
                            "compose" -> ComposeView(cStage, cTo, cName, entry)
                            "calc" -> CalcScreen(calc)
                            "camera" -> if (camOk) Box(Modifier.fillMaxSize()) {
                                CameraView(cam, camFront)
                                Box(Modifier.fillMaxSize().clickable { press("OK") })
                                Text(if (camFront) "Camera trước  (◀▶ đổi)" else "Camera sau  (◀▶ đổi)",
                                    Modifier.align(Alignment.TopCenter).background(LCD).padding(horizontal = 6.dp),
                                    color = INK, fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            } else Msg("Cần quyền Máy ảnh")
                            "gallery" -> GalleryView(gOk, gLoaded, gIds.size, gIdx, gBmp) { d ->
                                if (gIds.isNotEmpty()) gIdx = (gIdx + d + gIds.size) % gIds.size
                            }
                            "recorder" -> Column(Modifier.fillMaxSize()) {
                                Box(Modifier.weight(1f)) { Lines(recLines(recs, playing, recTick), sel, tapItem, scroll) }
                                Text("OK: ghi/nghe   #: xóa", Modifier.fillMaxWidth().background(INK).padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = LCD, fontFamily = MONO, fontSize = 12.sp, maxLines = 1)
                            }
                            "recording" -> Column(Modifier.fillMaxSize().clickable { press("OK") }, Arrangement.Center, Alignment.CenterHorizontally) {
                                Text(if ((recTick / 2) % 2 == 0) "● ĐANG GHI ÂM" else "○ ĐANG GHI ÂM",
                                    color = INK, fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(recTick.let { mmss(SystemClock.elapsedRealtime() - Rec.startedAt) },
                                    color = INK, fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 56.sp)
                                Text("OK: dừng và lưu", color = INK, fontFamily = MONO, fontSize = 14.sp)
                            }
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
                if (locked) LockScreen(now.take(5), dateNow(), lockAt != 0L)
            }
            }

            if (!inSplit.value) {
                Spacer(Modifier.height(6.dp))
                Keys(::press)
            }
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

/** Mũi tên hướng về bên trái (phím Quay lại). */
@Composable
fun BackArrow(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height; val sw = w * 0.14f
        val l = Offset(w * 0.10f, h * 0.5f); val r = Offset(w * 0.90f, h * 0.5f)
        drawLine(color, l, r, sw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color, l, Offset(w * 0.40f, h * 0.18f), sw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color, l, Offset(w * 0.40f, h * 0.82f), sw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
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
    BoxWithConstraints(Modifier.fillMaxWidth().weight(0.27f).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        val s = minOf(maxHeight, maxWidth * 0.48f)
        val c = s / 3
        Row(Modifier.fillMaxWidth().height(s)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                K(Modifier.fillMaxWidth().height(s * 0.336f), shape = RoundedCornerShape(16.dp), onClick = { p("SOFTL") }) { Glyph("Menu", 20) }
                K(Modifier.fillMaxWidth().height(s * 0.336f), shape = RoundedCornerShape(14.dp), onClick = { p("CALL") }) {
                    PhoneIcon(GREEN, false, Modifier.size(31.dp))
                }
            }
            Column(Modifier.width(s).fillMaxHeight()) {
                Row(Modifier.height(c)) {
                    Spacer(Modifier.size(c))
                    K(Modifier.size(c), shape = RoundedCornerShape(10.dp), onClick = { p("UP") }) { Glyph("▲", 14) }
                    Spacer(Modifier.size(c))
                }
                Row(Modifier.height(c)) {
                    K(Modifier.size(c), shape = RoundedCornerShape(10.dp), onClick = { p("LEFT") }) { Glyph("◀", 14) }
                    K(Modifier.size(c), shape = RoundedCornerShape(10.dp), bg = dk, onClick = { p("OK") }) { Glyph("OK", 15) }
                    K(Modifier.size(c), shape = RoundedCornerShape(10.dp), onClick = { p("RIGHT") }) { Glyph("▶", 14) }
                }
                Row(Modifier.height(c)) {
                    Spacer(Modifier.size(c))
                    K(Modifier.size(c), shape = RoundedCornerShape(10.dp), onClick = { p("DOWN") }) { Glyph("▼", 14) }
                    Spacer(Modifier.size(c))
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                K(Modifier.fillMaxWidth().height(s * 0.336f), shape = RoundedCornerShape(16.dp), onClick = { p("SOFTR") }) { BackArrow(KEYTXT, Modifier.size(34.dp)) }
                K(Modifier.fillMaxWidth().height(s * 0.336f), shape = RoundedCornerShape(14.dp), onClick = { p("END") }) {
                    PhoneIcon(RED, true, Modifier.size(31.dp))
                }
            }
        }
    }
    // Bàn phím số
    Column(Modifier.fillMaxWidth().weight(0.40f).padding(horizontal = 12.dp)) {
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
