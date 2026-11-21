import { Aviso, CabecalhoPagina, Etiqueta, Icone } from '../../shared/Interface.jsx';
import { estadoDaOrdem } from '../../shared/formatos.js';
import { useContext, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ContextoDeAutenticacao } from '../auth/AuthContext.jsx';
import { api } from '../../shared/api.js';
import './dashboard.css';

const etapas = [
  { rotulo: 'Aguardando início', cor: '#b7791f', estados: ['PENDENTE', 'RESERVADA'] },
  { rotulo: 'Em produção ou ajustes', cor: '#4a6f8f', estados: ['CONFECCAO', 'PROVA', 'EM_PROVA', 'AJUSTES', 'PASSADORIA'] },
  { rotulo: 'Prontas para entrega ou retirada', cor: '#4f7a5c', estados: ['CONCLUIDO'] },
  { rotulo: 'Em locação', cor: '#8a5a8f', estados: ['RETIRADA'] },
];
const formatoHora = new Intl.DateTimeFormat('pt-BR', { timeStyle: 'short' });

function saudacao() {
  const hora = new Date().getHours();
  return hora < 12 ? 'Bom dia' : hora < 18 ? 'Boa tarde' : 'Boa noite';
}

function chaveDaData(dataIso) {
  return [
    dataIso.getFullYear(),
    String(dataIso.getMonth() + 1).padStart(2, '0'),
    String(dataIso.getDate()).padStart(2, '0'),
  ].join('-');
}

