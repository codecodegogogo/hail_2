package com.aistra.hail.ui.calendar

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.aistra.hail.app.DisguiseSession
import com.aistra.hail.ui.main.MainActivity
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

class CalendarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (DisguiseSession.unlocked) {
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
    var yearTapCount by remember { mutableIntStateOf(0) }
    var lastYearTap by remember { mutableLongStateOf(0L) }

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
                            detectTapGestures {
                                val now = SystemClock.elapsedRealtime()
                                yearTapCount = if (now - lastYearTap <= 1_200L) yearTapCount + 1 else 1
                                lastYearTap = now
                                if (yearTapCount == 3) {
                                    yearTapCount = 0
                                    onReveal()
                                }
                            }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monthName(visibleYear, visibleMonth),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = { changeMonth(-1) }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = previousMonthDescription(),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { changeMonth(1) }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = nextMonthDescription(),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                WeekdayHeader()
                Spacer(Modifier.height(12.dp))
                MonthGrid(visibleYear, visibleMonth, today)
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
    val cellCount = ((leadingEmptyCells + daysInMonth + 6) / 7) * 7
    val availableWidth = LocalConfiguration.current.screenWidthDp.dp - 56.dp
    val maxWidth = 680.dp - 56.dp
    val cellSize = minOf(availableWidth, maxWidth) / 7f
    val fontSize = 18.sp

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        modifier = Modifier.weight(1f).height(cellSize.coerceIn(44.dp, 64.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day in 1..daysInMonth) {
                            Box(
                                modifier = Modifier
                                    .size(cellSize.coerceIn(42.dp, 54.dp))
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
                    }
                }
            }
        }
    }
}

private fun monthName(year: Int, month: Int): String =
    if (Locale.getDefault().language == "zh") "${month + 1}月"
    else SimpleDateFormat("LLLL", Locale.getDefault()).format(GregorianCalendar(year, month, 1).time)

private fun previousMonthDescription(): String = if (Locale.getDefault().language == "zh") "上个月" else "Previous month"

private fun nextMonthDescription(): String = if (Locale.getDefault().language == "zh") "下个月" else "Next month"
