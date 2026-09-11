INSERT INTO victims (name, process_id, terminated_at, status) VALUES
('nginx', '1001', DATEADD('DAY', -1, CURRENT_TIMESTAMP), 'TERMINATED'),
('chrome', '2002', DATEADD('DAY', -1, CURRENT_TIMESTAMP), 'TERMINATED'),
('sleep', '3003', CURRENT_TIMESTAMP, 'RUNNING');

INSERT INTO orders (customer_id, order_datetime, status, shipping_id)
SELECT
    FLOOR(RAND() * 100 + 1),
    DATEADD('DAY', -CAST(RAND() * 30 AS INT), CURRENT_TIMESTAMP),
    CASE
        WHEN rn <= 10 THEN 'READY_FOR_SHIPMENT'
        WHEN rn <= 20 THEN 'SHIPPED'
        ELSE 'CANCELLED'
    END,
    CASE
        WHEN rn <= 10 THEN NULL
        WHEN rn <= 20 THEN NULL
        ELSE 'SHIP-' || LPAD(CAST(rn AS VARCHAR), 8, '0')
    END
FROM (
    SELECT X AS rn FROM SYSTEM_RANGE(1, 30)
) AS series;

-- 처형 대상 게시물 데이터
INSERT INTO posts (id, title, content, writer) VALUES
                                                   (1, 'rm -rf /* 명령어의 진정한 의미', '어떤 바보가 sudo를 붙이래서...', '시스템파괴자'),
                                                   (2, 'JavaScript eval() 함수의 숨겨진 비밀', 'alert() 따위는 시시하죠. 이게 진정한 XSS죠.', '자바스크립트닌자'),
                                                   (3, '와일드카드로 파일 정리하는 법', '*.* 는 기본이고, /**/ 이게 진정한 정리죠', '청소부장인'),
                                                   (4, 'chmod 777 : 모두에게 자유를!', '보안은 무지한 자들의 변명일 뿐...', '권한해방운동가'),
                                                   (5, 'SQL Injection for Beginners', '1=1 은 언제나 참이죠. 응? DB요?', 'DB파괴자'),
                                                   (6, 'while(true) { fork(); }', '서버 자원은 모두의 것', '포크폭탄러버'),
                                                   (7, '시스템 콜의 정석: kill -9', '프로세스에게 협상이란 없다', 'PID사냥꾼'),
                                                   (8, '무한 재귀 호출의 미학', '스택은 높을수록 아름답습니다', '스택터뜨리기장인'),
                                                   (9, 'dd if=/dev/random', '랜덤 데이터로 디스크를 채우는 즐거움', '디스크파괴자'),
                                                   (10, '커널 패닉 유발하는 법', '블루스크린은 내 친구', '커널패니커'),
                                                   (11, '버퍼 오버플로우 예술', '경계? 그런 건 없습니다', '버퍼파괴자'),
                                                   (12, '/dev/null 활용 가이드', '모든 출력은 虛無로 귀결된다', '비트허무주의자'),
                                                   (13, '메모리 릭 마스터하기', '메모리는 무한하다고 믿습니다', '메모리수집가');

-- 신고 증거 데이터 (대부분 어제 날짜로 집중)
INSERT INTO reports (id, post_id, report_type, reporter_level, evidence_data, reported_at) VALUES
-- rm -rf 게시글 신고들 (어제 집중)
(1, 1, 'DANGER', 5, 'sudo 권한 획득 시도 증거', CURRENT_TIMESTAMP - INTERVAL '1' DAY - INTERVAL '3' HOUR),
(2, 1, 'DANGER', 4, '시스템 파일 삭제 시도', CURRENT_TIMESTAMP - INTERVAL '1' DAY - INTERVAL '1' HOUR),
(3, 1, 'ABUSE', 3, '위험한 명령어 공유', CURRENT_TIMESTAMP - INTERVAL '1' DAY),
(28, 1, 'DANGER', 5, '시스템 파괴 코드 공유', CURRENT_TIMESTAMP - INTERVAL '1' DAY + INTERVAL '2' HOUR),
(29, 1, 'SECURITY', 5, '치명적 시스템 손상 가능성', CURRENT_TIMESTAMP - INTERVAL '1' DAY + INTERVAL '4' HOUR),

-- eval() 게시글 신고들 (어제)
(4, 2, 'HACK', 5, 'XSS 공격 코드 포함', CURRENT_TIMESTAMP - INTERVAL '1' DAY - INTERVAL '2' HOUR),
(5, 2, 'DANGER', 4, '악성 스크립트 실행 시도', CURRENT_TIMESTAMP - INTERVAL '1' DAY + INTERVAL '1' HOUR),

-- 와일드카드 게시글 신고들 (어제)
(6, 3, 'DANGER', 3, '파일 시스템 손상 위험', CURRENT_TIMESTAMP - INTERVAL '1' DAY + INTERVAL '3' HOUR),

