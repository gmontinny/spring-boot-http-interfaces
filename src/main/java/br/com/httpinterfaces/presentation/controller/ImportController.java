package br.com.httpinterfaces.presentation.controller;

import br.com.httpinterfaces.domain.service.CsvImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/import")
@RequiredArgsConstructor
@Tag(name = "CSV Import", description = "Import product data from CSV files (amazon, shopee, lazada, shein, walmart)")
public class ImportController {

    private final CsvImportService csvImportService;

    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Import products from CSV file",
               description = "Upload a CSV file from the data/ folder. Use 'source' param to identify origin (amazon, shopee, etc.)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, Object>> importProducts(
            @RequestPart("file") MultipartFile file,
            @RequestParam String source) {
        try {
            int count = csvImportService.importFromFile(file, source);
            return ResponseEntity.ok(Map.of(
                    "imported", count,
                    "source", source,
                    "filename", file.getOriginalFilename()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Import failed: " + e.getMessage(),
                    "source", source
            ));
        }
    }
}
