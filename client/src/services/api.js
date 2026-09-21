const rawApiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'
const API_URL = rawApiUrl.replace(/\/+$/, '')

function authHeader() {
  const token = sessionStorage.getItem('bank_token')
  if (!token) return {}
  return { Authorization: `Bearer ${token}` }
}

async function request(path, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...authHeader(),
    ...(options.headers || {})
  }

  const response = await fetch(`${API_URL}${path}`, { ...options, headers })
  const text = await response.text()
  let data = null
  try { data = text ? JSON.parse(text) : null } catch { data = text }

  if (!response.ok) {
    const message = data?.message || data || `Request failed (${response.status})`
    throw new Error(message)
  }
  return data
}

export const api = {
  register: (payload) => request('/users/register', { method: 'POST', body: JSON.stringify(payload) }),
  login: (payload) => request('/users/login', { method: 'POST', body: JSON.stringify(payload) }),
  me: () => request('/users/me'),
  searchRecipients: (query = '') => request(`/users/recipients?query=${encodeURIComponent(query)}`),
  accounts: () => request('/accounts'),
  createAccount: (type) => request('/accounts', { method: 'POST', body: JSON.stringify({ type }) }),
  deposit: (accNo, amount) => request(`/accounts/${accNo}/deposit`, { method: 'POST', body: JSON.stringify({ amount: Number(amount) }) }),
  withdraw: (accNo, amount) => request(`/accounts/${accNo}/withdraw`, { method: 'POST', body: JSON.stringify({ amount: Number(amount) }) }),
  transfer: (fromAccNo, toAccNo, amount, recipientUsername) => request('/accounts/transfer', {
    method: 'POST',
    body: JSON.stringify({
      fromAccNo: Number(fromAccNo),
      toAccNo: toAccNo ? Number(toAccNo) : null,
      recipientUsername: recipientUsername || null,
      amount: Number(amount)
    })
  }),
  closeAccount: (accNo) => request(`/accounts/${accNo}/close`, { method: 'POST' }),
  transactions: (accNo) => request(`/transactions/${accNo}`),

  admin: {
    stats: () => request('/admin/stats'),
    users: () => request('/admin/users'),
    user: (userId) => request(`/admin/users/${userId}`),
    updateUserStatus: (userId, status) => request(`/admin/users/${userId}/status`, { method: 'PUT', body: JSON.stringify({ status }) }),
    updateUserRole: (userId, role) => request(`/admin/users/${userId}/role`, { method: 'PUT', body: JSON.stringify({ role }) }),
    accounts: () => request('/admin/accounts'),
    updateAccountStatus: (accNo, status) => request(`/admin/accounts/${accNo}/status`, { method: 'PUT', body: JSON.stringify({ status }) }),
    activities: () => request('/admin/activities')
  }
}
