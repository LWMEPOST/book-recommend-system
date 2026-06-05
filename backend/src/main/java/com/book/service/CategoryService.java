package com.book.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.book.common.Result;
import com.book.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {

    Result<List<Category>> getCategoryList();
}
