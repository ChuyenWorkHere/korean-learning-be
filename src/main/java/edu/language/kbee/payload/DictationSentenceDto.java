package edu.language.kbee.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DictationSentenceDto {
    private String id;
    private int orderIndex;
    private double startTime;
    private double endTime;
    private String text;
    private String translation;
}
