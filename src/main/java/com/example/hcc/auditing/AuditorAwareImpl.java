package com.example.hcc.auditing;


import com.example.hcc.entity.User;
import com.example.hcc.exceptions.UserMissingFromDatabaseException;
import com.example.hcc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuditorAwareImpl implements AuditorAware<User> {

    private final UserRepository userRepository;

    @Override
    public @NonNull Optional<User> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String username = ((Jwt)(authentication.getPrincipal())).getClaim("username");

        return userRepository.findByCognitoId(username)
                .or(() -> {
                    throw new UserMissingFromDatabaseException("User not found");
                });
    }
}
