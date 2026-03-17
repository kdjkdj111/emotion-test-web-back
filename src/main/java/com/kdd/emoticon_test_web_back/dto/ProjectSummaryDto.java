package com.kdd.emoticon_test_web_back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ProjectSummaryDto {
    private String projectId;
    private LocalDateTime createdAt;
    private long totalCount;    // 총 파일 수
    private long successCount;  // 통과한 파일 수
    private String type;    // STILL, ANIMATED 등 대표 타입
}