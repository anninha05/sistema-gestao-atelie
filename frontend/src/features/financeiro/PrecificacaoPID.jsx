import { useEffect, useMemo, useState } from 'react';
import { Aviso, Modal, Selecao, validarCampos } from '../../shared/Interface.jsx';
import { api, requisitarComCsrf } from '../../shared/api.js';

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const percentual = (valor) => new Intl.NumberFormat('pt-BR', { style: 'percent', maximumFractionDigits: 2 }).format(valor || 0);
const camposPoliticaVazios = {
  ganhoProporcional: '', ganhoIntegral: '', ganhoDerivativo: '', ajusteMinimo: '', ajusteMaximo: '',
  descontoFidelidade: '', quantidadeOsFidelidade: '',
};

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
  const [politica, setPolitica] = useState(camposPoliticaVazios);
  const [ordens, setOrdens] = useState([]);
  const [ordemId, setOrdemId] = useState('');
  const [pisoMinimo, setPisoMinimo] = useState('');
  const [execucoes, setExecucoes] = useState([]);
  const [calculo, setCalculo] = useState(null);
  const [sugestao, setSugestao] = useState(null);
  const [confirmacaoAberta, setConfirmacaoAberta] = useState(false);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState('');
  const [operacao, setOperacao] = useState('');

  const opcoesDeOrdens = useMemo(() => ordens.map((ordem) => [ordem.id,
    `${ordem.clienteNome} · ${ordem.tipoFluxo === 'LOCACAO' ? 'Locação' : 'Sob medida'} · ${moeda.format(ordem.valorTotal)}`,
  ]), [ordens]);

  async function carregarHistorico(mesSelecionado = mes) {
    const { data } = await api.get('/precificacao/atualizacoes', { params: { mes: mesSelecionado } });
    setExecucoes(data);
  }

  useEffect(() => {
    let ativa = true;
    setCarregando(true);
    setErro('');
    Promise.all([
      api.get('/precificacao/metas/' + mes).catch((falha) => {
        if (falha.response?.status === 404) return { data: null };
        throw falha;
      }),
      api.get('/precificacao/politica').catch((falha) => {
        if (falha.response?.status === 404) return { data: null };
        throw falha;
      }),
      api.get('/precificacao/atualizacoes', { params: { mes } }),
      api.get('/ordens-servico'),
    ]).then(([respostaMeta, respostaPolitica, respostaExecucoes, respostaOrdens]) => {
      if (!ativa) return;
      setMeta(respostaMeta.data);
      setValorMeta(valorCampo(respostaMeta.data?.valorMeta));
      if (respostaPolitica.data) {
        setPolitica(Object.fromEntries(Object.keys(camposPoliticaVazios).map((campo) => [
          campo, valorCampo(respostaPolitica.data[campo]),
        ])));
      }
      setExecucoes(respostaExecucoes.data);
      setOrdens(respostaOrdens.data.filter((ordem) => ordem.statusAtual === 'RESERVADA'));
    }).catch((falha) => {
      if (ativa) setErro(mensagemDaFalha(falha, 'Não foi possível carregar as configurações e dados do módulo PID.'));
    }).finally(() => { if (ativa) setCarregando(false); });
    return () => { ativa = false; };
  }, [mes]);

  function limparResultado() {
    setCalculo(null);
    setSugestao(null);
    setSucesso('');
  }

  async function salvarMeta(evento) {
    evento.preventDefault();
    const aviso = validarCampos(evento.currentTarget);
    if (aviso) { setErro(aviso); return; }
    setOperacao('meta'); setErro(''); setSucesso('');
    try {
      const { data } = await requisitarComCsrf('put', '/precificacao/metas/' + mes, {
        valorMeta: Number(valorMeta), indicador: 'RESULTADO_DE_CAIXA',
      });
      setMeta(data);
      setSucesso(`Meta de ${moeda.format(data.valorMeta)} salva para ${mes}.`);
    } catch (falha) { setErro(mensagemDaFalha(falha, 'Não foi possível salvar a meta mensal.')); }
    finally { setOperacao(''); }
  }

  async function salvarPolitica(evento) {
    evento.preventDefault();
    const aviso = validarCampos(evento.currentTarget);
    if (aviso) { setErro(aviso); return; }
    const desconto = politica.descontoFidelidade === '' ? null : Number(politica.descontoFidelidade) / 100;
    const quantidade = politica.quantidadeOsFidelidade === '' ? null : Number(politica.quantidadeOsFidelidade);
    if ((desconto == null) !== (quantidade == null)) {
      setErro('Preencha juntos o desconto de fidelidade e a quantidade mínima de ordens concluídas, ou deixe os dois vazios.');
      return;
    }
    setOperacao('politica'); setErro(''); setSucesso('');
    try {
      const { data } = await requisitarComCsrf('put', '/precificacao/politica', {
        ganhoProporcional: Number(politica.ganhoProporcional),
        ganhoIntegral: Number(politica.ganhoIntegral),
        ganhoDerivativo: Number(politica.ganhoDerivativo),
        ajusteMinimo: Number(politica.ajusteMinimo) / 100,
        ajusteMaximo: Number(politica.ajusteMaximo) / 100,
        descontoFidelidade: desconto,
        quantidadeOsFidelidade: quantidade,
      });
      setPolitica(Object.fromEntries(Object.keys(camposPoliticaVazios).map((campo) => [campo, valorCampo(
        campo === 'ajusteMinimo' || campo === 'ajusteMaximo' ? data[campo] * 100
          : campo === 'descontoFidelidade' ? data[campo] == null ? '' : data[campo] * 100 : data[campo],
      )])));
      limparResultado();
      setSucesso('Política de precificação salva. Ela será utilizada nas próximas atualizações e sugestões.');
    } catch (falha) { setErro(mensagemDaFalha(falha, 'Não foi possível salvar a política de precificação.')); }
    finally { setOperacao(''); }
  }

  async function atualizarIndicador() {
    setOperacao('pid'); setErro(''); setSucesso('');
    try {
      const { data } = await requisitarComCsrf('post', '/precificacao/atualizacao-diaria');
      await carregarHistorico();
      setSucesso(`Atualização de ${data.dataApuracao} registrada. Ajuste PID: ${percentual(data.ajusteLimitado)}.`);
    } catch (falha) { setErro(mensagemDaFalha(falha, 'Não foi possível atualizar o indicador diário.')); }
    finally { setOperacao(''); }
  }

  async function simular(evento) {
    evento.preventDefault();
    if (!ordemId) { setErro('Selecione uma ordem reservada para simular o preço.'); return; }
    const aviso = validarCampos(evento.currentTarget);
    if (aviso) { setErro(aviso); return; }
    setOperacao('simular'); setErro(''); setSucesso(''); setSugestao(null);
    try {
      const { data } = await requisitarComCsrf('post', '/precificacao/simular', {
        ordemServicoId: ordemId, pisoMinimo: Number(pisoMinimo),
      });
      setCalculo(data);
    } catch (falha) { setErro(mensagemDaFalha(falha, 'Não foi possível simular a sugestão de preço.')); }
    finally { setOperacao(''); }
  }

  async function registrarSugestao() {
    setOperacao('sugestao'); setErro(''); setSucesso('');
    try {
      const { data } = await requisitarComCsrf('post', '/precificacao/sugestoes', {
        ordemServicoId: ordemId, pisoMinimo: Number(pisoMinimo),
      });
      setSugestao(data);
      setCalculo(data);
      setSucesso('Sugestão registrada. O preço contratado continua inalterado até uma aprovação explícita.');
    } catch (falha) { setErro(mensagemDaFalha(falha, 'Não foi possível registrar a sugestão.')); }
    finally { setOperacao(''); }
  }

  async function aprovarSugestao() {
    if (!sugestao) return;
    setOperacao('aprovar'); setErro(''); setSucesso('');
    try {
      const { data } = await requisitarComCsrf('post', '/ordens-servico/' + sugestao.ordemServicoId + '/aprovar-preco', {
        sugestaoId: sugestao.id,
      });
      setConfirmacaoAberta(false);
      setSucesso(`Preço atualizado de ${moeda.format(data.valorAnterior)} para ${moeda.format(data.valorAprovado)}. A aprovação foi registrada.`);
      setOrdens((atuais) => atuais.map((ordem) => ordem.id === sugestao.ordemServicoId
        ? { ...ordem, valorTotal: data.valorAprovado } : ordem));
      setSugestao(null);
      setCalculo(null);
      setPisoMinimo('');
    } catch (falha) { setErro(mensagemDaFalha(falha, 'Não foi possível aprovar a sugestão.')); }
    finally { setOperacao(''); }
  }

  const ultimaExecucao = execucoes[0];

  return (
    <section className="pid-modulo" aria-labelledby="titulo-pid">
      <div className="pid-cabecalho">
        <div><span className="eyebrow">Gerência</span><h2 id="titulo-pid">Metas e precificação assistida</h2>
          <p>Configure a meta financeira, acompanhe o controlador PID e avalie sugestões antes de alterar uma ordem.</p></div>
        <button type="button" className="botao-secundario" onClick={atualizarIndicador}
          disabled={carregando || operacao !== '' || !meta || !politica.ganhoProporcional}>
          {operacao === 'pid' ? 'Atualizando…' : 'Atualizar indicador do dia'}
        </button>
      </div>

      {erro && <Aviso>{erro}</Aviso>}
      {sucesso && <Aviso tipo="sucesso">{sucesso}</Aviso>}
      {carregando && <p role="status">Carregando configurações de precificação…</p>}

      {!carregando && <>
        <div className="pid-cartoes-configuracao">
          <form className="pid-cartao" onSubmit={salvarMeta} noValidate aria-busy={operacao === 'meta'}>
            <h3>Meta mensal de caixa</h3>
            <p>A meta é comparada ao resultado de caixa acumulado no mês selecionado acima.</p>
            {meta && <p className="pid-detalhe">Última configuração: {meta.responsavelNome}</p>}
            <label>Valor da meta (R$)<input type="number" min="0.01" max="99999999.99" step="0.01"
              required value={valorMeta} onChange={(evento) => setValorMeta(evento.target.value)} /></label>
            <button type="submit" disabled={operacao !== ''}>{operacao === 'meta' ? 'Salvando…' : 'Salvar meta'}</button>
          </form>

          <form className="pid-cartao" onSubmit={salvarPolitica} noValidate aria-busy={operacao === 'politica'}>
            <h3>Política do controlador</h3>
            <p>Os ganhos e limites são definidos pela gerência; o sistema não preenche valores comerciais por conta própria.</p>
            <div className="pid-campos">
              <label>Ganho proporcional (Kp)<input type="number" step="any" required value={politica.ganhoProporcional}
                onChange={(evento) => setPolitica({ ...politica, ganhoProporcional: evento.target.value })} /></label>
              <label>Ganho integral (Ki)<input type="number" step="any" required value={politica.ganhoIntegral}
                onChange={(evento) => setPolitica({ ...politica, ganhoIntegral: evento.target.value })} /></label>
              <label>Ganho derivativo (Kd)<input type="number" step="any" required value={politica.ganhoDerivativo}
                onChange={(evento) => setPolitica({ ...politica, ganhoDerivativo: evento.target.value })} /></label>
              <label>Ajuste mínimo (%)<input type="number" step="any" required value={politica.ajusteMinimo}
                onChange={(evento) => setPolitica({ ...politica, ajusteMinimo: evento.target.value })} /></label>
              <label>Ajuste máximo (%)<input type="number" step="any" required value={politica.ajusteMaximo}
                onChange={(evento) => setPolitica({ ...politica, ajusteMaximo: evento.target.value })} /></label>
              <label>Desconto de fidelidade (%)<input type="number" min="0" max="99.99" step="0.01"
                value={politica.descontoFidelidade} onChange={(evento) => setPolitica({ ...politica, descontoFidelidade: evento.target.value })} /></label>
              <label>Ordens concluídas para fidelidade<input type="number" min="1" step="1"
                value={politica.quantidadeOsFidelidade} onChange={(evento) => setPolitica({ ...politica, quantidadeOsFidelidade: evento.target.value })} /></label>
            </div>
            <button type="submit" disabled={operacao !== ''}>{operacao === 'politica' ? 'Salvando…' : 'Salvar política'}</button>
          </form>
        </div>

        <div className="pid-cartao pid-historico">
          <div className="pid-subcabecalho"><div><h3>Execução diária</h3>
            <p>Uma atualização por dia para a competência {mes}. Repetir a ação no mesmo dia não duplica o cálculo.</p></div>
            <span className="pid-contador">{execucoes.length} {execucoes.length === 1 ? 'registro' : 'registros'}</span>
          </div>
          {!ultimaExecucao ? <p className="pid-vazio">Nenhuma atualização registrada para este mês.</p> : (
            <div className="pid-indicadores">
              <div><span>Apuração</span><strong>{dataLocal(ultimaExecucao.dataApuracao)}</strong></div>
              <div><span>Resultado realizado</span><strong>{moeda.format(ultimaExecucao.resultadoRealizado)}</strong></div>
              <div><span>Meta acumulada</span><strong>{moeda.format(ultimaExecucao.referenciaAcumulada)}</strong></div>
              <div><span>Erro normalizado</span><strong>{percentual(ultimaExecucao.erroNormalizado)}</strong></div>
              <div><span>Ajuste limitado</span><strong>{percentual(ultimaExecucao.ajusteLimitado)}</strong></div>
              <div><span>Responsável</span><strong>{ultimaExecucao.responsavelNome}</strong></div>
            </div>
          )}
          <p className="pid-observacao">Ajuste PID é uma recomendação matemática baseada no resultado de caixa; não prevê demanda nem garante aumento de lucro.</p>
        </div>

        <div className="pid-cartao pid-simulacao">
          <h3>Simular preço para uma ordem reservada</h3>
          <p>A simulação não altera a ordem nem cria recebimentos. O piso deve ser informado pela gerência com base nos custos e na margem definida pelo ateliê.</p>
          {!opcoesDeOrdens.length && <Aviso tipo="info">Não há ordens reservadas disponíveis para simulação.</Aviso>}
          <form className="pid-form-simulacao" onSubmit={simular} noValidate>
            <Selecao rotulo="Ordem de serviço" valor={ordemId} aoSelecionar={(novoId) => {
              setOrdemId(novoId); limparResultado();
            }} opcoes={opcoesDeOrdens} textoInicial="Pesquise uma ordem reservada" pesquisar />
            <label>Piso mínimo informado pela gerência (R$)<input type="number" min="0.01" max="99999999.99" step="0.01"
              required value={pisoMinimo} onChange={(evento) => { setPisoMinimo(evento.target.value); limparResultado(); }} /></label>
            <button type="submit" disabled={!ordens.length || operacao !== ''}>{operacao === 'simular' ? 'Calculando…' : 'Simular preço'}</button>
          </form>
          {calculo && <div className="pid-resultado" aria-live="polite">
            <div className="pid-resultado-topo"><div><span>Preço sugerido</span><strong>{moeda.format(calculo.precoSugerido)}</strong></div>
              <span className={sugestao ? 'pid-etiqueta registrada' : 'pid-etiqueta'}>{sugestao ? 'Registrada para aprovação' : 'Simulação'}</span></div>
            <dl className="pid-memoria">
              <div><dt>Preço-base atual</dt><dd>{moeda.format(calculo.precoBase)}</dd></div>
              <div><dt>Ajuste PID</dt><dd>{percentual(calculo.ajustePid)}</dd></div>
              <div><dt>Após ajuste</dt><dd>{moeda.format(calculo.precoAjustado)}</dd></div>
              <div><dt>Fidelidade</dt><dd>{calculo.elegivelFidelidade ? percentual(calculo.descontoFidelidade) : 'Não aplicado'}</dd></div>
              <div><dt>Após fidelidade</dt><dd>{moeda.format(calculo.precoAposFidelidade)}</dd></div>
              <div><dt>Piso informado</dt><dd>{moeda.format(calculo.pisoMinimo)}</dd></div>
              <div><dt>Histórico cancelado</dt><dd>{calculo.ordensCanceladas} ordem(ns)</dd></div>
            </dl>
            <p>{calculo.justificativa}</p>
            {sugestao ? <button type="button" onClick={() => setConfirmacaoAberta(true)} disabled={operacao !== ''}>
              {operacao === 'aprovar' ? 'Aplicando…' : 'Aprovar e aplicar novo preço'}
            </button> : <button type="button" className="botao-secundario" onClick={registrarSugestao} disabled={operacao !== ''}>
              {operacao === 'sugestao' ? 'Registrando…' : 'Registrar sugestão'}
            </button>}
          </div>}
        </div>
      </>}

      {confirmacaoAberta && sugestao && <Modal titulo="Aprovar novo preço" aoFechar={() => setConfirmacaoAberta(false)} ocupado={operacao === 'aprovar'}>
        <p>O valor total da ordem será alterado de <strong>{moeda.format(sugestao.precoBase)}</strong> para <strong>{moeda.format(sugestao.precoSugerido)}</strong>.</p>
        <p>O histórico de recebimentos será preservado e o saldo será recalculado. Esta ação será registrada em auditoria.</p>
        <div className="modal-acoes">
          <button type="button" className="botao-secundario" onClick={() => setConfirmacaoAberta(false)} disabled={operacao === 'aprovar'}>Voltar</button>
          <button type="button" onClick={aprovarSugestao} disabled={operacao === 'aprovar'}>
            {operacao === 'aprovar' ? 'Aprovando…' : 'Confirmar aprovação'}
          </button>
        </div>
      </Modal>}
    </section>
  );
}
