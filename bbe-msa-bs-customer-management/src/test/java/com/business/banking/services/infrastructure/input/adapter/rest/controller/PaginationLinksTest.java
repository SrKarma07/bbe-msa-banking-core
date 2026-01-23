package com.business.banking.services.infrastructure.input.adapter.rest.controller;

import com.business.banking.services.application.shared.PageResult;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class PaginationLinksTest {

    @Test
    void from_whenHasPrevAndNext_shouldBuildBothLinks_andReplaceExistingParams() {
        URI base = URI.create("http://localhost/api/customers?page=2&size=10");

        PageResult<Object> pr = new PageResult<>(
                null,
                2,
                10,
                100,
                5
        );

        String header = PaginationLinks.from(base, pr).asHeader();

        assertTrue(header.contains("rel=\"prev\""));
        assertTrue(header.contains("rel=\"next\""));
        assertTrue(header.contains("page=1"));
        assertTrue(header.contains("page=3"));
        assertTrue(header.contains("size=10"));
        assertTrue(header.contains(", "));
    }

    @Test
    void from_whenOnlyNext_shouldAppendParamsWithAmpersand() {
        URI base = URI.create("http://localhost/api/customers?state=true");

        PageResult<Object> pr = new PageResult<>(
                null,
                0,
                50,
                51,
                2
        );

        String header = PaginationLinks.from(base, pr).asHeader();

        assertFalse(header.contains("rel=\"prev\""));
        assertTrue(header.contains("rel=\"next\""));
        assertTrue(header.contains("state=true"));
        assertTrue(header.contains("page=1"));
        assertTrue(header.contains("size=50"));
        assertTrue(header.contains("?state=true&"));
    }

    @Test
    void from_whenNoPrevNoNext_shouldReturnEmptyHeader() {
        URI base = URI.create("http://localhost/api/customers");

        PageResult<Object> pr = new PageResult<>(
                null,
                0,
                20,
                0,
                1
        );

        String header = PaginationLinks.from(base, pr).asHeader();

        assertNotNull(header);
        assertTrue(header.isEmpty());
    }

    @Test
    void from_whenSizeAlreadyPresentButPageMissing_shouldAppendOnlyPage_andKeepSize() {
        URI base = URI.create("http://localhost/api/customers?size=20");

        PageResult<Object> pr = new PageResult<>(
                null,
                1,
                20,
                60,
                3
        );

        String header = PaginationLinks.from(base, pr).asHeader();

        assertTrue(header.contains("rel=\"prev\""));
        assertTrue(header.contains("rel=\"next\""));
        assertTrue(header.contains("size=20"));
        assertTrue(header.contains("page=0"));
        assertTrue(header.contains("page=2"));
        assertTrue(header.contains("?size=20&page=") || header.contains("?page="));
    }
}
