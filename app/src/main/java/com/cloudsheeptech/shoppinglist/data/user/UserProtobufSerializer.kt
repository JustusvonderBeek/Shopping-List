package com.cloudsheeptech.shoppinglist.data.user

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.preferences.protobuf.InvalidProtocolBufferException
import com.cloudsheeptech.shoppinglist.UserProto
import java.io.InputStream
import java.io.OutputStream

object UserProtobufSerializer : Serializer<UserProto>  {
    // TODO: https://developer.android.com/topic/libraries/architecture/datastore#groovy

    override val defaultValue: UserProto
        get() = UserProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): UserProto {
        try {
            return UserProto.parseFrom(input)
        } catch (ex : InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", ex)
        }
    }

    override suspend fun writeTo(t: UserProto, output: OutputStream) {
        t.writeTo(output)
    }

}

val Context.userDataStore: DataStore<UserProto> by dataStore(
    fileName = "userinfo.pb",
    serializer = UserProtobufSerializer
)