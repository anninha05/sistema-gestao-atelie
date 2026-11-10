import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { api } from '../../shared/api.js';
import Catalogo from '../../shared/Catalogo.jsx';
import { Aviso, Selecao } from '../../shared/Interface.jsx';
import AcoesDaOrdem from './AcoesDaOrdem.jsx';
import PagamentosDaOrdem from './PagamentosDaOrdem.jsx';

const tipos = { LOCACAO: 'Locação', SOB_MEDIDA: 'Sob medida' };
const estados = {
  PENDENTE: 'Pendente', RESERVADA: 'Reservada', CONFECCAO: 'Confecção',
  PROVA: 'Prova', EM_PROVA: 'Em prova', AJUSTES: 'Ajustes', PASSADORIA: 'Passadoria',
  CONCLUIDO: 'Concluída', RETIRADA: 'Retirada', ENTREGUE: 'Entregue',
  CANCELADA: 'Cancelada', DEVOLVIDA: 'Devolvida',
};
const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

function formatarData(valor) {
  return valor ? dataHora.format(new Date(valor)) : null;
}

function validarOrdem(formulario) {
  if (formulario.getAll('pecas').length === 0) return 'Selecione pelo menos uma peça.';
  const percentual = Number(formulario.get('descontoPercentual'));
  if (!Number.isFinite(percentual) || percentual < 0 || percentual >= 100) {
    return 'Informe um desconto de 0% a 99,99%.';
  }
  if (formulario.get('tipoFluxo') === 'LOCACAO' && formulario.get('fimLocacao') <= formulario.get('inicioLocacao')) {
    return 'O fim da locação precisa ser posterior ao início.';
  }
  return null;
}

function CamposDaOrdem({ clientes, pecas }) {
  const [tipo, setTipo] = useState('LOCACAO');
  const [disponibilidade, setDisponibilidade] = useState('');
  const [consultando, setConsultando] = useState(false);
  const pecasDoTipo = pecas.filter((peca) => peca.tipo === tipo);

  async function consultarDisponibilidade(evento) {
    const elementoFormulario = evento.currentTarget.form;
    const formulario = new FormData(elementoFormulario);
    const inicio = formulario.get('inicioLocacao');
    const fim = formulario.get('fimLocacao');
    const selecionadas = formulario.getAll('pecas');
    if (!selecionadas.length) {
      setDisponibilidade('Selecione pelo menos uma peça para consultar.');
      return;
    }
    if (!inicio || !fim) {
      setDisponibilidade('Preencha a data e o horário de início e fim da locação para consultar as peças selecionadas.');
      elementoFormulario.elements.namedItem(!inicio ? 'inicioLocacao' : 'fimLocacao').focus();
      return;
    }
    if (fim <= inicio) {
      setDisponibilidade('O fim da locação precisa ser posterior ao início.');
      return;
    }
    setConsultando(true);
    setDisponibilidade('');
    try {
      const resultados = await Promise.all(selecionadas.map(async (pecaId) => {
        const { data } = await api.get('/ordens-servico/disponibilidade', { params: { pecaId, inicio, fim } });
        return { pecaId, disponivel: data.disponivel };
      }));
      const atuais = new FormData(elementoFormulario);
      if (!elementoFormulario.isConnected || atuais.get('tipoFluxo') !== 'LOCACAO'
          || atuais.get('inicioLocacao') !== inicio || atuais.get('fimLocacao') !== fim
          || atuais.getAll('pecas').join(',') !== selecionadas.join(',')) return;
      const indisponiveis = resultados.filter((item) => !item.disponivel);
      setDisponibilidade(indisponiveis.length
        ? 'Sem disponibilidade no período: ' + indisponiveis.map((item) => pecas.find((peca) => peca.id === item.pecaId)?.descricao).join(', ')
        : 'Peças disponíveis nesta consulta. A reserva será confirmada ao salvar a ordem.');
    } catch {
      setDisponibilidade('Não foi possível consultar a disponibilidade. Tente novamente.');
    } finally { setConsultando(false); }
  }

  return (
    <>
      <Selecao rotulo="Cliente" nome="clienteId" obrigatorio textoInicial="Selecione um cliente"
        pesquisar="Buscar por nome ou CPF"
        opcoes={clientes.map((cliente) => [cliente.id, cliente.nome + ' · ' + cliente.cpf])} />
      <Selecao rotulo="Tipo" nome="tipoFluxo" valor={tipo} aoSelecionar={(valor) => {
        setTipo(valor); setDisponibilidade('');
      }} opcoes={Object.entries(tipos)} />
      <div className="ordem-selecao-pecas">
        <Selecao key={tipo} rotulo="Peças da ordem" nome="pecas" multipla
          aoSelecionar={() => setDisponibilidade('')}
          pesquisar="Buscar por descrição ou etiqueta" textoInicial="Selecione as peças"
          opcoes={pecasDoTipo.map((peca) => [
            peca.id,
            peca.descricao + ' · ' + moeda.format(peca.valorBase ?? 0) + ' · ' + (peca.codigoEtiqueta ?? ''),
          ])} />
      </div>
      {tipo === 'LOCACAO' && (
        <>
          <label>Início da locação<input name="inicioLocacao" type="datetime-local" required onChange={() => setDisponibilidade('')} /></label>
          <label>Fim da locação<input name="fimLocacao" type="datetime-local" required onChange={() => setDisponibilidade('')} /></label>
          <div className="ordem-selecao-pecas">
            <p className="catalogo-total">Selecione as peças e preencha as duas datas com horário para consultar o período.</p>
            <button type="button" className="botao-secundario" disabled={consultando} onClick={consultarDisponibilidade}>
              {consultando ? 'Consultando…' : 'Consultar disponibilidade'}
            </button>
            {disponibilidade && <Aviso tipo="info">{disponibilidade}</Aviso>}
          </div>
        </>
      )}
      <label>Data da prova<input name="dataProva" type="datetime-local" /></label>
      <label>Data do evento<input name="dataEvento" type="datetime-local" /></label>
      <label>Data de entrega<input name="dataEntrega" type="datetime-local" /></label>
      <label>Desconto (%)<input name="descontoPercentual" type="number" min="0" max="99.99"
        step="0.01" inputMode="decimal" placeholder="0" /></label>
      <label>Medidas<textarea name="medidas" rows={3} /></label>
      <label>Instruções de costura<textarea name="instrucoesCostura" rows={3} /></label>
    </>
  );
}

