# 各类密钥的申请、配置与启用

[English](credentials.md) · **简体中文** · [Español](credentials.es.md) · [返回 README](../README.zh-CN.md)

本文覆盖当前应用使用的密钥、系统自动生成的密码和加密密钥、上游网关凭据，以及待接入的微信支付凭据。目录结构、环境变量和部署步骤保留在 [README](../README.zh-CN.md#配置与凭据)。

## 使用前提

先按 README 完成首次部署；没有第三方密钥也能初始化应用和私有文件。以下命令适用于根目录 `compose.yaml`、已初始化的实例和 `.env.local`，生产环境统一替换为 `.env.prod.local`。旧部署的独立 Compose 文件不适用这些初始化命令。

以下路径以 `SAFILLU_RUNTIME_DIR=./.runtime` 为准；修改运行目录后，请同步替换路径前缀。容器内挂载位置为 `/run/secrets`。普通密钥文件只写实际值，不加引号、变量名或 `Bearer` 前缀；`config.json`、`initial-accounts.json` 为 JSON 格式。通过编辑器或密钥管理工具填写，避免把密钥写进命令历史。

| 密钥 | 获取来源 | 默认宿主机文件 | 用途 |
| --- | --- | --- | --- |
| 模型网关 API Key | Safillu 网关账号 | `.runtime/secrets/provider_key` | 真实模型生成 |
| R2 Access Key ID / Secret Access Key | Cloudflare R2 | `.runtime/secrets/storage/r2/access_key`、`access_secret` | 首选云存储 |
| 七牛 AK / SK | 七牛账号 | `.runtime/secrets/storage/access_key`、`access_secret` | 可选七牛存储 |
| SMTP 授权码 / 应用密码 | 邮箱服务商 | 私有数据库 `system_mail_account` | 注册验证码邮件 |
| Google OAuth Client ID / Secret | Google Auth Platform | `.runtime/secrets/google/client_id`、`client_secret` | Google 登录 |
| Stripe 服务端 Key / Webhook 签名密钥 | Stripe 账号或 Sandbox | `.runtime/secrets/stripe/secret_key`、`webhook_secret` | 积分充值 |
| 数据库、Redis、加密及初始账号密钥 | 安装器生成 | 见[自动生成的密钥](#自动生成的密钥) | 应用运行 |
| RunPod API Key、Endpoint ID | 网关运营者的 RunPod 账号 | 配置在网关渠道中，不放入本应用的密钥文件 | 自建上游推理服务 |
| 微信支付商户凭据 | 微信支付商户账号 | 当前没有生效的文件映射 | 后续支付接入 |

## 注册邮件 SMTP

邮箱注册需要真实的 SMTP 发信账号。向邮箱服务商开通 SMTP，按其要求获取应用专用密码或 SMTP 授权码。普通登录密码是否可用取决于服务商。

在芋道管理端的**系统管理 → 邮件管理 → 邮箱账号**中填写发信地址、用户名、授权码、SMTP 主机、端口和 SSL / STARTTLS 选项。配置保存在私有数据库的 `system_mail_account` 表，不写入 `.env.example`、源代码或 Git。465 端口通常使用 SSL；587 端口通常使用 STARTTLS，具体按邮箱服务商说明配置。

只有一个有效发信账号时，注册验证码自动使用它。有多个账号时，在**基础设施 → 参数配置**添加 `aigc.email.mail-account-id`，值为选定的邮箱账号 ID。验证码采用应用内中、英、西班牙语邮件文案，无需另建邮件模板。

若邮箱服务商需要指定 TLS 协议，可添加参数 `aigc.email.ssl-protocols`，值为 `TLSv1.2`、`TLSv1.3` 或 `TLSv1.2 TLSv1.3`；留空使用 JVM 默认协议。

验证码 5 分钟有效，60 秒内不能重复发送，连续错误 5 次即失效；注册成功后只能使用一次。发送和登录均有限流。验证码仅以带密钥摘要临时存入 Redis，不写入邮件日志。邮件服务未配置或发送失败时，页面会返回错误，不会显示发送成功。管理员原账号及权限体系保留。

## 模型网关

1. 登录 [Safillu 网关 API Keys 页面](https://api.safillu.com/keys/)。没有账号时，按该服务提供的方式注册或申请账号。
2. 创建 Key，分组选择 `default`，开放需要调用的模型权限，包括 `best-minimax-h3`。网关账号还需要可用额度和模型渠道。
3. 将 Key 保存到 `.runtime/secrets/provider_key`。
4. 调整文件权限，并启用已配置的模型注册表：

```bash
docker compose --env-file .env.local run --rm secrets-init
docker compose --env-file .env.local run --rm db-init provider
```

后端请求模型时读取 `/run/secrets/provider_key`，数据库保存 `file:/run/secrets/provider_key` 引用。同一个 Key 可调用其有权限的模型；平台积分与网关余额是两套记账。清空 Key 后执行 `db-init provider` 会禁用模型注册表。

当前适配固定使用 `https://api.safillu.com` 及其原生任务接口。更换其他供应商需要修改适配器，不能只替换 Key。可用接口参考[网关 Playground](https://api.safillu.com/playground)。

## Cloudflare R2

在 [Cloudflare 控制台](https://dash.cloudflare.com/)创建存储桶，在 R2 的 API Token 管理中创建账号或用户 Token，权限选择 **Object Read & Write**，限定到目标桶。把生成的 **Access Key ID**、**Secret Access Key** 分别存入前述 R2 文件。参考 [R2 鉴权说明](https://developers.cloudflare.com/r2/api/tokens/)和 [S3 配置指南](https://developers.cloudflare.com/r2/get-started/s3/)。

本应用的 S3 客户端使用这一对凭据，不读取单独的 Cloudflare 管理 API Token 值。不要把该 Token 值填入 `access_secret`。

```bash
cp docker/storage-r2.example.json .runtime/secrets/storage/r2/config.json
```

只在复制出的私有文件中填写自己的存储参数：

| JSON 字段 | 填写内容 |
| --- | --- |
| `endpoint` | 账号的 S3 Endpoint，通常为 `https://YOUR_ACCOUNT_ID.r2.cloudflarestorage.com` |
| `bucket` | 自己的存储桶名称 |
| `domain` | 无额外路径的公开 HTTPS 素材域名 |
| `region` | `auto` |
| `accessKey`、`accessSecret` | 保留模板中的 `file:/run/secrets/...` 引用 |
| `enablePublicAccess`、`encryptContent` | 本期公开素材流程保持 `true`、`false` |

`domain` 可配置为自定义公开域名，或启用桶的开发地址。Cloudflare 的 `r2.dev` 为带限流的开发用途地址，生产环境使用自定义域名。S3 Endpoint 不能当成公开素材域名。持有公开对象 URL 的人可以读取该对象。参考[公开桶设置](https://developers.cloudflare.com/r2/buckets/public-buckets/)。

在私有环境文件中保持 `R2_STORAGE_BOOTSTRAP=auto`，然后运行：

```bash
docker compose --env-file .env.local run --rm secrets-init
sh ./start.sh .env.local
```

应用通过上传、S3 读取、公开 HTTP 读取、删除校验后才启用 R2。检查后端容器内 `/app/logs/r2-storage-bootstrap.json` 的 `active: true`。配置 `880004` 使用后不能改指其他桶或改变加密模式；历史资产保留原配置。当前 Endpoint 校验器支持默认、EU、FedRAMP 形式，其他司法辖区形式需要增加代码支持。

## 七牛云（可选）

从[七牛密钥管理](https://portal.qiniu.com/user/key)获取 AK/SK；创建或选择 Kodo 桶，取得区域、S3 兼容 Endpoint 和素材域名。参考[七牛 SDK 鉴权](https://developer.qiniu.com/kodo/1239/java)和 [S3 服务域名](https://developer.qiniu.com/kodo/4088/s3-access-domainname)。

AK/SK 分别保存到 `.runtime/secrets/storage/access_key`、`access_secret`。在芋道后台的**基础设施 → 文件配置**核对配置 `880002` 的桶、区域、Endpoint 和域名，并保留密钥文件引用。仓库中的历史配置不能作为新运营者直接可用的存储桶；已有文件引用的配置不能改指其他位置。

明确选择七牛时，设置 `QINIU_STORAGE_BOOTSTRAP=true`、`R2_STORAGE_BOOTSTRAP=false`，执行 `secrets-init` 后重新运行启动脚本。应用校验存储后才切换。已有加密七牛资产必须保留原 `.runtime/secrets/storage/content_key`，替换密钥不会迁移或解密旧对象。

## Google 登录

进入 [Google Auth Platform](https://console.cloud.google.com/auth/overview)，选择项目，配置应用受众及授权同意信息，创建 **Web application** OAuth 客户端，添加准确的回调 URI，取得 Client ID 和 Client Secret。参考 [Web 服务端 OAuth 配置](https://developers.google.com/identity/protocols/oauth2/web-server)及[生产环境要求](https://developers.google.com/identity/protocols/oauth2/production-readiness/policy-compliance)。

分别保存到 `.runtime/secrets/google/client_id`、`client_secret`。环境文件只填写这些非密钥参数：

```dotenv
PUBLIC_ORIGIN=https://studio.example.com
GOOGLE_REDIRECT_URI=https://studio.example.com/app-api/member/auth/google-login
```

Google 中的回调 URI 必须与此完全一致，包括协议、域名、端口和路径。本地测试使用匹配端口的 `localhost` 回调，不能把局域网 IP 当作 Google 允许的本地开发地址。应用处于测试状态时，添加允许登录的测试用户。执行 `secrets-init`，然后重新创建后端：

```bash
docker compose --env-file .env.local up -d --no-deps --force-recreate backend
```

## Stripe 支付

首次联调使用独立 Stripe Sandbox。在 [API Keys 页面](https://dashboard.stripe.com/apikeys)优先创建满足当前调用需求的**受限服务端密钥**。后端创建、查询 Checkout Session 及其商品行，并在退款/争议事件处理中查询 PaymentIntent；为这些操作及关联资源授权，并在 Sandbox 中验证。当前网关不主动发起退款。参考[密钥类型](https://docs.stripe.com/keys)和[受限密钥权限](https://docs.stripe.com/keys/restricted-api-keys)。

保存到 `.runtime/secrets/stripe/secret_key`。代码支持受限密钥和普通账号服务端密钥，密钥环境必须与配置一致。不能填前端 Publishable Key；当前托管 Checkout 流程不需要另建 Publishable Key 密钥文件。

在 [Workbench Webhooks](https://dashboard.stripe.com/webhooks) 创建 **Snapshot** 类型的回调目标，地址为：

```text
https://studio.example.com/admin-api/aigc/payments/stripe/webhook
```

订阅本应用处理的事件：

```text
checkout.session.completed
checkout.session.async_payment_succeeded
checkout.session.async_payment_failed
checkout.session.expired
charge.refunded
charge.dispute.created
```

把该目标的签名密钥保存到 `.runtime/secrets/stripe/webhook_secret`，它与 API Key 不同。本地通过 CLI 转发时，使用监听器提供的签名密钥。Webhook 是支付验证和积分到账的必要环节，仅返回成功页面不会充值。参考 [Stripe Webhook 配置](https://docs.stripe.com/webhooks)。

先保持 `STRIPE_MODE=TEST`、`STRIPE_LIVE_ENABLED=false`。正式套餐配置完成且支付、回调验证通过后，正式收款还需要正式环境密钥及签名密钥、`STRIPE_MODE=LIVE`、`STRIPE_LIVE_ENABLED=true`、公网 HTTPS 的 `PUBLIC_ORIGIN` 和准确的 `STRIPE_PUBLIC_WEBHOOK_URL`。修改 Stripe 文件或环境参数后，执行 `secrets-init` 并重新创建后端。测试和正式凭据不能混用。

## RunPod 等模型上游凭据

如果自行运营推理网关渠道，从 RunPod 控制台的 Credentials → API Keys 获取限定到目标 Endpoint 的受限 Key，将它和 Serverless Endpoint ID 配置在**网关渠道**中，参考 [RunPod 凭据说明](https://docs.runpod.io/get-started/credentials)。Safillu 的 `provider_key` 仍填写网关 Key。其他上游服务也由对应渠道管理，不向本应用前端增加密钥。

## 微信支付（当前尚未接入本应用充值流程）

当前 Safillu 充值实现为 Stripe。已有的微信凭据不会自动启用微信支付；本期没有对应的密钥文件映射或 `.env` 启用开关。

后续接入时，在[微信支付商户平台](https://pay.weixin.qq.com/)申请商户、关联适用 AppID，准备商户号、商户 API 私钥、证书序列号、APIv3 密钥，以及所选验签模式要求的公钥或证书材料。参考[官方 Java SDK 前置条件](https://github.com/wechatpay-apiv3/wechatpay-java#前置条件)。APIv3 密钥与应用 Secret 不同。适配器和回调验证流程完成前，将材料保存在私有目录中，不填入 Stripe 文件。

## 自动生成的密钥

下列密钥无需外部申请，首次初始化自动生成，重跑时保留原值：

| `.runtime/secrets/` 下的文件 | 用途 |
| --- | --- |
| `mysql_root` | 初始化使用的 MySQL 管理员密码 |
| `db_app` | 应用数据库账号密码 |
| `redis_password` | Redis 认证密码 |
| `data_key` | 后端数据库加密字段使用的密钥 |
| `storage/content_key` | 旧存储对象的应用层加密密钥，本期公开 R2 对象不使用它加密 |
| `initial-accounts.json` | 初始管理员、运营账号密码；后续修改密码不会同步更新此文件 |

备份必须包含匹配的密钥、数据库与上传卷。重新生成加密密钥不能恢复旧数据访问。私有文件和环境覆盖文件已被 Git 忽略，模板中不填写真实凭据。

## 更新生效与排查

- 模型 Key：根目录 Compose 挂载的是密钥目录，可直接读取更新后的文件；启用或禁用注册表时运行 `db-init provider`，权限通过 `secrets-init` 修正。
- Google、Stripe：修改后重新创建 `backend`，因为配置在构造时加载。R2：重新运行启动脚本并检查启用结果。
- 普通 `.env` 参数：重新创建受影响的服务，或重新执行启动脚本；仅 `docker compose restart` 不会更新容器环境。
- 旧部署可能单独挂载 `<部署根目录>/deploy/secrets/` 中的文件；编辑器原子替换文件后可能需要重新创建后端。应使用旧部署自己的 Compose，不运行根目录新安装器。
- 模型列表为空：检查 `provider_key` 是否非空、`db-init provider` 是否成功。401/403 表示鉴权或权限问题，502 需要查看网关和上游日志，同时核对分组、模型权限、额度和渠道。
- 参考素材不可用：检查公开域名及无需鉴权的对象读取，不能只检查 S3 鉴权。真实密钥不放进 Issue、截图、前端变量或提交的配置。
