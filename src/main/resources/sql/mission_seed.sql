-- 미션 테스트용 데이터 (프로토타입, 3개)
-- 실제 미션 100개 확정 시 이 파일의 INSERT 목록을 교체해서 실행
-- day_type: ALL / WEEKDAY / WEEKEND
-- season  : ALL / SPRING / SUMMER / FALL / WINTER
INSERT INTO mission (title, description, day_type, season, created_at, updated_at) VALUES
('(테스트1) 퇴근길 한 정거장 걸어보기', '버스 대신 두 다리로, 노을을 조금 더 오래 보기', 'ALL', 'ALL', NOW(), NOW()),
('(테스트2) 혼자 가는 카페에서 창가 자리 굳이 앉기', '사람 많은 카페여도 굳이 창가 자리를 찾아서 앉아보기. 오래 걸려도, 자리가 없어도 한 번은 기다려보기.', 'ALL', 'ALL', NOW(), NOW()),
('(테스트3) 편의점 대신 동네 빵집 굳이 찾아가기', '가까운 편의점을 두고 굳이 동네 빵집까지 걸어가서 빵 하나 사보기.', 'ALL', 'ALL', NOW(), NOW());
