package com.zanatta.backend.backend;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Response;
import spark.Spark;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static final String BASE_URL = System.getenv("BASE_URL");
    public static LinkedHashMap<String, CrawlerDTO> listCrawlersRunning = new LinkedHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Spark.get("/crawl/:id", (req, res) -> {
            String newId = req.params(":id");
            String resultCrawler = getRequestDetailsById(newId);

            if (resultCrawler.isEmpty()) {
                JsonObject messageNotFound = new JsonObject();
                messageNotFound.addProperty("status", 404);
                messageNotFound.addProperty("message", String.format("crawl not found: %s",newId));
                responseMessageHandler(res, 400, messageNotFound.toString());
                return res.body();
            }
            responseMessageHandler(res, 200, resultCrawler);
            return res.body();
        });

        Spark.post("/crawl", (req, res) -> {
            String crawlId = generateNewSeachId();
            JsonObject jsonObject = new Gson().fromJson(req.body(), JsonObject.class);
            String keyword = jsonObject.get("keyword").getAsString();

            if (checkKeywordEmpty(keyword) || checkKeywordSize(keyword)) {
                JsonObject messageCharacter = new JsonObject();
                messageCharacter.addProperty("status", 400);
                messageCharacter.addProperty("message", "field 'keyword' is required (from 4 up to 32 chars)");
                responseMessageHandler(res, 400, messageCharacter.toString());
                return res.body();
            }
            JsonObject messageCrawlId = new JsonObject();
            messageCrawlId.addProperty("id", crawlId);
            responseMessageHandler(res, 200, messageCrawlId.toString());

            CompletableFuture.runAsync(() -> {
                try {
                    Set<String> visitedURLs = new HashSet<>();
                    ExecutorService executorService = Executors.newFixedThreadPool(15);
                    listCrawlersRunning.put(crawlId, new CrawlerDTO(crawlId));
                    initializeSearchCrawler(executorService, listCrawlersRunning.get(crawlId), visitedURLs,
                            BASE_URL, keyword);
                } catch (InterruptedException e) {
                    logger.error("Error initializing search crawler data", e);
                }
            });
            return messageCrawlId;
        });
    }

    private static void initializeSearchCrawler(ExecutorService executorService, CrawlerDTO valueCrawler,
                                                Set<String> visitedURLs, String rootURL, String keyword) throws InterruptedException {
        if (valueCrawler.getUrls().size() >= 100) {
            valueCrawler.setStatus(StatusCrawlerEnum.done);
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                logger.error("There was an error when trying to end the thread.", e);
                executorService.shutdownNow();
            }
            return;
        }
        visitedURLs.add(rootURL);

        executorService.execute( () -> {
            BufferedReader reader = null;
            InputStream inputStream = null;
            try {
                URL url = new URL(rootURL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    inputStream = connection.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    String inputLine;
                    Pattern urlPattern = Pattern.compile("href=\"(.*?)\"");
                    while ((inputLine = reader.readLine()) != null) {
                        if (!valueCrawler.getUrls().contains(rootURL)) {
                            Pattern keywordPattern = Pattern.compile("\\b"+ keyword +"\\b",
                                    Pattern.CASE_INSENSITIVE);
                            Matcher keywordMatcher = keywordPattern.matcher(inputLine);
                            while (keywordMatcher.find()) {
                                valueCrawler.addUrl(rootURL);
                            }
                        }
                        Matcher urlMatcher = urlPattern.matcher(inputLine);
                        while (urlMatcher.find()) {
                            if (urlMatcher.group(1).contains(".html")) {
                                String hasUrl = BASE_URL + urlMatcher.group(1);
                                if (!visitedURLs.contains(hasUrl)) {
                                    initializeSearchCrawler(executorService, valueCrawler, visitedURLs,
                                            hasUrl, keyword);
                                }
                            }
                        }
                    }
                }
                connection.disconnect();
            } catch (Exception e) {
                logger.error("Error while search for keyword", e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        logger.error("Was not possible finishing reader", e);
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        logger.error("Was not possible finishing input stream", e);
                    }
                }
            }
        });
    }

    private static String getRequestDetailsById(String id) {
        CrawlerDTO crawlerDTO = listCrawlersRunning.get(id);
        if (crawlerDTO != null) {
            return new Gson().toJson(crawlerDTO);
        }
        return "";
    }

    private static void responseMessageHandler(Response res, int statusCode, String message) {
        res.type("application/json");
        res.body(message);
        res.status(statusCode);
    }

    private static boolean checkKeywordSize(String keyword) {
        return (keyword.length() < 4 || keyword.length() > 32);
    }

    private static boolean checkKeywordEmpty(String keyword) {
        return (keyword == null || keyword.trim().isEmpty());
    }

    private static String generateNewSeachId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
