/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.client.impl.command;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.zeebe.client.api.commands.WorkflowInstanceCommand;
import io.zeebe.client.api.record.RecordMetadata;
import io.zeebe.client.api.record.ZeebeObjectMapper;
import io.zeebe.client.impl.data.MsgPackConverter;
import io.zeebe.client.impl.record.WorkflowInstanceRecordImpl;

public class WorkflowInstanceCommandImpl extends WorkflowInstanceRecordImpl implements WorkflowInstanceCommand
{
    private WorkflowInstanceCommandName name;

    @JsonCreator
    public WorkflowInstanceCommandImpl(@JacksonInject ZeebeObjectMapper objectMapper, @JacksonInject MsgPackConverter converter)
    {
        super(objectMapper, converter, RecordMetadata.RecordType.COMMAND);
    }

    public WorkflowInstanceCommandImpl(MsgPackConverter converter, WorkflowInstanceCommandName name)
    {
        super(null, converter, RecordMetadata.RecordType.COMMAND);

        this.name = name;
    }

    public WorkflowInstanceCommandImpl(WorkflowInstanceRecordImpl base, WorkflowInstanceCommandName name)
    {
        super(base, name.name());

        this.name = name;
    }

    @Override
    public WorkflowInstanceCommandName getName()
    {
        return name;
    }

    @Override
    public void mapIntent(String intent)
    {
        this.name = WorkflowInstanceCommandName.valueOf(intent);
    }

    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();
        builder.append("WorkflowInstanceCommand [command=");
        builder.append(name);
        builder.append(", workflowInstanceKey=");
        builder.append(getWorkflowInstanceKey());
        builder.append(", workflowKey=");
        builder.append(getWorkflowKey());
        builder.append(", bpmnProcessId=");
        builder.append(getBpmnProcessId());
        builder.append(", version=");
        builder.append(getVersion());
        builder.append(", activityId=");
        builder.append(getActivityId());
        builder.append(", payload=");
        builder.append(getPayload());
        builder.append("]");
        return builder.toString();
    }
}
