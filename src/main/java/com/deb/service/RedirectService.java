package com.deb.service;

import com.deb.dto.CreateRedirectRequest;
import com.deb.entity.Redirect;
import com.deb.exception.AliasGenerationException;
import com.deb.exception.BadRequestException;
import com.deb.exception.NotFoundException;
import com.deb.repository.RedirectRepository;
import com.deb.util.Base62Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class RedirectService {

    private static final Logger log = LoggerFactory.getLogger(RedirectService.class);
    private static final int MAX_ALIAS_GENERATION_ATTEMPTS = 5;

    private RedirectRepository redirectRepository;

    @Autowired
    public RedirectService(RedirectRepository redirectRepository) {
        this.redirectRepository = redirectRepository;
    }

    @Transactional
    public Redirect createRedirect(CreateRedirectRequest request) {
        Optional<Redirect> existingRedirectOpt = redirectRepository.findByUrl(request.getUrl());
        if (existingRedirectOpt.isPresent()) {
            Redirect existingRedirect = existingRedirectOpt.get();
            log.info("URL [{}] already exists with alias [{}]. Returning existing entry.", request.getUrl(), existingRedirect.getAlias());
            return existingRedirect;
        }

        log.info("URL [{}] not found. Proceeding to create new redirect.", request.getUrl());

        String alias;
        boolean isCustomAlias = StringUtils.hasText(request.getAlias()); // Check if alias is provided and not empty

        if (isCustomAlias) {
            alias = request.getAlias().trim();
            log.info("Attempting to use custom alias: {}", alias);
            if (redirectRepository.existsByAlias(alias)) {
                log.warn("Custom alias already exists: {}", alias);
                throw new BadRequestException("Custom alias '" + alias + "' already exists.");
            }
        } else {
            // User wants a random alias
            log.info("Generating random alias for URL: {}", request.getUrl());
            int attempts = 0;
            do {
                alias = Base62Utils.generateRandomAlias(); // Generate potential alias
                attempts++;
                if (attempts > MAX_ALIAS_GENERATION_ATTEMPTS) {
                    log.error("Failed to generate unique alias after {} attempts.", MAX_ALIAS_GENERATION_ATTEMPTS);
                    throw new AliasGenerationException("Could not generate a unique alias. Please try again later.");
                }
            } while (redirectRepository.existsByAlias(alias)); // Check for collision
            log.info("Generated unique alias: {}", alias);
        }

        // Alias is unique (either custom or generated), proceed to save
        Redirect redirect = new Redirect(alias, request.getUrl());
        Redirect savedRedirect = redirectRepository.save(redirect);
        log.info("Saved redirect: Alias={}, URL={}, ID={}", savedRedirect.getAlias(), savedRedirect.getUrl(), savedRedirect.getId());

        return savedRedirect;
    }

    @Transactional(readOnly = true)
    public Redirect getRedirect(String alias) {
        log.debug("Attempting to find redirect for alias: {}", alias);
        return redirectRepository.findByAlias(alias)
                .orElseThrow(() -> {
                    log.warn("Alias not found: {}", alias);
                    return new NotFoundException("Alias '" + alias + "' not found.");
                });
    }
}
