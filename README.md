# 数学问答系统

基于 Spring Boot 3.x + Vue 3 + DeepSeek AI 的智能数学问答平台，采用前后端分离架构。

## 技术栈

### 后端依赖（Maven）

| 依赖                                  | 版本       | 说明               |
| ----------------------------------- | -------- | ---------------- |
| spring-boot-starter-web             | 3.x      | Web 框架           |
| spring-boot-starter-validation      | 3.x      | 参数校验             |
| spring-boot-starter-data-jdbc       | 3.x      | JDBC 数据访问        |
| mybatis-spring-boot-starter         | 3.0.3    | MyBatis 集成       |
| mybatis-plus-spring-boot3-starter   | 3.5.5    | MyBatis-Plus ORM |
| mysql-connector-j                   | runtime  | MySQL 驱动         |
| spring-ai-starter-model-deepseek    | -        | DeepSeek AI 集成   |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.12.3   | JWT 认证           |
| hutool-all                          | 5.8.24   | Java 工具库         |
| lombok                              | provided | 简化代码             |
| spring-boot-starter-test            | test     | 单元测试             |

### 前端依赖（npm）

| 依赖                       | 版本      | 说明          |
| ------------------------ | ------- | ----------- |
| vue                      | ^3.5.26 | 前端框架        |
| vue-router               | ^4.6.4  | 路由管理        |
| pinia                    | ^3.0.4  | 状态管理        |
| axios                    | ^1.17.0 | HTTP 请求     |
| marked                   | ^18.0.5 | Markdown 渲染 |
| katex                    | ^0.17.0 | 数学公式渲染      |
| typescript               | \~5.9.3 | 类型支持        |
| vite                     | ^7.3.0  | 构建工具        |
| @vitejs/plugin-vue       | ^6.0.3  | Vue 插件      |
| eslint                   | ^9.39.2 | 代码检查        |
| prettier                 | 3.7.4   | 代码格式化       |
| vue-tsc                  | ^3.2.1  | Vue 类型检查    |
| vite-plugin-vue-devtools | ^8.0.5  | 开发者工具       |

## 已实现功能

### 用户模块

- 用户注册 / 登录
- 个人资料查看与修改
- 头像上传
- 密码修改
- JWT 认证与拦截

### 文章模块

- 文章发布（支持 HTML / Markdown）
- 文章编辑 / 删除
- 文章列表（分页、分类筛选、关键词搜索）
- 文章详情
- 封面图片上传
- 文章置顶
- 状态管理（草稿、发布、下架）
- 文章点赞 / 取消点赞
- 热门文章 / 最新文章
- 按分类获取文章

### 评论模块

- 发表评论
- 删除评论（仅作者和管理员）
- 评论点赞 / 取消点赞
- 热门评论展示

### 收藏模块

- 收藏文章 / 取消收藏
- 查询收藏状态
- 收藏列表（分页）

### AI 对话模块

- 基于 DeepSeek AI 的流式对话
- 对话会话管理（创建、删除、列表）
- 对话星标标记
- 对话历史记录
- Markdown + KaTeX 数学公式渲染
- 并发请求控制（Semaphore 限流）
- 自动重试机制（429/500/502/503 错误重试）

### 分类模块（后端已实现）

- 12 个数学分类（代数、方程、几何、概率等）
- 分类列表查询
- 按层级 / 父分类查询

## 未完成功能

| 功能     | 前端状态    | 后端状态 | 说明                                       |
| ------ | ------- | ---- | ---------------------------------------- |
| 通知中心   | 占位页面    | 未实现  | 前端仅显示"开发中"，后端无 Controller/Service/Mapper |
| 管理后台   | 占位页面    | 未实现  | 前端仅显示"开发中"，缺少用户管理、文章审核、系统配置等             |
| 分类动态加载 | API 已定义 | 已实现  | 前端视图硬编码分类数据，未调用后端分类 API                  |
| 非流式对话  | 未调用     | 已实现  | `POST /api/chat/message` 接口存在但前端未使用      |

