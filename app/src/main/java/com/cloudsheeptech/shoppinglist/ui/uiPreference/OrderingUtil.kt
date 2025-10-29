package com.cloudsheeptech.shoppinglist.ui.uiPreference

import android.content.res.Resources
import com.cloudsheeptech.shoppinglist.R

class OrderingUtil {
    companion object {
        fun orderingStringToEnum(
            resources: Resources,
            orderingString: String,
        ): Ordering =
            when (orderingString) {
                resources.getString(R.string.ordering_default_key) -> Ordering.DEFAULT
                resources.getString(R.string.ordering_alphabet_key) -> Ordering.ALPHABETICAL
                resources.getString(R.string.ordering_alphabet_rev_key) -> Ordering.ALPHABETICAL_REVERSE
                resources.getString(R.string.ordering_checked_key) -> Ordering.CHECKED_LAST
                resources.getString(R.string.ordering_supermarket_key) -> Ordering.SUPERMARKET_ODER
                else -> Ordering.DEFAULT
            }
    }
}
