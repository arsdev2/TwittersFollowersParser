package me.senjka.twitterfollowersparser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainClass {

    private static ArrayList<String> cookiesList = new ArrayList<>();
    private static int cookiesIndex = 0;
    private static String CHANNEL_NAME = "bitnami";

    public static void main(String[] args) throws Exception {
        init();
        clearFile();
		CHANNEL_NAME = readFromFile("channel_name.txt");
        long from = new Date().getTime();
        String response =  get("https://twitter.com/" + CHANNEL_NAME + "/followers");
        ArrayList<String> list = getUsernames(response);
        ListIterator<String> iterator = list.listIterator();
        int count = 0;
        while (iterator.hasNext() && count < 3){
           iterator.next();
            iterator.remove();
            count++;
        }
        writeToFile(list);
        String position = response.split("data-min-position=\"")[1].split("\"")[0];
		incrementIndex();
        writeToFile(position);
        Thread.sleep(10000);
        parse(readFromFile("last_id"));
        long timeElapsed = new Date().getTime() - from;
        System.out.println((long) (timeElapsed / 1000));
    }

    private static void incrementIndex() {
        cookiesIndex++;
        cookiesIndex %= cookiesList.size();
    }

    private static void init() throws Exception {
        for(int i = 1;i<2;i++) {
            cookiesList.add(readFromFile("cookie" + i + ".txt"));
        }
    }

    private static void clearFile() throws Exception{
        FileWriter writer = new FileWriter("result.txt", false);
        writer.write("");
        writer.close();
    }

    private static void writeToFile(ArrayList<String> list) throws Exception{
        writeToFile("result.txt", list, true);
    }

    private static void writeToFile(String text) throws Exception{
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(text);
        writeToFile("last_id", arrayList, false);
    }

    private static void writeToFile(String filename, ArrayList<String> list , boolean append) throws Exception{
        FileWriter writer = new FileWriter(filename, append);
        for(String el : list){
            writer.write(el);
        }
        writer.close();
    }

    private static String readFromFile(String filename) throws Exception{
        return Files.readAllLines(Paths.get(new File(filename).getPath())).get(0);
    }

    private static void parse(String position) throws Exception{
        String responseString = get("https://twitter.com/" + CHANNEL_NAME + "/followers/users?include_available_features=1&include_entities=1&max_position=" + position + "&reset_error_state=false");
        JsonObject result = new JsonParser().parse(responseString).getAsJsonObject();
        System.out.println(result.toString());
        String items_html = result.get("items_html").getAsString();
        writeToFile(getUsernames(items_html));
        String newPosition = result.get("min_position").getAsString();
        writeToFile(newPosition);
        incrementIndex();
        if(result.get("has_more_items").getAsBoolean()){
            Thread.sleep(10 * 1000);
            parse(newPosition);
        }
    }

    private static ArrayList<String> getUsernames(String html) {
        Pattern pattern = Pattern.compile("@<b>.*</b>");
        Matcher matcher = pattern.matcher(html);
        ArrayList<String> list = new ArrayList<String>();
        while (matcher.find()) {
            int start=matcher.start();
            int end=matcher.end();
            String res = html.substring(start,end).replace("<b>", "").replace("</b>", "").split("</span>")[0] + "\n";
            if(!list.contains(res)){
                list.add(res);
            }
        }
        return list;
    }

    private static String get(String url) throws Exception{
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet getRequest = new HttpGet(url);
        getRequest.addHeader("accept-language", "en-US,en;q=0.9,uk-UA;q=0.8,uk;q=0.7,ru-RU;q=0.6,ru;q=0.5");
        getRequest.addHeader("accept-encoding", "gzip, deflate, br");
        getRequest.addHeader("accept", "application/json, text/javascript, */*; q=0.01");
        getRequest.addHeader("referer", "https://twitter.com/" + CHANNEL_NAME + "/followers");
        getRequest.addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        getRequest.addHeader("x-requested-with", "XMLHttpRequest");
        getRequest.addHeader("x-twitter-active-user", "yes");
        getRequest.addHeader("cookie", cookiesList.get(cookiesIndex));
        HttpResponse response = client.execute(getRequest);
        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity, "UTF-8");
    }

}
