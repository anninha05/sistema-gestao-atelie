import axios from 'axios';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  withCredentials: true,
  timeout: 10000,
});

export function enviarComCsrf(caminho, dados) {
  return requisitarComCsrf('post', caminho, dados);
}

export async function requisitarComCsrf(metodo, caminho, dados) {
  const { data: csrf } = await api.get('/auth/csrf');
  return api.request({
    method: metodo,
    url: caminho,
    data: dados,
    headers: { [csrf.headerName]: csrf.token },
  });
}
