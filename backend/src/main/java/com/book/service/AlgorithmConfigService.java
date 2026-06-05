package com.book.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.book.common.Result;
import com.book.entity.AlgorithmConfig;

import java.util.List;

public interface AlgorithmConfigService extends IService<AlgorithmConfig> {

    Result<List<AlgorithmConfig>> getConfigList();

    Result<?> updateConfig(String configKey, String configValue);

    String getConfigValue(String configKey, String defaultValue);
}
