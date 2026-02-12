package com.akeel.aitbaar.data.local

import androidx.room.TypeConverter
import com.akeel.aitbaar.data.model.Status
class StatusConverter {

    @TypeConverter
    fun fromStatus(status: Status): String {
        return status.name
    }

    @TypeConverter
    fun toStatus(value: String): Status {
        return Status.valueOf(value)
    }
}
