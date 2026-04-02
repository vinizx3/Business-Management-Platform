package com.pmei.security;

import com.pmei.company.model.User;
import com.pmei.company.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for loading user details during authentication.
 *
 * Integrates with Spring Security to fetch user data from the database.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Service responsible for loading user details during authentication.
     *
     * Integrates with Spring Security to fetch user data from the database.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));


        return new CustomUserPrincipal(
                user,
                List.of(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }
}

