package us.huseli.retain.syncbackend.tasks.result

import us.huseli.retain.syncbackend.tasks.RemoteFile

class DownloadListJSONTaskResult<T : Any>(
    status: Status,
    message: String? = null,
    exception: Exception? = null,
    remoteFiles: List<RemoteFile> = emptyList(),
    override val objects: List<T> = emptyList(),
) : OperationTaskResult(status, message, exception, remoteFiles, objects = objects)
