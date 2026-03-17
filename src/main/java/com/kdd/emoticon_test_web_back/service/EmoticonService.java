package com.kdd.emoticon_test_web_back.service;

import com.kdd.emoticon_test_web_back.constant.ValidationCode;
import com.kdd.emoticon_test_web_back.domain.EmoticonProject;
import com.kdd.emoticon_test_web_back.dto.EmoticonResponseDto;
import com.kdd.emoticon_test_web_back.dto.ProjectSummaryDto;
import com.kdd.emoticon_test_web_back.dto.ValidationError;
import com.kdd.emoticon_test_web_back.repository.EmoticonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmoticonService {

    private final EmoticonRepository emoticonRepository;
    private final Tika tika = new Tika();

    /**
     * 개별 파일 검증 및 결과 저장
     */
    public EmoticonResponseDto processEmoticon(String userId, String projectId, MultipartFile file, String type, String fileId) {
        List<ValidationError> errors = new ArrayList<>();
        BufferedImage image = null;

        try {
            // 1. 파일 형식 검증 (MIME Type)
            if (!checkMimeType(file)) {
                errors.add(new ValidationError("INVALID_MIME", ValidationCode.INVALID_MIME.getMessage()));
            }

            // 2. 이미지 데이터 로드 및 손상 여부 확인
            image = ImageIO.read(file.getInputStream());
            if (image == null) {
                errors.add(new ValidationError("FILE_READ_ERROR", ValidationCode.FILE_READ_ERROR.getMessage()));
            }

            // 3. 타입별 상세 검증 로직 실행
            switch (type) {
                case "STILL":
                    validateStillEmoticon(image, file, errors);
                    break;
                case "MINI":
                    validateMiniEmoticon(image, file, errors);
                    break;
                case "ANIMATED":
                    validateAnimatedEmoticon(image, file, errors);
                    break;
                default:
                    errors.add(new ValidationError("WRONG_TYPE", "알 수 없는 타입: " + type));
            }

        } catch (IOException e) {
            log.error("File processing error: ", e);
            errors.add(new ValidationError("ERR_SERVER", "서버 내부 처리 오류"));
        }

        // 4. 검증 결과 저장 및 응답 DTO 반환
        return saveValidationResult(userId, projectId, fileId, file.getOriginalFilename(), type, errors);
    }

    /* =========================================================================
     * 타입별 검증 그룹화
     * ========================================================================= */

    private void validateStillEmoticon(BufferedImage image, MultipartFile file, List<ValidationError> errors) {
        checkDimensions(image, 360, 360, errors);
        checkFileSize(file, 150, errors);
        checkMarginViolation(image, 10, errors);
        checkStrayPixels(image, 15, errors);
    }

    private void validateMiniEmoticon(BufferedImage image, MultipartFile file, List<ValidationError> errors) {
        checkDimensions(image, 144, 144, errors);
        checkFileSize(file, 100, errors);
    }

    private void validateAnimatedEmoticon(BufferedImage image, MultipartFile file, List<ValidationError> errors) {
        checkDimensions(image, 360, 360, errors);
        checkFileSize(file, 2000, errors); // GIF 기준 2MB
    }

    /* =========================================================================
     * 세부 검증 유틸리티
     * ========================================================================= */

    private boolean checkMimeType(MultipartFile file) throws IOException {
        String mimeType = tika.detect(file.getInputStream());
        return "image/png".equals(mimeType);
    }

    private void checkDimensions(BufferedImage image, int targetW, int targetH, List<ValidationError> errors) {
        if (image == null) return;
        int width = image.getWidth();
        int height = image.getHeight();

        if (width != targetW || height != targetH) {
            errors.add(new ValidationError("ERR_DIMENSION",
                    String.format("규격 불일치: 현재 %dx%d (권장 %dx%d)", width, height, targetW, targetH)));
        }
    }

    private void checkFileSize(MultipartFile file, long maxKB, List<ValidationError> errors) {
        double fileSizeInKB = file.getSize() / 1024.0;
        if (fileSizeInKB > maxKB) {
            errors.add(new ValidationError("ERR_SIZE",
                    String.format("용량 초과: %.1fKB (제한 %dKB)", fileSizeInKB, maxKB)));
        }
    }

    private void checkMarginViolation(BufferedImage image, int margin, List<ValidationError> errors) {
        if (image == null) return;
        int w = image.getWidth();
        int h = image.getHeight();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // 외곽 테두리 영역 내 픽셀 존재 여부 확인
                if (x < margin || x >= w - margin || y < margin || y >= h - margin) {
                    int alpha = (image.getRGB(x, y) >> 24) & 0xff;
                    if (alpha > 0) {
                        errors.add(new ValidationError("ERR_MARGIN",
                                String.format("여백 침범 발견 (X:%d, Y:%d)", x, y)));
                        return;
                    }
                }
            }
        }
    }

    private void checkStrayPixels(BufferedImage image, int minBlobSize, List<ValidationError> errors) {
        if (image == null) return;
        int w = image.getWidth();
        int h = image.getHeight();
        boolean[][] visited = new boolean[h][w];
        int strayCount = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int alpha = (image.getRGB(x, y) >> 24) & 0xff;
                if (alpha > 0 && !visited[y][x]) {
                    int blobSize = bfsBlobSize(image, x, y, visited);
                    if (blobSize < minBlobSize) {
                        strayCount++;
                        if (strayCount <= 3) {
                            errors.add(new ValidationError("ERR_PIXEL",
                                    String.format("미세 픽셀 발견: %dpx (위치 X:%d, Y:%d)", blobSize, x, y)));
                        }
                    }
                }
            }
        }
        if (strayCount > 3) {
            errors.add(new ValidationError("ERR_PIXEL", "기타 " + (strayCount - 3) + "건의 미세 픽셀 추가 발견"));
        }
    }

    private int bfsBlobSize(BufferedImage image, int x, int y, boolean[][] visited) {
        int size = 0;
        Queue<int[]> q = new LinkedList<>();
        q.add(new int[]{x, y});
        visited[y][x] = true;

        int[] dx = {-1, 1, 0, 0, -1, -1, 1, 1};
        int[] dy = {0, 0, -1, 1, -1, 1, -1, 1};

        while (!q.isEmpty()) {
            int[] curr = q.poll();
            size++;
            for (int i = 0; i < 8; i++) {
                int nx = curr[0] + dx[i], ny = curr[1] + dy[i];
                if (nx >= 0 && nx < image.getWidth() && ny >= 0 && ny < image.getHeight() && !visited[ny][nx]) {
                    if (((image.getRGB(nx, ny) >> 24) & 0xff) > 0) {
                        visited[ny][nx] = true;
                        q.add(new int[]{nx, ny});
                    }
                }
            }
        }
        return size;
    }

    /* =========================================================================
     * 조회 및 히스토리 관리
     * ========================================================================= */

    public List<EmoticonProject> getUserHistory(String userId) {
        return emoticonRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 프로젝트 상세 조회 (파일명 중복 제거 후 최신 데이터 반환)
     */
    public List<EmoticonProject> getProjectDetails(String projectId) {
        List<EmoticonProject> allLogs = emoticonRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        Set<String> seenFiles = new HashSet<>();

        return allLogs.stream()
                .filter(log -> seenFiles.add(log.getOriginalFileName()))
                .collect(Collectors.toList());
    }

    /**
     * 프로젝트 목록 요약 조회 (그룹화 및 통계 계산)
     */
    public List<ProjectSummaryDto> getUserProjectList(String userId) {
        List<EmoticonProject> allLogs = emoticonRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return allLogs.stream()
                .collect(Collectors.groupingBy(EmoticonProject::getProjectId))
                .entrySet().stream()
                .map(entry -> {
                    List<EmoticonProject> projectFiles = entry.getValue();
                    Set<String> seen = new HashSet<>();
                    List<EmoticonProject> latestFiles = projectFiles.stream()
                            .filter(f -> seen.add(f.getOriginalFileName()))
                            .collect(Collectors.toList());

                    return ProjectSummaryDto.builder()
                            .projectId(entry.getKey())
                            .createdAt(latestFiles.get(0).getCreatedAt())
                            .totalCount(latestFiles.size())
                            .successCount(latestFiles.stream().filter(f -> "SUCCESS".equals(f.getStatus())).count())
                            .type(latestFiles.get(0).getEmoticonType())
                            .build();
                })
                .sorted(Comparator.comparing(ProjectSummaryDto::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    /**
     * DB 저장 및 DTO 매핑
     */
    private EmoticonResponseDto saveValidationResult(String userId, String projectId, String fileId, String fileName, String type, List<ValidationError> errors) {
        String status = errors.isEmpty() ? "SUCCESS" : "FAILED";

        // DB 저장용 에러 코드 문자열 (Comma Separated)
        String dbErrorCodes = errors.stream()
                .map(ValidationError::getCode)
                .collect(Collectors.joining(","));

        // 클라이언트 표시용 메시지 (Newline Separated)
        String frontendMessage = errors.stream()
                .map(ValidationError::getMessage)
                .collect(Collectors.joining("\n"));

        EmoticonProject project = EmoticonProject.builder()
                .userId(userId)
                .projectId(projectId)
                .fileId(fileId)
                .emoticonType(type)
                .originalFileName(fileName)
                .status(status)
                .errorMessage(dbErrorCodes)
                .createdAt(LocalDateTime.now())
                .build();

        emoticonRepository.save(project);

        return EmoticonResponseDto.builder()
                .fileId(fileId)
                .fileName(fileName)
                .status(status)
                .errorMessage(frontendMessage)
                .build();
    }
}