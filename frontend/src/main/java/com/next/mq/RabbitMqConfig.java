package com.next.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RabbitMqConfig {

    @Bean("directExchange")
    @Primary
    public DirectExchange directExchange() {
        return new DirectExchange(QueueConstants.COMMON_EXCHANGE, true, false);
    }

    @Bean("notifyQueue")
    @Primary
    public Queue notifyQueue() {
        return new Queue(QueueConstants.COMMON_QUEUE);
    }

    @Bean("bindingNotify")
    @Primary
    public Binding bindingNotify(@Qualifier("directExchange") DirectExchange directExchange,
                                 @Qualifier("notifyQueue") Queue notifyQueue) {
        return BindingBuilder.bind(notifyQueue).to(directExchange).with(QueueConstants.COMMON_ROUTING);
    }
}
