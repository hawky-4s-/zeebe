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
package io.zeebe.client.impl.subscription.topic;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import io.zeebe.client.api.subscription.PollableTopicSubscription;
import io.zeebe.client.api.subscription.TopicSubscriptionBuilderStep1.PollableTopicSubscriptionBuilderStep2;
import io.zeebe.client.api.subscription.TopicSubscriptionBuilderStep1.PollableTopicSubscriptionBuilderStep3;
import io.zeebe.client.cmd.ClientException;
import io.zeebe.client.impl.subscription.SubscriptionManager;

public class PollableTopicSubscriptionBuilderImpl implements PollableTopicSubscriptionBuilderStep2, PollableTopicSubscriptionBuilderStep3
{
    private TopicSubscriberGroupBuilder builder;

    public PollableTopicSubscriptionBuilderImpl(
            String topic,
            SubscriptionManager subscriptionManager,
            int prefetchCapacity)
    {
        builder = new TopicSubscriberGroupBuilder(topic, subscriptionManager, prefetchCapacity);
    }

    @Override
    public PollableTopicSubscription open()
    {
        final Future<TopicSubscriberGroup> subscription = builder.build();

        try
        {
            return subscription.get();
        }
        catch (InterruptedException | ExecutionException e)
        {
            throw new ClientException("Could not open subscription", e);
        }
    }

    @Override
    public PollableTopicSubscriptionBuilderStep3 startAtPosition(int partitionId, long position)
    {
        builder.startPosition(partitionId, position);
        return this;
    }

    @Override
    public PollableTopicSubscriptionBuilderStep3 startAtTailOfTopic()
    {
        builder.startAtTailOfTopic();
        return this;
    }

    @Override
    public PollableTopicSubscriptionBuilderStep3 startAtHeadOfTopic()
    {
        builder.startAtHeadOfTopic();
        return this;
    }

    @Override
    public PollableTopicSubscriptionBuilderStep3 name(String subscriptionName)
    {
        builder.name(subscriptionName);
        return this;
    }

    @Override
    public PollableTopicSubscriptionBuilderStep3 forcedStart()
    {
        builder.forceStart();
        return this;
    }

}
