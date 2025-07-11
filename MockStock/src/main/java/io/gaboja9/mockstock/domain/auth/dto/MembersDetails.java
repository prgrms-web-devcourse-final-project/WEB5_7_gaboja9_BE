package io.gaboja9.mockstock.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
@Accessors(chain = true)
public class MembersDetails implements OAuth2User {

    private String name;
    private String email;
    private String profileImage;
    @Setter private Map<String, Object> attributes;

    @Setter private String role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(this.role));
    }

    @Builder
    public MembersDetails(
            String name,
            String email,
            String profileImage,
            Map<String, Object> attributes,
            String role) {
        this.name = name;
        this.email = email;
        this.profileImage = profileImage;
        this.attributes = attributes;
        this.role = role;
    }
}
