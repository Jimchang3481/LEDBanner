package swifties.ticker.mindnight

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import swifties.ticker.mindnight.ui.theme.跑馬燈Theme


// --- Data Structures ---
data class FontOption(
    val name: String,
    val family: FontFamily,
    val isWeightAdjustable: Boolean = true,
    val fixedWeight: FontWeight? = null,
    val isEnabled: Boolean = true
)

data class FontWeightOption(val name: String, val weight: FontWeight)

enum class MarqueeDirection(val label: String) {
    RTL("由右至左"), LTR("由左至右")
}

enum class AppTheme(val label: String) {
    LIGHT("淺色"), DARK("深色"), SYSTEM("跟隨系統")
}

// --- Global Constants ---
private val FontSizes = listOf(24.sp, 36.sp, 48.sp, 64.sp, 80.sp)
private val FontWeightOptions = listOf(
    FontWeightOption("正常", FontWeight.W400),
    FontWeightOption("粗", FontWeight.W700)
)
// basicMarquee 接收的是 Velocity，值越小越慢，這裡我們定義每秒跑多少 dp
private val MarqueeSpeeds = listOf(30.dp, 60.dp, 100.dp, 150.dp, 250.dp)
private val PresetColors = listOf(
    Color.White, Color(0xFFFFEB3B), Color(0xFFFF9800), Color(0xFF4CAF50),
    Color(0xFF00BCD4), Color(0xFF2196F3), Color(0xFF9C27B0),
    Color(0xFFF44336), Color(0xFFE91E63), Color(0xFFFFC107),
    Color(0xFF8BC34A), Color(0xFF9E9E9E)
)

val fontOptionsList = listOf(
    FontOption("黑體", FontFamily.Default),
)

// --- Architecture: UiState & Events ---
data class MarqueeUiState(
    val inputText: String = "在此輸入文字",
    val fontSizeLevel: Int = 2,
    val selectedFontIndex: Int = 0,
    val selectedWeightIndex: Int = 0,
    val selectedColor: Color = Color.White,
    val speedLevel: Int = 2,
    val direction: MarqueeDirection = MarqueeDirection.RTL,
    val isBlinking: Boolean = false,
    val blinkSpeedLevel: Int = 2,
    val isMaxBrightnessEnabled: Boolean = true,
    val isFullscreen: Boolean = false,
    val currentDestination: AppDestinations = AppDestinations.MARQUEE,
    val savedList: List<MarqueeSaveItem> = emptyList(),
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val showDeleteConfirmation: Boolean = true,
    val useDynamicColors: Boolean = true,
    val customThemeColor: Color = Color(0xFF6750A4)
) {
    val currentFont: FontOption get() = fontOptionsList[selectedFontIndex]
    val currentWeight: FontWeight get() = if (currentFont.isWeightAdjustable) {
        FontWeightOptions[selectedWeightIndex].weight
    } else {
        currentFont.fixedWeight ?: FontWeight.Normal
    }
}

data class MarqueeSaveItem(
    val inputText: String,
    val fontSizeLevel: Int,
    val selectedFontIndex: Int,
    val selectedWeightIndex: Int,
    val selectedColor: Color,
    val speedLevel: Int,
    val direction: MarqueeDirection,
    val isBlinking: Boolean,
    val blinkSpeedLevel: Int
)

sealed interface MarqueeEvent {
    data class UpdateText(val text: String) : MarqueeEvent
    data class UpdateFontSize(val level: Int) : MarqueeEvent
    data class UpdateFont(val index: Int) : MarqueeEvent
    data class UpdateWeight(val index: Int) : MarqueeEvent
    data class UpdateColor(val color: Color) : MarqueeEvent
    data class UpdateSpeed(val level: Int) : MarqueeEvent
    data class UpdateDirection(val direction: MarqueeDirection) : MarqueeEvent
    data class ToggleBlinking(val enabled: Boolean) : MarqueeEvent
    data class UpdateBlinkSpeed(val level: Int) : MarqueeEvent
    data class ToggleMaxBrightness(val enabled: Boolean) : MarqueeEvent
    data class SetFullscreen(val enabled: Boolean) : MarqueeEvent
    data class Navigate(val destination: AppDestinations) : MarqueeEvent
    object SaveCurrent : MarqueeEvent
    data class DeleteSaved(val index: Int) : MarqueeEvent
    data class ApplySaved(val item: MarqueeSaveItem) : MarqueeEvent
    data class UpdateAppTheme(val theme: AppTheme) : MarqueeEvent
    data class ToggleDeleteConfirmation(val enabled: Boolean) : MarqueeEvent
    data class ToggleDynamicColors(val enabled: Boolean) : MarqueeEvent
    data class UpdateCustomThemeColor(val color: Color) : MarqueeEvent
}

class MarqueeViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs: SharedPreferences = application.getSharedPreferences("marquee_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(loadState())
    val uiState: StateFlow<MarqueeUiState> = _uiState.asStateFlow()

    private fun loadState(): MarqueeUiState {
        val savedListJson = prefs.getString("savedList", "[]") ?: "[]"
        val savedList = mutableListOf<MarqueeSaveItem>()
        try {
            val jsonArray = JSONArray(savedListJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                savedList.add(MarqueeSaveItem(
                    inputText = obj.getString("text"),
                    fontSizeLevel = obj.getInt("size"),
                    selectedFontIndex = obj.getInt("font"),
                    selectedWeightIndex = obj.getInt("weight"),
                    selectedColor = Color(obj.getInt("color")),
                    speedLevel = obj.getInt("speed"),
                    direction = MarqueeDirection.valueOf(obj.getString("dir")),
                    isBlinking = obj.getBoolean("blink"),
                    blinkSpeedLevel = obj.getInt("blinkSpeed")
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return MarqueeUiState(
            inputText = prefs.getString("inputText", "在此輸入文字") ?: "在此輸入文字",
            fontSizeLevel = prefs.getInt("fontSizeLevel", 2),
            selectedFontIndex = prefs.getInt("selectedFontIndex", 0),
            selectedWeightIndex = prefs.getInt("selectedWeightIndex", 0),
            selectedColor = Color(prefs.getInt("selectedColor", Color.White.toArgb())),
            speedLevel = prefs.getInt("speedLevel", 2),
            direction = MarqueeDirection.valueOf(prefs.getString("direction", MarqueeDirection.RTL.name) ?: MarqueeDirection.RTL.name),
            isBlinking = prefs.getBoolean("isBlinking", false),
            blinkSpeedLevel = prefs.getInt("blinkSpeedLevel", 2),
            isMaxBrightnessEnabled = prefs.getBoolean("isMaxBrightnessEnabled", true),
            appTheme = AppTheme.valueOf(prefs.getString("appTheme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name),
            showDeleteConfirmation = prefs.getBoolean("showDeleteConfirmation", true),
            useDynamicColors = prefs.getBoolean("useDynamicColors", true),
            customThemeColor = Color(prefs.getInt("customThemeColor", Color(0xFF6750A4).toArgb())),
            savedList = savedList
        )
    }

    private fun saveState(state: MarqueeUiState) {
        prefs.edit().apply {
            putString("inputText", state.inputText)
            putInt("fontSizeLevel", state.fontSizeLevel)
            putInt("selectedFontIndex", state.selectedFontIndex)
            putInt("selectedWeightIndex", state.selectedWeightIndex)
            putInt("selectedColor", state.selectedColor.toArgb())
            putInt("speedLevel", state.speedLevel)
            putString("direction", state.direction.name)
            putBoolean("isBlinking", state.isBlinking)
            putInt("blinkSpeedLevel", state.blinkSpeedLevel)
            putBoolean("isMaxBrightnessEnabled", state.isMaxBrightnessEnabled)
            putString("appTheme", state.appTheme.name)
            putBoolean("showDeleteConfirmation", state.showDeleteConfirmation)
            putBoolean("useDynamicColors", state.useDynamicColors)
            putInt("customThemeColor", state.customThemeColor.toArgb())
            
            // Save list as JSON
            val jsonArray = JSONArray()
            state.savedList.forEach { item ->
                val obj = JSONObject()
                obj.put("text", item.inputText)
                obj.put("size", item.fontSizeLevel)
                obj.put("font", item.selectedFontIndex)
                obj.put("weight", item.selectedWeightIndex)
                obj.put("color", item.selectedColor.toArgb())
                obj.put("speed", item.speedLevel)
                obj.put("dir", item.direction.name)
                obj.put("blink", item.isBlinking)
                obj.put("blinkSpeed", item.blinkSpeedLevel)
                jsonArray.put(obj)
            }
            putString("savedList", jsonArray.toString())
            apply()
        }
    }

    fun onEvent(event: MarqueeEvent) {
        _uiState.update { state ->
            val newState = when (event) {
                is MarqueeEvent.UpdateText -> state.copy(inputText = event.text)
                is MarqueeEvent.UpdateFontSize -> state.copy(fontSizeLevel = event.level)
                is MarqueeEvent.UpdateFont -> state.copy(
                    selectedFontIndex = event.index,
                    selectedWeightIndex = if (!fontOptionsList[event.index].isWeightAdjustable) 0 else state.selectedWeightIndex
                )
                is MarqueeEvent.UpdateWeight -> state.copy(selectedWeightIndex = event.index)
                is MarqueeEvent.UpdateColor -> state.copy(selectedColor = event.color)
                is MarqueeEvent.UpdateSpeed -> state.copy(speedLevel = event.level)
                is MarqueeEvent.UpdateDirection -> state.copy(direction = event.direction)
                is MarqueeEvent.ToggleBlinking -> state.copy(isBlinking = event.enabled)
                is MarqueeEvent.UpdateBlinkSpeed -> state.copy(blinkSpeedLevel = event.level)
                is MarqueeEvent.ToggleMaxBrightness -> state.copy(isMaxBrightnessEnabled = event.enabled)
                is MarqueeEvent.SetFullscreen -> state.copy(isFullscreen = event.enabled)
                is MarqueeEvent.Navigate -> state.copy(currentDestination = event.destination)
                is MarqueeEvent.SaveCurrent -> {
                    val newItem = MarqueeSaveItem(
                        inputText = state.inputText,
                        fontSizeLevel = state.fontSizeLevel,
                        selectedFontIndex = state.selectedFontIndex,
                        selectedWeightIndex = state.selectedWeightIndex,
                        selectedColor = state.selectedColor,
                        speedLevel = state.speedLevel,
                        direction = state.direction,
                        isBlinking = state.isBlinking,
                        blinkSpeedLevel = state.blinkSpeedLevel
                    )
                    state.copy(savedList = state.savedList + newItem)
                }
                is MarqueeEvent.DeleteSaved -> {
                    state.copy(savedList = state.savedList.toMutableList().apply { removeAt(event.index) })
                }
                is MarqueeEvent.ApplySaved -> {
                    state.copy(
                        inputText = event.item.inputText,
                        fontSizeLevel = event.item.fontSizeLevel,
                        selectedFontIndex = event.item.selectedFontIndex,
                        selectedWeightIndex = event.item.selectedWeightIndex,
                        selectedColor = event.item.selectedColor,
                        speedLevel = event.item.speedLevel,
                        direction = event.item.direction,
                        isBlinking = event.item.isBlinking,
                        blinkSpeedLevel = event.item.blinkSpeedLevel,
                        currentDestination = AppDestinations.MARQUEE
                    )
                }
                is MarqueeEvent.UpdateAppTheme -> state.copy(appTheme = event.theme)
                is MarqueeEvent.ToggleDeleteConfirmation -> state.copy(showDeleteConfirmation = event.enabled)
                is MarqueeEvent.ToggleDynamicColors -> state.copy(useDynamicColors = event.enabled)
                is MarqueeEvent.UpdateCustomThemeColor -> state.copy(customThemeColor = event.color)
                is MarqueeEvent.UpdateCustomThemeColor -> {
                    val rawColor = event.color
                    // 計算顏色的亮度 (0.0 是純黑，1.0 是純白)
                    val luminance = ColorUtils.calculateLuminance(rawColor.toArgb())

                    // 🔥 最強防護網：不管深色淺色模式，只要你選的顏色太黑(<0.1)或太白(>0.9)
                    // 我們就強制把你拉回預設的紫色，保護 App 不會瞎掉
                    val safeColor = if (luminance < 0.1f || luminance > 0.9f) {
                        Color(0xFF6750A4) // 預設的紫色
                    } else {
                        rawColor
                    }

                    state.copy(customThemeColor = safeColor)
                }
// ------------------------------------
            }
            saveState(newState)
            newState
        }
    }
}

// --- Main Activity ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MarqueeViewModel = viewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            
            val darkTheme = when (state.appTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            跑馬燈Theme(
                darkTheme = darkTheme,
                dynamicColor = state.useDynamicColors,
                seedColor = state.customThemeColor
            ) {
                MarqueeApp(viewModel)
            }
        }
    }
}

@Composable
fun MarqueeApp(viewModel: MarqueeViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 🔥 終極修正：放棄 primaryContainer，全部強制使用 primary
    val myCustomItemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            // 1. 背景改用 primary (藍色)，加上 20% 的透明度，這樣視覺上會有很高級的果凍感
            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),

            // 2. 裡面的圖示也強制變成藍色
            selectedIconColor = MaterialTheme.colorScheme.primary,

            // 3. 文字一樣維持藍色
            selectedTextColor = MaterialTheme.colorScheme.primary,

            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )

    if (state.isFullscreen) {
        FullscreenMarquee(
            state = state,
            onDismiss = { viewModel.onEvent(MarqueeEvent.SetFullscreen(false)) }
        )
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.forEach { dest ->
                    item(
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                        selected = dest == state.currentDestination,
                        onClick = { viewModel.onEvent(MarqueeEvent.Navigate(dest)) },
                        // 直接把剛剛算好的顏色變數傳進來
                        colors = myCustomItemColors
                    )
                }
            }
        ) {
            when (state.currentDestination) {
                AppDestinations.MARQUEE -> MarqueeSettingsScreen(state = state, onEvent = viewModel::onEvent)
                AppDestinations.SAVED -> SavedSettingsScreen(state, onEvent = viewModel::onEvent)
                AppDestinations.SETTINGS -> SettingsScreen(state, onEvent = viewModel::onEvent)
            }
        }
    }
}

