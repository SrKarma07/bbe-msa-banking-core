package com.business.banking.services.infrastructure.input.adapter.rest.controller;

import com.business.banking.services.application.shared.PageResult;

import java.net.URI;

public final class PaginationLinks {

    private final String header;

    private PaginationLinks(String header) {
        this.header = header;
    }

    public String asHeader() {
        return header;
    }

    public static PaginationLinks from(URI baseUri, PageResult<?> pageResult) {
        int page = pageResult.page();
        int size = pageResult.size();
        int totalPages = pageResult.totalPages();

        StringBuilder links = new StringBuilder();

        if (page > 0) {
            links.append(buildLink(baseUri, page - 1, size, "prev"));
        }

        if (page + 1 < totalPages) {
            if (!links.isEmpty()) links.append(", ");
            links.append(buildLink(baseUri, page + 1, size, "next"));
        }

        return new PaginationLinks(links.toString());
    }

    private static String buildLink(URI baseUri, int page, int size, String rel) {
        String uri = baseUri.toString();

        uri = uri.replaceAll("([&?])page=\\d+", "$1page=" + page);
        uri = uri.replaceAll("([&?])size=\\d+", "$1size=" + size);

        if (!uri.contains("page=")) {
            uri = appendQuery(uri, "page=" + page);
        }
        if (!uri.contains("size=")) {
            uri = appendQuery(uri, "size=" + size);
        }

        return "<" + uri + ">; rel=\"" + rel + "\"";
    }

    private static String appendQuery(String uri, String queryPart) {
        return uri.contains("?") ? (uri + "&" + queryPart) : (uri + "?" + queryPart);
    }
}
