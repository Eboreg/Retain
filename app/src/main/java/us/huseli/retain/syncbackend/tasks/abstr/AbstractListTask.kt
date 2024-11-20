package us.huseli.retain.syncbackend.tasks.abstr

import us.huseli.retain.syncbackend.Engine
import us.huseli.retain.syncbackend.tasks.result.TaskResult


abstract class AbstractListTask<ET : Engine, CRT : TaskResult, CT : AbstractTask<ET, CRT>, LT>(
    engine: ET,
    objects: Collection<LT>
) : AbstractBaseListTask<ET, TaskResult, CRT, CT, LT>(engine, objects)
