# Min-Ticketing

---

#### 01. 소개

> Min-Ticketing은 온라인 공연 좌석 예매 티케팅 서버 프로젝트입니다.

#### 02. 기능

- 자체 로그인 및 회원가입(회원가입 시 이메일 인증)
- 공연 정보 및 좌석 정보 조회
- 공연 좌석 예매
- 결제(Toss Payments API 사용)

#### 03. 사용 기술

- `kotlin`
- `Spring Boot`, `Spring Data JPA`
- `MySQL`, `Redis`
- `koTest`, `mockk`

## 🏘️ 프로젝트 아키텍쳐

---
![image](https://github.com/user-attachments/assets/caf4e5d0-ed0e-40e6-800e-50636f3e530f)



## 🤔 프로젝트 주요 관심사

---

- TDD 방법론 적용
- 대용량 트래픽 처리 테스트 및 가용성 향상
- Github 컨벤션 준수
- MySQL 쿼리 분석 및 튜닝
- 프로젝트 설계 변경에도 확장성 있는 코드 작성

## 💡 Technical Issues

---
- [공연 예매 로직에 Proxy를 적용해서 DB 부하 줄이기](https://www.notion.so/Proxy-DB-127fc957389c80f392e4daac72e3f6f0?pvs=4)
- [내가 만든 Docker Image가 ARM 아키텍쳐에서 동작하지 않을 때](https://www.notion.so/Docker-Image-ARM-124fc957389c80559eeadd7a86764277?pvs=4)
- [성능 최적화 1편 - 공연 정보 조회 API 개선하기](https://www.notion.so/API-11cfc957389c805aa7f9e0a4d0c1480b?pvs=4)
- [성능 최적화 2편 - 공연 정보 조회 API 병목 지점 파악하기](https://www.notion.so/API-123fc957389c80c28f8de0335e8d59f4?pvs=4)
- [성능 최적화 3편 - 공연 정보 조회 API 스케일 업 하고 다시 테스트하기](https://www.notion.so/API-124fc957389c80d59024dcf742b9e890?pvs=4)
- [Spring + Grafana Stack(Loki, Prometheus, Tempo)으로 모니터링 시스템 구축하기](https://flannel-dill-7dc.notion.site/Spring-Loki-Grafana-110fc957389c80d69bc5d33a9b5c2618?pvs=4)
- [Spring Web MVC 비동기 메서드 호출 시 Request 정보를 유지하려면 어떻게 해야할까?](https://flannel-dill-7dc.notion.site/Request-10dfc957389c809ea4f0da9566ab90ba?pvs=4)
- [공연 상세 조회 시 발생한 N + 1 문제를 해결하고, 확장성 있는 코드로 변경하기](https://flannel-dill-7dc.notion.site/N-1-ad7f7737e89e4ffba6866650f7925de6?pvs=4)
- [테스트 코드 개선하기, TDD와 BDD의 관계](https://flannel-dill-7dc.notion.site/0d997311ea344437b6cae3cb63487d76?pvs=4)
## 📖 WIKI

---

위키 페이지는 분량상 README에 담지 못한 본 프로젝트의 기획 문서와 설계 문서를 포함하고 있습니다.

- [프로젝트 문서 바로가기](https://github.com/f-lab-edu/min-ticketing/wiki)

