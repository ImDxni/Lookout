package me.dani.lookout.site.jpa;

import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface SiteRepository extends ListCrudRepository<Site, UUID> {
    Optional<Site> findByNameIgnoreCase(String name);

    Optional<Site> findByPublicKey(String publicKey);
}
