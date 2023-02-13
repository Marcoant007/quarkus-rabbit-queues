package yooga.app.service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.rabbitmq.client.Connection;

import io.quarkiverse.rabbitmqclient.RabbitMQClient;


@ApplicationScoped
public class RabbitMqConnection {

    @Inject RabbitMQClient rabbitMQClient;
    private Connection connection;

    @PostConstruct
    void conect(){
        connection = rabbitMQClient.connect();
    }

    @PreDestroy
    void disconnect(){
        try {
            connection.close(0, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection(){
        return connection;
    }
}
