import { useI18n } from '@/hooks/web/useI18n'
import { serverMessageKeys } from '@/locales/apiErrors'

export class ApiError extends Error {
  constructor(
    public code: number,
    public serverMessage: string
  ) {
    super(serverMessage)
    this.name = 'ApiError'
  }
}

export const apiErrorText = (code: number, message = ''): string => {
  const { t } = useI18n()
  const authKey = (
    { 1002000000: 'credentials', 1002000001: 'disabled', 1002003005: 'password' } as Record<
      number,
      string
    >
  )[code]
  const key = authKey || serverMessageKeys[message]
  if (key) return t(`apiErrors.${key}`)
  if (code === 409) return t('apiErrors.conflict')
  if ([401, 403, 404, 405, 408, 500, 501, 502, 503, 504, 505, 901].includes(code))
    return t(`sys.api.errMsg${code}`)
  if (code === 400) return t('apiErrors.parameters')
  return t('sys.api.apiRequestFailed')
}

export const errorText = (error: unknown): string => {
  if (error instanceof ApiError) return apiErrorText(error.code, error.serverMessage)
  const { t } = useI18n()
  return t('sys.api.apiRequestFailed')
}
