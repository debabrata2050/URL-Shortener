package com.deb.repository;

import com.deb.entity.Redirect;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RedirectRepository extends JpaRepository<Redirect,Long> {
    Optional<Redirect> findByAlias(String alias);
    Optional<Redirect> findByUrl(String url);
    Boolean existsByAlias(String alias);
}
