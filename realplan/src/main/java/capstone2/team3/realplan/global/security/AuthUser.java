package capstone2.team3.realplan.global.security;

import capstone2.team3.realplan.domain.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class AuthUser implements UserDetails {

    private final Long userId;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthUser(Long userId, String email, String password) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public static AuthUser from(User user) {
        return new AuthUser(user.getUserId(), user.getEmail(), user.getPasswordHash());
    }

    @Override
    public String getUsername() {
        return email;
    }
}
