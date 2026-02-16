# 🧠 Emoticon Lab - Back-end
> 이모티콘 이미지 분석 및 규격 검증 API 서버

프론트엔드에서 전송된 이미지 데이터를 메모리 상에서 즉시 분석하여 카카오 이모티콘 가이드라인 준수 여부를 판단합니다.

## ✨ 주요 기능
- **Image Analysis**: `ImageIO`를 이용한 이미지 메타데이터(해상도, 포맷) 추출
- **Validation Logic**: 정지형/미니 이모티콘 등 종류별 규격 검증 (진행 중)
- **CORS Configuration**: 로컬 개발 환경(Vite)과의 원활한 통신 지원
- **File Handling**: MultipartFile을 이용한 효율적인 데이터 처리

## 🛠 Tech Stack
- **Framework**: Spring Boot 4.x
- **Language**: Java
- **Tooling**: Lombok, Spring Web
- **Build**: Gradle

## 📍 핵심 API
- `POST /api/emoticons/upload`: 이미지 업로드 및 실시간 검증 결과 반환

## 🚧 Roadmap
- [ ] 이모티콘 종류별(미니, 움직이는 등) 검증 전략 패턴 적용
- [ ] 애니메이션 GIF 프레임 수 및 재생 시간 분석 로직 추가
