package me.dani.lookout.site.exceptions;

import java.util.UUID;

public class SiteNotFoundException extends RuntimeException {
    public SiteNotFoundException(UUID id) {
        super("Site " + id + " not found");
    }
}
