package com.healthcare.patient.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String serviceKey;

    @Value("${supabase.bucket}")
    private String bucket;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String uploadPatientDocument(MultipartFile file, String patientId) throws Exception {
        String path = "patients/" + patientId + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        // Encode each path segment, but keep slashes
        String encodedPath = Arrays.stream(path.split("/"))
                .map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));

        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + encodedPath;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Authorization", "Bearer " + serviceKey)
                .header("apikey", serviceKey)
                .header("x-upsert", "true")
                .header("Content-Type", file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            // Public URL pattern for Supabase storage
            return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + encodedPath;
        } else {
            throw new RuntimeException("Supabase upload failed: " + response.statusCode() + " " + response.body());
        }
    }
}
