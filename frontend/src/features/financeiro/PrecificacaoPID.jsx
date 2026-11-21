import { useEffect, useMemo, useState } from 'react';
import { Aviso, Modal, Selecao, validarCampos } from '../../shared/Interface.jsx';
import { api, requisitarComCsrf } from '../../shared/api.js';

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const percentual = (valor) => new Intl.NumberFormat('pt-BR', { style: 'percent', maximumFractionDigits: 2 }).format(valor || 0);
function mensagemDaFalha(falha, fallback) {
  return falha.response?.data?.mensagem || fallback;
}

function dataLocal(valor) {
  return valor ? new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeZone: 'UTC' }).format(new Date(valor + 'T12:00:00Z')) : '—';
}

function valorCampo(valor) {
  return valor == null ? '' : String(valor);
}

export default function PrecificacaoPID({ mes }) {
  const [meta, setMeta] = useState(null);
  const [valorMeta, setValorMeta] = useState('');
  const [indicadores, setIndicadores] = useState(null);
  const [ordens, setOrdens] = useState([]);
  const [ordemId, setOrdemId] = useState('');
  const [pisoMinimo, setPisoMinimo] = useState('');
  const [execucoes, setExecucoes] = useState([]);
  const [calculo, setCalculo] = useState(null);
  const [sugestao, setSugestao] = useState(null);
  const [confirmacaoAberta, setConfirmacaoAberta] = useState(false);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [erroMeta, setErroMeta] = useState('');
  const [sucesso, setSucesso] = useState('');
  const [operacao, setOperacao] = useState('');
  const [calibracao, setCalibracao] = useState(null);
  const [erroCalibracao, setErroCalibracao] = useState('');

  const opcoesDeOrdens = useMemo(() => ordens.map((ordem) => [ordem.id,
    `${ordem.clienteNome} · ${ordem.tipoFluxo === 'LOCACAO' ? 'Locação' : 'Sob medida'} · ${moeda.format(ordem.valorTotal)}`,
  ]), [ordens]);

  useEffect(() => {
    let ativa = true;
    setCarregando(true);
    setErro('');
    limparResultado();
    setConfirmacaoAberta(false);
    Promise.all([
      api.get('/precificacao/metas/' + mes).catch((falha) => {
        if (falha.response?.status === 404) return { data: null };
        throw falha;
      }),
      api.get('/precificacao/atualizacoes', { params: { mes } }),
      api.get('/ordens-servico'),
      api.get('/financeiro/indicadores', { params: { mes } }),
    ]).then(([respostaMeta, respostaExecucoes, respostaOrdens, respostaIndicadores]) => {
      if (!ativa) return;
      setMeta(respostaMeta.data);
      setValorMeta(valorCampo(respostaMeta.data?.valorMeta));
      setIndicadores(respostaIndicadores.data);
      setExecucoes(respostaExecucoes.data);
      setOrdens(respostaOrdens.data.filter((ordem) => ordem.statusAtual === 'RESERVADA'));
    }).catch((falha) => {
      if (ativa) setErro(mensagemDaFalha(falha, 'Não foi possível carregar as configurações e dados do módulo PID.'));
    }).finally(() => { if (ativa) setCarregando(false); });
    return () => { ativa = false; };
  }, [mes]);

  useEffect(() => {
    setPisoMinimo('');
  }, [ordemId]);

  function limparResultado() {
    setCalculo(null);
    setSugestao(null);
    setSucesso('');
  }

  async function salvarMeta(evento) {
    evento.preventDefault();
    const aviso = validarCampos(evento.currentTarget);
    if (aviso) { setErroMeta(aviso); return; }
    setOperacao('meta'); setErro(''); setErroMeta(''); setSucesso('');
    try {
      const { data } = await requisitarComCsrf('put', '/precificacao/metas/' + mes, {
        valorMeta: Number(valorMeta),
      });
      setMeta(data);
      setSucesso(`Meta de ${moeda.format(data.valorMeta)} salva para ${mes}.`);
    } catch (falha) { setErroMeta(mensagemDaFalha(falha, 'Não foi possível salvar a meta mensal.')); }
    finally { setOperacao(''); }
  }

  async function simular(evento) {
    evento.preventDefault();
    if (!ordemId) { setErro('Selecione uma ordem reservada para simular o preço.'); return; }
    const aviso = validarCampos(evento.currentTarget);
    if (aviso) { setErro(aviso); return; }
    setOperacao('simular'); setErro(''); setSucesso(''); setSugestao(null); setCalculo(null);
    try {
      const { data } = await requisitarComCsrf('post', '/precificacao/sugestoes', {
        ordemServicoId: ordemId, pisoMinimo: Number(pisoMinimo),
      });
      setCalculo(data);
      setSugestao(data);
      api.get('/precificacao/atualizacoes', { params: { mes } }).then(({ data: historico }) => setExecucoes(historico)).catch(() => {});
    } catch (falha) { setErro(mensagemDaFalha(falha, 'Não foi possível simular a sugestão de preço.')); }
    finally { setOperacao(''); }
  }

  async function aprovarSugestao() {
    if (!sugestao) return;
    setOperacao('aprovar'); setErro(''); setSucesso('');
    try {
      const { data } = await requisitarComCsrf('post', '/precificacao/sugestoes/' + sugestao.id + '/aprovar');
      setConfirmacaoAberta(false);
      setSucesso(`Preço atualizado de ${moeda.format(data.precoBase)} para ${moeda.format(data.precoSugerido)}. A aprovação foi registrada.`);
      setOrdens((atuais) => atuais.map((ordem) => ordem.id === data.ordemServicoId
        ? { ...ordem, valorTotal: data.precoSugerido } : ordem));
      setSugestao(null);
      setCalculo(null);

    } catch (falha) { setErro(mensagemDaFalha(falha, 'Não foi possível aprovar a sugestão.')); }
    finally { setOperacao(''); }
  }

  async function calibrar() {
    setOperacao('calibrar');
    setErroCalibracao('');
    try {
      const { data } = await requisitarComCsrf('post', '/precificacao/politica/calibrar');
      setCalibracao(data);
    } catch (falha) {
      setErroCalibracao(mensagemDaFalha(falha, 'Não foi possível calibrar os ganhos. Tente novamente.'));
    } finally { setOperacao(''); }
  }

  const ultimaExecucao = execucoes[0];
  const ordemSelecionada = ordens.find((ordem) => ordem.id === ordemId);

  return (
    <section className="pid-modulo" aria-labelledby="titulo-pid">
      <div className="pid-cabecalho">
        <div><span className="eyebrow">Gerência</span><h2 id="titulo-pid">Meta e sugestão de preço</h2>
          <p>Defina o resultado que deseja alcançar e avalie sugestões de preço para suas contratações.</p></div>
      </div>

      {erro && <Aviso>{erro}</Aviso>}
      {sucesso && <Aviso tipo="sucesso">{sucesso}</Aviso>}
      {carregando && <p role="status">Carregando configurações de precificação…</p>}

      {!carregando && <>
        <div className="pid-cartoes-configuracao">
          <form className="pid-cartao" onSubmit={salvarMeta} noValidate aria-busy={operacao === 'meta'}>
            <h3>1. Defina a meta do mês</h3>
            {erroMeta && <Aviso>{erroMeta}</Aviso>}
            <p>A meta é comparada ao resultado de caixa acumulado no mês selecionado acima.</p>
            {meta && <p className="pid-detalhe">Última configuração: {meta.responsavelNome}</p>}
            <label>Valor da meta (R$)<input type="number" min="0.01" max="99999999.99" step="0.01"
              required value={valorMeta} onChange={(evento) => { setValorMeta(evento.target.value); setErroMeta(''); }} /></label>
            <button type="submit" disabled={operacao !== ''}>{operacao === 'meta' ? 'Salvando…' : 'Salvar meta'}</button>
          </form>

          <div className="pid-cartao">
            <h3>Como está sua meta?</h3>
            {meta && indicadores ? <>
              <p>Você definiu {moeda.format(meta.valorMeta)} para este mês.</p>
              <dl className="pid-memoria">
                <div><dt>Resultado de caixa até agora</dt><dd>{moeda.format(indicadores.resultadoDeCaixa)}</dd></div>
                <div><dt>Falta para alcançar a meta</dt><dd>{moeda.format(Math.max(0, meta.valorMeta - indicadores.resultadoDeCaixa))}</dd></div>
              </dl>
              <progress className="pid-progresso" aria-label="Progresso da meta mensal"
                max="100" value={Math.max(0, Math.min(100, indicadores.resultadoDeCaixa / meta.valorMeta * 100))} />
              <p>{indicadores.resultadoDeCaixa >= meta.valorMeta ? 'Meta alcançada!' : 'Acompanhe seus recebimentos e despesas para observar a evolução.'}</p>
            </> : <p>Salve uma meta para acompanhar seu progresso neste mês.</p>}
          </div>
        </div>

        <details className="pid-cartao pid-historico">
          <summary>Consultar acompanhamento diário</summary>
          <div className="pid-subcabecalho"><div><h3>Recomendações de preço</h3>
            <p>O sistema atualiza as recomendações diariamente, quando há meta e configuração técnica disponíveis.</p></div>
            <span className="pid-contador">{execucoes.length} {execucoes.length === 1 ? 'registro' : 'registros'}</span>
          </div>
          {!ultimaExecucao ? <p className="pid-vazio">Ainda não há recomendação calculada para este mês.</p> : (
            <div className="pid-indicadores">
              <div><span>Apuração</span><strong>{dataLocal(ultimaExecucao.dataApuracao)}</strong></div>
              <div><span>Resultado realizado</span><strong>{moeda.format(ultimaExecucao.resultadoRealizado)}</strong></div>
              <div><span>Resultado esperado até a apuração</span><strong>{moeda.format(ultimaExecucao.referenciaAcumulada)}</strong></div>

              <div><span>Ajuste recomendado</span><strong>{percentual(ultimaExecucao.ajusteLimitado)}</strong></div>
              <div><span>Responsável</span><strong>{ultimaExecucao.responsavelNome}</strong></div>
            </div>
          )}
          <p className="pid-observacao">As sugestões consideram o resultado de caixa e o histórico do cliente. Você decide se deseja aplicar o preço.</p>
        </details>

        <details className="pid-cartao pid-historico">
          <summary>Aprendizado do agente</summary>
          <p>O agente testa combinações de ganhos em meses simulados do ateliê e mantém a que acompanha a meta com menos erro e menos variação de preço. Os limites de ajuste não mudam.</p>
          {erroCalibracao && <Aviso>{erroCalibracao}</Aviso>}
          <button type="button" onClick={calibrar} disabled={operacao !== ''}>
            {operacao === 'calibrar' ? 'Calibrando…' : 'Calibrar por simulação'}
          </button>
          {calibracao && (
            <div className="pid-indicadores">
              <div><span>Ganhos anteriores (Kp · Ki · Kd)</span><strong>{[calibracao.kpAnterior, calibracao.kiAnterior, calibracao.kdAnterior].map((g) => g.toFixed(3)).join(' · ')}</strong></div>
              <div><span>Ganhos calibrados (Kp · Ki · Kd)</span><strong>{[calibracao.kp, calibracao.ki, calibracao.kd].map((g) => g.toFixed(3)).join(' · ')}</strong></div>
              <div><span>Custo nos meses simulados</span><strong>{calibracao.custoAnterior.toFixed(4)} → {calibracao.custo.toFixed(4)}</strong></div>
              <div><span>Redução do custo</span><strong>{percentual(1 - calibracao.custo / calibracao.custoAnterior)}</strong></div>
              <div><span>Iterações da busca</span><strong>{calibracao.iteracoes}</strong></div>
            </div>
          )}
        </details>

        <div className="pid-cartao pid-simulacao">
          <h3>2. Escolha a OS e consulte o preço sugerido</h3>
          <p>Escolha uma OS, confira a sugestão e confirme apenas se quiser aplicar o novo preço.</p>
          {!opcoesDeOrdens.length && <Aviso tipo="info">Não há ordens reservadas disponíveis para simulação.</Aviso>}
          <form onSubmit={simular} noValidate>
          <fieldset className="pid-form-simulacao" disabled={operacao !== ''}>
            <Selecao rotulo="Ordem de serviço" valor={ordemId} aoSelecionar={(novoId) => {
              setOrdemId(novoId); limparResultado();
            }} opcoes={opcoesDeOrdens} textoInicial="Pesquise uma ordem reservada" pesquisar="Pesquisar por cliente" />
            <label>Preço mínimo desta OS (R$)<input type="number" min="0.01" max="99999999.99" step="0.01" required
              value={pisoMinimo} onChange={(evento) => { setPisoMinimo(evento.target.value); limparResultado(); }} /></label>
            <button type="submit" disabled={!ordemId || !meta || operacao !== ''}>{operacao === 'simular' ? 'Calculando…' : 'Consultar sugestão'}</button>
          </fieldset></form>
          <p className="pid-observacao">O preço mínimo é o menor valor que cobre seus custos e a margem desejada; a sugestão nunca fica abaixo dele.</p>
          {!meta && <Aviso tipo="info">Salve a meta do mês antes de consultar uma sugestão.</Aviso>}
          {ordemSelecionada && !calculo && <p>Preço atual de <strong>{ordemSelecionada.clienteNome}</strong>: <strong>{moeda.format(ordemSelecionada.valorTotal)}</strong>.</p>}
          {calculo && <div className="pid-resultado" aria-live="polite">
            <div className="pid-resultado-topo"><div><span>Preço sugerido</span><strong>{moeda.format(calculo.precoSugerido)}</strong></div>
              <span className={sugestao ? 'pid-etiqueta registrada' : 'pid-etiqueta'}>{sugestao ? 'Aguardando sua decisão' : 'Simulação'}</span></div>
            <p>Preço atual: <strong>{moeda.format(calculo.precoBase)}</strong>. O preço sugerido só será aplicado depois da sua confirmação.</p>
            <details><summary>Por que este preço?</summary>
            <dl className="pid-memoria">
              <div><dt>Preço-base atual</dt><dd>{moeda.format(calculo.precoBase)}</dd></div>
              <div><dt>Ajuste recomendado</dt><dd>{percentual(calculo.ajustePid)}</dd></div>
              <div><dt>Após ajuste</dt><dd>{moeda.format(calculo.precoAjustado)}</dd></div>
              <div><dt>Fidelidade</dt><dd>{calculo.elegivelFidelidade ? percentual(calculo.descontoFidelidade) : 'Não aplicado'}</dd></div>
              <div><dt>Após fidelidade</dt><dd>{moeda.format(calculo.precoAposFidelidade)}</dd></div>
              <div><dt>Piso informado</dt><dd>{moeda.format(calculo.pisoMinimo)}</dd></div>
              <div><dt>Histórico cancelado</dt><dd>{calculo.ordensCanceladas} ordem(ns)</dd></div>
            </dl>
            <p>{calculo.justificativa}</p></details>
            <div className="modal-acoes">
              <button type="button" className="botao-secundario" onClick={limparResultado} disabled={operacao !== ''}>Manter preço atual</button>
              <button type="button" onClick={() => setConfirmacaoAberta(true)} disabled={!sugestao || operacao !== ''}>
                {operacao === 'aprovar' ? 'Aplicando…' : 'Usar este preço'}
              </button>
            </div>
          </div>}
        </div>
      </>}

      {confirmacaoAberta && sugestao && <Modal titulo="Confirmar preço da OS" aoFechar={() => setConfirmacaoAberta(false)} ocupado={operacao === 'aprovar'}>
        {erro && <Aviso>{erro}</Aviso>}
        <p>O valor total da ordem será alterado de <strong>{moeda.format(sugestao.precoBase)}</strong> para <strong>{moeda.format(sugestao.precoSugerido)}</strong>.</p>
        <p>O histórico de recebimentos será preservado e o saldo será recalculado. Esta ação será registrada em auditoria.</p>
        <div className="modal-acoes">
          <button type="button" className="botao-secundario" onClick={() => setConfirmacaoAberta(false)} disabled={operacao === 'aprovar'}>Voltar</button>
          <button type="button" onClick={aprovarSugestao} disabled={operacao === 'aprovar'}>
            {operacao === 'aprovar' ? 'Aprovando…' : 'Confirmar preço'}
          </button>
        </div>
      </Modal>}
    </section>
  );
}
