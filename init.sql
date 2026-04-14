-- =====================================================================
-- init.sql - 데이터베이스 초기화 스크립트
-- =====================================================================
-- 클라이언트 연결 인코딩을 UTF-8로 강제 설정합니다.
-- 이 줄이 없으면 Docker init 스크립트가 latin1로 연결되어
-- 한글이 이중 인코딩(mojibake)되어 저장됩니다.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
-- =====================================================================
-- Docker Compose 실행 시 MySQL 컨테이너가 처음 시작될 때 자동으로 실행됩니다.
-- 위치: /docker-entrypoint-initdb.d/ 에 마운트됩니다.
--
-- 주의: 이 스크립트는 컨테이너의 데이터 볼륨이 비어있을 때만 실행됩니다.
--       기존 데이터가 있는 볼륨에서는 다시 실행되지 않습니다.
-- =====================================================================

-- 데이터베이스 생성 (이미 존재하면 무시)
CREATE DATABASE IF NOT EXISTS productdb
    CHARACTER SET utf8mb4      -- 한글, 이모지 등 모든 유니코드 지원
    COLLATE utf8mb4_unicode_ci; -- 대소문자 구분 없는 정렬 (ci: case-insensitive)

-- 애플리케이션 전용 사용자 생성 (root 계정 직접 사용 지양)
-- '%': 모든 호스트에서 접속 허용 (Docker 네트워크 내부 접속용)
CREATE USER IF NOT EXISTS 'appuser'@'%' IDENTIFIED BY 'apppass';

-- appuser에게 productdb에 대한 모든 권한 부여
GRANT ALL PRIVILEGES ON productdb.* TO 'appuser'@'%';

-- 권한 변경사항 즉시 반영
FLUSH PRIVILEGES;

-- productdb 데이터베이스 사용
USE productdb;

-- =====================================================================
-- 상품 테이블 생성
-- =====================================================================
-- JPA @Entity Product 클래스의 필드와 매핑됩니다.
-- DbConfig에서 hibernate.hbm2ddl.auto=validate 설정으로
-- Hibernate가 이 테이블과 엔티티 클래스의 매핑을 검증합니다.
-- =====================================================================
-- 카테고리 테이블 생성 (추가됨)
-- =====================================================================
CREATE TABLE category (
    category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS product (
    -- AUTO_INCREMENT: INSERT 시 자동으로 1씩 증가하는 기본 키
    -- JPA의 @GeneratedValue(strategy = GenerationType.IDENTITY)와 매핑
    id          BIGINT          NOT NULL AUTO_INCREMENT,

    -- VARCHAR: 가변 길이 문자열 (최대 100자)
    -- JPA의 @Column(nullable = false, length = 100)과 매핑
    name        VARCHAR(100)    NOT NULL,

    -- 카테고리: NULL 허용
    category    VARCHAR(50)     NULL,

    -- DECIMAL(10, 2): 전체 10자리, 소수점 2자리
    -- JPA의 @Column(precision = 10, scale = 2)와 매핑
    price       DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,

    -- TEXT: 긴 텍스트 저장 (최대 65,535 바이트)
    -- JPA의 @Lob과 매핑
    description TEXT            NULL,

    PRIMARY KEY (id)

) ENGINE=InnoDB              -- InnoDB: 트랜잭션, 외래 키 지원 (MySQL 기본 엔진)
  DEFAULT CHARSET=utf8mb4    -- 한글 지원
  COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 샘플 데이터 삽입
-- =====================================================================
-- 애플리케이션 시작 시 테스트용 데이터를 미리 넣어둡니다.

INSERT INTO product (name, category, price, description) VALUES
('Apple MacBook Pro 14인치',
 '전자제품',
 2990000,
 'M3 Pro 칩 탑재, 18GB 유니파이드 메모리, 512GB SSD.\n전문가를 위한 고성능 노트북입니다.'),

('삼성 갤럭시 S24 Ultra',
 '전자제품',
 1550000,
 '200MP 카메라, AI 기반 사진 처리, S펜 내장.\n최신 안드로이드 플래그십 스마트폰입니다.'),

('스프링 부트 실전 활용 마스터',
 '도서',
 38000,
 '김영한 저. JPA, Spring Data, QueryDSL 등 실무 필수 기술을 다루는 베스트셀러입니다.'),

('나이키 에어맥스 270',
 '스포츠',
 149000,
 '270도 에어 쿠셔닝 시스템으로 최고의 편안함을 제공합니다.\n다양한 컬러로 출시되었습니다.'),

('비비고 왕교자 만두 1.2kg',
 '식품',
 12900,
 '속재료가 꽉 찬 프리미엄 교자 만두. 에어프라이어, 찜, 구이 모두 가능합니다.'),

('무신사 스탠다드 오버핏 맨투맨',
 '의류',
 39000,
 '면 100% 소재의 편안한 오버핏 맨투맨. 봄/가을 활용도 높은 베이직 아이템입니다.');

-- 삽입 확인 쿼리 (로그에서 확인용)
SELECT CONCAT('샘플 데이터 ', COUNT(*), '개 삽입 완료') AS result FROM product;
