package me.dani.lookout.site.exceptions;


import jakarta.persistence.EntityNotFoundException;

import java.util.UUID;

public class SiteNotFoundException extends RuntimeException {
    public SiteNotFoundException(UUID id) { super("Site " + id + " not found"); }

}