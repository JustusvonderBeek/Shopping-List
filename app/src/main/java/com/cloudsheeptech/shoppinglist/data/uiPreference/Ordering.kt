package com.cloudsheeptech.shoppinglist.data.uiPreference

enum class Ordering(
    val position: Int,
) {
    DEFAULT(0),
    ALPHABETICAL(1),
    ALPHABETICAL_REVERSE(2),
    CHECKED_LAST(3),
    SUPERMARKET_ODER(4),
}
