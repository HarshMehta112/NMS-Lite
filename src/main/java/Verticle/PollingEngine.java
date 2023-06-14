package Verticle;

import Utils.Constants;
import Utils.SpawnProcess;
import com.fasterxml.jackson.databind.JsonNode;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PollingEngine extends AbstractVerticle
{
    EventBus eventBus;

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {
        eventBus = vertx.eventBus();

        HashMap<String,Integer> scheduleTime = new HashMap<>();

        scheduleTime.put("availabilityPolling", Constants.AVAILIBILITY_POLLING_TIME);

        scheduleTime.put("sshPolling",Constants.SSH_POLLING_TIME);

        HashMap<String,Integer> updatedScheduleTime = new HashMap<>();

        updatedScheduleTime.put("availabilityPolling", Constants.AVAILIBILITY_POLLING_TIME);

        updatedScheduleTime.put("sshPolling",Constants.SSH_POLLING_TIME);


        vertx.setPeriodic(Constants.SCHEDULER_DELAY,handler->
        {
            try
            {
                for(Map.Entry<String,Integer> entry: updatedScheduleTime.entrySet())
                {
                    int time = entry.getValue();

                    time = time-Constants.SCHEDULER_DELAY;

                    if(time<=0)
                    {
                        System.out.println(entry.getKey());

                        if(entry.getKey().equals("sshPolling"))
                        {
                            eventBus.<JsonArray>request(Constants.SSH_POLLING_PROCESS_TRIGGERED,"", response->
                            {
                                if(response.succeeded())
                                {
                                    vertx.executeBlocking(handlers->
                                    {
                                        JsonNode outputFromPlugin = SpawnProcess.spwanProcess(response.result().body());

                                        System.out.println("Output From Plugin "+outputFromPlugin);

                                        eventBus.<JsonNode>request(Constants.SSH_POLLING_DATA,outputFromPlugin,result->
                                        {
                                            if(result.succeeded())
                                            {
                                                System.out.println("Polling Data Dumped into the Database");
                                            }
                                            else
                                            {
                                                System.out.println("Some error in dumping the ssh polling data into Database");
                                            }
                                        });
                                    });
                                }
                                else
                                {
                                    System.out.println("Some Problem in loading Discovery Devices");
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
                                        System.out.println("response.result().body()" +response.result().body());

                                        HashMap<String,String> fpingPluginResult = SpawnProcess.fpingForAvailibility(response.result().body());

                                        System.out.println("Output from fping plugin "+fpingPluginResult);

                                        eventBus.<HashMap<String ,String>>request(Constants.AVAILABILITY_POLLING_DATA,fpingPluginResult,result->
                                        {
                                            if(result.succeeded())
                                            {
                                                System.out.println("Fping polling data sucessfully dumped into database");
                                            }
                                            else
                                            {
                                                System.out.println("Some error in dumping the fping polling data into Database");
                                            }
                                        });
                                    });
                                }
                                else
                                {
                                    System.out.println("Some error in loading monitor ip address");
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


    }
}