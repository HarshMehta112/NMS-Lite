package Utils;

public class UserConfig
{
    public static final int MAX_CONNECTION_POOL_SIZE = 10;

    public static final int MIN_CONNECTION_POOL_SIZE = 6;

    public static final int APPLICATION_THREAD_COUNT = Runtime.getRuntime().availableProcessors()*2;

    public static final int AVAILIBILITY_POLLING_TIME = 2*60*100;

    public static final int SSH_POLLING_TIME = 10*20*100;

    public static final int SCHEDULER_DELAY = 10*600;

    public static final int DASHBOARD_REFRESH_DELAY = 5000;
}
