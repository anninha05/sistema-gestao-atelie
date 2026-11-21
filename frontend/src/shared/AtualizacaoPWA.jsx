import { useEffect, useState } from 'react';
import { useRegisterSW } from 'virtual:pwa-register/react';

export default function AtualizacaoPWA() {
  const [registro, setRegistro] = useState(null);
  const [atualizando, setAtualizando] = useState(false);
  const [erro, setErro] = useState('');
  const { needRefresh: [novaVersao], updateServiceWorker: aplicarAtualizacao } = useRegisterSW({
    immediate: true,
    onRegisteredSW: (_endereco, registroCriado) => setRegistro(registroCriado),
  });

  useEffect(() => {
    if (!registro) return;
    function verificar() {
      if (navigator.onLine && document.visibilityState === 'visible') {
        registro.update().catch(() => {});
      }
    }
    verificar();
    const intervalo = window.setInterval(verificar, 60000);
    window.addEventListener('focus', verificar);
    return () => {
      window.clearInterval(intervalo);
      window.removeEventListener('focus', verificar);
    };
  }, [registro]);

  if (!novaVersao) return null;

  async function atualizar() {
    setAtualizando(true);
    setErro('');
    try {
      await aplicarAtualizacao(true);
    } catch {
      setErro('Não foi possível atualizar. Verifique a conexão e tente novamente.');
      setAtualizando(false);
    }
  }

  return (
    <aside className="atualizacao-pwa" aria-label="Atualização do Linho">
      <div role="status"><strong>Nova versão disponível</strong>
        <p>Salve o que estiver preenchendo antes de atualizar.</p></div>
      {erro && <p role="alert">{erro}</p>}
      <button type="button" disabled={atualizando} onClick={atualizar}>
        {atualizando ? 'Atualizando…' : 'Atualizar Linho'}
      </button>
    </aside>
  );
}
