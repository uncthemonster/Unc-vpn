package com.example.myvpn

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Server(val id: String, val name: String, val countryCode: String, val config: String)

class ServerStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("servers", Context.MODE_PRIVATE)

    fun load(): List<Server> = try {
        val arr = JSONArray(prefs.getString("list", "[]"))
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Server(o.getString("id"), o.getString("name"), o.getString("cc"), o.getString("config"))
        }
    } catch (e: Exception) { emptyList() }

    fun save(list: List<Server>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name).put("cc", it.countryCode).put("config", it.config))
        }
        prefs.edit().putString("list", arr.toString()).apply()
    }

    var selectedId: String?
        get() = prefs.getString("selected", null)
        set(v) { prefs.edit().putString("selected", v).apply() }
}
