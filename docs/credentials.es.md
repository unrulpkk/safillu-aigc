# Credenciales: obtención, configuración y activación

[English](credentials.md) · [简体中文](credentials.zh-CN.md) · **Español** · [Volver al README](../README.es.md)

Esta guía cubre las credenciales de la aplicación actual, los secretos generados automáticamente, las credenciales de los proveedores del gateway y la futura integración con WeChat Pay. La estructura, las variables de entorno y el despliegue permanecen en el [README](../README.es.md#configuración-y-credenciales).

## Antes de empezar

Completa la instalación inicial del README. La aplicación crea sus archivos privados sin exigir credenciales externas. Los comandos siguientes corresponden al `compose.yaml` raíz, una instalación inicializada y `.env.local`; utiliza `.env.prod.local` de forma coherente en producción. No corresponden al Compose independiente de una instalación antigua.

Las rutas presuponen `SAFILLU_RUNTIME_DIR=./.runtime`. Sustituye el prefijo si eliges otro directorio. Dentro de los contenedores se monta como `/run/secrets`. Cada archivo de secreto contiene solo el valor, sin comillas, nombre de variable ni prefijo `Bearer`; `config.json` e `initial-accounts.json` son las excepciones JSON. Utiliza un editor o gestor de secretos para evitar que las claves queden en el historial de comandos.

| Credencial | Procedencia | Archivo predeterminado en el anfitrión | Uso |
| --- | --- | --- | --- |
| API key del gateway de modelos | Cuenta del gateway Safillu | `.runtime/secrets/provider_key` | Generación real |
| Access Key ID / Secret Access Key de R2 | Cloudflare R2 | `.runtime/secrets/storage/r2/access_key`, `access_secret` | Almacenamiento principal en la nube |
| AK / SK de Qiniu | Cuenta de Qiniu | `.runtime/secrets/storage/access_key`, `access_secret` | Almacenamiento Qiniu opcional |
| Código SMTP / contraseña de aplicación | Proveedor de correo | Base de datos privada: `system_mail_account` | Correo de verificación del registro |
| Client ID / Secret de Google OAuth | Google Auth Platform | `.runtime/secrets/google/client_id`, `client_secret` | Acceso con Google |
| Clave de servidor / secreto de firma de Stripe | Cuenta o sandbox de Stripe | `.runtime/secrets/stripe/secret_key`, `webhook_secret` | Compra de créditos |
| Secretos de base de datos, Redis, cifrado y cuentas iniciales | Instalador | Consulta los [secretos generados](#secretos-generados-automáticamente) | Funcionamiento de la aplicación |
| API key e ID de endpoint de RunPod | Cuenta RunPod del operador del gateway | Canal del gateway, fuera de los secretos de Safillu | Inferencia propia del proveedor |
| Credenciales de comerciante de WeChat Pay | Cuenta de comerciante | Sin correspondencia activa con archivos de la aplicación | Integración de pago prevista |

## SMTP para el correo de registro

El registro por correo requiere un remitente SMTP operativo. Activa SMTP con tu proveedor de correo y obtén una contraseña de aplicación o un código de autorización SMTP según sus requisitos.

En la administración de Yudao, abre **Sistema → Correo → Cuentas de correo** e introduce la dirección del remitente, el usuario, el código de autorización, el servidor SMTP, el puerto y las opciones SSL / STARTTLS. Se guardan en la tabla privada `system_mail_account`, fuera de `.env.example`, el código fuente y Git. El puerto 465 suele usar SSL; el 587 suele usar STARTTLS. Sigue la configuración de tu proveedor.

Si solo hay una cuenta activa, los códigos de registro se envían con ella. Si hay varias, añade `aigc.email.mail-account-id` en **Infraestructura → Configuración**, con el ID de la cuenta elegida. Los mensajes de verificación incluyen textos en chino, inglés y español; no requieren una plantilla de correo adicional.

Si el proveedor requiere un protocolo TLS concreto, añade el parámetro `aigc.email.ssl-protocols` con `TLSv1.2`, `TLSv1.3` o `TLSv1.2 TLSv1.3`. Déjalo vacío para usar los protocolos predeterminados de la JVM.

Los códigos caducan en 5 minutos, tienen una espera de 60 segundos entre envíos y se invalidan tras 5 intentos incorrectos o un uso correcto. Los envíos y los intentos de acceso tienen límites de frecuencia. Redis guarda temporalmente solo resúmenes criptográficos con clave; los códigos se excluyen de los registros de correo. La falta de configuración SMTP o un fallo de envío devuelve un error. Se conservan las cuentas y los permisos nativos de administrador.

## Gateway de modelos

1. Accede a la [página API Keys del gateway Safillu](https://api.safillu.com/keys/). Si necesitas una cuenta, solicítala o regístrala mediante las opciones del servicio.
2. Crea una clave en el grupo `default` y permite los modelos necesarios, incluido `best-minimax-h3`. La cuenta del gateway también necesita cuota y canales disponibles.
3. Guarda la clave en `.runtime/secrets/provider_key`.
4. Ajusta los permisos de los archivos y activa el registro de modelos configurado:

```bash
docker compose --env-file .env.local run --rm secrets-init
docker compose --env-file .env.local run --rm db-init provider
```

El backend lee `/run/secrets/provider_key` al solicitar generación. La base de datos conserva la referencia `file:/run/secrets/provider_key`. Una clave sirve para los modelos autorizados; los créditos de la plataforma y el saldo del gateway son independientes. Vaciar la clave y ejecutar `db-init provider` desactiva el registro.

El adaptador actual utiliza `https://api.safillu.com` y sus rutas nativas de tareas. Otro proveedor necesita una adaptación, además de una clave distinta. Consulta las interfaces en el [Playground del gateway](https://api.safillu.com/playground).

## Cloudflare R2

Crea un bucket en el [panel de Cloudflare](https://dash.cloudflare.com/). En la gestión de tokens de R2, crea un token de cuenta o usuario con permiso **Object Read & Write**, limitado al bucket correspondiente. Guarda su **Access Key ID** y **Secret Access Key** en los archivos R2 de la tabla. Consulta la [autenticación de R2](https://developers.cloudflare.com/r2/api/tokens/) y la [configuración S3](https://developers.cloudflare.com/r2/get-started/s3/).

El cliente S3 de esta aplicación utiliza ese par; no utiliza el valor separado del token de administración de Cloudflare. No guardes ese token en `access_secret`.

```bash
cp docker/storage-r2.example.json .runtime/secrets/storage/r2/config.json
```

Modifica únicamente los ajustes de tu almacenamiento en la copia privada:

| Campo JSON | Valor |
| --- | --- |
| `endpoint` | Endpoint S3 de la cuenta, normalmente `https://YOUR_ACCOUNT_ID.r2.cloudflarestorage.com` |
| `bucket` | Nombre de tu bucket |
| `domain` | Dominio HTTPS público de los archivos, sin ruta adicional |
| `region` | `auto` |
| `accessKey`, `accessSecret` | Conserva las referencias `file:/run/secrets/...` de la plantilla |
| `enablePublicAccess`, `encryptContent` | Mantén `true`, `false` para los archivos públicos de esta versión |

Configura un dominio público propio o activa la URL de desarrollo del bucket. Cloudflare reserva `r2.dev` para desarrollo y aplica límites de tráfico; utiliza un dominio propio en producción. El endpoint S3 no es el dominio público de los archivos. Quien conozca la URL pública de un objeto puede leerlo. Consulta los [buckets públicos](https://developers.cloudflare.com/r2/buckets/public-buckets/).

Mantén `R2_STORAGE_BOOTSTRAP=auto` en el archivo privado de entorno y ejecuta:

```bash
docker compose --env-file .env.local run --rm secrets-init
sh ./start.sh .env.local
```

La activación comprueba subida, lectura S3, lectura HTTP pública y eliminación antes de seleccionar R2. Revisa `active: true` en `/app/logs/r2-storage-bootstrap.json` dentro del backend. La configuración `880004` no puede cambiar de bucket ni de modo de cifrado después de utilizarse. Los archivos anteriores mantienen su configuración. El validador actual admite endpoints predeterminados, EU y FedRAMP; otras jurisdicciones requieren soporte adicional en el código.

## Qiniu (opcional)

Obtén AK/SK en la [gestión de claves de Qiniu](https://portal.qiniu.com/user/key). Crea o elige un bucket Kodo y obtén su región, endpoint compatible con S3 y dominio de archivos. Referencias: [autenticación del SDK](https://developer.qiniu.com/kodo/1239/java) y [dominios S3](https://developer.qiniu.com/kodo/4088/s3-access-domainname).

Guarda AK/SK en `.runtime/secrets/storage/access_key` y `access_secret`. En **Infraestructura → Configuración de archivos** del panel Yudao, comprueba el bucket, la región, el endpoint y el dominio de la configuración `880002`, conservando las referencias a archivos de secretos. La configuración histórica incluida no es un bucket listo para otro operador. No cambies el destino de una configuración que ya tenga archivos asociados.

Para seleccionar Qiniu explícitamente, establece `QINIU_STORAGE_BOOTSTRAP=true` y `R2_STORAGE_BOOTSTRAP=false`, ejecuta `secrets-init` y vuelve a iniciar mediante el script. Se verifica el almacenamiento antes de seleccionarlo. Conserva el `storage/content_key` original de los archivos Qiniu cifrados; cambiar la clave no migra ni descifra los objetos anteriores.

## Acceso con Google

En [Google Auth Platform](https://console.cloud.google.com/auth/overview), selecciona un proyecto, configura la audiencia y el consentimiento de la aplicación, y crea un cliente OAuth de tipo **Web application**. Añade la URI de retorno exacta y obtén el Client ID y Client Secret. Consulta la [configuración OAuth para servidores web](https://developers.google.com/identity/protocols/oauth2/web-server) y los [requisitos de producción](https://developers.google.com/identity/protocols/oauth2/production-readiness/policy-compliance).

Guárdalos en `.runtime/secrets/google/client_id` y `client_secret`. En el archivo de entorno, configura estos valores sin secretos:

```dotenv
PUBLIC_ORIGIN=https://studio.example.com
GOOGLE_REDIRECT_URI=https://studio.example.com/app-api/member/auth/google-login
```

La URI registrada en Google debe coincidir en protocolo, dominio, puerto y ruta. Para pruebas locales utiliza `localhost` con el puerto correspondiente; una IP de la red local no sustituye las reglas de Google para desarrollo local. Añade usuarios de prueba mientras la aplicación OAuth esté en modo de pruebas. Ejecuta `secrets-init` y recrea el backend:

```bash
docker compose --env-file .env.local up -d --no-deps --force-recreate backend
```

## Pagos con Stripe

Para empezar, utiliza un sandbox independiente. En [API Keys](https://dashboard.stripe.com/apikeys), crea preferentemente una **clave restringida de servidor** con los permisos necesarios. El backend crea y consulta Checkout Sessions con sus líneas de compra y consulta PaymentIntents para procesar eventos de reembolso o disputa. Autoriza esas operaciones y sus dependencias y verifícalas en el sandbox. El gateway actual no inicia reembolsos. Referencias: [tipos de claves](https://docs.stripe.com/keys) y [permisos restringidos](https://docs.stripe.com/keys/restricted-api-keys).

Guarda la clave en `.runtime/secrets/stripe/secret_key`. Se admiten claves restringidas y claves de servidor de cuenta; su entorno debe coincidir con la configuración. Una clave publicable no sirve para este archivo, y el Checkout alojado actual no necesita un archivo de clave publicable.

En [Workbench Webhooks](https://dashboard.stripe.com/webhooks), crea un destino de eventos **Snapshot** con esta URL:

```text
https://studio.example.com/admin-api/aigc/payments/stripe/webhook
```

Suscríbelo a los eventos que procesa la aplicación:

```text
checkout.session.completed
checkout.session.async_payment_succeeded
checkout.session.async_payment_failed
checkout.session.expired
charge.refunded
charge.dispute.created
```

Guarda el secreto de firma de ese destino en `.runtime/secrets/stripe/webhook_secret`. Es distinto de la API key. Si reenvías eventos localmente con la CLI, utiliza el secreto del listener. Los webhooks son necesarios para verificar pagos y abonar créditos; volver a la página de éxito no abona créditos por sí solo. Consulta la [configuración de webhooks](https://docs.stripe.com/webhooks).

Empieza con `STRIPE_MODE=TEST` y `STRIPE_LIVE_ENABLED=false`. Tras configurar los paquetes reales y verificar pagos y callbacks, producción requiere claves y secreto de firma de modo real, `STRIPE_MODE=LIVE`, `STRIPE_LIVE_ENABLED=true`, `PUBLIC_ORIGIN` HTTPS público y el `STRIPE_PUBLIC_WEBHOOK_URL` exacto. Ejecuta `secrets-init` y recrea el backend tras cambiar archivos o variables de Stripe. Las credenciales de pruebas y reales no son intercambiables.

## RunPod y otros proveedores del gateway

Si operas el canal de inferencia, crea una API key restringida al endpoint necesario en Credentials → API Keys de RunPod. Configúrala junto con el ID del endpoint Serverless en el **canal del gateway**, siguiendo la [guía de credenciales](https://docs.runpod.io/get-started/credentials). El `provider_key` de Safillu sigue conteniendo la clave del gateway. Los demás proveedores también se configuran en sus canales, sin añadir secretos al frontend.

## WeChat Pay (pendiente de integración en esta aplicación)

La recarga actual de Safillu utiliza Stripe. Las credenciales antiguas de WeChat no lo activan automáticamente; esta versión no tiene rutas de archivos ni variables `.env` para habilitarlo.

Para una futura integración, solicita la cuenta en la [plataforma de comerciantes de WeChat Pay](https://pay.weixin.qq.com/), vincula el AppID adecuado y prepara el ID de comerciante, la clave privada de API, el número de serie del certificado, la clave APIv3 y el material de clave pública/certificado correspondiente al modo de verificación. Consulta los [requisitos del SDK Java oficial](https://github.com/wechatpay-apiv3/wechatpay-java#前置条件). La clave APIv3 es distinta del secreto de la aplicación. Conserva estos datos de forma privada hasta implementar el adaptador y verificar las notificaciones; no los guardes en los archivos de Stripe.

## Secretos generados automáticamente

No se solicitan a proveedores. El instalador los genera una vez y conserva los valores existentes:

| Archivo bajo `.runtime/secrets/` | Uso |
| --- | --- |
| `mysql_root` | Contraseña del administrador MySQL para la inicialización |
| `db_app` | Contraseña de la cuenta de base de datos de la aplicación |
| `redis_password` | Contraseña de autenticación de Redis |
| `data_key` | Clave de los campos cifrados de la base de datos |
| `storage/content_key` | Cifrado de objetos antiguos de la aplicación; no cifra los objetos R2 públicos de esta versión |
| `initial-accounts.json` | Contraseñas iniciales de administrador y operador; los cambios posteriores no actualizan el archivo |

Guarda copias de los secretos junto con su base de datos y volúmenes de archivos correspondientes. Generar claves nuevas no recupera el acceso a datos antiguos. Los secretos y ajustes privados se excluyen de Git; las plantillas no contienen credenciales reales.

## Aplicar cambios y resolver problemas

- Clave del gateway: el montaje de directorio del Compose raíz expone las actualizaciones del archivo. Ejecuta `db-init provider` al activar/desactivar el registro y `secrets-init` para ajustar permisos.
- Google/Stripe: recrea `backend`, ya que cargan la configuración al construirse. R2: vuelve a ejecutar el script de inicio y comprueba la activación.
- Variables `.env`: recrea los servicios afectados o ejecuta el script de inicio. `docker compose restart` no aplica un entorno de contenedor nuevo.
- Las instalaciones antiguas pueden montar archivos individuales bajo `<raíz-del-despliegue>/deploy/secrets/`; una sustitución atómica puede requerir recrear el backend. Utiliza su Compose, sin ejecutar el instalador raíz sobre esa instalación.
- Sin modelos: comprueba que `provider_key` no esté vacío y que `db-init provider` haya terminado correctamente. Los errores 401/403 indican problemas de autenticación o permisos; un 502 requiere revisar gateway y proveedor, junto con grupo, cuota y canales.
- Referencias inaccesibles: comprueba el dominio público y la lectura sin autenticación, además de S3. No incluyas claves reales en incidencias, capturas, variables del frontend ni configuración versionada.
