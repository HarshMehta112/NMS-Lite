package Verticle;

import Utils.Constants;
import Utils.UserConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import Database.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseVerticle extends AbstractVerticle
{
    private static final Logger logger = LoggerFactory.getLogger(DatabaseVerticle.class);

    static CustomConnectionPool connectionPool;

    static
    {
        connectionPool = CustomConnectionPool.getInstance();

        connectionPool.setURL(PropertiesFile.getURL());

        connectionPool.setUser(PropertiesFile.getUSER());

        connectionPool.setPassword(PropertiesFile.getPASSWORD());

        connectionPool.createConnectionPool();
    }

    EventBus eventBus;
    @Override
    public void start(Promise<Void> startPromise)
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

                    logger.info("Discovery Device added in Database");
                }
                else
                {
                    handler.reply("Some issue in adding Discovery Device in Database");

                    logger.error("Some issue in adding Discovery Device in Database");
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
                    eventBus.publish(Constants.DASHBOARD_DATA_EVENT_BUS_BRIDGE,handlers.result().toString());

                    logger.info("Dashboard Data published :"+handlers.result().toString());

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

                    logger.info("Discovery Device information edited in Database");
                }
                else
                {
                    handler.reply("Some issue in editing Discovery Device information in Database");

                    logger.info("Some issue in editing Discovery Device information in Database");
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

                        logger.error("Enable to fetch the Discovery Data from Database");
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
                        logger.info("Monitor Page Loading data "+loadMonitorData().result());

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

                        logger.info("SSH polling exe file input data "+fetchDataFromMonitorTable);

                        handler.reply(fetchDataFromMonitorTable);
                    }
                    else
                    {
                        logger.error("Enable to fetch the Discovery Data from Database for ssh polling");

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
                    if(fetchDataForAvailabilityPolling().succeeded())
                    {
                        handler.reply(fetchDataForAvailabilityPolling().result());
                    }
                    else
                    {
                        handler.reply("Enable to fetch the Discovery Data from Database for availibaliy polling");
                    }
                });
            },false);
        });


        eventBus.localConsumer(Constants.OUTPUT_AVAILABILITY_POLLING,handler->
        {
            vertx.executeBlocking(blockingHandler->
            {
                fpingPollingDataDump((HashMap<String, String>) handler.body()).onComplete(result->
                {
                    if(result.succeeded())
                    {
                        logger.info("availability Polling data dumped into database successfully");
                    }
                    else
                    {
                        logger.error("Some problem in availability polling data dumping "+result.cause().getMessage());
                    }
                });
            },false);
        });


        eventBus.localConsumer(Constants.OUTPUT_SSH_POLLING,handler->
        {
            vertx.executeBlocking(blockingHandler->
            {
                sshPollingDataDump((JsonNode) handler.body()).onComplete(result->
                {
                    if(result.succeeded())
                    {
                        logger.info("ssh Polling data dumped into database successfully");
                    }
                    else
                    {
                        logger.info("Some problem in ssh polling data dumping "+result.cause().getMessage());
                    }
                });
            },false);
        });


        eventBus.localConsumer(Constants.DELETE_DISCOVERY_DEVICE,handler->
        {
            vertx.executeBlocking(blockingHandler->
            {
                JsonObject deviceId = (JsonObject) handler.body();

                DeleteDevice(deviceId.getString("id")).onComplete(result->
                {
                    if(DeleteDevice(deviceId.getString("id")).succeeded())
                    {
                        handler.reply("Device deleted successfully");

                        logger.info("Device deleted successfully");
                    }
                    else
                    {
                        logger.error("Enbale to delete discovery Device");

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

                    logger.info("Monitor Device information "+resultInfo);

                    if(monitorDeviceInfo(handler.body().toString()).succeeded())
                    {
                        handler.reply(resultInfo);
                    }
                });
            },false);
        });



        eventBus.localConsumer(Constants.DELETE_MONITOR_DEVICE,handler->
        {
            vertx.executeBlocking(blockingHandler->
            {
                JsonObject deviceId = new JsonObject();

                deleteMonitorDevice(deviceId.getString("id")).onComplete(result->
                {
                    if(deleteMonitorDevice(deviceId.getString("id")).succeeded())
                    {
                        handler.reply("Monitor Device deleted successfully");

                        logger.info("Monitor Device deleted successfully");
                    }
                    else
                    {
                        handler.reply("Enbale to delete Monitor Device");

                        logger.info("Enbale to delete Monitor Device");
                    }
                });
            },false);
        });


        eventBus.localConsumer(Constants.RUN_PROVISION,handler->
        {
            vertx.executeBlocking(blockingHandler->
            {
                JsonObject deviceId = (JsonObject) handler.body();

                fetchDiscoveryDatabyID(deviceId.getString("id")).onComplete(result->
                {
                    JsonObject data = result.result();

                    logger.info("JSON result of RUN PROVISION "+data);

                    provisionedDeviceDataDump(data).onComplete(result1 ->
                    {
                        if(result1.succeeded())
                        {
                            logger.info("Discovery Device Added Succssfullly into Monitor Table");
                        }
                        else
                        {
                            logger.info("Some error occurred in adding discovery device into Monitor Tbale"+result1.cause().getMessage());
                        }
                    });
                });
            },false);

        });


        eventBus.localConsumer(Constants.RUN_DISCOVERY,handler->
        {
            vertx.executeBlocking(blockingHandler->
            {
                JsonObject deviceId = (JsonObject) handler.body();

                fetchDiscoveryDatabyID(deviceId.getString("id")).onComplete(result->
                {
                    eventBus.request(Constants.RUN_DISCOVERY_SPAWN_PEROCESS,fetchDiscoveryDatabyID(deviceId.getString("id")).result(),response->
                    {
                        if(response.succeeded())
                        {
                            if(!deviceId.getString("id").equals(""))
                            {
                                logger.info("Device Id of discovery device "+deviceId.getString("id"));

                                if(updateDiscovery(deviceId.getString("id")).succeeded())
                                {
                                    logger.info("Discovery Table Updated with Provision value");
                                }
                                else
                                {
                                    logger.info("Some Problem in Updating the Provision value");
                                }
                            }
                        }
                        else
                        {
                            logger.info("Some error in getting the output from .exe file");
                        }
                    });

                    if(fetchDiscoveryDatabyID(deviceId.getString("id")).succeeded())
                    {
                        handler.reply("Device discovered successfully");

                        logger.info("Device discovered successfully");
                    }
                    else
                    {
                        handler.reply("Device not discovered");

                        logger.info("Device not discovered");
                    }
                });
            },false);
        });

        vertx.setPeriodic(UserConfig.DASHBOARD_REFRESH_DELAY, handler->
        {
            Promise<List<JsonArray>> promise = Promise.promise();

            dashBoardDataLoad(promise);

            promise.future().onComplete(handlers->
            {
                logger.info("Dashboard refreshed data "+handlers.result());

                eventBus.publish(Constants.DASHBOARD_DATA_EVENT_BUS_BRIDGE,handlers.result().toString());

            });
        });

        startPromise.complete();

    }



    private void dashBoardDataLoad(Promise <List<JsonArray>> promise)
    {
        List<JsonArray> dashBoardData = new ArrayList<>();

        vertx.executeBlocking(blockingHandler->
        {
            Connection connection = connectionPool.getConnection();

            try
            {
                if (!(connection.isClosed()))
                {
                    Operations operations = new Operations(connection);

                    String query = "SELECT m.ipaddress, MAX(p.METRICVALUE) AS memory FROM POLLING_TABLE p, MONITOR_TABLE m WHERE p.metricType = 'memory.used.percentage' AND p.timestamp >= NOW() - INTERVAL '1000' MINUTE AND p.IPADDRESS = m.IPADDRESS GROUP BY p.ipaddress ORDER BY memory DESC LIMIT 5;";

                    List<Map<String, Object>> map = operations.selectQuery(query);

                    JsonArray top5MaxMemory = new JsonArray(map);

                    logger.info("Top 5 Memory " + top5MaxMemory);

                    dashBoardData.add(top5MaxMemory);

                    map = operations.selectQuery("SELECT m.ipaddress, MAX(p.METRICVALUE) AS disk FROM POLLING_TABLE p, MONITOR_TABLE m WHERE p.metricType = 'disk.used.percentage' AND p.timestamp >= NOW() - INTERVAL '1000' MINUTE AND p.IPADDRESS = m.IPADDRESS GROUP BY p.ipaddress ORDER BY disk DESC LIMIT 5;");

                    JsonArray top5MaxDisk = new JsonArray(map);

                    logger.info("Top 5 Memory " + top5MaxDisk);

                    dashBoardData.add(top5MaxDisk);

                    map = operations.selectQuery("SELECT m.ipaddress, MAX(p.METRICVALUE) AS cpu FROM POLLING_TABLE p, MONITOR_TABLE m WHERE p.metricType = 'cpu.user.percentage' AND p.timestamp >= NOW() - INTERVAL '1000' MINUTE AND p.IPADDRESS = m.IPADDRESS GROUP BY p.ipaddress ORDER BY cpu DESC LIMIT 5;");

                    JsonArray top5MaxCPU = new JsonArray(map);

                    logger.info("Top 5 CPU " + top5MaxCPU);

                    dashBoardData.add(top5MaxCPU);

                    map = operations.selectQuery("SELECT COUNT(CASE WHEN STATUS='Up' THEN 1 END) as UP,COUNT(CASE WHEN STATUS='Down' THEN 1 END) as DOWN FROM (SELECT MONITOR_TABLE.DEVICEID, MONITOR_TABLE.IPADDRESS, MONITOR_TABLE.TYPE,MONITOR_TABLE.NAME, AVAILABILITY_TABLE.STATUS FROM MONITOR_TABLE INNER JOIN AVAILABILITY_TABLE ON MONITOR_TABLE.IPADDRESS = AVAILABILITY_TABLE.IPADDRESS ORDER BY AVAILABILITY_TABLE.TIMESTAMP DESC lIMIT (SELECT COUNT(IPADDRESS) FROM MONITOR_TABLE))");

                    JsonArray status = new JsonArray(map);

                    dashBoardData.add(status);

                    promise.complete(dashBoardData);
                }
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

        try
        {
            if(!(connection.isClosed()))
            {
                Operations operations = new Operations(connection);

                String whereClause = "DEVICEID = " + Integer.valueOf(deviceID);

                operations.delete("DISCOVERY_TABLE",whereClause);

                promise.complete(true);
            }
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


        try
        {
            if(!(connection.isClosed()))
            {
                Operations operations = new Operations(connection);

                String whereClause = "DEVICEID = " + Integer.valueOf(deviceID);

                operations.delete("MONITOR_TABLE",whereClause);

                promise.complete(true);
            }
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

        JsonArray inputDataForSSHPolling;

        Connection connection = connectionPool.getConnection();

        try
        {
            if(!(connection.isClosed()))
            {
                Operations operations = new Operations(connection);

                List<Map<String, Object>> allData;

                String query = "SELECT IPADDRESS,USERNAME,PASSWORD,TYPE,DEVICEID FROM MONITOR_TABLE WHERE STATUS='Up'";

                allData = operations.selectQuery(query);

                logger.info("Data From Monitor Table for SSH Polling "+allData);

                inputDataForSSHPolling = new JsonArray();

                for(int index=0;index<allData.size();index++)
                {
                    inputDataForSSHPolling.add(allData.get(index));
                }

                JsonObject jsonObject =  inputDataForSSHPolling.getJsonObject(0);

                jsonObject.put("category","polling");

                inputDataForSSHPolling.remove(0);

                inputDataForSSHPolling.add(0,jsonObject);

                logger.info("Input data for SSH polling "+inputDataForSSHPolling);

                promise.complete(inputDataForSSHPolling);
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
        return promise.future();
    }

    private Future<JsonObject> fetchDiscoveryDatabyID(String deviceID)
    {
        Promise<JsonObject> promise = Promise.promise();

        JsonObject credentialData = new JsonObject();

        Connection connection = connectionPool.getConnection();

        try
        {
            if(!(connection.isClosed()))
            {
                Operations operations = new Operations(connection);

                List<Map<String, Object>> allData;

                String query = "SELECT IPADDRESS,USERNAME,PASSWORD,NAME,TYPE FROM DISCOVERY_TABLE WHERE DEVICEID = "+Integer.valueOf(deviceID);

                allData = operations.selectQuery(query);

                credentialData.put("username",allData.get(0).get("USERNAME"));

                credentialData.put("password",allData.get(0).get("PASSWORD"));

                credentialData.put("ip",allData.get(0).get("IPADDRESS"));

                credentialData.put("type",allData.get(0).get("TYPE"));

                credentialData.put("id",Integer.valueOf(deviceID));

                credentialData.put("name",allData.get(0).get("NAME"));

                promise.complete(credentialData);

            }
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

    //use select query method
    private Future<JsonObject> monitorDeviceInfo(String deviceId)
    {
        Promise<JsonObject> promise = Promise.promise();

        JsonObject result = new JsonObject();

        Connection connection = connectionPool.getConnection();

        String query = "SELECT MAX(CASE WHEN P.METRICTYPE = 'cpu.user.percentage' THEN P.METRICVALUE END) AS \"cpu.user.percentage\",MAX(CASE WHEN P.METRICTYPE = 'system.name' THEN P.METRICVALUE END) AS \"system.name\",MAX(CASE WHEN P.METRICTYPE = 'uptime' THEN P.METRICVALUE END) AS \"uptime\",MAX(CASE WHEN P.METRICTYPE = 'disk.used.percentage' THEN P.METRICVALUE END) AS \"disk.used.percentage\",MAX(CASE WHEN P.METRICTYPE = 'memory.used.percentage' THEN P.METRICVALUE END) AS \"memory.used.percentage\" FROM POLLING_TABLE P WHERE P.DEVICEID = ? AND P.METRICTYPE IN ('cpu.user.percentage', 'system.name', 'uptime', 'disk.used.percentage', 'memory.used.percentage') AND P.TIMESTAMP = (SELECT MAX(TIMESTAMP) FROM POLLING_TABLE WHERE DEVICEID = ? AND METRICTYPE = P.METRICTYPE);";

        try ( PreparedStatement statement = connection.prepareStatement(query) )
        {
            if(!(connection.isClosed()))
            {
                statement.setObject(1,Integer.parseInt(deviceId));

                statement.setObject(2,Integer.parseInt(deviceId));

                logger.info("Id from device info page "+Integer.parseInt(deviceId));

                ResultSet resultSet = statement.executeQuery();

                ResultSetMetaData metaData = resultSet.getMetaData();

                int columnCount = metaData.getColumnCount();

                while ( resultSet.next() )
                {
                    for ( int iterator = 1; iterator <= columnCount; iterator++ )
                    {
                        result.put(metaData.getColumnName(iterator), resultSet.getObject(iterator));
                    }
                }
                promise.complete(result);
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

        logger.info("Result form device info "+result);

        return promise.future();
    }



    private Future<List<Map<String,Object>>> loadMonitorData()
    {
        Promise<List<Map<String,Object>>> promise = Promise.promise();

        List< Map< String, Object > > resultList = null;

        Connection connection = connectionPool.getConnection();

        try
        {
            if(!(connection.isClosed()))
            {
                Operations operations = new Operations(connection);

                String query = "SELECT DEVICEID,NAME,IPADDRESS,TYPE,STATUS FROM MONITOR_TABLE";

                resultList = operations.selectQuery(query);

                //null check
                promise.complete(resultList);
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

        logger.info("Load the Monitor Table data "+resultList);

        return promise.future();
    }


    private Future<List<Map<String,Object>>> loadData()
    {
        Promise<List<Map<String,Object>>> promise = Promise.promise();

        List<Map<String, Object>> allData;

        Connection connection = connectionPool.getConnection();
        try
        {
            if(!(connection.isClosed()))
            {
                Operations operations = new Operations(connection);

                String query = "SELECT NAME,IPADDRESS,TYPE,DEVICEID,PROVISION FROM DISCOVERY_TABLE;";

                allData = operations.selectQuery(query);

                promise.complete(allData);
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

        return promise.future();
    }


    private Future<ArrayList> fetchDataForAvailabilityPolling()
    {
        Promise<ArrayList> promise = Promise.promise();

        Connection connection = connectionPool.getConnection();

        ArrayList<String> result = new ArrayList<>();

        try
        {
            if(!(connection.isClosed()))
            {
                Operations operations = new Operations(connection);

                String query = "SELECT IPADDRESS FROM MONITOR_TABLE ";

                List<Map<String, Object>> selectResult = operations.selectQuery(query);

                for(int index=0;index<selectResult.size();index++)
                {
                    String ip = selectResult.get(index).get("IPADDRESS").toString();

                    result.add(ip);
                }
                logger.info("Fetched Data for Availiblity Polling "+result);

                promise.complete(result);

            }
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


        try
        {
            if(!(connection.isClosed()))
            {
                Operations operations = new Operations(connection);

                HashMap<String, Object> data = new HashMap<>();

                data.put("NAME",credentialData.getValue("name"));

                data.put("IPADDRESS",credentialData.getValue("ip"));

                data.put("TYPE",credentialData.getValue("type"));

                data.put("USERNAME",credentialData.getValue("username"));

                data.put("PASSWORD",credentialData.getValue("password"));

                operations.insert("DISCOVERY_TABLE",data);

                promise.complete(true);
            }
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

        try
        {
            if(!(connection.isClosed()))
            {
                Operations operations = new Operations(connection);

                HashMap<String, Object> data = new HashMap<>();

                data.put("PROVISION",true);

                String whereClause = "DEVICEID = " + Integer.valueOf(deviceID);

                operations.update("DISCOVERY_TABLE",data,whereClause);

                promise.complete(true);
            }

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

        try
        {
            if(!(connection.isClosed()))
            {
                Operations operations = new Operations(connection);

                HashMap<String, Object> data = new HashMap<>();

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

        try
        {
            if(!(connection.isClosed()))
            {
                Operations operations = new Operations(connection);

                HashMap<String, Object> data = new HashMap<>();

                data.put("DEVICEID",credentialData.getValue("id"));

                data.put("NAME",credentialData.getValue("name"));

                data.put("IPADDRESS",credentialData.getValue("ip"));

                data.put("TYPE",credentialData.getValue("type"));

                data.put("USERNAME",credentialData.getValue("username"));

                data.put("PASSWORD",credentialData.getValue("password"));

                operations.insert("MONITOR_TABLE",data);

                promise.complete(true);
            }
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


    //update batch operation
    private Future<Boolean> sshPollingDataDump(JsonNode data)
    {
        Promise<Boolean> promise = Promise.promise();

        Connection connection = connectionPool.getConnection();

        String[] insertData = new String[]{"cpu.idle.percentage","cpu.system.percentage","cpu.user.percentage","disk.used.percentage","memory.free.percentage","memory.used.percentage","operating.system.name","operating.system.version","system.name","uptime"};

        try(PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO POLLING_TABLE VALUES(?,?,?,?,?)"))
        {
            if(!(connection.isClosed()))
            {
                ObjectMapper mapper = new ObjectMapper();

                JsonNode jsonArray = mapper.readTree(String.valueOf(data));

                for (JsonNode jsonObject : jsonArray)
                {
                    for (String dataName: insertData)
                    {
                        preparedStatement.setObject(1, jsonObject.get("id").asText());

                        preparedStatement.setObject(2,jsonObject.get("ip").asText());

                        preparedStatement.setObject(3,dataName);

                        preparedStatement.setObject(4,jsonObject.get(dataName).asText());

                        preparedStatement.setObject(5,jsonObject.get("timestamp").asText());

                        preparedStatement.addBatch();
                    }
                }
                preparedStatement.executeBatch();

                promise.complete(true);
            }
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

    //comman class for insert update
    private Future<Boolean> fpingPollingDataDump(HashMap<String,String > map)
    {
        Promise<Boolean> promise = Promise.promise();

        Connection connection = connectionPool.getConnection();

        try
        {
            if(!(connection.isClosed()))
            {
                PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO AVAILABILITY_TABLE(IPADDRESS,STATUS) VALUES(?,?)");

                for(Map.Entry<String, String> m: map.entrySet())
                {
                    preparedStatement.setString(1,m.getKey().toString());

                    preparedStatement.setString(2,m.getValue().toString());

                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();

                PreparedStatement statementforMonitorTableUpdate = connection.prepareStatement("UPDATE MONITOR_TABLE M SET M.STATUS=CASE WHEN M.IPADDRESS IN (SELECT DISTINCT IPADDRESS FROM AVAILABILITY_TABLE WHERE STATUS='Up' AND TIMESTAMP >= NOW()-INTERVAL '2' MINUTE) THEN 'Up' ELSE 'Down' END;");

                statementforMonitorTableUpdate.executeUpdate();

                promise.complete(true);
            }
        }
        catch (Exception exception)
        {
            promise.fail("Some error in dumping availability data in Database");

            promise.complete(false);

            exception.printStackTrace();
        }
        finally
        {
            connectionPool.releaseConnection(connection);
        }
        return promise.future();
    }



}