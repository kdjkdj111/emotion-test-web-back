package com.kdd.emoticon_test_web_back.repository;

import com.kdd.emoticon_test_web_back.domain.EmoticonProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmoticonRepository extends JpaRepository<EmoticonProject, Long> {
    List<EmoticonProject> findByUserId(String userId); //사용자 아이디로 조회
}
