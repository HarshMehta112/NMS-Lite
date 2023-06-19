package Database;
import Utils.Constants;
import Verticle.DatabaseVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


public class PropertiesFile
{

    private static final Logger logger = LoggerFactory.getLogger(PropertiesFile.class);

    static
    {

        Properties properties = new Properties();

        FileInputStream fileInputStream = null;

       try
       {
           fileInputStream  = new FileInputStream(Constants.DATABASE_PROPERTIES_PATH);

           properties.load(fileInputStream);

       }
       catch (Exception exception)
       {
           logger.error(exception.getCause().getMessage());
       }
       finally
       {
           try
           {
               if(fileInputStream!=null)
               {
                   fileInputStream.close();
               }
           }
           catch (IOException exception)
           {
               logger.error(exception.getCause().getMessage());
           }
       }

        URL = properties.getProperty("URL");

        USER = properties.getProperty("USER");

        PASSWORD = properties.getProperty("PASSWORD");
    }

    public static String URL;

    public static String USER;

    public static String PASSWORD;

    public static String getURL()
    {
        return URL;
    }

    public static String getUSER()
    {
        return USER;
    }


    public static String getPASSWORD()
    {
        return PASSWORD;
    }

}
