package com.domino.score

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import org.json.JSONObject

data class Player(val id: Int, val name: String)
data class Team(val id: Int, val name: String, val p1: Int, val p2: Int)
data class Hand(val a: Int, val b: Int)
data class Game(
    val id: Int, val date: Long, val mode: String, val target: Int, val bc: Int,
    val names: List<String>, val players: List<List<Int>>, val hands: List<Hand> = emptyList()
)

data class Row(val h: Hand, val a: Int, val b: Int, val reset: Boolean, val ignored: Boolean)
data class Res(
    val a: Int, val b: Int, val resets: Int, val berbookPhase: Boolean,
    val rows: List<Row>, val winner: Int?, val berbook: Boolean
)

/** موتور امتیازدهی: همهٔ دست‌ها از اول بازپخش می‌شوند (برای ویرایش دست‌ها ضروری است) */
fun replay(g: Game): Res {
    var a = 0; var b = 0; var r = 0
    var bp = g.mode == "berbook" || (g.mode == "mix" && g.bc > 0)
    var w: Int? = null; var bb = false
    val rows = mutableListOf<Row>()
    for (h in g.hands) {
        if (w != null) { rows += Row(h, a, b, false, true); continue }
        val reset = bp && ((a == 0 && b == 0 && h.a > 0 && h.b > 0) ||
                (a > 0 && b == 0 && h.b > 0) || (b > 0 && a == 0 && h.a > 0))
        if (reset) {
            r++; a = 0; b = 0
            if (g.mode == "mix" && r >= g.bc) bp = false
        } else {
            a += h.a; b += h.b
            if (a >= g.target || b >= g.target) {
                w = if (a > b) 0 else 1
                bb = (if (w == 0) b else a) == 0
            }
        }
        rows += Row(h, a, b, reset, false)
    }
    return Res(a, b, r, bp, rows, w, bb)
}

fun Game.toJ(): JSONObject = JSONObject().put("id", id).put("date", date).put("mode", mode)
    .put("target", target).put("bc", bc).put("names", JSONArray(names))
    .put("pl", JSONArray(players.flatten()))
    .put("hands", JSONArray(hands.map { JSONArray(listOf(it.a, it.b)) }))

fun gameFrom(o: JSONObject): Game {
    val pl = o.getJSONArray("pl"); val n = o.getJSONArray("names"); val h = o.getJSONArray("hands")
    return Game(
        o.getInt("id"), o.getLong("date"), o.getString("mode"), o.getInt("target"), o.getInt("bc"),
        List(2) { n.getString(it) },
        listOf(listOf(pl.getInt(0), pl.getInt(1)), listOf(pl.getInt(2), pl.getInt(3))),
        List(h.length()) { Hand(h.getJSONArray(it).getInt(0), h.getJSONArray(it).getInt(1)) }
    )
}

class PStat {
    var g = 0; var w = 0; var bw = 0; var bl = 0; var max = 0
    val pt = mutableMapOf<Int, IntArray>()   // هم‌تیمی‌ها: [برد, باخت]
    val op = mutableMapOf<Int, IntArray>()   // حریفان: [برد, باخت]
    val pct get() = if (g == 0) 0 else w * 100 / g
}

class AppVm(app: Application) : AndroidViewModel(app) {
    private val sp = app.getSharedPreferences("dom", 0)
    var players by mutableStateOf(listOf<Player>()); private set
    var teams by mutableStateOf(listOf<Team>()); private set
    var games by mutableStateOf(listOf<Game>()); private set
    var cur by mutableStateOf<Game?>(null); private set
    var theme by mutableStateOf(0); private set   // 0 خودکار، 1 روشن، 2 تیره
    private var nextId = 1

    init { import(sp.getString("data", null)) }

    fun name(id: Int) = players.find { it.id == id }?.name ?: "؟"
    fun tn(g: Game, i: Int) = g.names[i].ifBlank { g.players[i].joinToString(" و ") { name(it) } }

