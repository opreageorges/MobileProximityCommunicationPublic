package com.ogeorges.mobileproximitycommunication.localdatabase.entities

import androidx.room.*
import com.ogeorges.mobileproximitycommunication.localdatabase.TimeConverter

@Entity(tableName = "messages", foreignKeys = [ForeignKey(
    entity = DBFriend::class,
    parentColumns = ["public_key"],
    childColumns = ["sender_pk"],
    onDelete = ForeignKey.CASCADE
)]
)
data class DBMessage (
    @PrimaryKey(autoGenerate = true) val uid: Int?,
    @ColumnInfo(name = "body") val body: String,
    @ColumnInfo(name = "sender_pk") val senderId: String,
    @ColumnInfo(name = "sender_type") val senderType: String, // me or you
    @ColumnInfo(name = "is_received") var isReceived: Boolean,
    @ColumnInfo(name = "timestamp") @TypeConverters(TimeConverter::class) val timestamp: Long
)