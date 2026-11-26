package me.dani.lookout.site.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.dani.lookout.site.SiteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sites")
@RequiredArgsConstructor
public class SiteController {
    private final SiteService service;


    @PostMapping()
    public ResponseEntity<SiteResponse> createSite(@Valid @RequestBody CreateSiteRequest req){
        SiteResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/sites/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public SiteResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public SiteResponse update(@PathVariable UUID id,@Valid @RequestBody UpdateSiteRequest req){

        return service.update(id,req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @GetMapping
    public List<SiteResponse> list() { return service.list(); }

}
