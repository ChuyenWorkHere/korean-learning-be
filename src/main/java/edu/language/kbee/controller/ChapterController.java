package edu.language.kbee.controller;

import edu.language.kbee.payload.ChapterDto;
import edu.language.kbee.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/books/{bookId}/chapters")
public class ChapterController {

    private final ChapterService chapterService;

    @GetMapping
    public ResponseEntity<List<ChapterDto>> getChaptersByBookId(@PathVariable String bookId) {
        return ResponseEntity.ok(chapterService.getChaptersByBookId(bookId));
    }

    @GetMapping("/{chapterId}")
    public ResponseEntity<ChapterDto> getChapterById(@PathVariable String bookId, @PathVariable String chapterId) {
        return ResponseEntity.ok(chapterService.getChapterById(chapterId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ChapterDto> createChapter(@PathVariable String bookId, @RequestBody ChapterDto chapterDto) {
        return ResponseEntity.ok(chapterService.createChapter(bookId, chapterDto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{chapterId}")
    public ResponseEntity<ChapterDto> updateChapter(@PathVariable String bookId, @PathVariable String chapterId, @RequestBody ChapterDto chapterDto) {
        return ResponseEntity.ok(chapterService.updateChapter(chapterId, chapterDto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{chapterId}")
    public ResponseEntity<?> deleteChapter(@PathVariable String bookId, @PathVariable String chapterId) {
        chapterService.deleteChapter(chapterId);
        return ResponseEntity.ok("Chapter deleted successfully");
    }
}
