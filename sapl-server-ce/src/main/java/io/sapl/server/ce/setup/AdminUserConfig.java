package io.sapl.server.ce.setup;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserConfig {
    static final String USERNAME_PATH        = "io.sapl/server/accesscontrol/admin-username";
    static final String ENCODEDPASSWORD_PATH = "io.sapl/server/accesscontrol/encoded-admin-password";

    private String  username       = "";
    private String  password       = "";
    private String  passwordRepeat = "";
    private boolean saved          = false;

    public String getEncodedPassword() {
        PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        return encoder.encode(this.password);
    }

    public boolean isValidConfig() {
        return !this.username.isEmpty() && this.password.equals(this.passwordRepeat);
    }

    public AdminUserPasswordStrength getPasswordStrength() {
        if (password.length() > 9) {
            return AdminUserPasswordStrength.STRONG;
        } else if (password.length() > 5) {
            return AdminUserPasswordStrength.MODERATE;

        } else {
            return AdminUserPasswordStrength.WEAK;
        }
    }

}
