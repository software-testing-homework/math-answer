-- ============================================
-- 数学问答系统 - 数据库初始化脚本
-- 包含：数据库创建、表结构、初始数据、系统配置
-- ============================================

CREATE DATABASE IF NOT EXISTS math_qa_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE math_qa_system;

-- ============================================
-- 1. 用户表
-- ============================================
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(32) NOT NULL COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码',
    nickname VARCHAR(32) COMMENT '昵称',
    avatar VARCHAR(256) COMMENT '头像',
    email VARCHAR(128) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机',
    bio VARCHAR(500) COMMENT '简介',
    role TINYINT DEFAULT 0 COMMENT '角色:0普通用户,1管理员',
    status TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    login_count INT DEFAULT 0 COMMENT '登录次数',
    last_login_ip VARCHAR(45) COMMENT '最后登录IP',
    last_login_time DATETIME COMMENT '最后登录时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 2. 文章分类表
-- ============================================
CREATE TABLE sys_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(64) NOT NULL COMMENT '分类名称',
    code VARCHAR(64) NOT NULL COMMENT '分类编码',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID',
    level TINYINT DEFAULT 1 COMMENT '层级:1一级,2二级',
    sort INT DEFAULT 0 COMMENT '排序',
    description VARCHAR(256) COMMENT '描述',
    icon VARCHAR(128) COMMENT '图标',
    status TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章分类表';

