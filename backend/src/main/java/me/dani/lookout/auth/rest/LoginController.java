package me.dani.lookout.auth.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LoginController {
    private final AuthenticationManager authenticationManager;

    @PostMapping("/api/auth/login")
    public ResponseEntity<Void> login(@RequestBody LoginRequest request){
        Authentication auth = UsernamePasswordAuthenticationToken.unauthenticated(request.email(),request.password());

        Authentication response = this.authenticationManager.authenticate(auth);

        //Non so come rispondere con il token qui
    }
}
