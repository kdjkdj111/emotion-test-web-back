package com.kdd.emoticon_test_web_back.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EmoticonProject {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;

    // 🚀 [NEW] 이 파일이 속한 프로젝트(작업 세션)를 묶어주는 ID
    private String projectId;

    private String originalFileName;
    private String fileId;
    private String emoticonType;
    private String status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private LocalDateTime createdAt;
}