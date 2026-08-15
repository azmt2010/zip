package com.qtyy.admin

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ln
import kotlin.math.pow

private val Bg = Color(0xFF07070A)
private val Surface = Color(0xFF121018)
private val Surface2 = Color(0xFF1A1722)
private val Line = Color(0xFF2D2738)
private val Purple = Color(0xFFB28AFF)
private val PurpleDeep = Color(0xFF7C4DDB)
private val TextMain = Color(0xFFF8F5FF)
private val TextSub = Color(0xFFA9A2B6)
private val Green = Color(0xFF72E8AE)
private val Red = Color(0xFFFF7D8D)
private val Amber = Color(0xFFFFD27A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            QTYTheme {
                val prefs = remember { getSharedPreferences("qty_admin_auth", MODE_PRIVATE) }
                var logged by remember { mutableStateOf(!prefs.getString("token", null).isNullOrBlank()) }
                ApiClient.token = prefs.getString("token", null)
                if (logged) {
                    MainShell {
                        ApiClient.token = null
                        prefs.edit().remove("token").apply()
                        logged = false
                    }
                } else {
                    LoginScreen { token ->
                        prefs.edit().putString("token", token).apply()
                        logged = true
                    }
                }
            }
        }
    }
}

@Composable
private fun QTYTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Purple,
            onPrimary = Color.Black,
            background = Bg,
            surface = Surface,
            onSurface = TextMain,
            outline = Line,
            error = Red,
        ),
        content = content,
    )
}

@Composable
private fun Background(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                listOf(Color(0xFF25133B), Color(0xFF0D0A13), Bg, Bg),
                radius = 1400f,
            )
        )
    ) { content() }
}

