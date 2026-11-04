INSERT INTO reaction_presets (content, is_active)
SELECT seed.content, b'1'
FROM (
  SELECT 1 AS sort_order, '배고파' AS content
  UNION ALL SELECT 2, '인증해'
  UNION ALL SELECT 3, '가는중'
  UNION ALL SELECT 4, '지금 출발'
  UNION ALL SELECT 5, '뭐해?'
) AS seed
WHERE NOT EXISTS (
  SELECT 1
  FROM reaction_presets existing
  WHERE existing.content = seed.content
)
ORDER BY seed.sort_order;
