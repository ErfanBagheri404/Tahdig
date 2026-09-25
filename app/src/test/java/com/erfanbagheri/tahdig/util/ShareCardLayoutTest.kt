package com.erfanbagheri.tahdig.util

import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * #132 — every share layout must actually paint, at the size the issue names.
 *
 * NATIVE graphics mode is required: Robolectric's default canvas silently drops
 * every draw call, so a blank bitmap of the right size would pass a size-only
 * check while proving nothing about the card.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShareCardLayoutTest {

    private val dish = FoodEntity(
        id = 3,
        name = "قورمه سبزی",
        nameEn = "Ghormeh Sabzi",
        description = "سبزی را بشویید. لوبیا را بجوشانید. خورش را آرام بپزید.",
        categoryId = 3,
        mealTime = "LUNCH,DINNER",
        cuisine = "IRANI",
        difficulty = "MEDIUM",
        prepTimeMin = 90,
        ingredients = "سبزی خوردن،لوبیا قرمز،گوشت گوسفند",
    )

    private fun render(layout: ShareLayout) = ShareCard.render(
        ApplicationProvider.getApplicationContext(), dish, layout,
    )

    @Test
    fun `every layout writes a 1080x1350 png`() {
        for (layout in ShareLayout.entries) {
            val file = render(layout)
            assertTrue("${layout.label} produced no file", file.exists())
            assertTrue("${layout.label} is empty", file.length() > 0)

            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            assertTrue("${layout.label} is not a readable png", bmp != null)
            assertEquals("${layout.label} width", 1080, bmp.width)
            assertEquals("${layout.label} height", 1350, bmp.height)
        }
    }

    @Test
    fun `layouts are not the same image`() {
        // A layout enum that silently does nothing is the failure this catches.
        val bytes = ShareLayout.entries.map { layout ->
            render(layout).readBytes().toList()
        }
        assertEquals("two layouts rendered identically", 3, bytes.distinct().size)
    }

    @Test
    fun `each layout writes its own file so the chooser never serves a stale card`() {
        val names = ShareLayout.entries.map { it.name }
        val files = ShareLayout.entries.map { render(it).name }
        assertEquals(names.size, files.distinct().size)
        files.forEach { assertTrue("$it should be tagged with its layout", it.contains("tahdig-3-")) }
    }

    @Test
    fun `a dish with no english name still renders`() {
        val bare = dish.copy(nameEn = "", ingredients = "", description = "")
        for (layout in ShareLayout.entries) {
            val file = ShareCard.render(ApplicationProvider.getApplicationContext(), bare, layout)
            assertTrue("${layout.label} crashed on a bare dish", file.length() > 0)
        }
    }
}
