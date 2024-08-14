package com.cloudsheeptech.shoppinglist.data.items

import android.content.Context

class ItemClassifier {

    enum class ItemClass {
        DEFAULT,
        FRUITS,
        VEGETABLES,
        TEA,
        COFFEE,
        SWEETS,
        CEREAL,
        MEAT,
        FISH,
        CHEESE,
        SAUSAGE,
        MILK,
        OTHERS,
        DRINKS,
        HYGIENE
    }

    private val classifierStringMap : Map<String, ItemClass> = emptyMap()

    private fun loadStringsFromResources(context: Context) {
        
    }

    companion object {

        fun convertStringToItemClass(item : String) : ItemClass {
            return ItemClass.DEFAULT
        }
    }

}