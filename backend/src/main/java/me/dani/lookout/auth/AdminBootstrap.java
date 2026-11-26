package me.dani.lookout.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dani.lookout.auth.jpa.AppUser;
import me.dani.lookout.auth.jpa.AppUserRepository;
import me.dani.lookout.config.LookoutProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {
    private final LookoutProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final AppUserRepository repository;


    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if(repository.empty()){
            String email = properties.admin().email();
            String password = passwordEncoder.encode(properties.admin().password());
            repository.save(new AppUser(email,password));

            log.info("Created default user with email: {}", email);
        } else
            log.info("Default user already exists, skip creation. . .");
    }
}
