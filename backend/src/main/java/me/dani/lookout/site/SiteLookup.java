package me.dani.lookout.site;

import java.util.Optional;

public interface SiteLookup {

    Optional<SiteSnapshot> findByPublicKey(String publicKey);
}
