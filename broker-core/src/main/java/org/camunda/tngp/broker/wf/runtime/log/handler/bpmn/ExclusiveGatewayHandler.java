package org.camunda.tngp.broker.wf.runtime.log.handler.bpmn;

import org.agrona.DirectBuffer;
import org.camunda.tngp.bpmn.graph.BpmnEdgeTypes;
import org.camunda.tngp.bpmn.graph.FlowElementVisitor;
import org.camunda.tngp.bpmn.graph.MsgPackPropertyReader;
import org.camunda.tngp.bpmn.graph.MsgPackScalarReader;
import org.camunda.tngp.bpmn.graph.ProcessGraph;
import org.camunda.tngp.broker.log.LogWriters;
import org.camunda.tngp.broker.wf.runtime.data.JsonPathResult;
import org.camunda.tngp.broker.wf.runtime.data.MsgPackDocument;
import org.camunda.tngp.broker.wf.runtime.data.MsgPackDocumentImpl;
import org.camunda.tngp.broker.wf.runtime.log.bpmn.BpmnBranchEventReader;
import org.camunda.tngp.broker.wf.runtime.log.bpmn.BpmnFlowElementEventReader;
import org.camunda.tngp.broker.wf.runtime.log.bpmn.BpmnFlowElementEventWriter;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ConditionOperator;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;
import org.camunda.tngp.graph.bpmn.MsgPackType;
import org.camunda.tngp.hashindex.Long2LongHashIndex;
import org.camunda.tngp.log.LogReader;
import org.camunda.tngp.log.idgenerator.IdGenerator;
import org.camunda.tngp.util.buffer.BufferUtil;

public class ExclusiveGatewayHandler implements BpmnFlowElementAspectHandler
{

    public static final int NO_FLOW_ID = -1; // note: this assumes flow element IDs are positive!

    protected static final BooleanBiFunction<MsgPackScalarReader> EQUAL_OPERATOR = new EqualOperator();
    protected static final BooleanBiFunction<MsgPackScalarReader> GREATER_THAN_OPERATOR = new GreaterThanOperator();
    protected static final BooleanBiFunction<MsgPackScalarReader> LOWER_THAN_OPERATOR = new LowerThanOperator();

    protected BpmnFlowElementEventWriter eventWriter = new BpmnFlowElementEventWriter();
    protected final FlowElementVisitor flowElementVisitor = new FlowElementVisitor();
    protected BpmnBranchEventReader bpmnBranchEventReader = new BpmnBranchEventReader();

    protected final Long2LongHashIndex eventIndex;
    protected final LogReader logReader;

    protected final MsgPackDocument jsonDocument;

    public ExclusiveGatewayHandler(LogReader logReader, Long2LongHashIndex eventIndex)
    {
        this.eventIndex = eventIndex;
        this.logReader = logReader;
        this.jsonDocument = new MsgPackDocumentImpl(2);
    }

    @Override
    public BpmnAspect getHandledBpmnAspect()
    {
        return BpmnAspect.EXCLUSIVE_SPLIT;
    }

    @Override
    public int handle(BpmnFlowElementEventReader gatewayEvent, ProcessGraph process, LogWriters logWriters,
            IdGenerator idGenerator)
    {
        flowElementVisitor.init(process);

        final int flowToTake = determineActivatedFlow(gatewayEvent);

        if (flowToTake == NO_FLOW_ID)
        {
            System.err.println("Could not take any of the outgoing sequence flows. Workflow instance " + gatewayEvent.wfInstanceId() + " is stuck and won't continue execution");
        }
        else
        {
            flowElementVisitor.moveToNode(flowToTake);
            takeSequenceFlow(gatewayEvent, flowElementVisitor, logWriters);
        }

        return 0;
    }

    protected int determineActivatedFlow(BpmnFlowElementEventReader gatewayEvent)
    {
        final int gatewayId = gatewayEvent.flowElementId();
        flowElementVisitor.moveToNode(gatewayId);

        final int outgoingSequenceFlowsCount = flowElementVisitor.outgoingSequenceFlowsCount();
        initJsonDocument(gatewayEvent.bpmnBranchKey());

        int sequenceFlowIndex = 0;
        int defaultFlowId = NO_FLOW_ID;
        int flowToTake = NO_FLOW_ID;

        while (sequenceFlowIndex < outgoingSequenceFlowsCount && flowToTake == NO_FLOW_ID)
        {
            flowElementVisitor.moveToNode(gatewayId);
            flowElementVisitor.traverseEdge(BpmnEdgeTypes.NODE_OUTGOING_SEQUENCE_FLOWS, sequenceFlowIndex);

            if (flowElementVisitor.isDefaultFlow())
            {
                defaultFlowId = flowElementVisitor.nodeId();
            }
            else
            {
                final Boolean conditionResult = evaluateCondition(
                        jsonDocument,
                        flowElementVisitor.conditionArg1(),
                        flowElementVisitor.conditionOperator(),
                        flowElementVisitor.conditionArg2());

                if (conditionResult == Boolean.TRUE)
                {
                    flowToTake = flowElementVisitor.nodeId();
                }
            }

            sequenceFlowIndex++;
        }

        if (flowToTake == NO_FLOW_ID)
        {
            flowToTake = defaultFlowId;
        }

        return flowToTake;
    }

    protected void takeSequenceFlow(BpmnFlowElementEventReader gatewayEvent, FlowElementVisitor sequenceFlow, LogWriters logWriters)
    {
        final DirectBuffer stringIdBuffer = sequenceFlow.stringIdBuffer();

        eventWriter
            .bpmnBranchKey(gatewayEvent.bpmnBranchKey())
            .eventType(ExecutionEventType.SQF_EXECUTED)
            .flowElementId(sequenceFlow.nodeId())
            .flowElementIdString(stringIdBuffer, 0, stringIdBuffer.capacity())
            .processId(gatewayEvent.wfDefinitionId())
            .workflowInstanceId(gatewayEvent.wfInstanceId());

        logWriters.writeToCurrentLog(eventWriter);

    }

