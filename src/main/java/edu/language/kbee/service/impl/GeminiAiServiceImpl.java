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
    public JsonNode gradeWritingSubmission(String studentContent, String taskType, String blockContext) throws Exception {
        String url = baseUrl + apiKey;

        // Xây dựng ngữ cảnh chấm điểm (Dịch câu hay Viết luận)
        String context = taskType.equalsIgnoreCase("ESSAY")
                ? "Đánh giá dựa trên tiêu chuẩn bài thi TOPIK II phần Viết (sử dụng đuôi câu trang trọng -는다/다, mạch văn logic, từ vựng cao cấp, cách giãn cách từ chính xác)."
                : "Đánh giá dựa trên tiêu chuẩn dịch thuật chính xác, cách diễn đạt tự nhiên và sử dụng đúng văn phong lịch sự (-아요/어요 hoặc -습니다/ㅂ니다).";

        if (blockContext != null && !blockContext.isBlank()) {
             context += "\n\nYêu cầu của đề bài mà học sinh BẮT BUỘC phải tuân thủ:\n" + blockContext;
        }

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
            Bạn là một giáo viên dạy tiếng Hàn chuyên nghiệp. 
            Hãy phân tích bài làm của học sinh một cách cẩn thận. %s
            
            QUY TẮC CHẤM ĐIỂM NGHIÊM NGẶT:
            - Chấm điểm khắt khe! Những bài viết ở mức trung bình và có lỗi chỉ nên đạt từ 50-70 điểm.
            - Chỉ cho mức 90-100 điểm với bài viết xuất sắc, trôi chảy như người bản xứ, ngữ pháp hoàn hảo và trả lời đúng trọng tâm.
            - Nếu bài làm của học sinh chỉ đơn thuần sao chép lại đề bài/yêu cầu mà không tự trả lời, vô nghĩa, hoặc hoàn toàn lạc đề, BẠN BẮT BUỘC PHẢI CHẤM 0 ĐIỂM.
            
            Bài làm của học sinh: "%s"
            
            BẠN BẮT BUỘC PHẢI TRẢ VỀ ĐẦU RA BẰNG ĐỊNH DẠNG JSON NHƯ THEO YÊU CẦU DƯỚI ĐÂY. 
            TOÀN BỘ NỘI DUNG VĂN BẢN (DỊCH NGHĨA, NHẬN XÉT, GIẢI THÍCH, LỜI KHUYÊN) PHẢI ĐƯỢC VIẾT BẰNG TIẾNG VIỆT (Ngoại trừ trường 'type', 'severity', và đoạn văn bản trích dẫn tiếng Hàn). 
            KHÔNG DÙNG thẻ markdown (như ```json) hay giải thích gì thêm ở ngoài chuỗi JSON.
            
            {
              "original": "Văn bản gốc tiếng Hàn của học sinh (Giữ nguyên tiếng Hàn)",
              "userMeaning": "Dịch ý nghĩa chính xác từ văn bản gốc của học sinh sang tiếng Việt",
              "corrected": "Phiên bản tiếng Hàn đã được sửa lỗi ngữ pháp và diễn đạt tự nhiên hơn (Giữ nguyên tiếng Hàn)",
              "aiMeaning": "Bản dịch nghĩa nội dung đã sửa sang tiếng Việt",
              "score": Điểm số (số nguyên từ 0 đến 100),
              "performanceText": "Câu khích lệ ngắn gọn bằng Tiếng Việt (Ví dụ: 'Cố gắng lên!', 'Làm tốt lắm!')",
              "nativeNuance": "Giải thích ngắn gọn cách người Hàn bản xứ diễn đạt tự ngữ này tự nhiên hơn ra sao (Bằng Tiếng Việt)",
              "analysis": "Nhận xét tổng quan ngắn gọn về bài trình bày (Bằng Tiếng Việt)",
              "breakdowns": [
                 {
                   "type": "politeness|grammar|vocabulary", 
                   "title": "Tiêu đề ngắn gọn về lỗi (Bằng Tiếng Việt, vd: 'Thiếu tiểu từ', 'Sai ngữ pháp')",
                   "severity": "Critical Error|Correction Needed|Minor Suggestion",
                   "description": "Giải thích chi tiết nguyên nhân lỗi và cách sửa chuẩn (Bằng Tiếng Việt)",
                   "tip": "Mẹo nhỏ để luyện tập hoặc phân biệt (Bằng Tiếng Việt)",
                   "tags": ["Sai: xyz", "Đúng: abc"] 
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
