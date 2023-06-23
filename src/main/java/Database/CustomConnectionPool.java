package Database;

import Utils.UserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.ArrayBlockingQueue;


public class CustomConnectionPool
{

    private static final Logger logger = LoggerFactory.getLogger(CustomConnectionPool.class);

    private static String URL ;

    private static String USER;

    private static String PASSWORD ;

    private static ArrayBlockingQueue< Connection > connectionPool;

    private final int INITIALPOLLSIZE = UserConfig.MIN_CONNECTION_POOL_SIZE;

    private final int MAXPOOLSIZE = UserConfig.MAX_CONNECTION_POOL_SIZE;

    private static int poolSize = 0;

    private static CustomConnectionPool customConnectionPool;

    private CustomConnectionPool()
    {
        poolSize=INITIALPOLLSIZE;
    }

    public static CustomConnectionPool getInstance()
    {
        if(customConnectionPool==null)
        {
            customConnectionPool = new CustomConnectionPool();
        }
        return customConnectionPool;
    }


    public void setURL (String URL)
    {
        CustomConnectionPool.URL = URL;
    }

    public void setUser (String USER)
    {
        CustomConnectionPool.USER = USER;
    }

    public void setPassword (String password)
    {
        CustomConnectionPool.PASSWORD = password;
    }

    public boolean createConnectionPool ()
    {

        if ( poolSize < MAXPOOLSIZE )
        {
            connectionPool = new ArrayBlockingQueue<>(poolSize,true);

            for ( int index = 0; index < poolSize; index++ )
            {
                try
                {
                    Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);

                    connectionPool.add(connection);
                }
                catch ( Exception exception )
                {
                    logger.error(exception.getCause().getMessage());

                    return false;
                }
            }
        }
        return true;
    }

    public Connection getConnection ()
    {

        Connection connection = connectionPool.remove();

        return connection;
    }

    public void releaseConnection (Connection connection)
    {
        try
        {
            connectionPool.put(connection);
        }
        catch ( InterruptedException exception )
        {
            logger.error(exception.getCause().getMessage());
        }

    }

    public void closeAllConnections()
    {
        int size = connectionPool.size();

        for(int index=0;index<size;index++)
        {
            try
            {
                connectionPool.take().close();
            }
            catch ( Exception exception )
            {
                logger.error(exception.getCause().getMessage());
            }
        }
    }


}
