UPDATE folder f
SET is_default = true
WHERE f.name = '기본 폴더'
  AND NOT EXISTS (
      SELECT 1
      FROM folder existing_default
      WHERE existing_default.user_id = f.user_id
        AND existing_default.is_default = true
  );

INSERT INTO folder (user_id, name, is_default, created_at, updated_at)
SELECT u.user_id, '기본 폴더', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
WHERE NOT EXISTS (
    SELECT 1
    FROM folder f
    WHERE f.user_id = u.user_id
      AND f.is_default = true
)
  AND NOT EXISTS (
      SELECT 1
      FROM folder f
      WHERE f.user_id = u.user_id
        AND f.name = '기본 폴더'
  );
