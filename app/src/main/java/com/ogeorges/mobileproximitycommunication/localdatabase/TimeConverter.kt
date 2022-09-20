package com.ogeorges.mobileproximitycommunication.localdatabase

import androidx.room.TypeConverter
import java.sql.Date

class TimeConverter {

    @TypeConverter
    fun longToDate(value: Long): Date {
        return Date(value)
    }

    @TypeConverter
    fun dateToLong(value: Date): Long {
        return value.time
    }
}