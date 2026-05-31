package com.invoiceai.security;

import com.invoiceai.domain.User;
import com.invoiceai.exception.ResourceNotFoundException;
import com.invoiceai.repository.UserRepository;
import java.util.Collections;
import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user;
        try {
            UUID userId = UUID.fromString(username);
            user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        } catch (IllegalArgumentException exception) {
            user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        }

        return org.springframework.security.core.userdetails.User.withUsername(user.getId().toString())
            .password(user.getPasswordHash())
            .authorities(Collections.emptyList())
            .accountLocked(false)
            .disabled(false)
            .build();
    }
}