export default function OrdensServicoPage({ titulo }) {
  const { state: estadoDaRota } = useLocation();
  const [opcoes, setOpcoes] = useState(null);
  const [erroAoCarregarOpcoes, setErroAoCarregarOpcoes] = useState(false);
  const [tentativa, setTentativa] = useState(0);

  useEffect(() => {
    const controlador = new AbortController();
    const configuracao = { signal: controlador.signal };
    Promise.all([api.get('/clientes', configuracao), api.get('/pecas', configuracao)])
      .then(([clientes, pecas]) => setOpcoes({ clientes: clientes.data, pecas: pecas.data }))
      .catch(() => {
        if (!controlador.signal.aborted) setErroAoCarregarOpcoes(true);
      });

    return () => controlador.abort();
  }, [tentativa]);

  const podeCadastrar = Boolean(opcoes?.clientes.length && opcoes?.pecas.length);
  const nomesDasPecas = new Map(opcoes?.pecas.map((peca) => [peca.id, peca.descricao]));

  return (
    <>
      {erroAoCarregarOpcoes && (
        <div>
          <Aviso>Não foi possível carregar os clientes e as peças para o cadastro.</Aviso>
          <button type="button" onClick={() => {
            setErroAoCarregarOpcoes(false);
            setTentativa((anterior) => anterior + 1);
          }}>Tentar novamente</button>
        </div>
      )}
      {opcoes && !podeCadastrar && <Aviso tipo="info">Cadastre pelo menos um cliente e uma peça para abrir uma ordem.</Aviso>}
      <Catalogo titulo={titulo} descricao="Abra ordens de locação ou sob medida e acompanhe seus registros."
        caminho="/ordens-servico" rotuloCadastro="Nova ordem de serviço" rotuloBusca="Buscar por cliente ou estado"
        cadastroDesabilitado={!podeCadastrar} abrirAoEntrar={estadoDaRota?.novaOrdem === true}
        correspondeABusca={(ordem, busca) => [ordem.clienteNome, estados[ordem.statusAtual] ?? ordem.statusAtual]
          .some((valor) => valor.toLocaleLowerCase('pt-BR').includes(busca.toLocaleLowerCase('pt-BR')))}
        campos={opcoes && <CamposDaOrdem {...opcoes} />}
        validar={validarOrdem}
        montarDados={(formulario) => ({
          clienteId: formulario.get('clienteId'),
          tipoFluxo: formulario.get('tipoFluxo'),
          pecas: formulario.getAll('pecas'),
          descontoPercentual: Number(formulario.get('descontoPercentual')),
          inicioLocacao: formulario.get('inicioLocacao') || null,
          fimLocacao: formulario.get('fimLocacao') || null,
          dataProva: formulario.get('dataProva') || null,
          dataEvento: formulario.get('dataEvento') || null,
          dataEntrega: formulario.get('dataEntrega') || null,
          medidas: formulario.get('medidas').trim() || null,
          instrucoesCostura: formulario.get('instrucoesCostura').trim() || null,
        })}
        tituloDoItem={(ordem) => ordem.clienteNome}
        acoesDoItem={(ordem, aoAtualizar) => <>
          <AcoesDaOrdem ordem={ordem} aoAtualizar={aoAtualizar} />
          <div className="fila-acoes"><PagamentosDaOrdem ordem={ordem} /></div>
        </>}
        detalhesDoItem={(ordem) => [
          ['Tipo', tipos[ordem.tipoFluxo] ?? ordem.tipoFluxo],
          ['Estado', estados[ordem.statusAtual] ?? ordem.statusAtual],
          ['Peças', ordem.itens?.map((registro) => nomesDasPecas.get(registro.pecaId) ?? registro.pecaId).join(', ') || '—'],
          ['Total', moeda.format(ordem.valorTotal)],
          ...(ordem.inicioLocacao ? [['Início da locação', formatarData(ordem.inicioLocacao)],
            ['Fim da locação', formatarData(ordem.fimLocacao)]] : []),
          ['Prova', formatarData(ordem.dataProva)],
          ['Evento', formatarData(ordem.dataEvento)],
          ['Entrega', formatarData(ordem.dataEntrega)],
          ['Medidas', ordem.medidas],
          ['Instruções de costura', ordem.instrucoesCostura],
        ]}
      />
    </>
  );
}
