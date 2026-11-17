package me.dani.lookout.site;

import lombok.RequiredArgsConstructor;
import me.dani.lookout.site.exceptions.DuplicateSiteException;
import me.dani.lookout.site.exceptions.SiteNotFoundException;
import me.dani.lookout.site.jpa.Site;
import me.dani.lookout.site.jpa.SiteRepository;
import me.dani.lookout.site.rest.CreateSiteRequest;
import me.dani.lookout.site.rest.SiteResponse;
import me.dani.lookout.site.rest.UpdateSiteRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SiteService {
    private final SiteRepository repository;
    private static final SecureRandom RANDOM = new SecureRandom();

    public SiteResponse create(CreateSiteRequest req){
        String name = req.name().strip();
        if(repository.findByNameIgnoreCase(name).isPresent()){
            throw new DuplicateSiteException(name);
        }
        Site site = new Site(name,req.allowedOrigins(),req.samplingRate(),generatePublicKey());
        return SiteResponse.from(repository.save(site));
    }

    public SiteResponse update(UUID id, UpdateSiteRequest req){
        Site site = repository.findById(id).orElseThrow(() -> new SiteNotFoundException(id));

        String name = req.name().strip();
        if(!site.getName().equalsIgnoreCase(name)){
            if(repository.findByNameIgnoreCase(name).isPresent())
                throw new DuplicateSiteException(name);
        }

        site.update(name,req.allowedOrigins(),req.samplingRate());

        return SiteResponse.from(site);
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) throw new SiteNotFoundException(id);
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public SiteResponse get(UUID id){
        return repository.findById(id)
                .map(SiteResponse::from)
                .orElseThrow(() -> new SiteNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<SiteResponse> list() {
        return repository.findAll().stream().map(SiteResponse::from).toList();
    }


    private String generatePublicKey() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return "pk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

}
