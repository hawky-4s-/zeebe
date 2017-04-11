package org.camunda.tngp.broker.event;

import org.camunda.tngp.broker.event.processor.TopicSubscriptionService;
import org.camunda.tngp.logstreams.processor.StreamProcessorController;
import org.camunda.tngp.servicecontainer.ServiceName;

public class TopicSubscriptionServiceNames
{

    public static final ServiceName<TopicSubscriptionService> TOPIC_SUBSCRIPTION_SERVICE = ServiceName.newServiceName("log.subscription.manager", TopicSubscriptionService.class);

    public static ServiceName<StreamProcessorController> subscriptionPushServiceName(String logStreamName, String subscriptionName)
    {
        return ServiceName.newServiceName(String.format("log.%s.subscription.push.%s", logStreamName, subscriptionName), StreamProcessorController.class);
    }

    public static ServiceName<StreamProcessorController> subscriptionManagementServiceName(String logStreamName)
    {
        return ServiceName.newServiceName(String.format("log.%s.subscription.management", logStreamName), StreamProcessorController.class);
    }
}