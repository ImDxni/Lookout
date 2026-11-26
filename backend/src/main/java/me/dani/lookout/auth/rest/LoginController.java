package me.dani.lookout.auth.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import static me.dani.lookout.shared.utils.EmailUtils.normalize;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class LoginController {
    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final HttpSessionSecurityContextRepository repository = new HttpSessionSecurityContextRepository();


    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest,
                                      HttpServletResponse httpServletResponse){
        Authentication auth = UsernamePasswordAuthenticationToken.unauthenticated(normalize(request.email()),request.password());

        Authentication authenticated = this.authenticationManager.authenticate(auth);

        sessionAuthenticationStrategy.onAuthentication(authenticated, httpServletRequest, httpServletResponse);

        if (httpServletRequest.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken csrfToken) {
            csrfToken.getToken();
        }

        SecurityContextHolderStrategy holder = SecurityContextHolder.getContextHolderStrategy();
        SecurityContext context = holder.createEmptyContext();
        context.setAuthentication(authenticated);
        holder.setContext(context);

        repository.saveContext(context,httpServletRequest,httpServletResponse);

        return ResponseEntity.ok().build();
    }


    @GetMapping("/me")
    public ResponseEntity<String> me(@AuthenticationPrincipal UserDetails principal){

        return ResponseEntity.ok(principal.getUsername());
    }
}
