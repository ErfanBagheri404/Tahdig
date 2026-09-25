package com.erfanbagheri.tahdig.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.platform.testTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * #129 — one swipe per row, and the swipe reaches the verbs.
 *
 * Two invariants the static audit cannot check:
 *
 * 1. [oneA11yStop] merges its subtree: one node carries the spoken label
 *    and the two Text children are NOT separately focusable (otherwise the
 *    merge silently failed and each dish still costs three swipes).
 * 2. [SwipeActionRow] exposes BOTH swipe actions as semantics custom actions,
 *    so a screen-reader user who cannot perform the gesture still reaches the
 *    verbs from the row's own actions menu.
 *
 * Robolectric + ui-test, no emulator: rows render without ViewModels.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class A11yRowSemanticsTest {

    @get:Rule
    val compose = createComposeRule()

    private fun row(title: String, meta: String) {
        compose.setContent {
            Column(
                modifier = Modifier
                    .testTag("row")
                    .oneA11yStop("$title، $meta"),
            ) {
                Text(title)
                Text(meta)
            }
        }
    }

    @Test
    fun mergedRow_isOneNode_carryingTheSpokenLabel() {
        row("قورمه‌سبزی", "ناهار")
        // The merged label sits on the PARENT (the oneA11yStop node), not on a
        // Text — `onNodeWithText` queries Text nodes and never sees it.
        compose.onNodeWithTag("row").assert(hasContentDescription("قورمه‌سبزی، ناهار"))
        // Merge worked: the children collapsed into the parent, so the raw
        // title is no longer its own node.
        compose.onAllNodesWithContentDescription("قورمه‌سبزی، ناهار").assertCountEquals(1)
        val rowNode = compose.onNodeWithTag("row").fetchSemanticsNode()
        assert(rowNode.children.isEmpty()) { "expected merged children, got ${rowNode.children.size}" }
    }

    @Test
    fun swipeRow_exposesBothActions_withoutTheGesture() {
        var bought = false
        var deleted = false
        compose.setContent {
            SwipeActionRow(
                swipeLeft = RowAction("حذف", Icons.Default.Delete, { deleted = true }, destructive = true),
                swipeRight = RowAction("خریدم", Icons.Default.Check, { bought = true }),
            ) { Text("پیاز") }
        }
        // The verbs live on the row's OWN node (the Box that carries them),
        // with the menu still closed. Compared by LABEL: the data class
        // `equals` includes the lambda, so instance equality would always fail.
        val hasBothActions = SemanticsMatcher(
            "both swipe verbs exposed as custom actions",
        ) { node ->
            val labels = node.config.getOrElse(SemanticsActions.CustomActions) { emptyList() }
                .map { it.label }
            "حذف" in labels && "خریدم" in labels
        }
        compose.onNode(hasBothActions).assertExists()
        assert(!bought && !deleted)
    }
}
