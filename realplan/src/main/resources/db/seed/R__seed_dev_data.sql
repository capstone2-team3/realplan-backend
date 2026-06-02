INSERT INTO users (email, password_hash, nickname, created_at, updated_at)
SELECT 'test@test.com', 'temp_hash', '테스트유저', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE email = 'test@test.com'
);

INSERT INTO folder (user_id, name, is_default, created_at, updated_at)
SELECT u.user_id, '기본 폴더', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'test@test.com'
  AND NOT EXISTS (
      SELECT 1
      FROM folder f
      WHERE f.user_id = u.user_id
        AND f.is_default = true
  );

INSERT INTO folder (user_id, name, is_default, created_at, updated_at)
SELECT u.user_id, '테스트 폴더', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'test@test.com'
  AND NOT EXISTS (
      SELECT 1
      FROM folder f
      WHERE f.user_id = u.user_id
        AND f.name = '테스트 폴더'
  );
