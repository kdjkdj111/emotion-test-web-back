package com.kdd.emoticon_test_web_back.controller;

import com.kdd.emoticon_test_web_back.domain.EmoticonProject;
import com.kdd.emoticon_test_web_back.dto.EmoticonResponseDto;
import com.kdd.emoticon_test_web_back.dto.ProjectSummaryDto;
import com.kdd.emoticon_test_web_back.service.EmoticonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/emoticons")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // 💡 오타 수정 (" z" -> "*")
public class EmoticonController {

    private final EmoticonService emoticonService;

    // -------------------------------------------------------------------------
    // 1. 업로드 로직 (엄청나게 다이어트 성공!)
    // -------------------------------------------------------------------------
    @PostMapping("/upload")
    public ResponseEntity<EmoticonResponseDto> uploadEmoticon(
            @RequestParam("userId") String userId,
            @RequestParam("projectId") String projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @RequestParam("fileId") String fileId) {

        try {
            log.info("업로드 - 유저: {}, 프로젝트: {}, 파일: {}", userId, projectId, file.getOriginalFilename());

            // 💡 서비스 호출 시 projectId도 같이 넘겨줍니다.
            EmoticonResponseDto responseDto = emoticonService.processEmoticon(userId, projectId, file, type, fileId);

            return ResponseEntity.ok(responseDto);

        } catch (Exception e) {
            log.error("서버 에러", e);
            return ResponseEntity.internalServerError().body(
                    EmoticonResponseDto.builder()
                            .fileId(fileId)
                            .fileName(file.getOriginalFilename())
                            .status("ERROR")
                            .errorMessage("서버 내부 오류: " + e.getMessage())
                            .build()
            );
        }
    }

    // -------------------------------------------------------------------------
    // 2. 히스토리 로직 (엔티티 -> DTO 변환 및 암호 해독)
    // -------------------------------------------------------------------------
    // 🚀 기존 /history 엔드포인트를 프로젝트 상세 조회용으로 변경합니다.
    @GetMapping("/projects/{projectId}/history")
    public ResponseEntity<List<EmoticonResponseDto>> getProjectDetails(@PathVariable("projectId") String projectId) {
        try {
            log.info("프로젝트 상세 내역 조회 요청 - 프로젝트 ID: {}", projectId);

            // 1. 서비스에서 '중복이 제거된 최신 엔티티' 리스트를 가져옵니다.
            List<EmoticonProject> projects = emoticonService.getProjectDetails(projectId);

            // 2. DTO 변환 및 에러 코드 번역 (기존에 작성해둔 로직 그대로!)
            List<EmoticonResponseDto> history = projects.stream()
                    .map(project -> {
                        String codes = project.getErrorMessage();
                        String translatedMessage = "";

                        if (codes != null && !codes.isEmpty()) {
                            translatedMessage = Arrays.stream(codes.split(","))
                                    .map(this::translateErrorCode) // 만들어둔 번역기 돌리기
                                    .collect(Collectors.joining("\n"));
                        }

                        return EmoticonResponseDto.builder()
                                .fileId(project.getFileId())
                                .fileName(project.getOriginalFileName())
                                .status(project.getStatus())
                                .errorMessage(translatedMessage)
                                .build();
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(history);

        } catch (Exception e) {
            log.error("프로젝트 조회 중 에러", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // -------------------------------------------------------------------------
    // 💡 미니 번역기 메서드 (DB 코드 -> 리액트용 한글 메시지)
    // -------------------------------------------------------------------------
    private String translateErrorCode(String code) {
        switch (code.trim()) {
            case "ERR_MARGIN": return "여백 침범 오류가 있었습니다.";
            case "ERR_PIXEL": return "찌꺼기 픽셀이 발견되었습니다.";
            case "ERR_SIZE": return "파일 용량 초과 오류가 있었습니다.";
            case "ERR_DIMENSION": return "규격(사이즈) 불일치 오류가 있었습니다.";
            case "INVALID_MIME": return "잘못된 파일 형식입니다 (PNG만 허용).";
            case "FILE_READ_ERROR": return "이미지 파일을 읽을 수 없습니다 (손상 의심).";
            case "WRONG_TYPE": return "알 수 없는 이모티콘 타입입니다.";
            case "ERR_SERVER": return "서버 처리 중 알 수 없는 오류가 발생했습니다.";
            default: return "알 수 없는 오류 (" + code + ")";
        }
    }

    @GetMapping("/projects")
    public ResponseEntity<List<ProjectSummaryDto>> getProjectList(@RequestParam("userId") String userId) {
        log.info("유저 프로젝트 목록 조회 - 유저: {}", userId);
        return ResponseEntity.ok(emoticonService.getUserProjectList(userId));
    }
}