@Composable
private fun LoginScreen(onLogin: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val logoAlpha by animateFloatAsState(if (loading) .55f else 1f, label = "logo")

    Background {
        Box(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painterResource(R.drawable.launcher_foreground_art), null,
                    Modifier.size(108.dp).alpha(logoAlpha), contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(12.dp))
                Text("QTY 管理台", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("安全认证 · 永久卡密 · 云端更新", color = TextSub, fontSize = 13.sp)
                Spacer(Modifier.height(28.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(Purple))
                            Spacer(Modifier.width(8.dp))
                            Text(ApiClient.BASE_URL.removePrefix("https://"), color = TextSub, fontSize = 12.sp)
                            Spacer(Modifier.weight(1f))
                            Text("TLS", color = Green, fontSize = 11.sp)
                        }
                        AdminField(username, { username = it }, "管理员账号", singleLine = true)
                        AdminField(
                            password, { password = it }, "管理员密码", singleLine = true,
                            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                            trailing = {
                                IconButton({ showPass = !showPass }) {
                                    Icon(if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null)
                                }
                            },
                        )
                        error?.let { Text(it, color = Red, fontSize = 12.sp) }
                        Button(
                            onClick = {
                                if (username.isBlank() || password.isBlank()) { error = "请输入账号和密码"; return@Button }
                                loading = true; error = null
                                scope.launch {
                                    runCatching { ApiClient.login(username.trim(), password) }
                                        .onSuccess(onLogin)
                                        .onFailure { error = friendlyError(it) }
                                    loading = false
                                }
                            },
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Color.Black),
                        ) {
                            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.Black)
                            else { Icon(Icons.Rounded.Shield, null); Spacer(Modifier.width(8.dp)); Text("进入管理台", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}

private data class Tab(val title: String, val icon: ImageVector)

@Composable
private fun MainShell(onLogout: () -> Unit) {
    val tabs = remember {
        listOf(
            Tab("用户", Icons.Rounded.Person),
            Tab("卡密", Icons.Rounded.Key),
            Tab("云端更新", Icons.Rounded.CloudUpload),
            Tab("HWID", Icons.Rounded.Devices),
        )
    }
    var tab by remember { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }

    Background {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(painterResource(R.drawable.launcher_foreground_art), null, Modifier.size(42.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("QTY 管理台", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("45.116.78.241", color = TextSub, fontSize = 10.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onLogout) { Icon(Icons.Rounded.Logout, "退出", tint = TextSub) }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Color(0xF2121018), modifier = Modifier.navigationBarsPadding()) {
                    tabs.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Icon(item.icon, null) },
                            label = { Text(item.title, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Purple,
                                selectedTextColor = Purple,
                                indicatorColor = Color(0xFF2B203C),
                                unselectedIconColor = TextSub,
                                unselectedTextColor = TextSub,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            AnimatedContent(tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tab") { page ->
                when (page) {
                    0 -> UsersScreen(snackbar, Modifier.padding(padding))
                    1 -> CardsScreen(snackbar, Modifier.padding(padding))
                    2 -> CloudScreen(snackbar, Modifier.padding(padding))
                    else -> HwidScreen(snackbar, Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun UsersScreen(snackbar: SnackbarHostState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var dashboard by remember { mutableStateOf(Dashboard()) }
    var users by remember { mutableStateOf(emptyList<UserItem>()) }
    var query by remember { mutableStateOf("") }
    var cardQuery by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<UserItem?>(null) }
    var loading by remember { mutableStateOf(true) }
    var ipQuery by remember { mutableStateOf("") }
    var ipNote by remember { mutableStateOf("") }
    var ipBans by remember { mutableStateOf(emptyList<IpBanItem>()) }

    suspend fun refresh(showMessage: Boolean = false) {
        runCatching {
            dashboard = ApiClient.dashboard()
            users = ApiClient.users(query)
            ipBans = ApiClient.ipBans()
        }.onFailure { snackbar.showSnackbar(friendlyError(it)) }
        loading = false
        if (showMessage) snackbar.showSnackbar("已刷新")
    }
    LaunchedEffect(Unit) { refresh() }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("总预览", "实时查看账户与在线状态")
                Spacer(Modifier.weight(1f))
                IconButton({ scope.launch { refresh(true) } }) { Icon(Icons.Rounded.Refresh, null, tint = Purple) }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Metric("用户数量", dashboard.userCount.toString(), Icons.Rounded.Person) }
                item { Metric("在线数量", dashboard.onlineCount.toString(), Icons.Rounded.Bolt) }
                item { Metric("今日注册", dashboard.todayRegistration.toString(), Icons.Rounded.Today) }
                item { Metric("上周注册", dashboard.lastWeekRegistration.toString(), Icons.Rounded.DateRange) }
                item { Metric("今月注册", dashboard.monthRegistration.toString(), Icons.Rounded.CalendarMonth) }
            }
        }
        item { SectionTitle("用户管理", "搜索账号 / 卡密 / HWID / IP") }
        item {
            SearchField(query, { query = it }, "搜索用户") { scope.launch { loading = true; refresh() } }
        }
        if (loading) item { Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Purple) } }
        else if (users.isEmpty()) item { Empty("暂无用户") }
        else items(users, key = { it.id }) { user -> UserRow(user) { detail = user } }

        item { Spacer(Modifier.height(4.dp)); SectionTitle("卡密查账号", "输入卡密显示绑定账号和密码") }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminField(cardQuery, { cardQuery = it.uppercase() }, "输入卡密", singleLine = true)
                    Button(
                        { scope.launch {
                            if (cardQuery.isBlank()) return@launch
                            runCatching { ApiClient.userByCard(cardQuery) }
                                .onSuccess { r ->
                                    if (r.bound) detail = r.user else snackbar.showSnackbar("该卡密尚未绑定账号")
                                }.onFailure { snackbar.showSnackbar(friendlyError(it)) }
                        } },
                        Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                    ) { Icon(Icons.Rounded.Search, null); Spacer(Modifier.width(6.dp)); Text("查询") }
                }
            }
        }

        item { Spacer(Modifier.height(4.dp)); SectionTitle("IP 管理", "封禁或解除访问 IP") }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminField(ipQuery, { ipQuery = it }, "IP 地址", singleLine = true, keyboardType = KeyboardType.Uri)
                    AdminField(ipNote, { ipNote = it }, "备注", singleLine = true)
                    Button(
                        { scope.launch {
                            if (ipQuery.isBlank()) return@launch
                            runAction(snackbar) { ApiClient.banIp(ipQuery, ipNote); ipQuery = ""; ipNote = ""; ipBans = ApiClient.ipBans() }
                        } },
                        Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Red, contentColor = Color.Black),
                        shape = RoundedCornerShape(14.dp),
                    ) { Icon(Icons.Rounded.Block, null); Spacer(Modifier.width(6.dp)); Text("封禁 IP") }
                }
            }
        }
        if (ipBans.isNotEmpty()) items(ipBans, key = { "ip${it.id}" }) { item ->
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.ip, fontWeight = FontWeight.SemiBold)
                        Text(item.note.ifBlank { "无备注" }, color = TextSub, fontSize = 11.sp)
                    }
                    MiniAction("解禁", Icons.Rounded.LockOpen, Green) {
                        scope.launch { runAction(snackbar) { ApiClient.unbanIp(item.id); ipBans = ApiClient.ipBans() } }
                    }
                }
            }
        }
    }

    detail?.let { selected ->
        var username by remember(selected.id, selected.username) { mutableStateOf(selected.username) }
        var password by remember(selected.id) { mutableStateOf(selected.password.orEmpty()) }
        var enabled by remember(selected.id) { mutableStateOf(selected.enabled) }
        var revealing by remember(selected.id) { mutableStateOf(selected.password == null) }
        LaunchedEffect(selected.id) {
            if (selected.password == null && !selected.cardKey.isNullOrBlank()) {
                runCatching { ApiClient.userByCard(selected.cardKey) }.onSuccess { result ->
                    result.user?.let { u -> username = u.username; password = u.password.orEmpty(); enabled = u.enabled; revealing = false }
                }.onFailure { revealing = false }
            } else revealing = false
        }
        AlertDialog(
            onDismissRequest = { detail = null },
            containerColor = Surface2,
            title = { Text("编辑用户") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Detail("卡密", selected.cardKey ?: "-")
                    Detail("HWID", selected.hwid ?: "未绑定")
                    Detail("最后 IP", selected.lastIp ?: "-")
                    AdminField(username, { username = it }, "账号", singleLine = true)
                    AdminField(password, { password = it }, if (revealing) "读取密码中…" else "密码", singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("账号可用", Modifier.weight(1f)); Switch(enabled, { enabled = it })
                    }
                }
            },
            confirmButton = {
                TextButton({ scope.launch {
                    runCatching { ApiClient.updateUser(selected.id, username, password.takeIf { it.isNotBlank() }, enabled) }
                        .onSuccess { detail = null; refresh(); snackbar.showSnackbar("用户资料已更新") }
                        .onFailure { snackbar.showSnackbar(friendlyError(it)) }
                } }) { Text("保存", color = Purple) }
            },
            dismissButton = { TextButton({ detail = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun CardsScreen(snackbar: SnackbarHostState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var count by remember { mutableStateOf("1") }
    var note by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var cards by remember { mutableStateOf(emptyList<CardItem>()) }
    var generated by remember { mutableStateOf(emptyList<CardItem>()) }
    var loading by remember { mutableStateOf(true) }

    suspend fun refresh() {
        runCatching { cards = ApiClient.cards(query) }.onFailure { snackbar.showSnackbar(friendlyError(it)) }
        loading = false
    }
    LaunchedEffect(Unit) { refresh() }

    LazyColumn(
        modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionTitle("卡密生成", "生成卡密默认为永久有效") }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminField(count, { count = it.filter(Char::isDigit).take(3) }, "生成数量", Modifier.weight(.42f), true, KeyboardType.Number)
                        AdminField(note, { note = it }, "生成备注", Modifier.weight(.58f), true)
                    }
                    Button(
                        { scope.launch {
                            val n = count.toIntOrNull()?.coerceIn(1, 500) ?: 1
                            runCatching { ApiClient.generateCards(n, note) }
                                .onSuccess { generated = it; refresh(); snackbar.showSnackbar("已生成 ${it.size} 张永久卡密") }
                                .onFailure { snackbar.showSnackbar(friendlyError(it)) }
                        } },
                        Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                    ) { Icon(Icons.Rounded.AutoAwesome, null); Spacer(Modifier.width(7.dp)); Text("生成永久卡密") }
                }
            }
        }
        if (generated.isNotEmpty()) {
            item { Text("本次生成", color = TextSub, fontSize = 12.sp) }
            items(generated, key = { "g${it.id}" }) { CardRow(it, {}, {}) }
        }
        item { Spacer(Modifier.height(2.dp)); SectionTitle("卡密管理", "搜索、封禁、解禁与删除") }
        item { SearchField(query, { query = it.uppercase() }, "搜索卡密 / 备注") { scope.launch { loading = true; refresh() } } }
        if (loading) item { Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Purple) } }
        else if (cards.isEmpty()) item { Empty("暂无卡密") }
        else items(cards, key = { it.id }) { card ->
            CardRow(
                card,
                onBan = { scope.launch { runAction(snackbar) { ApiClient.setCardStatus(card.id, card.status != "banned"); refresh() } } },
                onDelete = { scope.launch { runAction(snackbar) { ApiClient.deleteCard(card.id); refresh() } } },
            )
        }
    }
}

