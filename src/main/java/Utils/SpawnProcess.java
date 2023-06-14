package Utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonArray;
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
    public static HashMap<String, String > fpingForAvailibility(ArrayList<String> list) {

        ArrayList<String> command = new ArrayList<>();

        command.add("fping");

        command.add("-c3");

        command.add("-q");

        command.add("-t1000");

        command.addAll(list);

        System.out.println(command);

        ProcessBuilder processBuilder = new ProcessBuilder();

        processBuilder.command(command);

        processBuilder.redirectErrorStream(true);

        Process process = null;

        try
        {
            process = processBuilder.start();
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }

        InputStream processInputStream = process.getInputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(processInputStream));

        String line = null;

        HashMap<String,String> fpingResult = new HashMap<>();

        int packetLoss=0;

        int avgLatency=0;

        while (true)
        {
            try
            {
                if (!((line = reader.readLine()) != null))
                    break;

                packetLoss = Integer.parseInt(((line.split(":"))[1]).split("=")[1].split(",")[0].split("/")[2].split("%")[0]);

                avgLatency = Integer.parseInt(((line.split(":"))[1]).split("=")[2].split("/")[2]);

                if(avgLatency==0 && packetLoss>50)
                {
                    fpingResult.put(((line.split(":"))[0]).trim(),"Down");
                }
                else
                {
                    fpingResult.put(((line.split(":"))[0]).trim(),"Up");
                }

                process.waitFor(60,TimeUnit.SECONDS);

            }
            catch (Exception exception)
            {
                avgLatency=0;
            }

        }

        System.out.println("Result in spwan process "+fpingResult);


        return fpingResult;
    }

    //spwan process generic
    public static JsonNode spwanProcess(JsonArray credential) {

        String encoder = (Base64.getEncoder().encodeToString((credential).toString().getBytes(StandardCharsets.UTF_8)));
        System.out.println(encoder);
        BufferedReader reader = null;

        Process process = null;

        JsonArray resultJsonarray = new JsonArray();

        JsonNode array = null;
        try {
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

            System.out.println(resultJsonarray);

        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }

        return array;
    }

}