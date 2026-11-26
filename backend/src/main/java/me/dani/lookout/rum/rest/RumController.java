package me.dani.lookout.rum.rest;

import lombok.RequiredArgsConstructor;
import me.dani.lookout.rum.RumIngestService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rum")
@RequiredArgsConstructor
public class RumController {
    private final RumIngestService service;

    @PostMapping(path = "/events", consumes = {MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Void> ingest(@RequestBody(required = false) String body,
                                       @RequestHeader(value = HttpHeaders.ORIGIN, required = false) String origin) {
        service.ingest(body, origin);
        return ResponseEntity.accepted().build();
    }
}
