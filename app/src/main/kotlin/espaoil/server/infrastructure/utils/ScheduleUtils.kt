package espaoil.server.infrastructure.utils

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.regex.Pattern

data class ScheduleBlock(
    val days: String,
    val hours: String
)

data class ParsedSchedule(
    val is24h: Boolean,
    val blocks: List<ScheduleBlock>,
    val raw: String,
    val confidence: String // "high" | "low"
)

enum class ScheduleOpenStatus {
    OPEN, CLOSED, UNKNOWN
}

data class ScheduleLiveStatus(
    val status: ScheduleOpenStatus,
    val nextOpening: LocalDateTime? = null,
    val nextOpeningLabel: String? = null
)

object ScheduleUtils {
    private val DAY_BLOCK_REGEX = Pattern.compile("^([^:]+?)\\s*:\\s*(.+)$")
    private val DAY_WITH_24H_REGEX = Pattern.compile("^([A-Za-zÁÉÍÓÚÑ\\-\\s]+?)\\s+(24\\s*H|24/7)$", Pattern.CASE_INSENSITIVE)
    private val HOURS_RANGE_REGEX = Pattern.compile("^\\d{1,2}:\\d{2}\\s*[-–]\\s*\\d{1,2}:\\d{2}$")

    private val DAY_TOKEN_TO_INDEX = mapOf(
        "L" to DayOfWeek.MONDAY, "LU" to DayOfWeek.MONDAY, "LUN" to DayOfWeek.MONDAY, "LUNES" to DayOfWeek.MONDAY,
        "M" to DayOfWeek.TUESDAY, "MA" to DayOfWeek.TUESDAY, "MAR" to DayOfWeek.TUESDAY, "MARTES" to DayOfWeek.TUESDAY,
        "X" to DayOfWeek.WEDNESDAY, "MI" to DayOfWeek.WEDNESDAY, "MIE" to DayOfWeek.WEDNESDAY, "MIERCOLES" to DayOfWeek.WEDNESDAY,
        "J" to DayOfWeek.THURSDAY, "JU" to DayOfWeek.THURSDAY, "JUE" to DayOfWeek.THURSDAY, "JUEVES" to DayOfWeek.THURSDAY,
        "V" to DayOfWeek.FRIDAY, "VI" to DayOfWeek.FRIDAY, "VIE" to DayOfWeek.FRIDAY, "VIERNES" to DayOfWeek.FRIDAY,
        "S" to DayOfWeek.SATURDAY, "SA" to DayOfWeek.SATURDAY, "SAB" to DayOfWeek.SATURDAY, "SABADO" to DayOfWeek.SATURDAY,
        "D" to DayOfWeek.SUNDAY, "DO" to DayOfWeek.SUNDAY, "DOM" to DayOfWeek.SUNDAY, "DOMINGO" to DayOfWeek.SUNDAY
    )

