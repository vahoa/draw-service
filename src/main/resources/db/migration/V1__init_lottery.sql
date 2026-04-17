-- ============================================
-- 抽奖系统数据库初始化脚本
-- ============================================

-- 奖品表
CREATE TABLE IF NOT EXISTS `prize` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '奖品ID',
    `name` VARCHAR(100) NOT NULL COMMENT '奖品名称',
    `type` TINYINT NOT NULL COMMENT '奖品类型：1-积分 2-会员 3-实物 4-虚拟卡券',
    `value` VARCHAR(100) COMMENT '奖品值(积分数量/会员天数/卡券码)',
    `image_url` VARCHAR(500) COMMENT '奖品图片URL',
    `probability` DECIMAL(10,8) NOT NULL COMMENT '中奖概率(0-1)',
    `stock` INT DEFAULT -1 COMMENT '库存数量,-1表示无限',
    `daily_limit` INT DEFAULT -1 COMMENT '每日发放上限,-1表示无限制',
    `today_sent` INT DEFAULT 0 COMMENT '今日已发放数量',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖品表';

-- 用户抽奖次数表
CREATE TABLE IF NOT EXISTS `user_lottery_quota` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `pool_id` BIGINT NOT NULL COMMENT '奖池ID',
    `total_quota` INT NOT NULL DEFAULT 0 COMMENT '总次数',
    `used_quota` INT NOT NULL DEFAULT 0 COMMENT '已使用次数',
    `free_quota` INT NOT NULL DEFAULT 0 COMMENT '免费次数',
    `reset_time` DATE NOT NULL COMMENT '重置日期',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_pool_date` (`user_id`, `pool_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户抽奖次数表';

-- 抽奖记录表
CREATE TABLE IF NOT EXISTS `lottery_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `record_no` VARCHAR(32) NOT NULL COMMENT '中奖记录编号',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `pool_id` BIGINT NOT NULL COMMENT '奖池ID',
    `prize_id` BIGINT COMMENT '奖品ID（未中奖为NULL）',
    `prize_name` VARCHAR(100) COMMENT '奖品名称',
    `prize_type` TINYINT COMMENT '奖品类型',
    `prize_value` VARCHAR(100) COMMENT '奖品值',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1-待发放 2-已发放 3-已领取',
    `draw_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '抽奖时间',
    `grant_time` DATETIME COMMENT '发放时间',
    `client_ip` VARCHAR(50) COMMENT '客户端IP',
    `device_id` VARCHAR(100) COMMENT '设备ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_no` (`record_no`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_draw_time` (`draw_time`),
    INDEX `idx_user_time` (`user_id`, `draw_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖记录表';

-- 初始化奖品数据（与图片对应）
INSERT INTO `prize` (`name`, `type`, `image_url`, `value`, `probability`, `stock`, `daily_limit`, `sort_order`) VALUES
('i会员', 2, '/images/prize/vip.png', '30', 0.05000000, 10000, 500, 1),
('3积分', 1, '/images/prize/coin-3.png', '3', 0.30000000, -1, -1, 2),
('1积分', 1, '/images/prize/coin-1.png', '1', 0.40000000, -1, -1, 3),
('华为折叠屏Mate X7', 3, '/images/prize/mate-x7.png', '华为Mate X7', 0.00100000, 5, 1, 4),
('大疆相机Pocket3', 3, '/images/prize/pocket3.png', '大疆Pocket3', 0.00500000, 20, 5, 5),
('华为手表GT6', 3, '/images/prize/watch-gt6.png', '华为GT6', 0.01000000, 50, 10, 6),
('华为蓝耳机', 3, '/images/prize/earphone.png', '华为FreeBuds', 0.02000000, 100, 20, 7),
('谢谢参与', 1, '/images/prize/thanks.png', '0', 0.21400000, -1, -1, 8);
