package edu.language.kbee.service.impl;

import edu.language.kbee.enums.BookStatus;
import edu.language.kbee.enums.CourseLevel;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.Book;
import edu.language.kbee.payload.BookDto;
import edu.language.kbee.payload.CreateBookRequest;
import edu.language.kbee.payload.UpdateBookRequest;
import edu.language.kbee.repository.BookRepository;
import edu.language.kbee.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    @Override
    public BookDto createBook(CreateBookRequest request) {
        Book book = Book.builder()
                .title(request.getTitle())
                .level(CourseLevel.valueOf(request.getLevel()))
                .duration(request.getDuration())
                .image(request.getImage())
                .status(request.getStatus() != null ? request.getStatus() : BookStatus.DRAFT)
                .build();

        Book savedBook = bookRepository.save(book);
        return mapToDto(savedBook);
    }

    @Override
    public BookDto getBookById(String bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with bookId" + bookId));
        return mapToDto(book);
    }

    @Override
    public List<BookDto> getAllBooks() {
        List<Book> books = bookRepository.findAll();
        return books.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public BookDto updateBook(String bookId, UpdateBookRequest request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with bookId" + bookId));

        book.setTitle(request.getTitle());
        book.setLevel(CourseLevel.valueOf(request.getLevel()));
        book.setDuration(request.getDuration());
        if(request.getImage() != null) {
            book.setImage(request.getImage());
        }

        Book updatedBook = bookRepository.save(book);
        return mapToDto(updatedBook);
    }

    @Override
    public void deleteBook(String bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with bookId" + bookId));
        bookRepository.delete(book);
    }

    @Override
    public BookDto toggleBookStatus(String bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with bookId" + bookId));

        if (book.getStatus() == BookStatus.PUBLISHED) {
            book.setStatus(BookStatus.DRAFT);
        } else {
            book.setStatus(BookStatus.PUBLISHED);
        }

        Book updatedBook = bookRepository.save(book);
        return mapToDto(updatedBook);
    }

    private BookDto mapToDto(Book book) {
        return BookDto.builder()
                .bookId(book.getBookId())
                .title(book.getTitle())
                .level(book.getLevel().name())
                .duration(book.getDuration())
                .image(book.getImage())
                .status(book.getStatus())
                .build();
    }
}
