package Utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class SpawnProcess
{
    private static final Logger logger = LoggerFactory.getLogger(SpawnProcess.class);

    static ObjectMapper mapper = new ObjectMapper();

    public static HashMap<String, String> fpingForAvailibility(ArrayList<String> list)
    {
        HashMap<String, String> fpingResult = new HashMap<>();

        ArrayList<String> command = new ArrayList<>();

        command.add("fping");

        command.add("-c3");

        command.add("-q");

        command.add("-t1000");

        command.addAll(list);

        BufferedReader reader = null;

        Process process = null;

        logger.debug("Fping command for checking availability of device " + command);

        try
        {
            ProcessBuilder processBuilder = new ProcessBuilder();

            processBuilder.command(command);

            processBuilder.redirectErrorStream(true);

            process = processBuilder.start();

            InputStream processInputStream = process.getInputStream();

            reader = new BufferedReader(new InputStreamReader(processInputStream));

            String line ;

            while (true)
            {
                if (((line = reader.readLine()) == null))
                    break;

                if (line.contains("min/avg/max"))
                {
                    fpingResult.put(((line.split(":"))[0]).trim(), "Up");
                }
                else
                {
                    fpingResult.put(((line.split(":"))[0]).trim(), "Down");
                }
                process.waitFor(60, TimeUnit.SECONDS);
            }
        }
        catch (Exception exception)
        {
            logger.error(exception.getCause().getMessage());
        }
        finally
        {
           try
           {
               reader.close();

               process.destroy();
           }
           catch (Exception exception)
           {
               logger.error(exception.getCause().getMessage());
           }
        }

        logger.debug("Result of fping Polling "+fpingResult);

        return fpingResult;
    }

    public static JsonNode spwanProcess(JsonArray credential) {

        String encoder = (Base64.getEncoder().encodeToString((credential).toString().getBytes(StandardCharsets.UTF_8)));

        logger.debug("Encoded String "+ encoder);

        BufferedReader reader = null;

        Process process = null;

        JsonArray resultJsonarray = new JsonArray();

        JsonNode array = null;

        try
        {
            ProcessBuilder builder = new ProcessBuilder(Constants.PLUGIN_PATH, encoder);

            process = builder.start();

            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;

            while ((line = reader.readLine()) != null)
            {
                JsonArray jsonArray = new JsonArray(line);

                resultJsonarray.addAll(jsonArray);
            }
            array = mapper.readTree(resultJsonarray.toString());

            process.waitFor(60,TimeUnit.SECONDS);

            logger.debug("Output from golang exe Plugin "+resultJsonarray);

        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
        finally
        {
           try
           {
               if(reader!=null)
               {
                   reader.close();
               }

               if(process!=null)
               {
                   process.destroy();
               }
           }
           catch (Exception exception)
           {
               logger.error(exception.getCause().getMessage());
           }

        }

        return array;
    }

}