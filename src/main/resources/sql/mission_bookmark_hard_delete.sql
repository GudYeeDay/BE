-- 보관함 삭제를 소프트 삭제에서 하드 삭제로 변경 (배포 시 1회 실행)
-- 1) 소프트 삭제로 남아 있던 항목 정리 (정리하지 않으면 보관함에 다시 보임)
DELETE FROM mission_bookmark WHERE deleted_at IS NOT NULL;

-- 2) 더 이상 쓰지 않는 컬럼 제거 (ddl-auto: update는 컬럼을 지우지 않음)
ALTER TABLE mission_bookmark DROP COLUMN deleted_at;
