package Verticle;

import Utils.Constants;
import Utils.SpawnProcess;
import Utils.UserConfig;
import com.fasterxml.jackson.databind.JsonNode;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PollingEngine extends AbstractVerticle
{
    private static final Logger logger = LoggerFactory.getLogger(PollingEngine.class);
    EventBus eventBus;

    @Override
    public void start(Promise<Void> startPromise)
    {
        eventBus = vertx.eventBus();

        HashMap<String,Integer> scheduleTime = new HashMap<>();

        scheduleTime.put("availabilityPolling", UserConfig.AVAILIBILITY_POLLING_TIME);

        scheduleTime.put("sshPolling",UserConfig.SSH_POLLING_TIME);

        HashMap<String,Integer> updatedScheduleTime = new HashMap<>(scheduleTime);

        vertx.setPeriodic(UserConfig.SCHEDULER_DELAY, handler->
        {
            try
            {
                for(Map.Entry<String,Integer> entry: updatedScheduleTime.entrySet())
                {
                    int time = entry.getValue();

                    time = time - UserConfig.SCHEDULER_DELAY;

                    if(time<=0)
                    {
                        logger.debug("Polling Started .... "+entry.getKey());

                        if(entry.getKey().equals("sshPolling"))
                        {
                            eventBus.<JsonArray>request(Constants.SSH_POLLING_PROCESS_TRIGGERED,"", response->
                            {
                                if(response.succeeded())
                                {
                                    vertx.executeBlocking(handlers->
                                    {
                                        JsonNode outputFromPlugin = SpawnProcess.spwanProcess(response.result().body());

                                        logger.debug("Output From SSH Plugin "+outputFromPlugin);

                                        eventBus.<JsonNode>request(Constants.OUTPUT_SSH_POLLING,outputFromPlugin,result->
                                        {
                                            if(result.succeeded())
                                            {
                                                logger.debug("Polling Data Dumped into the Database");
                                            }
                                            else
                                            {
                                                logger.debug("Some error in dumping the ssh polling data into Database");
                                            }
                                        });
                                    });
                                }
                                else
                                {
                                    logger.debug("Some Problem in loading Discovery Devices");
                                }
                            });
                        }
                        else
                        {
                            // trigger the task for fping

                            eventBus.<ArrayList>request(Constants.AVAILABILITY_POLLING_PROCESS_TRIGGERED,"",response->
                            {
                                if(response.succeeded())
                                {
                                    vertx.executeBlocking(handlers->
                                    {

                                        try
                                        {
                                            HashMap<String,String> fpingPluginResult = SpawnProcess.fpingForAvailibility(response.result().body());

                                            logger.debug("Output from fping plugin "+fpingPluginResult);

                                            eventBus.<HashMap<String ,String>>request(Constants.OUTPUT_AVAILABILITY_POLLING,fpingPluginResult,result->
                                            {
                                                if(result.succeeded())
                                                {
                                                    logger.debug("Fping polling data successfully dumped into database");
                                                }
                                                else
                                                {
//                                                    logger.debug(response.cause().getMessage()); //exception   chceck
                                                }
                                            });

                                        }
                                        catch (Exception exception)
                                        {
                                            exception.printStackTrace();
                                        }
                                    });
                                }
                                else
                                {
                                    logger.debug("Some error in loading monitor ip address");
                                }
                            });
                        }
                        updatedScheduleTime.put(entry.getKey(),scheduleTime.get(entry.getKey()));
                    }
                    else
                    {
                        updatedScheduleTime.put(entry.getKey(),time);
                    }
                }
            }
            catch (Exception exception)
            {
                exception.printStackTrace();
            }
        });

        startPromise.complete();

    }
}