@Composable
private fun CloudScreen(snackbar: SnackbarHostState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var fileMeta by remember { mutableStateOf<FileMeta?>(null) }
    var note by remember { mutableStateOf("") }
    var logs by remember { mutableStateOf(UploadLogResult(0, emptyList())) }
    var uploading by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        fileUri = uri
        fileMeta = uri?.let { queryFileMeta(context, it) }
    }
    suspend fun refresh() { runCatching { logs = ApiClient.uploadLogs() }.onFailure { snackbar.showSnackbar(friendlyError(it)) } }
    LaunchedEffect(Unit) { refresh() }

    LazyColumn(
        modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionTitle("DLL 云端更新", "上传文件后客户端可从服务器获取最新版本") }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton({ picker.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Rounded.FileOpen, null); Spacer(Modifier.width(7.dp)); Text("选择上传文件")
                    }
                    fileMeta?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Description, null, tint = Purple)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(it.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                Text(formatBytes(it.size), color = TextSub, fontSize = 11.sp)
                            }
                        }
                    }
                    AdminField(note, { note = it }, "更新备注", singleLine = true)
                    Button(
                        { if (fileUri != null) confirm = true }, enabled = fileUri != null && !uploading,
                        modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                    ) {
                        if (uploading) CircularProgressIndicator(Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                        else { Icon(Icons.Rounded.CloudUpload, null); Spacer(Modifier.width(7.dp)); Text("确认上传") }
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("上传日志", "累计上传 ${logs.total} 次")
                Spacer(Modifier.weight(1f))
                IconButton({ scope.launch { refresh() } }) { Icon(Icons.Rounded.Refresh, null, tint = Purple) }
            }
        }
        if (logs.items.isEmpty()) item { Empty("暂无上传记录") }
        else items(logs.items, key = { it.id }) { UploadRow(it) }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false }, containerColor = Surface2,
            title = { Text("确认上传") },
            text = { Text("将 ${fileMeta?.name ?: "文件"} 上传到云端并设为最新版本？", color = TextSub) },
            confirmButton = {
                TextButton({
                    val uri = fileUri ?: return@TextButton
                    val meta = fileMeta ?: return@TextButton
                    confirm = false; uploading = true
                    scope.launch {
                        runCatching { ApiClient.uploadFile(meta.name, note) { context.contentResolver.openInputStream(uri) ?: error("无法读取文件") } }
                            .onSuccess { fileUri = null; fileMeta = null; note = ""; refresh(); snackbar.showSnackbar("上传成功") }
                            .onFailure { snackbar.showSnackbar(friendlyError(it)) }
                        uploading = false
                    }
                }) { Text("上传", color = Purple) }
            },
            dismissButton = { TextButton({ confirm = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun HwidScreen(snackbar: SnackbarHostState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var users by remember { mutableStateOf(emptyList<UserItem>()) }
    var loading by remember { mutableStateOf(true) }
    suspend fun refresh() {
        runCatching { users = ApiClient.hwids(query) }.onFailure { snackbar.showSnackbar(friendlyError(it)) }
        loading = false
    }
    LaunchedEffect(Unit) { refresh() }

    LazyColumn(
        modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionTitle("HWID 管理", "解绑、封禁或解禁设备标识") }
        item { SearchField(query, { query = it }, "搜索账号 / HWID") { scope.launch { loading = true; refresh() } } }
        if (loading) item { Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Purple) } }
        else if (users.isEmpty()) item { Empty("暂无 HWID 记录") }
        else items(users, key = { it.id }) { user ->
            GlassCard(Modifier.fillMaxWidth().animateContentSize()) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFF241A34)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Devices, null, tint = Purple)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(user.username, fontWeight = FontWeight.SemiBold)
                            Text(user.hwid ?: "未绑定", color = TextSub, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Status(if (user.hwidBanned) "已封禁" else "正常", if (user.hwidBanned) Red else Green)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniAction("解绑", Icons.Rounded.LinkOff, TextSub) { scope.launch { runAction(snackbar) { ApiClient.hwidAction(user.id, "unbind"); refresh() } } }
                        if (user.hwidBanned) MiniAction("解禁", Icons.Rounded.CheckCircle, Green) { scope.launch { runAction(snackbar) { ApiClient.hwidAction(user.id, "unban"); refresh() } } }
                        else MiniAction("封禁", Icons.Rounded.Block, Red) { scope.launch { runAction(snackbar) { ApiClient.hwidAction(user.id, "ban"); refresh() } } }
                    }
                }
            }
        }
    }
}

