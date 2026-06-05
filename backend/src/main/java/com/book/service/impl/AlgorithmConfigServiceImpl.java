package com.book.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.book.common.Result;
import com.book.entity.AlgorithmConfig;
import com.book.mapper.AlgorithmConfigMapper;
import com.book.service.AlgorithmConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlgorithmConfigServiceImpl extends ServiceImpl<AlgorithmConfigMapper, AlgorithmConfig> implements AlgorithmConfigService {

    @Override
    public Result<List<AlgorithmConfig>> getConfigList() {
        LambdaQueryWrapper<AlgorithmConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(AlgorithmConfig::getId);
        return Result.success(list(wrapper));
    }

    @Override
    public Result<?> updateConfig(String configKey, String configValue) {
        Result<?> validationResult = validateConfigValue(configKey, configValue);
        if (validationResult != null) {
            return validationResult;
        }

        LambdaQueryWrapper<AlgorithmConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlgorithmConfig::getConfigKey, configKey);
        AlgorithmConfig config = getOne(wrapper);
        if (config == null) {
            return Result.error("配置项不存在");
        }
        config.setConfigValue(configValue.trim());
        updateById(config);
        return Result.success("更新成功", null);
    }

    @Override
    public String getConfigValue(String configKey, String defaultValue) {
        LambdaQueryWrapper<AlgorithmConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlgorithmConfig::getConfigKey, configKey);
        AlgorithmConfig config = getOne(wrapper);
        return config != null ? config.getConfigValue() : defaultValue;
    }

    private Result<?> validateConfigValue(String configKey, String configValue) {
        if (configValue == null || configValue.trim().isEmpty()) {
            return Result.error("配置值不能为空");
        }

        String value = configValue.trim();
        try {
            switch (configKey) {
                case "similar_user_count":
                case "recommend_count":
                case "cold_start_min_ratings":
                case "implicit_borrow_score":
                case "implicit_favorite_score":
                    if (Integer.parseInt(value) <= 0) {
                        return Result.error("该配置项必须为大于 0 的整数");
                    }
                    break;
                case "similarity_threshold":
                    double threshold = Double.parseDouble(value);
                    if (threshold < 0 || threshold > 1) {
                        return Result.error("相似度阈值必须在 0 到 1 之间");
                    }
                    break;
                default:
                    break;
            }
        } catch (NumberFormatException ex) {
            return Result.error("配置值格式不正确");
        }

        return null;
    }
}
