package yooga.app.controller;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import yooga.app.service.RabbitMqService;

@Path("send-message")
public class SendMessageController {
    
    @Inject RabbitMqService rabbitMqService;

    @GET
    public String message(){
        rabbitMqService.send("quarkus-rabbitmq", "Oii");
        return "Welcome to the rabbitMq Test";
    }
}
