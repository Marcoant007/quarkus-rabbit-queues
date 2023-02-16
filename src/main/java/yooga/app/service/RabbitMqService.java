package yooga.app.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysql.cj.exceptions.UnableToConnectException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import okhttp3.Response;
import yooga.app.util.JsonUtil;

@Startup
@ApplicationScoped
public class RabbitMqService {

  private static final int MAX_RETRIES = 3;
  private static final String DEAD_LETTER_EXCHANGE = "quarkus-reject-dlx";
  private static final String DEAD_LETTER_QUEUE = "quarkus-reject-dlq";
  private static final String MAIN_EXCHANGE = "quarkus-rabbitmq";
  private static final String MAIN_QUEUE = "quarkus-rabbitmq";
  private static final String DEAD_LETTER_ROUTING_KEY = "quarkus-rabbitmq";
  private static final Integer RETRY_DELAY_MS = 60000;
  private static final String RETRY_COUNT_HEADER = "x-retry-count";

  @Inject
  JsonUtil jsonUtil;
  @Inject
  HttpService httpService;
  @Inject
  RabbitMqConnection rabbitMqConnection;

  private Logger logger = LoggerFactory.getLogger(RabbitMqService.class);

  @ConfigProperty(name = "yooga.url-quarkus-fiscal")
  String urlQuarkusFiscal;

  private Channel channel;

  @PostConstruct
  void connect() {
    setupQueues();
    setupReceiving();
  }

  private void setupQueues() {
    try {
      Connection connection = rabbitMqConnection.getConnection();
      channel = connection.createChannel();
      channel.basicQos(1);

      // Dead-letter queue setup
      channel.exchangeDeclare(DEAD_LETTER_EXCHANGE, BuiltinExchangeType.TOPIC, true);
      channel.queueDeclare(DEAD_LETTER_QUEUE, true, false, false, null);
      channel.queueBind(DEAD_LETTER_QUEUE, DEAD_LETTER_EXCHANGE, "#");

      // Main queue setup
      Map<String, Object> args = new HashMap<String, Object>();
      args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
      args.put("x-dead-letter-routing-key", MAIN_QUEUE); // ai eu declaro a routingkey
      args.put("x-message-ttl", RETRY_DELAY_MS);
      channel.queueDeclare(MAIN_QUEUE, true, false, false, args);
      channel.exchangeDeclare(MAIN_EXCHANGE, BuiltinExchangeType.TOPIC, true);
      channel.queueBind(MAIN_QUEUE, MAIN_EXCHANGE, "#");

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

  private void setupReceiving() {
    try {
      channel.basicConsume(MAIN_QUEUE, false, new DefaultConsumer(channel) {
        long deliveryTag = 0;

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException {
          try {
            deliveryTag = envelope.getDeliveryTag();
            String json = new String(body, StandardCharsets.UTF_8);
            Log.info("mensagem: " + json);
            
            try (Response response = httpService.put(urlQuarkusFiscal + "produto/alterar-ncms-venda", jsonUtil.fromObject(body))) {
              if (response.code() != 200) {
                int retryCount = getRetryCount(properties);
                if (retryCount < MAX_RETRIES) {
                  Log.info("Tentativa de envio N: " + retryCount);
                  Map<String, Object> headers = new HashMap<>(properties.getHeaders());
                  headers.put(RETRY_COUNT_HEADER, retryCount + 1);
                  
                  AMQP.BasicProperties retryProps = new AMQP.BasicProperties.Builder().headers(headers).expiration(String.valueOf(RETRY_DELAY_MS)).build();
                  
                  channel.basicPublish(DEAD_LETTER_EXCHANGE, DEAD_LETTER_ROUTING_KEY, retryProps, body);
                  Log.info("Publicada após, tentativa de envio N: " + retryCount);
                }else {
                  logger.error("Max retries exceeded for message " + deliveryTag);
                  channel.basicNack(deliveryTag, false, false);
                }
              } else {
                channel.basicAck(deliveryTag, false);
              }
            }

          } catch (Exception e) {
            logger.error("Error 500 " + e.getMessage());
            int retryCount = getRetryCount(properties);
            if (retryCount < MAX_RETRIES) {
              Map<String, Object> headers = new HashMap<>(properties.getHeaders());
              headers.put(RETRY_COUNT_HEADER, retryCount + 1);
              AMQP.BasicProperties retryProps = new AMQP.BasicProperties.Builder().headers(headers).expiration(String.valueOf(RETRY_DELAY_MS)).build();

              channel.basicPublish(DEAD_LETTER_EXCHANGE, DEAD_LETTER_ROUTING_KEY, retryProps, body);
            } else {
              logger.error("Max retries exceeded for message " + deliveryTag);
              channel.basicNack(deliveryTag, false, false);
            }
          }
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private int getRetryCount(AMQP.BasicProperties properties) {
    int retryCount = 0;
    Map<String, Object> headers = properties.getHeaders();
    if (headers != null && headers.containsKey(RETRY_COUNT_HEADER)) {
      retryCount = (int) headers.get(RETRY_COUNT_HEADER);
    }
    return retryCount;
  }

  public void send(String queueName, String message) {
    try {
      Map<String, Object> headers = new HashMap<>();
      List<Map<String, Object>> deaths = new ArrayList<>();
      Map<String, Object> death = new HashMap<>();
      death.put("queue", DEAD_LETTER_QUEUE);
      death.put("reason", "rejected");
      death.put("time", System.currentTimeMillis() / 1000);
      deaths.add(death);
      headers.put("x-death", deaths);

      AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();
      propsBuilder.headers(headers);
      // propsBuilder.expiration("30000");
      AMQP.BasicProperties props = propsBuilder.build();

      String routingKey = "#";
      if (queueName.equals(DEAD_LETTER_QUEUE)) {
        routingKey = DEAD_LETTER_ROUTING_KEY;
        Log.info("routingKey: " + routingKey);

      }

      channel.basicPublish(queueName, routingKey, props, message.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
