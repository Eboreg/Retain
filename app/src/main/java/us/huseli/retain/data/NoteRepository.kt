package us.huseli.retain.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import us.huseli.retain.Logger
import us.huseli.retain.LoggingObject
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Note
import us.huseli.retain.data.entities.NoteCombined
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val nextCloud: NextCloud,
    private val ioScope: CoroutineScope,
    override var logger: Logger?,
    private val database: Database,
) : LoggingObject {
    val notes: Flow<List<Note>> = noteDao.loadNotes()
    val checklistItems: Flow<List<ChecklistItem>> = noteDao.loadChecklistItems()

    init {
        syncNextCloud()
    }

    fun getNote(id: UUID): Flow<Note?> = noteDao.loadNote(id)

    suspend fun deleteNotes(ids: Collection<UUID>) = noteDao.deleteNotes(ids)

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
        noteDao.getNote(id)?.let { note ->
            nextCloud.UploadNoteTask(
                note = note,
                checklistItems = noteDao.getChecklistItems(id),
                databaseVersion = database.openHelper.readableDatabase.version
            ).run { task ->
                if (task.success == true) log("Successfully uploaded $note to Nextcloud.")
                else if (task.success == false) log("Couldn't upload $note to Nextcloud.", Log.ERROR)
            }
        }
    }

    suspend fun upsertTextNote(id: UUID, title: String, text: String) {
        noteDao.upsertTextNote(id, title, text)
        noteDao.getNote(id)?.let { note ->
            nextCloud.UploadNoteTask(
                note = note,
                databaseVersion = database.openHelper.readableDatabase.version
            ).run { task ->
                if (task.success == true) log("Successfully uploaded $note to Nextcloud.")
                else if (task.success == false) log("Couldn't upload $note to Nextcloud.", Log.ERROR)
            }
        }
    }

    private suspend fun getNotesCombined(): List<NoteCombined> {
        val notes = noteDao.getNotes()
        val checklistItems = noteDao.getChecklistItems()

        return notes.map { note ->
            NoteCombined(
                note = note,
                checklistItems = checklistItems.filter { it.noteId == note.id },
                databaseVersion = database.openHelper.readableDatabase.version,
            )
        }
    }

    private fun syncNextCloud() {
        nextCloud.DownstreamSyncTask().run { downTask ->
            // Remote files have been fetched and parsed; now update DB where needed.
            ioScope.launch {
                val remoteNotesMap = downTask.remoteNotesCombined.associateBy { it.id }
                val localNotesMap = getNotesCombined().associateBy { it.id }

                // All notes on remote that either don't exist locally, or
                // have a newer timestamp than their local counterparts:
                val remoteUpdated = remoteNotesMap.filter { (id, remote) ->
                    localNotesMap[id]?.let { local -> local < remote } ?: true
                }.values

                // All local notes that either don't exist on remote, or
                // have a newer timestamp than their remote counterparts:
                val localUpdated = localNotesMap.filter { (id, local) ->
                    remoteNotesMap[id]?.let { remote -> remote < local } ?: true
                }.values

                log("New or updated on remote: $remoteUpdated")
                log("New or updated locally: $localUpdated")

                remoteUpdated.forEach { upsertNoteCombined(it) }
                if (remoteUpdated.isNotEmpty()) {
                    log(
                        message = "${remoteUpdated.size} new or updated notes synced from Nextcloud.",
                        addToFlow = true,
                    )
                }

                // Now upload all notes that are new or updated locally:
                nextCloud.UpstreamSyncTask(localUpdated).run { upTask ->
                    if (upTask.successCount > 0 && upTask.failCount > 0) {
                        log(
                            message = "${upTask.successCount} tasks successfully synced to Nextcloud, ${upTask.failCount} failed.",
                            level = Log.ERROR,
                            addToFlow = true,
                        )
                    } else if (upTask.successCount > 0) {
                        log(
                            message = "Successfully synced ${upTask.successCount} notes to Nextcloud.",
                            addToFlow = true,
                        )
                    } else if (upTask.failCount > 0) {
                        log(
                            message = "Failed to sync ${upTask.failCount} notes to Nextcloud.",
                            level = Log.ERROR,
                            addToFlow = true,
                        )
                    }
                }
            }
        }
    }

    private suspend fun upsertNoteCombined(noteCombined: NoteCombined) {
        noteDao.upsertNote(noteCombined)
        noteDao.replaceChecklistItems(noteCombined.id, noteCombined.checklistItems)
    }
}