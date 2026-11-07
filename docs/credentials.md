# Credentials: obtain, configure, and activate

**English** · [简体中文](credentials.zh-CN.md) · [Español](credentials.es.md) · [Back to README](../README.md)

This guide covers the current application's credentials, automatically generated secrets, upstream gateway credentials, and the planned WeChat Pay integration. Directory layout, environment variables, and deployment commands remain in the [README](../README.md#configuration-and-credentials).

## Before you start

Run the initial deployment described in the README. It creates private files without requiring third-party credentials. Commands below assume the root `compose.yaml`, an initialized installation, and `.env.local`; use `.env.prod.local` consistently for production. They are not commands for an older installation's separate Compose file.

Paths below use `SAFILLU_RUNTIME_DIR=./.runtime`. Replace that prefix if you choose another runtime directory. This directory is mounted at `/run/secrets` in containers. Secret files contain only the value, without quotes, a variable name, or a `Bearer` prefix; `config.json` and `initial-accounts.json` are JSON exceptions. Use an editor or a secret manager to populate files rather than putting credentials in shell history.

| Credential | Source | Default host file | Needed for |
| --- | --- | --- | --- |
| Model gateway API key | Safillu gateway account | `.runtime/secrets/provider_key` | Real generation |
| R2 Access Key ID / Secret Access Key | Cloudflare R2 | `.runtime/secrets/storage/r2/access_key`, `access_secret` | Preferred cloud storage |
| Qiniu AK / SK | Qiniu account | `.runtime/secrets/storage/access_key`, `access_secret` | Optional Qiniu storage |
| SMTP authorization code / app password | Email provider | Private database: `system_mail_account` | Registration verification emails |
| Google OAuth client ID / secret | Google Auth Platform | `.runtime/secrets/google/client_id`, `client_secret` | Google sign-in |
| Stripe server key / webhook signing secret | Stripe account or sandbox | `.runtime/secrets/stripe/secret_key`, `webhook_secret` | Credit purchases |
| Database, Redis, encryption, initial account secrets | Installer | See [generated secrets](#automatically-generated-secrets) | Application operation |
| RunPod API key and endpoint ID | Gateway operator's RunPod account | Gateway channel configuration, outside Safillu's secret files | Self-managed upstream inference |
| WeChat Pay merchant credentials | WeChat Pay merchant account | No active application mapping yet | Planned payment integration |

## Registration email SMTP

Email registration requires a working SMTP sender. Enable SMTP with your email provider and obtain an app password or SMTP authorization code as required by that provider.

In the Yudao admin, open **System → Mail → Mail accounts** and enter the sender address, username, authorization code, SMTP host, port, and SSL / STARTTLS settings. They are stored in the private `system_mail_account` database table, not in `.env.example`, source files, or Git. Port 465 typically uses SSL; port 587 typically uses STARTTLS. Follow your provider's settings.

When there is one active account, registration emails use it automatically. With multiple accounts, add `aigc.email.mail-account-id` under **Infrastructure → Configuration**, using the chosen mail account ID as its value. Verification emails have built-in Chinese, English, and Spanish text; a separate mail template is not required.

If your provider needs an explicit TLS protocol, add the `aigc.email.ssl-protocols` configuration parameter with `TLSv1.2`, `TLSv1.3`, or `TLSv1.2 TLSv1.3`. Leave it empty to use the JVM defaults.

Codes expire in 5 minutes, have a 60-second resend cooldown, and are invalidated after 5 incorrect attempts or one successful use. Sending and login are rate limited. Redis stores only keyed digests temporarily; verification codes are excluded from mail logs. Missing SMTP configuration or failed delivery returns an error rather than reporting success. Existing native administrator accounts and permissions remain available.

## Model gateway

1. Sign in to the [Safillu gateway API Keys page](https://api.safillu.com/keys/). Create or obtain an account through that service if needed.
2. Create a key in the `default` group and allow the models you intend to use, including `best-minimax-h3`. The gateway account needs its own available quota and channels.
3. Save the key in `.runtime/secrets/provider_key`.
4. Fix file permissions and enable the configured model registry:

```bash
docker compose --env-file .env.local run --rm secrets-init
docker compose --env-file .env.local run --rm db-init provider
```

The backend reads `/run/secrets/provider_key` for model requests; the database stores `file:/run/secrets/provider_key`, not the actual value. One key can serve its authorized models. Platform credits and the gateway's balance are separate. An empty key followed by `db-init provider` disables the registry.

The current adapter targets `https://api.safillu.com` and its native task routes. Using a different provider requires an adapter change, not just a different key. Check the [gateway playground](https://api.safillu.com/playground) for the service's available interfaces.

## Cloudflare R2

Create a bucket in the [Cloudflare dashboard](https://dash.cloudflare.com/). In R2's API token management, create an account or user token with **Object Read & Write**, scoped to the target bucket. Save its **Access Key ID** and **Secret Access Key** in the two R2 files above. See [R2 authentication](https://developers.cloudflare.com/r2/api/tokens/) and the [S3 setup guide](https://developers.cloudflare.com/r2/get-started/s3/).

The application's S3 client uses that pair; the separate Cloudflare management API token value is not an application credential. Do not put it in `access_secret`.

```bash
cp docker/storage-r2.example.json .runtime/secrets/storage/r2/config.json
```

Edit only your own storage settings in the copied file:

| JSON field | Value |
| --- | --- |
| `endpoint` | Account S3 endpoint, normally `https://YOUR_ACCOUNT_ID.r2.cloudflarestorage.com` |
| `bucket` | Your bucket name |
| `domain` | Public HTTPS media domain, without a trailing path |
| `region` | `auto` |
| `accessKey`, `accessSecret` | Keep the template's `file:/run/secrets/...` references |
| `enablePublicAccess`, `encryptContent` | Keep `true`, `false` for this release's public media workflow |

For `domain`, configure a custom public domain or enable the bucket's development URL. Cloudflare documents `r2.dev` as a rate-limited development option; use a custom domain for production. The S3 API endpoint is not the public media domain. Anyone with a public object URL can read that object. See [public bucket configuration](https://developers.cloudflare.com/r2/buckets/public-buckets/).

Keep `R2_STORAGE_BOOTSTRAP=auto` in your private environment file, then run:

```bash
docker compose --env-file .env.local run --rm secrets-init
sh ./start.sh .env.local
```

Activation verifies upload, S3 read, public HTTP read, and deletion before selecting R2. Inspect `/app/logs/r2-storage-bootstrap.json` inside the backend for `active: true`. Config ID `880004` cannot be repointed to another bucket or encryption mode after use. Historical assets retain their original configurations. The current endpoint validator supports the default, EU, and FedRAMP endpoint forms; other jurisdiction forms require code support.

## Qiniu (optional)

Obtain AK/SK from your [Qiniu key management](https://portal.qiniu.com/user/key) page. Create or select a Kodo bucket and obtain its region, S3-compatible endpoint, and media domain. References: [Qiniu SDK authentication](https://developer.qiniu.com/kodo/1239/java) and [S3 service domains](https://developer.qiniu.com/kodo/4088/s3-access-domainname).

Save AK/SK in `.runtime/secrets/storage/access_key` and `access_secret`. In Yudao's **Infrastructure → File configuration**, verify that configuration `880002` points to your bucket, region, endpoint, and domain, while retaining the secret-file references. The bundled historical configuration is not a ready-to-use bucket for a new operator. Never repoint a configuration already referenced by existing files.

To explicitly choose Qiniu at startup, set `QINIU_STORAGE_BOOTSTRAP=true` and `R2_STORAGE_BOOTSTRAP=false`, run `secrets-init`, then rerun the start script. This verifies storage before selecting it. Keep the original `.runtime/secrets/storage/content_key` for encrypted Qiniu assets; replacing it does not migrate or decrypt old objects.

## Google sign-in

In [Google Auth Platform](https://console.cloud.google.com/auth/overview), select a project, configure the application's audience/consent information, and create a **Web application** OAuth client. Add the exact callback URI and obtain its client ID and client secret. Follow [web-server OAuth setup](https://developers.google.com/identity/protocols/oauth2/web-server) and Google's [production-readiness guidance](https://developers.google.com/identity/protocols/oauth2/production-readiness/policy-compliance).

Save them to `.runtime/secrets/google/client_id` and `client_secret`. Set these non-secret values in the environment file:

```dotenv
PUBLIC_ORIGIN=https://studio.example.com
GOOGLE_REDIRECT_URI=https://studio.example.com/app-api/member/auth/google-login
```

Use the same callback in Google, including scheme, hostname, port, and path. For local testing, use a `localhost` callback matching the selected port; a LAN IP is not a substitute for Google's allowed local-development redirect rules. Add permitted test users while the OAuth application is in testing. Run `secrets-init`, then recreate the backend:

```bash
docker compose --env-file .env.local up -d --no-deps --force-recreate backend
```

## Stripe payments

Select an isolated Stripe sandbox for initial testing. On the [API keys page](https://dashboard.stripe.com/apikeys), prefer a **restricted server key** with the permissions this integration needs. The backend creates and retrieves Checkout Sessions with line items and retrieves PaymentIntents when processing refund/dispute events; grant those operations and dependencies, then verify them in the sandbox. It does not issue refunds through its current gateway. See [API key types](https://docs.stripe.com/keys) and [restricted permissions](https://docs.stripe.com/keys/restricted-api-keys).

Save the server key in `.runtime/secrets/stripe/secret_key`. Both restricted and standard account server keys are supported; the selected environment must match the key. A publishable frontend key does not belong in this file, and the current hosted Checkout flow does not require a publishable-key secret file.

Create a **snapshot** webhook destination in [Workbench Webhooks](https://dashboard.stripe.com/webhooks), using:

```text
https://studio.example.com/admin-api/aigc/payments/stripe/webhook
```

Subscribe to the events handled by this application:

```text
checkout.session.completed
checkout.session.async_payment_succeeded
checkout.session.async_payment_failed
checkout.session.expired
charge.refunded
charge.dispute.created
```

Save that destination's signing secret to `.runtime/secrets/stripe/webhook_secret`. It is separate from the API key. For CLI forwarding in local tests, use the listener's own signing secret instead. Webhooks are required for payment verification and credit fulfillment; a browser return alone does not credit an account. See [Stripe webhook setup](https://docs.stripe.com/webhooks).

Start with `STRIPE_MODE=TEST` and `STRIPE_LIVE_ENABLED=false`. After configuring live credit packs and verifying payment/webhook behavior, production additionally requires a live-mode key and signing secret, `STRIPE_MODE=LIVE`, `STRIPE_LIVE_ENABLED=true`, a public HTTPS `PUBLIC_ORIGIN`, and the exact `STRIPE_PUBLIC_WEBHOOK_URL`. Run `secrets-init` and recreate the backend after changing Stripe files or environment settings. Test and live credentials are not interchangeable.

## RunPod and other upstream model credentials

If you operate the gateway's inference channel, obtain a restricted API key from the RunPod console's Credentials → API Keys page, scoped to the intended endpoint. Configure that key and the serverless endpoint ID in the **gateway channel**, following [RunPod's credential guide](https://docs.runpod.io/get-started/credentials). Safillu's `provider_key` still contains the gateway key. Other upstream providers follow their channel configuration; they do not add frontend secrets to Safillu.

## WeChat Pay (not wired into this application's checkout yet)

The current Safillu checkout implementation is Stripe. Previously collected WeChat credentials do not enable it automatically, and this release has no WeChat secret-file mapping or `.env` activation switch.

For a future integration, apply through the [WeChat Pay merchant platform](https://pay.weixin.qq.com/), associate the appropriate AppID, and prepare the merchant ID, merchant API private key, certificate serial number, APIv3 key, and the verification public key/certificate material required by your chosen mode. See the [official Java SDK prerequisites](https://github.com/wechatpay-apiv3/wechatpay-java#前置条件). The APIv3 key is distinct from an application secret. Store the material privately until the payment adapter and verified callback flow are implemented; do not copy it into Stripe's files.

## Automatically generated secrets

These require no external application. The installer creates them once and preserves existing values:

| File under `.runtime/secrets/` | Purpose |
| --- | --- |
| `mysql_root` | MySQL administrator password for initialization |
| `db_app` | Application database password |
| `redis_password` | Redis authentication password |
| `data_key` | Key used by the backend's encrypted database fields |
| `storage/content_key` | Key for legacy application-encrypted storage objects; not used to encrypt this release's public R2 objects |
| `initial-accounts.json` | Initial administrator/operator passwords; later password changes do not update this file |

Back up secrets together with their matching database and upload volumes. Regenerating encryption keys does not restore access to old data. The files and their private environment overrides are Git-ignored; templates contain no real credentials.

## Applying updates and troubleshooting

- Model key replacement: the root Compose mount exposes the updated file directly; run `db-init provider` when enabling/disabling the registry and `secrets-init` to restore permissions.
- Google/Stripe changes: recreate `backend`; their configuration is loaded on construction. R2 changes: rerun the start script and inspect the activation result.
- Changed ordinary `.env` values: recreate affected services or rerun the start script. `docker compose restart` alone does not apply a new container environment.
- Older deployments may bind individual files under `<deployment-root>/deploy/secrets/`; atomic file replacement can require recreating that deployment's backend. Use its own Compose file, not the root installer's initialization commands.
- Missing models: check that `provider_key` is nonempty and `db-init provider` succeeded. HTTP 401/403 indicates authentication/permission problems; 502 requires checking the gateway/upstream. Confirm group, model permissions, available quota, and channels.
- Public-reference failures: check the R2 public domain and unsigned object reads, not only S3 authentication. Never paste real keys into issues, screenshots, frontend variables, or committed configuration.
