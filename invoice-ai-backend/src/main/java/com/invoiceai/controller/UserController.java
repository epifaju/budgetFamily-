package com.invoiceai.controller;

import com.invoiceai.dto.request.ChangePasswordRequest;
import com.invoiceai.dto.request.UpdateProfileRequest;
import com.invoiceai.dto.response.UserDataExportResponse;
import com.invoiceai.dto.response.UserResponse;
import com.invoiceai.service.UserDataExportService;
import com.invoiceai.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserDataExportService userDataExportService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/export")
    public ResponseEntity<UserDataExportResponse> exportMyData() {
        return ResponseEntity.ok(userDataExportService.exportCurrentUser());
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount() {
        userService.deleteCurrentUser();
        return ResponseEntity.noContent().build();
    }
}
