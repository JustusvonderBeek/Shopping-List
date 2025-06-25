package com.cloudsheeptech.shoppinglist

import com.cloudsheeptech.shoppinglist.data.core.DataSource

class TestDataSource<T, ID> : DataSource<T, ID> {
    var handedData: T? = null

    override suspend fun create(entity: T): T {
        handedData = entity
        return entity
    }

    override suspend fun read(identifier: ID): T {
        TODO("Not yet implemented")
    }

    override suspend fun readAll(): List<T> {
        TODO("Not yet implemented")
    }

    override suspend fun update(entity: T): T {
        TODO("Not yet implemented")
    }

    override suspend fun delete(identifier: ID): Boolean {
        TODO("Not yet implemented")
    }

    fun compareWithExpected(expected: T): Boolean = expected == handedData

    fun compareWithExpectedSerialized(expected: String): Boolean = expected == handedData.toString()
}
