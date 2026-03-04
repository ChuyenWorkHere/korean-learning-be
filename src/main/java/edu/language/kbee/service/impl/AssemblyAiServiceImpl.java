package edu.language.kbee.service.impl;

import edu.language.kbee.service.AssemblyAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssemblyAiServiceImpl implements AssemblyAiService {

    @Value("${assemblyai.api-key}")
    private String apiKey;

    private final String baseUrl = "https://api.assemblyai.com/v2/transcript";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public JsonNode generateTranscript(String audioUrl) throws Exception {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("audio_url", audioUrl);
        requestBody.put("language_code", "ko");
        requestBody.put("speaker_labels", true);
        requestBody.put("speech_models", List.of("universal-2"));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

        JsonNode rootNode = objectMapper.readTree(response.getBody());
        String transcriptId = rootNode.get("id").asText();

        String status = "queued";
        while (status.equals("queued") || status.equals("processing")) {
            Thread.sleep(3000); // Chờ 3 giây rồi hỏi lại để tránh spam API

            HttpEntity<String> pollRequest = new HttpEntity<>(headers);
            ResponseEntity<String> pollResponse = restTemplate.exchange(
                    baseUrl + "/" + transcriptId, HttpMethod.GET, pollRequest, String.class);

            JsonNode pollNode = objectMapper.readTree(pollResponse.getBody());
            status = pollNode.get("status").asText();

            if (status.equals("completed")) {
                return pollNode;
            } else if (status.equals("error")) {
                throw new Exception("AssemblyAI Error: " + pollNode.get("error").asText());
            }
        }
        return null;
    }
}
