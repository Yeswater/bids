const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8082'

export async function loadForm(modelCode, username, password) {
  return request(`/api/runtime/models/${encodeURIComponent(modelCode)}/form`, {
    method: 'GET',
    username,
    password
  })
}

export async function executeModel(modelCode, parameters, username, password) {
  return request(`/api/runtime/models/${encodeURIComponent(modelCode)}/execute`, {
    method: 'POST',
    username,
    password,
    body: JSON.stringify({ parameters })
  })
}

async function request(path, options) {
  const response = await fetch(`${API_BASE}${path}`, {
    method: options.method,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': basicAuth(options.username || 'admin', options.password || 'admin')
    },
    body: options.body
  })
  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    throw new Error(data.message || '请求失败')
  }
  return data
}

function basicAuth(username, password) {
  return `Basic ${btoa(`${username}:${password}`)}`
}
