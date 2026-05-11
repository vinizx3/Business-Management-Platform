package com.pmei.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    // Company
    @NotBlank
    private String companyName;

    @NotBlank
    private String companyDocument;

    @Email
    @NotBlank
    private String companyEmail;

    private String companyPhone;

    // User
    @NotBlank
    private String userName;

    @Email
    @NotBlank
    private String userEmail;

    @NotBlank
    private String password;
}
