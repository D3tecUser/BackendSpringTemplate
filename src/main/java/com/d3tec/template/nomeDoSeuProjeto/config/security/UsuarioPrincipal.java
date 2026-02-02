package com.d3tec.template.nomeDoSeuProjeto.config.security;

import com.d3tec.template.nomeDoSeuProjeto.dto.UserDto;
import com.d3tec.template.nomeDoSeuProjeto.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class UsuarioPrincipal implements UserDetails {

    private final UserDto userDto;
    private final Collection<? extends GrantedAuthority> authorities;

    public UsuarioPrincipal(User user, Collection<? extends GrantedAuthority> authorities) {
        this.userDto = mapToDto(user);
        this.authorities = authorities;
    }

    public UserDto getUserDto() { return this.userDto; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getUsername() { return userDto.getEmail(); }

    @Override public String getPassword() { return null; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    private UserDto mapToDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());

        return userDto;
    }
}