@Composable
fun SettingsScreen(state: MarqueeUiState, onEvent: (MarqueeEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("App 設定", style = MaterialTheme.typography.headlineSmall)

        // Theme Section
        Column {
            Text("主題模式", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(Modifier.selectableGroup()) {
                    AppTheme.entries.forEach { theme ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = state.appTheme == theme,
                                    onClick = { onEvent(MarqueeEvent.UpdateAppTheme(theme)) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.appTheme == theme,
                                onClick = null // handled by selectable Row
                            )
                            Text(
                                text = theme.label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

        // Features Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("功能設定", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            ListItem(
                headlineContent = { Text("確認對話框") },
                supportingContent = { Text("在刪除已儲存的跑馬燈時，跳出確認警示") },
                trailingContent = {
                    Switch(
                        checked = state.showDeleteConfirmation,
                        onCheckedChange = { onEvent(MarqueeEvent.ToggleDeleteConfirmation(it)) }
                    )
                }
            )

            ListItem(
                headlineContent = { Text("動態配色") },
                supportingContent = { Text("App 顏色跟隨系統桌布自動變化 (僅支援 Android 12+)") },
                trailingContent = {
                    Switch(
                        checked = state.useDynamicColors,
                        onCheckedChange = { onEvent(MarqueeEvent.ToggleDynamicColors(it)) }
                    )
                }
            )

            if (!state.useDynamicColors) {
                var showThemeColorPicker by remember { mutableStateOf(false) }
                ListItem(
                    headlineContent = { Text("主題樣式") },
                    supportingContent = { Text("手動挑選 App 的主色調") },
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(state.customThemeColor)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { showThemeColorPicker = true }
                        )
                    }
                )

                if (showThemeColorPicker) {
                    ColorPickerDialog(
                        initialColor = state.customThemeColor,
                        onColorSelected = {
                            onEvent(MarqueeEvent.UpdateCustomThemeColor(it))
                            showThemeColorPicker = false
                        },
                        onDismiss = { showThemeColorPicker = false }
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun SavedSettingsScreen(state: MarqueeUiState, onEvent: (MarqueeEvent) -> Unit) {
    var showDeleteConfirmIndex by remember { mutableStateOf<Int?>(null) }

    Box(Modifier.fillMaxSize().systemBarsPadding()) {
        if (state.savedList.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                Spacer(Modifier.height(16.dp))
                Text("尚無儲存的設定", color = Color.Gray)
                Text("在主頁面點擊上方儲存按鈕來新增紀錄", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        } else {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text("已儲存的跑馬燈", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(state.savedList) { index, item ->
                        SavedItemCard(
                            item = item, 
                            onApply = { onEvent(MarqueeEvent.ApplySaved(item)) }, 
                            onDelete = { 
                                if (state.showDeleteConfirmation) {
                                    showDeleteConfirmIndex = index 
                                } else {
                                    onEvent(MarqueeEvent.DeleteSaved(index))
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showDeleteConfirmIndex != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmIndex = null },
                title = { Text("確認刪除") },
                text = { Text("您確定要刪除這筆此設定檔嗎？") },
                confirmButton = {
                    Button(
                        onClick = {
                            onEvent(MarqueeEvent.DeleteSaved(showDeleteConfirmIndex!!))
                            showDeleteConfirmIndex = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("確定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmIndex = null }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
fun SavedItemCard(item: MarqueeSaveItem, onApply: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onApply() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.inputText,
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.titleMedium,
                    color = item.selectedColor
                )
                Text(
                    text = "大小 ${item.fontSizeLevel + 1} | ${if (item.selectedWeightIndex == 0) "正常" else "粗"} | 速度 ${item.speedLevel + 1} | ${item.direction.label} | ${if (item.isBlinking) "閃爍(速度 ${item.blinkSpeedLevel + 1})" else "無閃爍"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "刪除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// 核心優化：使用 SubcomposeLayout 進行無限寬度測量，徹底解決截斷問題
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NativeMarqueeText(
    text: String,
    style: TextStyle,
    velocity: Dp,
    direction: MarqueeDirection,
    isBlinking: Boolean,
    blinkSpeedLevel: Int,
    modifier: Modifier = Modifier
) {
    // --- 閃爍邏輯 ---
    val alpha by if (isBlinking) {
        val duration = listOf(1000, 750, 500, 300, 150)[blinkSpeedLevel]
        val infiniteTransition = rememberInfiniteTransition(label = "BlinkTransition")
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "BlinkAlpha"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val density = LocalDensity.current
    val spacingPx = with(density) { 50.dp.toPx() }
    val velocityPx = with(density) { velocity.toPx() }

    SubcomposeLayout(
        modifier = modifier
            .clipToBounds()
            .alpha(alpha)
    ) { constraints ->
        // 1. 【核心修復】：使用無限大的寬度 Constraints() 進行子組件測量，確保文字永不截斷
        val textPlaceable = subcompose("measure") {
            Text(text = text, style = style, maxLines = 1, softWrap = false)
        }.first().measure(Constraints())

        val textWidthPx = textPlaceable.width.toFloat()
        val containerWidthPx = constraints.maxWidth.toFloat()
        
        // 2. 佈局內容：根據測量出的真實寬度建立雙軌道
        val contentPlaceable = subcompose("content") {
            if (textWidthPx > 0f && velocityPx > 0f) {
                // 如果文字寬度小於螢幕，我們讓它至少跑完一個螢幕寬度，避免太快消失或接得太生硬
                val effectiveSpacingPx = if (textWidthPx < containerWidthPx) {
                    containerWidthPx // 短文字使用螢幕寬度作為間隔，確保完整進出
                } else {
                    spacingPx // 長文字維持小間隔
                }
                
                val segmentWidthPx = textWidthPx + effectiveSpacingPx
                val durationMillis = ((segmentWidthPx / velocityPx) * 1000f).toInt().coerceAtLeast(10)
                val infiniteTransition = rememberInfiniteTransition(label = "MarqueeLoop")
                
                val progress by key(segmentWidthPx, velocityPx, direction) {
                    infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "MarqueeProgress"
                    )
                }

                val offsetPx = if (direction == MarqueeDirection.RTL) {
                    -segmentWidthPx * progress
                } else {
                    -segmentWidthPx * (1f - progress)
                }

                Row(
                    modifier = Modifier.graphicsLayer { translationX = offsetPx },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Track A
                    Text(text = text, style = style, maxLines = 1, softWrap = false)
                    Spacer(modifier = Modifier.layout { measurable, _ ->
                        // 動態間隔
                        val placeable = measurable.measure(Constraints.fixedWidth(effectiveSpacingPx.toInt()))
                        layout(placeable.width, placeable.height) {
                            placeable.placeRelative(0, 0)
                        }
                    })
                    // Track B
                    Text(text = text, style = style, maxLines = 1, softWrap = false)
                    Spacer(modifier = Modifier.layout { measurable, _ ->
                        val placeable = measurable.measure(Constraints.fixedWidth(effectiveSpacingPx.toInt()))
                        layout(placeable.width, placeable.height) {
                            placeable.placeRelative(0, 0)
                        }
                    })
                }
            }
        }.firstOrNull()?.measure(Constraints(maxWidth = Int.MAX_VALUE)) // 確保 Row 也不被截斷

        layout(constraints.maxWidth, textPlaceable.height) {
            contentPlaceable?.placeRelative(0, 0)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarqueeSettingsScreen(
    state: MarqueeUiState,
    onEvent: (MarqueeEvent) -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("跑馬燈設定", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { onEvent(MarqueeEvent.SaveCurrent) }) {
                    Icon(Icons.Default.Save, "儲存當前設定")
                }
                Button(onClick = { onEvent(MarqueeEvent.SetFullscreen(true)) }) {
                    Icon(Icons.Default.PlayArrow, null)
                    Text(" 全螢幕顯示")
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            NativeMarqueeText(
                text = state.inputText.ifEmpty { " " },
                style = TextStyle(
                    color = state.selectedColor,
                    fontSize = FontSizes[state.fontSizeLevel],
                    fontFamily = state.currentFont.family,
                    fontWeight = state.currentWeight
                ),
                velocity = MarqueeSpeeds[state.speedLevel],
                direction = state.direction,
                isBlinking = state.isBlinking,
                blinkSpeedLevel = state.blinkSpeedLevel,
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = state.inputText,
            onValueChange = { onEvent(MarqueeEvent.UpdateText(it)) },
            label = { Text("顯示文字") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        SmoothSliderSetting(title = "字體大小", level = state.fontSizeLevel) { onEvent(MarqueeEvent.UpdateFontSize(it)) }

        // 字體樣式
        Column {
            Text("字體樣式", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                itemsIndexed(fontOptionsList) { index, font ->
                    FilterChip(
                        selected = state.selectedFontIndex == index,
                        onClick = {
                            if (font.isEnabled) onEvent(MarqueeEvent.UpdateFont(index))
                            else Toast.makeText(context, "字體開發中", Toast.LENGTH_SHORT).show()
                        },
                        label = { Text(font.name) },
                        leadingIcon = if (state.selectedFontIndex == index) {
                            { Icon(Icons.Default.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = if (!font.isEnabled) Color.Gray else Color.Unspecified,
                            containerColor = if (!font.isEnabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Unspecified
                        )
                    )
                }
            }
        }

        // 字體粗細
        Column {
            Text("字體粗細", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                FontWeightOptions.forEachIndexed { index, option ->
                    FilterChip(
                        selected = if (state.currentFont.isWeightAdjustable) state.selectedWeightIndex == index else index == 0,
                        onClick = {
                            if (state.currentFont.isWeightAdjustable) onEvent(MarqueeEvent.UpdateWeight(index))
                            else Toast.makeText(context, "${state.currentFont.name}不支援調整粗細", Toast.LENGTH_SHORT).show()
                        },
                        label = { Text(option.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = if (!state.currentFont.isWeightAdjustable && index != 0) Color.Gray else Color.Unspecified
                        )
                    )
                }
            }
        }

        SmoothSliderSetting(title = "滾動速度", level = state.speedLevel) { onEvent(MarqueeEvent.UpdateSpeed(it)) }

        // 捲動方向
        Column {
            Text("捲動方向", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                MarqueeDirection.entries.forEach { direction ->
                    FilterChip(
                        selected = state.direction == direction,
                        onClick = { onEvent(MarqueeEvent.UpdateDirection(direction)) },
                        label = { Text(direction.label) }
                    )
                }
            }
        }

        // 文字閃爍
        Column {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("文字閃爍", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Switch(checked = state.isBlinking, onCheckedChange = { onEvent(MarqueeEvent.ToggleBlinking(it)) })
            }
            if (state.isBlinking) {
                Spacer(Modifier.height(8.dp))
                SmoothSliderSetting(title = "閃爍速度", level = state.blinkSpeedLevel) { onEvent(MarqueeEvent.UpdateBlinkSpeed(it)) }
            }
        }

        // 顏色設定
        Column {
            Text("顏色設定", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            var showColorPicker by remember { mutableStateOf(false) }

            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PresetColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { onEvent(MarqueeEvent.UpdateColor(color)) }
                            .border(if (state.selectedColor == color) 2.dp else 1.dp, Color.Gray, CircleShape)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.sweepGradient(listOf(Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red)))
                        .clickable { showColorPicker = true }
                        .border(1.dp, Color.Gray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Palette, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            if (showColorPicker) {
                ColorPickerDialog(
                    initialColor = state.selectedColor,
                    onColorSelected = {
                        onEvent(MarqueeEvent.UpdateColor(it))
                        showColorPicker = false
                    },
                    onDismiss = { showColorPicker = false }
                )
            }
        }

        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("全螢幕自動最大亮度", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = state.isMaxBrightnessEnabled, onCheckedChange = { onEvent(MarqueeEvent.ToggleMaxBrightness(it)) })
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ColorPickerDialog 保持不變，因為這部分封裝得還行，主要是把狀態提升了。
@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val hsv = remember {
        val hsvArray = FloatArray(3)
        ColorUtils.colorToHSL(initialColor.toArgb(), hsvArray)
        mutableStateListOf(hsvArray[0], hsvArray[1], hsvArray[2])
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("選擇顏色") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 飽和度與明度區塊
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    val x = change.position.x.coerceIn(0f, size.width.toFloat())
                                    val y = change.position.y.coerceIn(0f, size.height.toFloat())
                                    hsv[1] = x / size.width
                                    hsv[2] = 1f - (y / size.height)
                                }
                            }
                    ) {
                        val saturationGradient = Brush.linearGradient(
                            colors = listOf(Color.White, Color.hsv(hsv[0], 1f, 1f)),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f)
                        )
                        val valueGradient = Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.Black),
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height)
                        )

                        drawRect(saturationGradient)
                        drawRect(valueGradient)

                        val selX = hsv[1] * size.width
                        val selY = (1f - hsv[2]) * size.height
                        drawCircle(
                            color = if (hsv[2] > 0.5f) Color.Black else Color.White,
                            radius = 8.dp.toPx(),
                            center = Offset(selX, selY),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // 色相區塊
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    val x = change.position.x.coerceIn(0f, size.width.toFloat())
                                    hsv[0] = (x / size.width) * 360f
                                }
                            }
                    ) {
                        val hueGradient = Brush.linearGradient(
                            colors = listOf(
                                Color.Red, Color.Yellow, Color.Green,
                                Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f)
                        )
                        drawRect(hueGradient)

                        val selX = (hsv[0] / 360f) * size.width
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(selX - 2.dp.toPx(), 0f),
                            size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // 預覽區塊
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.hsv(hsv[0], hsv[1], hsv[2]))
                            .border(1.dp, Color.Gray, CircleShape)
                    )
                    Text("選中的顏色", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onColorSelected(Color.hsv(hsv[0], hsv[1], hsv[2])) }) {
                Text("確定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun SmoothSliderSetting(title: String, level: Int, onValueChange: (Int) -> Unit) {
    Column {
        Text("$title (檔位 ${level + 1})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Slider(value = level.toFloat(), onValueChange = { onValueChange(it.toInt()) }, valueRange = 0f..4f, steps = 3)
    }
}

@Composable
fun FullscreenMarquee(state: MarqueeUiState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val window = (context as? ComponentActivity)?.window
    val activity = context as? ComponentActivity
    BackHandler { onDismiss() }

    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    LaunchedEffect(Unit) {
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        if (state.isMaxBrightnessEnabled) {
            val params = window?.attributes
            params?.screenBrightness = 1.0f
            window?.attributes = params
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            window?.let {
                WindowCompat.getInsetsController(it, it.decorView).show(WindowInsetsCompat.Type.systemBars())
                val params = it.attributes
                params.screenBrightness = -1.0f
                it.attributes = params
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black).clickable { onDismiss() }, Alignment.Center) {
        NativeMarqueeText(
            text = state.inputText,
            style = TextStyle(
                color = state.selectedColor,
                fontSize = FontSizes[state.fontSizeLevel] * 2, // 字體放大了兩倍
                fontFamily = state.currentFont.family,
                fontWeight = state.currentWeight
            ),
            // 🔥 核心修正：全螢幕字體放大兩倍，移動距離必須乘兩倍，視覺上才會是一樣的檔位速度！
            velocity = MarqueeSpeeds[state.speedLevel] * 2,
            direction = state.direction,
            isBlinking = state.isBlinking,
            blinkSpeedLevel = state.blinkSpeedLevel,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
    }
}

enum class AppDestinations(val label: String, val icon: ImageVector) {
    MARQUEE("跑馬燈", Icons.Default.RecordVoiceOver),
    SAVED("儲存", Icons.Default.Save),
    SETTINGS("設定", Icons.Default.Settings),
}