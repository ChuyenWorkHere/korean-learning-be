package edu.language.kbee.service.impl;

import edu.language.kbee.service.GeminiAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiAiServiceImpl implements GeminiAiService {

    @Value("${gemini.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    @Override
    public JsonNode generateWritingPrompt(String topic) throws Exception {
        // Dùng model gemini-1.5-flash cho tốc độ siêu nhanh
        String url = baseUrl + apiKey;

        // 1. Tạo "Câu thần chú" (Prompt) ép AI trả về chuẩn định dạng JSON
        String systemPrompt = "Bạn là một chuyên gia ra đề thi TOPIK II phần Viết (Writing). " +
                "Hãy tạo một đề bài viết nghị luận (Argumentative essay) về chủ đề: " + topic + ". " +
                "TRẢ VỀ KẾT QUẢ DƯỚI DẠNG JSON CHUẨN (KHÔNG CÓ FORMAT MARKDOWN HAY CODE BLOCK), cấu trúc như sau: " +
                "{ \"title\": \"Tên chủ đề tiếng Anh\", \"instructions\": \"Yêu cầu đề bài bằng tiếng Hàn (kèm giải nghĩa tiếng Anh ngắn)\", \"minWords\": 200, \"maxWords\": 300, \"timeLimit\": 30, \"difficulty\": \"Intermediate (Level 3-4)\" }";

        // 2. Build body gửi cho Google
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", systemPrompt);

        Map<String, Object> parts = new HashMap<>();
        parts.put("parts", List.of(textPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(parts));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        // 3. Gọi API
        String response = restTemplate.postForObject(url, request, String.class);

        // 4. Bóc tách kết quả JSON trả về
        JsonNode rootNode = objectMapper.readTree(response);
        String aiResponseText = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

        // Xóa các tag markdown (như ```json và ```) nếu AI lỡ sinh ra
        aiResponseText = aiResponseText.replaceAll("(?s)^```json\\s*", "").replaceAll("(?s)\\s*```$", "").trim();

        // Chuyển chuỗi JSON của AI thành Object để trả về cho React
        return objectMapper.readTree(aiResponseText);
    }
}
