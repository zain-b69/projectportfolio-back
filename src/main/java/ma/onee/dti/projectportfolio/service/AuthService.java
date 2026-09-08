package ma.onee.dti.projectportfolio.service;

import java.time.LocalDateTime;
import ma.onee.dti.projectportfolio.dto.AuthResponse;
import ma.onee.dti.projectportfolio.dto.LoginRequest;
import ma.onee.dti.projectportfolio.dto.RegisterRequest;
import ma.onee.dti.projectportfolio.entity.Role;
import ma.onee.dti.projectportfolio.entity.Utilisateur;
import ma.onee.dti.projectportfolio.enums.RoleLibelle;
import ma.onee.dti.projectportfolio.enums.TypeAction;
import ma.onee.dti.projectportfolio.exception.DuplicateResourceException;
import ma.onee.dti.projectportfolio.exception.ResourceNotFoundException;
import ma.onee.dti.projectportfolio.repository.RoleRepository;
import ma.onee.dti.projectportfolio.repository.UtilisateurRepository;
import ma.onee.dti.projectportfolio.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final HistoriqueService historiqueService;

    public AuthService(
            UtilisateurRepository utilisateurRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            HistoriqueService historiqueService
    ) {
        this.utilisateurRepository = utilisateurRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.historiqueService = historiqueService;
    }

    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (utilisateurRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("Un utilisateur avec cet email existe deja");
        }

        Role defaultRole = roleRepository.findByLibelle(RoleLibelle.ROLE_UTILISATEUR_SIMPLE)
                .orElseThrow(() -> new ResourceNotFoundException("Role par defaut introuvable"));

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(request.getNom().trim());
        utilisateur.setPrenom(request.getPrenom().trim());
        utilisateur.setEmail(normalizedEmail);
        utilisateur.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        utilisateur.setDateDerniereConnexion(LocalDateTime.now());
        utilisateur.setRole(defaultRole);

        Utilisateur savedUser = utilisateurRepository.save(utilisateur);
        String jwtToken = jwtService.generateToken(savedUser);

        return buildAuthResponse(jwtToken, savedUser);
    }

    @Transactional
    public AuthResponse authenticate(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().trim().toLowerCase(),
                        request.getMotDePasse()
                )
        );

        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        utilisateur.setDateDerniereConnexion(LocalDateTime.now());
        utilisateurRepository.save(utilisateur);

        historiqueService.record(
                null,
                utilisateur,
                TypeAction.CONNEXION,
                "Connexion de l'utilisateur " + utilisateur.getEmail()
        );

        String jwtToken = jwtService.generateToken(utilisateur);

        return buildAuthResponse(jwtToken, utilisateur);
    }

    private AuthResponse buildAuthResponse(String token, Utilisateur utilisateur) {
        return AuthResponse.builder()
                .token(token)
                .idUtilisateur(utilisateur.getIdUtilisateur())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .email(utilisateur.getEmail())
                .role(utilisateur.getRole() != null && utilisateur.getRole().getLibelle() != null
                        ? utilisateur.getRole().getLibelle().name()
                        : null)
                .build();
    }
}