private suspend fun runAction(snackbar: SnackbarHostState, block: suspend () -> Unit) {
    runCatching { block() }.onSuccess { snackbar.showSnackbar("操作完成") }.onFailure { snackbar.showSnackbar(friendlyError(it)) }
}

@Composable
private fun Metric(label: String, value: String, icon: ImageVector) {
    GlassCard(Modifier.width(134.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF281E37)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Purple, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp)); Text(label, color = TextSub, fontSize = 11.sp)
            }
            Spacer(Modifier.height(10.dp)); Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun UserRow(user: UserItem, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF241A34)), contentAlignment = Alignment.Center) {
                Text(user.username.take(1).uppercase(), color = Purple, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.username, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(8.dp)); Status(if (user.enabled) "正常" else "停用", if (user.enabled) Green else Red)
                }
                Text(user.cardKey ?: "无卡密", color = TextSub, fontSize = 12.sp, maxLines = 1)
                Text("IP ${user.lastIp ?: "-"} · ${formatTime(user.lastSeen)}", color = TextSub, fontSize = 11.sp, maxLines = 1)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = TextSub)
        }
    }
}

@Composable
private fun CardRow(card: CardItem, onBan: () -> Unit, onDelete: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().animateContentSize()) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Key, null, tint = Purple); Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(card.key, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(card.note.ifBlank { "无备注" }, color = TextSub, fontSize = 11.sp)
                }
                val color = when (card.status) { "banned" -> Red; "used" -> Amber; else -> Green }
                val label = when (card.status) { "banned" -> "封禁"; "used" -> "已使用"; else -> "未使用" }
                Status(label, color)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                MiniAction(if (card.status == "banned") "解禁" else "封禁", if (card.status == "banned") Icons.Rounded.CheckCircle else Icons.Rounded.Block, if (card.status == "banned") Green else Red, onBan)
                MiniAction("删除", Icons.Rounded.DeleteOutline, TextSub, onDelete)
                Spacer(Modifier.weight(1f)); Text("永久", color = Purple, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun UploadRow(item: UploadItem) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFF241A34)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.CloudUpload, null, tint = Purple, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.fileName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${formatBytes(item.sizeBytes)} · ${formatTime(item.uploadedAt)}", color = TextSub, fontSize = 12.sp)
                Text("SHA256 ${item.sha256.take(12)}…", color = TextSub, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, hint: String, onSearch: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        AdminField(value, onValueChange, hint, Modifier.weight(1f), true)
        IconButton(onSearch, Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF291D3B))) {
            Icon(Icons.Rounded.Search, null, tint = Purple)
        }
    }
}

