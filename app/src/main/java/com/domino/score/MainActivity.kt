@file:OptIn(ExperimentalMaterial3Api::class)

package com.domino.score

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Green = Color(0xFF1E9E5A)
val Red = Color(0xFFD23F3F)
val Gold = Color(0xFFB8892B)
val VZ = FontFamily(Font(R.font.vazirmatn_regular), Font(R.font.vazirmatn_bold, FontWeight.Bold))

fun Typography.withFont(f: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = f), displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f), headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f), headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f), titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f), bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f), bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f), labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f)
)

fun fmtDate(t: Long): String =
    SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.forLanguageTag("fa-IR-u-ca-persian")).format(Date(t))

class MainActivity : ComponentActivity() {
    private val vm: AppVm by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App(vm) }
    }
}

class BentoColors(val bg: Color, val card: Color, val mint: Color, val sky: Color, val peach: Color, val lilac: Color, val butter: Color)
val BentoLight = BentoColors(Color(0xFFF1F0EB), Color.White, Color(0xFFD7EFDF), Color(0xFFD8E8F7), Color(0xFFFADFCB), Color(0xFFE4DCF5), Color(0xFFF6EBC4))
val BentoDark = BentoColors(Color(0xFF0F1416), Color(0xFF1A2124), Color(0xFF1B3A2C), Color(0xFF1C3347), Color(0xFF48301F), Color(0xFF2E2848), Color(0xFF453B1A))
val LocalBento = staticCompositionLocalOf { BentoLight }

@Composable
fun App(vm: AppVm) {
    val dark = when (vm.theme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val bt = if (dark) BentoDark else BentoLight
    val cs = if (dark) darkColorScheme(primary = Color(0xFF3FCF85), onPrimary = Color(0xFF06210F), background = bt.bg, surface = bt.card,
        onBackground = Color(0xFFE8EFE9), onSurface = Color(0xFFE8EFE9))
    else lightColorScheme(primary = Color(0xFF14452F), onPrimary = Color.White, background = bt.bg, surface = bt.card,
        onBackground = Color(0xFF1D2A26), onSurface = Color(0xFF1D2A26))
    MaterialTheme(colorScheme = cs, typography = Typography().withFont(VZ)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, LocalBento provides bt) {
            var tab by rememberSaveable { mutableStateOf(0) }
            Scaffold(
                containerColor = bt.bg,
                bottomBar = {
                    NavigationBar(containerColor = bt.card) {
                        listOf("🎲" to "بازی", "👥" to "بازیکنان", "📜" to "تاریخچه", "🏆" to "آمار", "⚙️" to "تنظیمات")
                            .forEachIndexed { i, (e, l) ->
                                NavigationBarItem(tab == i, { tab = i }, { Text(e, fontSize = 20.sp) }, label = { Text(l, fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(indicatorColor = bt.mint))
                            }
                    }
                }
            ) { pad ->
                AnimatedContent(
                    targetState = tab, modifier = Modifier.padding(pad).fillMaxSize(),
                    transitionSpec = {
                        val fwd = targetState > initialState
                        (slideInHorizontally(tween(320)) { w -> if (fwd) -w / 3 else w / 3 } + fadeIn(tween(320))) togetherWith
                            (slideOutHorizontally(tween(320)) { w -> if (fwd) w / 3 else -w / 3 } + fadeOut(tween(200)))
                    }, label = "tabs"
                ) { t ->
                    when (t) { 0 -> GameTab(vm); 1 -> PlayersTab(vm); 2 -> HistoryTab(vm); 3 -> StatsTab(vm); else -> SettingsTab(vm) }
                }
            }
        }
    }
}

/* ---------- کمک‌کامپوننت‌ها (کاشی‌های Bento) ---------- */
@Composable
fun Card2(title: String? = null, c: Color = LocalBento.current.card, mod: Modifier = Modifier.fillMaxWidth(), content: @Composable ColumnScope.() -> Unit) {
    Card(mod.padding(vertical = 5.dp), shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = c, contentColor = MaterialTheme.colorScheme.onSurface)) {
        Column(Modifier.padding(16.dp)) {
            if (title != null) Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 10.dp))
            content()
        }
    }
}

@Composable
fun Stat(v: String, l: String, c: Color, mod: Modifier = Modifier, vc: Color = Color.Unspecified) = Card2(c = c, mod = mod) {
    Text(v, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = vc, maxLines = 1)
    Text(l, fontSize = 12.sp)
}

