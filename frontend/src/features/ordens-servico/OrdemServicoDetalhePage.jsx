import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../../shared/api.js';
import { Aviso, Etiqueta, Icone } from '../../shared/Interface.jsx';
import { dataHora, estadoDaOrdem, moeda, tiposFluxo } from '../../shared/formatos.js';
import AcoesDaOrdem from './AcoesDaOrdem.jsx';
import PagamentosDaOrdem from './PagamentosDaOrdem.jsx';
import './ordem.css';

const etapasPorFluxo = {
  LOCACAO: [
    ['Reserva', 'Peças reservadas', ['PENDENTE', 'RESERVADA']],
    ['Ajustes e prova', 'Conferência e prova', ['AJUSTES', 'PROVA', 'EM_PROVA']],
    ['Passadoria', 'Preparação final', ['PASSADORIA']],
    ['Pronta', 'Aguardando retirada', ['CONCLUIDO']],
    ['Retirada', 'Em locação', ['RETIRADA']],
    ['Devolução', 'Locação encerrada', ['DEVOLVIDA']],
  ],
  SOB_MEDIDA: [
    ['Reserva', 'Pedido confirmado', ['PENDENTE', 'RESERVADA']],
    ['Confecção', 'Corte e costura', ['CONFECCAO']],
    ['Prova e ajustes', 'Caimento e acabamento', ['PROVA', 'EM_PROVA', 'AJUSTES']],
    ['Passadoria', 'Preparação final', ['PASSADORIA']],
    ['Pronta', 'Aguardando entrega', ['CONCLUIDO']],
    ['Entrega', 'Peça entregue', ['ENTREGUE']],
  ],
};
const situacoes = { PENDENTE: ['Pendente', 'amarelo'], SINAL_PAGO: ['Pagamento parcial', 'amarelo'], QUITADO: ['Quitado', 'verde'] };
const metodos = { PIX: 'Pix', CARTAO: 'Cartão', DINHEIRO: 'Dinheiro', TRANSFERENCIA: 'Transferência' };

function formatarData(valor) {
  return valor ? dataHora.format(new Date(valor)) : '—';
}

function FluxoDaOrdem({ ordem }) {
  const etapas = etapasPorFluxo[ordem.tipoFluxo] ?? [];
  const cancelada = ordem.statusAtual === 'CANCELADA';
  const atual = etapas.findIndex(([, , estados]) => estados.includes(ordem.statusAtual));
  return (
    <section className="cartao ordem-fluxo" aria-labelledby="ordem-fluxo-titulo">
      <div className="ordem-cartao-topo">
        <h2 id="ordem-fluxo-titulo">Acompanhamento do fluxo</h2>
        <span className="ordem-fluxo-resumo">
          {cancelada ? 'Ordem cancelada' : `${atual + 1} de ${etapas.length} etapas · ${etapas[atual]?.[0] ?? ''}`}
        </span>
      </div>
      <ol>
        {etapas.map(([rotulo, detalhe], indice) => (
          <li key={rotulo} className={cancelada ? '' : indice < atual ? 'concluida' : indice === atual ? 'atual' : ''}
            aria-current={!cancelada && indice === atual ? 'step' : undefined}>
            <strong>{rotulo}</strong>
            <span>{detalhe}</span>
          </li>
        ))}
      </ol>
    </section>
  );
}

