package com.cloudsheeptech.shoppinglist.database.migrations

import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec

class AppDatabaseMigration {
    @RenameColumn.Entries(
        RenameColumn(
            tableName = "list_table",
            fromColumnName = "CreatedBy",
            toColumnName = "createdBy",
        ),
        RenameColumn(
            tableName = "list_table",
            fromColumnName = "ID",
            toColumnName = "listId",
        ),
        RenameColumn(
            tableName = "list_table",
            fromColumnName = "Name",
            toColumnName = "title",
        ),
        RenameColumn(
            tableName = "list_table",
            fromColumnName = "CreatedByName",
            toColumnName = "createdByName",
        ),
        RenameColumn(
            tableName = "list_table",
            fromColumnName = "LastEdited",
            toColumnName = "lastUpdated",
        ),
        RenameColumn(
            tableName = "items",
            fromColumnName = "ID",
            toColumnName = "id",
        ),
        RenameColumn(
            tableName = "items",
            fromColumnName = "Name",
            toColumnName = "name",
        ),
        RenameColumn(
            tableName = "items",
            fromColumnName = "Icon",
            toColumnName = "icon",
        ),
        RenameColumn(
            tableName = "user",
            fromColumnName = "UserId",
            toColumnName = "OnlineID",
        ),
    )
    class Database19To20Migration : AutoMigrationSpec

    @RenameColumn.Entries(
        RenameColumn(
            tableName = "online_user",
            fromColumnName = "ID",
            toColumnName = "onlineId",
        ),
        RenameColumn(
            tableName = "online_user",
            fromColumnName = "Name",
            toColumnName = "username",
        ),
    )
    class Database20To21Migration : AutoMigrationSpec

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