@Composable
fun Num(v: String, l: String, mod: Modifier = Modifier, on: (String) -> Unit) = OutlinedTextField(
    v, { on(it.filter { c -> c.isDigit() }.take(4)) }, label = { Text(l, maxLines = 1) }, singleLine = true, shape = RoundedCornerShape(16.dp),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = mod
)

@Composable
fun Picker(sel: Int, items: List<Pair<Int, String>>, hint: String, mod: Modifier = Modifier, on: (Int) -> Unit) {
    var o by remember { mutableStateOf(false) }
    Box(mod) {
        OutlinedButton({ o = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text(items.find { it.first == sel }?.second ?: hint, maxLines = 1) }
        DropdownMenu(o, { o = false }) { items.forEach { DropdownMenuItem({ Text(it.second) }, { on(it.first); o = false }) } }
    }
}

@Composable
fun Seg(t: String, on: Boolean, mod: Modifier = Modifier, click: () -> Unit) {
    val sh = RoundedCornerShape(16.dp)
    if (on) Button(click, mod, shape = sh) { Text(t) } else OutlinedButton(click, mod, shape = sh) { Text(t) }
}

@Composable
fun RowScope.Cell(t: String, w: Float = 1f, c: Color = Color.Unspecified, b: Boolean = false) =
    Text(t, Modifier.weight(w), textAlign = TextAlign.Center, color = c, fontSize = 13.sp, fontWeight = if (b) FontWeight.Bold else null)

fun Modifier.dominoBg(c: Color) = drawBehind {
    val s = 170.dp.toPx(); var y = 0f; var row = 0
    while (y < size.height) {
        var x = if (row % 2 == 0) 0f else s / 2; var k = 0
        while (x < size.width) {
            val ang = if ((row + k) % 2 == 0) -20f else 35f
            val w = 70.dp.toPx(); val h = 35.dp.toPx(); val st = 2.dp.toPx(); val d = 3.dp.toPx()
            rotate(ang, Offset(x + w / 2, y + h / 2)) {
                drawRoundRect(c, Offset(x, y), Size(w, h), CornerRadius(6.dp.toPx()), style = Stroke(st))
                drawLine(c, Offset(x + w / 2, y), Offset(x + w / 2, y + h), st)
                drawCircle(c, d, Offset(x + w * .25f, y + h / 2))
                drawCircle(c, d, Offset(x + w * .75f, y + h * .3f))
                drawCircle(c, d, Offset(x + w * .75f, y + h * .7f))
            }
            x += s; k++
        }
        y += s * .7f; row++
    }
}

/* ---------- تب بازی ---------- */
@Composable
fun GameTab(vm: AppVm) {
    AnimatedContent(
        targetState = vm.cur != null,
        transitionSpec = { (fadeIn(tween(300)) + scaleIn(tween(300), 0.92f)) togetherWith fadeOut(tween(150)) },
        label = "game"
    ) { playing ->
        val g = vm.cur
        if (playing && g != null) Play(vm, g) else Setup(vm)
    }
}

@Composable
fun Setup(vm: AppVm) {
    val bt = LocalBento.current
    var mode by rememberSaveable { mutableStateOf("score") }
    var target by rememberSaveable { mutableStateOf("102") }
    var bc by rememberSaveable { mutableStateOf("2") }
    val ids = remember { mutableStateListOf(0, 0, 0, 0) }
    val names = remember { mutableStateListOf("", "") }
    var err by remember { mutableStateOf("") }
    val pl = vm.players.map { it.id to it.name }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp)) {
            if (vm.players.size < 4) Card2(c = bt.butter) { Text("ابتدا از تب «بازیکنان» حداقل ۴ بازیکن اضافه کنید.") }
            Card2("حالت بازی", bt.mint) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("score" to "امتیازی", "berbook" to "بربوک", "mix" to "ترکیبی").forEach { (k, l) ->
                        Seg(l, mode == k, Modifier.weight(1f)) { mode = k }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card2("امتیاز نهایی", bt.butter, Modifier.weight(1f)) { Num(target, "امتیاز", Modifier.fillMaxWidth()) { target = it } }
                Card2("دفعات بربوک", bt.lilac, Modifier.weight(1f)) {
                    if (mode == "mix") Num(bc, "تعداد", Modifier.fillMaxWidth()) { bc = it }
                    else Text("فقط در حالت ترکیبی فعال است", fontSize = 13.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (i in 0..1) Card2("تیم ${i + 1}", if (i == 0) bt.sky else bt.peach, Modifier.weight(1f)) {
                    if (vm.teams.isNotEmpty()) Picker(0, vm.teams.map { it.id to it.name.ifBlank { "${vm.name(it.p1)} و ${vm.name(it.p2)}" } }, "تیم ذخیره‌شده", Modifier.fillMaxWidth().padding(bottom = 6.dp)) { id ->
                        vm.teams.find { it.id == id }?.let { ids[i * 2] = it.p1; ids[i * 2 + 1] = it.p2; names[i] = it.name }
                    }
                    Picker(ids[i * 2], pl, "بازیکن ۱", Modifier.fillMaxWidth().padding(bottom = 6.dp)) { ids[i * 2] = it }
                    Picker(ids[i * 2 + 1], pl, "بازیکن ۲", Modifier.fillMaxWidth().padding(bottom = 6.dp)) { ids[i * 2 + 1] = it }
                    OutlinedTextField(names[i], { names[i] = it }, label = { Text("نام تیم (اختیاری)") }, singleLine = true,
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                }
            }
        }
        // دکمهٔ شروع مسابقه: پایین صفحه
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            if (err.isNotEmpty()) Text(err, color = Red, modifier = Modifier.padding(bottom = 6.dp))
            Button({
                val t = target.toIntOrNull() ?: 0
                if (ids.any { it == 0 } || ids.toSet().size < 4) err = "چهار بازیکن متفاوت انتخاب کنید"
                else if (t < 1) err = "امتیاز نهایی نامعتبر است"
                else { err = ""; vm.start(mode, t, bc.toIntOrNull() ?: 0, names.toList(), ids.toList()) }
            }, Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(22.dp)) {
                Text("🎲 شروع مسابقه دومینو", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun Play(vm: AppVm, g: Game) {
    val bt = LocalBento.current
    val r = remember(g) { replay(g) }
    val done = r.winner != null
    var ea by remember { mutableStateOf("") }
    var eb by remember { mutableStateOf("") }
    var edit by remember { mutableStateOf(-1) }
    var conf by remember { mutableStateOf(false) }
    val modeName = mapOf("score" to "امتیازی", "berbook" to "بربوک", "mix" to "ترکیبی")[g.mode] ?: ""
    Box(Modifier.fillMaxSize().dominoBg(Color.Gray.copy(alpha = .15f))) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp)) {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat(modeName, "حالت بازی", bt.butter, Modifier.weight(1f).fillMaxHeight())
                Stat("${g.target}", "امتیاز نهایی", bt.lilac, Modifier.weight(1f).fillMaxHeight())
                if (g.mode != "score") Stat("${r.resets}" + (if (g.mode == "mix") "/${g.bc}" else ""),
                    "صفر شدن · " + (if (r.berbookPhase) "بربوک" else "امتیازی"), bt.mint, Modifier.weight(1f).fillMaxHeight())
            }
            if (done) Card2(c = if (r.berbook) Red else Green) {
                Text((if (r.berbook) "🎉 بربوک! " else "🏆 ") + "برنده: " + vm.tn(g, r.winner!!), Modifier.fillMaxWidth(),
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
            }
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (i in 0..1) {
                    val v = if (i == 0) r.a else r.b
                    val av by animateIntAsState(v, tween(600), label = "score$i")
                    Card2((if (done && r.winner == i) "🏆 " else "") + vm.tn(g, i), if (i == 0) bt.sky else bt.peach, Modifier.weight(1f).fillMaxHeight()) {
                        Text("$av", Modifier.fillMaxWidth(), fontSize = 52.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                        LinearProgressIndicator(progress = minOf(1f, av / g.target.toFloat()), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)))
                    }
                }
            }
            if (!done) Card2("ثبت دست جدید", bt.mint) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Num(ea, vm.tn(g, 0), Modifier.weight(1f)) { ea = it }
                    Num(eb, vm.tn(g, 1), Modifier.weight(1f)) { eb = it }
                }
                Button({
                    val a = ea.toIntOrNull() ?: 0; val b = eb.toIntOrNull() ?: 0
                    if (a + b > 0) { vm.addHand(a, b); ea = ""; eb = "" }
                }, Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(16.dp)) { Text("ثبت امتیاز") }
            }
            Card2("دست‌ها (برای ویرایش لمس کنید)") {
                if (r.rows.isEmpty()) Text("هنوز دستی ثبت نشده.")
                r.rows.forEachIndexed { i, x ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { edit = i }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Cell("${i + 1}", .5f); Cell("${x.h.a}"); Cell("${x.h.b}")
                        if (x.ignored) Cell("—", 1.5f) else if (x.reset) Cell("🔄 صفر شد", 1.5f, Red, true) else Cell("${x.a} / ${x.b}", 1.5f, b = true)
                    }
                    HorizontalDivider()
                }
            }
            if (done) Button({ vm.finish() }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(Green)) { Text("ذخیره نتیجه و بازی جدید") }
            TextButton({ conf = true }, Modifier.fillMaxWidth()) { Text("لغو بازی", color = Red) }
        }
    }
    if (edit in g.hands.indices) {
        val h = g.hands[edit]
        var x by remember(edit) { mutableStateOf(h.a.toString()) }
        var y by remember(edit) { mutableStateOf(h.b.toString()) }
        AlertDialog(onDismissRequest = { edit = -1 }, title = { Text("ویرایش دست ${edit + 1}") }, shape = RoundedCornerShape(28.dp),
            text = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Num(x, vm.tn(g, 0), Modifier.weight(1f)) { x = it }; Num(y, vm.tn(g, 1), Modifier.weight(1f)) { y = it } } },
            confirmButton = { TextButton({ vm.editHand(edit, x.toIntOrNull() ?: 0, y.toIntOrNull() ?: 0); edit = -1 }) { Text("ذخیره") } },
            dismissButton = { Row { TextButton({ vm.delHand(edit); edit = -1 }) { Text("حذف", color = Red) }; TextButton({ edit = -1 }) { Text("بستن") } } })
    }
    if (conf) AlertDialog(onDismissRequest = { conf = false }, title = { Text("لغو بازی؟") }, text = { Text("امتیازات این بازی ذخیره نمی‌شود.") }, shape = RoundedCornerShape(28.dp),
        confirmButton = { TextButton({ conf = false; vm.cancel() }) { Text("لغو بازی", color = Red) } },
        dismissButton = { TextButton({ conf = false }) { Text("ادامه بازی") } })
}

