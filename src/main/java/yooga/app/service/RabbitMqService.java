package yooga.app.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.mysql.cj.exceptions.UnableToConnectException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;

@Startup
@ApplicationScoped
public class RabbitMqService {

  @Inject RabbitMqConnection rabbitMqConnection;
  private Channel channel;

  @PostConstruct
  void connect(){
    setupQueues();
    setupReceiving();
  }

  private void setupQueues(){
    try {
      Connection connection = rabbitMqConnection.getConnection();
      channel = connection.createChannel();
      channel.basicQos(1);
      channel.exchangeDeclare("quarkus-rabbitmq", BuiltinExchangeType.TOPIC, true);
      channel.queueDeclare("quarkus-rabbitmq", true, false, false, null);
      channel.queueBind("quarkus-rabbitmq", "quarkus-rabbitmq", "#");
    } catch (Exception e) {
      throw new UnableToConnectException(e);      
    }
  }

  @PreDestroy
    void disconnect() {
        try {
            channel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupReceiving(){
    try {
      channel.basicConsume("quarkus-rabbitmq", false, new DefaultConsumer(channel){
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
          try {
            long deliveryTag = envelope.getDeliveryTag();
            String json = new String(body, StandardCharsets.UTF_8);
            Log.info("mensagem: " + json);
            channel.basicAck(deliveryTag, false);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void send(String queueName, String message) {
    try {
        channel.basicPublish(queueName, "#", null, message.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
        throw new UncheckedIOException(e);
    }
}
}
