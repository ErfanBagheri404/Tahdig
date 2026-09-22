package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An archived shopping trip (#108): date + counts + the rows as they were when
 * the trip ended. Read-only history — it lives in its own table so clearing
 * the live list never touches it.
 */
@Entity(tableName = "shopping_trips")
data class ShoppingTripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** When «پایان سفر خرید» was pressed (epoch millis). */
    @ColumnInfo(name = "ended_at") val endedAt: Long,
    /** Rows on the list at archive time. */
    @ColumnInfo(name = "total") val total: Int,
    /** Rows ticked off in-store at archive time. */
    @ColumnInfo(name = "bought") val bought: Int,
    /** JSON snapshot: [{item, checked}] — read-only for the history view. */
    @ColumnInfo(name = "items_json") val itemsJson: String,
)
