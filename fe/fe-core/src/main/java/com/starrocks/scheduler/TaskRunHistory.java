// Copyright 2021-present StarRocks, Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


package com.starrocks.scheduler;

import com.google.common.collect.Maps;
import com.starrocks.common.Config;
import com.starrocks.scheduler.persist.TaskRunStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TaskRunHistory {
    private static final Logger LOG = LogManager.getLogger(TaskRunHistory.class);
    private static final int MEMORY_TASK_RUN_SAMPLES = 10;

    // Thread-Safe history map: QueryId -> TaskRunStatus
    // The same task-id may contain multi history task run status, so use query_id instead.
    private final Map<String, TaskRunStatus> historyTaskRunMap =
            Collections.synchronizedMap(Maps.newLinkedHashMap());

    public void addHistory(TaskRunStatus status) {
        historyTaskRunMap.put(status.getQueryId(), status);
    }

    public TaskRunStatus getTask(String queryId) {
        if (queryId == null) {
            return null;
        }
        return historyTaskRunMap.get(queryId);
    }

    public void removeTask(String queryId) {
        if (queryId == null) {
            return;
        }
        historyTaskRunMap.remove(queryId);
    }

    // Reserve historyTaskRunMap values to keep the last insert at the first.
    public List<TaskRunStatus> getAllHistory() {
        List<TaskRunStatus> historyRunStatus =
                new ArrayList<>(historyTaskRunMap.values());
        Collections.reverse(historyRunStatus);
        return historyRunStatus;
    }

    public void forceGC() {
        List<TaskRunStatus> allHistory = getAllHistory();
        int beforeSize = allHistory.size();
        LOG.info("try to trigger force gc, size before GC:{}, task_runs_max_history_number:{}.", beforeSize,
                Config.task_runs_max_history_number);
        if (beforeSize <= Config.task_runs_max_history_number) {
            return;
        }
        int startIndex = Math.min(allHistory.size(), Config.task_runs_max_history_number);
        // Creating a new list for elements to be removed to avoid ConcurrentModificationException
        List<String> idsToRemove = allHistory.subList(startIndex, beforeSize)
                .stream()
                .map(TaskRunStatus::getQueryId)
                .collect(Collectors.toList());

        // Now remove outside of iteration
        idsToRemove.forEach(this::removeTask);
        LOG.warn("Too much task metadata triggers forced task_run GC, " +
                "size before GC:{}, size after GC:{}.", beforeSize, historyTaskRunMap.size());
    }

    public long getTaskRunCount() {
        return historyTaskRunMap.size();
    }

    public synchronized List<Object> getSamplesForMemoryTracker() {
        return historyTaskRunMap.values()
                .stream()
                .limit(MEMORY_TASK_RUN_SAMPLES)
                .collect(Collectors.toList());
    }
}
