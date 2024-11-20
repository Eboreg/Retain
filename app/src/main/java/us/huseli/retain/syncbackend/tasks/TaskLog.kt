package us.huseli.retain.syncbackend.tasks

data class TaskLog(val simpleName: String, val totalCount: Int = 1, var finishedCount: Int = 0) {
    val isFinished: Boolean
        get() = finishedCount >= totalCount
}