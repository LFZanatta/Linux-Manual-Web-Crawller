package com.zanatta.backend.backend;

import java.util.ArrayList;
import java.util.List;

public class CrawlerDTO {

    private final String id;

    private StatusCrawlerEnum status = StatusCrawlerEnum.active;

    private List<String> urls = new ArrayList<>();

    public CrawlerDTO(String id) {
        this.id = id;
    }

    public void addUrl(String url) {
        this.urls.add(url);
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setStatus(StatusCrawlerEnum status) {
        this.status = status;
    }

}