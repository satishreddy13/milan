import axios from 'axios'

const client = axios.create({
  baseURL: '/api',   // proxied to http://localhost:38080/api by Vite
  headers: { 'Content-Type': 'application/json' },
})

export default client
