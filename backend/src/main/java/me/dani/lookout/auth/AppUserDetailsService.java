package me.dani.lookout.auth;

import lombok.RequiredArgsConstructor;
import me.dani.lookout.auth.jpa.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {
    private final AppUserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return repository.findByEmailIgnoreCase(email)
                .map(appUser -> User.withUsername(appUser.getEmail()).password(appUser.getPassword()).roles("ADMIN"))
                .orElseThrow(() -> new UsernameNotFoundException(email))
                .build();
    }
}
