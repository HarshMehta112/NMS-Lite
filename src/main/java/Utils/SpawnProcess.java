package Utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.IOException;
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

    public static HashMap<String, String> fpingForAvailibility(ArrayList<String> list)
    {
        HashMap<String, String> fpingResult = new HashMap<>();

        ArrayList<String> command = new ArrayList<>();

        command.add("fping");

        command.add("-c3");

        command.add("-q");

        command.add("-t1000");

        command.addAll(list);

        logger.info("Fping command for checking availability of device " + command);

        try
        {
            ProcessBuilder processBuilder = new ProcessBuilder();

            processBuilder.command(command);

            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            InputStream processInputStream = process.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(processInputStream));

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
            exception.printStackTrace();
        }

        logger.info("Result of fping Polling "+fpingResult);

        return fpingResult;
    }

    //make spawn process generic
    public static JsonNode spwanProcess(JsonArray credential) {

        String encoder = (Base64.getEncoder().encodeToString((credential).toString().getBytes(StandardCharsets.UTF_8)));

        BufferedReader reader;

        Process process;

        JsonArray resultJsonarray = new JsonArray();

        JsonNode array = null;

        try
        {
            ProcessBuilder builder = new ProcessBuilder(Constants.PLUGIN_PATH, encoder);

            process = builder.start();

            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;

            ObjectMapper mapper = new ObjectMapper();

            while ((line = reader.readLine()) != null)
            {
                array = mapper.readTree(line);
            }

            process.waitFor(60,TimeUnit.SECONDS);

            logger.info("Output from golang exe Plugin "+resultJsonarray);

        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }

        return array;
    }

}