package Utils;

public class Constants {
    public static final String PROPERTY_FILE_PATH = "/root/IdeaProjects/Motadata/src/main/resources/webroot/user.properties";

    public static final String SSL_KEYSTORE_PATH = "/home/harsh/JavaWork/Vert.xWeb/src/main/resources/server-keystore.jks";

    public static final String SSL_PASSWORD = "harshmehta";

    public static final String ADD_DISCOVERY_DEVICE = "AddDiscoveryDevice";

    public static final String GET_ALL_DISCOVERY_DEVICE = "GetAllDiscoveryTableData";

    public static final String DATABASE_PROPERTIES_PATH = "/home/harsh/Project/db.properties";

    public static final String DELETE_DISCOVERY_DEVICE = "DeleteDiscoveryDevice";

    public static final String EDIT_DISCOVERY_DEVICE = "EditDiscoveryDevice";

    public static final String PLUGIN_PATH = "/root/IdeaProjects/Motadata/src/main/Bootstrap";

    public static final String RUN_DISCOVERY = "DiscoveryRun";

    public static final String RUN_DISCOVERY_SPAWN_PEROCESS = "discoverySpawn";

    public static final int AVAILIBILITY_POLLING_TIME = 2*60*100;

    public static final int SSH_POLLING_TIME = 5*6000*100;

    public static final int SCHEDULER_DELAY = 10*60;

    public static final int DASHBOARD_REFRESH_DELAY = 5000;

    public static final String OUTPUT_SSH_POLLING = "SSHPollingRequest";

    public static final String SSH_POLLING_PROCESS_TRIGGERED = "SSHPollingProcessSpawn";

    public static final String AVAILABILITY_POLLING_PROCESS_TRIGGERED = "AvailabilityPollingProcessSpawn" ;

    public static final String OUTPUT_AVAILABILITY_POLLING = "AvailabilityPollingRequest";

    public static final String RUN_PROVISION = "AddDeviceToMonitorTable";

    public static final String LOAD_MONITOR_DEVICE = "loadMonitorDevices";

    public static final String DELETE_MONITOR_DEVICE = "deleteMonitorDevice";

    public static final String MONITOR_DEVICE_INFO = "deviceMonitorInfo";

    public static final String DASHBOARD_LOAD = "loadDashBoard";

    public static final String DASHBOARD_DATA_EVENT_BUS_BRIDGE = "updates.data";
}
