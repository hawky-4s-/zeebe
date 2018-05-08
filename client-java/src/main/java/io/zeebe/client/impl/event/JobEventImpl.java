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
package io.zeebe.client.impl.event;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.zeebe.client.api.events.JobEvent;
import io.zeebe.client.api.record.RecordMetadata;
import io.zeebe.client.api.record.ZeebeObjectMapper;
import io.zeebe.client.impl.data.MsgPackConverter;
import io.zeebe.client.impl.record.JobRecordImpl;

public class JobEventImpl extends JobRecordImpl implements JobEvent
{
    private JobState state;

    @JsonCreator
    public JobEventImpl(@JacksonInject ZeebeObjectMapper objectMapper, @JacksonInject MsgPackConverter converter)
    {
        super(objectMapper, converter, RecordMetadata.RecordType.EVENT);
    }

    @Override
    public JobState getState()
    {
        return state;
    }

    @Override
    protected void mapIntent(String intent)
    {
        state = JobState.valueOf(intent);
    }

    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();
        builder.append("Job [state=");
        builder.append(state);
        builder.append(", type=");
        builder.append(getType());
        builder.append(", retries=");
        builder.append(getRetries());
        builder.append(", lockOwner=");
        builder.append(getLockOwner());
        builder.append(", lockTime=");
        builder.append(getLockExpirationTime());
        builder.append(", headers=");
        builder.append(getHeaders());
        builder.append(", customHeaders=");
        builder.append(getCustomHeaders());
        builder.append(", payload=");
        builder.append(getPayload());
        builder.append("]");
        return builder.toString();
    }

}
