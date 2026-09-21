export function saveSession(token, username, role = 'ROLE_CUSTOMER') {
  sessionStorage.setItem('bank_token', token)
  sessionStorage.setItem('bank_username', username)
  sessionStorage.setItem('bank_role', role)
}

export function clearSession() {
  sessionStorage.removeItem('bank_token')
  sessionStorage.removeItem('bank_username')
  sessionStorage.removeItem('bank_role')
}

export function getToken() {
  return sessionStorage.getItem('bank_token') || ''
}

export function isLoggedIn() {
  return Boolean(getToken())
}

export function currentUsername() {
  return sessionStorage.getItem('bank_username') || ''
}

export function currentUserRole() {
  return sessionStorage.getItem('bank_role') || 'ROLE_CUSTOMER'
}

export function isAdmin() {
  return currentUserRole() === 'ROLE_ADMIN'
}
