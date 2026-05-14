package edu.language.kbee.service.impl;

import edu.language.kbee.service.AiService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class OpenRouterAiServiceImpl implements AiService {

    @Value("${openrouter.api-key}")
    private String apiKey;

    @Value("${openrouter.model}")
    private String modelName;

    public static final Logger logger = LoggerFactory.getLogger(OpenRouterAiServiceImpl.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl = "https://openrouter.ai/api/v1/chat/completions";

    @Override
    public JsonNode generateWritingPrompt(String topic) throws Exception {
        String systemMsg = "Bạn là một chuyên gia ra đề thi TOPIK II phần Viết (Writing). " +
                "CHỈ trả về JSON thuần túy, KHÔNG có markdown, KHÔNG có giải thích thêm.";
        String userMsg = "Hãy tạo một đề bài viết nghị luận (Argumentative essay) về chủ đề: " + topic + ". " +
                "TRẢ VỀ KẾT QUẢ DƯỚI DẠNG JSON CHUẨN (KHÔNG CÓ FORMAT MARKDOWN HAY CODE BLOCK), cấu trúc như sau: " +
                "{ \"title\": \"Tên chủ đề tiếng Anh\", \"instructions\": \"Yêu cầu đề bài bằng tiếng Hàn (kèm 3 câu hỏi gợi ý bằng tiếng Hàn ở phía dưới)\", \"minWords\": 200, \"maxWords\": 300, \"timeLimit\": 30, \"difficulty\": \"Intermediate (Level 3-4)\" }";

        return callOpenRouterApi(systemMsg, userMsg);
    }

    @Override
    public JsonNode generateTranslationPrompt(String topic) throws Exception {
        String systemMsg = "Bạn là một chuyên gia ra đề luyện dịch thuật tiếng Hàn. " +
                "CHỈ trả về JSON thuần túy, KHÔNG có markdown, KHÔNG có giải thích thêm.";
        String userMsg = "Hãy tạo một câu hỏi dịch thuật ngắn gọn về chủ đề: " + topic + ". " +
                "TRẢ VỀ KẾT QUẢ DƯỚI DẠNG JSON CHUẨN (KHÔNG CÓ FORMAT MARKDOWN HAY CODE BLOCK), cấu trúc như sau: " +
                "{\n" +
                "  \"english\": \"Câu gốc bằng tiếng Việt\",\n" +
                "  \"korean\": \"Câu dịch chuẩn nhất bằng tiếng Hàn\",\n" +
                "  \"variations\": [\"Bản dịch tiếng Hàn chấp nhận được 1\", \"Bản dịch tiếng Hàn chấp nhận được 2\"]\n" +
                "}";

        return callOpenRouterApi(systemMsg, userMsg);
    }

    @Override
    public JsonNode gradeWritingSubmission(String studentContent, String taskType, String blockContext) throws Exception {
        String context = taskType.equalsIgnoreCase("ESSAY")
                ? "Đánh giá dựa trên tiêu chuẩn bài thi TOPIK II phần Viết (sử dụng đuôi câu trang trọng -는다/다, mạch văn logic, từ vựng cao cấp, cách giãn cách từ chính xác)."
                : "Đánh giá dựa trên tiêu chuẩn dịch thuật chính xác, cách diễn đạt tự nhiên và sử dụng đúng văn phong lịch sự (-아요/어요 hoặc -습니다/ㅂ니다).";

        if (blockContext != null && !blockContext.isBlank()) {
            context += "\n\nYêu cầu của đề bài mà học sinh BẮT BUỘC phải tuân thủ:\n" + blockContext;
        }

        String systemMsg = "Bạn là một giáo viên dạy tiếng Hàn chuyên nghiệp. CHỈ trả về JSON thuần túy, KHÔNG có markdown, KHÔNG có giải thích thêm.";
        String userMsg = """
            Hãy phân tích bài làm của học sinh một cách cẩn thận. %s
            
            QUY TẮC CHẤM ĐIỂM NGHIÊM NGẶT:
            - Chấm điểm khắt khe! Những bài viết ở mức trung bình và có lỗi chỉ nên đạt từ 50-70 điểm.
            - Chỉ cho mức 90-100 điểm với bài viết xuất sắc, trôi chảy như người bản xứ, ngữ pháp hoàn hảo và trả lời đúng trọng tâm.
            - Nếu bài làm của học sinh chỉ đơn thuần sao chép lại đề bài/yêu cầu mà không tự trả lời, vô nghĩa, hoặc hoàn toàn lạc đề, BẠN BẮT BUỘC PHẢI CHẤM 0 ĐIỂM.
            - Ở trường 'corrected', BẮT BUỘC phải cung cấp một phiên bản hay hơn, cao cấp hơn và tự nhiên hơn (sử dụng từ vựng/ngữ pháp bản xứ), tuyệt đối KHÔNG ĐƯỢC trả về chuỗi giống hệt với bản gốc kể cả khi học sinh viết đúng.
            
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

        return callOpenRouterApi(systemMsg, userMsg);
    }

    @Override
    public JsonNode generateQuizzes(String context, String skillType) {
        String promptContext;
        switch (skillType.toLowerCase()) {
            case "grammar":
                promptContext = "phần lý thuyết ngữ pháp sau: " + context;
                break;
            case "listening":
                promptContext = "transcript của bài nghe sau: " + context;
                break;
            case "reading":
                promptContext = "đoạn văn bản sau: " + context;
                break;
            default:
                promptContext = "nội dung sau: " + context;
        }

        String systemMsg = "Bạn là chuyên gia giảng dạy tiếng Hàn. CHỈ trả về JSON Array thuần túy, KHÔNG có markdown, KHÔNG có giải thích thêm.";
        String userMsg = "Dựa vào " + promptContext + ", " +
                "hãy tạo chính xác 3 câu hỏi trắc nghiệm tiếng Hàn/tiếng Việt." +
                "TRẢ VỀ DUY NHẤT MỘT MẢNG JSON ARRAY CHỨA ĐÚNG 3 OBJECT (KHÔNG DÙNG FORMAT MARKDOWN), MỖI OBJECT CÓ CẤU TRÚC SAU:\n" +
                "{\n" +
                "  \"question\": \"Nội dung câu hỏi?\",\n" +
                "  \"explanation\": \"Giải thích ngắn gọn lý do chọn đáp án đúng bằng tiếng Việt.\",\n" +
                "  \"options\": [\n" +
                "    { \"text\": \"Lựa chọn 1\", \"isCorrect\": true },\n" +
                "    { \"text\": \"Lựa chọn 2\", \"isCorrect\": false },\n" +
                "    { \"text\": \"Lựa chọn 3\", \"isCorrect\": false },\n" +
                "    { \"text\": \"Lựa chọn 4\", \"isCorrect\": false }\n" +
                "  ]\n" +
                "}\n" +
                "Hãy trộn ngẫu nhiên vị trí đáp án đúng (không phải lúc nào cũng ở vị trí đầu tiên).";

        try {
            return callOpenRouterApi(systemMsg, userMsg);
        } catch (Exception e) {
            logger.error("Lỗi tạo quiz: {}", e.getMessage());
            throw new RuntimeException("Lỗi tạo quiz: " + e.getMessage());
        }
    }

    @Override
    public JsonNode splitSubtitleWithAI(String rawSubtitleText) throws Exception {
        String systemMsg = "Bạn là chuyên gia ngôn ngữ tiếng Hàn. CHỈ trả về JSON Array thuần túy, KHÔNG có markdown, KHÔNG có giải thích thêm.";
        String userMsg = """
            Tôi cung cấp cho bạn một đoạn transcript tiếng Hàn (phụ đề từ YouTube).
            
            NHIỆM VỤ: Hãy chia đoạn transcript này thành các câu/cụm từ có ý nghĩa hoàn chỉnh, phù hợp cho bài tập nghe chép chính tả (dictation).
            
            QUY TẮC CHIA CÂU:
            1. Mỗi câu/cụm từ phải có ý nghĩa hoàn chỉnh.
            2. Mỗi câu nên dài hơn một chút: khoảng 15-30 âm tiết / ký tự tiếng Hàn (tương đương 5-12 từ/eojeol). Tránh chia lắt nhắt quá ngắn.
            3. Câu dài phải được tách thành các cụm theo ranh giới tự nhiên (sau trợ từ, liên từ -고/-면/-서/-는데, hoặc dấu phẩy).
            4. TUYỆT ĐỐI KHÔNG sửa lỗi chính tả, KHÔNG thêm bớt từ. Phải giữ nguyên 100% text gốc. Nếu đoạn text bị cắt dở, cứ giữ nguyên.
            5. Giữ nguyên thứ tự xuất hiện trong bản gốc.
            
            TRẢ VỀ KẾT QUẢ DƯỚI DẠNG JSON OBJECT, cấu trúc như sau:
            {
              "sentences": [
                "câu 1", 
                "câu 2", 
                "câu 3"
              ]
            }
            
            TRANSCRIPT:
            """ + rawSubtitleText;

        return callOpenRouterApi(systemMsg, userMsg);
    }

    @Override
    public JsonNode extractVocabulary(String content) throws Exception {
        String systemMsg = "Bạn là một chuyên gia ngôn ngữ tiếng Hàn. CHỈ trả về JSON thuần túy, KHÔNG có markdown, KHÔNG có giải thích thêm.";
        String userMsg = "Hãy phân tích đoạn văn bản tiếng Hàn dưới đây và trích xuất ra các từ vựng quan trọng, cụm từ hay, và biểu đạt đáng lưu ý mà người học tiếng Hàn nên biết. " +
                "QUY TẮC: " +
                "1. Trích xuất từ 5 đến 15 từ/cụm từ (tùy độ dài văn bản). " +
                "2. Ưu tiên: từ vựng trung cấp-cao cấp, thành ngữ, cụm từ cố định, biểu đạt hay, ngữ pháp đặc biệt. " +
                "3. Bỏ qua các từ quá cơ bản mà ai cũng biết (ví dụ: 나, 그, 이, 은/는, 이다...). " +
                "4. Với mỗi mục, cung cấp nghĩa tiếng Việt rõ ràng. " +
                "5. Nếu văn bản quá ngắn hoặc không có nội dung tiếng Hàn, trả về mảng rỗng. " +
                "TRẢ VỀ KẾT QUẢ DƯỚI DẠNG JSON OBJECT CHUẨN, cấu trúc: " +
                "{ \"vocabulary\": [ { \"word\": \"từ/cụm từ tiếng Hàn\", \"meaning\": \"nghĩa tiếng Việt\", \"type\": \"noun|verb|adjective|adverb|phrase|idiom|grammar\" } ] } " +
                "VĂN BẢN CẦN PHÂN TÍCH: " + content;

        return callOpenRouterApi(systemMsg, userMsg);
    }

    private JsonNode callOpenRouterApi(String systemMsg, String userMsg) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey.trim());
        headers.set("HTTP-Referer", "http://localhost:8080");
        headers.set("X-Title", "K-Bee App");

        // Cấu trúc messages chuẩn: System trước, User sau
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemMsg));
        messages.add(Map.of("role", "user", "content", userMsg));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("messages", messages);

        // QUAN TRỌNG: Giảm temperature để AI không viết thêm văn bản rác
        requestBody.put("temperature", 0.2);
        requestBody.put("top_p", 0.9);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            String response = restTemplate.postForObject(baseUrl, entity, String.class);
            JsonNode rootNode = objectMapper.readTree(response);

            if (rootNode.has("error")) {
                throw new RuntimeException("OpenRouter Error: " + rootNode.path("error").path("message").asText());
            }

            String aiContent = rootNode.path("choices").get(0).path("message").path("content").asText();
            aiContent = cleanAiResponse(aiContent);

            return objectMapper.readTree(aiContent);
        } catch (Exception e) {
            logger.error("API Call failed: {}", e.getMessage());
            throw e;
        }
    }

    private String cleanAiResponse(String response) {
        if (response == null) return "{}";
        // Loại bỏ markdown và các đoạn văn bản thừa không phải JSON
        String cleaned = response.trim();
        if (cleaned.contains("```json")) {
            cleaned = cleaned.substring(cleaned.indexOf("```json") + 7);
            cleaned = cleaned.substring(0, cleaned.lastIndexOf("```"));
        } else if (cleaned.contains("```")) {
            cleaned = cleaned.substring(cleaned.indexOf("```") + 3);
            cleaned = cleaned.substring(0, cleaned.lastIndexOf("```"));
        }

        // Tìm vị trí bắt đầu và kết thúc của JSON thực tế ({ hoặc [)
        int firstBrace = cleaned.indexOf('{');
        int firstBracket = cleaned.indexOf('[');
        int start = (firstBrace != -1 && (firstBracket == -1 || firstBrace < firstBracket)) ? firstBrace : firstBracket;

        int lastBrace = cleaned.lastIndexOf('}');
        int lastBracket = cleaned.lastIndexOf(']');
        int end = (lastBrace > lastBracket) ? lastBrace : lastBracket;

        if (start != -1 && end != -1 && end > start) {
            return cleaned.substring(start, end + 1);
        }

        return cleaned.trim();
    }
}