    private val DAY_ORDER = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    )

    private fun normalizeText(value: String): String =
        value.replace("\\s+".toRegex(), " ").replace("\\s*[-–]\\s*".toRegex(), "-").trim()

    private fun is24hToken(value: String): Boolean =
        normalizeText(value).matches("^(24\\s*H|24/7)$".toRegex(RegexOption.IGNORE_CASE))

    private fun hasRangeHours(value: String): Boolean =
        HOURS_RANGE_REGEX.matcher(normalizeText(value)).matches()

    private fun normalizeDayToken(value: String): String =
        java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .uppercase()
            .replace(".", "")
            .replace("\\s+".toRegex(), "")
            .trim()

    private fun dayTokenToDayOfWeek(token: String): DayOfWeek? {
        val normalized = normalizeDayToken(token)
        return DAY_TOKEN_TO_INDEX[normalized]
    }

    private fun parseDaysExpression(days: String): Set<DayOfWeek>? {
        val normalized = normalizeText(days).uppercase()
        if (normalized.isEmpty()) return null

        val compact = normalized.replace("\\s+".toRegex(), "")
        if (compact == "L-D") {
            return DayOfWeek.values().toSet()
        }

        val rangeParts = compact.split("-")
        if (rangeParts.size == 2) {
            val startToken = rangeParts[0].substring(0, 1)
            val endToken = rangeParts[1].substring(0, 1)
            val startDay = dayTokenToDayOfWeek(startToken)
            val endDay = dayTokenToDayOfWeek(endToken)

            if (startDay != null && endDay != null) {
                val set = mutableSetOf<DayOfWeek>()
                var current: DayOfWeek = startDay
                while (true) {
                    set.add(current)
                    if (current == endDay) break
                    current = current.plus(1)
                }
                return set
            }
        }

        val parts = normalized.split("[,/]".toRegex()).map { normalizeText(it) }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null

        val result = mutableSetOf<DayOfWeek>()
        for (part in parts) {
            val day = dayTokenToDayOfWeek(part) ?: return null
            result.add(day)
        }
        return result
    }

    private fun parseMinutes(timeValue: String): Int? {
        val parts = timeValue.split(":")
        if (parts.size != 2) return null
        val hours = parts[0].toIntOrNull() ?: return null
        val minutes = parts[1].toIntOrNull() ?: return null

        if (hours < 0 || hours > 24 || minutes < 0 || minutes > 59) return null
        if (hours == 24 && minutes != 0) return null

        return hours * 60 + minutes
    }

    private fun parseHourRange(hours: String): Pair<Int, Int>? {
        val normalized = normalizeText(hours)
        if (!hasRangeHours(normalized)) return null

        val parts = normalized.split("-").map { it.trim() }
        if (parts.size != 2) return null
        val start = parseMinutes(parts[0]) ?: return null
        val end = parseMinutes(parts[1]) ?: return null

        return start to end
    }

    fun parseSchedule(schedule: String?): ParsedSchedule {
        val raw = normalizeText(schedule ?: "")
        if (raw.isEmpty()) {
            return ParsedSchedule(is24h = false, blocks = emptyList(), raw = raw, confidence = "low")
        }

        val segments = raw.split("[;|]+".toRegex()).map { it.trim() }.filter { it.isNotEmpty() }
        val blocks = mutableListOf<ScheduleBlock>()
        var lowConfidence = false
        var has24hBlock = false

        for (segment in segments) {
            val dayBlockMatch = DAY_BLOCK_REGEX.matcher(segment)
            if (dayBlockMatch.matches()) {
                val days = normalizeText(dayBlockMatch.group(1))
                val hours = normalizeText(dayBlockMatch.group(2))

                if (is24hToken(hours)) has24hBlock = true
                if (!is24hToken(hours) && !hasRangeHours(hours)) lowConfidence = true

                blocks.add(ScheduleBlock(days, hours))
                continue
            }

            val day24hMatch = DAY_WITH_24H_REGEX.matcher(segment)
            if (day24hMatch.matches()) {
                blocks.add(ScheduleBlock(normalizeText(day24hMatch.group(1)), "24H"))
                has24hBlock = true
                continue
            }

            if (is24hToken(segment)) {
                blocks.add(ScheduleBlock("L-D", "24H"))
                has24hBlock = true
                continue
            }

            lowConfidence = true
        }

        val is24h = blocks.isNotEmpty() &&
                blocks.all { is24hToken(it.hours) } &&
                (blocks.any { it.days.uppercase() == "L-D" } || blocks.size == 1 || has24hBlock)

        return ParsedSchedule(
            is24h = is24h,
            blocks = blocks,
            raw = raw,
            confidence = if (lowConfidence) "low" else "high"
        )
    }

    fun getLiveStatus(parsed: ParsedSchedule, now: LocalDateTime = LocalDateTime.now(ZoneId.of("Europe/Madrid"))): ScheduleLiveStatus {
        if (parsed.blocks.isEmpty() || parsed.confidence == "low") {
            return ScheduleLiveStatus(ScheduleOpenStatus.UNKNOWN)
        }

        val currentDay = now.dayOfWeek
        val currentMinutes = now.hour * 60 + now.minute
        var hasEvaluableBlock = false

        for (block in parsed.blocks) {
            val daySet = parseDaysExpression(block.days) ?: continue
            hasEvaluableBlock = true

            if (is24hToken(block.hours)) {
                if (daySet.contains(currentDay)) {
                    return ScheduleLiveStatus(ScheduleOpenStatus.OPEN)
                }
                continue
            }

            val range = parseHourRange(block.hours) ?: continue
            if (range.first <= range.second) {
                if (daySet.contains(currentDay) && currentMinutes >= range.first && currentMinutes < range.second) {
                    return ScheduleLiveStatus(ScheduleOpenStatus.OPEN)
                }
            } else {
                // Rango nocturno (ej. 22:00-06:00)
                val previousDay = currentDay.minus(1)
                if ((daySet.contains(currentDay) && currentMinutes >= range.first) || 
                    (daySet.contains(previousDay) && currentMinutes < range.second)) {
                    return ScheduleLiveStatus(ScheduleOpenStatus.OPEN)
                }
            }
        }

        if (!hasEvaluableBlock) return ScheduleLiveStatus(ScheduleOpenStatus.UNKNOWN)

        val nextOpening = findNextOpening(parsed, now)
        return ScheduleLiveStatus(
            status = ScheduleOpenStatus.CLOSED,
            nextOpening = nextOpening,
            nextOpeningLabel = nextOpening?.let { formatNextOpeningLabel(it, now) }
        )
    }

    private fun findNextOpening(parsed: ParsedSchedule, now: LocalDateTime): LocalDateTime? {
        val baseDay = now.toLocalDate().atStartOfDay()
        val candidates = mutableListOf<LocalDateTime>()

        for (offset in 0..7) {
            val dayDate = baseDay.plusDays(offset.toLong())
            val dayOfWeek = dayDate.dayOfWeek

            for (block in parsed.blocks) {
                val daySet = parseDaysExpression(block.days)
                if (daySet == null || !daySet.contains(dayOfWeek)) continue

                if (is24hToken(block.hours)) {
                    val openingAt = dayDate
                    if (openingAt.isAfter(now)) candidates.add(openingAt)
                    continue
                }

                val range = parseHourRange(block.hours) ?: continue
                val openingAt = dayDate.plusMinutes(range.first.toLong())
                if (openingAt.isAfter(now)) candidates.add(openingAt)
            }
        }

        return candidates.minByOrNull { it }
    }

    private fun formatNextOpeningLabel(nextOpening: LocalDateTime, now: LocalDateTime): String {
        val openingDate = nextOpening.toLocalDate()
        val nowDate = now.toLocalDate()
        val diffDays = ChronoUnit.DAYS.between(nowDate, openingDate)
        
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val hour = nextOpening.format(timeFormatter)

        return when (diffDays) {
            0L -> "hoy a las $hour"
            1L -> "mañana a las $hour"
            else -> {
                val dayName = nextOpening.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
                "${dayName.replaceFirstChar { it.uppercase() }} a las $hour"
            }
        }
    }
}
