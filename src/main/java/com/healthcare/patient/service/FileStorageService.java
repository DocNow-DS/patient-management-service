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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final Path fileStorageLocation;
    private final boolean cloudinaryEnabled;
    private final String cloudinaryCloudName;
    private final String cloudinaryUploadPreset;
    private final long maxFileSizeBytes;
    private final Set<String> allowedExtensions;
    private final boolean failOnRemoteUploadError;
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
            @Value("${report.fail-on-remote-upload-error:true}") boolean failOnRemoteUploadError,
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
        this.failOnRemoteUploadError = failOnRemoteUploadError;
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
                logger.warn("Cloudinary upload failed: {}", ex.getMessage(), ex);
                if (failOnRemoteUploadError) {
                    throw new RuntimeException("Cloudinary upload failed", ex);
                }
            }
        }

        if (canUploadToSupabase()) {
            try {
                return uploadToSupabase(file, originalFilename);
            } catch (Exception ex) {
                logger.warn("Supabase upload failed: {}", ex.getMessage(), ex);
                if (failOnRemoteUploadError) {
                    throw new RuntimeException("Supabase upload failed", ex);
                }
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

    private String encodePathComponent(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to encode path component", e);
        }
    }

    private boolean canUploadToSupabase() {
        return !supabaseUrl.isBlank() && !supabaseServiceKey.isBlank() && !supabaseBucket.isBlank();
    }

    private String uploadToSupabase(MultipartFile file, String originalFilename) throws Exception {
        String path = "patients/" + UUID.randomUUID() + "_" + originalFilename;
        String encodedPath = Arrays.stream(path.split("/"))
            .map(this::encodePathComponent)
            .collect(Collectors.joining("/"));

        String storageBase = getStorageBaseUrl();
        String uploadUrl = storageBase + "/storage/v1/object/" + supabaseBucket + "/" + encodedPath;
        logger.info("Uploading report to Supabase storage: bucket='{}', path='{}'", supabaseBucket, encodedPath);
        logger.debug("Upload metadata: originalFilename='{}', size={}, contentType={}", originalFilename, file.getSize(), file.getContentType());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Authorization", "Bearer " + supabaseServiceKey)
                .header("apikey", supabaseServiceKey)
                .header("x-upsert", "true")
                .header("Content-Type", file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                .build();

        try {
            logger.debug("Sending PUT to Supabase: {}", uploadUrl);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("Supabase upload response: status={}, bodyLen={}", response.statusCode(), response.body() == null ? 0 : response.body().length());
            logger.debug("Supabase response body: {}", response.body());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return createSupabaseSignedUrl(storageBase, encodedPath);
            } else {
                throw new RuntimeException("Supabase upload failed: " + response.statusCode() + " " + response.body());
            }
        } catch (Exception e) {
            logger.error("Exception while uploading to Supabase: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String createSupabaseSignedUrl(String storageBase, String encodedPath) throws Exception {
        return createSupabaseSignedUrl(storageBase, supabaseBucket, encodedPath);
    }

    private String createSupabaseSignedUrl(String storageBase, String bucketName, String encodedPath) throws Exception {
        String signUrl = storageBase + "/storage/v1/object/sign/" + bucketName + "/" + encodedPath;

        HttpRequest signRequest = HttpRequest.newBuilder()
                .uri(URI.create(signUrl))
                .header("Authorization", "Bearer " + supabaseServiceKey)
                .header("apikey", supabaseServiceKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"expiresIn\":604800}"))
                .build();

        HttpResponse<String> signResponse = httpClient.send(signRequest, HttpResponse.BodyHandlers.ofString());
        if (signResponse.statusCode() < 200 || signResponse.statusCode() >= 300) {
            throw new RuntimeException("Supabase sign URL failed: " + signResponse.statusCode() + " " + signResponse.body());
        }

        Map<String, Object> payload = objectMapper.readValue(signResponse.body(), new TypeReference<>() {});
        Object signed = payload.get("signedURL");
        if (signed == null || signed.toString().isBlank()) {
            throw new RuntimeException("Supabase signed URL missing in response");
        }

        String signedUrl = signed.toString();
        if (signedUrl.startsWith("http://") || signedUrl.startsWith("https://")) {
            return signedUrl;
        }

        if (!signedUrl.startsWith("/")) {
            signedUrl = "/" + signedUrl;
        }

        if (!signedUrl.startsWith("/storage/v1/")) {
            signedUrl = "/storage/v1" + signedUrl;
        }

        return storageBase + signedUrl;
    }

    public String resolveAccessUrl(String storedUrl) {
        if (storedUrl == null) return null;
        String value = storedUrl.trim();
        if (value.isEmpty()) return value;

        value = value.replace(".storage.storage.supabase.co", ".storage.supabase.co");
        value = value.replace("/storage/v1/s3/object/public/", "/storage/v1/object/public/");
        value = value.replace("/storage/v1/s3/object/", "/storage/v1/object/");

        if (!value.contains("/storage/v1/")) {
            return value;
        }

        String marker = "/storage/v1/object/public/";
        int idx = value.indexOf(marker);
        if (idx < 0) {
            marker = "/storage/v1/object/";
            idx = value.indexOf(marker);
        }
        if (idx < 0) {
            return value;
        }

        String suffix = value.substring(idx + marker.length());
        int slash = suffix.indexOf('/');
        if (slash <= 0 || slash >= suffix.length() - 1) {
            return value;
        }

        String bucketName = suffix.substring(0, slash);
        String encodedPath = suffix.substring(slash + 1);
        if (bucketName.isBlank() || encodedPath.isBlank()) {
            return value;
        }

        try {
            return createSupabaseSignedUrl(getStorageBaseUrl(), bucketName, encodedPath);
        } catch (Exception ex) {
            logger.warn("Failed to resolve signed URL for stored report, using original URL: {}", ex.getMessage());
            return value;
        }
    }

    private String getStorageBaseUrl() {
        if (supabaseUrl == null || supabaseUrl.isBlank()) return supabaseUrl;

        String base = supabaseUrl.trim();
        int storagePathIdx = base.indexOf("/storage/v1");
        if (storagePathIdx > -1) {
            base = base.substring(0, storagePathIdx);
        }
        base = base.replace(".storage.storage.supabase.co", ".storage.supabase.co");
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        if (base.contains(".storage.")) return base;
        if (base.endsWith(".supabase.co")) {
            return base.replaceFirst("\\.supabase\\.co$", ".storage.supabase.co");
        }
        return base;
    }
}