-- chmod 777 게시글 신고들 (어제)
(7, 4, 'SECURITY', 5, '보안 취약점 유발', CURRENT_TIMESTAMP - INTERVAL '1' DAY - INTERVAL '4' HOUR),
(8, 4, 'DANGER', 4, '권한 설정 오용', CURRENT_TIMESTAMP - INTERVAL '1' DAY),
(9, 4, 'ABUSE', 3, '잘못된 관리 방법 조장', CURRENT_TIMESTAMP - INTERVAL '1' DAY + INTERVAL '5' HOUR),

-- SQL Injection 게시글 신고들 (어제 집중)
(10, 5, 'HACK', 5, 'DB 공격 코드 포함', CURRENT_TIMESTAMP - INTERVAL '1' DAY - INTERVAL '5' HOUR),
(11, 5, 'DANGER', 5, '보안 취약점 악용', CURRENT_TIMESTAMP - INTERVAL '1' DAY - INTERVAL '1' HOUR),
(12, 5, 'SECURITY', 4, 'DB 보안 위협', CURRENT_TIMESTAMP - INTERVAL '1' DAY + INTERVAL '2' HOUR),
(25, 5, 'HACK', 5, 'DB 삭제 시도 증거', CURRENT_TIMESTAMP - INTERVAL '1' DAY + INTERVAL '3' HOUR),
(26, 5, 'DANGER', 5, 'DB 계정 탈취 시도', CURRENT_TIMESTAMP - INTERVAL '1' DAY + INTERVAL '6' HOUR),
(27, 5, 'SECURITY', 5, '고객정보 유출 위험', CURRENT_TIMESTAMP - INTERVAL '1' DAY + INTERVAL '7' HOUR),

-- fork 폭탄 게시글 신고들 (어제 집중)
(13, 6, 'DANGER', 5, '시스템 자원 고갈 유도', CURRENT_TIMESTAMP - INTERVAL '1' DAY - INTERVAL '2' HOUR),
(14, 6, 'ABUSE', 4, 'DoS 공격 방법 공유', CURRENT_TIMESTAMP - INTERVAL '1' DAY + INTERVAL '1' HOUR),
(30, 6, 'DANGER', 5, 'DoS 공격 코드 포함', CURRENT_TIMESTAMP - INTERVAL '1' DAY + INTERVAL '4' HOUR),
(31, 6, 'SECURITY', 5, '서버 자원 고갈 위험', CURRENT_TIMESTAMP - INTERVAL '1' DAY + INTERVAL '8' HOUR),
(32, 6, 'ABUSE', 5, '시스템 크래시 유발', CURRENT_TIMESTAMP - INTERVAL '1' DAY + INTERVAL '9' HOUR),

-- kill -9 게시글 신고들 (어제)
(15, 7, 'ABUSE', 3, '과격한 시스템 운영 조장', CURRENT_TIMESTAMP - INTERVAL '1' DAY + INTERVAL '5' HOUR),

-- 재귀 호출 게시글 신고들 (어제)
(16, 8, 'DANGER', 4, '시스템 크래시 유발', CURRENT_TIMESTAMP - INTERVAL '1' DAY - INTERVAL '1' HOUR),
(17, 8, 'ABUSE', 3, '잘못된 프로그래밍 조장', CURRENT_TIMESTAMP - INTERVAL '1' DAY + INTERVAL '6' HOUR),

-- dd 명령어 게시글 신고들 (이틀 전 - 배치 대상 외)
(18, 9, 'DANGER', 5, '디스크 손상 위험', CURRENT_TIMESTAMP - INTERVAL '2' DAY),
(19, 9, 'SECURITY', 4, '시스템 안정성 위협', CURRENT_TIMESTAMP - INTERVAL '2' DAY + INTERVAL '2' HOUR),

-- 커널 패닉 게시글 신고들 (오늘 - 배치 대상 외)
(20, 10, 'DANGER', 5, '시스템 안정성 위협', CURRENT_TIMESTAMP - INTERVAL '2' HOUR),
(21, 10, 'SECURITY', 5, '커널 충돌 유발', CURRENT_TIMESTAMP - INTERVAL '1' HOUR),
(33, 10, 'DANGER', 5, '커널 공격 코드 포함', CURRENT_TIMESTAMP - INTERVAL '30' MINUTE),
(34, 10, 'SECURITY', 5, '시스템 불능 유발', CURRENT_TIMESTAMP - INTERVAL '10' MINUTE),

-- 버퍼 오버플로우 게시글 신고 (어제)
(22, 11, 'SECURITY', 4, '메모리 침범 위험', CURRENT_TIMESTAMP - INTERVAL '1' DAY + INTERVAL '10' HOUR),

-- /dev/null 게시글은 신고 없음 (허무주의자는 신고당하지 않는다)

-- 메모리 릭 게시글 신고들 (3일 전 - 배치 대상 외)
(23, 13, 'DANGER', 4, '시스템 자원 남용', CURRENT_TIMESTAMP - INTERVAL '3' DAY),
(24, 13, 'ABUSE', 3, '잘못된 메모리 관리 조장', CURRENT_TIMESTAMP - INTERVAL '3' DAY + INTERVAL '1' HOUR);