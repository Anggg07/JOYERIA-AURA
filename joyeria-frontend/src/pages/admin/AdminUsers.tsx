import { useEffect, useState } from 'react'
import { adminService } from '../../services/admin.service'
import { useToast } from '../../contexts/ToastContext'
import type { User } from '../../types'
import Skeleton from '../../components/Skeleton'
import Pagination from '../../components/Pagination'
import { formatDate } from '../../utils/format'
import { getErrorMessage } from '../../utils/errors'

export default function AdminUsers() {
  const [users, setUsers] = useState<User[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const { toast } = useToast()

  useEffect(() => { load() }, [page])

  const load = async () => {
    setLoading(true)
    try {
      const data = await adminService.getUsers(page, 20)
      setUsers(data.content); setTotalPages(data.totalPages)
    } catch { setUsers([]); setTotalPages(0) }
    finally { setLoading(false) }
  }

  const changeRole = async (id: number, role: string) => {
    try { await adminService.updateUserRole(id, role); toast('Rol actualizado'); load() }
    catch (err: unknown) {
      const message = getErrorMessage(err, 'Error al actualizar rol')
      toast(message || 'Error al actualizar rol', 'error')
    }
  }

  const toggleActive = async (user: User) => {
    try {
      await adminService.toggleUserActive(user.id)
      toast(user.active ? 'Usuario desactivado' : 'Usuario activado', 'info')
      load()
    } catch (err: unknown) {
      const message = getErrorMessage(err, 'Error al actualizar')
      toast(message || 'Error al actualizar', 'error')
    }
  }

  const input = 'px-2 py-1 rounded-lg border border-line text-xs outline-none focus:border-[#C9A227] bg-surface'

  return (
    <div>
      <h1 className="font-serif text-2xl font-medium text-foreground mb-6">Usuarios</h1>

      {loading ? (
        <Skeleton className="h-80" />
      ) : (
        <div className="bg-surface rounded-lg border border-line/70 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-foreground-faint border-b border-line/70 bg-background-warm">
                  <th className="p-4">Usuario</th>
                  <th className="p-4">Email</th>
                  <th className="p-4">Registro</th>
                  <th className="p-4">Rol</th>
                  <th className="p-4">Estado</th>
                </tr>
              </thead>
              <tbody>
                {users.length === 0 ? (
                  <tr><td colSpan={5} className="p-8 text-center text-foreground-faint">No hay usuarios.</td></tr>
                ) : users.map(u => (
                  <tr key={u.id} className="border-b border-line/40 last:border-0 hover:bg-surface-muted/50">
                    <td className="p-4">
                      <p className="font-medium text-foreground">{u.firstName} {u.lastName}</p>
                      {u.phone && <p className="text-xs text-foreground-faint">{u.phone}</p>}
                    </td>
                    <td className="p-4 text-foreground-faint">{u.email}</td>
                    <td className="p-4 text-foreground-faint">{u.createdAt ? formatDate(u.createdAt) : '-'}</td>
                    <td className="p-4">
                      <select value={u.role} onChange={e => changeRole(u.id, e.target.value)} className={input}>
                        <option value="USER">Usuario</option>
                        <option value="ADMIN">Administrador</option>
                      </select>
                    </td>
                    <td className="p-4">
                      <button onClick={() => toggleActive(u)}
                        className={`text-xs px-3 py-1.5 rounded-full transition ${u.active ? 'bg-green-100 text-green-700 hover:bg-green-200' : 'bg-red-100 text-red-700 hover:bg-red-200'}`}>
                        {u.active ? 'Activo' : 'Bloqueado'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {totalPages > 1 && <div className="p-4 border-t border-line/70"><Pagination page={page} totalPages={totalPages} onPageChange={setPage} /></div>}
        </div>
      )}
    </div>
  )
}