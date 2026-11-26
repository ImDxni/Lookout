package me.dani.lookout.site;

import lombok.RequiredArgsConstructor;
import me.dani.lookout.site.jpa.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
class SiteLookupService implements SiteLookup {
    private final SiteRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<SiteSnapshot> findByPublicKey(String publicKey) {
        if (publicKey == null || publicKey.isBlank()) {
            return Optional.empty();
        }
        return repository.findByPublicKey(publicKey)
                .map(site -> new SiteSnapshot(site.getId(), site.getAllowedOrigins(), site.getSamplingRate()));
    }
}