/* ---------- بازیکنان و تیم‌ها ---------- */
@Composable
fun PlayersTab(vm: AppVm) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var q by remember { mutableStateOf("") }
    var open by remember { mutableStateOf(0) }
    var ren by remember { mutableStateOf<Player?>(null) }
    val st = vm.stats()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        item {
            Card2("افزودن بازیکن", LocalBento.current.mint) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(name, { name = it }, Modifier.weight(1f), label = { Text("نام بازیکن") }, singleLine = true)
                    Button({ vm.addPlayer(name); name = "" }) { Text("افزودن") }
                }
            }
            OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), label = { Text("🔍 جست‌وجو") }, singleLine = true)
        }
        items(vm.players.filter { it.name.contains(q) }, key = { it.id }) { p ->
            val s = st[p.id] ?: PStat()
            Card2 {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.Bold); Text("${s.g} بازی · ${s.pct}٪ برد", fontSize = 12.sp) }
                    TextButton({ open = if (open == p.id) 0 else p.id }) { Text("کارنامه") }
                    TextButton({ ren = p }) { Text("✏️") }
                    TextButton({ if (!vm.deletePlayer(p.id)) Toast.makeText(ctx, "این بازیکن در تاریخچه حضور دارد و حذف نمی‌شود", Toast.LENGTH_LONG).show() }) { Text("🗑") }
                }
                if (open == p.id) {
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    Text("بازی: ${s.g}   برد: ${s.w}   باخت: ${s.g - s.w}   بیشترین امتیاز: ${s.max}")
                    Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("🏆 پیروزی با بربوک: ${s.bw}", color = Green, fontWeight = FontWeight.Bold)
                        Text("شکست با بربوک: ${s.bl}", color = Red, fontWeight = FontWeight.Bold)
                    }
                    Text("هم‌تیمی‌ها", fontWeight = FontWeight.Bold)
                    s.pt.forEach { (id, a) -> Text("${vm.name(id)}: ${a[0]} برد، ${a[1]} باخت", fontSize = 13.sp) }
                    Text("حریفان", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                    s.op.forEach { (id, a) -> Text("${vm.name(id)}: ${a[0]} برد، ${a[1]} باخت", fontSize = 13.sp) }
                }
            }
        }
        item { TeamsCard(vm); Spacer(Modifier.height(12.dp)) }
    }
    ren?.let { p ->
        var n by remember(p.id) { mutableStateOf(p.name) }
        AlertDialog(onDismissRequest = { ren = null }, title = { Text("ویرایش نام بازیکن") },
            text = { OutlinedTextField(n, { n = it }, singleLine = true) },
            confirmButton = { TextButton({ if (n.isNotBlank()) vm.rename(p.id, n.trim()); ren = null }) { Text("ذخیره") } },
            dismissButton = { TextButton({ ren = null }) { Text("بستن") } })
    }
}

