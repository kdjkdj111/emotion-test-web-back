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

    public EmoticonProject processEmoticon(String userId, MultipartFile file, String type, String fileId) {
        List<String> errorMessages = new ArrayList<>();
        BufferedImage image = null;

        //검증
        try {
            //png 파일 검증
            if (!checkMimeType(file)) {
                errorMessages.add(ValidationCode.INVALID_MIME.getMessage());
            }

            //이미지 손상 검증
            image = ImageIO.read(file.getInputStream());
            if (image == null) {
                // 파일은 있는데 이미지로 못 읽는 경우 (손상된 파일 등)
                errorMessages.add(ValidationCode.FILE_READ_ERROR.getMessage());
            }

            //타입별 검증 로직
            switch (type) {
                case "STILL":
                    validateStillEmoticon(image, file, errorMessages);
                    break;
                case "MINI":
                    validateMiniEmoticon(image, file, errorMessages);
                    break;
                case "ANIMATED":
                    validateAnimatedEmoticon(image, file, errorMessages);
                    break;
                default:
                    errorMessages.add("알 수 없는 이모티콘 타입입니다: " + type);
            }

        } catch (IOException e) {
            log.error("파일 처리 중 오류 발생", e);
            errorMessages.add(ValidationCode.FILE_READ_ERROR.getMessage());
        }

        return saveValidationResult(userId,fileId, file.getOriginalFilename(),type, errorMessages);
    }

    //타입별 로직 묶음
    private void validateStillEmoticon(BufferedImage image, MultipartFile file, List<String> errorMessages) {
        checkDimensions(image, 360, 360, errorMessages);
        checkFileSize(file, 150, errorMessages); // 150KB
        // 필요 시 픽셀 검사 추가 가능
        // if (!checkPixels(image)) errorMessages.add("반투명 픽셀 오류...");
    }
    private void validateMiniEmoticon(BufferedImage image, MultipartFile file, List<String> errorMessages) {
        checkDimensions(image, 144, 144, errorMessages);
        checkFileSize(file, 100, errorMessages); // 미니는 더 작게 (예시)
    }
    private void validateAnimatedEmoticon(BufferedImage image, MultipartFile file, List<String> errorMessages) {
        checkDimensions(image, 360, 360, errorMessages);
        checkFileSize(file, 2000, errorMessages); // GIF는 용량이 큼 (2MB 예시)
        //GIF 프레임 수 검사 로직 추가 예정
    }


    //각종 검사 로직
    private boolean checkMimeType(MultipartFile file) throws IOException {
        String mimeType = tika.detect(file.getInputStream());
        return "image/png".equals(mimeType);
    }

    private void checkDimensions(BufferedImage image, int targetW, int targetH, List<String> errorMessages) {
        int width = image.getWidth();
        int height = image.getHeight();

        if (width != targetW || height != targetH) {
            log.warn("규격 불일치: {}x{} (기대: {}x{})", width, height, targetW, targetH);
            errorMessages.add("규격 불일치: 현재 " + width + "x" + height + "px (권장: " + targetW + "x" + targetH + "px)");
        }
    }

    private void checkFileSize(MultipartFile file, long maxKB, List<String> errorMessages) {
        long fileSizeInBytes = file.getSize();
        double fileSizeInKB = fileSizeInBytes / 1024.0;
        long maxSizeInBytes = maxKB * 1024;

        if (fileSizeInBytes > maxSizeInBytes) {
            String formattedSize = String.format("%.1f", fileSizeInKB);
            errorMessages.add("용량 초과: 현재 " + formattedSize + "KB (제한: " + maxKB + "KB)");
        }
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

    private EmoticonProject saveValidationResult(String userId, String fileId, String fileName, String type, List<String> errors) {
        String status = errors.isEmpty() ? "SUCCESS" : "FAILED";
        // 여러 에러 메시지를 줄바꿈(\n)으로 합칩니다.
        String detailedMessage = String.join("\n", errors);

        EmoticonProject project = EmoticonProject.builder()
                .userId(userId)
                .fileId(fileId)
                .emoticonType(type)
                .originalFileName(fileName)
                .status(status)
                .errorMessage(detailedMessage)
                .createdAt(LocalDateTime.now())
                .build();

        return emoticonRepository.save(project);
    }
}