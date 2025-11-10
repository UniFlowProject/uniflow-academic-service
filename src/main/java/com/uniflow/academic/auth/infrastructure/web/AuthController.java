package com.uniflow.academic.auth.infrastructure.web;

import com.uniflow.academic.auth.application.ports.in.AuthenticateWithGoogleUseCase;
import com.uniflow.academic.auth.infrastructure.web.dto.AuthResponse;
import com.uniflow.academic.auth.infrastructure.web.dto.GoogleAuthRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final AuthenticateWithGoogleUseCase authenticateWithGoogleUseCase;

    @PostMapping("/google/callback")
    @Operation(summary = "Authenticate with Google")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Authentication failed")
    })
    public ResponseEntity<AuthResponse> authenticateWithGoogle(
            @Valid @RequestBody GoogleAuthRequest request
    ) {
        try {
            log.info("POST /auth/google - Google authentication request");
            AuthResponse response = authenticateWithGoogleUseCase.execute(
                    request.getCode(),
                    request.getRedirectUri()
            );
            System.out.println("Authentication response: " + response);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Exception during transaction", ex, ex.getCause());
            throw ex;
        }
    }

//    @GetMapping("/google/callback")
//    @Operation(summary = "Authenticate with Google")
//    @ApiResponses({
//            @ApiResponse(
//                    responseCode = "200",
//                    description = "Authentication successful",
//                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
//            ),
//            @ApiResponse(responseCode = "400", description = "Invalid request"),
//            @ApiResponse(responseCode = "401", description = "Authentication failed")
//    })
//    public ResponseEntity<AuthResponse> authenticateWithGoogle2(
//            @RequestParam("code") String code
//    ) {
//        log.info("POST /auth/google - Google authentication request");
//        AuthResponse response = authenticateWithGoogleUseCase.execute(
//                code,
//                null
//        );
//        return ResponseEntity.ok(response);
//    }
}