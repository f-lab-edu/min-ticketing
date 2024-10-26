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
- `Grafana`, `Prometheus`, `Loki`, `Grafana Tempo`

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
- [공연 예매 로직에 Proxy를 적용해서 DB 부하 줄이기](https://velog.io/@minturtle_/%EA%B3%B5%EC%97%B0-%EC%98%88%EB%A7%A4-%EC%8B%9C-Proxy%EB%A5%BC-%EC%82%AC%EC%9A%A9%ED%95%B4-DB-%EB%B6%80%ED%95%98-%EC%A4%84%EC%9D%B4%EA%B8%B0)
- [내가 만든 Docker Image가 ARM 아키텍쳐에서 동작하지 않을 때](https://velog.io/@minturtle_/%EB%82%B4%EA%B0%80-%EB%A7%8C%EB%93%A0-Docker-Image%EA%B0%80-ARM-%EC%95%84%ED%82%A4%ED%85%8D%EC%B3%90%EC%97%90%EC%84%9C-%EC%8B%A4%ED%96%89%EB%90%98%EC%A7%80-%EC%95%8A%EC%9D%84-%EB%95%8C)
- [성능 최적화 1편 - 공연 정보 조회 API 개선하기](https://velog.io/@minturtle_/%EA%B3%B5%EC%97%B0-%EC%A0%95%EB%B3%B4-%EC%A1%B0%ED%9A%8C-API-%EA%B0%9C%EC%84%A0%ED%95%98%EA%B8%B0)
- [성능 최적화 2편 - 공연 정보 조회 API 병목 지점 파악하기](https://velog.io/@minturtle_/%EA%B3%B5%EC%97%B0-%EC%A0%95%EB%B3%B4-%EC%A1%B0%ED%9A%8C-API-%EB%B3%91%EB%AA%A9-%EC%A7%80%EC%A0%90-%ED%8C%8C%EC%95%85%ED%95%98%EA%B8%B0)
- [성능 최적화 3편 - 공연 정보 조회 API 스케일 업 하고 다시 테스트하기](https://velog.io/@minturtle_/%EA%B3%B5%EC%97%B0-%EC%A0%95%EB%B3%B4-%EC%A1%B0%ED%9A%8C-API-%EC%8A%A4%EC%BC%80%EC%9D%BC-%EC%97%85-%ED%95%98%EA%B3%A0-%EB%8B%A4%EC%8B%9C-%ED%85%8C%EC%8A%A4%ED%8A%B8%ED%95%98%EA%B8%B0)
- [성능 최적화 4편 - 공연 정보 조회 API에 캐싱 적용하기](https://velog.io/@minturtle_/%EA%B3%B5%EC%97%B0-%EC%A1%B0%ED%9A%8C-API%EC%97%90-%EC%BA%90%EC%8B%B1-%EC%A0%81%EC%9A%A9%ED%95%98%EA%B8%B0)
- [Spring + Grafana Stack(Loki, Prometheus)으로 모니터링 시스템 구축하기](https://velog.io/@minturtle_/Spring-Grafana-Loki-Prometheus-Tempo%EB%A1%9C-%EB%AA%A8%EB%8B%88%ED%84%B0%EB%A7%81-%EC%8B%9C%EC%8A%A4%ED%85%9C-%EA%B5%AC%EC%B6%95%ED%95%98%EA%B8%B0)
- [Spring Web MVC 비동기 메서드 호출 시 Request 정보를 유지하려면 어떻게 해야할까?](https://velog.io/@minturtle_/%EB%B9%84%EB%8F%99%EA%B8%B0-%ED%99%98%EA%B2%BD%EC%97%90%EC%84%9C-Request%EB%A5%BC-%EC%9C%A0%EC%A7%80%ED%95%98%EB%A0%A4%EB%A9%B4-%EC%96%B4%EB%96%BB%EA%B2%8C-%ED%95%B4%EC%95%BC%ED%95%A0%EA%B9%8C)
- [공연 상세 조회 시 발생한 N + 1 문제를 해결하고, 확장성 있는 코드로 변경하기](https://velog.io/@minturtle_/N-1-%EC%BF%BC%EB%A6%AC-%EA%B0%9C%EC%84%A0%ED%95%98%EA%B3%A0-%ED%99%95%EC%9E%A5%EC%84%B1-%EC%9E%88%EB%8A%94-%EC%BD%94%EB%93%9C-%EB%A7%8C%EB%93%A4%EA%B8%B0)
- [테스트 코드 개선하기, TDD와 BDD의 관계](https://velog.io/@minturtle_/%ED%85%8C%EC%8A%A4%ED%8A%B8-%EC%BD%94%EB%93%9C-%EA%B0%9C%EC%84%A0%ED%95%98%EA%B8%B0)
## 📖 WIKI

---

위키 페이지는 분량상 README에 담지 못한 본 프로젝트의 기획 문서와 설계 문서를 포함하고 있습니다.

- [프로젝트 문서 바로가기](https://github.com/f-lab-edu/min-ticketing/wiki)

