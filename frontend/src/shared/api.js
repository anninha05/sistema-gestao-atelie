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

export async function baixarPdf(caminho, nome) {
  const { data } = await api.get(caminho, { responseType: 'blob' });
  const endereco = URL.createObjectURL(data);
  const link = document.createElement('a');
  link.href = endereco;
  link.download = nome;
  link.click();
  setTimeout(() => URL.revokeObjectURL(endereco), 1000);
}
