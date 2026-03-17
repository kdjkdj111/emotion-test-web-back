package com.kdd.emoticon_test_web_back.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ValidationError {
    private String code;     // DB 저장용 (예: "ERR_MARGIN")
    private String message;  // 프론트엔드용 (예: "여백 침범: X:14, Y:20")
}