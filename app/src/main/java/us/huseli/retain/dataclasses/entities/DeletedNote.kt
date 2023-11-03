package us.huseli.retain.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class DeletedNote(@ColumnInfo(name = "deletedNoteId") @PrimaryKey val id: UUID)
