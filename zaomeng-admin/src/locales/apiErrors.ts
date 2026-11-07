// Map server-owned validation messages, never submitted content or arbitrary text.
export const apiErrorMessages = {
  'zh-CN': {
    credentials: '账号或密码不正确',
    disabled: '该账号已停用',
    password: '当前密码不正确',
    parameters: '请检查输入内容和参数范围',
    conflict: '内容已被其他操作更新，请刷新后重试。当前输入仍保留在此处。',
    mediaRequired: '发布前请上传封面和对应的原始媒体',
    mediaType: '媒体类型与内容类型不一致',
    mediaSize: '文件不能为空，且不能超过 50 MB',
    mediaFormat: '仅支持静态图片、音频和视频文件',
    mediaMissing: '媒体文件不存在或已无法访问',
    contentMissing: '内容不存在或已下架',
    model: '所选模型不可用，请选择其他模型',
    textLength: '请检查内容的文本长度',
    permission: '仅内容运营账号可以管理探索内容'
  },
  en: {
    credentials: 'Incorrect username or password',
    disabled: 'This account is disabled',
    password: 'The current password is incorrect',
    parameters: 'Check your input and parameter ranges',
    conflict:
      'This content was updated by another operation. Refresh and try again. Your current input is still shown here.',
    mediaRequired: 'Upload a cover and the corresponding original media before publishing',
    mediaType: 'The media type does not match the content type',
    mediaSize: 'The file must not be empty and must be 50 MB or smaller',
    mediaFormat: 'Only still images, audio and video files are supported',
    mediaMissing: 'The media file was not found or is no longer accessible',
    contentMissing: 'The content was not found or has been unpublished',
    model: 'The selected model is unavailable. Choose another model.',
    textLength: 'Check the length of the content text',
    permission: 'Only content operators can manage Explore content'
  },
  es: {
    credentials: 'Usuario o contraseña incorrectos',
    disabled: 'Esta cuenta está desactivada',
    password: 'La contraseña actual es incorrecta',
    parameters: 'Revisa los datos introducidos y los límites de los parámetros',
    conflict:
      'Otra operación ha actualizado este contenido. Actualiza la página e inténtalo de nuevo. Los datos introducidos siguen visibles aquí.',
    mediaRequired: 'Sube una portada y el archivo original correspondiente antes de publicar',
    mediaType: 'El tipo de archivo no coincide con el tipo de contenido',
    mediaSize: 'El archivo no puede estar vacío y debe tener un máximo de 50 MB',
    mediaFormat: 'Solo se admiten imágenes estáticas y archivos de audio o vídeo',
    mediaMissing: 'El archivo no existe o ya no está disponible',
    contentMissing: 'El contenido no existe o se ha retirado',
    model: 'El modelo seleccionado no está disponible. Selecciona otro.',
    textLength: 'Revisa la longitud del texto del contenido',
    permission: 'Solo los gestores de contenido pueden gestionar Explorar'
  }
}
export const serverMessageKeys: Record<string, keyof typeof apiErrorMessages.en> = {
  '登录失败，账号密码不正确': 'credentials',
  '登录失败，账号被禁用': 'disabled',
  用户密码校验失败: 'password',
  '探索内容已被修改，请刷新后重试': 'conflict',
  发布前请上传封面和对应的原始媒体: 'mediaRequired',
  探索媒体类型与内容类型不一致: 'mediaType',
  '单个文件需大于 0 且不超过 50MB': 'mediaSize',
  '探索媒体仅支持静态图片、音频和视频': 'mediaFormat',
  探索媒体不存在: 'mediaMissing',
  探索内容不存在: 'contentMissing',
  探索内容不存在或未发布: 'contentMissing',
  请检查探索内容的文本长度: 'textLength',
  探索参数超出允许范围: 'parameters',
  语速参数错误: 'parameters',
  探索配置必须是对象: 'parameters',
  未知探索分类: 'parameters',
  分页参数错误: 'parameters',
  未知发布状态: 'parameters',
  仅内容运营账号可以管理探索内容: 'permission'
}
