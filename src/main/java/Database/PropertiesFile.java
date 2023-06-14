package Database;
import Utils.Constants;
import java.io.FileInputStream;
import java.util.Properties;


public class PropertiesFile
{

    static
    {

        Properties properties = new Properties();

       try
       {
           FileInputStream fileInputStream = new FileInputStream(Constants.DATABASE_PROPERTIES_PATH);

           properties.load(fileInputStream);

       }
       catch (Exception exception)
       {
           exception.printStackTrace();
       }


        URL = properties.getProperty("URL");

        USER = properties.getProperty("USER");

        PASSWORD = properties.getProperty("PASSWORD");
    }

    public static String URL;

    public static String USER;

    public static String PASSWORD;

    public static String getURL() {
        return URL;
    }

    public static String getUSER() {
        return USER;
    }


    public static String getPASSWORD() {
        return PASSWORD;
    }

}