export default function OrdemServicoDetalhePage() {
  const { id } = useParams();
  const [dados, setDados] = useState(null);
  const [erro, setErro] = useState(false);
  const [tentativa, setTentativa] = useState(0);

  useEffect(() => {
    const controlador = new AbortController();
    const configuracao = { signal: controlador.signal };
    setErro(false);
    Promise.all([
      api.get('/ordens-servico/' + id, configuracao),
      api.get('/ordens-servico/' + id + '/pagamentos', configuracao),
      api.get('/pecas', configuracao),
    ]).then(async ([ordem, pagamento, pecas]) => {
      const cliente = await api.get('/clientes/' + ordem.data.clienteId, configuracao)
        .then(({ data }) => data).catch(() => null);
      setDados({ ordem: ordem.data, pagamento: pagamento.data, pecas: new Map(pecas.data.map((peca) => [peca.id, peca])), cliente });
    }).catch(() => {
      if (!controlador.signal.aborted) setErro(true);
    });
    return () => controlador.abort();
  }, [id, tentativa]);

  function recarregar() {
    setTentativa((anterior) => anterior + 1);
  }

  if (erro) {
    return (
      <div>
        <Aviso>Não foi possível carregar esta ordem de serviço.</Aviso>
        <div className="fila-acoes">
          <button type="button" onClick={recarregar}>Tentar novamente</button>
          <Link className="botao-link botao-secundario" to="/ordens-servico">Voltar para a lista</Link>
        </div>
      </div>
    );
  }

  if (!dados) return <p role="status">Carregando ordem de serviço…</p>;

  const { ordem, pagamento, pecas, cliente } = dados;
  const [estado, tom] = estadoDaOrdem(ordem.statusAtual);
  const [situacao, tomSituacao] = situacoes[pagamento.statusFinanceiro] ?? [pagamento.statusFinanceiro, 'neutro'];
  const subtotal = ordem.itens.reduce((soma, item) => soma + Number(item.valorUnitario), 0);
  const desconto = Number(ordem.desconto ?? 0);
  const ajuste = Number(ordem.valorTotal) - (subtotal - desconto);
  const locacao = ordem.tipoFluxo === 'LOCACAO';
  const dias = locacao && ordem.inicioLocacao && ordem.fimLocacao
    ? Math.max(1, Math.ceil((new Date(ordem.fimLocacao) - new Date(ordem.inicioLocacao)) / 86400000)) : null;
  const historico = [
    ...pagamento.recebimentos.map((item) => ({
      chave: item.id, data: item.data + 'T12:00:00',
      titulo: (item.sinal ? 'Sinal recebido' : 'Pagamento recebido') + ' via ' + (metodos[item.metodo] ?? item.metodo) + ' · ' + moeda.format(item.valor),
      texto: item.data.split('-').reverse().join('/'),
    })),
    ...(ordem.criadoEm ? [{ chave: 'criacao', data: ordem.criadoEm, titulo: 'Ordem criada · ' + tiposFluxo[ordem.tipoFluxo], texto: formatarData(ordem.criadoEm) }] : []),
  ].sort((a, b) => b.data.localeCompare(a.data));

  return (
    <section className="ordem-detalhe" aria-labelledby="ordem-titulo">
      <header className="cabecalho-pagina">
        <div>
          <Link className="ordem-voltar" to="/ordens-servico"><Icone nome="voltar" /> Ordens de serviço</Link>
          <h1 id="ordem-titulo">Ordem de {ordem.clienteNome}</h1>
          <p className="intro">
            {tiposFluxo[ordem.tipoFluxo]}{ordem.criadoEm ? ' · Criada em ' + formatarData(ordem.criadoEm) : ''}
          </p>
        </div>
        <Etiqueta tom={tom}>{estado}</Etiqueta>
      </header>

      <FluxoDaOrdem ordem={ordem} />

      <div className="ordem-grade">
        <div className="ordem-coluna">
          <section className="cartao" aria-labelledby="ordem-cliente">
            <div className="ordem-cartao-topo">
              <h2 id="ordem-cliente">Cliente</h2>
              <Link className="ordem-link" to={'/clientes#' + ordem.clienteId}>Ver cadastro <Icone nome="avancar" /></Link>
            </div>
            <p className="ordem-cliente-nome">{ordem.clienteNome}</p>
            {cliente && <p className="ordem-cliente-contato">
              <span>CPF {cliente.cpf.replace(/^(\d{3})(\d{3})(\d{3})(\d{2})$/, '$1.$2.$3-$4')}</span>
              <span>{cliente.telefone}</span>
              <span>{cliente.email}</span>
            </p>}
          </section>

          <section className="cartao ordem-pecas-cartao" aria-labelledby="ordem-pecas">
            <h2 id="ordem-pecas">Peças da ordem · {ordem.itens.length}</h2>
            <div className="ordem-tabela">
              <table>
                <thead><tr><th>Descrição</th><th>Tamanho</th><th>Cor</th><th>Valor</th></tr></thead>
                <tbody>
                  {ordem.itens.map((item) => {
                    const peca = pecas.get(item.pecaId);
                    return (
                      <tr key={item.pecaId}>
                        <td>
                          <strong>{peca?.descricao ?? 'Peça'}</strong>
                          <span>{peca?.codigoEtiqueta}</span>
                          {(item.medidas || item.instrucoesCostura) && (
                            <span className="ordem-observacao">{[item.medidas, item.instrucoesCostura].filter(Boolean).join(' · ')}</span>
                          )}
                        </td>
                        <td>{peca?.tamanho || '—'}</td>
                        <td>{peca?.cor || '—'}</td>
                        <td>{moeda.format(item.valorUnitario)}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>

          <section className="cartao" aria-labelledby="ordem-agenda">
            <h2 id="ordem-agenda">Agendamento</h2>
            <dl className="ordem-agendamento">
              {locacao && <>
                <div><dt>Início · Retirada</dt><dd>{formatarData(ordem.inicioLocacao)}</dd></div>
                <div><dt>Fim · Devolução</dt><dd>{formatarData(ordem.fimLocacao)}</dd></div>
                {dias && <div><dt>Duração</dt><dd>{dias} {dias === 1 ? 'dia' : 'dias'} de locação</dd></div>}
              </>}
              {ordem.dataProva && <div><dt>Prova</dt><dd>{formatarData(ordem.dataProva)}</dd></div>}
              {ordem.dataEvento && <div><dt>Evento</dt><dd>{formatarData(ordem.dataEvento)}</dd></div>}
              {ordem.dataEntrega && <div><dt>Entrega</dt><dd>{formatarData(ordem.dataEntrega)}</dd></div>}
            </dl>
            {(ordem.medidas || ordem.instrucoesCostura) && (
              <p className="ordem-observacao">{[ordem.medidas, ordem.instrucoesCostura].filter(Boolean).join(' · ')}</p>
            )}
          </section>
        </div>

        <div className="ordem-coluna">
          <section className="cartao" aria-labelledby="ordem-valores">
            <h2 id="ordem-valores">Valores e pagamentos</h2>
            <p className="ordem-total">{moeda.format(ordem.valorTotal)}</p>
            <dl className="ordem-valores">
              <div><dt>Subtotal · {ordem.itens.length} {ordem.itens.length === 1 ? 'peça' : 'peças'}</dt><dd>{moeda.format(subtotal)}</dd></div>
              <div><dt>Desconto</dt><dd>{moeda.format(desconto)}</dd></div>
              {Math.abs(ajuste) >= 0.01 && <div><dt>Ajuste de preço aprovado</dt><dd>{moeda.format(ajuste)}</dd></div>}
              <div><dt>Recebido</dt><dd>{moeda.format(pagamento.valorPago)}</dd></div>
              <div className="ordem-saldo"><dt>Saldo a receber</dt><dd>{moeda.format(pagamento.saldoDevedor)}</dd></div>
            </dl>
            <Etiqueta tom={tomSituacao}>{situacao}</Etiqueta>
            {ordem.pendenciaAcerto && <Aviso tipo="info">Há uma pendência de acerto registrada nesta ordem.</Aviso>}
            <div className="fila-acoes">
              <PagamentosDaOrdem ordem={ordem} rotulo="Pagamentos e recibos" aoFechar={recarregar}
                aoAtualizar={(atualizada) => setDados((anteriores) => ({ ...anteriores, ordem: atualizada }))} />
            </div>
          </section>

          <section className="cartao" aria-labelledby="ordem-acoes">
            <h2 id="ordem-acoes">Próximos passos</h2>
            <AcoesDaOrdem ordem={ordem} aoAtualizar={() => recarregar()} />
          </section>

          <section className="cartao" aria-labelledby="ordem-historico">
            <h2 id="ordem-historico">Histórico da ordem</h2>
            <ol className="ordem-historico">
              {historico.map((item) => (
                <li key={item.chave}><strong>{item.titulo}</strong><span>{item.texto}</span></li>
              ))}
            </ol>
          </section>
        </div>
      </div>
    </section>
  );
}
