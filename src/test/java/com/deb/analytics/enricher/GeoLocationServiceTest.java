package com.deb.analytics.enricher;

import com.maxmind.geoip2.GeoIp2Provider;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeoLocationServiceTest {

    @Mock
    private GeoIp2Provider geoIp2Provider;

    @Test
    @DisplayName("Localhost and Private IPs return LO and PR without querying MaxMind")
    void testLocalhostAndPrivateIps() {
        GeoLocationService service = new GeoLocationService(geoIp2Provider);

        GeoLocationService.GeoLocation loopback = service.resolve("127.0.0.1");
        assertEquals("LO", loopback.countryCode());
        assertEquals("Localhost", loopback.city());

        GeoLocationService.GeoLocation ipv6 = service.resolve("::1");
        assertEquals("LO", ipv6.countryCode());

        GeoLocationService.GeoLocation lan = service.resolve("192.168.1.50");
        assertEquals("PR", lan.countryCode());
        assertEquals("Private Network", lan.city());

        verifyNoInteractions(geoIp2Provider);
    }

    @Test
    @DisplayName("MaxMind provider lookup succeeds for public IP")
    void testMaxMindLookupSuccess() throws Exception {
        CityResponse cityResponse = mock(CityResponse.class);
        Country country = mock(Country.class);
        City city = mock(City.class);

        when(country.isoCode()).thenReturn("IN");
        when(city.name()).thenReturn("Bangalore");
        when(cityResponse.country()).thenReturn(country);
        when(cityResponse.city()).thenReturn(city);

        when(geoIp2Provider.city(any(InetAddress.class))).thenReturn(cityResponse);

        GeoLocationService service = new GeoLocationService(geoIp2Provider);
        GeoLocationService.GeoLocation result = service.resolve("103.21.244.1");

        assertEquals("IN", result.countryCode());
        assertEquals("Bangalore", result.city());
        verify(geoIp2Provider).city(any(InetAddress.class));
    }

    @Test
    @DisplayName("MaxMind failure gracefully falls back to heuristic subnet match")
    void testMaxMindFailureFallback() throws Exception {
        when(geoIp2Provider.city(any(InetAddress.class))).thenThrow(new RuntimeException("API rate limit"));

        GeoLocationService service = new GeoLocationService(geoIp2Provider);
        GeoLocationService.GeoLocation result = service.resolve("8.8.8.8");

        assertEquals("US", result.countryCode());
        assertEquals("North America", result.city());
    }

    @Test
    @DisplayName("Null provider uses heuristic resolver directly")
    void testNullProviderUsesHeuristics() {
        GeoLocationService service = new GeoLocationService();

        GeoLocationService.GeoLocation result = service.resolve("81.2.69.142");
        assertEquals("GB", result.countryCode());
        assertEquals("United Kingdom", result.city());
    }

    @Test
    @DisplayName("Local DatabaseReader successfully resolves real IP from data/GeoLite2-City.mmdb")
    void testLocalDatabaseReader() throws Exception {
        java.io.File dbFile = new java.io.File("data/GeoLite2-City.mmdb");
        if (dbFile.exists()) {
            try (com.maxmind.geoip2.DatabaseReader reader = new com.maxmind.geoip2.DatabaseReader.Builder(dbFile).build()) {
                GeoLocationService service = new GeoLocationService(reader);
                GeoLocationService.GeoLocation result = service.resolve("8.8.8.8");
                assertEquals("US", result.countryCode());
            }
        }
    }
}
