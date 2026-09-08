package ma.onee.dti.projectportfolio.security;

import java.util.Collections;
import ma.onee.dti.projectportfolio.entity.Utilisateur;
import ma.onee.dti.projectportfolio.repository.UtilisateurRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    public CustomUserDetailsService(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouve: " + username));

        String role = utilisateur.getRole() != null && utilisateur.getRole().getLibelle() != null
                ? utilisateur.getRole().getLibelle().name()
                : "ROLE_UTILISATEUR_SIMPLE";

        return User.builder()
                .username(utilisateur.getEmail())
                .password(utilisateur.getMotDePasse())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(role)))
                .build();
    }
}
