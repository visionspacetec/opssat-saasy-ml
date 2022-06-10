package com.tanagraspace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;

import org.json.JSONException;
import org.json.JSONObject;
/**
 * Hello world!
 */
public final class App {
    private App() {
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }
    
    public static void readFromUrl(String url) throws Exception {
        InputStream is = new URL(url).openStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        String jsonText = readAll(rd);
        System.out.println(jsonText);
    }

    /**
     * Says hello to the world.
     * @param args The arguments of the program.
     */
    public static void main(String[] args) {

        // set ML server host
        String host;
        try {
            host = System.getProperty("host", "localhost");
        } catch (NumberFormatException e) {
            host = "localhost";
        }


        // set ML server port
        int port;
        try {
            port = Integer.parseInt(System.getProperty("port", "8888"));
        } catch (NumberFormatException e) {
            port = 8888;
        }

        String serverAddress = "http://" + host + ":" + port;
        
        try{

            System.out.println("Test API requests on " + serverAddress);

            System.out.println("\nTest #1:");
            readFromUrl(serverAddress + "/api/v1/training/classifier/bayesian/aode");

            System.out.println("\nTest #2:");
            readFromUrl(serverAddress + "/api/v1/training/classifier/boosting/bagging ");
            
            System.out.println("\nTest #3:");
            readFromUrl(serverAddress + "/api/v1/inference");

            System.out.println("\nTest completed.");

        }catch(Exception e){
            e.printStackTrace();
        }
        
    }
}
