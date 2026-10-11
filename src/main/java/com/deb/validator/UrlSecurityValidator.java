package com.deb.validator;

import com.deb.exception.InvalidDestinationUrlException;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * Validates destination URLs against malicious schemes, SSRF attacks,
 * local network probing, and cloud instance metadata theft.
 */
@Component
public class UrlSecurityValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final String AWS_METADATA_IP = "169.254.169.254";

    public void validateUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            throw new InvalidDestinationUrlException("Destination URL cannot be empty");
        }

        URI uri;
        try {
            uri = new URI(rawUrl.trim()).normalize();
        } catch (URISyntaxException e) {
            throw new InvalidDestinationUrlException("Malformed URL format: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new InvalidDestinationUrlException("Invalid URL scheme '" + scheme + "'. Only HTTP and HTTPS are permitted.");
        }

        String host = uri.getHost();
        if (host == null || host.trim().isEmpty()) {
            throw new InvalidDestinationUrlException("URL must contain a valid host name");
        }

        String lowerHost = host.toLowerCase().trim();

        // Reject localhost and metadata names
        if (lowerHost.equals("localhost") || lowerHost.endsWith(".localhost") || lowerHost.equals("metadata.google.internal")) {
            throw new InvalidDestinationUrlException("SSRF blocked: host points to loopback or metadata domain");
        }

        // Check resolved IP addresses for private, loopback, link-local ranges
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isBlockedIpAddress(address)) {
                    throw new InvalidDestinationUrlException("SSRF blocked: IP address " + address.getHostAddress() + " is private, loopback, or metadata");
                }
            }
        } catch (UnknownHostException e) {
            // In unit tests or offline environments, unresolved non-IP hostnames with valid domain syntax are allowed
            if (isDirectIpLiteral(host)) {
                throw new InvalidDestinationUrlException("Could not resolve IP literal: " + host);
            }
        }
    }

    private boolean isBlockedIpAddress(InetAddress address) {
        String hostAddress = address.getHostAddress();

        if (address.isLoopbackAddress() ||
            address.isAnyLocalAddress() ||
            address.isLinkLocalAddress() ||
            address.isSiteLocalAddress() ||
            address.isMulticastAddress()) {
            return true;
        }

        // Link-local / AWS / GCP / Azure metadata endpoint
        if (AWS_METADATA_IP.equals(hostAddress)) {
            return true;
        }

        // Additional RFC 1918 & Carrier-grade NAT checks
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;

            // 10.0.0.0/8
            if (b0 == 10) return true;
            // 172.16.0.0/12
            if (b0 == 172 && (b1 >= 16 && b1 <= 31)) return true;
            // 192.168.0.0/16
            if (b0 == 192 && b1 == 168) return true;
            // 127.0.0.0/8
            if (b0 == 127) return true;
            // 100.64.0.0/10 (Carrier-Grade NAT)
            if (b0 == 100 && (b1 >= 64 && b1 <= 127)) return true;
            // 0.0.0.0
            if (b0 == 0) return true;
        }

        return false;
    }

    private boolean isDirectIpLiteral(String host) {
        return host.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$") || host.contains(":");
    }
}
