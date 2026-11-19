-- 冒烟测试专用数据，可重复执行（每次先清理再插入）
-- 用于验证：训练报表导出、证书 ZIP、个性化推荐、训练汇总统计
USE icpc_system;

DELETE FROM problem_link WHERE target_type = 'TRAINING'
  AND target_id IN (SELECT training_id FROM training WHERE name = 'SmokeTest Training');
DELETE FROM training WHERE name = 'SmokeTest Training';

DELETE FROM problem_tag WHERE problem_id IN (SELECT problem_id FROM problem WHERE title = 'SmokeTest Problem');
DELETE FROM problem WHERE title = 'SmokeTest Problem';
DELETE FROM tag WHERE name = 'SmokeTest Tag';

DELETE FROM contest_result WHERE contest_id IN (SELECT contest_id FROM contest WHERE name = 'SmokeTest Contest');
DELETE FROM contest WHERE name = 'SmokeTest Contest';

INSERT INTO tag (name, category, sort_order, create_time, update_time)
VALUES ('SmokeTest Tag', 'DP', 1, NOW(), NOW());

INSERT INTO problem (title, difficulty, visibility, create_time, update_time)
VALUES ('SmokeTest Problem', 'EASY', 'PUBLIC', NOW(), NOW());

INSERT INTO problem_tag (problem_id, tag_id)
SELECT p.problem_id, t.tag_id
FROM problem p, tag t
WHERE p.title = 'SmokeTest Problem' AND t.name = 'SmokeTest Tag';

INSERT INTO training (name, type, status, creator_id, start_time, end_time,
                      leaderboard_visible, freeze_time, create_time, update_time)
VALUES ('SmokeTest Training', 'SOLO', 'PUBLISHED', 1, NOW(),
        DATE_ADD(NOW(), INTERVAL 1 DAY), 1, 0, NOW(), NOW());

INSERT INTO problem_link (target_type, target_id, problem_id, sort_order)
SELECT 'TRAINING', tr.training_id, p.problem_id, 0
FROM training tr, problem p
WHERE tr.name = 'SmokeTest Training' AND p.title = 'SmokeTest Problem';

INSERT INTO contest (name, category, status, start_time, end_time, creator_id,
                     max_team_size, freeze_minutes, create_time, update_time)
VALUES ('SmokeTest Contest', 'SCHOLASTIC', 'ENDED',
        DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR),
        1, 3, 0, NOW(), NOW());

INSERT INTO contest_result (contest_id, member_id, rank_no, solved_count, penalty, award,
                            create_time, update_time)
SELECT c.contest_id, 2, 1, 3, 100, 'First Prize', NOW(), NOW()
FROM contest c WHERE c.name = 'SmokeTest Contest';

SELECT 'tag' AS kind, COUNT(*) AS cnt FROM tag WHERE name = 'SmokeTest Tag'
UNION ALL SELECT 'problem', COUNT(*) FROM problem WHERE title = 'SmokeTest Problem'
UNION ALL SELECT 'training', COUNT(*) FROM training WHERE name = 'SmokeTest Training'
UNION ALL SELECT 'contest', COUNT(*) FROM contest WHERE name = 'SmokeTest Contest'
UNION ALL SELECT 'result', COUNT(*) FROM contest_result
  WHERE contest_id IN (SELECT contest_id FROM contest WHERE name = 'SmokeTest Contest');
