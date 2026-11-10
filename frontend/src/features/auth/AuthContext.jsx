import { createContext, useEffect, useState } from 'react';
import { api, enviarComCsrf } from '../../shared/api.js';

export const ContextoDeAutenticacao = createContext(null);

export function ProvedorDeAutenticacao({ children }) {
  const [usuario, setUsuario] = useState(null);
  const [carregando, setCarregando] = useState(true);
  const [erroDaSessao, setErroDaSessao] = useState('');

  useEffect(() => {
    const controlador = new AbortController();
    api.get('/auth/me', { signal: controlador.signal })
      .then(({ data }) => setUsuario(data))
      .catch((erro) => {
        if (!controlador.signal.aborted && erro.response?.status !== 401) {
          setErroDaSessao('Não foi possível consultar a sessão. Verifique se a API está disponível.');
        }
      })
      .finally(() => {
        if (!controlador.signal.aborted) setCarregando(false);
      });

    return () => controlador.abort();
  }, []);

  async function entrar(email, senha) {
    setErroDaSessao('');
    await enviarComCsrf('/auth/login', new URLSearchParams({ email, password: senha }));
    const { data } = await api.get('/auth/me');
    setUsuario(data);
  }

  async function sair() {
    await enviarComCsrf('/auth/logout');
    setUsuario(null);
  }

  return (
    <ContextoDeAutenticacao.Provider value={{ usuario, carregando, erroDaSessao, entrar, sair }}>
      {children}
    </ContextoDeAutenticacao.Provider>
  );
}
