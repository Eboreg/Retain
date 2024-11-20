package us.huseli.retain.syncbackend.tasks.result

open class TaskResult(
    open val status: Status,
    open val message: String? = null,
    open val exception: Exception? = null,
) {
    enum class Status { OK, UNKNOWN_HOST, CONNECT_ERROR, AUTH_ERROR, PATH_NOT_FOUND, OTHER_ERROR }

    val success
        get() = status == Status.OK
    val hasNetworkError
        get() = listOf(Status.CONNECT_ERROR, Status.AUTH_ERROR, Status.UNKNOWN_HOST).contains(status)

    fun copy(status: Status = this.status, message: String? = this.message, exception: Exception? = this.exception) =
        TaskResult(status, message, exception)
}