## API 接口

### 用户相关

```
POST   /api/user/register           用户注册
POST   /api/user/login              用户登录
GET    /api/user/profile            获取用户信息
PUT    /api/user/profile            更新用户信息
PUT    /api/user/password           修改密码
POST   /api/user/avatar             上传头像
```

### 文章相关

```
GET    /api/article/list            获取文章列表（分页、筛选、搜索）
GET    /api/article/my              获取我的文章
GET    /api/article/{id}            获取文章详情
POST   /api/article/create          创建文章
PUT    /api/article/update          更新文章
DELETE /api/article/{id}            删除文章
POST   /api/article/{id}/like       点赞文章
DELETE /api/article/{id}/like       取消点赞
GET    /api/article/{id}/favorite/status  查询收藏状态
GET    /api/article/hot             获取热门文章
GET    /api/article/hot/category/{categoryId}   按分类获取热门文章
GET    /api/article/latest          获取最新文章
GET    /api/article/latest/category/{categoryId} 按分类获取最新文章
POST   /api/article/upload          上传封面图片
```

### 评论相关

```
GET    /api/comment/list            获取评论列表（分页）
GET    /api/comment/top             获取热门评论
POST   /api/comment/create          发表评论
DELETE /api/comment/{id}            删除评论
POST   /api/comment/{id}/like       点赞评论
DELETE /api/comment/{id}/like       取消点赞评论
```

### 收藏相关

```
POST   /api/favorite/article/{articleId}         收藏文章
DELETE /api/favorite/article/{articleId}          取消收藏
GET    /api/favorite/article/{articleId}/status   查询收藏状态
GET    /api/favorite/articles                     获取收藏列表（分页）
```

### AI 对话相关

```
POST   /api/chat/conversations                 创建对话会话
GET    /api/chat/conversations                 获取对话列表
GET    /api/chat/conversations/{id}            获取对话详情
DELETE /api/chat/conversations/{id}            删除对话
POST   /api/chat/conversations/{id}/star       切换星标
POST   /api/chat/stream                        流式对话（SSE）
POST   /api/chat/message                       非流式对话（前端未调用）
```

### 分类相关

```
GET    /api/category/list                      获取分类列表
GET    /api/category/{id}                      获取分类详情
GET    /api/category/level/{level}             按层级获取分类
GET    /api/category/parent/{parentId}         按父分类获取子分类
```

## 数据库表

| 表名                 | 说明              |
| ------------------ | --------------- |
| sys\_user          | 用户表             |
| sys\_article       | 文章表             |
| sys\_category      | 分类表             |
| sys\_comment       | 评论表             |
| sys\_comment\_like | 评论点赞表           |
| user\_favorite     | 收藏表（支持对话/消息/文章） |
| chat\_conversation | 对话会话表           |
| chat\_message      | 消息表             |
| chat\_feedback     | 反馈表（未使用）        |
| sys\_notification  | 通知表（未使用）        |
| sys\_login\_log    | 登录日志表（未使用）      |
| sys\_config        | 系统配置表（未使用）      |

## 部署

### 环境要求

- JDK 21+
- Maven 3.6+
- MySQL 8.0+
- Node.js 16+

### 后端

1. 创建数据库并执行 `init_all.sql`
2. 修改 `application.yaml` 中的数据库密码和 DeepSeek API Key
3. 主分支的ai配置根据下面改，没用zhipu了
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/math_qa_system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password: 自己的密码
  ai:
    zhipuai:
      api-key: "自己的api key"
      base-url: "https://api.deepseek.com"
      chat:
        options:
          model: deepseek-v4-flash
4. 启动：`mvn spring-boot:run -pl start-01-ai`
5. 访问：<http://localhost:8081>

### 前端

1. 安装依赖：`npm install`
2. 开发模式：`npm run dev`
3. 访问：<http://localhost:5173>

