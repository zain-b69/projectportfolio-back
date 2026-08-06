package ma.onee.dsi.projectportfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import ma.onee.dsi.projectportfolio.dto.AuthResponse;
import ma.onee.dsi.projectportfolio.dto.LoginRequest;
import ma.onee.dsi.projectportfolio.entity.Role;
import ma.onee.dsi.projectportfolio.entity.Utilisateur;
import ma.onee.dsi.projectportfolio.enums.RoleLibelle;
import ma.onee.dsi.projectportfolio.repository.RoleRepository;
import ma.onee.dsi.projectportfolio.repository.UtilisateurRepository;
import ma.onee.dsi.projectportfolio.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private HistoriqueService historiqueService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                utilisateurRepository,
                roleRepository,
                passwordEncoder,
                jwtService,
                authenticationManager,
                historiqueService
        );
    }

    @Test
    void authenticateReturnsTokenAndFlatUserFields() {
        LoginRequest request = new LoginRequest();
        request.setEmail(" K.BENNANI@ONEE.MA ");
        request.setMotDePasse("123456");

        Utilisateur utilisateur = utilisateur();

        when(utilisateurRepository.findByEmailIgnoreCase("k.bennani@onee.ma"))
                .thenReturn(Optional.of(utilisateur));
        when(utilisateurRepository.save(utilisateur)).thenReturn(utilisateur);
        when(jwtService.generateToken(utilisateur)).thenReturn("jwt-token");

        AuthResponse response = authService.authenticate(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getIdUtilisateur()).isEqualTo(2L);
        assertThat(response.getNom()).isEqualTo("Bennani");
        assertThat(response.getPrenom()).isEqualTo("Karim");
        assertThat(response.getEmail()).isEqualTo("k.bennani@onee.ma");
        assertThat(response.getRole()).isEqualTo("ROLE_RESPONSABLE_PROJET");

        ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());

        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) authenticationCaptor.getValue();
        assertThat(authentication.getPrincipal()).isEqualTo("k.bennani@onee.ma");
        assertThat(authentication.getCredentials()).isEqualTo("123456");
    }

    private Utilisateur utilisateur() {
        Role role = new Role();
        role.setLibelle(RoleLibelle.ROLE_RESPONSABLE_PROJET);

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setIdUtilisateur(2L);
        utilisateur.setNom("Bennani");
        utilisateur.setPrenom("Karim");
        utilisateur.setEmail("k.bennani@onee.ma");
        utilisateur.setMotDePasse("$2a$10$hash");
        utilisateur.setRole(role);

        return utilisateur;
    }
}
