package com.kdd.emoticon_test_web_back.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmoticonResponseDto {
    private String fileId;
    private String fileName;      // 파일명 (어떤 파일인지 알아야 하니까)
    private String status;        // "SUCCESS" or "FAILED" (아이콘 띄울지 말지 결정)
    private String errorMessage;  // 에러 내용 (아이콘 눌렀을 때 띄울 내용)
}