package com.cloudsheeptech.shoppinglist.data

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

class OffsetDateTimeUtil {
    companion object {
        fun areDateTimesEqual(
            dateTime1: OffsetDateTime,
            dateTime2: OffsetDateTime,
        ): Boolean = dateTime1.truncatedTo(ChronoUnit.SECONDS).isEqual(dateTime2.truncatedTo(ChronoUnit.SECONDS))
    }
}
