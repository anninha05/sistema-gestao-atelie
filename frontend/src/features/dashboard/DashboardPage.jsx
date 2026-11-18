import { Aviso, Icone } from '../../shared/Interface.jsx';
import { useContext, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ContextoDeAutenticacao } from '../auth/AuthContext.jsx';
import { api } from '../../shared/api.js';
import './dashboard.css';

const etapas = [
  { rotulo: 'Aguardando início', estados: ['PENDENTE', 'RESERVADA'] },
  { rotulo: 'Em produção ou ajustes', estados: ['CONFECCAO', 'PROVA', 'EM_PROVA', 'AJUSTES', 'PASSADORIA'] },
  { rotulo: 'Prontas para entrega ou retirada', estados: ['CONCLUIDO'] },
  { rotulo: 'Em locação', estados: ['RETIRADA'] },
];
const formatoDia = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'full' });
const formatoCompromisso = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

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

  const agora = new Date();
  const ordensAtivas = (painel?.ordens ?? []).filter((ordem) =>
    etapas.some((etapa) => etapa.estados.includes(ordem.statusAtual)));
  const agenda = painel?.compromissos ?? [];
  const pecasDeLocacao = (painel?.pecas ?? []).filter((peca) => peca.tipo === 'LOCACAO');
  const pecasDisponiveis = pecasDeLocacao.filter((peca) => peca.status === 'DISPONIVEL').length;
  const locacoes = ordensAtivas.filter((ordem) => ordem.tipoFluxo === 'LOCACAO').length;

  const indicadores = ehRecepcao ? [
    ['Compromissos do dia', agenda.length, 'Provas, retiradas, entregas e devoluções'],
    ['Retiradas do dia', agenda.filter((item) => item.tipoCompromisso === 'Retirada').length, 'Locações com retirada prevista'],
    ['Devoluções previstas', agenda.filter((item) => item.tipoCompromisso === 'Devolução').length, 'Locações com devolução prevista'],
  ] : [
    ['Ordens de serviço ativas', ordensAtivas.length, locacoes + ' de locação · ' + (ordensAtivas.length - locacoes) + ' sob medida'],
    ['Clientes cadastrados', painel?.clientes.length, 'Total de clientes do ateliê'],
    ['Peças disponíveis', pecasDisponiveis, 'De ' + pecasDeLocacao.length + ' peças no inventário de locação'],
  ];

  return (
    <section className="dashboard" aria-labelledby="dashboard-title">
      <header className="dashboard-header">
        <div>
          <p className="eyebrow">{titulo} · {formatoDia.format(agora)}</p>
          <h1 id="dashboard-title">Olá, {usuario.nome.trim().split(/\s+/)[0]}!</h1>
          <p className="intro">Acompanhe o andamento do ateliê e os próximos compromissos.</p>
        </div>
        <Link className="dashboard-action botao-com-icone" to="/ordens-servico" state={{ novaOrdem: true }}>
          <Icone nome="adicionar" /> Nova ordem de serviço
        </Link>
      </header>

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
          <dl className="dashboard-metrics" aria-label="Indicadores">
            {indicadores.map(([rotulo, value, detail]) => (
              <div className="dashboard-card" key={rotulo}>
                <dt>{rotulo}</dt>
                <dd className="dashboard-value">{value}</dd>
                <dd className="dashboard-detail">{detail}</dd>
              </div>
            ))}
          </dl>

          <div className="dashboard-overview">
            <section className="dashboard-card" aria-labelledby="dashboard-agenda">
              <h2 id="dashboard-agenda">Agenda diária</h2>
              <label>Data<input type="date" value={dataSelecionada} onChange={(evento) => setDataSelecionada(evento.target.value)} /></label>
              {agenda.length === 0 ? <p>Nenhum compromisso previsto para esta data.</p> : (
                <ul className="dashboard-appointments" role="list">
                  {agenda.map(({ ordemId, dataHora, tipoCompromisso, clienteNome, statusAtual }) => (
                    <li key={ordemId + tipoCompromisso}>
                      <time dateTime={dataHora}>{formatoCompromisso.format(new Date(dataHora))}</time>
                      <Link to={'/ordens-servico#' + ordemId}><strong>{clienteNome}</strong></Link>
                      <span>{tipoCompromisso} · {statusAtual}</span>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section className="dashboard-card" aria-labelledby="dashboard-flow">
              <h2 id="dashboard-flow">Ritmo do ateliê</h2>
              <p className="dashboard-detail">{ordensAtivas.length} ordens em andamento</p>
              <dl className="dashboard-stages">
                {etapas.map(({ rotulo, estados }) => (
                  <div key={rotulo}>
                    <dt>{rotulo}</dt>
                    <dd>{ordensAtivas.filter((ordem) => estados.includes(ordem.statusAtual)).length}</dd>
                  </div>
                ))}
              </dl>
            </section>
          </div>
        </>
      )}
    </section>
  );
}
