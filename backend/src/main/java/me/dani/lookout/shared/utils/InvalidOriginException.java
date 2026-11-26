package me.dani.lookout.shared.utils;

public class InvalidOriginException extends IllegalArgumentException {
    public InvalidOriginException(String origin, String reason) {
        super("Invalid origin '" + origin + "': " + reason);
    }
}
