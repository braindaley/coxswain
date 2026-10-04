package svenmeier.coxswain.pete

import android.content.Context
import org.json.JSONObject
import svenmeier.coxswain.gym.Difficulty
import svenmeier.coxswain.gym.Program
import svenmeier.coxswain.gym.Segment
import kotlin.math.roundToInt

/** The supplied 24-week plan, stored as versioned app data rather than parsed coaching prose. */
class PetePlanCatalog private constructor(val weeks: List<List<PeteSession>>) {
    fun session(week: Int, index: Int): PeteSession = weeks[week - 1][index]

    companion object {
        fun load(context: Context): PetePlanCatalog = parse(
            context.assets.open("petes_plan.json").bufferedReader().use { it.readText() }
        )

        fun parse(json: String): PetePlanCatalog {
            val root = JSONObject(json)
            require(root.getInt("version") == 1)
            val source = root.getJSONArray("weeks")
            require(source.length() == 24)
            val weeks = (0 until source.length()).map { weekIndex ->
                val rows = source.getJSONArray(weekIndex)
                require(rows.length() == 5)
                (0 until rows.length()).map { index ->
                    val row = rows.getJSONObject(index)
                    val label = row.getString("name")
                    val target = PeteSession.parseTarget(label)
                    PeteSession(
                        week = weekIndex + 1,
                        index = index,
                        name = label,
                        optional = row.getBoolean("optional"),
                        note = row.getString("note"),
                        pieces = target.first,
                        amount = target.second,
                        unit = target.third,
                        restSeconds = target.fourth
                    ).also { require(it.optional == (index >= 3)) }
                }
            }
            return PetePlanCatalog(weeks)
        }
    }
}

data class PeteSession(
    val week: Int,
    val index: Int,
    val name: String,
    val optional: Boolean,
    val note: String,
    val pieces: Int,
    val amount: Int,
    val unit: Unit,
    val restSeconds: Int
) {
    enum class Unit { METERS, MINUTES }

    val required get() = !optional
    val plannedWorkSeconds get() = if (unit == Unit.MINUTES) pieces * amount * 60 else 0
    val plannedDistance get() = if (unit == Unit.METERS) pieces * amount else 0
    val totalRestSeconds get() = (pieces - 1) * restSeconds

    fun estimatedMinutes(splitSeconds: Int): Int {
        val work = if (unit == Unit.METERS) plannedDistance.toDouble() * splitSeconds / 500.0
            else plannedWorkSeconds.toDouble()
        return ((work + totalRestSeconds) / 60.0).roundToInt()
    }

    fun program(goal: PeteGoal = PeteGoal.None): Program {
        val program = Program("Pete's Plan · W$week · $name")
        program.getSegments().clear()
        for (piece in 0 until pieces) {
            val work = Segment(Difficulty.MEDIUM)
            work.name.set(if (pieces == 1) name else "Row ${piece + 1}")
            if (unit == Unit.METERS) work.setDistance(amount) else work.setDuration(amount * 60)
            when (goal) {
                is PeteGoal.Speed -> if (goal.firstPieces == 0 || piece < goal.firstPieces) {
                    work.setSpeed((50_000.0 / goal.splitSeconds).roundToInt())
                }
                is PeteGoal.StrokeRateCap -> work.setStrokeRate(goal.spm)
                is PeteGoal.StrokeRate -> work.setStrokeRate(goal.spm)
                else -> {}
            }
            program.addSegment(work)
            if (piece < pieces - 1) {
                val rest = Segment(Difficulty.REST).setDuration(restSeconds)
                rest.name.set("Rest ${piece + 1}")
                program.addSegment(rest)
            }
        }
        return program
    }

    companion object {
        private val PATTERN = Regex("^(?:(\\d+) × )?(\\d+)(m|min)(?: · (\\d+)min rest)?$")
        fun parseTarget(label: String): Quad<Int, Int, Unit, Int> {
            val match = requireNotNull(PATTERN.matchEntire(label.replace(",", ""))) { "Unknown Pete's Plan target: $label" }
            val pieces = match.groupValues[1].toIntOrNull() ?: 1
            val amount = match.groupValues[2].toInt()
            val unit = if (match.groupValues[3] == "m") Unit.METERS else Unit.MINUTES
            val rest = (match.groupValues[4].toIntOrNull() ?: 0) * 60
            require(pieces >= 1 && amount > 0 && (pieces == 1 || rest > 0))
            return Quad(pieces, amount, unit, rest)
        }
    }
}

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
