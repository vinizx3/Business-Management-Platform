package com.pmei.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompanyCreateRequest {

    @NotBlank(message = "Name is required.")
    private String name;

    @NotBlank(message = "Document is required.")
    private String document;

    @Email
    @NotBlank(message = "Email is required.")
    private String email;

    @NotBlank(message = "Phone is required.")
    private String phone;
}
