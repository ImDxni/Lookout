package me.dani.lookout.auth.jpa;

import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends ListCrudRepository<AppUser, UUID> {

    default boolean empty(){
        return count() == 0;
    }

    Optional<AppUser> findByEmailIgnoreCase(String email);

}
