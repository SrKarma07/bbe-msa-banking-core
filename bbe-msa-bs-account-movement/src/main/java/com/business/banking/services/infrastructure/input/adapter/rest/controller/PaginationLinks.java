package com.business.banking.services.infrastructure.input.adapter.rest.controller;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

public final class PaginationLinks {

    private PaginationLinks() {
    }

    public static String build(ServerWebExchange exchange, int page, int size, long total) {
        long lastPage = total == 0 ? 0 : Math.max((total - 1) / size, 0);
        StringBuilder sb = new StringBuilder();

        if (page > 0) {
            append(sb, link(exchange, page - 1, size), "prev");
        }
        if (page < lastPage) {
            append(sb, link(exchange, page + 1, size), "next");
        }
        return sb.toString();
    }

    private static void append(StringBuilder sb, String url, String rel) {
        if (sb.length() > 0) sb.append(", ");
        sb.append("<").append(url).append(">").append("; rel=\"").append(rel).append("\"");
    }

    private static String link(ServerWebExchange exchange, int page, int size) {
        var uri = exchange.getRequest().getURI();
        return UriComponentsBuilder.fromUri(uri)
                .replaceQueryParam("page", page)
                .replaceQueryParam("size", size)
                .build(true)
                .toUriString();
    }
}
