package us.huseli.retain.syncbackend.tasks.abstr

import us.huseli.retain.syncbackend.Engine
import us.huseli.retain.syncbackend.tasks.result.OperationTaskResult


abstract class AbstractOperationTask<ET : Engine, RT : OperationTaskResult>(engine: ET) : AbstractTask<ET, RT>(engine)
