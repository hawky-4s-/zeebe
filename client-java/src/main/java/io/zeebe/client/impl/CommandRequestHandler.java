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
package io.zeebe.client.impl;

import java.util.function.BiFunction;

import io.zeebe.client.api.record.Record;
import io.zeebe.client.api.record.RecordMetadata;
import io.zeebe.client.cmd.ClientCommandRejectedException;
import io.zeebe.client.impl.record.*;
import io.zeebe.protocol.clientapi.*;
import org.agrona.*;
import org.agrona.io.DirectBufferInputStream;
import org.agrona.io.ExpandableDirectBufferOutputStream;

public class CommandRequestHandler implements RequestResponseHandler
{
    protected MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    protected ExecuteCommandRequestEncoder encoder = new ExecuteCommandRequestEncoder();

    protected ExecuteCommandResponseDecoder decoder = new ExecuteCommandResponseDecoder();

    protected RecordImpl command;
    protected BiFunction<Record, String, String> errorFunction;

    protected final ZeebeObjectMapperImpl objectMapper;

    protected ExpandableArrayBuffer serializedCommand = new ExpandableArrayBuffer();
    protected int serializedCommandLength = 0;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public CommandRequestHandler(ZeebeObjectMapperImpl objectMapper, CommandImpl command)
    {
        this.objectMapper = objectMapper;
        this.command = command.getCommand();
        this.errorFunction = command::generateError;
        serialize(command.getCommand());
    }

    protected void serialize(Record event)
    {
        int offset = 0;
        headerEncoder.wrap(serializedCommand, offset)
            .blockLength(encoder.sbeBlockLength())
            .schemaId(encoder.sbeSchemaId())
            .templateId(encoder.sbeTemplateId())
            .version(encoder.sbeSchemaVersion());

        offset += headerEncoder.encodedLength();

        encoder.wrap(serializedCommand, offset);

        final RecordMetadata metadata = event.getMetadata();

        encoder
            .partitionId(metadata.getPartitionId())
            .position(metadata.getPosition());

        if (metadata.getKey() < 0)
        {
            encoder.key(ExecuteCommandRequestEncoder.keyNullValue());
        }
        else
        {
            encoder.key(metadata.getKey());
        }

        encoder.valueType(EventTypeMapping.mapEventType(metadata.getValueType()));
        encoder.intent(Intent.valueOf(metadata.getIntent()));

        offset = encoder.limit();
        final int commandHeaderOffset = offset;
        final int serializedCommandOffset = commandHeaderOffset + ExecuteCommandRequestEncoder.valueHeaderLength();

        final ExpandableDirectBufferOutputStream out = new ExpandableDirectBufferOutputStream(serializedCommand, serializedCommandOffset);

        objectMapper.toJson(out, event);

        // can only write the header after we have written the command, as we don't know the length beforehand
        final short commandLength = (short) out.position();
        serializedCommand.putShort(commandHeaderOffset, commandLength, java.nio.ByteOrder.LITTLE_ENDIAN);

        serializedCommandLength = serializedCommandOffset + out.position();
    }

    @Override
    public int getLength()
    {
        return serializedCommandLength;
    }

    @Override
    public void write(MutableDirectBuffer buffer, int offset)
    {
        buffer.putBytes(offset, serializedCommand, 0, serializedCommandLength);
    }

    @Override
    public boolean handlesResponse(MessageHeaderDecoder responseHeader)
    {
        return responseHeader.schemaId() == ExecuteCommandResponseDecoder.SCHEMA_ID && responseHeader.templateId() == ExecuteCommandResponseDecoder.TEMPLATE_ID;
    }

    @Override
    public RecordImpl getResult(DirectBuffer buffer, int offset, int blockLength, int version)
    {
        decoder.wrap(buffer, offset, blockLength, version);


        final int partitionId = decoder.partitionId();
        final long position = decoder.position();
        final long key = decoder.key();
        final RecordType recordType = decoder.recordType();

        if (recordType == RecordType.COMMAND_REJECTION)
        {
            final String rejectionReason = "unknown";

            throw new ClientCommandRejectedException(errorFunction.apply(command, rejectionReason));
        }

        final String intent = decoder.intent().name();

        final int valueLength = decoder.valueLength();

        final DirectBufferInputStream inStream = new DirectBufferInputStream(
                buffer,
                decoder.limit() + ExecuteCommandResponseDecoder.valueHeaderLength(),
                valueLength);

        final RecordImpl result = objectMapper.fromJson(inStream, command.getClass());

        result.setIntent(intent);

        final RecordMetadataImpl metadata = result.getMetadata();
        metadata.setKey(key);
        metadata.setPartitionId(partitionId);
        metadata.setTopicName(command.getMetadata().getTopicName());
        metadata.setPosition(position);
        metadata.setRecordType(getRecordType(recordType));
        metadata.setValueType(command.getMetadata().getValueType());

        return result;
    }

    private RecordMetadata.RecordType getRecordType(RecordType type)
    {
        switch (type)
        {
            case EVENT:
                return RecordMetadata.RecordType.EVENT;
            case COMMAND:
                return RecordMetadata.RecordType.COMMAND;
            case COMMAND_REJECTION:
                return RecordMetadata.RecordType.COMMAND_REJECTION;
            default:
                return null;
        }
    }

    @Override
    public String getTargetTopic()
    {
        final RecordMetadata metadata = command.getMetadata();
        return metadata.getTopicName();
    }

    @Override
    public int getTargetPartition()
    {
        if (command.hasValidPartitionId())
        {
            return command.getMetadata().getPartitionId();
        }
        else
        {
            return -1;
        }
    }

    @Override
    public void onSelectedPartition(int partitionId)
    {
        command.setPartitionId(partitionId);
        encoder.partitionId(partitionId);
    }

    @Override
    public String describeRequest()
    {
        final RecordMetadata metadata = command.getMetadata();
        return "[ topic = " + metadata.getTopicName() +
                ", partition = " + (command.hasValidPartitionId() ? metadata.getPartitionId() : "any") +
                ", value type = " + metadata.getValueType() +
                ", command = " + command.getMetadata().getIntent() + " ]";
    }

}
