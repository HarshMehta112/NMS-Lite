package Verticle;

import Utils.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import Database.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseVerticle extends AbstractVerticle
{
    static CustomConnectionPool connectionPool;

    static
    {
        connectionPool = CustomConnectionPool.getInstance(8);  //constant

        connectionPool.setURL(PropertiesFile.getURL());

        connectionPool.setUser(PropertiesFile.getUSER());

        connectionPool.setPassword(PropertiesFile.getPASSWORD());

        connectionPool.createConnectionPool();
    }

    EventBus eventBus;
    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {


        eventBus = vertx.eventBus();

        eventBus.localConsumer(Constants.ADD_DISCOVERY_DEVICE, handler->
        {
            JsonObject deviceDetails = (JsonObject) handler.body();

            vertx.executeBlocking(blockingHandler->
            {
                if(AddDevice(deviceDetails).succeeded())
                {
                    handler.reply("Discovery Device added in Database");
                }
                else
                {
                    handler.reply("Some issue in adding Discovery Device in Database");
                }
            },false);
        });

        eventBus.localConsumer(Constants.DASHBOARD_LOAD,handler->
        {
            vertx.executeBlocking(blockingHandler->
            {
                Promise<List<JsonArray>> promise = Promise.promise();

                dashBoardDataLoad(promise);

                promise.future().onComplete(handlers->
                {
                    eventBus.publish("updates.data",handlers.result().toString());

                });
            });
        });

        eventBus.localConsumer(Constants.EDIT_DISCOVERY_DEVICE, handler->
        {
            JsonObject deviceDetails = (JsonObject) handler.body();

            vertx.executeBlocking(blockingHandler->
            {
                if(EditDevice(deviceDetails).succeeded())
                {
                    handler.reply("Discovery Device information edited in Database");
                }
                else
                {
                    handler.reply("Some issue in editing Discovery Device information in Database");
                }
            },false);
        });


        eventBus.localConsumer(Constants.GET_ALL_DISCOVERY_DEVICE,handler->
        {
            vertx.executeBlocking(blockingHandler->
            {
                loadData().onComplete(result->
                {
                    if(loadData().succeeded())
                    {
                        handler.reply(new JsonArray(loadData().result()));
                    }
                    else
                    {
                        handler.reply("Enable to fetch the Discovery Data from Database");
                    }
                });
            },false);
        });

        eventBus.localConsumer(Constants.LOAD_MONITOR_DEVICE,handler->
        {
            vertx.executeBlocking(blockingHandler->
            {
                loadMonitorData().onComplete(result->
                {
                    if(loadMonitorData().succeeded())
                    {
                        System.out.println("Result "+loadMonitorData().result());

                        handler.reply(new JsonArray(loadMonitorData().result()));
                    }
                    else
                    {
                        handler.reply("Enable to fetch the Monitor Data from Database");
                    }
                });
            },false);
        });



        eventBus.localConsumer(Constants.SSH_POLLING_PROCESS_TRIGGERED,handler->
        {
           vertx.executeBlocking(blockingHandler->
           {
               fetchMonitorData().onComplete(result->
               {
                   if(fetchMonitorData().succeeded())
                   {
                       JsonArray fetchDataFromMonitorTable = fetchMonitorData().result();

                       handler.reply(fetchDataFromMonitorTable);
                   }
                   else
                   {
                       handler.reply("Enable to fetch the Discovery Data from Database for ssh polling");
                   }
               });
           },false);
        });

        eventBus.localConsumer(Constants.AVAILABILITY_POLLING_PROCESS_TRIGGERED,handler->
        {
            vertx.executeBlocking(blockingHandler->
            {
                fetchDataForAvailabilityPolling().onComplete(arrayListAsyncResult ->
                {
                    System.out.println(fetchDataForAvailabilityPolling());

                    if(fetchDataForAvailabilityPolling().succeeded())
                    {
                        ArrayList<String> list = fetchDataForAvailabilityPolling().result();

                        System.out.println(list);

                        handler.reply(list);
                    }
                    else
                    {
                        handler.reply("Enable to fetch the Discovery Data from Database for availibaliy polling");
                    }
                });
            },false);
        });



        eventBus.localConsumer(Constants.AVAILABILITY_POLLING_DATA,handler->
        {
            vertx.executeBlocking(blockingHandle->
            {
                fpingPollingDataDump((HashMap<String, String>) handler.body()).onComplete(result->
                {
                    if(result.succeeded())
                    {
                        System.out.println("availability Polling data dumped into database successfully");
                    }
                    else
                    {
                        System.out.println("Some problem in availability polling data dumping");

                        System.out.println(result.cause().getMessage());
                    }
                });
            },false);
        });


        eventBus.localConsumer(Constants.SSH_POLLING_DATA,handler->
        {
            vertx.executeBlocking(blockingHandler->
            {
                sshPollingDataDump((JsonNode) handler.body()).onComplete(result->
                {
                    if(result.succeeded())
                    {
                        System.out.println("ssh Polling data dumped into database successfully");
                    }
                    else
                    {
                        System.out.println("Some problem in ssh polling data dumping");

                        System.out.println(result.cause().getMessage());
                    }
                });
            },false);
        });


        eventBus.localConsumer(Constants.DELETE_DISCOVERY_DEVICE,handler->
        {
            System.out.println(handler.body());

            vertx.executeBlocking(blockingHandler->
            {
                DeleteDevice(handler.body().toString()).onComplete(result->
                {
                    if(DeleteDevice(handler.body().toString()).succeeded())
                    {
                        handler.reply("Device deleted successfully");
                    }
                    else
                    {
                        handler.reply("Enbale to delete discovery Device");
                    }
                });
            },false);
        });


        eventBus.localConsumer(Constants.MONITOR_DEVICE_INFO,handler->
        {
            vertx.executeBlocking(blockingHandler->
            {
                monitorDeviceInfo(handler.body().toString()).onComplete(result->
                {
                    JsonObject resultInfo  = monitorDeviceInfo(handler.body().toString()).result();

                    System.out.println("Result Info "+resultInfo);

                   if(monitorDeviceInfo(handler.body().toString()).succeeded())
                   {
                       handler.reply(resultInfo);
                   }
                });
            },false);
        });



        eventBus.localConsumer(Constants.DELETE_MONITOR_DEVICE,handler->
        {
            System.out.println("handler.body() "+handler.body());

           vertx.executeBlocking(blockingHandler->
           {
               deleteMonitorDevice(handler.body().toString()).onComplete(result->
               {
                   if(deleteMonitorDevice(handler.body().toString()).succeeded())
                   {
                       handler.reply("Monitor Device deleted successfully");
                   }
                   else
                   {
                       handler.reply("Enbale to delete Monitor Device");
                   }
               });
           },false);
        });


        eventBus.localConsumer(Constants.RUN_PROVISION,handler->
        {
            System.out.println(handler.body());

            vertx.executeBlocking(blockingHandler->
            {
                fetchDiscoveryDatabyID(handler.body().toString()).onComplete(result->
                {
                    JsonObject data = result.result();

                    System.out.println("JSON Result of RUN PROVISION "+data);

                    provisionedDeviceDataDump(data).onComplete(result1 ->
                    {
                        if(result1.succeeded())
                        {
                            System.out.println("Discovery Device Added Succssfullly into Monitor Table");
                        }
                        else
                        {
                            System.out.println("Some error occurred in adding discovery device into Monitor Tbale");

                            System.out.println(result1.cause().getMessage());
                        }
                    });
                });
            },false);

        });


        eventBus.localConsumer(Constants.RUN_DISCOVERY,handler->
        {
            System.out.println(handler.body());

            vertx.executeBlocking(blockingHandler->
            {
                fetchDiscoveryDatabyID(handler.body().toString()).onComplete(result->
                {

                    eventBus.request(Constants.RUN_DISCOVERY_SPAWN_PEROCESS,fetchDiscoveryDatabyID(handler.body().toString()).result(),response->
                    {
                        if(response.succeeded())
                        {
                            String deviceId = response.result().body().toString();

                            if(!deviceId.equals(""))
                            {
                                System.out.println(deviceId);

                                if(updateDiscovery(deviceId).succeeded())
                                {
                                    System.out.println("Discovery Table Updated with Provision value");
                                }
                                else
                                {
                                    System.out.println("Some Problem in Updating the Provision value");
                                }
                            }
                        }
                        else
                        {
                            System.out.println("Some error in getting the output from .exe file");
                        }
                    });

                    if(fetchDiscoveryDatabyID(handler.body().toString()).succeeded())
                    {
                        handler.reply("Device discovered successfully");
                    }
                    else
                    {
                        handler.reply("Device not discovered");
                    }
                });
            },false);
        });

        vertx.setPeriodic(5000,handler->
        {
            Promise<List<JsonArray>> promise = Promise.promise();

            dashBoardDataLoad(promise);

            promise.future().onComplete(handlers->
            {
                eventBus.publish("updates.data",handlers.result().toString());

            });
        });

    }



    private void dashBoardDataLoad(Promise <List<JsonArray>> promise)
    {
//        Promise<HashMap<String,List<Map<String,Object>>>> promise = Promise.promise();

        List<JsonArray> dashBoardData = new ArrayList<>();

        vertx.executeBlocking(blockingHandler->
        {
            Connection connection = connectionPool.getConnection();

            Operations operations = new Operations(connection);

            try
            {
                String query = "SELECT m.ipaddress, MAX(p.METRICVALUE) AS memory FROM POLLING_TABLE p, MONITOR_TABLE m WHERE p.metricType = 'disk.used.percentage' AND p.timestamp >= NOW() - INTERVAL '1000' MINUTE AND p.IPADDRESS = m.IPADDRESS GROUP BY p.ipaddress ORDER BY memory DESC LIMIT 5;";

                List<Map<String,Object>> map = operations.selectQuery(query);

                JsonArray top5MaxMemory =  new JsonArray(map);

                dashBoardData.add(top5MaxMemory);

                map = null;

                map = operations.selectQuery("SELECT m.ipaddress, MAX(p.METRICVALUE) AS disk FROM POLLING_TABLE p, MONITOR_TABLE m WHERE p.metricType = 'disk.used.percentage' AND p.timestamp >= NOW() - INTERVAL '1000' MINUTE AND p.IPADDRESS = m.IPADDRESS GROUP BY p.ipaddress ORDER BY disk DESC LIMIT 5;");

                JsonArray top5MaxDisk =  new JsonArray(map);

                dashBoardData.add(top5MaxDisk);

                map = null;

                map = operations.selectQuery("SELECT m.ipaddress, MAX(p.METRICVALUE) AS cpu FROM POLLING_TABLE p, MONITOR_TABLE m WHERE p.metricType = 'cpu.idle.percentage' AND p.timestamp >= NOW() - INTERVAL '1000' MINUTE AND p.IPADDRESS = m.IPADDRESS GROUP BY p.ipaddress ORDER BY cpu DESC LIMIT 5;");

                JsonArray top5MaxCPU =  new JsonArray(map);

                dashBoardData.add(top5MaxCPU);

                map = null;

                map = operations.selectQuery("SELECT COUNT(CASE WHEN STATUS='Up' THEN 1 END) as UP,COUNT(CASE WHEN STATUS='Down' THEN 1 END) as DOWN FROM (SELECT MONITOR_TABLE.DEVICEID, MONITOR_TABLE.IPADDRESS, MONITOR_TABLE.TYPE,MONITOR_TABLE.NAME, AVAILABILITY_TABLE.STATUS FROM MONITOR_TABLE INNER JOIN AVAILABILITY_TABLE ON MONITOR_TABLE.IPADDRESS = AVAILABILITY_TABLE.IPADDRESS ORDER BY AVAILABILITY_TABLE.TIMESTAMP DESC lIMIT (SELECT COUNT(IPADDRESS) FROM MONITOR_TABLE))");

                JsonArray status =  new JsonArray(map);

                dashBoardData.add(status);

                promise.complete(dashBoardData);
            }
            catch (Exception exception)
            {
                promise.fail("Some error in fetching the dash board data");

                exception.printStackTrace();
            }
            finally
            {
                connectionPool.releaseConnection(connection);
            }
        });

    }


    private Future<Boolean> DeleteDevice(String deviceID)
    {
        Promise<Boolean> promise = Promise.promise();

        Connection connection = connectionPool.getConnection();

        Operations operations = new Operations(connection);

        try
        {
            String whereClause = "DEVICEID = "+Integer.valueOf(deviceID);

            operations.delete("DISCOVERY_TABLE",whereClause);

            promise.complete(true);
        }
        catch (Exception exception)
        {
            promise.complete(false);

            exception.printStackTrace();
        }
        finally
        {
            connectionPool.releaseConnection(connection);
        }
        return promise.future();

    }

    private Future<Boolean> deleteMonitorDevice(String deviceID)
    {
        Promise<Boolean> promise = Promise.promise();

        Connection connection = connectionPool.getConnection();

        Operations operations = new Operations(connection);

        try
        {
            String whereClause = "DEVICEID = "+Integer.valueOf(deviceID);

            operations.delete("MONITOR_TABLE",whereClause);

            promise.complete(true);
        }
        catch (Exception exception)
        {
            promise.complete(false);

            exception.printStackTrace();
        }
        finally
        {
            connectionPool.releaseConnection(connection);
        }
        return promise.future();

    }


    private Future<JsonArray> fetchMonitorData()
    {
        Promise<JsonArray> promise = Promise.promise();

        JsonArray inputDataForSSHPolling = null;

        Connection connection = connectionPool.getConnection();

        try
        {
            Operations operations = new Operations(connection);

            List<Map<String, Object>> allData = null;

            ArrayList<String> columns = new ArrayList<>();

            columns.add("IPADDRESS");

            columns.add("USERNAME");

            columns.add("PASSWORD");

            columns.add("TYPE");

            columns.add("DEVICEID");

            allData = operations.selectwithWhere("MONITOR_TABLE",columns,"");

            System.out.println("Data From Monitor Table "+allData);

            inputDataForSSHPolling = new JsonArray();

            for(int index=0;index<allData.size();index++)
            {
                inputDataForSSHPolling.add(allData.get(index));
            }

            JsonObject jsonObject = (JsonObject) inputDataForSSHPolling.getJsonObject(0);

            System.out.println(jsonObject);

            jsonObject.put("category","polling");

            inputDataForSSHPolling.remove(0);

            inputDataForSSHPolling.add(0,jsonObject);

            System.out.println("FInal For Exe Demp "+inputDataForSSHPolling);

            promise.complete(inputDataForSSHPolling);
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
        finally {
            connectionPool.releaseConnection(connection);
        }
        return promise.future();
    }

    private Future<JsonObject> fetchDiscoveryDatabyID(String deviceID)
    {
        Promise<JsonObject> promise = Promise.promise();

        JsonObject credentialData = new JsonObject();

        Connection connection = connectionPool.getConnection();

        try
        {
            Operations operations = new Operations(connection);

            List<Map<String, Object>> allData = null;

            ArrayList<String> columns = new ArrayList<>();

            columns.add("IPADDRESS");

            columns.add("USERNAME");

            columns.add("PASSWORD");

            columns.add("NAME");

            columns.add("TYPE");

            String whereCluase = " WHERE DEVICEID = "+Integer.valueOf(deviceID);

            allData = operations.selectwithWhere("DISCOVERY_TABLE",columns,whereCluase);


            credentialData.put("username",allData.get(0).get("USERNAME"));

            credentialData.put("password",allData.get(0).get("PASSWORD"));

            credentialData.put("ip",allData.get(0).get("IPADDRESS"));

            credentialData.put("type",allData.get(0).get("TYPE"));

            credentialData.put("id",Integer.valueOf(deviceID));

            credentialData.put("name",allData.get(0).get("NAME"));

            promise.complete(credentialData);

        }
        catch (Exception exception)
        {
            promise.fail("Some Error in fetch the data to discover the device");

            exception.printStackTrace();
        }
        finally
        {
            connectionPool.releaseConnection(connection);
        }
        return promise.future();
    }


    private Future<JsonObject> monitorDeviceInfo(String deviceId)
    {
        Promise<JsonObject> promise = Promise.promise();

        JsonObject result = new JsonObject();

        Connection connection = connectionPool.getConnection();

        String query = "SELECT P.METRICTYPE,P.METRICVALUE FROM POLLING_TABLE P WHERE P.DEVICEID=? AND P.METRICTYPE IN ('cpu.user.percentage','system.name','uptime','disk.used.percentage','memory.used.percentage') AND P.TIMESTAMP = (SELECT MAX(TIMESTAMP) FROM POLLING_TABLE WHERE DEVICEID=? AND METRICTYPE = P.METRICTYPE)";

        try ( PreparedStatement statement = connection.prepareStatement(query) )
        {

            statement.setObject(1,Integer.parseInt(deviceId));

            statement.setObject(2,Integer.parseInt(deviceId));

            ResultSet resultSet = statement.executeQuery();

            ResultSetMetaData metaData = resultSet.getMetaData();

            int columnCount = metaData.getColumnCount();

            while (resultSet.next())
            {
                Map<String, Object> row = new HashMap<>();

                for (int iterator = 1; iterator <= columnCount; iterator++)
                {
                    row.put(metaData.getColumnName(iterator), resultSet.getObject(iterator));
                }
            }

            promise.complete(result);
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }

        System.out.println("Result "+result);

        return promise.future();
    }



    private Future<List<Map<String,Object>>> loadMonitorData()
    {
        Promise<List<Map<String,Object>>> promise = Promise.promise();

        List< Map< String, Object > > resultList = null;

        Connection connection = connectionPool.getConnection();

        try
        {
            Operations operations = new Operations(connection);

            ArrayList<String> columns = new ArrayList<>();

            columns.add("DEVICEID");

            columns.add("NAME");

            columns.add("IPADDRESS");

            columns.add("TYPE");

            columns.add("STATUS");

            resultList = operations.selectwithWhere("MONITOR_TABLE",columns,"");

            promise.complete(resultList);
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
        finally
        {
            connectionPool.releaseConnection(connection);
        }

        System.out.println("Result list "+resultList);

        return promise.future();
    }


    private Future<List<Map<String,Object>>> loadData()
    {
        Promise<List<Map<String,Object>>> promise = Promise.promise();

        List<Map<String, Object>> allData = null;

        Connection connection = connectionPool.getConnection();
        try
        {

            Operations operations = new Operations(connection);

            ArrayList<String> columns = new ArrayList<>();

            columns.add("NAME");

            columns.add("IPADDRESS");

            columns.add("TYPE");

            columns.add("DEVICEID");

            columns.add("PROVISION");

            allData = operations.selectwithWhere("DISCOVERY_TABLE",columns,"");

            promise.complete(allData);

        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
        finally
        {
            connectionPool.releaseConnection(connection);
        }

        return promise.future();
    }


    private Future<ArrayList> fetchDataForAvailabilityPolling()
    {
        Promise<ArrayList> promise = Promise.promise();

        Connection connection = connectionPool.getConnection();

        ArrayList<String> result = new ArrayList<>();

        try
        {
            Operations operations = new Operations(connection);

            ArrayList<String> columns = new ArrayList<>();

            columns.add("IPADDRESS");

            List<Map<String, Object>> selectResult = operations.selectwithWhere("MONITOR_TABLE",columns,"");


            System.out.println(selectResult.size());

            for(int index=0;index<selectResult.size();index++)
            {
                String ip = selectResult.get(index).get("IPADDRESS").toString();

                result.add(ip);
            }

            System.out.println("in fetchData function "+result);

            promise.complete(result);

        }
        catch (Exception exception)
        {
            promise.fail("Some error in fetching data for availibality polling");

            exception.printStackTrace();
        }
        finally
        {
            connectionPool.releaseConnection(connection);
        }

        return promise.future();

    }



    private Future<Boolean> AddDevice(JsonObject credentialData)
    {
        Promise<Boolean> promise = Promise.promise();

        Connection connection = connectionPool.getConnection();

       // add connection.isClosed();

        Operations operations = new Operations(connection);

        try
        {
            HashMap<String,Object> data = new HashMap<>();

            data.put("NAME",credentialData.getValue("name"));

            data.put("IPADDRESS",credentialData.getValue("ip"));

            data.put("TYPE",credentialData.getValue("type"));

            data.put("USERNAME",credentialData.getValue("username"));

            data.put("PASSWORD",credentialData.getValue("password"));

//            data.put("TAG",credentialData.getValue("tag"));

            operations.insert("DISCOVERY_TABLE",data,"");

            promise.complete(true);
        }
        catch (Exception exception)
        {
            promise.complete(false);

            exception.printStackTrace();
        }
        finally
        {
            connectionPool.releaseConnection(connection);
        }
        return promise.future();
    }

    private Future<Boolean> updateDiscovery(String deviceID)
    {
        Promise<Boolean> promise = Promise.promise();

        Connection connection = connectionPool.getConnection();

        Operations operations = new Operations(connection);

        try
        {
            HashMap<String,Object> data = new HashMap<>();

            data.put("PROVISION",true);

            String whereClause = "DEVICEID = " + Integer.valueOf(deviceID);

            operations.update("DISCOVERY_TABLE",data,whereClause);

            promise.complete(true);

        }
        catch (Exception exception)
        {
            promise.complete(false);

            exception.printStackTrace();
        }
        finally
        {
            connectionPool.releaseConnection(connection);
        }
        return promise.future();
    }


    private Future<Boolean> EditDevice(JsonObject credentialData)
    {
        Promise<Boolean> promise = Promise.promise();

        Connection connection = connectionPool.getConnection();

        Operations operations = new Operations(connection);

        try
        {
            HashMap<String,Object> data = new HashMap<>();

            data.put("NAME",credentialData.getValue("name"));

            data.put("IPADDRESS",credentialData.getValue("ip"));

            data.put("TYPE",credentialData.getValue("type"));

            data.put("USERNAME",credentialData.getValue("username"));

            data.put("PASSWORD",credentialData.getValue("password"));

            data.put("PROVISION",false);

            String whereClause = "DEVICEID = "+credentialData.getString("id");

            operations.update("DISCOVERY_TABLE",data,whereClause);

            promise.complete(true);
        }
        catch (Exception exception)
        {
            promise.complete(false);

            exception.printStackTrace();
        }
        finally
        {
            connectionPool.releaseConnection(connection);
        }
        return promise.future();
    }

    private Future<Boolean> provisionedDeviceDataDump(JsonObject credentialData)
    {
        Promise<Boolean> promise = Promise.promise();

        Connection connection = connectionPool.getConnection();

        Operations operations = new Operations(connection);

        try
        {
            HashMap<String,Object> data = new HashMap<>();

            data.put("DEVICEID",credentialData.getValue("id"));

            data.put("NAME",credentialData.getValue("name"));

            data.put("IPADDRESS",credentialData.getValue("ip"));

            data.put("TYPE",credentialData.getValue("type"));

            data.put("USERNAME",credentialData.getValue("username"));

            data.put("PASSWORD",credentialData.getValue("password"));

            operations.insert("MONITOR_TABLE",data,"");

            promise.complete(true);
        }
        catch (Exception exception)
        {
            promise.complete(false);

            exception.printStackTrace();
        }
        finally
        {
            connectionPool.releaseConnection(connection);
        }
        return promise.future();
    }



    private Future<Boolean> sshPollingDataDump(JsonNode data)
    {
        Promise<Boolean> promise = Promise.promise();

        Connection connection = connectionPool.getConnection();

        try
        {
            ObjectMapper mapper = new ObjectMapper();

            JsonNode jsonArray = mapper.readTree(String.valueOf(data));

            for (JsonNode jsonObject : jsonArray)
            {
                batchExecute(jsonObject);
            }
            promise.complete(true);
        }
        catch (Exception exception)
        {
            promise.complete(false);

            exception.printStackTrace();
        }
        finally
        {
            connectionPool.releaseConnection(connection);
        }
        return promise.future();

    }

    private Future<Boolean> fpingPollingDataDump(HashMap<String,String > map)
    {
        Promise<Boolean> promise = Promise.promise();

        Connection connection = connectionPool.getConnection();

        try
        {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO AVAILABILITY_TABLE(IPADDRESS,STATUS) VALUES(?,?)");

            for(Map.Entry m: map.entrySet())
            {
                preparedStatement.setString(1,m.getKey().toString());

                preparedStatement.setString(2,m.getValue().toString());

                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();

            PreparedStatement preparedStatement1 = connection.prepareStatement("UPDATE MONITOR_TABLE M SET M.STATUS=CASE WHEN M.IPADDRESS IN (SELECT DISTINCT IPADDRESS FROM AVAILABILITY_TABLE WHERE STATUS='Up' AND TIMESTAMP >= NOW()-INTERVAL '2' MINUTE) THEN 'Up' ELSE 'Down' END;");

            preparedStatement1.executeUpdate();

            promise.complete(true);
        }
        catch (Exception exception)
        {
            promise.fail("Some error in dumping avaliliblaty data in Ddatabse");

            promise.complete(false);

            exception.printStackTrace();
        }
        finally
        {
            connectionPool.releaseConnection(connection);
        }
        return promise.future();
    }

    private void batchExecute(JsonNode data)
    {
        Connection connection = connectionPool.getConnection();

        try
        {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO POLLING_TABLE VALUES(?,?,?,?,?)");

            if(!(data.isNull()))
            {
                preparedStatement.setObject(1,data.get("id").asText());

                preparedStatement.setObject(2,data.get("ip").asText());

                preparedStatement.setObject(3,"cpu.idle.percentage");

                preparedStatement.setObject(4,data.get("cpu.idle.percentage").asText());

                preparedStatement.setObject(5,data.get("timestamp").asText());

                preparedStatement.addBatch();

                preparedStatement.setObject(1,data.get("id").asText());

                preparedStatement.setObject(2,data.get("ip").asText());

                preparedStatement.setObject(3,"cpu.system.percentage");

                preparedStatement.setObject(4,data.get("cpu.system.percentage").asText());

                preparedStatement.setObject(5,data.get("timestamp").asText());

                preparedStatement.addBatch();

                preparedStatement.setObject(1,data.get("id").asText());

                preparedStatement.setObject(2,data.get("ip").asText());

                preparedStatement.setObject(3,"cpu.user.percentage");

                preparedStatement.setObject(4,data.get("cpu.user.percentage").asText());

                preparedStatement.setObject(5,data.get("timestamp").asText());

                preparedStatement.addBatch();

                preparedStatement.setObject(1,data.get("id").asText());

                preparedStatement.setObject(2,data.get("ip").asText());

                preparedStatement.setObject(3,"disk.used.percentage");

                preparedStatement.setObject(4,data.get("disk.used.percentage").asText());

                preparedStatement.setObject(5,data.get("timestamp").asText());

                preparedStatement.addBatch();

                preparedStatement.setObject(1,data.get("id").asText());

                preparedStatement.setObject(2,data.get("ip").asText());

                preparedStatement.setObject(3,"memory.free.percentage");

                preparedStatement.setObject(4,data.get("memory.free.percentage").asText());

                preparedStatement.setObject(5,data.get("timestamp").asText());

                preparedStatement.addBatch();

                preparedStatement.setObject(1,data.get("id").asText());

                preparedStatement.setObject(2,data.get("ip").asText());

                preparedStatement.setObject(3,"memory.used.percentage");

                preparedStatement.setObject(4,data.get("memory.used.percentage").asText());

                preparedStatement.setObject(5,data.get("timestamp").asText());

                preparedStatement.addBatch();

                preparedStatement.setObject(1,data.get("id").asText());

                preparedStatement.setObject(2,data.get("ip").asText());

                preparedStatement.setObject(3,"operating.system.name");

                preparedStatement.setObject(4,data.get("operating.system.name").asText());

                preparedStatement.setObject(5,data.get("timestamp").asText());

                preparedStatement.addBatch();

                preparedStatement.setObject(1,data.get("id").asText());

                preparedStatement.setObject(2,data.get("ip").asText());

                preparedStatement.setObject(3,"operating.system.version");

                preparedStatement.setObject(4,data.get("operating.system.version").asText());

                preparedStatement.setObject(5,data.get("timestamp").asText());

                preparedStatement.addBatch();

                preparedStatement.setObject(1,data.get("id").asText());

                preparedStatement.setObject(2,data.get("ip").asText());

                preparedStatement.setObject(3,"system.name");

                preparedStatement.setObject(4,data.get("system.name").asText());

                preparedStatement.setObject(5,data.get("timestamp").asText());

                preparedStatement.addBatch();

                preparedStatement.setObject(1,data.get("id").asText());

                preparedStatement.setObject(2,data.get("ip").asText());

                preparedStatement.setObject(3,"uptime");

                preparedStatement.setObject(4,data.get("uptime").asText());

                preparedStatement.setObject(5,data.get("timestamp").asText());

                preparedStatement.addBatch();

                preparedStatement.executeBatch();
            }

        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
        finally
        {
            connectionPool.releaseConnection(connection);
        }
    }




}