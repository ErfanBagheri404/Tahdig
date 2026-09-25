package com.erfanbagheri.tahdig.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import android.content.Intent
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import java.io.InputStream
import java.io.OutputStream

/**
 * Moving `.tahdig.json` files in and out of the app (#132).
 *
 * Tahdig has no backend, so a shared *file* is how a recipe travels: the app
 * writes one, a messenger carries it, the other app imports it. That makes the
 * stream layer the whole feature — everything interesting lives in [RecipeFile].
 *
 * The read/write pair takes plain streams so it is testable without a content
 * resolver; the Context overloads are the only Android-aware part.
 */
object RecipeTransfer {

    /** Suggested file name for a single-dish export. */
    fun fileName(food: FoodEntity): String =
        "tahdig-${food.id}.${RecipeFile.EXTENSION}"

    fun writeTo(out: OutputStream, recipes: List<RecipeFile.Recipe>) {
        out.use { it.write(RecipeFile.encode(recipes).toByteArray(Charsets.UTF_8)) }
    }

    /**
     * @throws RecipeFile.InvalidRecipeFile with a user-facing Persian message, so a
     *   corrupt file never reaches the database.
     */
    fun readFrom(input: InputStream): List<RecipeFile.Recipe> =
        RecipeFile.decode(input.use { it.readBytes().toString(Charsets.UTF_8) })

    fun exportTo(context: Context, uri: Uri, recipes: List<RecipeFile.Recipe>) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { writeTo(it, recipes) }
            ?: throw RecipeFile.InvalidRecipeFile("فایل قابل نوشتن نیست")
    }

    fun importFrom(context: Context, uri: Uri): List<RecipeFile.Recipe> =
        readFrom(context.contentResolver.openInputStream(uri)
            ?: throw RecipeFile.InvalidRecipeFile("فایل قابل خواندن نیست"))

    /**
     * Share the file itself, rather than a card: the point is that the other
     * person gets a file they can import, not a picture.
     */
    fun shareFile(context: Context, file: java.io.File, subject: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = RecipeFile.MIME
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "ارسال دستور آشپزی"))
    }
}