@Composable
fun TeamsCard(vm: AppVm) {
    var a by remember { mutableStateOf(0) }
    var b by remember { mutableStateOf(0) }
    var n by remember { mutableStateOf("") }
    val pl = vm.players.map { it.id to it.name }
    Card2("تیم‌های ذخیره‌شده", LocalBento.current.lilac) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Picker(a, pl, "بازیکن ۱", Modifier.weight(1f)) { a = it }
            Picker(b, pl, "بازیکن ۲", Modifier.weight(1f)) { b = it }
        }
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(n, { n = it }, Modifier.weight(1f), label = { Text("نام تیم") }, singleLine = true)
            Button({ if (a != 0 && b != 0 && a != b) { vm.addTeam(n, a, b); n = ""; a = 0; b = 0 } }) { Text("ذخیره") }
        }
        vm.teams.forEach { t ->
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(t.name.ifBlank { "تیم" } + "  (${vm.name(t.p1)} و ${vm.name(t.p2)})", Modifier.weight(1f))
                TextButton({ vm.deleteTeam(t.id) }) { Text("حذف", color = Red) }
            }
        }
    }
}

/* ---------- تاریخچه ---------- */
@Composable
fun HistoryTab(vm: AppVm) {
    val bt = LocalBento.current
    var open by remember { mutableStateOf(0) }
    AnimatedContent(
        targetState = open,
        transitionSpec = {
            if (targetState != 0) (slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300))) togetherWith (slideOutHorizontally(tween(300)) { it / 3 } + fadeOut(tween(200)))
            else (slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(300))) togetherWith (slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(200)))
        }, label = "hist"
    ) { o ->
        val g = vm.games.find { it.id == o }
        if (g != null) {
            val r = replay(g)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp)) {
                TextButton({ open = 0 }) { Text("→ بازگشت") }
                Card2(fmtDate(g.date), bt.sky) {
                    Text("${vm.tn(g, 0)}  ${r.a} : ${r.b}  ${vm.tn(g, 1)}", fontWeight = FontWeight.Bold)
                    Text("برنده: " + (r.winner?.let { vm.tn(g, it) } ?: "—") + if (r.berbook) "   🎉 بربوک" else "", color = if (r.berbook) Green else Color.Unspecified)
                    Text("دفعات صفر شدن: ${r.resets}")
                }
                Card2("جزئیات دست‌ها") {
                    r.rows.forEachIndexed { i, x ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Cell("${i + 1}", .5f); Cell("${x.h.a}"); Cell("${x.h.b}")
                            if (x.reset) Cell("🔄 صفر شد", 1.5f, Red, true) else Cell("${x.a} / ${x.b}", 1.5f)
                        }
                        HorizontalDivider()
                    }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                if (vm.games.isEmpty()) item { Card2(c = bt.butter) { Text("هنوز بازی‌ای ثبت نشده.") } }
                items(vm.games, key = { it.id }) { x ->
                    val r = replay(x)
                    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { open = x.id }, shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(if (r.berbook) bt.mint else bt.card, MaterialTheme.colorScheme.onSurface)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(fmtDate(x.date), fontSize = 12.sp)
                            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Cell(vm.tn(x, 0), 1.5f, if (r.winner == 0) Green else Color.Unspecified, r.winner == 0)
                                Cell("${r.a} : ${r.b}", 1f, b = true)
                                Cell(vm.tn(x, 1), 1.5f, if (r.winner == 1) Green else Color.Unspecified, r.winner == 1)
                            }
                            Text((if (r.berbook) "🎉 بربوک   " else "") + "صفر شدن: ${r.resets}", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/* ---------- آمار ---------- */
@Composable
fun StatsTab(vm: AppVm) {
    val bt = LocalBento.current
    val st = vm.stats()
    val list = vm.players.mapNotNull { p -> st[p.id]?.takeIf { it.g > 0 }?.let { p to it } }
        .sortedWith(compareByDescending<Pair<Player, PStat>> { it.second.pct }.thenByDescending { it.second.g })
    var best = 0; var bestTeam = "—"; var bbs = 0
    vm.games.forEach { g -> val r = replay(g); if (r.berbook) bbs++; val v = maxOf(r.a, r.b); if (v > best) { best = v; bestTeam = vm.tn(g, if (r.a >= r.b) 0 else 1) } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("${vm.games.size}", "تعداد بازی‌ها", bt.sky, Modifier.weight(1f).fillMaxHeight())
            Stat("🎉 $bbs", "تعداد بربوک‌ها", bt.mint, Modifier.weight(1f).fillMaxHeight(), Green)
            Stat("$best", "بیشترین امتیاز", bt.butter, Modifier.weight(1f).fillMaxHeight())
        }
        if (best > 0) Card2(c = bt.lilac) { Text("رکورد امتیاز: $best — $bestTeam", fontWeight = FontWeight.Bold) }
        Card2("جدول رتبه‌بندی") {
            if (list.isEmpty()) Text("داده‌ای نیست.") else {
                Row { Cell("بازیکن", 1.6f, b = true); Cell("بازی", b = true); Cell("برد", b = true); Cell("٪", b = true); Cell("🏆", b = true); Cell("باخت بربوک", 1.2f, b = true) }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                list.forEachIndexed { idx, (p, s) ->
                    Row(Modifier.padding(vertical = 5.dp)) {
                        Cell(listOf("🥇 ", "🥈 ", "🥉 ").getOrElse(idx) { "" } + p.name, 1.6f); Cell("${s.g}"); Cell("${s.w}"); Cell("${s.pct}")
                        Cell("${s.bw}", c = Green, b = true); Cell("${s.bl}", 1.2f, Red, true)
                    }
                }
            }
        }
    }
}

/* ---------- تنظیمات ---------- */
@Composable
fun SettingsTab(vm: AppVm) {
    val ctx = LocalContext.current
    var conf by remember { mutableStateOf(false) }
    val exp = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u ->
        u?.let { ctx.contentResolver.openOutputStream(it)?.use { o -> o.write(vm.export().toByteArray()) }
            Toast.makeText(ctx, "نسخه پشتیبان ذخیره شد", Toast.LENGTH_SHORT).show() }
    }
    val imp = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        u?.let {
            val s = ctx.contentResolver.openInputStream(it)?.bufferedReader()?.readText()
            Toast.makeText(ctx, if (vm.import(s)) "بازیابی شد" else "فایل نامعتبر است", Toast.LENGTH_SHORT).show()
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        Card2("ظاهر برنامه", LocalBento.current.sky) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("خودکار", "روشن", "تیره").forEachIndexed { i, l -> Seg(l, vm.theme == i, Modifier.weight(1f)) { vm.updateTheme(i) } }
            }
        }
        Card2("تغییر نام بازیکنان", LocalBento.current.butter) {
            if (vm.players.isEmpty()) Text("بازیکنی نیست.")
            vm.players.forEach { p -> OutlinedTextField(p.name, { vm.rename(p.id, it) }, Modifier.fillMaxWidth().padding(vertical = 3.dp), singleLine = true) }
        }
        Card2("تغییر نام تیم‌ها") {
            if (vm.teams.isEmpty()) Text("تیمی ذخیره نشده.")
            vm.teams.forEach { t -> OutlinedTextField(t.name, { vm.renameTeam(t.id, it) }, Modifier.fillMaxWidth().padding(vertical = 3.dp), singleLine = true) }
        }
        Card2("پشتیبان‌گیری", LocalBento.current.mint) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ exp.launch("domino-backup.json") }, Modifier.weight(1f)) { Text("ذخیره فایل") }
                OutlinedButton({ imp.launch(arrayOf("*/*")) }, Modifier.weight(1f)) { Text("بازیابی") }
            }
        }
        Card2("مدیریت سابقه", LocalBento.current.peach) { OutlinedButton({ conf = true }, Modifier.fillMaxWidth()) { Text("پاک‌کردن تاریخچه بازی‌ها", color = Red) } }
    }
    if (conf) AlertDialog(onDismissRequest = { conf = false }, title = { Text("پاک‌کردن تاریخچه؟") }, text = { Text("همهٔ بازی‌های ذخیره‌شده حذف می‌شوند.") },
        confirmButton = { TextButton({ vm.clearHistory(); conf = false }) { Text("پاک شود", color = Red) } },
        dismissButton = { TextButton({ conf = false }) { Text("انصراف") } })
}
