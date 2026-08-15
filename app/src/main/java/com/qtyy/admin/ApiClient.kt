package com.qtyy.admin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object ApiClient {
    const val BASE_URL = "https://45.116.78.241"
    @Volatile var token: String? = null

    private fun nullableString(obj: JSONObject, key: String): String? =
        if (obj.has(key) && !obj.isNull(key)) obj.optString(key) else null

    private fun connection(path: String, method: String, auth: Boolean): HttpsURLConnection {
        val url = URL(BASE_URL + path)
        return (url.openConnection() as HttpsURLConnection).apply {
            requestMethod = method
            connectTimeout = 12_000
            readTimeout = 35_000
            useCaches = false
            doInput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "QTYAdmin-Android/1.0")
            if (auth) token?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
    }

    private fun readAll(input: InputStream?): String {
        if (input == null) return ""
        return BufferedInputStream(input).use { stream ->
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            while (true) {
                val n = stream.read(buf)
                if (n <= 0) break
                out.write(buf, 0, n)
            }
            out.toString(Charsets.UTF_8.name())
        }
    }

    private fun parseOrThrow(conn: HttpURLConnection): JSONObject {
        val code = conn.responseCode
        val text = readAll(if (code in 200..299) conn.inputStream else conn.errorStream)
        if (code !in 200..299) {
            val detail = runCatching {
                if (text.isBlank()) null else JSONObject(text).optString("detail").takeIf { it.isNotBlank() }
            }.getOrNull()
            throw ApiException(code, detail ?: "服务器返回错误 $code")
        }
        return if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    suspend fun request(method: String, path: String, body: JSONObject? = null, auth: Boolean = true): JSONObject =
        withContext(Dispatchers.IO) {
            val conn = connection(path, method, auth)
            try {
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    BufferedOutputStream(conn.outputStream).use { out -> out.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }
                parseOrThrow(conn)
            } finally { conn.disconnect() }
        }

    suspend fun login(username: String, password: String): String {
        val json = request("POST", "/api/admin/login", JSONObject().put("username", username).put("password", password), false)
        return json.getString("token").also { token = it }
    }

    suspend fun dashboard(): Dashboard {
        val j = request("GET", "/api/dashboard")
        return Dashboard(j.optInt("user_count"), j.optInt("online_count"), j.optInt("today_registration"), j.optInt("last_week_registration"), j.optInt("month_registration"))
    }

    suspend fun users(query: String = ""): List<UserItem> {
        val q = java.net.URLEncoder.encode(query, "UTF-8")
        val arr = request("GET", "/api/users?query=$q&limit=200").optJSONArray("items") ?: JSONArray()
        return List(arr.length()) { userFrom(arr.getJSONObject(it)) }
    }

    suspend fun userByCard(cardKey: String): UserDetailResult {
        val key = java.net.URLEncoder.encode(cardKey.trim(), "UTF-8")
        val j = request("GET", "/api/users/by-card/$key")
        return if (j.optBoolean("bound")) UserDetailResult(true, userFrom(j.getJSONObject("user"), true), null)
        else UserDetailResult(false, null, cardFrom(j.getJSONObject("card")))
    }

    suspend fun updateUser(userId: Int, username: String?, password: String?, enabled: Boolean?): UserItem {
        val body = JSONObject()
        if (!username.isNullOrBlank()) body.put("username", username.trim())
        if (!password.isNullOrBlank()) body.put("password", password)
        if (enabled != null) body.put("enabled", enabled)
        return userFrom(request("PATCH", "/api/users/$userId", body), true)
    }

    suspend fun cards(query: String = ""): List<CardItem> {
        val q = java.net.URLEncoder.encode(query, "UTF-8")
        val arr = request("GET", "/api/cards?query=$q&limit=200").optJSONArray("items") ?: JSONArray()
        return List(arr.length()) { cardFrom(arr.getJSONObject(it)) }
    }

    suspend fun generateCards(count: Int, note: String): List<CardItem> {
        val j = request("POST", "/api/cards/generate", JSONObject().put("count", count).put("note", note))
        val arr = j.optJSONArray("cards") ?: JSONArray()
        return List(arr.length()) { cardFrom(arr.getJSONObject(it)) }
    }

    suspend fun setCardStatus(id: Int, ban: Boolean): CardItem =
        cardFrom(request("POST", if (ban) "/api/cards/$id/ban" else "/api/cards/$id/unban", JSONObject()))

    suspend fun deleteCard(id: Int) { request("DELETE", "/api/cards/$id") }

    suspend fun hwids(query: String = ""): List<UserItem> {
        val q = java.net.URLEncoder.encode(query, "UTF-8")
        val arr = request("GET", "/api/hwid?query=$q").optJSONArray("items") ?: JSONArray()
        return List(arr.length()) { userFrom(arr.getJSONObject(it)) }
    }

    suspend fun hwidAction(userId: Int, action: String): UserItem =
        userFrom(request("POST", "/api/users/$userId/hwid/$action", JSONObject()))

    suspend fun ipBans(query: String = ""): List<IpBanItem> {
        val q = java.net.URLEncoder.encode(query, "UTF-8")
        val arr = request("GET", "/api/ip?query=$q").optJSONArray("items") ?: JSONArray()
        return List(arr.length()) { idx ->
            val j = arr.getJSONObject(idx)
            IpBanItem(j.optInt("id"), j.optString("ip"), j.optString("note"), j.optBoolean("active"), j.optString("created_at"))
        }
    }

    suspend fun banIp(ip: String, note: String): IpBanItem {
        val j = request("POST", "/api/ip/ban", JSONObject().put("ip", ip.trim()).put("note", note.trim()))
        return IpBanItem(j.optInt("id"), j.optString("ip"), j.optString("note"), j.optBoolean("active"), j.optString("created_at"))
    }

    suspend fun unbanIp(id: Int): IpBanItem {
        val j = request("POST", "/api/ip/$id/unban", JSONObject())
        return IpBanItem(j.optInt("id"), j.optString("ip"), j.optString("note"), j.optBoolean("active"), j.optString("created_at"))
    }

    suspend fun uploadLogs(): UploadLogResult {
        val j = request("GET", "/api/cloud/logs?limit=200")
        val arr = j.optJSONArray("items") ?: JSONArray()
        return UploadLogResult(j.optInt("total_uploads"), List(arr.length()) { uploadFrom(arr.getJSONObject(it)) })
    }

    suspend fun uploadFile(fileName: String, note: String, inputProvider: () -> InputStream): UploadItem = withContext(Dispatchers.IO) {
        val boundary = "----QTY${System.nanoTime()}"
        val conn = connection("/api/cloud/upload", "POST", true).apply {
            doOutput = true
            setChunkedStreamingMode(1024 * 1024)
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        try {
            BufferedOutputStream(conn.outputStream).use { out ->
                fun text(s: String) = out.write(s.toByteArray(Charsets.UTF_8))
                text("--$boundary\r\nContent-Disposition: form-data; name=\"note\"\r\n\r\n$note\r\n")
                val safe = fileName.replace("\"", "_")
                text("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"$safe\"\r\nContent-Type: application/octet-stream\r\n\r\n")
                inputProvider().use { input ->
                    val buf = ByteArray(1024 * 1024)
                    while (true) { val n = input.read(buf); if (n <= 0) break; out.write(buf, 0, n) }
                }
                text("\r\n--$boundary--\r\n")
            }
            uploadFrom(parseOrThrow(conn).getJSONObject("upload"))
        } finally { conn.disconnect() }
    }

    private fun userFrom(j: JSONObject, revealPassword: Boolean = false) = UserItem(
        j.optInt("id"), j.optString("username"), if (revealPassword) nullableString(j, "password") else null,
        nullableString(j, "card_key"), nullableString(j, "hwid"), j.optBoolean("hwid_banned"), nullableString(j, "last_ip"),
        j.optBoolean("enabled", true), j.optString("created_at"), nullableString(j, "last_seen")
    )

    private fun cardFrom(j: JSONObject) = CardItem(
        j.optInt("id"), j.optString("card_key"), j.optString("note"), j.optString("status"), j.optString("created_at"),
        nullableString(j, "used_at"), if (j.has("user_id") && !j.isNull("user_id")) j.optInt("user_id") else null, j.optBoolean("permanent", true)
    )

    private fun uploadFrom(j: JSONObject) = UploadItem(j.optInt("id"), j.optString("original_name"), j.optLong("size_bytes"), j.optString("sha256"), j.optString("note"), j.optString("uploaded_at"))
}

class ApiException(val statusCode: Int, override val message: String) : Exception(message)
data class Dashboard(val userCount: Int = 0, val onlineCount: Int = 0, val todayRegistration: Int = 0, val lastWeekRegistration: Int = 0, val monthRegistration: Int = 0)
data class UserItem(val id: Int, val username: String, val password: String?, val cardKey: String?, val hwid: String?, val hwidBanned: Boolean, val lastIp: String?, val enabled: Boolean, val createdAt: String, val lastSeen: String?)
data class UserDetailResult(val bound: Boolean, val user: UserItem?, val card: CardItem?)
data class CardItem(val id: Int, val key: String, val note: String, val status: String, val createdAt: String, val usedAt: String?, val userId: Int?, val permanent: Boolean)
data class IpBanItem(val id: Int, val ip: String, val note: String, val active: Boolean, val createdAt: String)
data class UploadItem(val id: Int, val fileName: String, val sizeBytes: Long, val sha256: String, val note: String, val uploadedAt: String)
data class UploadLogResult(val total: Int, val items: List<UploadItem>)
