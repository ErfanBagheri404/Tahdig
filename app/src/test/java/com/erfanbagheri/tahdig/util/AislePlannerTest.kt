package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Test

/** Group-reordering and snapshot-numbering rules for the store route (#108). */
class AislePlannerTest {

    private val groups = mapOf(
        "غلات" to listOf("برنج"),
        "پروتئین" to listOf("مرغ"),
        "لبنیات" to listOf("ماست"),
    )

    @Test
    fun `custom order wins, OTHER last`() {
        val secs = AislePlanner.plan(
            groups + (AislePlanner.OTHER to listOf("نمک")),
            order = listOf("لبنیات", "پروتئین"),
        )
        assertEquals(listOf("لبنیات", "پروتئین", "غلات", AislePlanner.OTHER), secs.map { it.header })
    }

    @Test
    fun `renames apply to headers only, rows untouched`() {
        val secs = AislePlanner.plan(groups, renames = mapOf("پروتئین" to "گوشت"))
        val renamed = secs.first { it.rows == listOf("مرغ") }
        assertEquals("گوشت", renamed.header)
    }

    @Test
    fun `hidden aisles fold into OTHER, nothing dropped`() {
        val secs = AislePlanner.plan(groups, hidden = setOf("پروتئین"))
        val other = secs.single { it.header == AislePlanner.OTHER }
        assertEquals(listOf("مرغ"), other.rows)
        assertEquals(3, secs.sumOf { it.rows.size })
    }

    @Test
    fun `trip counts`() {
        val (total, bought) = AislePlanner.tripCounts(
            listOf("a" to true, "b" to false, "c" to true),
        )
        assertEquals(3, total)
        assertEquals(2, bought)
    }
}