    protected void initJsonDocument(long bpmnBranchKey)
    {
        final long branchPosition = eventIndex.get(bpmnBranchKey, -1L);
        logReader.seek(branchPosition);
        logReader.next().readValue(bpmnBranchEventReader);

        final DirectBuffer payload = bpmnBranchEventReader.materializedPayload();
        jsonDocument.wrap(payload, 0, payload.capacity());
    }

    /**
     * @return true if condition is valid and evaluates to true, false if condition is valid and evaluates to false, null if condition is not valid
     */
    protected Boolean evaluateCondition(MsgPackDocument json, MsgPackPropertyReader arg1, ConditionOperator comparisonOperator, MsgPackPropertyReader arg2)
    {
        final MsgPackScalarReader arg1Value = resolveToScalar(json, arg1);
        final MsgPackScalarReader arg2Value = resolveToScalar(json, arg2);

        if (arg1Value == null || arg2Value == null)
        {
            return null;
        }
        else
        {
            final boolean comparisonFulfilled;
            switch (comparisonOperator)
            {
                case EQUAL:
                    comparisonFulfilled = EQUAL_OPERATOR.apply(arg1Value, arg2Value);
                    break;
                case NOT_EQUAL:
                    comparisonFulfilled = !EQUAL_OPERATOR.apply(arg1Value, arg2Value);
                    break;
                case GREATER_THAN:
                    comparisonFulfilled = GREATER_THAN_OPERATOR.apply(arg1Value, arg2Value);
                    break;
                case GREATER_THAN_OR_EQUAL:
                    comparisonFulfilled = !LOWER_THAN_OPERATOR.apply(arg1Value, arg2Value);
                    break;
                case LOWER_THAN:
                    comparisonFulfilled = LOWER_THAN_OPERATOR.apply(arg1Value, arg2Value);
                    break;
                case LOWER_THAN_OR_EQUAL:
                    comparisonFulfilled = !GREATER_THAN_OPERATOR.apply(arg1Value, arg2Value);
                    break;
                default:
                    comparisonFulfilled = false;
                    break;
            }

            // avoiding auto-boxing. We cannot return primitive boolean as we need three-value-logic here
            if (comparisonFulfilled)
            {
                return Boolean.TRUE;
            }
            else
            {
                return Boolean.FALSE;
            }
        }

    }

    protected MsgPackScalarReader resolveToScalar(MsgPackDocument document, MsgPackPropertyReader msgPackProperty)
    {
        if (msgPackProperty.type() == MsgPackType.EXPRESSION)
        {
            return resolveJsonPathToScalar(document, msgPackProperty.valueExpression());
        }
        else
        {
            return msgPackProperty;
        }
    }

    protected MsgPackScalarReader resolveJsonPathToScalar(MsgPackDocument document, DirectBuffer jsonPathExpression)
    {
        final JsonPathResult jsonPathResult = document.jsonPath(jsonPathExpression, 0, jsonPathExpression.capacity());
        if (jsonPathResult.hasResolved())
        {
            if (jsonPathResult.isArray() || jsonPathResult.isObject())
            {
                System.err.println("Sequence flow " + flowElementVisitor.stringId() + ": json path did not resolve to a primitive value (String, Number, Boolean, null)");
                return null;
            }
            else
            {
                return jsonPathResult;
            }
        }
        else
        {
            System.err.println("Sequence flow " + flowElementVisitor.stringId() + ": json path did not resolve");
            return null;
        }
    }

    protected static class EqualOperator implements BooleanBiFunction<MsgPackScalarReader>
    {

        @Override
        public boolean apply(MsgPackScalarReader o1, MsgPackScalarReader o2)
        {
            if (o1.isBoolean() && o2.isBoolean())
            {
                return o1.asBoolean() == o2.asBoolean();
            }
            else if (o1.isInteger() && o2.isInteger())
            {
                return o1.asInteger() == o2.asInteger();
            }
            else if (o1.isFloat() && o2.isFloat())
            {
                return o1.asFloat() == o2.asFloat();
            }
            else if (o1.isString() && o2.isString())
            {
                return BufferUtil.contentsEqual(o1.asEncodedString(), o2.asEncodedString());
            }
            else
            {
                return o1.isNil() && o2.isNil();
            }
        }
    }

    protected static class GreaterThanOperator implements BooleanBiFunction<MsgPackScalarReader>
    {
        @Override
        public boolean apply(MsgPackScalarReader arg1, MsgPackScalarReader arg2)
        {
            // TODO: could also lexicographically compare strings, but that may not be trivial based
            //   on the encoded byte arrays
            return (arg1.isInteger() && arg2.isInteger() && arg1.asInteger() > arg2.asInteger()) ||
                    (arg1.isFloat() && arg2.isFloat() && arg1.asFloat() > arg2.asFloat());
        }
    }

    protected static class LowerThanOperator implements BooleanBiFunction<MsgPackScalarReader>
    {
        @Override
        public boolean apply(MsgPackScalarReader arg1, MsgPackScalarReader arg2)
        {
            // TODO: could also lexicographically compare strings, but that may not be trivial based
            //   on the encoded byte arrays
            return (arg1.isInteger() && arg2.isInteger() && arg1.asInteger() < arg2.asInteger()) ||
                    (arg1.isFloat() && arg2.isFloat() && arg1.asFloat() < arg2.asFloat());
        }
    }

    protected interface BooleanBiFunction<T>
    {
        boolean apply(T arg1, T arg2);
    }

}
