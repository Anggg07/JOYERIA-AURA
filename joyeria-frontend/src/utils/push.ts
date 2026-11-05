import api from '../services/api'

function urlBase64ToUint8Array(base64String: string) {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4)
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/')
  const rawData = window.atob(base64)
  const outputArray = new Uint8Array(rawData.length)
  for (let i = 0; i < rawData.length; ++i) outputArray[i] = rawData.charCodeAt(i)
  return outputArray
}

export async function registerServiceWorker(): Promise<ServiceWorkerRegistration | null> {
  if (!('serviceWorker' in navigator)) return null
  try {
    return await navigator.serviceWorker.register('/sw.js')
  } catch (e) {
    console.warn('SW registration failed', e)
    return null
  }
}

export async function subscribeToPush(): Promise<boolean> {
  const reg = await registerServiceWorker()
  if (!reg || !('PushManager' in window)) return false
  try {
    const { data } = await api.get('/push/vapid-public-key')
    const publicKey = data?.data?.publicKey
    if (!publicKey) return false
    let sub = await reg.pushManager.getSubscription()
    if (!sub) {
      sub = await reg.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(publicKey),
      })
    }
    await api.post('/push/subscribe', {
      endpoint: sub.endpoint,
      p256dh: btoa(String.fromCharCode(...new Uint8Array(sub.getKey('p256dh')!))),
      auth: btoa(String.fromCharCode(...new Uint8Array(sub.getKey('auth')!))),
      userAgent: navigator.userAgent,
    })
    return true
  } catch (e) {
    console.warn('Push subscribe failed', e)
    return false
  }
}

export async function unsubscribeFromPush(): Promise<void> {
  const reg = await navigator.serviceWorker.getRegistration()
  if (!reg) return
  const sub = await reg.pushManager.getSubscription()
  if (sub) {
    await api.delete('/push/subscribe', { params: { endpoint: sub.endpoint } }).catch(() => {})
    await sub.unsubscribe()
  }
}