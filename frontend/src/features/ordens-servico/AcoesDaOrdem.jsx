import { useContext, useState } from 'react';
import { ContextoDeAutenticacao } from '../auth/AuthContext.jsx';
import { requisitarComCsrf } from '../../shared/api.js';
import { Aviso, Modal } from '../../shared/Interface.jsx';

const acoesDeProducao = {
  PENDENTE: [['confeccao', 'Iniciar trabalho'], ['prova', 'Enviar para prova']],
  RESERVADA: [['confeccao', 'Iniciar trabalho'], ['prova', 'Enviar para prova']],
  CONFECCAO: [['prova', 'Enviar para prova']],
  PROVA: [['confeccao', 'Iniciar ajustes']],
  EM_PROVA: [['confeccao', 'Iniciar ajustes'], ['passadoria', 'Iniciar passadoria']],
  AJUSTES: [['prova', 'Enviar para prova'], ['passadoria', 'Iniciar passadoria']],
  PASSADORIA: [['concluir', 'Concluir produção']],
};

export default function AcoesDaOrdem({ ordem, aoAtualizar }) {
  const { usuario } = useContext(ContextoDeAutenticacao);
  const [acaoEscolhida, setAcaoEscolhida] = useState(null);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');
  const [confirmacao, setConfirmacao] = useState('');
  const acoes = usuario.perfil === 'ROLE_ADMIN'
    ? [...(acoesDeProducao[ordem.statusAtual] ?? [])] : [];
  const atendimento = ['ROLE_ADMIN', 'ROLE_ATENDENTE'].includes(usuario.perfil);

  if (atendimento && ordem.statusAtual === 'CONCLUIDO') {
    acoes.push(['retirar', ordem.tipoFluxo === 'LOCACAO' ? 'Registrar retirada' : 'Registrar entrega']);
  }
  if (atendimento && ordem.statusAtual === 'RETIRADA') {
    acoes.push(['devolver', 'Registrar devolução']);
  }
  if (atendimento && ['PENDENTE', 'RESERVADA', 'CONFECCAO', 'PROVA', 'EM_PROVA', 'AJUSTES', 'PASSADORIA', 'CONCLUIDO'].includes(ordem.statusAtual)) {
    acoes.push(['cancelar', 'Cancelar ordem']);
  }

  async function confirmar() {
    setSalvando(true);
    setErro('');
    try {
      const { data } = await requisitarComCsrf('patch', '/ordens-servico/' + ordem.id + '/' + acaoEscolhida[0]);
      aoAtualizar(data);
      setAcaoEscolhida(null);
      setConfirmacao('Estado da ordem atualizado.');
    } catch (falha) {
      setErro(falha.response?.data?.mensagem || 'Não foi possível atualizar a ordem. Tente novamente.');
    } finally {
      setSalvando(false);
    }
  }

  return (
    <div>
      {confirmacao && <Aviso tipo="sucesso">{confirmacao}</Aviso>}
      <div className="fila-acoes">
        {acoes.map(([acao, rotulo]) => (
          <button key={acao} type="button" aria-haspopup="dialog"
            className={acao === 'cancelar' ? 'botao-secundario' : undefined}
            onClick={() => {
              setErro('');
              setConfirmacao('');
              setAcaoEscolhida([acao, rotulo]);
            }}>{rotulo}</button>
        ))}
      </div>
      {acaoEscolhida && (
        <Modal titulo={acaoEscolhida[1]} ocupado={salvando} aoFechar={() => setAcaoEscolhida(null)}>
          <p>Confirma esta ação na ordem de {ordem.clienteNome}?</p>
          {acaoEscolhida[0] === 'cancelar' && <p>O cancelamento encerra o fluxo desta ordem.</p>}
          {erro && <Aviso>{erro}</Aviso>}
          <div className="modal-acoes">
            <button className="botao-secundario" type="button" disabled={salvando}
              onClick={() => setAcaoEscolhida(null)}>Voltar</button>
            <button type="button" disabled={salvando} onClick={confirmar}>
              {salvando ? 'Atualizando…' : 'Confirmar'}
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}
