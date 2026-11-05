package com.scuec.icpc.common.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 登录用户主体。角色以 ROLE_ 前缀交给 Spring Security。
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long memberId;
    private final String studentId;
    private final String name;
    private final List<String> roles;

    public UserPrincipal(Long memberId, String studentId, String name, List<String> roles) {
        this.memberId = memberId;
        this.studentId = studentId;
        this.name = name;
        this.roles = roles == null ? List.of() : List.copyOf(roles);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return studentId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
