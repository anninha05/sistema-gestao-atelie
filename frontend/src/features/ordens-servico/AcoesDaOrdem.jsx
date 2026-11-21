import { useContext, useState } from 'react';
import { ContextoDeAutenticacao } from '../auth/AuthContext.jsx';
import { requisitarComCsrf, baixarPdf } from '../../shared/api.js';
import { Aviso, Modal } from '../../shared/Interface.jsx';
import { acoesDiretas, oferece, rotasDasAcoes } from './acoes.js';

export default function AcoesDaOrdem({ ordem, aoAtualizar }) {
  const { usuario } = useContext(ContextoDeAutenticacao);
  const [acaoEscolhida, setAcaoEscolhida] = useState(null);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');
  const [confirmacao, setConfirmacao] = useState('');
  const [reagendamentoAberto, setReagendamentoAberto] = useState(false);
  const [erroDocumento, setErroDocumento] = useState('');
  const [gerandoDocumento, setGerandoDocumento] = useState(false);
  const acoes = acoesDiretas(ordem);
  const podeReagendar = oferece(ordem, 'REAGENDAR');
  const podeEmitirContrato = ['ROLE_ADMIN', 'ROLE_ATENDENTE'].includes(usuario.perfil) && ordem.tipoFluxo === 'LOCACAO';
  const bloqueios = acoes.filter((item) => !item.habilitada);

  async function confirmar() {
    setSalvando(true);
    setErro('');
    try {
      const { data } = await requisitarComCsrf('patch', '/ordens-servico/' + ordem.id + '/' + rotasDasAcoes[acaoEscolhida.acao]);
      aoAtualizar(data);
      setAcaoEscolhida(null);
      setConfirmacao(acaoEscolhida.acao === 'DEVOLVER'
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
        {podeEmitirContrato && <button type="button" className="botao-secundario"
          disabled={gerandoDocumento} onClick={async () => {
            setGerandoDocumento(true); setErroDocumento('');
            try { await baixarPdf('/ordens-servico/' + ordem.id + '/contrato.pdf', 'contrato-' + ordem.id + '.pdf'); }
            catch { setErroDocumento('Não foi possível gerar o contrato.'); }
            finally { setGerandoDocumento(false); }
          }}>{gerandoDocumento ? 'Gerando contrato…' : 'Baixar contrato PDF'}</button>}
        {podeReagendar && <button type="button" className="botao-secundario"
          onClick={() => { setErro(""); setReagendamentoAberto(true); }}>Reagendar</button>}
        {acoes.map((item) => (
          <button key={item.acao} type="button" aria-haspopup="dialog"
            className={item.acao === 'CANCELAR' ? 'botao-secundario' : undefined}
            disabled={!item.habilitada}
            aria-describedby={item.habilitada ? undefined : 'bloqueio-' + ordem.id + '-' + item.acao}
            onClick={() => {
              setErro('');
              setConfirmacao('');
              setAcaoEscolhida(item);
            }}>{item.rotulo}</button>
        ))}
      </div>
      {bloqueios.map((item) => (
        <p key={item.acao} id={'bloqueio-' + ordem.id + '-' + item.acao} className="acao-bloqueada">
          {item.rotulo}: {item.motivoBloqueio}
        </p>
      ))}
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
        <Modal titulo={acaoEscolhida.rotulo} ocupado={salvando} aoFechar={() => setAcaoEscolhida(null)}>
          <p>Confirma esta ação na ordem de {ordem.clienteNome}?</p>
          {acaoEscolhida.acao === 'RETIRAR' && (
            <p>A ordem precisa estar quitada e com a produção concluída.
              {ordem.tipoFluxo === 'LOCACAO' && ' As peças também precisam estar disponíveis no inventário e serão marcadas como alugadas.'}
            </p>
          )}
          {acaoEscolhida.acao === 'CANCELAR' && <p>O cancelamento encerra o fluxo desta ordem.</p>}
          {acaoEscolhida.acao === 'CANCELAR' && ordem.pendenciaAcerto && (
            <p>Há valores recebidos nesta ordem. O cancelamento manterá o histórico e registrará uma pendência de acerto, sem estorno automático.</p>
          )}
          {acaoEscolhida.acao === 'DEVOLVER' && (
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
