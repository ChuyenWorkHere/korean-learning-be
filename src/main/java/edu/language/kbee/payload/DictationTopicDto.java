package edu.language.kbee.payload;

import edu.language.kbee.enums.DictationTopicStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DictationTopicDto {
    private String id;
    private String title;
    private String description;
    private String icon;
    private DictationTopicStatus status;
    private int lessonCount;
}
