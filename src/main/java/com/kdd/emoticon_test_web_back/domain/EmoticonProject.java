package com.kdd.emoticon_test_web_back.domain;

import jakarta.persistence.*;
import lombok.*;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EmoticonProject {


    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 데이터베이스 식별자

    private String userId; // 사용자 아이디

    private String originalFileName; // 사용자가 올린 원래 파일명

    private String savedFileName; // 서버에 저장된 UUID 파일명

    private String filePath; // 파일의 로컬 저장 경로

    private String status; // 현재 검증 상태

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private LocalDateTime createdAt; // 생성 시간
}