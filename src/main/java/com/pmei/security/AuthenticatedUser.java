package com.pmei.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class AuthenticatedUser {

    private String email;
    private String role;
    private UUID companyId;
}
