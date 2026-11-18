import { useContext, useState } from 'react';
import { ContextoDeAutenticacao } from '../auth/AuthContext.jsx';
import { requisitarComCsrf, baixarPdf } from '../../shared/api.js';
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
  const podeReagendar = atendimento && !["RETIRADA", "ENTREGUE", "DEVOLVIDA", "CANCELADA"].includes(ordem.statusAtual);
  const [reagendamentoAberto, setReagendamentoAberto] = useState(false);
  const [erroDocumento, setErroDocumento] = useState('');
  const [gerandoDocumento, setGerandoDocumento] = useState(false);

  if (atendimento && ordem.statusAtual === 'CONCLUIDO') {
    acoes.push(['retirar', ordem.tipoFluxo === 'LOCACAO' ? 'Registrar retirada' : 'Registrar entrega']);
  }
  if (atendimento && ordem.tipoFluxo === 'LOCACAO' && ordem.statusAtual === 'RETIRADA') {
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
      setConfirmacao(acaoEscolhida[0] === 'devolver'
        ? 'Devolução registrada. As peças estão em lavagem; libere-as no inventário após a conferência.'
        : 'Estado da ordem atualizado.');
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
        {atendimento && ordem.tipoFluxo === 'LOCACAO' && <button type="button" className="botao-secundario"
          disabled={gerandoDocumento} onClick={async () => {
            setGerandoDocumento(true); setErroDocumento('');
            try { await baixarPdf('/ordens-servico/' + ordem.id + '/contrato.pdf', 'contrato-' + ordem.id + '.pdf'); }
            catch { setErroDocumento('Não foi possível gerar o contrato.'); }
            finally { setGerandoDocumento(false); }
          }}>{gerandoDocumento ? 'Gerando contrato…' : 'Baixar contrato PDF'}</button>}
        {podeReagendar && <button type="button" className="botao-secundario"
          onClick={() => { setErro(""); setReagendamentoAberto(true); }}>Reagendar</button>}
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
      {erroDocumento && <Aviso>{erroDocumento}</Aviso>}
      {reagendamentoAberto && <Modal titulo="Reagendar ordem" ocupado={salvando} aoFechar={() => setReagendamentoAberto(false)}>
        <form className="catalogo-form" onSubmit={async (evento) => {
          evento.preventDefault();
          const formulario = new FormData(evento.currentTarget);
          const inicio = formulario.get('inicioLocacao');
          const fim = formulario.get('fimLocacao');
          if (ordem.tipoFluxo === 'LOCACAO' && (!inicio || !fim || fim <= inicio)) {
            setErro('Informe um período de locação válido.');
            return;
          }
          setSalvando(true);
          setErro('');
          try {
            const { data } = await requisitarComCsrf('patch', '/ordens-servico/' + ordem.id + '/reagendar', {
              inicioLocacao: inicio || null, fimLocacao: fim || null,
              dataProva: formulario.get('dataProva') || null,
              dataEvento: formulario.get('dataEvento') || null,
              dataEntrega: formulario.get('dataEntrega') || null,
            });
            aoAtualizar(data);
            setReagendamentoAberto(false);
            setConfirmacao('Datas atualizadas. Se já houver contrato emitido, gere uma nova via.');
          } catch (falha) {
            setErro(falha.response?.data?.mensagem || 'Não foi possível reagendar a ordem.');
          } finally { setSalvando(false); }
        }} noValidate>
          {erro && <Aviso>{erro}</Aviso>}
          <fieldset disabled={salvando}>
            {ordem.tipoFluxo === 'LOCACAO' && <>
              <label>Início da locação<input name="inicioLocacao" type="datetime-local" defaultValue={ordem.inicioLocacao?.slice(0, 16)} required /></label>
              <label>Fim da locação<input name="fimLocacao" type="datetime-local" defaultValue={ordem.fimLocacao?.slice(0, 16)} required /></label>
            </>}
            <label>Data da prova<input name="dataProva" type="datetime-local" defaultValue={ordem.dataProva?.slice(0, 16)} /></label>
            <label>Data do evento<input name="dataEvento" type="datetime-local" defaultValue={ordem.dataEvento?.slice(0, 16)} /></label>
            <label>Data de entrega<input name="dataEntrega" type="datetime-local" defaultValue={ordem.dataEntrega?.slice(0, 16)} /></label>
          </fieldset>
          <div className="modal-acoes">
            <button type="button" className="botao-secundario" disabled={salvando} onClick={() => setReagendamentoAberto(false)}>Cancelar</button>
            <button type="submit" disabled={salvando}>{salvando ? "Salvando…" : "Salvar datas"}</button>
          </div>
        </form>
      </Modal>}
      {acaoEscolhida && (
        <Modal titulo={acaoEscolhida[1]} ocupado={salvando} aoFechar={() => setAcaoEscolhida(null)}>
          <p>Confirma esta ação na ordem de {ordem.clienteNome}?</p>
          {acaoEscolhida[0] === 'retirar' && (
            <p>A ordem precisa estar quitada e com a produção concluída.
              {ordem.tipoFluxo === 'LOCACAO' && ' As peças também precisam estar disponíveis no inventário e serão marcadas como alugadas.'}
            </p>
          )}
          {acaoEscolhida[0] === 'cancelar' && <p>O cancelamento encerra o fluxo desta ordem.</p>}
          {acaoEscolhida[0] === 'cancelar' && ordem.pendenciaAcerto && (
            <p>Há valores recebidos nesta ordem. O cancelamento manterá o histórico e registrará uma pendência de acerto, sem estorno automático.</p>
          )}
          {acaoEscolhida[0] === 'devolver' && (
            <p>Todas as peças desta locação serão encaminhadas à lavagem. Elas continuarão indisponíveis
              até a conclusão da lavagem e a liberação após conferência no inventário.</p>
          )}
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
