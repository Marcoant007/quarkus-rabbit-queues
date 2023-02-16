package yooga.app.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Response;
import yooga.app.dto.RequestNcmDTO;
import yooga.app.util.JsonUtil;

@ApplicationScoped
public class ProdutoService {

    @ConfigProperty(name = "yooga.url-quarkus-fiscal")
    String urlQuarkusFiscal;
    
    @Inject HttpService httpService;
    @Inject RabbitMqService rabbitMqService;
    @Inject JsonUtil jsonUtil;


    private Logger logger = LoggerFactory.getLogger(ProdutoService.class);

    public void alterarNcmProduto(RequestNcmDTO ncm){
        try {
            ncm.setIdi(68);
            ncm.setNcmInvalid(ncm.getNcmInvalid());
            ncm.setNcmValid(ncm.getNcmValid());
            try(Response response = httpService.put(urlQuarkusFiscal + "produto/alterar-ncms-venda", jsonUtil.fromObject(ncm))){
                if(response.code() == 200){
                    rabbitMqService.send("quarkus-rabbitmq", jsonUtil.fromObject(ncm));
                }else {
                    rabbitMqService.send("quarkus-rabbitmq", jsonUtil.fromObject(ncm));
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            rabbitMqService.send("quarkus-rabbitmq", "Deu erro " + ncm.getIdi().toString());
        }
    }
}