-- ============================================
-- 3. 文章表
-- ============================================
CREATE TABLE sys_article (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    title VARCHAR(128) NOT NULL COMMENT '标题',
    summary VARCHAR(512) COMMENT '摘要',
    content LONGTEXT NOT NULL COMMENT '内容',
    content_type TINYINT DEFAULT 1 COMMENT '内容类型:1HTML,2Markdown',
    cover_image VARCHAR(256) COMMENT '封面',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    author_id BIGINT NOT NULL COMMENT '作者ID',
    tags VARCHAR(256) COMMENT '标签',
    view_count INT DEFAULT 0 COMMENT '浏览量',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    status TINYINT DEFAULT 1 COMMENT '状态:0草稿,1发布,2下架',
    is_top TINYINT DEFAULT 0 COMMENT '是否置顶',
    publish_time DATETIME COMMENT '发布时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

-- ============================================
-- 4. 评论表
-- ============================================
CREATE TABLE sys_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    article_id BIGINT NOT NULL COMMENT '文章ID',
    user_id BIGINT NOT NULL COMMENT '评论用户ID',
    content TEXT NOT NULL COMMENT '评论内容',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    reply_count INT DEFAULT 0 COMMENT '回复数',
    status TINYINT DEFAULT 1 COMMENT '状态:0已删除,1正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_article_id (article_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- ============================================
-- 5. 评论点赞表
-- ============================================
CREATE TABLE sys_comment_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    comment_id BIGINT NOT NULL COMMENT '评论ID',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_comment_user (comment_id, user_id),
    KEY idx_comment_id (comment_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞表';

-- ============================================
-- 6. 对话会话表
-- ============================================
CREATE TABLE chat_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(128) NOT NULL COMMENT '标题',
    description VARCHAR(256) COMMENT '描述',
    message_count INT DEFAULT 0 COMMENT '消息数',
    token_count INT DEFAULT 0 COMMENT 'token数',
    is_star TINYINT DEFAULT 0 COMMENT '是否星标',
    status TINYINT DEFAULT 1 COMMENT '状态:0已删除,1正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_message_time DATETIME COMMENT '最后消息时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话会话表';

-- ============================================
-- 7. 消息表
-- ============================================
CREATE TABLE chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色:user/assistant/system',
    content LONGTEXT NOT NULL COMMENT '内容',
    content_type VARCHAR(20) DEFAULT 'text' COMMENT '内容类型',
    token_count INT DEFAULT 0 COMMENT 'token数',
    tokens_in INT DEFAULT 0 COMMENT '输入token',
    tokens_out INT DEFAULT 0 COMMENT '输出token',
    status TINYINT DEFAULT 1 COMMENT '状态:0已删除,1正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- ============================================
-- 8. 反馈表
-- ============================================
CREATE TABLE chat_feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT COMMENT '用户ID',
    message_id BIGINT NOT NULL COMMENT '消息ID',
    feedback_type TINYINT NOT NULL COMMENT '反馈类型:1满意,2不满意',
    reason VARCHAR(64) COMMENT '原因',
    suggestion VARCHAR(512) COMMENT '建议',
    status TINYINT DEFAULT 0 COMMENT '处理状态:0未处理,1已处理',
    admin_remark VARCHAR(256) COMMENT '管理员备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈表';

-- ============================================
-- 9. 收藏表
-- ============================================
CREATE TABLE user_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    target_type TINYINT NOT NULL COMMENT '目标类型:1对话,2消息,3文章',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    folder_id BIGINT DEFAULT 0 COMMENT '收藏夹ID',
    remark VARCHAR(256) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- ============================================
-- 10. 通知表
-- ============================================
CREATE TABLE sys_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type TINYINT NOT NULL COMMENT '通知类型:1公告,2私信',
    title VARCHAR(128) NOT NULL COMMENT '标题',
    content VARCHAR(1024) NOT NULL COMMENT '内容',
    sender_id BIGINT COMMENT '发送者ID',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读',
    read_time DATETIME COMMENT '阅读时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- ============================================
-- 11. 登录日志表
-- ============================================
CREATE TABLE sys_login_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(32) COMMENT '用户名',
    login_ip VARCHAR(45) COMMENT '登录IP',
    login_device VARCHAR(256) COMMENT '设备信息',
    login_browser VARCHAR(64) COMMENT '浏览器',
    login_os VARCHAR(64) COMMENT '系统',
    login_result TINYINT DEFAULT 1 COMMENT '结果:1成功,0失败',
    fail_reason VARCHAR(256) COMMENT '失败原因',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- ============================================
-- 12. 系统配置表
-- ============================================
CREATE TABLE sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    config_key VARCHAR(64) NOT NULL COMMENT '配置键',
    config_value VARCHAR(1024) NOT NULL COMMENT '配置值',
    config_group VARCHAR(32) COMMENT '配置分组',
    description VARCHAR(256) COMMENT '描述',
    status TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ============================================
-- 初始数据：数学分类
-- ============================================
INSERT INTO sys_category (name, code, parent_id, level, sort, description, icon, status, create_time, update_time) VALUES
('算式与化简', 'algebra', 0, 1, 1, '代数算式的化简与计算', '', 1, NOW(), NOW()),
('方程求解', 'equation', 0, 1, 2, '各类方程的求解方法', '', 1, NOW(), NOW()),
('不等式与估计', 'inequality', 0, 1, 3, '不等式的证明与数值估计', '', 1, NOW(), NOW()),
('函数图像与性质', 'function', 0, 1, 4, '函数的图像、性质与应用', '', 1, NOW(), NOW()),
('数列与递推', 'sequence', 0, 1, 5, '数列的性质与递推关系', '', 1, NOW(), NOW()),
('几何度量与证明', 'geometry', 0, 1, 6, '几何图形的度量与证明', '', 1, NOW(), NOW()),
('变换与对称', 'transformation', 0, 1, 7, '几何变换与对称性', '', 1, NOW(), NOW()),
('统计与概率', 'statistics', 0, 1, 8, '统计学与概率论基础', '', 1, NOW(), NOW()),
('组合与计数', 'combination', 0, 1, 9, '组合数学与计数原理', '', 1, NOW(), NOW()),
('优化与建模', 'optimization', 0, 1, 10, '最优化问题与数学建模', '', 1, NOW(), NOW()),
('算法与数值', 'algorithm', 0, 1, 11, '算法设计与数值计算', '', 1, NOW(), NOW()),
('证明与公理化', 'proof', 0, 1, 12, '数学证明方法与公理化体系', '', 1, NOW(), NOW());

-- ============================================
-- 初始数据：系统默认配置
-- ============================================
INSERT INTO sys_config (config_key, config_value, config_group, description) VALUES
('site.name', '数学问答系统', 'site', '站点名称'),
('site.description', '基于SpringAI的智能数学问答平台', 'site', '站点描述'),
('site.keywords', '数学,AI,问答,学习', 'site', 'SEO关键字'),
('chat.model', 'glm-4.7', 'ai', 'AI模型名称'),
('chat.temperature', '0.3', 'ai', '回复随机性'),
('chat.max_tokens', '2000', 'ai', '最大回复长度'),
('chat.system_prompt', '你是一位专业的数学老师，善于用通俗易懂的语言解释数学概念，提供详细的解题步骤和思路分析。', 'ai', '系统提示词'),
('user.register.enabled', 'true', 'user', '是否开放注册'),
('user.register.verification', 'false', 'user', '是否需要邮箱验证'),
('user.login.max_attempts', '5', 'user', '最大登录尝试次数'),
('user.login.lockout_minutes', '15', 'user', '锁定时间(分钟)');
