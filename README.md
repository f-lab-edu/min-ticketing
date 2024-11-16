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
- `MySQL`, `Redis`, `ElasticSearch`
- `koTest`, `mockk`
- `Grafana`, `Prometheus`, `ELK Stack`

## 🏘️ 프로젝트 아키텍쳐

---

![image](https://github.com/user-attachments/assets/ad924393-37c1-4c12-aaa4-8b8fbfd594d2)



## 🤔 프로젝트 주요 관심사

---

- TDD 방법론 적용
- 대용량 트래픽 처리 테스트 및 가용성 향상
- Github 컨벤션 준수
- MySQL 쿼리 분석 및 튜닝
- 프로젝트 설계 변경에도 확장성 있는 코드 작성

## 💡 Technical Issues

---
- [공연 예매 로직에 Proxy를 적용해서 DB 부하 줄이기](https://minturtle.tistory.com/66)
- [내가 만든 Docker Image가 ARM 아키텍쳐에서 동작하지 않을 때](https://minturtle.tistory.com/65)
- [성능 최적화 1편 - 공연 정보 조회 API 쿼리 분석하고 개선하기](https://minturtle.tistory.com/64)
- [성능 최적화 2편 - 공연 조회 API 성능 측정 및 개선 사안 찾아보기](https://minturtle.tistory.com/69)
- [성능 최적화 3편 -공연 조회 API에 캐싱을 적용하고 성능 테스트하기](https://minturtle.tistory.com/70)
- [성능 최적화 4편 - 아키텍처 최적화, 비동기 로깅을 통한 공연 정보 조회 API 최적화 하기](https://minturtle.tistory.com/71)
- [Spring + Grafana Stack(Loki, Prometheus)으로 모니터링 시스템 구축하기](https://minturtle.tistory.com/63)
- [Spring + ELK 로 로그 시스템 구축하기](https://minturtle.tistory.com/68)
- [Spring Web MVC 비동기 메서드 호출 시 Request 정보를 유지하려면 어떻게 해야할까?](https://minturtle.tistory.com/62)
- [공연 상세 조회 시 발생한 N + 1 문제를 해결하고, 확장성 있는 코드로 변경하기](https://minturtle.tistory.com/57)
- [테스트 코드 개선하기, TDD와 BDD의 관계](https://minturtle.tistory.com/56)

  
## 📖 WIKI

---

위키 페이지는 분량상 README에 담지 못한 본 프로젝트의 기획 문서와 설계 문서를 포함하고 있습니다.

- [프로젝트 문서 바로가기](https://github.com/f-lab-edu/min-ticketing/wiki)

