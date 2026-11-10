import { useEffect, useState } from 'react';
import { api, requisitarComCsrf } from '../../shared/api.js';
import { Aviso, Modal, Selecao, validarCampos } from '../../shared/Interface.jsx';

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const metodos = [['PIX', 'Pix'], ['CARTAO', 'Cartão'], ['DINHEIRO', 'Dinheiro'], ['TRANSFERENCIA', 'Transferência']];
const situacoes = { PENDENTE: 'Pendente', SINAL_PAGO: 'Pagamento parcial', QUITADO: 'Quitado' };

function ConteudoPagamentos({ ordem, aoFechar }) {
  const [pagamento, setPagamento] = useState(null);
  const [erro, setErro] = useState('');
  const [salvando, setSalvando] = useState(false);
  const [tentativa, setTentativa] = useState(0);
  const [confirmacao, setConfirmacao] = useState('');
  const [precisaAtualizar, setPrecisaAtualizar] = useState(false);
  const [consultando, setConsultando] = useState(true);
  const caminho = '/ordens-servico/' + ordem.id + '/pagamentos';

  useEffect(() => {
    const controlador = new AbortController();
    setConsultando(true);
    api.get(caminho, { signal: controlador.signal })
      .then(({ data }) => {
        if (controlador.signal.aborted) return;
        setPagamento(data);
        setPrecisaAtualizar(false);
        if (tentativa > 0) setConfirmacao('Pagamentos atualizados com os dados mais recentes.');
      })
      .catch(() => {
        if (!controlador.signal.aborted) setErro('Não foi possível consultar os pagamentos.');
      })
      .finally(() => {
        if (!controlador.signal.aborted) setConsultando(false);
      });
    return () => controlador.abort();
  }, [caminho, tentativa]);

  async function salvar(evento) {
    evento.preventDefault();
    if (salvando || consultando || precisaAtualizar) return;
    const formulario = evento.currentTarget;
    const mensagem = validarCampos(formulario);
    if (mensagem) { setErro(mensagem); return; }
    const dados = new FormData(formulario);
    setSalvando(true);
    setErro('');
    setConfirmacao('');
    try {
      const { data } = await requisitarComCsrf('post', caminho + (dados.get('sinal') ? '/sinal' : ''), {
        valor: Number(dados.get('valor')), data: dados.get('data'), metodo: dados.get('metodo'),
      });
      setPagamento(data);
      formulario.reset();
      setConfirmacao('Recebimento registrado.');
    } catch (falha) {
      setPrecisaAtualizar(true);
      setErro(falha.response?.data?.mensagem || 'Não foi possível registrar. Consulte os pagamentos antes de tentar novamente.');
    } finally { setSalvando(false); }
  }

  return (
    <Modal titulo={'Pagamentos de ' + ordem.clienteNome} aoFechar={aoFechar} ocupado={salvando}>
      {erro && <Aviso>{erro}</Aviso>}
      {!pagamento && !erro && <p role="status">Consultando pagamentos…</p>}
      <button type="button" className="botao-secundario" disabled={salvando || consultando} onClick={() => {
        setErro('');
        setConfirmacao('');
        setConsultando(true);
        setTentativa((valor) => valor + 1);
      }}>{consultando ? 'Atualizando…' : 'Atualizar pagamentos'}</button>
      {confirmacao && <Aviso tipo="sucesso">{confirmacao}</Aviso>}
      {pagamento && <>
        {pagamento.saldoDevedor === 0 && <p>Esta ordem está quitada. Todos os recebimentos estão no histórico abaixo.</p>}
        <dl>
          <dt>Total</dt><dd>{moeda.format(pagamento.valorTotal)}</dd>
          <dt>Recebido</dt><dd>{moeda.format(pagamento.valorPago)}</dd>
          <dt>Saldo devedor</dt><dd>{moeda.format(pagamento.saldoDevedor)}</dd>
          <dt>Situação</dt><dd>{situacoes[pagamento.statusFinanceiro]}</dd>
        </dl>
        <h3>Histórico de recebimentos</h3>
        {pagamento.recebimentos.length === 0 && <p>Nenhum recebimento registrado.</p>}
        <ul>{pagamento.recebimentos.map((item, indice) => (
          <li key={indice}>{item.data.split('-').reverse().join('/')} · {moeda.format(item.valor)} · {metodos.find(([valor]) => valor === item.metodo)?.[1]}
            {item.sinal ? ' · Sinal' : ''}</li>
        ))}</ul>
        {pagamento.saldoDevedor > 0 && (
          <form className="catalogo-form" noValidate onSubmit={salvar}>
            <fieldset disabled={salvando || consultando || precisaAtualizar}>
              <label>Valor (R$)<input name="valor" type="number" min="0.01" step="0.01"
                max={pagamento.saldoDevedor} inputMode="decimal" required /></label>
              <label>Data do recebimento<input name="data" type="date" required /></label>
              <Selecao nome="metodo" rotulo="Método de pagamento" opcoes={metodos} obrigatorio />
              {!pagamento.recebimentos.some((item) => item.sinal) && (
                <div className="ordem-pecas"><label><input name="sinal" type="checkbox" /> Registrar como sinal</label></div>
              )}
            </fieldset>
            <div className="modal-acoes"><button disabled={salvando || consultando || precisaAtualizar} type="submit">
              {salvando ? 'Registrando…' : 'Registrar recebimento'}
            </button></div>
          </form>
        )}
      </>}
    </Modal>
  );
}

export default function PagamentosDaOrdem({ ordem }) {
  const [aberto, setAberto] = useState(false);
  return <>
    <button className="botao-secundario" type="button" aria-haspopup="dialog" onClick={() => setAberto(true)}>Pagamentos e saldo</button>
    {aberto && <ConteudoPagamentos ordem={ordem} aoFechar={() => setAberto(false)} />}
  </>;
}
