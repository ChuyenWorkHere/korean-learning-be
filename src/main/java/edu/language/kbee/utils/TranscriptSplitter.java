package edu.language.kbee.utils;

import edu.language.kbee.payload.DictationSentenceDto;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.List;

public class TranscriptSplitter {

    private static final double MAX_DURATION_SEC = 60.0;
    private static final int MAX_WORDS = 100;
    private static final double MAX_PAUSE_SEC = 1.0;

    // ===== Dành cho AssemblyAI (word-level JSON) =====

    public static List<DictationSentenceDto> split(JsonNode assemblyAiResponse) {
        List<DictationSentenceDto> sentences = new ArrayList<>();
        
        JsonNode wordsNode = assemblyAiResponse.get("words");
        if (wordsNode == null || !wordsNode.isArray() || wordsNode.isEmpty()) {
            return sentences;
        }

        ArrayNode words = (ArrayNode) wordsNode;
        int orderIndex = 1;
        
        List<JsonNode> currentSentenceWords = new ArrayList<>();
        double currentSentenceStart = -1;
        double currentSentenceEnd = -1;

        for (int i = 0; i < words.size(); i++) {
            JsonNode word = words.get(i);
            // AssemblyAI returns time in milliseconds
            double wordStart = word.get("start").asDouble() / 1000.0;
            double wordEnd = word.get("end").asDouble() / 1000.0;

            if (currentSentenceWords.isEmpty()) {
                currentSentenceStart = wordStart;
            }

            boolean shouldSplit = false;
            if (!currentSentenceWords.isEmpty()) {
                double durationWithWord = wordEnd - currentSentenceStart;
                double pauseBeforeWord = wordStart - currentSentenceEnd;
                int wordsCount = currentSentenceWords.size() + 1;

                if (durationWithWord > MAX_DURATION_SEC 
                    || wordsCount > MAX_WORDS 
                    || pauseBeforeWord > MAX_PAUSE_SEC) {
                    shouldSplit = true;
                }
            }

            if (shouldSplit) {
                sentences.add(buildSentence(orderIndex++, currentSentenceStart, currentSentenceEnd, currentSentenceWords));
                currentSentenceWords.clear();
                currentSentenceStart = wordStart;
            }
            
            currentSentenceWords.add(word);
            currentSentenceEnd = wordEnd;
        }

        if (!currentSentenceWords.isEmpty()) {
            sentences.add(buildSentence(orderIndex, currentSentenceStart, currentSentenceEnd, currentSentenceWords));
        }

        return sentences;
    }

    private static DictationSentenceDto buildSentence(int order, double start, double end, List<JsonNode> words) {
        StringBuilder textBuilder = new StringBuilder();
        for (JsonNode w : words) {
            textBuilder.append(w.get("text").asText()).append(" ");
        }
        
        return DictationSentenceDto.builder()
                .orderIndex(order)
                .startTime(start)
                .endTime(end)
                .text(textBuilder.toString().trim())
                .translation("")
                .build();
    }

    /**
     * Chỉ tách các câu quá dài, KHÔNG gộp.
     * Dùng cho YouTube subtitles vì chúng đã có ranh giới câu chuẩn sẵn.
     */
    public static List<DictationSentenceDto> splitSegments(List<DictationSentenceDto> rawSegments) {
        if (rawSegments == null || rawSegments.isEmpty()) return rawSegments;

        List<DictationSentenceDto> result = new ArrayList<>();
        int orderIndex = 1;
        for (DictationSentenceDto seg : rawSegments) {
            double duration = seg.getEndTime() - seg.getStartTime();
            int wordCount = countWords(seg.getText());

            if (duration <= MAX_DURATION_SEC && wordCount <= MAX_WORDS) {
                result.add(seg.toBuilder().orderIndex(orderIndex++).build());
            } else {
                for (DictationSentenceDto part : splitLongSegment(seg)) {
                    result.add(part.toBuilder().orderIndex(orderIndex++).build());
                }
            }
        }

        return result;
    }




    private static List<DictationSentenceDto> splitLongSegment(DictationSentenceDto seg) {
        List<DictationSentenceDto> parts = new ArrayList<>();
        String[] words = seg.getText().split("\\s+");
        double totalDuration = seg.getEndTime() - seg.getStartTime();
        double timePerWord = words.length > 0 ? totalDuration / words.length : totalDuration;

        int numParts = Math.max(
                (int) Math.ceil(totalDuration / MAX_DURATION_SEC),
                (int) Math.ceil((double) words.length / MAX_WORDS));
        int wordsPerPart = (int) Math.ceil((double) words.length / numParts);

        int wordIdx = 0;
        for (int p = 0; p < numParts && wordIdx < words.length; p++) {
            StringBuilder partText = new StringBuilder();
            double partStart = seg.getStartTime() + (wordIdx * timePerWord);
            int count = 0;

            while (wordIdx < words.length && count < wordsPerPart) {
                if (partText.length() > 0) partText.append(" ");
                partText.append(words[wordIdx]);
                wordIdx++;
                count++;
            }

            double partEnd = (p == numParts - 1 || wordIdx >= words.length)
                    ? seg.getEndTime()
                    : seg.getStartTime() + (wordIdx * timePerWord);

            parts.add(DictationSentenceDto.builder()
                    .startTime(Math.round(partStart * 100.0) / 100.0)
                    .endTime(Math.round(partEnd * 100.0) / 100.0)
                    .text(partText.toString())
                    .translation("").build());
        }
        return parts;
    }

    private static int countWords(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        return text.trim().split("\\s+").length;
    }
}
