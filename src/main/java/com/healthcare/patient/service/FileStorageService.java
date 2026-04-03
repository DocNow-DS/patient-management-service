package com.healthcare.patient.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;
    private final boolean cloudinaryEnabled;
    private final String cloudinaryCloudName;
    private final String cloudinaryUploadPreset;
    private final long maxFileSizeBytes;
    private final Set<String> allowedExtensions;
    private final ObjectMapper objectMapper;
    private final String supabaseUrl;
    private final String supabaseServiceKey;
    private final String supabaseBucket;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public FileStorageService(
            @Value("${file.upload-dir:./uploads}") String uploadDir,
            @Value("${cloudinary.enabled:true}") boolean cloudinaryEnabled,
            @Value("${cloudinary.cloud-name:}") String cloudinaryCloudName,
            @Value("${cloudinary.upload-preset:}") String cloudinaryUploadPreset,
            @Value("${report.max-file-size-mb:10}") long maxFileSizeMb,
            @Value("${report.allowed-extensions:pdf,jpg,jpeg,png,doc,docx}") String allowedExtensionsCsv,
            @Value("${supabase.url:}") String supabaseUrl,
            @Value("${supabase.service-key:}") String supabaseServiceKey,
            @Value("${supabase.bucket:}") String supabaseBucket) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.cloudinaryEnabled = cloudinaryEnabled;
        this.cloudinaryCloudName = cloudinaryCloudName == null ? "" : cloudinaryCloudName.trim();
        this.cloudinaryUploadPreset = cloudinaryUploadPreset == null ? "" : cloudinaryUploadPreset.trim();
        this.maxFileSizeBytes = Math.max(1, maxFileSizeMb) * 1024L * 1024L;
        this.allowedExtensions = Arrays.stream(allowedExtensionsCsv.split(","))
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .map(v -> v.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        this.objectMapper = new ObjectMapper();

        this.supabaseUrl = supabaseUrl == null ? "" : supabaseUrl.trim();
        this.supabaseServiceKey = supabaseServiceKey == null ? "" : supabaseServiceKey.trim();
        this.supabaseBucket = supabaseBucket == null ? "" : supabaseBucket.trim();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Uploaded file is empty");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new RuntimeException("File exceeds max allowed size of " + (maxFileSizeBytes / (1024 * 1024)) + " MB");
        }

        String originalFilename = file.getOriginalFilename() == null ? "report" : file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        if (!extension.isBlank() && !allowedExtensions.isEmpty() && !allowedExtensions.contains(extension)) {
            throw new RuntimeException("Unsupported file type. Allowed: " + String.join(", ", allowedExtensions));
        }

        if (canUploadToCloudinary()) {
            try {
                return uploadToCloudinary(file, originalFilename);
            } catch (Exception ex) {
                System.out.println("Cloudinary upload failed, using local storage fallback: " + ex.getMessage());
            }
        }

        if (canUploadToSupabase()) {
            try {
                return uploadToSupabase(file, originalFilename);
            } catch (Exception ex) {
                System.out.println("Supabase upload failed, using local storage fallback: " + ex.getMessage());
            }
        }

        String fileName = UUID.randomUUID() + "_" + originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");

        try {
            if (fileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return targetLocation.toString();
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    private boolean canUploadToCloudinary() {
        return cloudinaryEnabled && !cloudinaryCloudName.isBlank() && !cloudinaryUploadPreset.isBlank();
    }

    private String uploadToCloudinary(MultipartFile file, String originalFilename) throws IOException {
        String boundary = "----HealthCareBoundary" + UUID.randomUUID();
        String endpoint = "https://api.cloudinary.com/v1_1/" + cloudinaryCloudName + "/raw/upload";

        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream output = connection.getOutputStream()) {
            writeTextPart(output, boundary, "upload_preset", cloudinaryUploadPreset);
            writeTextPart(output, boundary, "folder", "healthcare/reports");
            writeFilePart(output, boundary, file, originalFilename);
            output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        }

        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new RuntimeException("Cloudinary upload returned status " + status);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = reader.lines().collect(Collectors.joining("\n"));
            Map<String, Object> payload = objectMapper.readValue(response, new TypeReference<>() {});
            Object secureUrl = payload.get("secure_url");
            if (secureUrl == null || secureUrl.toString().isBlank()) {
                throw new RuntimeException("Cloudinary response did not include secure_url");
            }
            return secureUrl.toString();
        }
    }

    private void writeTextPart(OutputStream output, String boundary, String name, String value) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write((value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private void writeFilePart(OutputStream output, String boundary, MultipartFile file, String originalFilename) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + originalFilename + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + (file.getContentType() == null ? "application/octet-stream" : file.getContentType()) + "\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write(file.getBytes());
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot == -1 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private boolean canUploadToSupabase() {
        return !supabaseUrl.isBlank() && !supabaseServiceKey.isBlank() && !supabaseBucket.isBlank();
    }

    private String uploadToSupabase(MultipartFile file, String originalFilename) throws Exception {
        String path = "patients/" + UUID.randomUUID() + "_" + originalFilename;
        String encodedPath = Arrays.stream(path.split("/"))
                .map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));

        String uploadUrl = supabaseUrl + "/storage/v1/object/" + supabaseBucket + "/" + encodedPath;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Authorization", "Bearer " + supabaseServiceKey)
                .header("apikey", supabaseServiceKey)
                .header("x-upsert", "true")
                .header("Content-Type", file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return supabaseUrl + "/storage/v1/object/public/" + supabaseBucket + "/" + encodedPath;
        } else {
            throw new RuntimeException("Supabase upload failed: " + response.statusCode() + " " + response.body());
        }
    }
}
