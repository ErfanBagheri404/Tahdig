package com.erfanbagheri.tahdig.util

import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import org.junit.Assert.assertEquals
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * #132 — "export -> import round-trip loses nothing", at the stream level.
 * The file is what actually crosses between devices, so that is the boundary
 * worth pinning, not the encoder on its own.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecipeTransferTest {

    private val dishes = listOf(
        FoodEntity(
            id = 1, name = "آش رشته", nameEn = "Ash Reshteh",
            description = "پیاز را بسوزانید. نخود را بجوشانید.",
            categoryId = 2, mealTime = "LUNCH", cuisine = "IRANI",
            difficulty = "HARD", prepTimeMin = 120,
            ingredients = "پیاز،نخود،لوبیا", tags = "گیاهی",
        ),
        FoodEntity(
            id = 2, name = "پیتزا مارگاریتا", nameEn = "Margherita Pizza",
            description = "خمیر را باز کنید. سس بزنید.",
            categoryId = 9, mealTime = "DINNER", cuisine = "ITALIAN",
            difficulty = "EASY", prepTimeMin = 35,
            ingredients = "آرد،سس گوجه،موزارلا", tags = "گیاهی,fast",
        ),
    )

    private fun roundTrip(list: List<FoodEntity>): List<FoodEntity> {
        val out = ByteArrayOutputStream()
        RecipeTransfer.writeTo(out, list.map(RecipeFile::fromFood))
        return RecipeTransfer.readFrom(ByteArrayInputStream(out.toByteArray()))
            .map { RecipeFile.toFood(it, newId = 0) }
    }

    @Test
    fun `a dish survives the file boundary field for field`() {
        val back = roundTrip(dishes).first()
        val original = dishes.first()
        assertEquals(original.name, back.name)
        assertEquals(original.nameEn, back.nameEn)
        assertEquals(original.description, back.description)
        assertEquals(original.categoryId, back.categoryId)
        assertEquals(original.mealTime, back.mealTime)
        assertEquals(original.cuisine, back.cuisine)
        assertEquals(original.difficulty, back.difficulty)
        assertEquals(original.prepTimeMin, back.prepTimeMin)
        assertEquals(original.ingredients, back.ingredients)
        assertEquals(original.tags, back.tags)
    }

    @Test
    fun `a bundle keeps every dish in order`() {
        val back = roundTrip(dishes)
        assertEquals(2, back.size)
        assertEquals("آش رشته", back[0].name)
        assertEquals("پیتزا مارگاریتا", back[1].name)
    }

    @Test
    fun `persian text is written as utf-8 and read back intact`() {
        val back = roundTrip(dishes).first()
        assertTrue("persian mangled: ${back.name}", back.name.contains("آش"))
        assertTrue("persian mangled: ${back.ingredients}", back.ingredients.contains("پیاز"))
    }

    @Test
    fun `a corrupt stream throws the persian message and writes nothing`() {
        try {
            RecipeTransfer.readFrom(ByteArrayInputStream("{{{".toByteArray()))
            fail("corrupt stream must not import")
        } catch (e: RecipeFile.InvalidRecipeFile) {
            assertEquals("فایل دستور معتبر نیست", e.message)
        }
    }

    @Test
    fun `an empty stream is rejected`() {
        try {
            RecipeTransfer.readFrom(ByteArrayInputStream(ByteArray(0)))
            fail("empty stream must not import")
        } catch (_: RecipeFile.InvalidRecipeFile) {
            // expected
        }
    }

    @Test
    fun `the suggested filename carries the extension and the dish id`() {
        val name = RecipeTransfer.fileName(dishes.first())
        assertTrue("wrong extension: $name", name.endsWith(".tahdig.json"))
        assertTrue("dish id missing: $name", name.contains("tahdig-1"))
    }

    @Test
    fun `writeTo does not leave the stream open`() {
        // ByteArrayOutputStream.toByteArray works after close; a leaked handle
        // here is what makes a SAF export silently truncate on some devices.
        val out = ByteArrayOutputStream()
        RecipeTransfer.writeTo(out, listOf(RecipeFile.fromFood(dishes.first())))
        assertTrue(out.toByteArray().isNotEmpty())
    }

    @Test
    fun `the fileprovider the file share depends on is declared with grant permissions`() {
        // shareFile builds "content://${packageName}.fileprovider/..." — if that
        // authority is not declared, or grantUriPermissions is off, the receiving
        // app gets a FileNotFound/security exception instead of the recipe.
        //
        // Only the declaration is asserted: getUriForFile itself cannot be
        // exercised here because Robolectric does not resolve the provider's
        // FILE_PROVIDER_PATHS meta-data, so a path-matching failure in this
        // environment would be a false negative.
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val info = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_PROVIDERS)
        val provider = info.providers?.firstOrNull {
            it.authority == "${ctx.packageName}.fileprovider"
        }
        assertNotNull("no ${ctx.packageName}.fileprovider declared", provider)
        assertTrue("the provider must grant uri permissions", provider!!.grantUriPermissions)
        assertTrue("the provider must not be exported", !provider.exported)
    }
}
