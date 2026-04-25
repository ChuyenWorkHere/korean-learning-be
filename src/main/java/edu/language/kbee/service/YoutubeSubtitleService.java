package edu.language.kbee.service;

import edu.language.kbee.payload.DictationSentenceDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import tools.jackson.databind.JsonNode;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class YoutubeSubtitleService {

    private final AiService aiService;

    public YoutubeSubtitleService(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * Dùng yt-dlp để tải phụ đề từ YouTube, sau đó dùng AI (Gemini) để chia câu thông minh.
     */
    public List<DictationSentenceDto> extractSubtitles(String youtubeId) throws Exception {
        Path tempDir = Files.createTempDirectory("yt_sub_");
        String outputTemplate = tempDir.resolve("sub_" + UUID.randomUUID().toString().substring(0, 8)).toString();

        try {
            String ytDlpPath = findYtDlpPath();

            // Bước 1: Tải phụ đề (ưu tiên ko > en > any)
            String subFile = downloadSubtitle(ytDlpPath, youtubeId, outputTemplate, "ko,en");
            if (subFile == null) {
                subFile = downloadSubtitle(ytDlpPath, youtubeId, outputTemplate, "all");
            }
            if (subFile == null) {
                throw new Exception("Video này không có phụ đề (CC) nào.");
            }

            // Bước 2: Parse XML thành danh sách câu thô (kèm thời gian)
            String xmlContent = Files.readString(Path.of(subFile), StandardCharsets.UTF_8);
            List<RawSegment> rawSegments = parseXmlToRawSegments(xmlContent);

            if (rawSegments.isEmpty()) {
                throw new Exception("File phụ đề trống.");
            }

            // Bước 3: Ghép toàn bộ text gửi cho AI chia câu
            // Chia nhỏ thành các đoạn (chunk) để tránh lỗi API do độ dài quá mức.
            // Giảm xuống 800 ký tự để tránh AI bị cắt cụt output (max_tokens).
            List<String> allAiSentences = new ArrayList<>();
            StringBuilder chunkText = new StringBuilder();

            for (RawSegment seg : rawSegments) {
                // Kiểm tra xem việc thêm segment này có làm chunk vượt quá độ dài tối đa không
                if (chunkText.length() + seg.text.length() > 800) {
                    if (chunkText.length() > 0) {
                        try {
                            JsonNode aiResult = aiService.splitSubtitleWithAI(chunkText.toString().trim());
                            if (aiResult != null && aiResult.has("sentences") && aiResult.get("sentences").isArray()) {
                                for (JsonNode node : aiResult.get("sentences")) {
                                    allAiSentences.add(node.asText());
                                }
                            } else {
                                log.warn("AI didn't return an array: {}", aiResult);
                            }
                        } catch (Exception e) {
                            log.error("AI split failed for chunk. Falling back to raw text.", e);
                            allAiSentences.add(chunkText.toString().trim());
                        }
                        chunkText.setLength(0); // Reset chunk
                    }
                }
                chunkText.append(seg.text).append(" ");
            }

            // Gửi chunk cuối cùng (nếu còn)
            if (chunkText.length() > 0) {
                try {
                    JsonNode aiResult = aiService.splitSubtitleWithAI(chunkText.toString().trim());
                    if (aiResult != null && aiResult.has("sentences") && aiResult.get("sentences").isArray()) {
                        for (JsonNode node : aiResult.get("sentences")) {
                            allAiSentences.add(node.asText());
                        }
                    } else {
                        log.warn("AI didn't return an array for last chunk: {}", aiResult);
                    }
                } catch (Exception e) {
                    log.error("AI split failed for last chunk. Falling back to raw text.", e);
                    allAiSentences.add(chunkText.toString().trim());
                }
            }

            // Bước 4: Map lại thời gian cho các câu AI đã chia
            return mapTimestamps(allAiSentences, rawSegments);

        } finally {
            cleanupTempDir(tempDir);
        }
    }

    // ===== Nội bộ: Record cho segment thô =====

    private record RawSegment(double start, double end, String text) {}

    // ===== Map timestamp từ raw segments cho các câu AI trả về =====

    private List<DictationSentenceDto> mapTimestamps(List<String> aiSentences, List<RawSegment> rawSegments) {
        List<DictationSentenceDto> result = new ArrayList<>();

        // Tạo danh sách tất cả các từ kèm thời gian ước lượng
        List<WordWithTime> allWords = new ArrayList<>();
        for (RawSegment seg : rawSegments) {
            String[] words = seg.text.split("\\s+");
            double timePerWord = words.length > 0 ? (seg.end - seg.start) / words.length : 0;
            for (int i = 0; i < words.length; i++) {
                double wStart = seg.start + i * timePerWord;
                double wEnd = seg.start + (i + 1) * timePerWord;
                allWords.add(new WordWithTime(words[i], wStart, wEnd));
            }
        }

        int wordIdx = 0;
        int orderIndex = 1;

        for (int i = 0; i < aiSentences.size(); i++) {
            String sentence = aiSentences.get(i).trim();
            if (sentence.isEmpty()) continue;

            String[] sentenceWords = sentence.split("\\s+");

            int matchStart = findBestMatch(allWords, sentenceWords, wordIdx);
            if (matchStart < 0) matchStart = wordIdx; 

            // Nếu có từ bị lọt thỏm (wordIdx < matchStart), nối chúng vào đầu câu hiện tại
            // để không bị mất chữ và không bị hổng thời gian.
            StringBuilder finalSentence = new StringBuilder();
            for(int j = wordIdx; j < matchStart; j++) {
                 finalSentence.append(allWords.get(j).word).append(" ");
            }
            finalSentence.append(sentence);

            int matchEnd = Math.min(matchStart + sentenceWords.length, allWords.size());

            // Bắt đầu từ wordIdx luôn để không hổng thời gian
            double startTime = allWords.get(wordIdx).start; 
            double endTime = allWords.get(matchEnd - 1).end;

            result.add(DictationSentenceDto.builder()
                    .orderIndex(orderIndex++)
                    .startTime(Math.round(startTime * 100.0) / 100.0)
                    .endTime(Math.round(endTime * 100.0) / 100.0)
                    .text(finalSentence.toString().trim())
                    .translation("")
                    .build());

            wordIdx = matchEnd;
        }

        // Nếu còn thừa từ cuối cùng chưa map hết vào câu nào
        if (wordIdx < allWords.size() && !result.isEmpty()) {
             DictationSentenceDto last = result.get(result.size() - 1);
             StringBuilder extra = new StringBuilder(last.getText());
             for(int j = wordIdx; j < allWords.size(); j++) {
                 extra.append(" ").append(allWords.get(j).word);
             }
             last.setText(extra.toString().trim());
             last.setEndTime(Math.round(allWords.get(allWords.size()-1).end * 100.0) / 100.0);
        }

        return result;
    }

    private record WordWithTime(String word, double start, double end) {}

    /**
     * Tìm vị trí tốt nhất trong allWords khớp với sentenceWords, bắt đầu từ startIdx.
     */
    private int findBestMatch(List<WordWithTime> allWords, String[] sentenceWords, int startIdx) {
        if (sentenceWords.length == 0) return startIdx;

        String firstWord = sentenceWords[0];
        // Tìm từ startIdx trở đi
        for (int i = startIdx; i < allWords.size(); i++) {
            if (allWords.get(i).word.startsWith(firstWord) || firstWord.startsWith(allWords.get(i).word)) {
                return i;
            }
        }
        return -1;
    }

    // ===== Parse XML =====

    private List<RawSegment> parseXmlToRawSegments(String xmlContent) throws Exception {
        List<RawSegment> segments = new ArrayList<>();

        if (!xmlContent.trim().startsWith("<?xml")) {
            xmlContent = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + xmlContent;
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
        NodeList textNodes = document.getElementsByTagName("text");

        for (int i = 0; i < textNodes.getLength(); i++) {
            Element el = (Element) textNodes.item(i);
            String startStr = el.getAttribute("start");
            String durStr = el.getAttribute("dur");
            String text = el.getTextContent();

            if (text == null || text.trim().isEmpty()) continue;

            text = text.replaceAll("&amp;", "&")
                    .replaceAll("&#39;", "'")
                    .replaceAll("&quot;", "\"")
                    .replaceAll("&lt;", "<")
                    .replaceAll("&gt;", ">")
                    .replaceAll("\n", " ")
                    .trim();

            if (text.isEmpty()) continue;

            double start = Double.parseDouble(startStr);
            double dur = (durStr == null || durStr.isEmpty()) ? 2.0 : Double.parseDouble(durStr);
            double end = start + dur;

            segments.add(new RawSegment(
                    Math.round(start * 100.0) / 100.0,
                    Math.round(end * 100.0) / 100.0,
                    text));
        }
        return segments;
    }

    // ===== yt-dlp =====

    private String findYtDlpPath() throws Exception {
        String userDir = System.getProperty("user.dir");
        File ytDlp = new File(userDir, "yt-dlp.exe");
        if (ytDlp.exists()) return ytDlp.getAbsolutePath();

        try {
            Process p = new ProcessBuilder("where", "yt-dlp").start();
            String result = new String(p.getInputStream().readAllBytes()).trim();
            if (!result.isEmpty() && new File(result.split("\\R")[0]).exists()) {
                return result.split("\\R")[0];
            }
        } catch (Exception ignored) {}

        throw new Exception("Không tìm thấy yt-dlp.exe. Hãy đặt file yt-dlp.exe vào thư mục gốc của project.");
    }

    private String downloadSubtitle(String ytDlpPath, String youtubeId, String outputTemplate, String subLang) {
        try {
            List<String> command = List.of(
                    ytDlpPath,
                    "--write-sub", "--write-auto-sub",
                    "--sub-lang", subLang,
                    "--sub-format", "srv1",
                    "--skip-download",
                    "-o", outputTemplate,
                    "https://www.youtube.com/watch?v=" + youtubeId
            );

            log.info("Running yt-dlp: {}", String.join(" ", command));
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            log.info("yt-dlp exit: {}, output: {}", exitCode, output);

            if (exitCode != 0) return null;

            File dir = new File(outputTemplate).getParentFile();
            if (dir != null && dir.exists()) {
                File[] files = dir.listFiles((d, name) -> name.endsWith(".srv1"));
                if (files != null && files.length > 0) {
                    for (File f : files)
                        if (f.getName().contains(".ko.") || f.getName().contains(".ko-"))
                            return f.getAbsolutePath();
                    for (File f : files)
                        if (f.getName().contains(".en.") || f.getName().contains(".en-"))
                            return f.getAbsolutePath();
                    return files[0].getAbsolutePath();
                }
            }
        } catch (Exception e) {
            log.error("Error running yt-dlp", e);
        }
        return null;
    }

    private void cleanupTempDir(Path tempDir) {
        try {
            if (Files.exists(tempDir)) {
                Files.walk(tempDir)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                        });
            }
        } catch (IOException e) {
            log.warn("Could not cleanup temp dir: {}", tempDir, e);
        }
    }
}
