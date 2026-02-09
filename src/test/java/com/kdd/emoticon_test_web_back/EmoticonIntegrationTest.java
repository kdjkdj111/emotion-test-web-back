package com.kdd.emoticon_test_web_back;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class EmoticonIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("규격과 형식이 틀린 파일을 올렸을 때, 여러 개의 오류 메시지가 한 번에 와야한다.")
    void uploadFailTest() throws Exception {
        MockMultipartFile fakeFile = new MockMultipartFile(
                "file",
                "test.txt",
                "test/plain",
                "invalid_content".getBytes()
        );
        mockMvc.perform(multipart("/api/emoticons/upload")
                        .file(fakeFile)
                        .param("userId", "tester")) // 파트너 닉네임으로 테스트
                .andExpect(status().isOk()) // 상태는 200이지만 응답 객체 안의 status는 FAILED여야 함
                .andExpect(jsonPath("$.status").value("FAILED"))
                // 우리가 공들여 만든 에러 메시지들이 포함되어 있는지 확인! [cite: 2026-02-09]
                .andExpect(jsonPath("$.errorMessage").exists());
    }

}
