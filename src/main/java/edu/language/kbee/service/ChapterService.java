package edu.language.kbee.service;

import edu.language.kbee.payload.ChapterDto;

import java.util.List;

public interface ChapterService {
    ChapterDto createChapter(String bookId, ChapterDto chapterDto);
    ChapterDto getChapterById(String chapterId);
    List<ChapterDto> getChaptersByBookId(String bookId);
    ChapterDto updateChapter(String chapterId, ChapterDto chapterDto);
    void deleteChapter(String chapterId);
}
