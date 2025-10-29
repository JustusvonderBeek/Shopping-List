package com.cloudsheeptech.shoppinglist.util

import com.cloudsheeptech.shoppinglist.recipe.model.ApiIngredient
import com.cloudsheeptech.shoppinglist.recipe.model.ReceiptItemMapping

class ConversionUtil {
    companion object {
        fun convertToReceiptItemMapping(
            item: ApiIngredient,
            recipeId: Long,
            createdBy: Long,
        ): ReceiptItemMapping =
            ReceiptItemMapping(
                0L,
                recipeId,
                createdBy,
                item.id,
                item.quantity,
                item.quantityType,
            )

        // TODO: This is not finished yet, include the name and icon
        fun convertToApiIngredient(item: ReceiptItemMapping): ApiIngredient =
            ApiIngredient(
                item.id,
                "",
                "",
                item.quantity,
                item.quantityType,
            )
    }
}
