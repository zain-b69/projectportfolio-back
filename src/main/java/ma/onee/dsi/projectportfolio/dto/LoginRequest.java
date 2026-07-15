package ma.onee.dsi.projectportfolio.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    private String email;
    private String motDePasse;
}
