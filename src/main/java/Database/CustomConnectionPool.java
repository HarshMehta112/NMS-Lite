package Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;


public class CustomConnectionPool
{

    private static String URL ;

    private static String USER;

    private static String PASSWORD ;

    private static ArrayBlockingQueue< Connection > connectionPool;

    private static ArrayList< Connection > activeConnection = new ArrayList<>();

    private final int INITIALPOLLSIZE = 5;

    private final int MAXPOOLSIZE = 10;

    private static int poolSize = 0;

    private static CustomConnectionPool customConnectionPool;

    private CustomConnectionPool()
    {
        poolSize=INITIALPOLLSIZE;
    }

    private CustomConnectionPool(int size)
    {
        poolSize = size;
    }

    public static CustomConnectionPool getInstance()
    {
        if(customConnectionPool==null)
        {
            customConnectionPool = new CustomConnectionPool();
        }
        return customConnectionPool;
    }

    public static CustomConnectionPool getInstance(int size)
    {
        if(customConnectionPool==null)
        {
            customConnectionPool = new CustomConnectionPool(size);
        }
        return customConnectionPool;
    }


    public void setURL (String URL)
    {

        this.URL = URL;
    }

    public void setUser (String USER)
    {

        this.USER = USER;
    }

    public void setPassword (String password)
    {

        this.PASSWORD = PASSWORD;
    }


    public String getURL ()
    {

        return URL;
    }

    public String getUser ()
    {

        return USER;
    }

    public String getPassword ()
    {

        return PASSWORD;
    }

    public int getActiveConnections ()
    {

        return activeConnection.size();
    }

    public int getMaxConnections ()
    {

        return MAXPOOLSIZE;
    }

    public void createConnectionPool ()
    {

        if ( poolSize < MAXPOOLSIZE )
        {
            connectionPool = new ArrayBlockingQueue<>(poolSize);

            for ( int index = 0; index < poolSize; index++ )
            {
                try
                {
                    Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);

                    connectionPool.add(connection);
                }
                catch ( SQLException exception )
                {
                    exception.printStackTrace();
                }
            }
        }
        else
        {
            throw new RuntimeException("Please enter pool size appropriately");
        }
    }

    public Connection getConnection ()
    {

        Connection connection = connectionPool.remove();

        activeConnection.add(connection);

        return connection;
    }

    public void releaseConnection (Connection connection)
    {

        try
        {
            connectionPool.put(connection);

            activeConnection.remove(connection);

        }
        catch ( InterruptedException exception )
        {
            exception.printStackTrace();
        }

    }

    public void closeAllConnections()
    {
        int size = activeConnection.size();

        for(int index=0;index<size;index++)
        {
            try
            {
                activeConnection.get(index).close();
            }
            catch ( Exception exception )
            {
                exception.printStackTrace();
            }
        }
    }


}
