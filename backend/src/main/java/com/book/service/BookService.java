package com.book.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.book.common.Result;
import com.book.dto.BookDTO;
import com.book.entity.Book;
import com.book.vo.BookVO;

import java.util.List;
import java.util.Map;

public interface BookService extends IService<Book> {

    Result<IPage<BookVO>> getBookList(int page, int size, String keyword, Long categoryId);

    Result<IPage<BookVO>> getAdminBookList(int page, int size, String keyword, Long categoryId, Integer status);

    Result<BookVO> getBookDetail(Long id);

    Result<?> addBook(BookDTO dto);

    Result<?> updateBook(Long id, BookDTO dto);

    Result<Map<String, Object>> batchImportBooks(List<BookDTO> dtoList);

    Result<?> deleteBook(Long id);
}