@Composable
private fun MiniAction(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    OutlinedButton(
        onClick, contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp), shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color), border = BorderStroke(1.dp, color.copy(alpha = .35f)),
    ) { Icon(icon, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text(label, fontSize = 12.sp) }
}

@Composable
private fun Status(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(99.dp)).background(color.copy(alpha = .13f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, color = TextSub, fontSize = 12.sp, modifier = Modifier.width(62.dp))
        Text(value, color = TextMain, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp); Text(subtitle, color = TextSub, fontSize = 11.sp) }
}

@Composable
private fun Empty(text: String) {
    GlassCard(Modifier.fillMaxWidth()) { Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) { Text(text, color = TextSub, fontSize = 13.sp) } }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier, colors = CardDefaults.cardColors(containerColor = Surface.copy(alpha = .94f)),
        shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Line.copy(alpha = .9f)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) { content() }
}

@Composable
private fun AdminField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value, onValueChange, { Text(label) }, modifier.fillMaxWidth(), singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType), visualTransformation = visualTransformation,
        trailingIcon = trailing, shape = RoundedCornerShape(15.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Purple, unfocusedBorderColor = Line, focusedLabelColor = Purple,
            unfocusedLabelColor = TextSub, cursorColor = Purple, focusedTextColor = TextMain, unfocusedTextColor = TextMain,
        ),
    )
}

private data class FileMeta(val name: String, val size: Long)

private fun queryFileMeta(context: Context, uri: Uri): FileMeta {
    var name = "update.bin"; var size = 0L
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME); val si = c.getColumnIndex(OpenableColumns.SIZE)
            if (ni >= 0) name = c.getString(ni) ?: name
            if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
        }
    }
    return FileMeta(name, size)
}

private fun friendlyError(t: Throwable): String = when (t) {
    is ApiException -> t.message
    else -> t.message?.takeIf { it.isNotBlank() } ?: "网络连接失败，请检查服务器状态"
}

private fun formatTime(value: String?): String {
    if (value.isNullOrBlank()) return "未在线"
    return runCatching {
        OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
    }.getOrElse { value.replace('T', ' ').take(16) }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val i = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.lastIndex)
    val v = bytes / 1024.0.pow(i.toDouble())
    return if (i == 0) "$bytes B" else String.format("%.2f %s", v, units[i])
}
