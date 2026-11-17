package me.dani.lookout.site.exceptions;

public class DuplicateSiteException extends RuntimeException {
    public DuplicateSiteException(String name) {
        super("Site with name " + name + " already exists!");
    }
}
