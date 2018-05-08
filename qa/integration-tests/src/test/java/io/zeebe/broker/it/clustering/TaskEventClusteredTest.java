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
package io.zeebe.broker.it.clustering;

import static org.assertj.core.api.Assertions.assertThat;

import io.zeebe.broker.it.ClientRule;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.event.TaskEvent;
import io.zeebe.client.impl.clustering.BrokerInfoImpl;
import io.zeebe.client.impl.topic.Topic;
import io.zeebe.client.impl.topic.Topics;
import io.zeebe.test.util.AutoCloseableRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;


public class TaskEventClusteredTest
{
    public ClientRule clientRule = new ClientRule(false);
    public AutoCloseableRule closeables = new AutoCloseableRule();
    public ClusteringRule clusteringRule = new ClusteringRule(closeables, clientRule);

    @Rule
    public RuleChain ruleChain =
        RuleChain.outerRule(closeables)
                 .around(clientRule)
                 .around(clusteringRule);

    @Test
    public void shouldCreateTaskWhenFollowerUnavailable()
    {
        // given
        final ZeebeClient client = clientRule.getClient();

        final String topicName = "foo";
        clusteringRule.createTopic(topicName, 1);

        final Topics topics = client.topics().getTopics().execute();
        final Topic topic = topics.getTopics()
            .stream()
            .filter(t -> topicName.equals(t.getName()))
            .findFirst()
            .get();

        final BrokerInfoImpl leader = clusteringRule.getLeaderForPartition(topic.getPartitions().get(0).getId());

        // choosing a new leader in a raft group where the previously leading broker is no longer available
        clusteringRule.stopBroker(leader.getSocketAddress());

        // when
        final TaskEvent taskEvent = client.tasks().create(topicName, "bar").execute();

        // then
        assertThat(taskEvent.getState()).isEqualTo("CREATED");
    }
}
