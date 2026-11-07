const copy = {
  videoRateHint: [
    '视频使用现有分辨率分档价格，费率在此只读；名称和启停可修改。',
    'Video uses the existing resolution-based prices. Its rate is read-only here; name and availability can be changed.',
    'El vídeo usa los precios actuales por resolución. La tarifa es de solo lectura aquí; se pueden cambiar el nombre y la disponibilidad.'
  ],
  queueMessage: [
    '已进入队列，预占 {points} 积分',
    'Queued; {points} credits reserved',
    'En cola; {points} créditos reservados'
  ],
  submittingMessage: [
    '正在提交模型服务',
    'Submitting to the model provider',
    'Enviando al proveedor del modelo'
  ],
  runningMessage: [
    '模型正在生成，等待服务商返回结果',
    'Generation in progress; waiting for the provider',
    'Generación en curso; esperando al proveedor'
  ],
  archivingMessage: [
    '生成完成，正在将原始文件归档到个人资产',
    'Generation complete; archiving original files to personal assets',
    'Generación completada; guardando los archivos originales en los recursos personales'
  ],
  settledMessage: [
    '结果已保存至个人资产，结算 {points} 积分',
    'Results saved to personal assets; {points} credits charged',
    'Resultados guardados en los recursos personales; se han descontado {points} créditos'
  ],

  download: ['下载', 'Download', 'Descargar'],
  users: ['用户数量', 'Users', 'Usuarios'],
  openService: ['打开服务', 'Open service', 'Abrir servicio'],
  databaseMonitor: ['数据库监控', 'Database monitoring', 'Monitorización de la base de datos'],
  serverMonitor: ['服务监控', 'Server monitoring', 'Monitorización del servidor'],
  apiDocs: ['API 文档', 'API documentation', 'Documentación de la API'],
  traceMonitor: ['链路追踪', 'Distributed tracing', 'Trazabilidad distribuida'],
  admin: ['Safillu 管理', 'Studio management', 'Gestión del estudio'],
  sharedData: [
    '与Safillu前端使用同一数据库及原生用户身份；此处展示实时业务记录。',
    'Live records from the same database and native user accounts as the creator site.',
    'Registros reales de la misma base de datos y las mismas cuentas nativas del sitio de creación.'
  ],
  search: ['搜索', 'Search', 'Buscar'],
  searchPlaceholder: [
    '搜索账号、名称、编号或提示词',
    'Search accounts, names, references or prompts',
    'Buscar cuentas, nombres, referencias o instrucciones'
  ],
  allStatuses: ['全部状态', 'All statuses', 'Todos los estados'],
  refresh: ['刷新', 'Refresh', 'Actualizar'],
  empty: ['暂无记录', 'No records', 'No hay registros'],
  actions: ['操作', 'Actions', 'Acciones'],
  detail: ['详情', 'Details', 'Detalles'],
  preview: ['预览', 'Preview', 'Vista previa'],
  edit: ['编辑', 'Edit', 'Editar'],
  restore: ['恢复', 'Restore', 'Restaurar'],
  trash: ['移入回收站', 'Move to trash', 'Mover a la papelera'],
  adjustCredits: ['调整积分', 'Adjust credits', 'Ajustar créditos'],
  enable: ['启用', 'Enable', 'Activar'],
  disable: ['停用', 'Disable', 'Desactivar'],
  yes: ['是', 'Yes', 'Sí'],
  no: ['否', 'No', 'No'],
  saved: ['已保存', 'Saved', 'Guardado'],
  delta: ['调整数量', 'Adjustment', 'Cantidad del ajuste'],
  reason: ['操作原因', 'Reason', 'Motivo'],
  editModel: ['编辑模型配置', 'Edit model settings', 'Editar configuración del modelo'],
  events: ['任务状态记录', 'Task status history', 'Historial de estados de la tarea'],
  creditHint: [
    '正数增加可用积分，负数扣减可用积分；冻结和已消耗积分不变。每次调整都会留下幂等流水和操作人。',
    'A positive amount adds available credits; a negative amount deducts them. Reserved and consumed credits stay unchanged. Each adjustment records its operator and an idempotent ledger entry.',
    'Un valor positivo añade créditos disponibles y uno negativo los descuenta. Los créditos reservados y consumidos no cambian. Cada ajuste registra al operador y un asiento idempotente.'
  ],
  creditValidation: [
    '请填写操作原因和非零调整数量',
    'Enter a reason and a nonzero adjustment',
    'Introduce un motivo y una cantidad distinta de cero'
  ],
  modelValidation: ['请填写模型名称', 'Enter a model name', 'Introduce el nombre del modelo'],
  modelHint: [
    '修改将影响前端之后提交的生成任务；已提交任务保留原始价格和参数快照。密钥不会返回到浏览器。',
    'Changes apply to future generation requests. Submitted tasks retain their original prices and parameter snapshots. Provider keys are never returned to the browser.',
    'Los cambios se aplican a las futuras solicitudes. Las tareas enviadas conservan sus precios y parámetros originales. Las claves del proveedor nunca se devuelven al navegador.'
  ],
  paymentHint: [
    '订单与前端充值记录、pay_order 使用同一套数据。TEST 为测试环境；正式收款仍需正式账户和套餐配置。',
    'Orders share the creator purchase records and pay_order data. TEST indicates the test environment. Live payments require a configured live account and packages.',
    'Los pedidos comparten los registros de recarga del sitio y los datos de pay_order. TEST indica el entorno de pruebas. Los cobros reales requieren una cuenta y paquetes de producción configurados.'
  ],
  workspaceConfirm: [
    '确认更改该创作空间的启用状态？停用后，该空间不能提交新的创作请求。',
    'Change this workspace status? A disabled workspace cannot submit new creation requests.',
    '¿Quieres cambiar el estado del espacio? Un espacio desactivado no puede enviar nuevas solicitudes de creación.'
  ],
  trashConfirm: [
    '确认将该资产移入用户回收站？可以恢复，不会删除原始文件。',
    'Move this asset to the user’s trash? It can be restored; the original file will be kept.',
    '¿Quieres mover este recurso a la papelera del usuario? Podrá restaurarse y se conservará el archivo original.'
  ],
  restoreConfirm: [
    '确认从用户回收站恢复该资产？',
    'Restore this asset from the user’s trash?',
    '¿Quieres restaurar este recurso desde la papelera del usuario?'
  ],
  workbench: ['管理工作台', 'Admin workspace', 'Panel de administración'],
  navigation: ['全部管理入口', 'Management modules', 'Módulos de administración'],
  overview: ['业务概览', 'Business overview', 'Resumen de actividad'],
  unconfigured: [
    '此服务地址尚未配置',
    'This service URL is not configured',
    'La URL de este servicio no está configurada'
  ],
  configureService: [
    '请在配置管理中设置对应服务地址后使用。',
    'Set the service URL in configuration management to use this module.',
    'Configura la URL del servicio en la gestión de configuración para usar este módulo.'
  ],
  configurationKey: ['配置键名', 'Configuration key', 'Clave de configuración'],
  openConfig: ['打开配置管理', 'Open configuration management', 'Abrir gestión de configuración']
} as const
const resources = {
  workspaces: ['用户空间与积分', 'User workspaces and credits', 'Espacios y créditos de usuarios'],
  tasks: ['生成任务', 'Generation tasks', 'Tareas de generación'],
  assets: ['资产管理', 'Asset management', 'Gestión de recursos'],
  models: ['生成模型', 'Generation models', 'Modelos de generación'],
  ledger: ['积分流水', 'Credit ledger', 'Movimientos de créditos'],
  orders: ['充值订单', 'Top-up orders', 'Pedidos de recarga'],
  packs: ['充值套餐', 'Credit packages', 'Paquetes de créditos'],
  conversations: ['创作对话', 'Conversations', 'Conversaciones'],
  subjects: ['主体管理', 'Subjects', 'Sujetos'],
  voices: ['音色管理', 'Voices', 'Voces'],
  motions: ['动作模板', 'Motion templates', 'Plantillas de movimiento']
} as const
const fields = {
  id: ['编号', 'ID', 'ID'],
  username: ['用户账号', 'Username', 'Usuario'],
  nickname: ['用户昵称', 'Display name', 'Nombre para mostrar'],
  userId: ['用户编号', 'User ID', 'ID de usuario'],
  name: ['名称', 'Name', 'Nombre'],
  title: ['标题', 'Title', 'Título'],
  status: ['状态', 'Status', 'Estado'],
  available: ['可用积分', 'Available credits', 'Créditos disponibles'],
  reserved: ['冻结积分', 'Reserved credits', 'Créditos reservados'],
  consumed: ['已消耗积分', 'Consumed credits', 'Créditos consumidos'],
  created: ['创建时间', 'Created on', 'Fecha de creación'],
  updated: ['更新时间', 'Updated on', 'Última actualización'],
  finished: ['完成时间', 'Finished on', 'Fecha de finalización'],
  taskNo: ['任务编号', 'Task reference', 'Referencia de tarea'],
  operation: ['创作类型', 'Operation', 'Tipo de creación'],
  modelName: ['模型名称', 'Model name', 'Nombre del modelo'],
  progress: ['进度（%）', 'Progress (%)', 'Progreso (%)'],
  prompt: ['提示词', 'Prompt', 'Instrucciones'],
  kind: ['类型', 'Type', 'Tipo'],
  source: ['来源', 'Source', 'Origen'],
  model: ['模型标识', 'Model identifier', 'Identificador del modelo'],
  platform: ['供应平台', 'Provider platform', 'Plataforma del proveedor'],
  billingUnit: ['计费单位', 'Billing unit', 'Unidad de consumo'],
  pointsPerUnit: ['单位积分', 'Credits per unit', 'Créditos por unidad'],
  configVersion: ['配置版本', 'Configuration version', 'Versión de configuración'],
  workspaceId: ['空间编号', 'Workspace ID', 'ID de espacio'],
  taskId: ['任务编号', 'Task ID', 'ID de tarea'],
  entryType: ['流水类型', 'Entry type', 'Tipo de movimiento'],
  availableDelta: ['可用变动', 'Available credit change', 'Cambio de créditos disponibles'],
  reservedDelta: ['冻结变动', 'Reserved credit change', 'Cambio de créditos reservados'],
  consumedDelta: ['消耗变动', 'Consumed credit change', 'Cambio de créditos consumidos'],
  availableAfter: ['操作后可用', 'Available credits after', 'Créditos disponibles después'],
  reservedAfter: ['操作后冻结', 'Reserved credits after', 'Créditos reservados después'],
  consumedAfter: ['操作后累计消耗', 'Consumed credits after', 'Créditos consumidos después'],
  remark: ['备注', 'Notes', 'Notas'],
  purchaseNo: ['订单编号', 'Order reference', 'Referencia de pedido'],
  packName: ['套餐名称', 'Package name', 'Nombre del paquete'],
  environment: ['环境', 'Environment', 'Entorno'],
  amountMinor: ['金额', 'Amount', 'Importe'],
  creditPoints: ['积分数量', 'Credits', 'Créditos'],
  credited: ['已到账', 'Credits granted', 'Créditos abonados'],
  code: ['编码', 'Code', 'Código'],
  enabled: ['启用状态', 'Enabled', 'Activado'],
  pinned: ['置顶', 'Pinned', 'Fijado'],
  version: ['版本', 'Version', 'Versión'],
  visibility: ['可见范围', 'Visibility', 'Visibilidad'],
  gender: ['性别', 'Gender', 'Género'],
  language: ['语言', 'Language', 'Idioma'],
  durationMs: ['时长（毫秒）', 'Duration (ms)', 'Duración (ms)'],
  width: ['宽度', 'Width', 'Ancho'],
  height: ['高度', 'Height', 'Alto'],
  description: ['说明', 'Description', 'Descripción'],
  errorCode: ['错误代码', 'Error code', 'Código de error'],
  errorMessage: ['状态说明', 'Status message', 'Información del estado'],
  retryOfTaskId: ['再次生成来源', 'Regenerated from task', 'Tarea de origen de la regeneración'],
  count: ['数量', 'Quantity', 'Cantidad'],
  parameters: ['任务参数', 'Task parameters', 'Parámetros de la tarea'],
  pricing: ['价格快照', 'Pricing snapshot', 'Precio al enviar la tarea'],
  defaults: ['默认参数', 'Default parameters', 'Parámetros predeterminados'],
  limits: ['生成限制', 'Generation limits', 'Límites de generación'],
  parameterSchema: ['参数定义', 'Parameter schema', 'Definición de parámetros'],
  modelId: ['模型编号', 'Model ID', 'ID del modelo'],
  payOrderId: ['芋道支付订单编号', 'Yudao payment order ID', 'ID de pedido de pago de Yudao'],
  checkoutSessionId: ['Stripe 会话编号', 'Stripe session ID', 'ID de sesión de Stripe'],
  paymentIntentId: ['支付意向编号', 'Payment intent ID', 'ID de intención de pago'],
  payOrderStatus: [
    '支付订单状态码',
    'Payment order status code',
    'Código de estado del pedido de pago'
  ],
  paid: ['支付时间', 'Paid on', 'Fecha de pago'],
  refundMinor: ['已退款金额', 'Refunded amount', 'Importe reembolsado'],
  currency: ['币种', 'Currency', 'Moneda'],
  creator: ['操作人编号', 'Operator ID', 'ID del operador'],
  businessKey: ['幂等业务编号', 'Idempotency reference', 'Referencia de idempotencia'],
  trashed: ['回收站状态', 'In trash', 'En la papelera'],
  isDefault: ['默认对话', 'Default conversation', 'Conversación predeterminada'],
  coverAssetId: ['封面资产编号', 'Cover asset ID', 'ID del recurso de portada'],
  ageGroup: ['年龄段', 'Age group', 'Grupo de edad'],
  billingHold: ['支付核对中', 'Billing review hold', 'Retención por revisión del pago']
} as const
const statuses = {
  ENABLED: ['已启用', 'Enabled', 'Activado'],
  DISABLED: ['已停用', 'Disabled', 'Desactivado'],
  PENDING: ['待处理', 'Pending', 'Pendiente'],
  QUEUED: ['排队中', 'Queued', 'En cola'],
  SUBMITTING: ['提交中', 'Submitting', 'Enviando'],
  RUNNING: ['生成中', 'Generating', 'Generando'],
  MATERIALIZING: ['归档中', 'Archiving', 'Archivando'],
  SUCCEEDED: ['成功', 'Succeeded', 'Completado'],
  PARTIAL_SUCCEEDED: ['部分成功', 'Partially succeeded', 'Completado parcialmente'],
  FAILED: ['失败', 'Failed', 'Fallido'],
  CANCELED: ['已取消', 'Canceled', 'Cancelado'],
  TIMED_OUT: ['已超时', 'Timed out', 'Tiempo agotado'],
  READY: ['可用', 'Ready', 'Disponible'],
  UPLOADING: ['上传中', 'Uploading', 'Subiendo'],
  TRASHED: ['回收站', 'Trash', 'Papelera'],
  GRANT: ['发放', 'Grant', 'Abono'],
  RESERVE: ['冻结', 'Reserve', 'Reserva'],
  SETTLE: ['结算', 'Settlement', 'Liquidación'],
  RELEASE: ['解冻', 'Release', 'Liberación'],
  ADJUST: ['人工调整', 'Manual adjustment', 'Ajuste manual'],
  CREATING: ['创建中', 'Creating', 'Creando'],
  OPEN: ['待支付', 'Awaiting payment', 'Pendiente de pago'],
  PROCESSING: ['处理中', 'Processing', 'Procesando'],
  PAID: ['已支付', 'Paid', 'Pagado'],
  EXPIRED: ['已过期', 'Expired', 'Caducado'],
  REVIEW: ['待核对', 'Under review', 'En revisión'],
  TEST: ['测试环境', 'Test', 'Pruebas'],
  LIVE: ['正式环境', 'Live', 'Producción'],
  DRAFT: ['草稿', 'Draft', 'Borrador'],
  PUBLIC: ['公开', 'Public', 'Público'],
  PRIVATE: ['私有', 'Private', 'Privado'],
  IMAGE: ['图片', 'Image', 'Imagen'],
  IMAGE_EDIT: ['图片编辑', 'Image editing', 'Edición de imágenes'],
  VIDEO: ['视频', 'Video', 'Vídeo'],
  AUDIO: ['音频', 'Audio', 'Audio'],
  MUSIC: ['音乐', 'Music', 'Música'],
  AVATAR: ['数字人', 'Digital human', 'Avatar digital'],
  MOTION: ['动作', 'Motion', 'Movimiento'],
  DOCUMENT: ['文档', 'Document', 'Documento'],
  UPLOAD: ['上传', 'Uploaded', 'Subido'],
  GENERATED: ['生成', 'Generated', 'Generado'],
  EDITED: ['编辑', 'Edited', 'Editado'],
  CHARACTER: ['角色 / 字符', 'Character', 'Personaje / carácter'],
  OBJECT: ['物品', 'Object', 'Objeto'],
  SCENE: ['场景', 'Scene', 'Escena'],
  SECOND: ['秒', 'Second', 'Segundo'],
  REQUEST: ['请求', 'Request', 'Solicitud']
} as const
const language = (data: Record<string, readonly string[]>, index: number) =>
  Object.fromEntries(Object.entries(data).map(([key, values]) => [key, values[index]]))
const build = (index: number) => ({
  ...language(copy, index),
  resources: language(resources, index),
  fields: language(fields, index),
  statuses: language(statuses, index)
})
export const businessZh = build(0),
  businessEn = build(1),
  businessEs = build(2)
