package Verticle;

import Utils.Constants;
import Utils.UserConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.*;
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

    EventBus eventBus;
    @Override
    public void start(Promise<Void> startPromise)
    {

        connectionPool = CustomConnectionPool.getInstance();

        connectionPool.setURL(PropertiesFile.getURL());

        connectionPool.setUser(PropertiesFile.getUSER());

        connectionPool.setPassword(PropertiesFile.getPASSWORD());

        if(!connectionPool.createConnectionPool())
        {
            startPromise.fail("Error in creating connection pool");

            connectionPool.closeAllConnections();
        }

        eventBus = vertx.eventBus();

        eventBus.localConsumer(Constants.DATABASE_CONSUMER).handler(message->
        {
            JsonObject receivedJsonObject = (JsonObject) message.body();

            switch (receivedJsonObject.getString("RequestFrom"))
            {
                case Constants.ADD_DISCOVERY_DEVICE:
                {
                    JsonObject deviceDetails = (JsonObject) message.body();

                    vertx.executeBlocking(blockingHandler->
                    {
                        if(AddDevice(deviceDetails).succeeded())
                        {
                            message.reply("Discovery Device added in Database");

                            logger.debug("Discovery Device added in Database");
                        }
                        else
                        {
                            message.reply("Some issue in adding Discovery Device in Database");

                            logger.error("Some issue in adding Discovery Device in Database");
                        }
                    },false);
                    break;
                }

                case Constants.LOAD_MONITOR_DEVICE:
                {
                    vertx.executeBlocking(blockingHandler->
                    {
                        loadMonitorData().onComplete(result->
                        {
                            if(result.succeeded())
                            {
                                logger.debug("Monitor Page Loading data "+result.result());

                                message.reply(new JsonArray(loadMonitorData().result()));
                            }
                            else
                            {
                                message.reply("Enable to fetch the Monitor Data from Database");

                                logger.error("Enable to fetch the Monitor Data from Database");
                            }
                        });
                    },false);

                    break;
                }

                case Constants.GET_ALL_DISCOVERY_DEVICE:
                {
                    vertx.executeBlocking(blockingHandler->
                    {
                        loadDiscoveryData().onComplete(result->
                        {
                            if(result.succeeded())
                            {
                                message.reply(new JsonArray(result.result()));
                            }
                            else
                            {
                                message.reply("Enable to fetch the Discovery Data from Database");

                                logger.error("Enable to fetch the Discovery Data from Database");
                            }
                        });
                    },false);

                    break;
                }

                case Constants.DELETE_DISCOVERY_DEVICE:
                {
                    vertx.executeBlocking(blockingHandler->
                    {
                        JsonObject deviceId = (JsonObject) message.body();

                        DeleteDevice(deviceId.getString("id")).onComplete(result->
                        {
                            if(result.succeeded())
                            {
                                message.reply("Device deleted successfully");

                                logger.debug("Device deleted successfully");
                            }
                            else
                            {
                                logger.error("Enbale to delete discovery Device");

                                message.reply("Enbale to delete discovery Device");
                            }
                        });
                    },false);
                    break;
                }

                case Constants.DELETE_MONITOR_DEVICE:
                {
                    vertx.executeBlocking(blockingHandler->
                    {
                        JsonObject deviceId = (JsonObject) message.body();

                        deleteMonitorDevice(deviceId.getString("id")).onComplete(result->
                        {
                            if(deleteMonitorDevice(deviceId.getString("id")).succeeded())
                            {
                                message.reply("Monitor Device deleted successfully");

                                logger.debug("Monitor Device deleted successfully");
                            }
                            else
                            {
                                message.reply("Enbale to delete Monitor Device");

                                logger.debug("Enbale to delete Monitor Device");
                            }
                        });
                    },false);
                    break;
                }

                case Constants.RUN_DISCOVERY:
                {
                    vertx.executeBlocking(blockingHandler->
                    {
                        JsonObject deviceId = (JsonObject) message.body();

                        fetchDiscoveryDatabyID(deviceId.getString("id")).onComplete(result->
                        {
                            if(result.succeeded())
                            {
                                eventBus.request(Constants.RUN_DISCOVERY_SPAWN_PEROCESS,result.result(),response->
                                {
                                    if(response.succeeded())
                                    {
                                        if(response.result().body().equals("true"))
                                        {
                                            logger.debug("Device Id of discovery device "+deviceId.getString("id"));

                                            if(updateDiscovery(deviceId.getString("id"),true).succeeded())
                                            {
                                                logger.debug("Discovery Table Updated with Provision value");
                                            }
                                            else
                                            {
                                                logger.debug("Some Problem in Updating the Provision value");
                                            }
                                        }
                                        else
                                        {
                                            if(updateDiscovery(deviceId.getString("id"),false).succeeded())
                                            {
                                                logger.debug("Discovery Table Updated with Provision value");

                                                message.reply("Device discovered successfully");

                                            }
                                            else
                                            {
                                                message.reply("Device not discovered");

                                                logger.debug("Some Problem in Updating the Provision value");
                                            }
                                        }
                                    }
                                    else
                                    {
                                        logger.debug("Some error in getting the output from .exe file");
                                    }
                                });
                            }
                        });
                    },false);
                    break;
                }

                case Constants.RUN_PROVISION:
                {
                    vertx.executeBlocking(blockingHandler->
                    {
                        JsonObject deviceId = (JsonObject) message.body();

                        fetchDiscoveryDatabyID(deviceId.getString("id")).onComplete(result->
                        {
                            if(result.succeeded())
                            {
                                JsonObject data = result.result();

                                logger.debug("JSON result of RUN PROVISION "+data);

                                provisionedDeviceDataDump(data).onComplete(result1 ->
                                {
                                    if(result1.succeeded())
                                    {
                                        logger.debug("Discovery Device Added Succssfullly into Monitor Table");
                                    }
                                    else
                                    {
                                        logger.debug("Some error occurred in adding discovery device into Monitor Tbale"+result1.cause().getMessage());
                                    }
                                });
                            }
                            else
                            {
                                logger.error("Some error occurred in  fetchDiscoveryDatabyID method output");
                            }
                        });
                    },false);
                    break;
                }

                case Constants.DASHBOARD_LOAD:
                {
                    vertx.executeBlocking(blockingHandler->
                    {
                        Promise<List<JsonArray>> promise = Promise.promise();

                        dashBoardDataLoad(promise);

                        promise.future().onComplete(handlers->
                        {
                            eventBus.publish(Constants.DASHBOARD_DATA_EVENT_BUS_BRIDGE,handlers.result().toString());

                            logger.debug("Dashboard Data published :"+handlers.result().toString());

                        });
                    },false);
                    break;
                }

                case Constants.MONITOR_DEVICE_INFO:
                {
                    vertx.executeBlocking(blockingHandler->
                    {
                        JsonObject deviceId = (JsonObject) message.body();

                        monitorDeviceInfo(deviceId.getString("id")).onComplete(result->
                        {
                            JsonObject resultInfo  = result.result();

                            logger.debug("Monitor Device information "+resultInfo);

                            message.reply(resultInfo);

                        });
                    },false);
                    break;
                }

                case Constants.EDIT_DISCOVERY_DEVICE:
                {
                    JsonObject deviceDetails = (JsonObject) message.body();

                    vertx.executeBlocking(blockingHandler->
                    {
                        if(EditDevice(deviceDetails).succeeded())
                        {
                            message.reply("Discovery Device information edited in Database");

                            logger.debug("Discovery Device information edited in Database");
                        }
                        else
                        {
                            message.reply("Some issue in editing Discovery Device information in Database");

                            logger.debug("Some issue in editing Discovery Device information in Database");
                        }
                    },false);
                    break;
                }

                case Constants.SSH_POLLING_PROCESS_TRIGGERED:
                {
                    vertx.executeBlocking(blockingHandler->
                    {
                        fetchMonitorData().onComplete(result->
                        {
                            if(result.succeeded())
                            {
                                JsonArray fetchDataFromMonitorTable = result.result();

                                logger.debug("SSH polling exe file input data "+fetchDataFromMonitorTable);

                                message.reply(fetchDataFromMonitorTable);
                            }
                            else
                            {
                                logger.error("Enable to fetch the Discovery Data from Database for ssh polling");

                                message.reply("Enable to fetch the Discovery Data from Database for ssh polling");
                            }
                        });
                    },false);
                    break;
                }

                case Constants.AVAILABILITY_POLLING_PROCESS_TRIGGERED:
                {
                    vertx.executeBlocking(blockingHandler->
                    {
                        fetchDataForAvailabilityPolling().onComplete(arrayListAsyncResult ->
                        {
                            if(arrayListAsyncResult.succeeded())
                            {
                                message.reply(arrayListAsyncResult.result());
                            }
                            else
                            {
                                message.reply("Enable to fetch the Discovery Data from Database for availibaliy polling");
                            }
                        });
                    },false);
                    break;
                }

            }
        });


        eventBus.localConsumer(Constants.OUTPUT_AVAILABILITY_POLLING,handler->
        {
            vertx.executeBlocking(blockingHandler->
            {
                fpingPollingDataDump((HashMap<String, String>) handler.body()).onComplete(result->
                {
                    if(result.succeeded())
                    {
                        logger.debug("availability Polling data dumped into database successfully");
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
                        handler.reply("ssh Polling data dumped into database successfully");

                        logger.debug("ssh Polling data dumped into database successfully");
                    }
                    else
                    {
                        handler.reply("ssh Polling data not dumped into database");

                        logger.error("Some problem in ssh polling data dumping "+result.cause().getMessage());
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
                logger.debug("Dashboard refreshed data "+handlers.result());

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

                    String query = "SELECT m.ipaddress, MAX(p.METRICVALUE) AS memory FROM POLLING_TABLE p, MONITOR_TABLE m WHERE p.metricType = 'memory.used.percentage' AND p.timestamp >= NOW() - INTERVAL '5' MINUTE AND p.deviceid = m.deviceid GROUP BY p.deviceid ORDER BY memory DESC LIMIT 5;";

                    List<Map<String, Object>> map = operations.selectQuery(query);

                    JsonArray top5MaxMemory = null;

                    if(map!=null)
                    {
                        top5MaxMemory = new JsonArray(map);

                        dashBoardData.add(top5MaxMemory);
                    }

                    logger.debug("Top 5 Memory " + top5MaxMemory);

                    map = operations.selectQuery("SELECT m.ipaddress, MAX(p.METRICVALUE) AS disk FROM POLLING_TABLE p, MONITOR_TABLE m WHERE p.metricType = 'disk.used.percentage' AND p.timestamp >= NOW() - INTERVAL '5' MINUTE AND p.deviceid = m.deviceid GROUP BY p.deviceid ORDER BY disk DESC LIMIT 5;");

                    JsonArray top5MaxDisk = null;

                    if(map!=null)
                    {
                        top5MaxDisk = new JsonArray(map);

                        dashBoardData.add(top5MaxDisk);
                    }

                    logger.debug("Top 5 Memory " + top5MaxDisk);

                    map = operations.selectQuery("SELECT m.ipaddress, MAX(p.METRICVALUE) AS cpu FROM POLLING_TABLE p, MONITOR_TABLE m WHERE p.metricType = 'cpu.user.percentage' AND p.timestamp >= NOW() - INTERVAL '5' MINUTE AND p.deviceid = m.deviceid GROUP BY p.deviceid ORDER BY cpu DESC LIMIT 5;");

                    JsonArray top5MaxCPU = null;

                    if(map!=null)
                    {
                        top5MaxCPU = new JsonArray(map);

                        dashBoardData.add(top5MaxCPU);
                    }

                    logger.debug("Top 5 CPU " + top5MaxCPU);

                    map = operations.selectQuery("SELECT COUNT(*) FILTER (WHERE STATUS = 'Up') AS UP, COUNT(*) FILTER (WHERE STATUS = 'Down') AS DOWN, COUNT(*) FILTER (WHERE STATUS = 'Unknown') AS UNKNOWNS, COUNT(*) AS TOTAL FROM MONITOR_TABLE;");

                    JsonArray status ;

                    if(map!=null)
                    {
                        status = new JsonArray(map);

                        dashBoardData.add(status);
                    }

                    promise.complete(dashBoardData);
                }
                else
                {
                    promise.fail("dashBoardDataLoad method promise failed due to connection closed");
                }
            }
            catch (Exception exception)
            {
                promise.fail("Some error in fetching the dash board data");

                logger.error(exception.getCause().getMessage());
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

                int deleteCount = operations.delete("DISCOVERY_TABLE",whereClause);

                if(deleteCount>0)
                {
                    promise.complete(true);
                }
                else
                {
                    promise.complete(false);
                }
            }
            else
            {
                promise.complete(false);

                promise.fail("loadDiscoveryData method promise failed due to connection closed");
            }
        }
        catch (Exception exception)
        {
            promise.complete(false);

            logger.error(exception.getCause().getMessage());
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

                int deleteCount = operations.delete("MONITOR_TABLE",whereClause);

                if(deleteCount>0)
                {
                    promise.complete(true);
                }
                else
                {
                    promise.fail("deleteMonitorDevice method promise failed due to delete count < 0");
                }
            }
            else
            {
                promise.fail("deleteMonitorDevice method promise failed due to connection closed");
            }
        }
        catch (Exception exception)
        {
            promise.complete(false);

            logger.error(exception.getCause().getMessage());
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

                logger.debug("Data From Monitor Table for SSH Polling "+allData);

                if(allData!=null)
                {
                    inputDataForSSHPolling = new JsonArray();

                    for(int index=0;index<allData.size();index++)
                    {
                        inputDataForSSHPolling.add(allData.get(index));
                    }

                    JsonObject jsonObject =  inputDataForSSHPolling.getJsonObject(0);

                    if(jsonObject!=null)
                    {
                        jsonObject.put("category","polling");

                        inputDataForSSHPolling.remove(0);

                        inputDataForSSHPolling.add(0,jsonObject);

                        logger.debug("Input data for SSH polling "+inputDataForSSHPolling);

                        promise.complete(inputDataForSSHPolling);
                    }
                }
            }
            else
            {
                promise.fail("fetchMonitorData method promise failed due to connection failed");
            }
        }
        catch (Exception exception)
        {
            logger.error(exception.getCause().getMessage());
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

                if(allData!=null)
                {
                    credentialData.put("username",allData.get(0).get("USERNAME"));

                    credentialData.put("password",allData.get(0).get("PASSWORD"));

                    credentialData.put("ip",allData.get(0).get("IPADDRESS"));

                    credentialData.put("type",allData.get(0).get("TYPE"));

                    credentialData.put("id",Integer.valueOf(deviceID));

                    credentialData.put("name",allData.get(0).get("NAME"));

                    promise.complete(credentialData);

                }
                else
                {
                    promise.fail("loadDiscoveryDeviceById method promise failed due to null data");
                }
            }
            else
            {
                promise.fail("loadDiscoveryDeviceById method promise failed connection closed");
            }
        }
        catch (Exception exception)
        {
            promise.fail("Some Error in fetch the data to discover the device");

            logger.error(exception.getCause().getMessage());
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

        String query = "SELECT MAX(CASE WHEN P.METRICTYPE = 'cpu.user.percentage' THEN P.METRICVALUE END) AS \"cpu.user.percentage\",MAX(CASE WHEN P.METRICTYPE = 'system.name' THEN P.METRICVALUE END) AS \"system.name\",MAX(CASE WHEN P.METRICTYPE = 'uptime' THEN P.METRICVALUE END) AS \"uptime\",MAX(CASE WHEN P.METRICTYPE = 'disk.used.percentage' THEN P.METRICVALUE END) AS \"disk.used.percentage\",MAX(CASE WHEN P.METRICTYPE = 'memory.used.percentage' THEN P.METRICVALUE END) AS \"memory.used.percentage\" FROM POLLING_TABLE P WHERE P.DEVICEID = ? AND P.METRICTYPE IN ('cpu.user.percentage', 'system.name', 'uptime', 'disk.used.percentage', 'memory.used.percentage') AND P.TIMESTAMP = (SELECT MAX(TIMESTAMP) FROM POLLING_TABLE WHERE DEVICEID = ? AND METRICTYPE = P.METRICTYPE);";

        try ( PreparedStatement statement = connection.prepareStatement(query) )
        {
            if(!(connection.isClosed()))
            {
                statement.setObject(1,Integer.valueOf(deviceId));

                statement.setObject(2,Integer.valueOf(deviceId));

                logger.debug("Id from device info page "+Integer.valueOf(deviceId));

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
            else
            {
                promise.fail("monitorDeviceInfo method rpomise failed due to connection failed");
            }
        }
        catch (Exception exception)
        {
            logger.error(exception.getCause().getMessage());
        }
        finally
        {
            connectionPool.releaseConnection(connection);
        }
        logger.debug("Result form device info "+result);

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

                if(resultList!=null)
                {
                    promise.complete(resultList);
                }
                else
                {
                    promise.fail("loadMontorData method promise failed due to null result");
                }
            }
            else
            {
                promise.fail("loadMontorData method promise failed due to connection closed");
            }
        }
        catch (Exception exception)
        {
            logger.error(exception.getCause().getMessage());
        }
        finally
        {
            connectionPool.releaseConnection(connection);
        }

        logger.debug("Load the Monitor Table data "+resultList);

        return promise.future();
    }


    private Future<List<Map<String,Object>>> loadDiscoveryData()
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

                if(selectResult!=null)
                {
                    for(int index=0;index<selectResult.size();index++)
                    {
                        String ip = selectResult.get(index).get("IPADDRESS").toString();

                        result.add(ip);
                    }

                    logger.debug("Fetched Data for Availiblity Polling "+result);

                    promise.complete(result);
                }
                else
                {
                    promise.fail("fetchDataForAvailabilityPolling method promise failed due to null data");
                }
            }
            else
            {
                promise.fail("fetchDataForAvailabilityPolling method promise failed due to connection closed");
            }
        }
        catch (Exception exception)
        {
            promise.fail("Some error in fetching data for availibality polling");

            logger.error(exception.getCause().getMessage());
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

                int insertedRows = operations.insert("DISCOVERY_TABLE",data);

                if(insertedRows>0)
                {
                    promise.complete(true);
                }
                else
                {
                    promise.complete(false);
                }
            }
            else
            {
                promise.complete(false);

                promise.fail("Add device Promise failed");
            }
        }
        catch (Exception exception)
        {
            promise.complete(false);

            logger.error(exception.getCause().getMessage());
        }
        finally
        {
            connectionPool.releaseConnection(connection);
        }
        return promise.future();
    }

    private Future<Boolean> updateDiscovery(String deviceID,boolean answer)
    {
        Promise<Boolean> promise = Promise.promise();

        Connection connection = connectionPool.getConnection();

        try
        {
            if(!(connection.isClosed()))
            {
                Operations operations = new Operations(connection);

                HashMap<String, Object> data = new HashMap<>();

                data.put("PROVISION",answer);

                String whereClause = "DEVICEID = " + Integer.valueOf(deviceID);

                operations.update("DISCOVERY_TABLE",data,whereClause);

                promise.complete(true);
            }
            else
            {
                promise.fail("updateDiscovery method promise failed due to connection closed");
            }

        }
        catch (Exception exception)
        {
            promise.complete(false);

            logger.error(exception.getCause().getMessage());
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

                int updateCount = operations.update("DISCOVERY_TABLE",data,whereClause);

                if(updateCount>0)
                {
                    promise.complete(true);
                }
                else
                {
                    promise.fail("EditDevice method promise failed due to updateCount<0");
                }
            }
            else
            {
                promise.fail("EditDevice method promise failed due to connection closed");
            }
        }
        catch (Exception exception)
        {
            promise.complete(false);

            logger.error(exception.getCause().getMessage());
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
            else
            {
                promise.fail("provisionedDeviceDataDump method promise failed due to connection closed");
            }
        }
        catch (Exception exception)
        {
            promise.complete(false);

            logger.error(exception.getCause().getMessage());
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

        String[] insertData = new String[]{"cpu.idle.percentage","cpu.system.percentage","cpu.user.percentage","disk.used.percentage","memory.free.percentage","memory.used.percentage","operating.system.name","operating.system.version","system.name","uptime"};

        try(PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO POLLING_TABLE VALUES(?,?,?,?,?)"))
        {
            if(!(connection.isClosed()))
            {
                ObjectMapper mapper = new ObjectMapper();

                JsonNode jsonArray = mapper.readTree(String.valueOf(data));

                for (JsonNode jsonObject : jsonArray)
                {
                   if(jsonObject!=null)
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
                       preparedStatement.executeBatch();
                   }
                }

                promise.complete(true);
            }
            else
            {
                promise.fail("sshPollingDataDump method promise failed due to connection closed");
            }
        }
        catch (Exception exception)
        {
            promise.complete(false);

            logger.error(exception.getCause().getMessage());
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
            else
            {
                promise.fail("fpingPollingDataDump method promise failed due to connection closed");
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