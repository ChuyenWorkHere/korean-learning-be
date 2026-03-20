package edu.language.kbee.service.impl;

import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.Book;
import edu.language.kbee.model.Chapter;
import edu.language.kbee.payload.ChapterDto;
import edu.language.kbee.repository.BookRepository;
import edu.language.kbee.repository.ChapterRepository;
import edu.language.kbee.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChapterServiceImpl implements ChapterService {

    private final ChapterRepository chapterRepository;
    private final BookRepository bookRepository;

    @Override
    public ChapterDto createChapter(String bookId, ChapterDto chapterDto) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with bookId" + bookId));

        Chapter chapter = Chapter.builder()
                .title(chapterDto.getTitle())
                .content(chapterDto.getContent())
                .book(book)
                .chapterOrder(chapterDto.getChapterOrder())
                .build();

        Chapter savedChapter = chapterRepository.save(chapter);
        return mapToDto(savedChapter);
    }

    @Override
    public ChapterDto getChapterById(String chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found with chapterId" + chapterId));
        return mapToDto(chapter);
    }

    @Override
    public List<ChapterDto> getChaptersByBookId(String bookId) {
        List<Chapter> chapters = chapterRepository.findByBookBookIdOrderByChapterOrderAsc(bookId);
        return chapters.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public ChapterDto updateChapter(String chapterId, ChapterDto chapterDto) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found with chapterId" + chapterId));

        chapter.setTitle(chapterDto.getTitle());
        chapter.setContent(chapterDto.getContent());
        if(chapterDto.getChapterOrder() != null) {
            chapter.setChapterOrder(chapterDto.getChapterOrder());
        }

        Chapter updatedChapter = chapterRepository.save(chapter);
        return mapToDto(updatedChapter);
    }

    @Override
    public void deleteChapter(String chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found with chapterId" + chapterId));
        chapterRepository.delete(chapter);
    }

    private ChapterDto mapToDto(Chapter chapter) {
        return ChapterDto.builder()
                .chapterId(chapter.getChapterId())
                .title(chapter.getTitle())
                .content(chapter.getContent())
                .bookId(chapter.getBook().getBookId())
                .chapterOrder(chapter.getChapterOrder())
                .build();
    }
}
