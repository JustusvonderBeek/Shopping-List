package com.cloudsheeptech.shoppinglist.util

import com.cloudsheeptech.shoppinglist.data.receiptItemAndDescriptionMapping.ReceiptItemMapping
import com.cloudsheeptech.shoppinglist.data.recipe.ApiIngredient

class ConversionUtil {

    companion object {

        fun convertToReceiptItemMapping(
            item: ApiIngredient,
            recipeId: Long,
            createdBy: Long
        ): ReceiptItemMapping {
            return ReceiptItemMapping(
                0L,
                recipeId,
                createdBy,
                item.id,
                item.quantity,
                item.quantityType,
            )
        }

        // TODO: This is not finished yet, include the name and icon
        fun convertToApiIngredient(item: ReceiptItemMapping): ApiIngredient {
            return ApiIngredient(
                item.id,
                "",
                "",
                item.quantity,
                item.quantityType
            )
        }

    }

}