    fun export(): String = JSONObject().put("nid", nextId).put("theme", theme)
        .put("players", JSONArray(players.map { JSONObject().put("id", it.id).put("name", it.name) }))
        .put("teams", JSONArray(teams.map {
            JSONObject().put("id", it.id).put("name", it.name).put("p1", it.p1).put("p2", it.p2)
        }))
        .put("games", JSONArray(games.map { it.toJ() }))
        .put("cur", cur?.toJ() ?: JSONObject.NULL).toString()

    fun import(s: String?): Boolean = try {
        val o = JSONObject(s!!)
        nextId = o.getInt("nid"); theme = o.optInt("theme", 0)
        val p = o.getJSONArray("players")
        players = List(p.length()) { Player(p.getJSONObject(it).getInt("id"), p.getJSONObject(it).getString("name")) }
        val t = o.getJSONArray("teams")
        teams = List(t.length()) { val x = t.getJSONObject(it); Team(x.getInt("id"), x.getString("name"), x.getInt("p1"), x.getInt("p2")) }
        val g = o.getJSONArray("games")
        games = List(g.length()) { gameFrom(g.getJSONObject(it)) }
        cur = if (o.isNull("cur")) null else gameFrom(o.getJSONObject("cur"))
        save(); true
    } catch (e: Exception) { false }

    private fun save() { sp.edit().putString("data", export()).apply() }

    fun addPlayer(n: String) { if (n.isBlank()) return; players = players + Player(nextId++, n.trim()); save() }
    fun rename(id: Int, n: String) { players = players.map { if (it.id == id) it.copy(name = n) else it }; save() }
    fun deletePlayer(id: Int): Boolean {
        if (games.any { g -> g.players.flatten().contains(id) } || cur?.players?.flatten()?.contains(id) == true) return false
        players = players.filter { it.id != id }; teams = teams.filter { it.p1 != id && it.p2 != id }; save(); return true
    }
    fun addTeam(n: String, a: Int, b: Int) { teams = teams + Team(nextId++, n.trim(), a, b); save() }
    fun deleteTeam(id: Int) { teams = teams.filter { it.id != id }; save() }
    fun renameTeam(id: Int, n: String) { teams = teams.map { if (it.id == id) it.copy(name = n) else it }; save() }

    fun start(mode: String, target: Int, bc: Int, names: List<String>, ids: List<Int>) {
        cur = Game(nextId++, System.currentTimeMillis(), mode, target, bc, names, listOf(ids.take(2), ids.drop(2))); save()
    }
    private fun upd(f: (List<Hand>) -> List<Hand>) { cur = cur?.let { it.copy(hands = f(it.hands)) }; save() }
    fun addHand(a: Int, b: Int) = upd { it + Hand(a, b) }
    fun editHand(i: Int, a: Int, b: Int) = upd { l -> l.mapIndexed { k, h -> if (k == i) Hand(a, b) else h } }
    fun delHand(i: Int) = upd { l -> l.filterIndexed { k, _ -> k != i } }
    fun finish() { cur?.let { games = listOf(it) + games }; cur = null; save() }
    fun cancel() { cur = null; save() }
    fun clearHistory() { games = emptyList(); save() }
    fun updateTheme(t: Int) { theme = t; save() }

    fun stats(): Map<Int, PStat> {
        val m = players.associate { it.id to PStat() }
        for (g in games) {
            val r = replay(g); val w = r.winner ?: continue
            for (i in 0..1) for (id in g.players[i]) {
                val s = m[id] ?: continue; val win = w == i
                s.g++; if (win) s.w++
                if (r.berbook) { if (win) s.bw++ else s.bl++ }
                s.max = maxOf(s.max, if (i == 0) r.a else r.b)
                val mate = g.players[i].first { it != id }
                s.pt.getOrPut(mate) { IntArray(2) }[if (win) 0 else 1]++
                for (o in g.players[1 - i]) s.op.getOrPut(o) { IntArray(2) }[if (win) 0 else 1]++
            }
        }
        return m
    }
}
