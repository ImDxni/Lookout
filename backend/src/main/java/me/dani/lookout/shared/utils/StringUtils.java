package me.dani.lookout.shared.utils;

import java.util.Locale;

public class StringUtils {

    public static String normalizeEmail(String email){
        return email.toLowerCase(Locale.ROOT).trim();
    }
}
