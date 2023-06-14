package Verticle;

import Utils.Constants;
import Utils.SpawnProcess;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class DiscoveryEngine extends AbstractVerticle
{
    EventBus eventBus;

    @Override
    public void start(Promise<Void> startPromise)
    {
        eventBus = vertx.eventBus();

        eventBus.localConsumer(Constants.RUN_DISCOVERY_SPAWN_PEROCESS,handler->
        {
            JsonObject deviceDetails = (JsonObject) handler.body();

            deviceDetails.put("category","discovery");

            System.out.println("Device Details "+deviceDetails);

            JsonArray inputArray = new JsonArray().add(deviceDetails);

            System.out.println("Inpiut Array "+inputArray);

            try
            {
                vertx.executeBlocking(blockingHandler->
                {
                    JsonNode discoveryResultFromPlugin = SpawnProcess.spwanProcess(inputArray);

                    ObjectMapper mapper = new ObjectMapper();

                    JsonNode element = discoveryResultFromPlugin.elements().next();

                    String jsonString = null;

                    try
                    {
                        jsonString = mapper.writeValueAsString(element);
                    }
                    catch (JsonProcessingException exception)
                    {
                        exception.printStackTrace();
                    }

                    if(jsonString.equals("\"success\""))
                    {
                        handler.reply(deviceDetails.getString("id"));

                        System.out.println(deviceDetails.getString("id"));
                    }
                    else
                    {
                        handler.reply("");
                    }

                },false);

            }
            catch (Exception exception)
            {
                exception.printStackTrace();
            }

        });

    }
}