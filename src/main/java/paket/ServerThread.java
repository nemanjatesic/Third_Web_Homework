package paket;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServerThread implements Runnable {

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

    public ServerThread(Socket sock) {
        this.client = sock;

        try {
            //inicijalizacija ulaznog toka
            in = new BufferedReader(
                    new InputStreamReader(
                            client.getInputStream()));

            //inicijalizacija izlaznog sistema
            out = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    client.getOutputStream())), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void run() {
        try {
            // uzimamo samo prvu liniju zahteva, da bismo izvukli parametar
            String komanda = in.readLine();
            System.out.println("Komanda je : " + komanda);
            String response = "";

            HttpResponse<String> responseQuote = sendGet();

            if (responseQuote.statusCode() != 200) {
                ObjectMapper mapper = new ObjectMapper();
                Map jsonMap = mapper.readValue(responseQuote.body(), Map.class);

                Map quoteObj = ((Map)jsonMap.get("error"));
                response = napraviOdogovor(komanda, responseQuote.statusCode(),quoteObj.get("message").toString());
            }else {
                ObjectMapper mapper = new ObjectMapper();
                Map jsonMap = mapper.readValue(responseQuote.body(), Map.class);

                List quoteObj = (List)((Map)jsonMap.get("contents")).get("quotes");

                System.out.println("CAO " + ((Map)quoteObj.get(0)).get("quote"));
                response = napraviOdogovor(komanda, responseQuote.statusCode(), ((Map)quoteObj.get(0)).get("quote").toString());
            }
            //treba odgovoriti browser-u po http protokolu:
            out.println(response);

            in.close();
            out.close();
            client.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String napraviOdogovor(String komanda, int code, String quote) {
        String retVal = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n";

        retVal += "<html><head><title>Odgovor servera</title></head>\n";
        retVal += "<body><h1>Status code : " + code + "</h1>\n";
        retVal += "<h1>Message : " + quote + "</h1>\n";
        retVal += "</body></html>";

        System.out.println("HTTP odgovor:");
        System.out.println(retVal);

        return retVal;
    }

    private HttpResponse<String> sendGet() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://quotes.rest/qod"))
                .setHeader("User-Agent", "Java 11 HttpClient Bot")
                .setHeader("Accept","json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        // print status code
        System.out.println(response.statusCode());
        // print response body
        System.out.println(response.body());

        return response;
    }

}
