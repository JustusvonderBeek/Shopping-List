package com.cloudsheeptech.shoppinglist.data.database

import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec

class AppDatabaseMigration {
    @RenameColumn.Entries(
        RenameColumn(
            tableName = "shared_table",
            fromColumnName = "ID",
            toColumnName = "ListId",
        ),
        RenameColumn(
            tableName = "shared_table",
            fromColumnName = "createdBy",
            toColumnName = "CreatedBy",
        ),
    )
    class Database34To35Migration : AutoMigrationSpec
}
