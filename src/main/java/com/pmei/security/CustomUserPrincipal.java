package com.pmei.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

/**
 * Custom implementation of Spring Security UserDetails.
 *
 * Represents the authenticated user and stores essential authentication data.
 */
@Getter
public class CustomUserPrincipal implements UserDetails {

    private final UUID id;
    private final String username;
    private final String email;
    private final String password;
    private final UUID companyId;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Constructor used when loading user from database.
     */
    public CustomUserPrincipal(com.pmei.company.model.User user, Collection<? extends GrantedAuthority> authorities) {
        this.id = user.getId();
        this.username = user.getEmail();
        this.password = user.getPassword();
        this.email = user.getEmail();
        this.companyId = user.getCompany().getId();
        this.authorities = authorities;
    }

    /**
     * Constructor used when loading user from database.
     */
    public CustomUserPrincipal(String email, UUID companyId, Collection<? extends GrantedAuthority> authorities) {
        this.id = null;
        this.username = email;
        this.password = null;
        this.email = email;
        this.companyId = companyId;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}