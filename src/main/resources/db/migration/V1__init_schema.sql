CREATE TABLE `users` (
  `is_chat_bubble_notification_enabled` bit(1) NOT NULL DEFAULT b'1',
  `is_friend_arrival_notification_enabled` bit(1) NOT NULL DEFAULT b'1',
  `is_location_notification_enabled` bit(1) NOT NULL DEFAULT b'1',
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `kakao_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `nickname` varchar(50) NOT NULL,
  `email` varchar(320) DEFAULT NULL,
  `profile_image_url` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKk4ycaj27putgcujmehwbsrmmc` (`kakao_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reaction_presets` (
  `is_active` bit(1) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `refresh_tokens` (
  `expires_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token_hash` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKo2mlirhldriil2y7krapq4frt` (`token_hash`),
  KEY `FK1lih5y2npsf8u5o3vhdb9y0os` (`user_id`),
  CONSTRAINT `FK1lih5y2npsf8u5o3vhdb9y0os` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `favorite_searches` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `keyword` varchar(100) NOT NULL,
  `normalized_keyword` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `road_address_name` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_favorite_searches_user_normalized_keyword` (`user_id`,`normalized_keyword`),
  CONSTRAINT `FK6ao6jgncqes3tfoqmftew4qef` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `push_subscriptions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `refreshed_at` datetime(6) NOT NULL,
  `registered_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `auth` varchar(30) NOT NULL,
  `endpoint_hash` varchar(64) NOT NULL,
  `p256dh` varchar(100) NOT NULL,
  `endpoint` varchar(2048) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_push_subscriptions_endpoint_hash` (`endpoint_hash`),
  KEY `FK1v577hpc7v9mdrm2uyk6kqgnl` (`user_id`),
  CONSTRAINT `FK1v577hpc7v9mdrm2uyk6kqgnl` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `meetings` (
  `arrival_radiusm` int NOT NULL,
  `capacity` int NOT NULL,
  `destination_latitude` decimal(10,7) NOT NULL,
  `destination_longitude` decimal(10,7) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `host_user_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `meeting_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `memo` varchar(12) DEFAULT NULL,
  `invite_code` varchar(20) NOT NULL,
  `title` varchar(50) NOT NULL,
  `destination_name` varchar(100) NOT NULL,
  `destination_address` varchar(200) DEFAULT NULL,
  `status` enum('CANCELED','COMPLETED','IN_PROGRESS','WAITING') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKey9kqrbqikl5fc4buce6ajqbh` (`host_user_id`),
  CONSTRAINT `FKey9kqrbqikl5fc4buce6ajqbh` FOREIGN KEY (`host_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `meeting_members` (
  `current_latitude` decimal(10,7) DEFAULT NULL,
  `current_longitude` decimal(10,7) DEFAULT NULL,
  `departure_latitude` decimal(10,7) DEFAULT NULL,
  `departure_longitude` decimal(10,7) DEFAULT NULL,
  `estimated_duration_seconds` int DEFAULT NULL,
  `is_chat_bubble_notification_enabled` bit(1) NOT NULL,
  `is_custom_nickname` bit(1) NOT NULL,
  `is_friend_arrival_notification_enabled` bit(1) NOT NULL,
  `is_location_notification_enabled` bit(1) NOT NULL,
  `arrived_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `departed_at` datetime(6) DEFAULT NULL,
  `departure_reminder_attempted_at` datetime(6) DEFAULT NULL,
  `duration_calculated_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `location_updated_at` datetime(6) DEFAULT NULL,
  `meeting_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `nickname` varchar(50) NOT NULL,
  `transport_line` varchar(50) DEFAULT NULL,
  `departure_name` varchar(100) DEFAULT NULL,
  `profile_image_url` varchar(500) NOT NULL,
  `role` enum('GUEST','HOST') NOT NULL,
  `status` enum('ARRIVED','MOVING','NOT_STARTED') NOT NULL,
  `transport_type` enum('BUS','CAR','ETC','SUBWAY','WALK') DEFAULT NULL,
  `travel_mode` enum('CAR','TRANSIT','WALK') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKf0yer61yyfen0a33nre5kkyv4` (`meeting_id`),
  KEY `FK59dw9pwblepm19v2ybg20r0ap` (`user_id`),
  CONSTRAINT `FK59dw9pwblepm19v2ybg20r0ap` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKf0yer61yyfen0a33nre5kkyv4` FOREIGN KEY (`meeting_id`) REFERENCES `meetings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `meeting_member_routes` (
  `route_index` int NOT NULL,
  `section_time_seconds` int NOT NULL,
  `station_count` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `meeting_member_id` bigint NOT NULL,
  `route_name` varchar(50) DEFAULT NULL,
  `end_name` varchar(100) DEFAULT NULL,
  `start_name` varchar(100) DEFAULT NULL,
  `transport_type` enum('BUS','CAR','ETC','SUBWAY','WALK') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKnva8uxow2tunx7pqev7r1e76b` (`meeting_member_id`),
  CONSTRAINT `FKnva8uxow2tunx7pqev7r1e76b` FOREIGN KEY (`meeting_member_id`) REFERENCES `meeting_members` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `member_images` (
  `is_default_image` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `meeting_member_id` bigint NOT NULL,
  `image_url` varchar(500) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKoi35ht7waw8nkd3ggg4rxclj2` (`meeting_member_id`),
  CONSTRAINT `FKoi35ht7waw8nkd3ggg4rxclj2` FOREIGN KEY (`meeting_member_id`) REFERENCES `meeting_members` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `puzzle_pages` (
  `page_number` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `meeting_id` bigint NOT NULL,
  `representative_member_image_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKm0gxs9fqbo2i70mwtv53bm5kr` (`meeting_id`),
  KEY `FK4qn7qr99snmbl1dx8ifp8t0ax` (`representative_member_image_id`),
  CONSTRAINT `FK4qn7qr99snmbl1dx8ifp8t0ax` FOREIGN KEY (`representative_member_image_id`) REFERENCES `member_images` (`id`),
  CONSTRAINT `FKm0gxs9fqbo2i70mwtv53bm5kr` FOREIGN KEY (`meeting_id`) REFERENCES `meetings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `puzzle_pieces` (
  `is_revealed` bit(1) NOT NULL,
  `is_system_filled` bit(1) NOT NULL,
  `piece_index` tinyint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `meeting_member_id` bigint DEFAULT NULL,
  `puzzle_page_id` bigint NOT NULL,
  `revealed_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKpi9jyhmjmwewmy2icu5uqurwn` (`meeting_member_id`),
  KEY `FKhj97j22vpm3wsj3qhp0j90svw` (`puzzle_page_id`),
  CONSTRAINT `FKhj97j22vpm3wsj3qhp0j90svw` FOREIGN KEY (`puzzle_page_id`) REFERENCES `puzzle_pages` (`id`),
  CONSTRAINT `FKpi9jyhmjmwewmy2icu5uqurwn` FOREIGN KEY (`meeting_member_id`) REFERENCES `meeting_members` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `puzzle_collections` (
  `collected_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `puzzle_page_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `image_url` varchar(500) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKoen28ngbbo5csrpx8awaf2kh8` (`puzzle_page_id`),
  KEY `FKq7v0jn8pyo096btabf07yqn1k` (`user_id`),
  CONSTRAINT `FKoen28ngbbo5csrpx8awaf2kh8` FOREIGN KEY (`puzzle_page_id`) REFERENCES `puzzle_pages` (`id`),
  CONSTRAINT `FKq7v0jn8pyo096btabf07yqn1k` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reaction_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reaction_preset_id` bigint NOT NULL,
  `sender_member_id` bigint NOT NULL,
  `sent_at` datetime(6) NOT NULL,
  `content` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK5g3809nm85r6g9jln61t29t9o` (`reaction_preset_id`),
  KEY `FK1n0stwmf93vp7okfr3nhqhlk0` (`sender_member_id`),
  CONSTRAINT `FK1n0stwmf93vp7okfr3nhqhlk0` FOREIGN KEY (`sender_member_id`) REFERENCES `meeting_members` (`id`),
  CONSTRAINT `FK5g3809nm85r6g9jln61t29t9o` FOREIGN KEY (`reaction_preset_id`) REFERENCES `reaction_presets` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
