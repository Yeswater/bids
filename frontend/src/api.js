const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

export async function loadForm(modelCode, username, roles) {
  return request(`/api/runtime/models/${encodeURIComponent(modelCode)}/form`, {
    method: 'GET',
    username,
    roles
  })
}

export async function executeModel(modelCode, parameters, username, roles) {
  return request(`/api/runtime/models/${encodeURIComponent(modelCode)}/execute`, {
    method: 'POST',
    username,
    roles,
    body: JSON.stringify({ parameters })
  })
}

async function request(path, options) {
  const response = await fetch(`${API_BASE}${path}`, {
    method: options.method,
    headers: {
      'Content-Type': 'application/json',
      'X-Bids-User': options.username || 'demo-admin',
      'X-Bids-Roles': options.roles || 'ADMIN'
    },
    body: options.body
  })
  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    throw new Error(data.message || '请求失败')
  }
  return data
}