export default function DashboardPage({ titulo }) {
  const { usuario } = useContext(ContextoDeAutenticacao);
  const ehRecepcao = usuario.perfil === 'ROLE_ATENDENTE';
  const [painel, setPainel] = useState(null);
  const [erro, setErro] = useState(false);
  const [tentativa, setTentativa] = useState(0);
  const [dataSelecionada, setDataSelecionada] = useState(() => chaveDaData(new Date()));

  useEffect(() => {
    const controlador = new AbortController();
    const configuracao = { signal: controlador.signal };
    Promise.all([
      api.get('/ordens-servico', configuracao),
      api.get('/clientes', configuracao),
      api.get('/pecas', configuracao),
      api.get('/dashboard/compromissos', { ...configuracao, params: { data: dataSelecionada } }),
    ]).then(([ordens, clientes, pecas, compromissos]) => setPainel({
      ordens: ordens.data,
      clientes: clientes.data,
      pecas: pecas.data,
      compromissos: compromissos.data,
    })).catch(() => {
      if (!controlador.signal.aborted) setErro(true);
    });

    return () => controlador.abort();
  }, [tentativa, dataSelecionada]);

  const ordensAtivas = (painel?.ordens ?? []).filter((ordem) =>
    etapas.some((etapa) => etapa.estados.includes(ordem.statusAtual)));
  const agenda = painel?.compromissos ?? [];
  const pecasDeLocacao = (painel?.pecas ?? []).filter((peca) => peca.tipo === 'LOCACAO');
  const pecasDisponiveis = pecasDeLocacao.filter((peca) => peca.status === 'DISPONIVEL').length;
  const locacoes = ordensAtivas.filter((ordem) => ordem.tipoFluxo === 'LOCACAO').length;

  const indicadores = ehRecepcao ? [
    ['Compromissos do dia', agenda.length, 'Provas, retiradas, entregas e devoluções', 'agenda'],
    ['Retiradas do dia', agenda.filter((item) => item.tipoCompromisso === 'Retirada').length, 'Locações com retirada prevista', 'pecas'],
    ['Devoluções previstas', agenda.filter((item) => item.tipoCompromisso === 'Devolução').length, 'Locações com devolução prevista', 'prazo'],
  ] : [
    ['Ordens de serviço ativas', ordensAtivas.length, locacoes + ' de locação · ' + (ordensAtivas.length - locacoes) + ' sob medida', 'ordens'],
    ['Clientes cadastrados', painel?.clientes.length, 'Total de clientes do ateliê', 'clientes'],
    ['Peças disponíveis', pecasDisponiveis, 'De ' + pecasDeLocacao.length + ' peças no inventário de locação', 'pecas'],
  ];

  return (
    <section className="dashboard" aria-labelledby="dashboard-title">
      <CabecalhoPagina id="dashboard-title" titulo={saudacao() + ', ' + usuario.nome.trim().split(/\s+/)[0]}
        descricao="Seu ateliê em dia. Veja o que precisa da sua atenção hoje.">
        <Link className="botao-link" to="/ordens-servico" state={{ novaOrdem: true }}>
          <Icone nome="adicionar" /> Nova ordem de serviço
        </Link>
      </CabecalhoPagina>

      {!painel && !erro && <p role="status">Carregando dashboard…</p>}
      {erro && (
        <div>
          <Aviso>Não foi possível carregar os dados do dashboard.</Aviso>
          <button type="button" onClick={() => {
            setErro(false);
            setTentativa((anterior) => anterior + 1);
          }}>Tentar novamente</button>
        </div>
      )}
      {painel && (
        <>
          <dl className="indicadores" aria-label="Indicadores">
            {indicadores.map(([rotulo, valor, detalhe, icone]) => (
              <div className="cartao indicador" key={rotulo}>
                <dt className="indicador-topo">{rotulo} <Icone nome={icone} /></dt>
                <dd className="indicador-valor">{valor}</dd>
                <dd className="indicador-detalhe">{detalhe}</dd>
              </div>
            ))}
          </dl>

          <div className="dashboard-overview">
            <section className="cartao" aria-labelledby="dashboard-agenda">
              <div className="dashboard-cartao-topo">
                <div>
                  <h2 id="dashboard-agenda">Agenda do dia</h2>
                  <p className="indicador-detalhe">Provas, entregas, retiradas e devoluções</p>
                </div>
                <label className="dashboard-data"><span className="sr-only">Data</span>
                  <input type="date" value={dataSelecionada} onChange={(evento) => setDataSelecionada(evento.target.value)} />
                </label>
              </div>
              {agenda.length === 0 ? <p className="dashboard-vazio">Nenhum compromisso previsto para esta data.</p> : (
                <ul className="dashboard-appointments" role="list">
                  {[...agenda].sort((a, b) => a.dataHora.localeCompare(b.dataHora)).map(({ ordemId, dataHora, tipoCompromisso, clienteNome, statusAtual }) => {
                    const [estado, tom] = estadoDaOrdem(statusAtual);
                    return (
                      <li key={ordemId + tipoCompromisso}>
                        <time dateTime={dataHora}>{formatoHora.format(new Date(dataHora))}</time>
                        <div>
                          <Link to={'/ordens-servico#' + ordemId}>{clienteNome}</Link>
                          <span>{tipoCompromisso}</span>
                        </div>
                        <Etiqueta tom={tom}>{estado}</Etiqueta>
                      </li>
                    );
                  })}
                </ul>
              )}
            </section>

            <section className="cartao" aria-labelledby="dashboard-flow">
              <h2 id="dashboard-flow">Ritmo do ateliê</h2>
              <p className="indicador-detalhe">{ordensAtivas.length} ordens em andamento</p>
              <dl className="dashboard-stages">
                {etapas.map(({ rotulo, estados, cor }) => {
                  const quantidade = ordensAtivas.filter((ordem) => estados.includes(ordem.statusAtual)).length;
                  return (
                    <div key={rotulo}>
                      <dt>{rotulo}</dt>
                      <dd>{quantidade}</dd>
                      <dd className="dashboard-barra" aria-hidden="true">
                        <span style={{ width: (ordensAtivas.length ? quantidade / ordensAtivas.length * 100 : 0) + '%', background: cor }} />
                      </dd>
                    </div>
                  );
                })}
              </dl>
            </section>
          </div>
        </>
      )}
    </section>
  );
}
