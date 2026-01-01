package com.tp27.book.service;

import com.tp27.book.client.PricingClient;
import com.tp27.book.dto.BorrowResponse;
import com.tp27.book.exception.BookNotFoundException;
import com.tp27.book.exception.OutOfStockException;
import com.tp27.book.model.Book;
import com.tp27.book.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookService {

    private static final Logger logger = LoggerFactory.getLogger(BookService.class);

    private final BookRepository bookRepository;
    private final PricingClient pricingClient;

    public BookService(BookRepository bookRepository, PricingClient pricingClient) {
        this.bookRepository = bookRepository;
        this.pricingClient = pricingClient;
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Book not found with id: " + id));
    }

    public Book createBook(Book book) {
        logger.info("Creating new book: {}", book.getTitle());
        return bookRepository.save(book);
    }

    /**
     * Borrow a book with pessimistic locking to prevent negative stock
     * The @Transactional annotation ensures the lock is held for the entire transaction
     */
    @Transactional
    public BorrowResponse borrowBook(Long bookId) {
        logger.info("Attempting to borrow book with id: {}", bookId);

        // Use pessimistic lock (SELECT FOR UPDATE) to prevent concurrent modifications
        Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book not found with id: " + bookId));

        // Check if stock is available
        if (book.getStock() <= 0) {
            logger.warn("Book {} is out of stock", bookId);
            throw new OutOfStockException("Book is out of stock");
        }

        // Decrement stock
        book.setStock(book.getStock() - 1);
        bookRepository.save(book);

        // Get price from pricing service (with Resilience4j protection)
        Double price = pricingClient.getPrice(bookId);

        logger.info("Book {} borrowed successfully. Stock left: {}, Price: {}", 
                    bookId, book.getStock(), price);

        return new BorrowResponse(
                book.getId(),
                book.getTitle(),
                book.getStock(),
                price,
                "Book borrowed successfully"
        );
    }
}
