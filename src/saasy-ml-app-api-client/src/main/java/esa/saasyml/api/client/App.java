package esa.saasyml.api.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server API client
 */
public final class App {
    
    private static Logger logger = LoggerFactory.getLogger(App.class);

    private App() {
    }
    
    public static void readFromUrl(String serverAddress, String payload) throws Exception {

        try {

            System.out.println("\n"+serverAddress);
            // payload = "{ \"expId\": 123, \"datasetId\": 1, \"iterations\": 10, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\"] }";
            System.out.println(payload);

            URL url = new URL(serverAddress);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
    
            OutputStream os = conn.getOutputStream();
            os.write(payload.getBytes());
            os.flush();
    
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode() + ".\nError message: " + conn.getResponseMessage());
            }
    
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File("em-train-data-17-08-2022.json")));
    
            String output;
            System.out.println("\nOutput from sever: \n");
            logger.info("Output from Server: \n");
            while ((output = br.readLine()) != null) {
                System.out.println(output);
                logger.info(output);
                writer.write(output);
            }

            br.close();
            writer.close();
    
            conn.disconnect();
    
        } catch (MalformedURLException e) {

            e.printStackTrace();

        } catch (IOException e) {

            e.printStackTrace();

        }
    }

    /**
     * @param args The arguments of the program.
     */
    public static void main(String[] args) {

        // how to use information
        String howToUse = "$ java -jar saasy-ml-app-api-client-0.1.0-SNAPSHOT-jar-with-dependencies.jar -server [URL] -payload [JSON] (optional -payloadFile [JSON_FILE])";

        // we stored all in a tuple <o, v>, where o is an option and v is a set or values (tests)
        final Map<String, String> params = new HashMap<>();

        // for each argument, we take the values
        for (int index = 0; index < args.length; index++) {

            final String arg = args[index];

            if (arg.charAt(0) == '-') {

                if (arg.length() < 2) {
                    System.err.println("Error at argument " + arg);
                    return;
                }

                params.put(arg.substring(1), args[++index]);

            }
        }

        String serverAddress = "";

        // if it is empty or it does not containt the server address
        if (params.isEmpty() || !params.containsKey("server")) {
            serverAddress = "http://localhost:9999/api/v1/training/data/subscribe/";
        } else {
            // get the server address
            serverAddress = params.get("server");
        }

        if (!params.containsKey("payload") && !params.containsKey("payloadFile")) {
            System.out.println("Payload can not be empty");
        } else {
            String payload = "";
            
            if (params.containsKey("payload")) {
                payload = params.get("payload");
            } else {
                try{
                    payload = readFile(params.get("payloadFile"));
                } catch (Exception e) {
                    System.out.println("Error reading the JSON");
                    payload = "Error";
                }
            }

            logger.info("************* ************************************ ****");
            logger.info("************* Test API requests **************");
            logger.info("- server address: "+serverAddress);
            logger.info("- payload: "+ payload);
            logger.info("************* ************************************ ****\n");
            
            try{
    
                logger.info("\nTest #1:");
                readFromUrl(serverAddress, payload);
                logger.info("\nTest completed.");
    
            } catch(Exception e){
                e.printStackTrace();
            }
        }
        
        System.out.println("\nHelp of use:\n" + howToUse + "\n");

    }

    private static String readFile(String fileName) throws IOException {

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            return sb.toString();
        }

    }
}
