package com.pmei.auth.service;

import com.pmei.auth.dto.LoginRequest;
import com.pmei.auth.dto.LoginResponse;
import com.pmei.auth.dto.RegisterRequest;
import com.pmei.security.JwtService;
import com.pmei.shared.exception.ResourceAlreadyExistsException;
import com.pmei.company.model.Company;
import com.pmei.company.model.Role;
import com.pmei.company.model.User;
import com.pmei.company.repository.CompanyRepository;
import com.pmei.company.repository.UserRepository;

import com.pmei.shared.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


/**
 * Service responsible for authenticating and registering users and companies.
 *
 * Contains business rules related to registering a new company
 * along with its administrator user, in addition to the login process
 * with JWT token generation.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Registers a new company and its administrator user.
     *
     * Business rules:
     * - Does not allow registration of a company with an already existing CNPJ/document
     * - Does not allow registration of a user with an already existing email
     * - Automatically creates a user with the ADMIN role linked to the company
     *
     * @param request Data required for company and user registration
     * @throws ResourceAlreadyExistsException If the company document or email is already registered
     */
    @Transactional
    public void register(RegisterRequest request){

        // Checks if a company already exists with the same document (e.g., CNPJ)
        if (companyRepository.findByDocument(request.getCompanyDocument()).isPresent()) {
            throw new ResourceAlreadyExistsException("Company already registered.");
        }

        // Checks if a user with the same email already exists
        if (userRepository.findByEmail(request.getUserEmail()).isPresent()) {
            throw new ResourceAlreadyExistsException("Email already registered.");
        }

        // Creation of the Company entity using the received data
        Company company = Company.builder()
                .name(request.getCompanyName())
                .document(request.getCompanyDocument())
                .email(request.getCompanyEmail())
                .active(true)
                .phone(request.getCompanyPhone())
                .build();

        // The company persists in the database before associating it with the user
        companyRepository.save(company);

        // Creation of an administrator user linked to the newly created company
        User user = User.builder()
                .name(request.getUserName())
                .email(request.getUserEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                // The password is encrypted before saving (a good security practice)
                .role(Role.ADMIN) //The first user is always ADMIN
                .company(company)
                .active(true)
                .build();

        // The user persists in the database
        userRepository.save(user);
    }

    /**
     * Performs user authentication and generates a JWT token
     *
     * Flow:
     * - Authenticates credentials using AuthenticationManager
     * - Retrieves the user from the database
     * - Generates a JWT token for authenticated access
     *
     * @param request Login data (email and password)
     * @return JWT token encapsulated in LoginResponse
     * @throws ResourceNotFoundException If the user is not found
     */
    public LoginResponse login(LoginRequest request) {

        // Performs credential authentication (Spring Security)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password())
        );

        // Retrieves the user from the database after authentication
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.email()));

        // Generating the JWT token for the authenticated user
        String token = jwtService.generateToken(user);

        return new LoginResponse(token);
    }
}
