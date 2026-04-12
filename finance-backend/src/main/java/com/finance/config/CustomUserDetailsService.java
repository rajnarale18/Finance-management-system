package com.finance.config;

import com.finance.model.User;
import com.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * CustomUserDetailsService - Tells Spring Security HOW to load a user from our database.
 *
 * Spring Security needs to know:
 * 1. Where to find the user (our MySQL DB via UserRepository)
 * 2. What the user's password is (to check against login input)
 * 3. What roles/authorities the user has
 *
 * We implement UserDetailsService to provide this info.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true) // keeps the session open so role loads in ONE query
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // Look up user in the database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Check if the account is active
        if (!user.isActive()) {
            throw new UsernameNotFoundException("User account is deactivated: " + username);
        }

        // Spring Security needs the role with "ROLE_" prefix
        // e.g., "ADMIN" becomes "ROLE_ADMIN"
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(
                "ROLE_" + user.getRole().getName()
        );

        // Return Spring Security's UserDetails object
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(authority)
        );
    }
}
