package com.kdd.emoticon_test_web_back.service;

import com.kdd.emoticon_test_web_back.constant.ValidationCode;
import com.kdd.emoticon_test_web_back.domain.EmoticonProject;
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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmoticonService {

    private final EmoticonRepository emoticonRepository;
    private final Tika tika = new Tika();

    public EmoticonProject processEmoticon(String userId, MultipartFile file) {
        List<String> errorMessages = new ArrayList<>();
        BufferedImage image = null;

        // 1. 보안 검증 (MIME Type)
        try {
            if (!checkMimeType(file)) {
                errorMessages.add(ValidationCode.INVALID_MIME.getMessage());
            }
        } catch (IOException e) {
            errorMessages.add(ValidationCode.FILE_READ_ERROR.getMessage());
        }

        // 2. 규격 및 이미지 기반 검증
        try {
            image = ImageIO.read(file.getInputStream());
            if (image == null) {
                errorMessages.add(ValidationCode.FILE_READ_ERROR.getMessage());
            } else {
                // 규격 검사
                if (image.getWidth() != 360 || image.getHeight() != 360) {
                    errorMessages.add(ValidationCode.INVALID_SIZE.getMessage());
                }
                // 픽셀(투명도) 검사
                if (!checkPixels(image)) {
                    errorMessages.add(ValidationCode.INVALID_OPACITY.getMessage());
                }
            }
        } catch (IOException e) {
            errorMessages.add(ValidationCode.FILE_READ_ERROR.getMessage());
        }

        EmoticonProject project = saveValidationResult(userId, file.getOriginalFilename(), errorMessages);

        // 3. 최종 결과 집계 및 DB 저장
        return emoticonRepository.save(project);
    }

    private boolean checkMimeType(MultipartFile file) throws IOException {
        String mimeType = tika.detect(file.getInputStream());
        return "image/png".equals(mimeType);
    }

    private boolean checkPixels(BufferedImage image) {
        boolean isValid = true;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xff;
                // 반투명 픽셀 검사 로직 (필요에 따라 기준 값 조정)
                if (alpha > 0 && alpha < 150) {
                    log.warn("부적절한 픽셀 발견: ({}, {}), Alpha: {}", x, y, alpha);
                    isValid = false;
                    // 모든 픽셀을 다 검사할지, 하나만 발견해도 종료할지는 기획에 따라 결정
                }
            }
        }
        return isValid;
    }

    private EmoticonProject saveValidationResult(String userId, String fileName, List<String> errors) {
        String status = errors.isEmpty() ? "SUCCESS" : "FAILED";
        // 여러 에러 메시지를 줄바꿈(\n)으로 합칩니다.
        String detailedMessage = String.join("\n", errors);

        EmoticonProject project = EmoticonProject.builder()
                .userId(userId)
                .originalFileName(fileName)
                .status(status)
                .errorMessage(detailedMessage) // Entity에 해당 필드 추가 필요
                .createdAt(LocalDateTime.now())
                .build();

        emoticonRepository.save(project);
        log.info("검증 완료 - 상태: {}, 메시지: {}", status, detailedMessage);

        return project;
    }
}