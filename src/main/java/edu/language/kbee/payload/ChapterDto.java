package edu.language.kbee.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChapterDto {
    private String chapterId;
    private String title;
    private String content;
    private String bookId;
    private Integer chapterOrder;
}
