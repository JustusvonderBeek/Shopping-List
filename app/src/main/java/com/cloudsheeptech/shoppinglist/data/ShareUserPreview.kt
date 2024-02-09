package com.cloudsheeptech.shoppinglist.data

data class ShareUserPreview(
    var UserId : Long,
    var Name : String,
    var Shared : Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (other is ShareUserPreview) {
            return other.UserId == this.UserId
        }
        return false
    }
}
