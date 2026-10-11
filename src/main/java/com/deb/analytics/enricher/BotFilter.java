package com.deb.analytics.enricher;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BotFilter {

    private static final List<String> BOT_SIGNATURES = List.of(
            "googlebot", "bingbot", "yandexbot", "duckduckbot", "baiduspider",
            "twitterbot", "facebookexternalhit", "slackbot", "discordbot",
            "whatsapp", "telegrambot", "linkedinbot", "applebot", "semrushbot",
            "ahrefsbot", "mj12bot", "dotbot", "petalbot", "curl", "wget",
            "python-requests", "aiohttp", "go-http-client", "apache-httpclient",
            "postmanruntime", "insomnia", "bot", "crawler", "spider"
    );

    /**
     * Determines whether a User-Agent represents a crawler, unfurler, or bot.
     */
    public boolean isBot(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return false;
        }

        String lowerUa = userAgent.toLowerCase();
        for (String signature : BOT_SIGNATURES) {
            if (lowerUa.contains(signature)) {
                return true;
            }
        }
        return false;
    }
}
