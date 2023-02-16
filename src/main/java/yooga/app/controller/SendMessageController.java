package yooga.app.controller;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import yooga.app.dto.RequestNcmDTO;
import yooga.app.service.RabbitMqService;
import yooga.app.util.JsonUtil;

@Path("send-message")
public class SendMessageController {
    
    @Inject RabbitMqService rabbitMqService;
    @Inject JsonUtil jsonUtil;

    @PUT
    public void message(RequestNcmDTO ncm){
        rabbitMqService.send("quarkus-rabbitmq", jsonUtil.fromObject(ncm));
       
    }
}
