package com.kdd.emoticon_test_web_back.controller;


import com.kdd.emoticon_test_web_back.domain.EmoticonProject;
import com.kdd.emoticon_test_web_back.dto.EmoticonResponseDto;
import com.kdd.emoticon_test_web_back.service.EmoticonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController // @Controller 대신 @RestController를 쓰면 JSON 반환이 기본이 됩니다. [cite: 2026-02-03]
@RequestMapping("/api/emoticons") // 엔드포인트 공통 경로 설정
@RequiredArgsConstructor // 1. emoticonService 주입을 위한 생성자를 자동으로 만듭니다. [cite: 2026-02-03]
@Slf4j // 2. log 객체를 자동으로 생성해 줍니다. (private static final Logger log ... 와 동일) [cite: 2026-02-03]
@CrossOrigin(origins = "http://localhost:5173")
public class EmoticonController {

    private final EmoticonService emoticonService;

    @PostMapping("/upload")
    public ResponseEntity<EmoticonResponseDto> uploadEmoticon( // 반환 타입 변경
                                                               @RequestParam("userId") String userId,
                                                               @RequestParam("file") MultipartFile file,
                                                               @RequestParam("type") String type,
                                                               @RequestParam("fileId") String fileId)
    {

        try {
            log.info("업로드 요청 - 유저: {}, 타입: {}, 파일: {}", userId, type, file.getOriginalFilename());

            // 1. 서비스 호출 (저장된 엔티티를 받음)
            EmoticonProject savedProject = emoticonService.processEmoticon(userId, file, type, fileId);

            // 2. 엔티티 -> DTO 변환 (리액트가 쓰기 편하게 포장)
            EmoticonResponseDto responseDto = EmoticonResponseDto.builder()
                    .fileId(fileId)
                    .fileName(savedProject.getOriginalFileName())
                    .status(savedProject.getStatus())
                    .errorMessage(savedProject.getErrorMessage())
                    .build();

            // 3. JSON으로 응답 전송
            return ResponseEntity.ok(responseDto);

        } catch (Exception e) {
            log.error("서버 에러", e);
            // 서버 에러가 났을 때도 규격에 맞춰서 에러 응답을 보내줍니다.
            return ResponseEntity.internalServerError().body(
                    EmoticonResponseDto.builder()
                            .fileName(file.getOriginalFilename())
                            .status("ERROR")
                            .errorMessage("서버 내부 오류: " + e.getMessage())
                            .build()
            );
        }
    }


}
