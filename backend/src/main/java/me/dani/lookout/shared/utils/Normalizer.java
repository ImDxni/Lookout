package me.dani.lookout.shared.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

public final class Normalizer {

    private static final Set<String> ORIGIN_SCHEMES = Set.of("http", "https");

    private Normalizer() {}

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeOrigin(String origin) {
        URI uri = parse(origin.trim());

        String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
        if (scheme == null || !ORIGIN_SCHEMES.contains(scheme)) {
            throw new InvalidOriginException(origin, "scheme must be http or https");
        }
        if (uri.getHost() == null || uri.getRawUserInfo() != null) {
            throw new InvalidOriginException(origin, "host is missing or contains credentials");
        }
        String path = uri.getRawPath();
        if ((path != null && !path.isEmpty() && !path.equals("/")) || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new InvalidOriginException(origin, "an origin cannot contain a path, query or fragment");
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);
        int port = uri.getPort();
        boolean defaultPort = port == -1
                || (scheme.equals("http") && port == 80)
                || (scheme.equals("https") && port == 443);

        return scheme + "://" + host + (defaultPort ? "" : ":" + port);
    }

    private static URI parse(String origin) {
        try {
            return new URI(origin);
        } catch (URISyntaxException e) {
            throw new InvalidOriginException(origin, "not a valid URI");
        }
    }
}
