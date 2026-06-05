package com.book.controller;

import com.book.common.Result;
import com.book.entity.AlgorithmConfig;
import com.book.service.AlgorithmConfigService;
import com.book.service.RecommendService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/algorithm")
public class AlgorithmController {

    private final AlgorithmConfigService algorithmConfigService;
    private final RecommendService recommendService;

    public AlgorithmController(AlgorithmConfigService algorithmConfigService,
                               RecommendService recommendService) {
        this.algorithmConfigService = algorithmConfigService;
        this.recommendService = recommendService;
    }

    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<AlgorithmConfig>> getConfigList() {
        return algorithmConfigService.getConfigList();
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getSummary() {
        return recommendService.getRecommendSummary();
    }

    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> updateConfig(@RequestParam String configKey,
                                                    @RequestParam String configValue) {
        Result<?> updateResult = algorithmConfigService.updateConfig(configKey, configValue);
        if (updateResult.getCode() != 200) {
            return Result.error(updateResult.getMsg());
        }

        Result<Map<String, Object>> refreshResult = recommendService.refreshRecommendCache();
        return Result.success("算法配置已更新，并已刷新推荐缓存", refreshResult.getData());
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> refreshRecommend() {
        return recommendService.refreshRecommendCache();
    }
}
