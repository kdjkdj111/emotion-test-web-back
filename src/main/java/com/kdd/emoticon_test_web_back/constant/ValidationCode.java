package com.kdd.emoticon_test_web_back.constant;

public enum ValidationCode {
    INVALID_MIME("PNG 파일만 허용됩니다."),
    INVALID_SIZE("규격이 맞지 않습니다. (360x360 필수)"),
    INVALID_OPACITY("외곽선 투명도가 부적절한 픽셀이 발견되었습니다."),
    FILE_READ_ERROR("파일을 읽는 중 오류가 발생했습니다."),
    FILE_SIZE_EXCEEDED("파일 용량이 150KB를 초과했습니다.");

    private final String message;
    ValidationCode(String message) { this.message = message; }
    public String getMessage() { return message; }
}