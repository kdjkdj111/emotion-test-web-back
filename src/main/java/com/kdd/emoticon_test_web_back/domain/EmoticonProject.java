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

    private String fileId; // 프론트에서 생성한 파일 고유 ID

    private String emoticonType; // STILL, MINI, ANIMATED 등

    private String status; // 현재 검증 상태

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private LocalDateTime createdAt; // 생성 시간
}