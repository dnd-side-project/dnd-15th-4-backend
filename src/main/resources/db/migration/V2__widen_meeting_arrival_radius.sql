UPDATE `meetings`
SET `arrival_radiusm` = 500
WHERE `arrival_radiusm` = 50
  AND `status` IN ('WAITING', 'IN_PROGRESS');
