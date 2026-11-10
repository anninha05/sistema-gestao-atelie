import { useContext, useState } from 'react';
import { ContextoDeAutenticacao } from '../auth/AuthContext.jsx';
import { requisitarComCsrf } from '../../shared/api.js';
import { Aviso, Modal } from '../../shared/Interface.jsx';

export default function AcoesDaPeca({ peca, aoAtualizar }) {
  const { usuario } = useContext(ContextoDeAutenticacao);
  const [acao, setAcao] = useState(null);
  const [erro, setErro] = useState('');
  const [salvando, setSalvando] = useState(false);
  const acoes = [];
  if (['ROLE_ADMIN', 'ROLE_PRODUCAO'].includes(usuario.perfil)) {
    if (['DISPONIVEL', 'EM_LAVAGEM'].includes(peca.status)) acoes.push(['costura', 'Enviar para costura']);
    if (['DISPONIVEL', 'ALUGADA', 'EM_COSTURA', 'MANUTENCAO'].includes(peca.status)) acoes.push(['lavagem', 'Enviar para lavagem']);
  }
  if (usuario.perfil !== 'ROLE_PRODUCAO') {
    if (peca.status === 'DISPONIVEL') acoes.push(['alugar', 'Marcar como alugada']);
    if (['ALUGADA', 'EM_COSTURA', 'EM_LAVAGEM', 'MANUTENCAO'].includes(peca.status)) acoes.push(['devolver', 'Devolver ao estoque']);
  }

  async function confirmar() {
    setSalvando(true);
    setErro('');
    try {
      const { data } = await requisitarComCsrf('patch', '/pecas/' + peca.id + '/' + acao[0]);
      aoAtualizar(data);
      setAcao(null);
    } catch (falha) {
      setErro(falha.response?.data?.mensagem || 'Não foi possível atualizar a peça.');
    } finally { setSalvando(false); }
  }

  return <>
    <div className="fila-acoes">{acoes.map((item) => <button key={item[0]} type="button"
      className="botao-secundario" aria-haspopup="dialog" onClick={() => { setErro(''); setAcao(item); }}>{item[1]}</button>)}</div>
    {acao && <Modal titulo={acao[1]} ocupado={salvando} aoFechar={() => setAcao(null)}>
      <p>Confirma a alteração de estoque para {peca.descricao}?</p>
      <p>Esta ação atualiza a peça. O andamento da ordem é registrado na tela de ordens de serviço.</p>
      {erro && <Aviso>{erro}</Aviso>}
      <div className="modal-acoes"><button type="button" disabled={salvando} onClick={confirmar}>
        {salvando ? 'Atualizando…' : 'Confirmar'}
      </button></div>
    </Modal>}
  </>;
}
