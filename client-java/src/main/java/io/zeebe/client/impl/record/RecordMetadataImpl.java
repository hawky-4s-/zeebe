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
package io.zeebe.client.impl.record;

import io.zeebe.client.api.record.RecordMetadata;
import io.zeebe.protocol.clientapi.ExecuteCommandRequestEncoder;

public class RecordMetadataImpl implements RecordMetadata
{
    private String topicName;
    private int partitionId = ExecuteCommandRequestEncoder.partitionIdNullValue();
    private long key = ExecuteCommandRequestEncoder.keyNullValue();
    private long position = ExecuteCommandRequestEncoder.positionNullValue();
    private RecordType recordType;
    private ValueType valueType;
    private String intent;

    @Override
    public String getTopicName()
    {
        return topicName;
    }

    public void setTopicName(String topicName)
    {
        this.topicName = topicName;
    }

    @Override
    public int getPartitionId()
    {
        return partitionId;
    }

    public void setPartitionId(int partitionId)
    {
        this.partitionId = partitionId;
    }

    public boolean hasPartitionId()
    {
        return partitionId != ExecuteCommandRequestEncoder.partitionIdNullValue();
    }

    @Override
    public long getPosition()
    {
        return position;
    }

    public void setPosition(long position)
    {
        this.position = position;
    }

    @Override
    public long getKey()
    {
        return key;
    }

    public void setKey(long key)
    {
        this.key = key;
    }

    @Override
    public RecordType getRecordType()
    {
        return recordType;
    }

    public void setRecordType(RecordType recordType)
    {
        this.recordType = recordType;
    }

    @Override
    public ValueType getValueType()
    {
        return valueType;
    }

    public void setValueType(ValueType valueType)
    {
        this.valueType = valueType;
    }

    @Override
    public String getIntent()
    {
        return intent;
    }

    public void setIntent(String intent)
    {
        this.intent = intent;
    }

    @Override
    public String toString()
    {
        return "EventMetadata [topicName=" + topicName + ", partitionId=" + partitionId + ", key=" +
                key + ", position=" + position + ", eventType=" + recordType + "]";
    }

}
