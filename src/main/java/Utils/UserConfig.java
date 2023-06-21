package Utils;

public class UserConfig
{
    public static final int MAX_CONNECTION_POOL_SIZE = 10;

    public static final int MIN_CONNECTION_POOL_SIZE = 6;

    public static final int APPLICATION_THREAD_COUNT = Runtime.getRuntime().availableProcessors()*2;

    public static final int AVAILIBILITY_POLLING_TIME =2*60*1000;

    public static final int SSH_POLLING_TIME = 5*60*1000;

    public static final int SCHEDULER_DELAY = 1000*60;

    public static final int DASHBOARD_REFRESH_DELAY = 2*60*1000;

    public static final int HTTP_PORT = 8080;

    public static final int SESSION_TIMEOUT = 20*60*1000;

}
