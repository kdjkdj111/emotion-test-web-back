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

        //사이즈 검증
        checkFileSize(file, errorMessages);

        // 1. 보안 검증 (MIME Type)
        try {
            if (!checkMimeType(file)) {
                errorMessages.add(ValidationCode.INVALID_MIME.getMessage());
            }
            if (errorMessages.isEmpty()) {
                image = ImageIO.read(file.getInputStream());

                if (image == null) {
                    // 파일은 있는데 이미지로 못 읽는 경우 (손상된 파일 등)
                    errorMessages.add(ValidationCode.FILE_READ_ERROR.getMessage());
                } else {
                    // 이미지가 정상적으로 로딩되었을 때만 규격을 잰다.
                    checkDimensions(image, errorMessages);
                }
            }
        } catch (IOException e) {
            errorMessages.add(ValidationCode.FILE_READ_ERROR.getMessage());
        }

        return saveValidationResult(userId, file.getOriginalFilename(), errorMessages);
    }

    private void checkDimensions(BufferedImage image, List<String> errorMessages) {
        int width = image.getWidth();
        int height = image.getHeight();

        if (width != 360 || height != 360) {
            log.warn("규격 불일치: {}x{}", width, height);
            errorMessages.add("규격 불일치: 현재 " + width + "x" + height + "px (권장: 360x360px)");
        }
    }

    private void checkFileSize(MultipartFile file, List<String> errorMessages) {
        long fileSizeInBytes = file.getSize();
        double fileSizeInKB = fileSizeInBytes / 1024.0; // 실수를 위해 1024.0으로 나눔
        long maxSizeInBytes = 150 * 1024;

        if (fileSizeInBytes > maxSizeInBytes) {
            // 소수점 첫째 자리까지 반올림해서 메시지 생성
            String formattedSize = String.format("%.1f", fileSizeInKB);
            log.warn("용량 초과: {} KB", formattedSize);
            errorMessages.add("용량 초과: 현재 " + formattedSize + "KB (제한: 150KB)");
        }
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
                if (alpha > 0 && alpha < 10) {
                    log.warn("부적절한 픽셀 발견: ({}, {}), Alpha: {}", x, y, alpha);
                    isValid = false;
                    break;
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

        return emoticonRepository.save(project);
    }
}