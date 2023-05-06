package us.huseli.retain.data

import kotlinx.coroutines.flow.Flow
import us.huseli.retain.LoggingObject
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Note
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(private val noteDao: NoteDao) : LoggingObject {
    val notes: Flow<List<Note>> = noteDao.loadNotes()

    val checklistItems: Flow<List<ChecklistItem>> = noteDao.loadChecklistItems()

    fun getNote(id: UUID): Flow<Note?> = noteDao.getNote(id)

    suspend fun deleteNotes(notes: Collection<Note>) = noteDao.deleteNotes(notes)

    suspend fun deleteChecklistItem(item: ChecklistItem) = noteDao.deleteChecklistItem(item)

    suspend fun insertChecklistItem(noteId: UUID, text: String, checked: Boolean, position: Int) {
        noteDao.makePlaceForChecklistItem(noteId, position)
        noteDao.insertChecklistItem(UUID.randomUUID(), noteId, text, checked, position)
    }

    fun loadChecklistItems(noteId: UUID): Flow<List<ChecklistItem>> = noteDao.loadChecklistItems(noteId)

    suspend fun updateChecklistItem(item: ChecklistItem, text: String, checked: Boolean, position: Int) {
        if (position != item.position) noteDao.makePlaceForChecklistItem(item.id, item.noteId, position)
        noteDao.updateChecklistItem(item.id, text, checked, position)
    }

    suspend fun upsertChecklistNote(id: UUID, title: String, showChecked: Boolean) {
        noteDao.upsertChecklistNote(id, title, showChecked)
    }

    suspend fun upsertTextNote(id: UUID, title: String, text: String) {
        noteDao.upsertTextNote(id, title, text)
    }
}