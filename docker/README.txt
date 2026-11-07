Safillu — Docker 一键部署
=======================

各类密钥申请、私有文件配置与启用说明：
  English:  ../docs/credentials.md
  简体中文: ../docs/credentials.zh-CN.md
  Español:  ../docs/credentials.es.md
目录结构与 .env.example 每个参数的说明见根目录对应语言的 README。

要求：Docker Engine / Docker Desktop（Linux containers）和 Compose v2.20+。
建议至少 4 核、8 GB 内存、15 GB 可用磁盘；首次构建需联网下载基础镜像及 Maven/pnpm 依赖。
不需要在宿主机安装 Java、Node、MySQL 或 Redis。
同机已有站点占用 8088/8089 时，先在 .env.local 修改 WEB_PORT、ADMIN_PORT 和 PUBLIC_ORIGIN。

在本文件上级目录执行：
  Windows PowerShell:  .\start.ps1
  Linux/macOS:         sh ./start.sh
也可直接执行：
  docker compose up -d --build --wait --wait-timeout 300

默认入口：创作站 http://localhost:8088 / 芋道管理后台 http://localhost:8089
后台账号 admin（原生超级管理员）、contentops（仅内容运营）。密码自动随机生成，见
  .runtime/secrets/initial-accounts.json
Linux 非 root 用户可通过 sudo 读取此文件；后端进程本身不能读取它或 MySQL root 密码。
没有固定默认密码。普通用户在创作站注册，使用同一套数据库和芋道认证。
新数据库默认为 safillu；新项目 Docker 卷以 safillu_ 为前缀。
不会导入旧项目用户、资产、示例探索内容或真实供应商密钥。

包含：前台 Nginx、官方芋道 Vue3 管理端、Java 21 后端、MySQL 8.4、Redis 7，
一次性 secrets-init 和 db-init。自动创建原生 System/Infra 菜单、权限、字典、
Quartz 表、AIGC 表、管理员和运营账号。模型未配置时禁用真实生成入口。
MySQL/Redis/Java 不发布宿主机端口；两个网站同源代理 API。
常驻服务提供健康检查；脚本依次构建镜像以降低内存峰值，等待初始化及网站健康后才返回成功。

本地 / prod 配置
----------------
start 脚本首次复制 .env.example 到 .env.local；该文件及 .runtime 已忽略。
更改端口、项目名、DB_NAME、PUBLIC_ORIGIN 等只改 .env.local，重新执行 start。
生产模板为 .env.prod.example；可复制为 .env.prod.local，使用：
  sh ./start.sh .env.prod.local
或 .\start.ps1 -EnvFile .env.prod.local
生产 PUBLIC_ORIGIN 设为实际 HTTPS 创作域名，前方使用 HTTPS 反向代理。
Google 的 GOOGLE_REDIRECT_URI 应设为此域名下的 /app-api/member/auth/google-login。
默认不开启真实 Stripe 扣款；正式密钥、套餐、币种和 webhook 联调后再显式启用。

第三方配置（可选，均只放本机私有目录）
--------------------------------------
模型：把密钥写入 .runtime/secrets/provider_key，再运行：
  docker compose --env-file .env.local run --rm secrets-init
  docker compose --env-file .env.local run --rm db-init provider
服务端即时读取模型密钥，浏览器不会收到密钥。空密钥执行 provider 会禁用模型。
Google：secrets/google/client_id、client_secret；更改后重启 backend。
Stripe：secrets/stripe/secret_key、webhook_secret；更改后重启 backend。
上述 secrets/ 指 .runtime/secrets/。不要把真实值放进 VITE_ 环境变量。

存储
----
存储优先级：配置完整的 Cloudflare R2 优先；未配置或校验失败时保留现有主存储。
旧资产继续从原存储读取，不自动搬迁。
全新无云凭据安装使用 uploads 持久卷，资产经有权限校验的后端接口读取，
不向公网开放 /admin-api/infra/file/{configId}/get/... 直链。
七牛：安装 storage/access_key、access_secret、content_key 三个原有私密文件，
确认 bucket/domain 与数据库 880002 配置一致，设 QINIU_STORAGE_BOOTSTRAP=true。
启动后自动做上传、读取、删除校验，通过才切换主存储；失败保留当前主存储。
已有七牛资产必须保留原 content_key，不能用新生成的密钥覆盖旧密钥。

