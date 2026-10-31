package br.com.httpinterfaces.config;

import br.com.httpinterfaces.domain.repository.ProductRepository;
import br.com.httpinterfaces.domain.service.CsvImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

    private final CsvImportService csvImportService;
    private final ProductRepository productRepository;

    private static final Map<String, String> CSV_SOURCES = new LinkedHashMap<>();

    static {
        CSV_SOURCES.put("data/amazon-products.csv",  "amazon");
        CSV_SOURCES.put("data/shopee-products.csv",  "shopee");
        CSV_SOURCES.put("data/lazada-products.csv",  "lazada");
        CSV_SOURCES.put("data/shein-products.csv",   "shein");
        CSV_SOURCES.put("data/walmart-products.csv", "walmart");
    }

    @Override
    public void run(ApplicationArguments args) {
        if (productRepository.count() > 0) {
            log.info("Products table already populated ({} records), skipping CSV import.", productRepository.count());
            return;
        }

        log.info("Products table is empty — starting CSV import...");
        int total = 0;

        for (Map.Entry<String, String> entry : CSV_SOURCES.entrySet()) {
            String filePath = entry.getKey();
            String source   = entry.getValue();

            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                log.warn("CSV file not found, skipping: {}", path.toAbsolutePath());
                continue;
            }

            try {
                byte[] bytes = Files.readAllBytes(path);
                MultipartFile file = new ByteArrayMultipartFile(path.getFileName().toString(), bytes);
                int imported = csvImportService.importFromFile(file, source);
                total += imported;
                log.info("Imported {} products from {} (source={})", imported, filePath, source);
            } catch (Exception e) {
                log.error("Failed to import {}: {}", filePath, e.getMessage(), e);
            }
        }

        log.info("CSV import complete. Total products imported: {}", total);
    }

    /** Minimal MultipartFile backed by a byte array — avoids spring-test dependency in main. */
    private record ByteArrayMultipartFile(String filename, byte[] content) implements MultipartFile {
        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return filename; }
        @Override public String getContentType() { return "text/csv"; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(File dest) throws IOException {
            try (FileOutputStream fos = new FileOutputStream(dest)) { fos.write(content); }
        }
    }
}
