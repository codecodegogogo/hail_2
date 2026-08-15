package com.aistra.hail.ui.calendar

import android.content.Intent
import android.icu.util.ChineseCalendar
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.aistra.hail.app.DisguiseSession
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.main.MainActivity
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

class CalendarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!HailData.calendarDisguise || DisguiseSession.unlocked) {
            openHail()
            return
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            CalendarTheme {
                CalendarScreen(onReveal = ::openHail)
            }
        }
    }

    private fun openHail() {
        DisguiseSession.unlocked = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

private val CalendarBlue = Color(0xFF0A7CFF)
private val CalendarLightScheme = lightColorScheme(
    primary = CalendarBlue,
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF111318),
    surface = Color.White,
    onSurface = Color(0xFF111318),
    onSurfaceVariant = Color(0xFF686B73)
)
private val CalendarDarkScheme = darkColorScheme(
    primary = CalendarBlue,
    onPrimary = Color.White,
    background = Color.Black,
    onBackground = Color(0xFFF5F7FA),
    surface = Color.Black,
    onSurface = Color(0xFFF5F7FA),
    onSurfaceVariant = Color(0xFFB5B8C0)
)

@Composable
private fun CalendarTheme(content: @Composable () -> Unit) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val view = LocalView.current
    val window = (LocalContext.current as CalendarActivity).window
    SideEffect {
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = if (dark) CalendarDarkScheme else CalendarLightScheme, content = content)
}

@Composable
private fun CalendarScreen(onReveal: () -> Unit) {
    val today = remember { Calendar.getInstance() }
    var visibleYear by rememberSaveable { mutableIntStateOf(today.get(Calendar.YEAR)) }
    var visibleMonth by rememberSaveable { mutableIntStateOf(today.get(Calendar.MONTH)) }
    var yearDoubleTapAt by remember { mutableLongStateOf(0L) }

    fun changeMonth(offset: Int) {
        val calendar = GregorianCalendar(visibleYear, visibleMonth, 1).apply { add(Calendar.MONTH, offset) }
        visibleYear = calendar.get(Calendar.YEAR)
        visibleMonth = calendar.get(Calendar.MONTH)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp).padding(horizontal = 28.dp, vertical = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { yearDoubleTapAt = SystemClock.elapsedRealtime() }
                            )
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = visibleYear.toString(),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(24.dp))
                AnimatedContent(
                    targetState = visibleYear to visibleMonth,
                    transitionSpec = {
                        val sourceIndex = initialState.first * 12 + initialState.second
                        val targetIndex = targetState.first * 12 + targetState.second
                        val direction = if (targetIndex > sourceIndex) 1 else -1
                        (slideInHorizontally(tween(180)) { width -> direction * width } + fadeIn(tween(140)))
                            .togetherWith(
                                slideOutHorizontally(tween(150)) { width -> -direction * width } +
                                    fadeOut(tween(120))
                            )
                    },
                    label = "calendarMonth"
                ) { (year, month) ->
                    Column(
                        modifier = Modifier
                            .semantics {
                                contentDescription = if (Locale.getDefault().language == "zh") {
                                    "日历，左右滑动切换月份"
                                } else {
                                    "Calendar, swipe left or right to change month"
                                }
                            }
                            .pointerInput(year, month) {
                                var horizontalDrag = 0f
                                val swipeThreshold = 32.dp.toPx()
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        horizontalDrag += dragAmount
                                    },
                                    onDragEnd = {
                                        when {
                                            horizontalDrag <= -swipeThreshold -> changeMonth(1)
                                            horizontalDrag >= swipeThreshold -> changeMonth(-1)
                                        }
                                        horizontalDrag = 0f
                                    },
                                    onDragCancel = { horizontalDrag = 0f }
                                )
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .pointerInput(year, month, yearDoubleTapAt) {
                                    detectTapGestures(
                                        onLongPress = {
                                            val now = SystemClock.elapsedRealtime()
                                            val sequenceIsValid = yearDoubleTapAt != 0L &&
                                                now - yearDoubleTapAt <= 3_000L
                                            yearDoubleTapAt = 0L
                                            if (sequenceIsValid) onReveal()
                                        }
                                    )
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = monthName(year, month),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        WeekdayHeader()
                        Spacer(Modifier.height(12.dp))
                        MonthGrid(
                            year = year,
                            month = month,
                            today = today
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val labels = remember {
        if (Locale.getDefault().language == "zh") {
            listOf("日", "一", "二", "三", "四", "五", "六")
        } else {
            DateFormatSymbols.getInstance(Locale.getDefault()).shortWeekdays
                .let { symbols -> (Calendar.SUNDAY..Calendar.SATURDAY).map { symbols[it].take(1) } }
        }
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MonthGrid(year: Int, month: Int, today: Calendar) {
    val firstDay = remember(year, month) { GregorianCalendar(year, month, 1) }
    val leadingEmptyCells = firstDay.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cellCount = 42
    val availableWidth = LocalConfiguration.current.screenWidthDp.dp - 56.dp
    val maxWidth = 680.dp - 56.dp
    val cellSize = minOf(availableWidth, maxWidth) / 7f
    val fontSize = 20.sp
    val lunarDates = remember(year, month) {
        (1..daysInMonth).associateWith { day -> lunarDate(year, month, day) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(cellCount / 7) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val position = row * 7 + column
                    val day = position - leadingEmptyCells + 1
                    val isToday = day in 1..daysInMonth &&
                        today.get(Calendar.YEAR) == year &&
                        today.get(Calendar.MONTH) == month &&
                        today.get(Calendar.DAY_OF_MONTH) == day
                    Box(
                        modifier = Modifier.weight(1f).height(68.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day in 1..daysInMonth) {
                            val lunar = lunarDates[day]
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(cellSize.coerceIn(40.dp, 42.dp))
                                            .background(
                                                if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            color = if (isToday) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onBackground,
                                            fontSize = fontSize,
                                            fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(22.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (lunar != null) {
                                        Text(
                                            text = lunar,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun monthName(year: Int, month: Int): String =
    if (Locale.getDefault().language == "zh") "${month + 1}月"
    else SimpleDateFormat("LLLL", Locale.getDefault()).format(GregorianCalendar(year, month, 1).time)

private val LunarMonthNames = arrayOf(
    "正月", "二月", "三月", "四月", "五月", "六月",
    "七月", "八月", "九月", "十月", "冬月", "腊月"
)

private val LunarDayNames = arrayOf(
    "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
    "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
    "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
)

private fun lunarDate(year: Int, month: Int, day: Int): String? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
    val solar = GregorianCalendar(year, month, day)
    val lunar = ChineseCalendar().apply { time = solar.time }
    val lunarMonth = lunar.get(ChineseCalendar.MONTH)
    val lunarDay = lunar.get(ChineseCalendar.DAY_OF_MONTH)
    if (lunarMonth !in LunarMonthNames.indices || lunarDay !in 1..LunarDayNames.size) return null
    return if (lunarDay == 1) {
        val leapPrefix = if (lunar.get(android.icu.util.Calendar.IS_LEAP_MONTH) == 1) "闰" else ""
        leapPrefix + LunarMonthNames[lunarMonth]
    } else {
        LunarDayNames[lunarDay - 1]
    }
}