Cloudflare R2：复制 docker/storage-r2.example.json 到
  .runtime/secrets/storage/r2/config.json
填写 ACCOUNT_ID、bucket 和 domain（公开的 r2.dev 或自定义 HTTPS 域名），
私钥分别放 storage/r2/access_key、access_secret。
先运行 secrets-init 调整文件读取权限，再重新运行 start。
R2_STORAGE_BOOTSTRAP 默认 auto：检测到上述私有配置与凭据后，应用注册配置 880004，
并在读写校验通过后优先启用 R2；auto 模式下 R2 优先于 QINIU_STORAGE_BOOTSTRAP。
设为 false 可以禁用 R2 自动启用；设为 true 则显式请求校验并启用 R2。
显式将 R2 与七牛的启动标志同时设为 true 会拒绝启动，应只显式选择一个。
也可运行 docker compose --env-file .env.local run --rm db-init r2 手动安装非主配置。
已有 880004 配置不能指向其他桶，否则拒绝切换，防止历史文件读取位置改变。
只有真实上传、S3 读取、无需鉴权的公开图片读取、删除全部通过后才设为主存储；日志见 backend 的
  /app/logs/r2-storage-bootstrap.json
本期 R2 个人资产使用公开域名，不做应用层加密（encryptContent=false）。
旧文件仍按自己的 config_id 和原加密设置读取，不能直接修改旧配置的加密模式。
region=auto，S3 API Endpoint 用于签名上传；模型使用公开域名下无需鉴权的图片 URL。
参考图必须先上传完成再提交生成；旧加密/本地参考图会另外上传一份到当前公开存储后引用。

日常运维
--------
状态：docker compose --env-file .env.local ps -a
日志：docker compose --env-file .env.local logs --tail=100 backend db-init
停止：docker compose --env-file .env.local down
升级：更新源代码后重新执行 start（自动构建并按历史记录追加迁移）。
重启/升级不会重置账号、密码、菜单、模型开关和用户数据。
首次初始化中断可重跑；SQL 按语句记录进度。已应用的迁移校验和变化会拒绝继续，
应该新增迁移文件而非改写已应用文件。
安装器拒绝接管没有 safillu_schema_history 的既有数据库，避免覆盖其他系统。
不要对旧部署直接运行这个全新安装器；旧库迁移另做备份和升级验证。

备份必须同时包含 MySQL 数据、uploads 卷以及整个 .runtime/secrets（特别是加密密钥）。
普通 down 保留数据卷；不要执行 down -v，除非明确要清空整套数据。
不要删除 secrets 后复用旧数据库卷；密码和密钥不匹配会导致启动或历史资产读取失败。
复制到另一台机器时也要保留上述卷和 secrets，不能只复制镜像。

源码部署包
----------
  python docker/package-release.py
产出 safillu-docker-source.tar.gz，可在新机器解压并直接执行 start。
只收集构建所需源文件和安全模板，排除 .git、node_modules、target、本地配置、
密钥、备份、测试录制及运行数据。发布前仍应检查新增文件是否含个人凭据。

Linux 验收脚本
-------------
仅在隔离测试实例上运行（会真实注册用户、创建会话、上传文件）：
  python docker/smoke-test.py .env.local http://127.0.0.1:8088 http://127.0.0.1:8089
首次保存私有测试状态到运行目录，后续运行自动检查该用户、会话、文件与密钥是否保留。
可先 docker compose down（不加 -v），再 up -d --no-build --wait，重新运行脚本验证持久化。
Linux Python 只需标准库；这些验收脚本不是部署前置要求。
安装保护测试仅允许在名字含 smoke 或 qa 的隔离 Compose 项目运行：
  python docker/guard-test.py .env.local
该脚本验证迁移校验和被篡改时拒绝继续，以及拒绝接管其他数据库。
单元检查：python docker/test_bootstrap.py；R2 两项模拟测试随 Java 镜像构建执行。
