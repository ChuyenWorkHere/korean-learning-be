package edu.language.kbee.controller;

import edu.language.kbee.payload.BookDto;
import edu.language.kbee.payload.CreateBookRequest;
import edu.language.kbee.payload.UpdateBookRequest;
import edu.language.kbee.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/books")
public class BookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<List<BookDto>> getAllBooks() {
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<BookDto> getBookById(@PathVariable String bookId) {
        return ResponseEntity.ok(bookService.getBookById(bookId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<BookDto> createBook(@RequestBody CreateBookRequest request) {
        return ResponseEntity.ok(bookService.createBook(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{bookId}")
    public ResponseEntity<BookDto> updateBook(@PathVariable String bookId, @RequestBody UpdateBookRequest request) {
        return ResponseEntity.ok(bookService.updateBook(bookId, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{bookId}")
    public ResponseEntity<?> deleteBook(@PathVariable String bookId) {
        bookService.deleteBook(bookId);
        return ResponseEntity.ok("Book deleted successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{bookId}/status")
    public ResponseEntity<BookDto> toggleBookStatus(@PathVariable String bookId) {
        return ResponseEntity.ok(bookService.toggleBookStatus(bookId));
    }
}
