package com.myshareapp.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PhpCompatiblePasswordEncoder implements PasswordEncoder {
    private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder(10);

    @Override
    public String encode(CharSequence rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null || rawPassword == null) return false;
        String normalized = encodedPassword.startsWith("$2y$")
                ? "$2a$" + encodedPassword.substring(4)
                : encodedPassword;
        return delegate.matches(rawPassword, normalized);
    }
}
