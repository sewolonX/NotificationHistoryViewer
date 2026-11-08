package app.sewolon.notificationhistoryviewer

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Xml
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.StringReader
import androidx.core.graphics.drawable.toBitmap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread
import kotlinx.coroutines.CancellationException
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SearchBarDefaults
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.icon.basic.ArrowUpDown
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.icon.basic.SearchCleanup
import top.yukonga.miuix.kmp.icon.basic.Close
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur

class MainActivity : ComponentActivity() {
    private data class AppInfo(val name: String, val icon: ImageBitmap?)
    private data class EntryKey(val time: Long, val pkg: String, val title: String, val text: String)
    private data class NotificationGroup(val entries: List<HistoryReader.Entry>)
    private data class SearchRecord(val entry: HistoryReader.Entry, val fields: List<String>)

    private var entries by mutableStateOf<List<HistoryReader.Entry>>(emptyList())
    private var searchRecords by mutableStateOf<List<SearchRecord>>(emptyList())
    private val apps = mutableStateMapOf<String, AppInfo>()
    private var busy by mutableStateOf(false)
    private var status by mutableStateOf("")
    private var query by mutableStateOf("")
    private var newBackupCount by mutableIntStateOf(0)
    private var searchScopes by mutableIntStateOf(0x3ff)
    private var useRegex by mutableStateOf(false)
    private var ignoreSeparators by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("search_settings", MODE_PRIVATE)
        if (!preferences.contains("package_migration_done")) {
            try {
                val xml = String(HistoryReader.rootRead(
                    "cat /data/user/0/com.example.notificationhistoryviewer/shared_prefs/search_settings.xml"
                ), Charsets.UTF_8)
                val parser = Xml.newPullParser().apply { setInput(StringReader(xml)) }
                val editor = preferences.edit()
                while (parser.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType != org.xmlpull.v1.XmlPullParser.START_TAG) continue
                    when (parser.getAttributeValue(null, "name")) {
                        "query" -> editor.putString("query", parser.nextText())
                        "scopes" -> parser.getAttributeValue(null, "value")?.toIntOrNull()?.let { editor.putInt("scopes", it) }
                        "regex" -> editor.putBoolean("regex", parser.getAttributeValue(null, "value") == "true")
                        "separators" -> editor.putBoolean("separators", parser.getAttributeValue(null, "value") == "true")
                    }
                }
                editor.putBoolean("package_migration_done", true).commit()
            } catch (_: Exception) {
                // A fresh installation has no old package data to migrate.
            }
        }
        query = preferences.getString("query", "").orEmpty()
        searchScopes = preferences.getInt("scopes", 0x3ff)
        useRegex = preferences.getBoolean("regex", false)
        ignoreSeparators = preferences.getBoolean("separators", false)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        setContent {
            val controller = remember { ThemeController(ColorSchemeMode.System) }
            MiuixTheme(controller = controller) {
                val dark = isSystemInDarkTheme()
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                }
                Screen()
            }
        }
        refresh()
    }

    private fun refresh() {
        if (busy) return
        busy = true
        status = getString(R.string.prepare_history)
        thread {
            try {
                runOnUiThread { status = getString(R.string.check_backup) }
                val backupDirectory = File(getExternalFilesDir(null) ?: throw IllegalStateException(getString(R.string.storage_unavailable)), "notification_history")
                if (!backupDirectory.isDirectory && !backupDirectory.mkdirs()) throw IllegalStateException(getString(R.string.backup_directory_error))
                runOnUiThread { status = getString(R.string.scan_system_files) }
                val nativeNames = String(HistoryReader.rootRead("ls -1 ${HistoryReader.DIR}"))
                    .split(Regex("\\s+"))
                    .filter { it.matches(Regex("[0-9]{10,20}")) }
                    .toSet()
                var backupCount = 0
                var backupFailures = 0
                nativeNames.forEachIndexed { index, name ->
                    try {
                        if (HistoryReader.backupIfMissing(name, backupDirectory)) backupCount++
                    } catch (_: Exception) {
                        backupFailures++
                    }
                    if (index % 5 == 0 || index == nativeNames.size - 1) {
                        runOnUiThread { status = getString(R.string.backup_progress, index + 1, nativeNames.size) }
                    }
                }
                val names = (nativeNames + backupDirectory.listFiles().orEmpty()
                    .filter { it.isFile && it.name.matches(Regex("[0-9]{10,20}")) }
                    .map { it.name }).sortedDescending()
                val loaded = ArrayList<HistoryReader.Entry>()
                var failures = 0
                names.forEachIndexed { index, name ->
                    try {
                        val backup = File(backupDirectory, name)
                        val bytes = if (backup.isFile) backup.readBytes()
                        else HistoryReader.rootRead("cat ${HistoryReader.DIR}$name")
                        val parsed = HistoryReader.parse(bytes)
                        val systemPath = HistoryReader.DIR + name
                        parsed.forEach { entry ->
                            entry.source = if (backup.isFile) "$systemPath\n${backup.absolutePath}" else systemPath
                        }
                        loaded.addAll(parsed)
                    } catch (_: Exception) {
                        failures++
                    }
                    if (index % 5 == 0 || index == names.lastIndex) {
                        runOnUiThread { status = getString(R.string.parse_progress, index + 1, names.size) }
                    }
                }
                runOnUiThread { status = getString(R.string.read_system_memory) }
                var systemError: String? = null
                try {
                    loaded.addAll(HistoryReader.readSystemHistory(applicationInfo.sourceDir))
                } catch (e: Exception) {
                    systemError = e.message ?: e.toString()
                }
                runOnUiThread { status = getString(R.string.merge_records) }
                val unique = loaded.distinctBy { EntryKey(it.time, it.pkg, it.title, it.text) }
                    .sortedByDescending { it.time }
                val packages = unique.map { it.pkg }.distinct()
                val info = HashMap<String, AppInfo>()
                packages.forEachIndexed { index, packageName ->
                    info[packageName] = loadAppInfo(packageName)
                    if (index % 10 == 0 || index == packages.lastIndex) {
                        runOnUiThread { status = getString(R.string.app_info_progress, index + 1, packages.size) }
                    }
                }
                val indexed = indexEntries(unique, info)
                runOnUiThread {
                    entries = unique
                    searchRecords = indexed
                    newBackupCount = backupCount
                    apps.clear()
                    apps.putAll(info)
                    busy = false
                    status = getString(R.string.result_summary, unique.size, backupCount) +
                        (if (backupFailures > 0) getString(R.string.backup_failures, backupFailures) else "") +
                        (if (failures > 0) getString(R.string.read_failures, failures) else "") +
                        (if (systemError != null) getString(R.string.memory_failure) else "")
                }
            } catch (e: Exception) {
                runOnUiThread { busy = false; status = getString(R.string.read_error, e.message ?: e.toString()) }
            }
        }
    }

    private fun loadAppInfo(packageName: String): AppInfo {
        return try {
            val application = packageManager.getApplicationInfo(packageName, 0)
            val name = packageManager.getApplicationLabel(application).toString().ifBlank { packageName }
            val icon = packageManager.getApplicationIcon(application).toBitmap(96, 96).asImageBitmap()
            AppInfo(name, icon)
        } catch (_: PackageManager.NameNotFoundException) {
            AppInfo(packageName, null)
        } catch (_: Exception) {
            AppInfo(packageName, null)
        }
    }

    private fun loadNotificationIcon(entry: HistoryReader.Entry): ImageBitmap? = try {
        when (entry.iconType) {
            2 -> packageManager.getApplicationIcon(entry.iconPackage.ifEmpty { entry.pkg })
                .let { drawable ->
                    val icon = android.graphics.drawable.Icon.createWithResource(
                        entry.iconPackage.ifEmpty { entry.pkg }, entry.iconResourceId
                    )
                    icon.loadDrawable(this)?.toBitmap(96, 96)?.asImageBitmap()
                        ?: drawable.toBitmap(96, 96).asImageBitmap()
                }
            3 -> entry.iconData?.let { android.graphics.drawable.Icon.createWithData(it, 0, it.size) }
                ?.loadDrawable(this)?.toBitmap(96, 96)?.asImageBitmap()
            5 -> entry.iconData?.let { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) }
                ?.asImageBitmap()
            else -> null
        }
    } catch (_: Exception) { null }

    private fun groupTimeRange(group: List<HistoryReader.Entry>): String {
        val format = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
        val start = format.format(Date(group.last().time))
        val end = format.format(Date(group.first().time))
        val common = start.commonPrefixWith(end)
        val cut = common.indexOfLast { it == ' ' || it == ':' }.takeIf { it >= 0 }?.plus(1) ?: 0
        return "$start ~ ${end.drop(cut)}"
    }

    private fun saveSearch() {
        getSharedPreferences("search_settings", MODE_PRIVATE).edit()
            .putString("query", query).putInt("scopes", searchScopes)
            .putBoolean("regex", useRegex).putBoolean("separators", ignoreSeparators).apply()
    }

    private val separators = Regex("[\\s\\u3000_\\-:./()（）：。／－＿~|～｜]+")

    private fun indexEntries(data: List<HistoryReader.Entry>, names: Map<String, AppInfo>): List<SearchRecord> {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return data.map { entry -> SearchRecord(entry, listOf(
            names[entry.pkg]?.name.orEmpty(), entry.pkg, entry.title, entry.text,
            format.format(Date(entry.time)), entry.userId.takeIf { it >= 0 }?.toString().orEmpty(),
            entry.channelName, entry.channelId, entry.conversationId, entry.source
        )) }
    }

    @Composable
    private fun NotificationRow(
        entry: HistoryReader.Entry,
        app: AppInfo?,
        dateFormat: SimpleDateFormat,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        val appName = app?.name ?: entry.pkg
        val appIconDescription = stringResource(R.string.a11y_app_icon, appName)
        val detailsAction = stringResource(R.string.view_notification_details)
        Column(modifier) {
            Row(
                Modifier.fillMaxWidth().clickable(
                    onClickLabel = detailsAction,
                    role = Role.Button,
                    onClick = onClick
                )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (app?.icon != null) {
                    Image(app.icon, contentDescription = appIconDescription, modifier = Modifier.size(38.dp).clip(RoundedCornerShape(9.dp)))
                } else {
                    Box(Modifier.size(38.dp).semantics { contentDescription = appIconDescription }, contentAlignment = Alignment.Center) {
                        Text(appName.take(1))
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(app?.name ?: entry.pkg, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(6.dp))
                        Text(dateFormat.format(Date(entry.time)), style = MiuixTheme.textStyles.body2)
                    }
                    Text(entry.pkg, style = MiuixTheme.textStyles.body2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (entry.title.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(entry.title)
                    }
                    if (entry.text.isNotBlank()) {
                        Text(entry.text, style = MiuixTheme.textStyles.body2)
                    }
                }
            }
            HorizontalDivider()
        }
    }

    @Composable
    private fun Screen() {
        var selectedEntry by remember { mutableStateOf<HistoryReader.Entry?>(null) }
        var sheetOpen by remember { mutableStateOf(false) }
        var scopeOpen by remember { mutableStateOf(false) }
        var searchExpanded by remember { mutableStateOf(false) }
        val searchInteraction = remember { MutableInteractionSource() }
        val searchFocused by searchInteraction.collectIsFocusedAsState()
        val focusManager = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        val dismissSearchFocus = {
            focusManager.clearFocus()
            keyboard?.hide()
        }
        val listFocusDismiss = remember(focusManager, keyboard) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (source == NestedScrollSource.UserInput) dismissSearchFocus()
                    return Offset.Zero
                }
            }
        }
        val search = query.trim()
        var filtered by remember { mutableStateOf<List<HistoryReader.Entry>>(emptyList()) }
        LaunchedEffect(searchRecords, search, searchScopes, useRegex, ignoreSeparators) {
            if (search.isNotEmpty()) delay(120)
            val records = searchRecords
            val scopeMask = searchScopes
            val regexMode = useRegex
            val removeSeparators = ignoreSeparators
            filtered = withContext(Dispatchers.Default) {
                if (search.isEmpty()) records.map { it.entry } else {
                    val normalizedTerm = if (removeSeparators) search.replace(separators, "") else search
                    val pattern = if (regexMode) runCatching { Regex(search, RegexOption.IGNORE_CASE) }.getOrNull() else null
                    val plainRegex = regexMode && normalizedTerm.none { it in "\\[]{}+*?^$|.()" }
                    records.asSequence().filter { record ->
                        record.fields.indices.any { index ->
                            if (scopeMask and (1 shl index) == 0) false else {
                                val raw = record.fields[index]
                                if (regexMode && pattern?.containsMatchIn(raw) == true) true
                                else if (removeSeparators && (!regexMode || plainRegex))
                                    raw.replace(separators, "").contains(normalizedTerm, ignoreCase = true)
                                else if (regexMode) false
                                else raw.contains(search, ignoreCase = true)
                            }
                        }
                    }.map { it.entry }.toList()
                }
            }
        }
        val groups = remember(filtered) {
            filtered.fold(mutableListOf<MutableList<HistoryReader.Entry>>()) { result, entry ->
                if (result.lastOrNull()?.lastOrNull()?.pkg == entry.pkg) result.last().add(entry)
                else result.add(mutableListOf(entry))
                result
            }.map { NotificationGroup(it.toList()) }
        }
        val expandedGroups = remember { mutableStateMapOf<EntryKey, Boolean>() }
        val revealedRemainderGroups = remember { mutableStateMapOf<EntryKey, Boolean>() }
        val listState = rememberLazyListState()
        val pullToRefreshState = rememberPullToRefreshState()
        val scopeLabels = listOf(R.string.scope_app, R.string.detail_package, R.string.detail_title,
            R.string.detail_content, R.string.detail_time, R.string.detail_user_id,
            R.string.detail_channel_name, R.string.detail_channel_id,
            R.string.detail_conversation_id, R.string.detail_source)
        val sourceCounts = remember(entries) { entries.groupingBy { it.source }.eachCount() }
        val coroutineScope = rememberCoroutineScope()
        val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
        val scrollBehavior = MiuixScrollBehavior()
        val density = LocalDensity.current
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        var barHeight by remember { mutableStateOf(200.dp) }
        val blurEnabled = isRuntimeShaderSupported()
        val topColor = if (isSystemInDarkTheme()) Color.Black else MiuixTheme.colorScheme.background
        val backdrop = if (blurEnabled) rememberLayerBackdrop {
            drawRect(topColor)
            drawContent()
        } else null
        Scaffold(containerColor = topColor, contentWindowInsets = WindowInsets(0)) {
        Box(
            Modifier.fillMaxSize().background(topColor).imePadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusManager.clearFocus(); keyboard?.hide() }
        ) {
            PullToRefresh(
                isRefreshing = busy,
                onRefresh = { dismissSearchFocus(); refresh() },
                pullToRefreshState = pullToRefreshState,
                contentPadding = PaddingValues(top = barHeight),
                refreshTexts = listOf(
                    stringResource(R.string.refresh_pulling),
                    stringResource(R.string.refresh_release),
                    stringResource(R.string.refresh_refreshing),
                    stringResource(R.string.refresh_complete)
                )
            ) {
                LazyColumn(
                    Modifier.fillMaxSize()
                        .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                        .nestedScroll(listFocusDismiss)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    state = listState,
                    contentPadding = PaddingValues(top = barHeight, bottom = 32.dp)
                ) {
                    if (groups.isNotEmpty()) item { HorizontalDivider() }
                    if (!busy && filtered.isEmpty()) {
                        item { Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                            Text(if (entries.isEmpty()) status else stringResource(R.string.empty_search))
                        } }
                    } else {
                        groups.forEach { group ->
                            val first = group.entries.first()
                            val groupKey = EntryKey(first.time, first.pkg, first.title, first.text)
                            if (group.entries.size >= 3) {
                                item(key = "group:$groupKey", contentType = "group") {
                                    val expanded = expandedGroups[groupKey] == true
                                    var turns by remember(groupKey) { mutableIntStateOf(0) }
                                    val rotation by animateFloatAsState(
                                        targetValue = turns * 360f,
                                        animationSpec = tween(durationMillis = 400),
                                        label = "expand rotation"
                                    )
                                    Row(
                                        Modifier.fillMaxWidth().clickable(
                                            onClickLabel = stringResource(if (expanded) R.string.collapse else R.string.expand),
                                            role = Role.Button
                                        ) {
                                            expandedGroups[groupKey] = !expanded
                                            if (expanded) revealedRemainderGroups[groupKey] = false
                                            else coroutineScope.launch {
                                                delay(350)
                                                if (expandedGroups[groupKey] == true) revealedRemainderGroups[groupKey] = true
                                            }
                                            turns++
                                        }.padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SmallTitle(
                                            text = "${apps[first.pkg]?.name ?: first.pkg} (${group.entries.size})",
                                            modifier = Modifier.weight(1f),
                                            insideMargin = PaddingValues(0.dp)
                                        )
                                        SmallTitle(text = groupTimeRange(group.entries), insideMargin = PaddingValues(0.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            imageVector = if (expanded) MiuixIcons.ExpandLess else MiuixIcons.ExpandMore,
                                            contentDescription = stringResource(if (expanded) R.string.collapse else R.string.expand),
                                            modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = rotation }
                                        )
                                    }
                                    HorizontalDivider()
                                }
                            }
                            if (group.entries.size >= 3) {
                                // Only the first screenful participates in the height animation.
                                // The remaining notifications stay as lazy items below it.
                                item(key = "preview:$groupKey", contentType = "preview") {
                                    AnimatedVisibility(
                                        visible = expandedGroups[groupKey] == true,
                                        enter = expandVertically(expandFrom = Alignment.Top, animationSpec = tween(350)),
                                        exit = shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(350))
                                    ) {
                                        Column {
                                            group.entries.take(8).forEach { entry ->
                                                NotificationRow(entry, apps[entry.pkg], dateFormat) {
                                                    focusManager.clearFocus()
                                                    keyboard?.hide()
                                                    selectedEntry = entry
                                                    sheetOpen = true
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            val lazyEntries = if (group.entries.size < 3) group.entries
                                else if (revealedRemainderGroups[groupKey] == true) group.entries.drop(8)
                                else emptyList()
                            items(
                                items = lazyEntries,
                                key = { entry -> "entry:${entry.time}:${entry.pkg}:${entry.title}:${entry.text}" },
                                contentType = { "notification" }
                            ) { entry ->
                                NotificationRow(entry, apps[entry.pkg], dateFormat) {
                                    focusManager.clearFocus()
                                    keyboard?.hide()
                                    selectedEntry = entry
                                    sheetOpen = true
                                }
                            }
                        }
                    }
                }
            }
            Box(
                Modifier.fillMaxWidth()
                    .then(if (backdrop != null) Modifier.textureBlur(
                        backdrop = backdrop,
                        shape = RectangleShape,
                        blurRadius = 12f,
                        colors = BlurColors(blendColors = listOf(BlendColorEntry(topColor.copy(alpha = 0.8f))))
                    ) else Modifier.background(topColor))
            ) {
                TopAppBar(
                    title = stringResource(R.string.screen_title),
                    largeTitle = stringResource(R.string.screen_title),
                    color = Color.Transparent,
                    scrollBehavior = scrollBehavior,
                    modifier = Modifier.fillMaxWidth().onSizeChanged { barHeight = with(density) { it.height.toDp() } },
                    navigationIcon = {
                        AnimatedVisibility(
                            visible = listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0,
                            enter = fadeIn() + slideInVertically { it / 2 },
                            exit = fadeOut() + slideOutVertically { it / 2 }
                        ) {
                            IconButton(onClick = { dismissSearchFocus(); coroutineScope.launch { listState.animateScrollToItem(0) } }) {
                                Icon(imageVector = MiuixIcons.Back, contentDescription = stringResource(R.string.back_to_top),
                                    modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = 90f })
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { dismissSearchFocus(); refresh() }, enabled = !busy) {
                            if (busy) {
                                val refreshingDescription = stringResource(R.string.refresh_refreshing)
                                InfiniteProgressIndicator(
                                    modifier = Modifier.semantics { contentDescription = refreshingDescription },
                                    color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    size = 24.dp,
                                    strokeWidth = 2.dp,
                                    orbitingDotSize = 3.dp
                                )
                            } else {
                                Icon(imageVector = MiuixIcons.Refresh, contentDescription = stringResource(R.string.refresh), modifier = Modifier.size(24.dp))
                            }
                        }
                    },
                    bottomContent = {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Text(
                                if (busy) status else buildString {
                                    append(getString(R.string.total_count, entries.size))
                                    append(" | ")
                                    append(getString(R.string.filtered_count, filtered.size))
                                    if (newBackupCount > 0) { append(" | "); append(getString(R.string.new_backup_count, newBackupCount)) }
                                },
                                style = MiuixTheme.textStyles.body2,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            val searchDescription = stringResource(R.string.search_hint)
                            val clearSearchDescription = stringResource(R.string.clear_search)
                            SearchBar(
                                inputField = { InputField(query = query, onQueryChange = {
                                    query = it; saveSearch()
                                    if (it.isEmpty() && !searchFocused) searchExpanded = false
                                },
                                    onSearch = { focusManager.clearFocus(); keyboard?.hide() },
                                    expanded = searchExpanded, onExpandedChange = { searchExpanded = it },
                                    label = "",
                                    modifier = Modifier.semantics { contentDescription = searchDescription },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = MiuixIcons.Basic.Search,
                                            contentDescription = searchDescription,
                                            tint = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                                            modifier = Modifier.padding(
                                                start = SearchBarDefaults.LeadingIconStartPadding,
                                                end = SearchBarDefaults.LeadingIconEndPadding
                                            )
                                        )
                                    },
                                    trailingIcon = {
                                        AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                                            Box(
                                                Modifier.padding(
                                                    start = SearchBarDefaults.TrailingIconStartPadding,
                                                    end = SearchBarDefaults.TrailingIconEndPadding
                                                ),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Icon(
                                                    imageVector = MiuixIcons.Basic.SearchCleanup,
                                                    contentDescription = clearSearchDescription,
                                                    tint = MiuixTheme.colorScheme.onSurfaceContainerHighest,
                                                    modifier = Modifier.clip(CircleShape).clickable(
                                                        onClickLabel = clearSearchDescription,
                                                        role = Role.Button
                                                    ) {
                                                        query = ""
                                                        saveSearch()
                                                        if (!searchFocused) searchExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    },
                                    interactionSource = searchInteraction) },
                                expanded = searchExpanded,
                                onExpandedChange = { searchExpanded = it; if (!it) { focusManager.clearFocus(); keyboard?.hide() } },
                                outsideEndAction = {
                                    Text(text = stringResource(R.string.cancel),
                                        color = MiuixTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 12.dp).clickable(
                                            interactionSource = null, indication = null
                                        ) { query = ""; saveSearch(); focusManager.clearFocus(); keyboard?.hide(); searchExpanded = false })
                                },
                                content = {}, modifier = Modifier.width(screenWidth - 32.dp),
                                insideMargin = androidx.compose.ui.unit.DpSize(0.dp, 0.dp)
                            )
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                Row(Modifier.clickable(
                                    interactionSource = null,
                                    indication = null,
                                    onClickLabel = stringResource(R.string.search_scope),
                                    role = Role.Button
                                ) {
                                    dismissSearchFocus()
                                    scopeOpen = !scopeOpen
                                }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.search_scope), style = MiuixTheme.textStyles.subtitle,
                                        color = MiuixTheme.colorScheme.onBackgroundVariant)
                                    Spacer(Modifier.width(8.dp))
                                    Icon(imageVector = MiuixIcons.Basic.ArrowUpDown, contentDescription = stringResource(R.string.search_scope),
                                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                        modifier = Modifier.size(10.dp, 16.dp))
                                }
                                OverlayListPopup(show = scopeOpen,
                                    alignment = PopupPositionProvider.Align.End,
                                    onDismissRequest = { scopeOpen = false },
                                    maxHeight = 400.dp, enableWindowDim = true) {
                                    ListPopupColumn {
                                        scopeLabels.forEachIndexed { index, label ->
                                            DropdownImpl(
                                                item = DropdownItem(text = stringResource(label)),
                                                optionSize = 12,
                                                isSelected = searchScopes and (1 shl index) != 0,
                                                index = index,
                                                isFirst = index == 0,
                                                isLast = false,
                                                onSelectedIndexChange = {
                                                    searchScopes = searchScopes xor (1 shl index)
                                                    saveSearch()
                                                }
                                            )
                                        }
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                                            thickness = 1.5.dp
                                        )
                                        DropdownImpl(
                                            item = DropdownItem(text = stringResource(R.string.regex_search)),
                                            optionSize = 12,
                                            isSelected = useRegex,
                                            index = 10,
                                            isFirst = false,
                                            isLast = false,
                                            onSelectedIndexChange = { useRegex = !useRegex; saveSearch() }
                                        )
                                        DropdownImpl(
                                            item = DropdownItem(text = stringResource(R.string.ignore_separators)),
                                            optionSize = 12,
                                            isSelected = ignoreSeparators,
                                            index = 11,
                                            isFirst = false,
                                            isLast = true,
                                            onSelectedIndexChange = { ignoreSeparators = !ignoreSeparators; saveSearch() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
        WindowBottomSheet(
            show = sheetOpen,
            title = selectedEntry?.let { apps[it.pkg]?.name ?: it.pkg },
            backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
            startAction = {
                val dismissState = LocalDismissState.current
                IconButton(onClick = { dismissState?.invoke() ?: run { sheetOpen = false } }) {
                    Icon(
                        imageVector = MiuixIcons.Basic.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = MiuixTheme.colorScheme.onSurface
                    )
                }
            },
            onDismissRequest = { sheetOpen = false },
            onDismissFinished = { selectedEntry = null },
            allowDismiss = true,
            enableNestedScroll = true
        ) {
            val dialogView = LocalView.current
            val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
                ?: (dialogView as? DialogWindowProvider)?.window
            val dark = isSystemInDarkTheme()
            SideEffect {
                dialogWindow?.let { dialog ->
                    WindowCompat.getInsetsController(dialog, dialog.decorView).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                }
            }
            selectedEntry?.let { entry ->
                val packageInfo = apps[entry.pkg]
                val appName = packageInfo?.name ?: entry.pkg
                val notificationIcon = remember(entry) { loadNotificationIcon(entry) }
                val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                Column(
                    Modifier.fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp + navigationBarBottom)
                ) {
                    SelectionContainer {
                        Column {
                            SmallTitle(text = stringResource(R.string.detail_app_icon), insideMargin = PaddingValues(top = 6.dp, bottom = 4.dp))
                            if (packageInfo?.icon != null) {
                                Image(packageInfo.icon, contentDescription = stringResource(R.string.a11y_app_icon, appName), modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                            } else {
                                Text(stringResource(R.string.detail_unavailable), fontFamily = FontFamily.Monospace)
                            }
                            SmallTitle(text = stringResource(R.string.detail_notification_icon), insideMargin = PaddingValues(top = 10.dp, bottom = 4.dp))
                            if (notificationIcon != null) {
                                Spacer(Modifier.height(8.dp))
                                Image(notificationIcon, contentDescription = stringResource(R.string.a11y_notification_icon, entry.title.ifBlank { appName }), modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)))
                            } else {
                                Text(stringResource(R.string.detail_unavailable), fontFamily = FontFamily.Monospace)
                            }
                            DetailField(stringResource(R.string.detail_package), entry.pkg)
                            DetailField(stringResource(R.string.detail_time),
                                remember(entry.time) { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(entry.time)) })
                            DetailField(stringResource(R.string.detail_user_id), if (entry.userId >= 0) entry.userId.toString() else stringResource(R.string.detail_unavailable))
                            DetailField(stringResource(R.string.detail_channel_name), entry.channelName.ifEmpty { stringResource(R.string.detail_unavailable) })
                            DetailField(stringResource(R.string.detail_channel_id), entry.channelId.ifEmpty { stringResource(R.string.detail_unavailable) })
                            DetailField(stringResource(R.string.detail_conversation_id), entry.conversationId.ifEmpty { stringResource(R.string.detail_unavailable) })
                            if (entry.title.isNotEmpty()) DetailField(stringResource(R.string.detail_title), entry.title)
                            if (entry.text.isNotEmpty()) DetailField(stringResource(R.string.detail_content), entry.text)
                            val sourceValue = if (entry.source == "@system-memory") stringResource(R.string.detail_source_memory)
                                else entry.source.replace("\n", "\n${stringResource(R.string.detail_source_backup)}: ")
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                SmallTitle(text = stringResource(R.string.detail_source), insideMargin = PaddingValues(top = 10.dp, bottom = 4.dp))
                                SmallTitle(text = stringResource(R.string.same_source_count, sourceCounts[entry.source] ?: 0),
                                    insideMargin = PaddingValues(top = 10.dp, bottom = 4.dp))
                            }
                            Text(sourceValue.ifEmpty { stringResource(R.string.detail_unavailable) }, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
        }

    }

    @Composable
    private fun DetailField(label: String, value: String) {
        SmallTitle(text = label, insideMargin = PaddingValues(top = 10.dp, bottom = 4.dp))
        Text(value, fontFamily = FontFamily.Monospace)
    }
}
