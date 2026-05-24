import { toast } from 'sonner'

export function notifySuccess(message: string) {
  toast.success(message)
}

export function notifyError(error: unknown, fallbackMessage = '요청에 실패했습니다.') {
  const message =
    error instanceof Error ? error.message : typeof error === 'string' ? error : fallbackMessage
  toast.error(message || fallbackMessage)
}

export function notifyInfo(message: string) {
  toast.info(message)
}

export function notifyWarning(message: string) {
  toast.warning(message)
}
