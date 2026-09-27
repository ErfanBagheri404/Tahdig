package com.erfanbagheri.tahdig.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Make a list row ONE TalkBack stop, with its parts spoken in reading order
 * (#129).
 *
 * A row built from a title, a meta line and a state chip is three separate
 * focus stops: a screen-reader user pays three swipes per dish to hear one
 * thing, and can lose the thread between them. Merging is the difference
 * between one dish per swipe and three.
 *
 * [spoken] is passed explicitly rather than harvested from the subtree,
 * because that is the only way to guarantee the ORDER — harvesting yields
 * whatever Compose happened to walk, which put the price before the name.
 *
 * Only for rows with NO inner interactive control. Merging folds a descendant
 * button's action into the parent's, which hides a favourite/delete button
 * behind the actions menu; a row with one of those needs per-row judgement
 * (a custom action or a child stop), not a blanket merge.
 */
fun Modifier.oneA11yStop(spoken: String): Modifier =
    semantics(mergeDescendants = true) { contentDescription = spoken }
