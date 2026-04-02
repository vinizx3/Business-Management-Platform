package com.pmei.company.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponseDTO {

    private UUID id;
    private String name;
    private String document;
    private String email;
    private String phone;
}
