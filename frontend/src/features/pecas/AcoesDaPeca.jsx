import { useContext, useState } from 'react';
import { ContextoDeAutenticacao } from '../auth/AuthContext.jsx';
import { requisitarComCsrf } from '../../shared/api.js';
import { Aviso, Modal } from '../../shared/Interface.jsx';

export default function AcoesDaPeca({ peca, aoAtualizar }) {
  const { usuario } = useContext(ContextoDeAutenticacao);
  const [acao, setAcao] = useState(null);
  const [erro, setErro] = useState('');
  const [salvando, setSalvando] = useState(false);
  const [lavagemConcluida, setLavagemConcluida] = useState(false);
  const [condicoesConferidas, setCondicoesConferidas] = useState(false);
  const acoes = [];
  if (['ROLE_ADMIN', 'ROLE_PRODUCAO'].includes(usuario.perfil)) {
    if (['DISPONIVEL', 'EM_LAVAGEM'].includes(peca.status)) acoes.push(['costura', 'Enviar para costura/manutenção']);
    if (['DISPONIVEL', 'EM_COSTURA', 'MANUTENCAO'].includes(peca.status)) acoes.push(['lavagem', 'Enviar para lavagem']);
  }
  if (usuario.perfil !== 'ROLE_PRODUCAO') {
    if (peca.status === 'DISPONIVEL') acoes.push(['alugar', 'Marcar como alugada']);
  }
  if (['ROLE_ADMIN', 'ROLE_ATENDENTE', 'ROLE_PRODUCAO'].includes(usuario.perfil)
      && ['EM_COSTURA', 'EM_LAVAGEM', 'MANUTENCAO'].includes(peca.status)) {
    acoes.push(['devolver', 'Conferir e liberar peça']);
  }

  async function confirmar() {
    setSalvando(true);
    setErro('');
    try {
      const { data } = await requisitarComCsrf('patch', '/pecas/' + peca.id + '/' + acao[0],
        acao[0] === 'devolver' ? { lavagemConcluida, condicoesConferidas } : undefined);
      aoAtualizar(data);
      setAcao(null);
    } catch (falha) {
      setErro(falha.response?.data?.mensagem || 'Não foi possível atualizar a peça.');
    } finally { setSalvando(false); }
  }

  return <>
    <div className="fila-acoes">{acoes.map((item) => <button key={item[0]} type="button"
      className="botao-secundario" aria-haspopup="dialog" onClick={() => {
        setErro(''); setLavagemConcluida(false); setCondicoesConferidas(false); setAcao(item);
      }}>{item[1]}</button>)}</div>
    {peca.status === 'ALUGADA' && <p>Registre a devolução na ordem de serviço para encaminhar esta peça à lavagem.</p>}
    {peca.status === 'EM_LAVAGEM' && <p>Indisponível até concluir a lavagem e conferir as condições da peça.</p>}
    {acao && <Modal titulo={acao[1]} ocupado={salvando} aoFechar={() => setAcao(null)}>
      <p>Confirma a alteração de estoque para {peca.descricao}?</p>
      {acao[0] === 'devolver' ? <>
        <p>Libere somente após concluir a lavagem e os reparos necessários. Se houver pendências, mantenha a peça indisponível.</p>
        <div className="ordem-pecas">
          <label><input type="checkbox" checked={lavagemConcluida} disabled={salvando}
            onChange={(evento) => setLavagemConcluida(evento.target.checked)} /> Lavagem concluída</label>
          <label><input type="checkbox" checked={condicoesConferidas} disabled={salvando}
            onChange={(evento) => setCondicoesConferidas(evento.target.checked)} /> Peça conferida e apta para uso, sem reparos pendentes</label>
        </div>
      </> : <p>Esta ação atualiza a condição física da peça e preserva as reservas futuras.</p>}
      {erro && <Aviso>{erro}</Aviso>}
      <div className="modal-acoes"><button type="button"
        disabled={salvando || (acao[0] === 'devolver' && (!lavagemConcluida || !condicoesConferidas))} onClick={confirmar}>
        {salvando ? 'Atualizando…' : 'Confirmar'}
      </button></div>
    </Modal>}
  </>;
}
