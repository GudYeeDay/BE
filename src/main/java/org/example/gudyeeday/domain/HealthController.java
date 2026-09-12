package org.example.gudyeeday.domain;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health Check API", description = "상태체크")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/healthCheck")
public class HealthController {

    @GetMapping
    @Operation(summary = "서버 상태 확인")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}