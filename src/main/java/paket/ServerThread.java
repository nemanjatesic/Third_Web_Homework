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
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            String komanda = in.readLine();
            String response = "";

            if (komanda == null) return;

            String path = komanda.split(" ")[1];
            String method = komanda.split(" ")[0];

            if (!method.equals("GET")){
                response = napraviError(405);
            }
            if (path.equals("/") && method.equals("GET")) {
                response = homeRoute();
            }else if (path.equals("/qod") && method.equals("GET")) {
                response = qodRoute();
            }else if (method.equals("GET")){
                response = napraviError(404);
            }
            //treba odgovoriti browser-u po http protokolu:
            out.println(response);

            in.close();
            out.close();
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String homeRoute() {
        return napraviRedirect("/qod");
    }

    public String qodRoute() throws Exception{
        String response = "";
        HttpResponse<String> responseQuote = sendGet();

        if (responseQuote.statusCode() != 200) {
            ObjectMapper mapper = new ObjectMapper();
            Map jsonMap = mapper.readValue(responseQuote.body(), Map.class);

            Map quoteObj = ((Map)jsonMap.get("error"));
            response = napraviOdogovor(responseQuote.statusCode(),quoteObj.get("message").toString());
        }else {
            ObjectMapper mapper = new ObjectMapper();
            Map jsonMap = mapper.readValue(responseQuote.body(), Map.class);

            List quoteObj = (List)((Map)jsonMap.get("contents")).get("quotes");

            response = napraviOdogovor(responseQuote.statusCode(), ((Map)quoteObj.get(0)).get("quote").toString());
        }
        return response;
    }

    private String napraviRedirect(String redirectTo) {
        String retVal = "HTTP/1.1 301 ERROR\r\nLocation: " + redirectTo + "\r\n\r\n";

        return retVal;
    }

    private String napraviOdogovor(int code, String quote) {
        String retVal = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n";

        retVal += "<html><head><title>Odgovor servera</title></head>\n";
        retVal += "<body><h1>Status code : " + code + "</h1>\n";
        retVal += "<h1>Message : " + quote + "</h1>\n";
        retVal += "</body></html>";

        return retVal;
    }

    private String napraviError(int code) {
        String errorMsg;
        if (code == 404) errorMsg = "Not Found";
        else if (code == 405) errorMsg = "Method Not Allowed";
        else errorMsg = "Error";

        String retVal = "HTTP/1.1 " + code + " " + errorMsg + "\r\nContent-Type: text/html\r\n\r\n";

        retVal += "<html><head><title>Odgovor servera</title></head>\n";
        retVal += "<body><h1>Error code : " + code + "</h1>\n";
        retVal += "</body></html>";

        return retVal;
    }

    private HttpResponse<String> sendGet() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://quotes.rest/qod"))
                .setHeader("Accept","application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

}
