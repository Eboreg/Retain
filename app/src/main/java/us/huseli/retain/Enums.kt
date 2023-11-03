package us.huseli.retain

object Enums {
    enum class NoteType { TEXT, CHECKLIST }

    enum class Side { LEFT, RIGHT }

    enum class HomeScreenViewType { LIST, GRID }

    enum class SyncBackend(val displayName: String) {
        NONE("None"),
        NEXTCLOUD("Nextcloud"),
        SFTP("SFTP"),
        DROPBOX("Dropbox"),
    }
}
