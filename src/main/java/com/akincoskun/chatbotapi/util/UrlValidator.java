package com.akincoskun.chatbotapi.util;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * URL doğrulama ve SSRF (Server-Side Request Forgery) koruması.
 * Yerel ağ adreslerine ve özel IP aralıklarına istek yapmayı engeller.
 */
public final class UrlValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private UrlValidator() {}

    /**
     * URL'nin güvenli ve erişilebilir olduğunu doğrular.
     *
     * @param urlStr doğrulanacak URL
     * @throws IllegalArgumentException güvensiz veya geçersiz URL ise
     */
    public static void validateForFetch(String urlStr) {
        URI uri;
        try {
            uri = new URI(urlStr);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + urlStr);
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new IllegalArgumentException("URL scheme must be http or https");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL must have a valid host");
        }

        checkSsrf(host);
    }

    private static void checkSsrf(String host) {
        // Literal hostname check
        String lower = host.toLowerCase();
        if (lower.equals("localhost") || lower.endsWith(".local") || lower.endsWith(".internal")) {
            throw new IllegalArgumentException("Access to internal hosts is not allowed");
        }

        // DNS resolve check
        try {
            InetAddress address = InetAddress.getByName(host);
            if (isPrivateOrReserved(address)) {
                throw new IllegalArgumentException("Access to private/reserved IP addresses is not allowed");
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Cannot resolve host: " + host);
        }
    }

    private static boolean isPrivateOrReserved(InetAddress address) {
        return address.isLoopbackAddress()
                || address.isSiteLocalAddress()
                || address.isLinkLocalAddress()
                || address.isAnyLocalAddress()
                || address.isMulticastAddress();
    }
}
