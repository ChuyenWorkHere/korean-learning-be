package edu.language.kbee.service.ai;

public final class AiResponseParser {

    private AiResponseParser() {
    }

    public static String extractJson(String response) {
        if (response == null) {
            return "{}";
        }

        String cleaned = response.trim();
        if (cleaned.contains("```json")) {
            cleaned = cleaned.substring(cleaned.indexOf("```json") + 7);
            cleaned = cleaned.substring(0, cleaned.lastIndexOf("```"));
        } else if (cleaned.contains("```")) {
            cleaned = cleaned.substring(cleaned.indexOf("```") + 3);
            cleaned = cleaned.substring(0, cleaned.lastIndexOf("```"));
        }

        int firstBrace = cleaned.indexOf('{');
        int firstBracket = cleaned.indexOf('[');
        int start = (firstBrace != -1 && (firstBracket == -1 || firstBrace < firstBracket))
                ? firstBrace
                : firstBracket;

        int lastBrace = cleaned.lastIndexOf('}');
        int lastBracket = cleaned.lastIndexOf(']');
        int end = Math.max(lastBrace, lastBracket);

        if (start != -1 && end != -1 && end > start) {
            return cleaned.substring(start, end + 1);
        }

        return cleaned.trim();
    }
}
