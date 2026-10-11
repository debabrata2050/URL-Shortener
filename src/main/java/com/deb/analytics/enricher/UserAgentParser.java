package com.deb.analytics.enricher;

import org.springframework.stereotype.Component;

@Component
public class UserAgentParser {

    public String parseDeviceType(String userAgent, boolean isBot) {
        if (isBot) {
            return "BOT";
        }
        if (userAgent == null || userAgent.isEmpty()) {
            return "UNKNOWN";
        }

        String lowerUa = userAgent.toLowerCase();
        if (lowerUa.contains("tablet") || lowerUa.contains("ipad")) {
            return "TABLET";
        }
        if (lowerUa.contains("mobile") || lowerUa.contains("iphone") || lowerUa.contains("android")) {
            return "MOBILE";
        }
        if (lowerUa.contains("windows") || lowerUa.contains("macintosh") || lowerUa.contains("linux") || lowerUa.contains("x11")) {
            return "DESKTOP";
        }

        return "OTHER";
    }

    public String parseBrowserFamily(String userAgent, boolean isBot) {
        if (isBot) {
            return "BOT";
        }
        if (userAgent == null || userAgent.isEmpty()) {
            return "UNKNOWN";
        }

        String lowerUa = userAgent.toLowerCase();

        if (lowerUa.contains("edg/") || lowerUa.contains("edge/")) {
            return "EDGE";
        }
        if (lowerUa.contains("opr/") || lowerUa.contains("opera/")) {
            return "OPERA";
        }
        if (lowerUa.contains("chrome/") && !lowerUa.contains("chromium/")) {
            return "CHROME";
        }
        if (lowerUa.contains("firefox/")) {
            return "FIREFOX";
        }
        if (lowerUa.contains("safari/") && !lowerUa.contains("chrome/")) {
            return "SAFARI";
        }

        return "OTHER";
    }
}
