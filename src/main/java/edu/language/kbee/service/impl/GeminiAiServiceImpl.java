package edu.language.kbee.service.impl;

import edu.language.kbee.service.AiService;
import edu.language.kbee.service.ai.AiResponseParser;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "gemini")
@RequiredArgsConstructor
public class GeminiAiServiceImpl implements AiService {

    @Value("${gemini.api-key}")
    private String apiKey;

    public static Logger logger = LoggerFactory.getLogger(GeminiAiServiceImpl.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=";


    @Override
    public JsonNode generateWritingPrompt(String topic) throws Exception {
        // Dùng model gemini-3-flash cho tốc độ siêu nhanh
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
    public JsonNode generateTranslationPrompt(String topic) throws Exception {
        String url = baseUrl + apiKey;

        String systemPrompt = "Bạn là một chuyên gia ra đề luyện dịch thuật tiếng Hàn. " +
                "Hãy tạo một câu hỏi dịch thuật ngắn gọn về chủ đề: " + topic + ". " +
                "TRẢ VỀ KẾT QUẢ DƯỚI DẠNG JSON CHUẨN (KHÔNG CÓ FORMAT MARKDOWN HAY CODE BLOCK), cấu trúc như sau: " +
                "{\n" +
                "  \"english\": \"Câu gốc bằng tiếng Việt\",\n" +
                "  \"korean\": \"Câu dịch chuẩn nhất bằng tiếng Hàn\",\n" +
                "  \"variations\": [\"Bản dịch tiếng Hàn chấp nhận được 1\", \"Bản dịch tiếng Hàn chấp nhận được 2\"]\n" +
                "}";

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

        // Xây dựng ngữ cảnh chấm điểm
        String context = taskType.equalsIgnoreCase("ESSAY")
                ? """
                  Đánh giá dựa trên tiêu chuẩn bài thi TOPIK II phần Viết. BẮT BUỘC chấm điểm theo 4 tiêu chí sau:
                  (1) Nội dung và hoàn thành nhiệm vụ: Trả lời đúng yêu cầu đề bài, lập luận thuyết phục.
                  (2) Tổ chức và mạch lạc: Cấu trúc 3 phần (Mở bài, Thân bài, Kết bài) rõ ràng, các đoạn liên kết logic.
                  (3) Sử dụng từ vựng: Từ vựng đa dạng, phù hợp trình độ cao cấp.
                  (4) Chính xác ngữ pháp: Ngữ pháp chính xác, sử dụng được cấu trúc nâng cao.
                  
                  YÊU CẦU VỀ CẤU TRÚC (KHUYẾN NGHỊ):
                  - Mở bài (서론): nên có, giới thiệu chủ đề và nêu quan điểm.
                  - Thân bài (본론): nên trình bày 2-3 luận điểm với dẫn chứng cụ thể. (Điểm cộng lớn nếu trình bày được mặt đối lập rồi phản bác).
                  - Kết bài (결론): nên có, tóm tắt quan điểm và kết luận.
                  - CHÚ Ý QUAN TRỌNG VỀ ĐỘ DÀI: Hãy đếm TỔNG số lượng ký tự của toàn bộ bài viết (không tính khoảng trắng). KHÔNG trừ điểm nếu các phần (Mở bài/Thân bài/Kết bài) ngắn hơn mức chuẩn mực. CHỈ TRỪ ĐIỂM NẶNG khi TỔNG SỐ KÝ TỰ CỦA CẢ BÀI VIẾT không nằm trong khoảng "Yêu cầu độ dài" được quy định ở bên dưới (từ minWords đến maxWords).
                  
                  PHẦN NHẬN XÉT (Hãy trình bày thật chi tiết vào trường 'analysis' của file JSON):
                  - Cung cấp điểm số cho từng tiêu chí trong 4 tiêu chí trên.
                  - Nhận xét cụ thể về điểm mạnh và điểm yếu.
                  - Gợi ý cách cải thiện.
                  - Cuối phần nhận xét, BẮT BUỘC thêm dòng: "Quá trình viết - nhận phản hồi - sửa - viết lại này mô phỏng việc học với giáo viên riêng, giúp bạn tiến bộ nhanh hơn nhiều so với luyện viết một mình."
                  """
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

    @Override
    public JsonNode generateQuizzes(String context, String skillType) {
        String url = baseUrl + apiKey;

        String lowerSkillType = skillType == null ? "general" : skillType.toLowerCase();
        String quizInstruction;
        switch (lowerSkillType) {
            case "grammar":
                quizInstruction = """
                    Bạn là chuyên gia giảng dạy tiếng Hàn. Dựa vào phần lý thuyết ngữ pháp sau, hãy tạo chính xác 3 câu hỏi trắc nghiệm tập trung vào DỊCH Việt-Hàn hoặc Hàn-Việt.
                    YÊU CẦU RIÊNG CHO QUIZ NGỮ PHÁP:
                    - Mỗi câu phải kiểm tra việc dùng đúng cấu trúc ngữ pháp trong ngữ cảnh dịch.
                    - Ưu tiên dạng: "Câu nào dịch đúng câu tiếng Việt sau sang tiếng Hàn?" hoặc "Câu tiếng Hàn sau nghĩa là gì?".
                    - Không hỏi định nghĩa lan man, không hỏi mẹo lý thuyết thuần túy nếu không gắn với câu dịch.
                    - Các đáp án sai phải là lỗi thường gặp về chia đuôi, tiểu từ, trật tự từ, mức lịch sự hoặc dùng sai cấu trúc.
                    - Giải thích phải chỉ rõ vì sao bản dịch đúng và lỗi của các lựa chọn gây nhiễu.
                    NỘI DUNG NGỮ PHÁP:
                    """ + context;
                break;
            case "listening":
                quizInstruction = "Bạn là chuyên gia giảng dạy tiếng Hàn. Dựa trực tiếp vào transcript của bài nghe sau, hãy tạo chính xác 3 câu hỏi trắc nghiệm kiểm tra hiểu nội dung, ý chính, chi tiết nghe được hoặc suy luận gần từ transcript. TRANSCRIPT:\n" + context;
                break;
            case "reading":
                quizInstruction = "Bạn là chuyên gia giảng dạy tiếng Hàn. Dựa trực tiếp vào đoạn đọc sau, hãy tạo chính xác 3 câu hỏi trắc nghiệm kiểm tra hiểu nội dung, ý chính, chi tiết, từ vựng trong ngữ cảnh hoặc suy luận từ bài đọc. ĐOẠN ĐỌC:\n" + context;
                break;
            default:
                quizInstruction = "Bạn là chuyên gia giảng dạy tiếng Hàn. Dựa trực tiếp vào nội dung bài học sau, hãy tạo chính xác 3 câu hỏi trắc nghiệm kiểm tra nội dung trọng tâm của bài. Không tạo câu hỏi ngoài phạm vi nội dung đã cho. NỘI DUNG:\n" + context;
        }

        String systemPrompt = quizInstruction +
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

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", systemPrompt);

        Map<String, Object> parts = new HashMap<>();
        parts.put("parts", List.of(textPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(parts));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            String response = restTemplate.postForObject(url, request, String.class);
            JsonNode rootNode = objectMapper.readTree(response);
            String aiResponseText = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            aiResponseText = cleanAiResponse(aiResponseText);
            return objectMapper.readTree(aiResponseText);
        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Object> getStringObjectMap(String studentContent, String context) {
        String systemPrompt = """
            Bạn là một giáo viên dạy tiếng Hàn chuyên nghiệp. 
            Hãy phân tích bài làm của học sinh một cách cẩn thận. %s
            
            QUY TẮC CHẤM ĐIỂM NGHIÊM NGẶT:
            - Chấm điểm khắt khe! Những bài viết ở mức trung bình và có lỗi chỉ nên đạt từ 50-70 điểm.
            - Chỉ cho mức 90-100 điểm với bài viết xuất sắc, trôi chảy như người bản xứ, ngữ pháp hoàn hảo và trả lời đúng trọng tâm.
            - Nếu bài làm của học sinh chỉ đơn thuần sao chép lại đề bài/yêu cầu mà không tự trả lời, vô nghĩa, hoặc hoàn toàn lạc đề, sử dụng ngôn ngữ khác tiếng Hàn, BẠN BẮT BUỘC PHẢI CHẤM 0 ĐIỂM.
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
              "analysis": "Nhận xét tổng quan hoặc phân tích chi tiết (điểm từng tiêu chí, điểm mạnh/yếu, gợi ý) tùy theo yêu cầu của từng loại bài (Bằng Tiếng Việt)",
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

    @Override
    public JsonNode splitSubtitleWithAI(String rawSubtitleText) throws Exception {
        String url = baseUrl + apiKey;

        String systemPrompt = """
            Bạn là chuyên gia ngôn ngữ tiếng Hàn. Tôi sẽ cung cấp cho bạn một đoạn transcript tiếng Hàn (phụ đề từ YouTube).
            
            NHIỆM VỤ: Hãy chia đoạn transcript này thành các câu/cụm từ có ý nghĩa hoàn chỉnh, phù hợp cho bài tập nghe chép chính tả (dictation).
            
            QUY TẮC CHIA CÂU:
            1. CHỈ tách câu khi gặp dấu kết thúc câu rõ ràng (như dấu chấm '.', dấu hỏi chấm '?', dấu chấm than '!', dấu ngã '~' hoặc các đuôi câu kết thúc rõ ràng).
            2. KHÔNG ĐƯỢC tách lắt nhắt thành các cụm từ ngắn. Các câu dài đến đâu cũng kệ, cứ giữ nguyên thành 1 câu hoàn chỉnh.
            3. TUYỆT ĐỐI KHÔNG sửa lỗi chính tả, KHÔNG thêm bớt từ. Phải giữ nguyên 100% text gốc. Nếu đoạn text bị cắt dở, cứ giữ nguyên.
            4. Giữ nguyên thứ tự xuất hiện trong bản gốc.
            
            TRẢ VỀ KẾT QUẢ DƯỚI DẠNG JSON OBJECT CHUẨN (KHÔNG CÓ MARKDOWN), cấu trúc như sau:
            {
              "sentences": [
                "câu 1 hoàn chỉnh", 
                "câu 2 hoàn chỉnh", 
                "câu 3 hoàn chỉnh"
              ]
            }
            
            TRANSCRIPT:
            """ + rawSubtitleText;

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
    public JsonNode extractVocabulary(String content) throws Exception {
        String url = baseUrl + apiKey;

        String systemPrompt = "Bạn là một chuyên gia ngôn ngữ tiếng Hàn. Tôi sẽ cung cấp cho bạn một đoạn văn bản tiếng Hàn (bài đọc/truyện). " +
                "NHIỆM VỤ: Hãy phân tích đoạn văn bản và trích xuất ra các từ vựng quan trọng, cụm từ hay, và biểu đạt đáng lưu ý mà người học tiếng Hàn nên biết. " +
                "QUY TẮC: " +
                "1. Trích xuất từ 5 đến 15 từ/cụm từ (tùy độ dài văn bản). " +
                "2. Ưu tiên: từ vựng trung cấp-cao cấp, thành ngữ, cụm từ cố định, biểu đạt hay, ngữ pháp đặc biệt. " +
                "3. Bỏ qua các từ quá cơ bản mà ai cũng biết (ví dụ: 나, 그, 이, 은/는, 이다...). " +
                "4. Với mỗi mục, cung cấp nghĩa tiếng Việt rõ ràng. " +
                "5. Nếu văn bản quá ngắn hoặc không có nội dung tiếng Hàn, trả về mảng rỗng. " +
                "TRẢ VỀ KẾT QUẢ DƯỚI DẠNG JSON OBJECT CHUẨN (KHÔNG CÓ MARKDOWN HAY CODE BLOCK), cấu trúc: " +
                "{ \"vocabulary\": [ { \"word\": \"từ/cụm từ tiếng Hàn\", \"meaning\": \"nghĩa tiếng Việt\", \"type\": \"noun|verb|adjective|adverb|phrase|idiom|grammar\" } ] } " +
                "VĂN BẢN CẦN PHÂN TÍCH: " + content;

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
    public JsonNode generateGrammarLesson(String grammarPoint, String level) throws Exception {
        String url = baseUrl + apiKey;

        String systemPrompt = """
            Bạn là giáo viên tiếng Hàn chuyên thiết kế bài học ngữ pháp cho người Việt.
            Tạo một bài học ngữ pháp tiếng Hàn ngắn gọn, đúng trọng tâm.

            CHỦ ĐIỂM NGỮ PHÁP: %s
            TRÌNH ĐỘ: %s

            YÊU CẦU NỘI DUNG:
            - Không lan man, không nói kiến thức ngoài chủ điểm.
            - Giải thích bằng tiếng Việt dễ hiểu, phù hợp trình độ.
            - Nếu có tiếng Hàn, phải tự nhiên và đúng ngữ pháp.
            - Phần mô tả chỉ 1 đoạn 2-4 câu.
            - Phần lưu ý có 3-5 ý, mỗi ý 1 câu ngắn.
            - Phần ví dụ có đúng 3 dòng, mỗi dòng gồm từ gốc/cụm gốc, câu tiếng Hàn, nghĩa tiếng Việt.
            - Phần quizzes có đúng 3 câu hỏi trắc nghiệm luyện dịch Việt-Hàn hoặc Hàn-Việt, tập trung vào đúng chủ điểm ngữ pháp.
            - Mỗi câu quiz có đúng 4 lựa chọn, đúng 1 lựa chọn isCorrect=true, giải thích ngắn bằng tiếng Việt.
            - Bắt buộc đảo vị trí đáp án đúng: câu 1 không đặt đáp án đúng ở lựa chọn đầu tiên, câu 2 và câu 3 phải có đáp án đúng ở vị trí khác nhau.
            - Không dùng HTML, markdown, bullet ký tự đặc biệt hoặc đánh số trong chuỗi.

            CHỈ TRẢ VỀ JSON OBJECT THUẦN TÚY theo cấu trúc:
            {
              "title": "Tên bài học ngắn gọn, ví dụ: 입니다",
              "description": "Đoạn tổng quan lý thuyết bằng tiếng Việt.",
              "structure": "Công thức ngắn, ví dụ: N + 입니다",
              "meaning": "Ý nghĩa ngắn, ví dụ: Là ... / Tôi là ... / Đây là ...",
              "usageNotes": [
                "Lưu ý 1",
                "Lưu ý 2",
                "Lưu ý 3"
              ],
              "examples": [
                {
                  "base": "학생",
                  "sentence": "저는 학생입니다.",
                  "meaning": "Tôi là học sinh."
                }
              ],
              "quizzes": [
                {
                  "question": "Câu nào dịch đúng câu tiếng Việt sau sang tiếng Hàn?",
                  "explanation": "Giải thích ngắn gọn vì sao đáp án đúng.",
                  "options": [
                    { "text": "Lựa chọn sai 1", "isCorrect": false },
                    { "text": "Lựa chọn đúng", "isCorrect": true },
                    { "text": "Lựa chọn sai 2", "isCorrect": false },
                    { "text": "Lựa chọn sai 3", "isCorrect": false }
                  ]
                },
                {
                  "question": "Câu tiếng Hàn sau có nghĩa là gì?",
                  "explanation": "Giải thích ngắn gọn vì sao đáp án đúng.",
                  "options": [
                    { "text": "Lựa chọn sai 1", "isCorrect": false },
                    { "text": "Lựa chọn sai 2", "isCorrect": false },
                    { "text": "Lựa chọn sai 3", "isCorrect": false },
                    { "text": "Lựa chọn đúng", "isCorrect": true }
                  ]
                },
                {
                  "question": "Chọn câu dùng đúng cấu trúc ngữ pháp.",
                  "explanation": "Giải thích ngắn gọn vì sao đáp án đúng.",
                  "options": [
                    { "text": "Lựa chọn sai 1", "isCorrect": false },
                    { "text": "Lựa chọn sai 2", "isCorrect": false },
                    { "text": "Lựa chọn đúng", "isCorrect": true },
                    { "text": "Lựa chọn sai 3", "isCorrect": false }
                  ]
                }
              ]
            }
            """.formatted(grammarPoint, level);

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

    private String cleanAiResponse(String response) {
        return AiResponseParser.extractJson(response);
    }
}
