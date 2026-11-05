import { test, expect } from '@playwright/test'

test.describe('Flujo de compra (camino feliz)', () => {
  test.beforeEach(async ({ context }) => {
    await context.addInitScript(() => {
      localStorage.setItem('cookieConsent', JSON.stringify({ type: 'all', ts: Date.now(), version: 1 }))
    })
  })

  test('registro -> carrito -> checkout -> pago (sandbox) -> pedido confirmado', async ({ page }) => {
    const stamp = Date.now()
    const email = `e2e${stamp}@example.com`
    const phone = '3' + String(Math.floor(100000000 + Math.random() * 899999999))

    // 1. Registro
    await page.goto('/registro')
    await page.fill('input[name="firstName"]', 'E2E')
    await page.fill('input[name="lastName"]', 'Tester')
    await page.fill('input[name="email"]', email)
    await page.fill('input[name="phone"]', phone)
    await page.fill('input[name="password"]', 'Test1234')
    await page.fill('input[name="confirmPassword"]', 'Test1234')
    await page.getByRole('checkbox').check()
    await page.getByRole('button', { name: 'Crear cuenta' }).click()

    // Vuelve al inicio autenticado
    await expect(page).toHaveURL(/localhost:5173\/(\?.*)?$/)

    // 2. Catálogo -> agregar primer producto con stock
    await page.goto('/productos')
    const addButton = page.locator('button:enabled', { hasText: 'Agregar al carrito' }).first()
    await addButton.scrollIntoViewIfNeeded()
    await addButton.click()

    // 3. Carrito
    await page.goto('/carrito')
    await expect(page.getByRole('heading', { name: 'Carrito de compras' })).toBeVisible()
    await page.getByRole('button', { name: /Proceder al pago/ }).click()

    // 4. Checkout - datos de envío
    await expect(page.getByRole('heading', { name: 'Finalizar compra' })).toBeVisible()
    await page.getByPlaceholder('Calle 123 # 45-67, Apto 301').fill('Calle 1 # 2-3')
    await page.getByPlaceholder('Bogotá').fill('Bogotá')
    await page.getByPlaceholder('María García').fill('E2E Tester')
    await page.getByPlaceholder('+57 300 000 0000').fill('3001234567')
    await page.getByRole('button', { name: /Continuar al pago/ }).click()

    // 5. Checkout - pago
    const payButton = page.getByRole('button', { name: /^Pagar/ })
    await expect(payButton).toBeVisible()
    await payButton.click()

    // 6. Pasarela simulada (sandbox)
    await expect(page).toHaveURL(/\/pago\/sandbox/)
    const sandboxPay = page.getByRole('button', { name: /^Pagar/ })
    await expect(sandboxPay).toBeVisible()
    await sandboxPay.click()

    // 7. Confirmación del pedido
    await expect(page).toHaveURL(/\/pedido-confirmado\//)
  })
})
