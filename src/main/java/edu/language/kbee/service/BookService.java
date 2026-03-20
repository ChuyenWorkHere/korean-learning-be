package edu.language.kbee.service;

import edu.language.kbee.payload.BookDto;
import edu.language.kbee.payload.CreateBookRequest;
import edu.language.kbee.payload.UpdateBookRequest;

import java.util.List;

public interface BookService {
    BookDto createBook(CreateBookRequest request);
    BookDto getBookById(String bookId);
    List<BookDto> getAllBooks();
    BookDto updateBook(String bookId, UpdateBookRequest request);
    void deleteBook(String bookId);
    BookDto toggleBookStatus(String bookId);
}
