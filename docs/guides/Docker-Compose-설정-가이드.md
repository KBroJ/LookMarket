# Docker Compose 설정 가이드

> LookMarket 프로젝트의 `docker/docker-compose.yml` 상세 설명
>
> 각 서비스의 역할과 설정 항목을 초보자도 이해할 수 있도록 자세히 설명합니다.

---

## 📋 목차

1. [Docker Compose란?](#docker-compose란)
2. [전체 구조 개요](#전체-구조-개요)
3. [서비스별 상세 설명](#서비스별-상세-설명)
4. [볼륨(Volumes) 설명](#볼륨-설명)
5. [네트워크(Networks) 설명](#네트워크-설명)
6. [유용한 명령어](#유용한-명령어)
7. [문제 해결](#문제-해결)

---

## Docker Compose란?

**Docker Compose**는 여러 개의 Docker 컨테이너를 **하나의 설정 파일**로 관리하는 도구입니다.

### 왜 사용하나?

**Docker Compose 없이**:
```bash
# MySQL 실행
docker run -d --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root1234 mysql:8.0

# Redis 실행
docker run -d --name redis -p 6379:6379 redis:7-alpine

# Elasticsearch 실행
docker run -d --name elasticsearch -p 9200:9200 -e "discovery.type=single-node" elasticsearch:8.11.0

# ... 7개 서비스를 일일이 실행 😱
```

**Docker Compose 사용**:
```bash
# 한 번에 7개 서비스 실행!
docker-compose up -d
```

---

## 전체 구조 개요

### 파일 위치
```
lookmarket/
└── docker/
    └── docker-compose.yml  ← 이 파일
```

### 실행 중인 서비스 (7개)

| 서비스 | 이미지 | 포트 | 역할 |
|--------|--------|------|------|
| **mysql** | mysql:8.0 | 3306 | 주 데이터베이스 |
| **redis** | redis:7-alpine | 6379 | 캐시 + 분산 락 |
| **elasticsearch** | elasticsearch:8.11.0 | 9200, 9300 | 검색 엔진 |
| **zookeeper** | cp-zookeeper:7.5.0 | 2181 | Kafka 조정 서비스 |
| **kafka** | cp-kafka:7.5.0 | 9092 | 이벤트 스트리밍 |
| **kafka-connect** | debezium/connect:2.5 | 8083 | CDC (데이터 변경 캡처) |
| **kafka-ui** | kafka-ui:latest | 8989 | Kafka 모니터링 |

### 의존성 관계

```
kafka-ui → kafka → zookeeper
                ↓
kafka-connect → mysql
```

---

## 서비스별 상세 설명

## 1. MySQL 8.0

```yaml
mysql:
  image: mysql:8.0
  container_name: lookmarket-mysql
  restart: unless-stopped
  ports:
    - "3306:3306"
  environment:
    MYSQL_ROOT_PASSWORD: root1234
    MYSQL_DATABASE: lookmarket
    MYSQL_USER: lookmarket
    MYSQL_PASSWORD: lookmarket1234
    TZ: Asia/Seoul
  volumes:
    - mysql-data:/var/lib/mysql
    - ./init-db:/docker-entrypoint-initdb.d
  command:
    - --character-set-server=utf8mb4
    - --collation-server=utf8mb4_unicode_ci
    - --default-authentication-plugin=mysql_native_password
    - --binlog-format=ROW
    - --binlog-row-image=FULL
    - --server-id=1
  networks:
    - lookmarket-network
```

### 주요 설정 설명

#### `image: mysql:8.0`
- Docker Hub에서 MySQL 8.0 공식 이미지 사용

#### `container_name: lookmarket-mysql`
- 컨테이너 이름을 `lookmarket-mysql`로 지정
- `docker ps`에서 이 이름으로 표시됨

#### `restart: unless-stopped`
- 컨테이너가 멈추면 자동으로 재시작
- 개발자가 명시적으로 중지(`docker stop`)하기 전까지 계속 실행

#### `ports: - "3306:3306"`
- **포트 매핑**: 호스트의 3306 포트 → 컨테이너의 3306 포트
- `localhost:3306`으로 접속 가능

**포트 형식**: `"호스트포트:컨테이너포트"`

#### `environment:` - 환경 변수

| 환경 변수 | 값 | 의미 |
|----------|-----|------|
| `MYSQL_ROOT_PASSWORD` | root1234 | root 계정 비밀번호 |
| `MYSQL_DATABASE` | lookmarket | 자동 생성할 데이터베이스명 |
| `MYSQL_USER` | lookmarket | 생성할 사용자명 |
| `MYSQL_PASSWORD` | lookmarket1234 | 사용자 비밀번호 |
| `TZ` | Asia/Seoul | 타임존 설정 (한국 시간) |

**접속 정보**:
```bash
호스트: localhost:3306
사용자: lookmarket
비밀번호: lookmarket1234
데이터베이스: lookmarket
```

#### `volumes:` - 데이터 영구 저장

```yaml
- mysql-data:/var/lib/mysql
```
- **mysql-data**: Docker 볼륨 (데이터 저장소)
- **/var/lib/mysql**: MySQL 데이터 디렉토리
- 컨테이너를 삭제해도 데이터는 보존됨!

```yaml
- ./init-db:/docker-entrypoint-initdb.d
```
- **./init-db**: 호스트의 init-db 폴더
- **docker-entrypoint-initdb.d**: MySQL 초기화 스크립트 위치
- 이 폴더에 .sql 파일을 넣으면 컨테이너 시작 시 자동 실행

#### `command:` - MySQL 실행 옵션

| 옵션 | 의미 |
|------|------|
| `--character-set-server=utf8mb4` | 문자셋: UTF-8 (이모지 지원) |
| `--collation-server=utf8mb4_unicode_ci` | 정렬 규칙 |
| `--default-authentication-plugin=mysql_native_password` | 인증 방식 (Spring Boot 호환) |
| `--binlog-format=ROW` | **바이너리 로그 포맷**: ROW (CDC 필수) |
| `--binlog-row-image=FULL` | **행 이미지**: FULL (변경 전후 모두 기록) |
| `--server-id=1` | 서버 ID (복제/CDC용) |

**중요**: `binlog-format=ROW`는 **Debezium CDC**를 위한 설정입니다.

---

## 2. Redis 7.x

```yaml
redis:
  image: redis:7-alpine
  container_name: lookmarket-redis
  restart: unless-stopped
  ports:
    - "6379:6379"
  command: redis-server --appendonly yes
  volumes:
    - redis-data:/data
  networks:
    - lookmarket-network
```

### 주요 설정 설명

#### `image: redis:7-alpine`
- **alpine**: 경량화된 리눅스 배포판 (이미지 크기 작음)
- Redis 7 버전 사용

#### `command: redis-server --appendonly yes`
- **appendonly yes**: AOF(Append Only File) 활성화
- 데이터를 디스크에 저장하여 재시작 시에도 복구 가능

**AOF란?**
- Redis의 모든 쓰기 명령을 파일에 기록
- 메모리에만 저장하는 것보다 안전

#### `volumes: - redis-data:/data`
- Redis 데이터를 영구 저장
- 컨테이너 재시작 시에도 캐시 데이터 유지

---

## 3. Elasticsearch 8.x

```yaml
elasticsearch:
  image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
  container_name: lookmarket-elasticsearch
  restart: unless-stopped
  ports:
    - "9200:9200"
    - "9300:9300"
  environment:
    - discovery.type=single-node
    - xpack.security.enabled=false
    - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    - cluster.name=lookmarket-cluster
    - node.name=lookmarket-node
  volumes:
    - elasticsearch-data:/usr/share/elasticsearch/data
  networks:
    - lookmarket-network
```

### 주요 설정 설명

#### `ports:`
- **9200**: HTTP API 포트 (REST API)
- **9300**: 노드 간 통신 포트 (클러스터링용)

#### `environment:` - 환경 변수

| 환경 변수 | 값 | 의미 |
|----------|-----|------|
| `discovery.type` | single-node | 단일 노드 모드 (개발용) |
| `xpack.security.enabled` | false | 보안 기능 비활성화 (개발용) |
| `ES_JAVA_OPTS` | -Xms512m -Xmx512m | 힙 메모리: 최소/최대 512MB |
| `cluster.name` | lookmarket-cluster | 클러스터 이름 |
| `node.name` | lookmarket-node | 노드 이름 |

**메모리 설정 이유**:
- Elasticsearch는 기본적으로 많은 메모리 사용
- 개발 환경에서는 512MB로 제한하여 시스템 부담 감소

**주의**: 프로덕션에서는 `xpack.security.enabled=true` 권장!

---

## 4. Zookeeper

```yaml
zookeeper:
  image: confluentinc/cp-zookeeper:7.5.0
  container_name: lookmarket-zookeeper
  restart: unless-stopped
  ports:
    - "2181:2181"
  environment:
    ZOOKEEPER_CLIENT_PORT: 2181
    ZOOKEEPER_TICK_TIME: 2000
  volumes:
    - zookeeper-data:/var/lib/zookeeper/data
    - zookeeper-logs:/var/lib/zookeeper/log
  networks:
    - lookmarket-network
```

### Zookeeper란?

**Kafka의 조정자(Coordinator)**:
- Kafka 브로커 관리
- 토픽 메타데이터 저장
- 리더 선출

**Kafka 없이는 사용 안 함**: Kafka 전용 서비스

### 주요 설정 설명

#### `ZOOKEEPER_CLIENT_PORT: 2181`
- Kafka가 Zookeeper에 접속하는 포트

#### `ZOOKEEPER_TICK_TIME: 2000`
- 기본 시간 단위: 2000ms (2초)
- 하트비트 간격 등에 사용

---

## 5. Kafka

```yaml
kafka:
  image: confluentinc/cp-kafka:7.5.0
  container_name: lookmarket-kafka
  restart: unless-stopped
  depends_on:
    - zookeeper
  ports:
    - "9092:9092"
    - "29092:29092"
  environment:
    KAFKA_BROKER_ID: 1
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
    KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
    KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
    KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'
  volumes:
    - kafka-data:/var/lib/kafka/data
  networks:
    - lookmarket-network
```

### 주요 설정 설명

#### `depends_on: - zookeeper`
- **의존성 선언**: Zookeeper가 먼저 시작된 후 Kafka 시작
- Kafka는 Zookeeper 없이 동작 불가

#### `ports:`
- **9092**: 외부 접속 포트 (호스트에서 접속)
- **29092**: 내부 접속 포트 (다른 컨테이너에서 접속)

#### `environment:` - Kafka 설정

| 환경 변수 | 값 | 의미 |
|----------|-----|------|
| `KAFKA_BROKER_ID` | 1 | 브로커 고유 ID |
| `KAFKA_ZOOKEEPER_CONNECT` | zookeeper:2181 | Zookeeper 접속 주소 |
| `KAFKA_ADVERTISED_LISTENERS` | (복잡) | 클라이언트에게 알릴 주소 |
| `KAFKA_AUTO_CREATE_TOPICS_ENABLE` | true | 토픽 자동 생성 허용 |
| `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR` | 1 | 복제 계수 (단일 브로커) |

**KAFKA_ADVERTISED_LISTENERS 설명**:
```
PLAINTEXT://kafka:29092        ← 컨테이너 내부에서 접속
PLAINTEXT_HOST://localhost:9092 ← 호스트(Windows)에서 접속
```

**Spring Boot 설정에서**:
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092  # 호스트에서 접속
```

---

## 6. Kafka Connect (Debezium)

```yaml
kafka-connect:
  image: debezium/connect:2.5
  container_name: lookmarket-kafka-connect
  restart: unless-stopped
  depends_on:
    - kafka
    - mysql
  ports:
    - "8083:8083"
  environment:
    BOOTSTRAP_SERVERS: kafka:29092
    GROUP_ID: 1
    CONFIG_STORAGE_TOPIC: connect_configs
    OFFSET_STORAGE_TOPIC: connect_offsets
    STATUS_STORAGE_TOPIC: connect_status
    CONFIG_STORAGE_REPLICATION_FACTOR: 1
    OFFSET_STORAGE_REPLICATION_FACTOR: 1
    STATUS_STORAGE_REPLICATION_FACTOR: 1
  networks:
    - lookmarket-network
```

### Kafka Connect란?

**CDC (Change Data Capture)** 도구:
- MySQL 데이터 변경을 자동으로 감지
- 변경 내용을 Kafka 토픽으로 전송
- Elasticsearch와 자동 동기화

**예시**:
```
MySQL에서 상품 가격 변경
  ↓ (Debezium이 감지)
Kafka 토픽에 이벤트 발행
  ↓ (Consumer가 소비)
Elasticsearch 인덱스 업데이트
```

### 주요 설정 설명

#### `depends_on:`
```yaml
- kafka
- mysql
```
- Kafka와 MySQL이 먼저 시작되어야 함

#### `BOOTSTRAP_SERVERS: kafka:29092`
- Kafka 연결 주소 (컨테이너 내부 주소 사용)

#### 토픽 설정
- `connect_configs`: Connector 설정 저장
- `connect_offsets`: 오프셋 저장 (어디까지 읽었는지)
- `connect_status`: 상태 정보 저장

---

## 7. Kafka UI

```yaml
kafka-ui:
  image: provectuslabs/kafka-ui:latest
  container_name: lookmarket-kafka-ui
  restart: unless-stopped
  depends_on:
    - kafka
  ports:
    - "8989:8080"
  environment:
    KAFKA_CLUSTERS_0_NAME: lookmarket-cluster
    KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
    KAFKA_CLUSTERS_0_ZOOKEEPER: zookeeper:2181
  networks:
    - lookmarket-network
```

### Kafka UI란?

**웹 기반 Kafka 모니터링 도구**:
- 브라우저에서 `http://localhost:8989` 접속
- 토픽, 메시지, 컨슈머 그룹 등을 GUI로 확인

**주요 기능**:
- 토픽 목록 조회
- 메시지 내용 확인
- 컨슈머 그룹 상태 모니터링
- 파티션 정보 확인

### 주요 설정 설명

#### `ports: - "8989:8080"`
- 컨테이너 내부 8080 포트를 호스트의 8989 포트로 매핑
- **왜 8989?** 8080은 Spring Boot가 사용하므로 충돌 방지

#### `KAFKA_CLUSTERS_0_*`
- **0**: 첫 번째 클러스터 (여러 클러스터 연결 가능)
- `NAME`: lookmarket-cluster
- `BOOTSTRAPSERVERS`: Kafka 주소
- `ZOOKEEPER`: Zookeeper 주소

---

## 볼륨 설명

```yaml
volumes:
  mysql-data:
  redis-data:
  elasticsearch-data:
  zookeeper-data:
  zookeeper-logs:
  kafka-data:
```

### 볼륨이란?

**컨테이너 데이터를 영구 저장하는 공간**

### 왜 필요한가?

**볼륨 없이**:
```
컨테이너 생성 → 데이터 저장 → 컨테이너 삭제
                              ↓
                        데이터도 삭제됨 😱
```

**볼륨 사용**:
```
컨테이너 생성 → 볼륨에 데이터 저장 → 컨테이너 삭제
                                    ↓
                              데이터는 유지! ✅
```

### 각 볼륨의 역할

| 볼륨 | 저장 내용 |
|------|----------|
| `mysql-data` | MySQL 데이터베이스 파일 |
| `redis-data` | Redis AOF 파일 (데이터 복구용) |
| `elasticsearch-data` | Elasticsearch 인덱스 데이터 |
| `zookeeper-data` | Zookeeper 메타데이터 |
| `zookeeper-logs` | Zookeeper 로그 |
| `kafka-data` | Kafka 토픽 데이터 |

### 볼륨 관리 명령어

```bash
# 볼륨 목록 조회
docker volume ls

# 특정 볼륨 상세 정보
docker volume inspect mysql-data

# 볼륨 삭제 (주의: 데이터 손실!)
docker volume rm mysql-data

# 사용하지 않는 볼륨 모두 삭제
docker volume prune
```

---

## 네트워크 설명

```yaml
networks:
  lookmarket-network:
    driver: bridge
```

### 네트워크란?

**컨테이너 간 통신을 위한 가상 네트워크**

### 왜 필요한가?

**네트워크 없이**:
```
컨테이너들이 서로를 찾을 수 없음 😱
```

**네트워크 사용**:
```
lookmarket-network 안에서:
  - kafka가 zookeeper:2181로 접속 ✅
  - kafka-connect가 kafka:29092로 접속 ✅
```

### bridge 드라이버란?

**기본 네트워크 드라이버**:
- 모든 컨테이너가 동일한 가상 네트워크에 연결
- 컨테이너명으로 서로 접근 가능 (DNS 자동 설정)

**예시**:
```yaml
# Kafka 설정
KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
                         ↑
                   컨테이너명으로 접속!
```

---

## 유용한 명령어

### 기본 명령어

```bash
# 모든 서비스 시작 (백그라운드)
docker-compose up -d

# 모든 서비스 중지
docker-compose down

# 모든 서비스 중지 + 볼륨 삭제 (데이터 손실 주의!)
docker-compose down -v

# 실행 중인 컨테이너 확인
docker-compose ps
docker ps
```

### 로그 확인

```bash
# 모든 서비스 로그
docker-compose logs

# 특정 서비스 로그
docker-compose logs mysql
docker-compose logs kafka

# 실시간 로그 확인 (tail -f)
docker-compose logs -f elasticsearch

# 최근 100줄만 보기
docker-compose logs --tail=100 kafka
```

### 서비스 재시작

```bash
# 특정 서비스만 재시작
docker-compose restart mysql
docker-compose restart kafka

# 모든 서비스 재시작
docker-compose restart
```

### 서비스 중지/시작

```bash
# 특정 서비스만 중지
docker-compose stop mysql

# 특정 서비스만 시작
docker-compose start mysql

# 특정 서비스 제거 (중지 + 삭제)
docker-compose rm -s mysql
```

### 컨테이너 내부 접속

```bash
# MySQL 쉘 접속
docker exec -it lookmarket-mysql mysql -u lookmarket -plookmarket1234

# Redis CLI 접속
docker exec -it lookmarket-redis redis-cli

# Bash 쉘 접속
docker exec -it lookmarket-mysql bash
```

---

## 문제 해결

### 문제 1: "port is already allocated" 에러

**증상**:
```
Error: bind: address already in use
```

**원인**: 해당 포트를 다른 프로그램이 사용 중

**해결**:
```bash
# Windows - 포트 사용 확인
netstat -ano | findstr :3306

# 프로세스 종료 (관리자 권한)
taskkill /PID <PID번호> /F
```

### 문제 2: Elasticsearch가 시작되지 않음

**증상**:
```
max virtual memory areas vm.max_map_count [65530] is too low
```

**원인**: Linux 커널 파라미터 부족

**해결 (Windows WSL2)**:
```bash
wsl -d docker-desktop
sysctl -w vm.max_map_count=262144
```

### 문제 3: Kafka가 시작되지 않음

**원인**: Zookeeper가 준비되지 않음

**해결**:
```bash
# Zookeeper 로그 확인
docker-compose logs zookeeper

# Zookeeper만 먼저 시작 후 Kafka 시작
docker-compose up -d zookeeper
# 10초 대기
docker-compose up -d kafka
```

### 문제 4: "No space left on device" 에러

**원인**: Docker 디스크 용량 부족

**해결**:
```bash
# 사용하지 않는 이미지/컨테이너/볼륨 삭제
docker system prune -a

# 볼륨만 정리
docker volume prune
```

### 문제 5: 컨테이너가 계속 재시작됨

**확인**:
```bash
# 로그 확인
docker-compose logs <서비스명>

# 상태 확인
docker-compose ps
```

**일반적인 원인**:
- 설정 오류 (환경 변수 등)
- 메모리 부족
- 의존 서비스 미실행

---

## 설정 커스터마이징

### 포트 변경

```yaml
# 예: MySQL 포트를 3307로 변경
ports:
  - "3307:3306"  # 호스트:컨테이너
```

### 메모리 제한

```yaml
# Elasticsearch 메모리 증가
environment:
  - "ES_JAVA_OPTS=-Xms1g -Xmx1g"  # 512MB → 1GB
```

### 볼륨을 호스트 경로로 변경

```yaml
# Named Volume (현재)
volumes:
  - mysql-data:/var/lib/mysql

# Host Path (변경)
volumes:
  - ./data/mysql:/var/lib/mysql  # 상대 경로
  - C:/data/mysql:/var/lib/mysql # 절대 경로 (Windows)
```

---

## 참고 자료

### 공식 문서
- [Docker Compose 문서](https://docs.docker.com/compose/)
- [MySQL Docker Hub](https://hub.docker.com/_/mysql)
- [Redis Docker Hub](https://hub.docker.com/_/redis)
- [Elasticsearch 가이드](https://www.elastic.co/guide/en/elasticsearch/reference/current/docker.html)
- [Confluent Kafka 가이드](https://docs.confluent.io/platform/current/installation/docker/installation.html)
- [Debezium 문서](https://debezium.io/documentation/reference/stable/tutorial.html)

### 관련 프로젝트 문서
- [Phase 0 환경 검증 완료 보고서](./Phase0-환경검증-완료보고서.md)
- [멀티 모듈 아키텍처 가이드](./멀티모듈-아키텍처-가이드.md)
- [CLAUDE.md](../CLAUDE.md)

---

## 정리

### 핵심 개념

1. **Docker Compose**: 여러 컨테이너를 한 번에 관리
2. **Services**: 각 컨테이너 정의 (mysql, redis, kafka 등)
3. **Volumes**: 데이터 영구 저장
4. **Networks**: 컨테이너 간 통신
5. **depends_on**: 서비스 시작 순서 제어

### 실행 순서

1. Zookeeper 시작
2. Kafka 시작 (Zookeeper 의존)
3. MySQL, Redis, Elasticsearch 시작 (독립적)
4. Kafka Connect 시작 (Kafka + MySQL 의존)
5. Kafka UI 시작 (Kafka 의존)

### 주요 명령어 요약

| 작업 | 명령어 |
|------|--------|
| 전체 시작 | `docker-compose up -d` |
| 전체 중지 | `docker-compose down` |
| 로그 확인 | `docker-compose logs -f <서비스>` |
| 재시작 | `docker-compose restart <서비스>` |
| 상태 확인 | `docker-compose ps` |

---

**작성일**: 2025-12-16
**작성자**: LookMarket 개발팀
**버전**: 1.0
