package org.kohsuke.spring.amqp;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Kohsuke Kawaguchi
 */
@Configuration
@EnableRabbit   // without this annotation nobody scans @RabbitListener
public class PojoListener implements Runnable {
    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(PojoListener.class);
        // in Spring Boot, CommandLineRunner is probably more idiomatic
        context.getBean(PojoListener.class).run();
    }

    @Bean
    public CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        connectionFactory.setHost("localhost");
        return connectionFactory;
    }

    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory con) {
        return new RabbitAdmin(con);
    }

    /**
     * By definining {@link Queue} as a bean, {@link RabbitAdmin#initialize()} auto-declares them.
     */
    @Bean
    public Queue queue() {
        return new Queue("pojoQueue", false);
    }

    /**
     * {@link AmqpTemplate} is somewhat ill-named and poorly abstracted but this represents the sending endpoint.
     */
    @Bean
    public AmqpTemplate template(ConnectionFactory con, Queue queue) {
        RabbitTemplate template = new RabbitTemplate(con);
        template.setRoutingKey(queue.getName());
        return template;
    }

    @RabbitListener(queues="#{queue.name}")
    public void onReceive(String msg) {
        System.out.println("Received: "+msg);
    }

    /**
     * Listener container is the code surrounding the invocation of @RabbitListener.
     * It looks like this needs to be explicitly wired in, with this specific bean name.
     * This feels odd.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        return factory;
    }

    @Autowired
    public AmqpTemplate template;

    @Autowired
    public ConfigurableApplicationContext context;

    @Override
    public void run() {
        try {
            template.convertAndSend("foo");
            Thread.sleep(1500);

            // RabbitMQ connector starts a bunch of threads in the background
            // and so it needs to be explicitly shut down
            context.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
