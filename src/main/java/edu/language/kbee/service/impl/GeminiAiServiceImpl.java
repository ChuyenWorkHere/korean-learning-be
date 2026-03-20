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

        String systemPrompt = "Bạn là một chuyên gia ra đề thi TOPIK II phần Viết (Writing). " +
                "Hãy tạo một đề bài viết nghị luận (Argumentative essay) về chủ đề: " + topic + ". " +
                "TRẢ VỀ KẾT QUẢ DƯỚI DẠNG JSON CHUẨN (KHÔNG CÓ FORMAT MARKDOWN HAY CODE BLOCK), cấu trúc như sau: " +
                "{ \"title\": \"Tên chủ đề tiếng Anh\", \"instructions\": \"Yêu cầu đề bài bằng tiếng Hàn (kèm 3 câu hỏi gợi ý bằng tiếng Hàn ở phía dưới)\", \"minWords\": 200, \"maxWords\": 300, \"timeLimit\": 30, \"difficulty\": \"Intermediate (Level 3-4)\" }";

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", systemPrompt);

        Map<String, Object> parts = new HashMap<>();
        parts.put("parts", List.of(textPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(parts));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        String response = restTemplate.postForObject(url, request, String.class);

        JsonNode rootNode = objectMapper.readTree(response);
        String aiResponseText = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

        aiResponseText = cleanAiResponse(aiResponseText);

        return objectMapper.readTree(aiResponseText);
    }

    @Override
    public JsonNode gradeWritingSubmission(String studentContent, String taskType) throws Exception {
        String url = baseUrl + apiKey;

        // Xây dựng ngữ cảnh chấm điểm (Dịch câu hay Viết luận)
        String context = taskType.equalsIgnoreCase("ESSAY")
                ? "Evaluate this based on TOPIK II Essay criteria (formal endings like -는다/다, logical flow, advanced vocabulary, spacing)."
                : "Evaluate this based on standard translation accuracy, natural phrasing, and polite conversational forms (-아요/어요 or -습니다/ㅂ니다).";

        Map<String, Object> textPart = getStringObjectMap(studentContent, context);

        Map<String, Object> parts = new HashMap<>();
        parts.put("parts", List.of(textPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(parts));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        String response = restTemplate.postForObject(url, request, String.class);

        // Parse kết quả
        JsonNode rootNode = objectMapper.readTree(response);
        String aiResponseText = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

        // Làm sạch chuỗi JSON nếu Gemini lỡ thêm markdown
        aiResponseText = cleanAiResponse(aiResponseText);

        return objectMapper.readTree(aiResponseText);
    }

    private static Map<String, Object> getStringObjectMap(String studentContent, String context) {
        String systemPrompt = """
            You are a professional Korean language teacher. 
            Analyze the student's submission carefully. %s
            
            Student's submission: "%s"
            
            You MUST return the output strictly in the following JSON format WITHOUT ANY markdown wrappers (like ```json) or additional text.
            
            {
              "original": "exact student text",
              "userMeaning": "English translation of what the student wrote",
              "corrected": "Grammatically correct and natural Korean version",
              "aiMeaning": "English translation of the corrected version",
              "score": integer from 0 to 100,
              "performanceText": "Short encouraging phrase (e.g., 'Great Attempt!', 'Needs Work')",
              "nativeNuance": "A short explanation of how native Koreans would phrase this naturally",
              "analysis": "A brief overall comment on their writing",
              "breakdowns": [
                 {
                   "type": "politeness|grammar|vocabulary", 
                   "title": "Short title (e.g., 'Missing Particles')",
                   "severity": "Critical Error|Correction Needed|Minor Suggestion",
                   "description": "Detailed explanation of the mistake",
                   "tip": "A small tip for future practice",
                   "tags": ["Wrong: xyz", "Right: abc"] 
                 }
              ]
            }
            """.formatted(context, studentContent);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", systemPrompt);
        return textPart;
    }

    private String cleanAiResponse(String response) {
        return response.replaceAll("(?s)^```json\\s*", "").replaceAll("(?s)\\s*```$", "").trim();
    }
}
