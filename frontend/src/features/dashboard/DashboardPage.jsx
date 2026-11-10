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
const tiposDeCompromisso = [
  ['dataProva', 'Prova'],
  ['inicioLocacao', 'Retirada'],
  ['dataEntrega', 'Entrega'],
  ['fimLocacao', 'Devolução'],
];
const formatoDia = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'full' });
const formatoCompromisso = new Intl.DateTimeFormat('pt-BR', { weekday: 'short', dia: 'numeric', month: 'short' });

function chaveDaData(dataIso) {
  return [
    dataIso.getFullYear(),
    String(dataIso.getMonth() + 1).padStart(2, '0'),
    String(dataIso.getDate()).padStart(2, '0'),
  ].join('-');
}

function buscarCompromissos(ordens, inicio, fim) {
  return ordens.flatMap((ordem) => tiposDeCompromisso.flatMap(([campo, rotulo]) => {
    const dataIso = ordem[campo];
    const dia = dataIso?.slice(0, 10);
    if (!dia || dia < inicio || dia > fim) return [];
    if (ordem.statusAtual === 'RETIRADA' && campo !== 'fimLocacao') return [];
    if (ordem.statusAtual === 'CONCLUIDO' && campo === 'dataProva') return [];
    return [{ id: ordem.id + '-' + campo, dataIso, rotulo, cliente: ordem.clienteNome }];
  })).sort((primeiro, segundo) => primeiro.dataIso.localeCompare(segundo.dataIso));
}

export default function DashboardPage({ titulo }) {
  const { usuario } = useContext(ContextoDeAutenticacao);
  const ehRecepcao = usuario.perfil === 'ROLE_ATENDENTE';
  const [painel, setPainel] = useState(null);
  const [erro, setErro] = useState(false);
  const [tentativa, setTentativa] = useState(0);

  useEffect(() => {
    const controlador = new AbortController();
    const configuracao = { signal: controlador.signal };
    Promise.all([
      api.get('/ordens-servico', configuracao),
      api.get('/clientes', configuracao),
      api.get('/pecas', configuracao),
    ]).then(([ordens, clientes, pecas]) => setPainel({
      ordens: ordens.data,
      clientes: clientes.data,
      pecas: pecas.data,
    })).catch(() => {
      if (!controlador.signal.aborted) setErro(true);
    });

    return () => controlador.abort();
  }, [tentativa]);

  const agora = new Date();
  const ultimoDia = new Date(agora);
  ultimoDia.setDate(agora.getDate() + 6);
  const hoje = chaveDaData(agora);
  const ordensAtivas = (painel?.ordens ?? []).filter((ordem) =>
    etapas.some((etapa) => etapa.estados.includes(ordem.statusAtual)));
  const agenda = buscarCompromissos(ordensAtivas, hoje, chaveDaData(ultimoDia));
  const agendaDeHoje = agenda.filter((item) => item.dataIso.startsWith(hoje));
  const pecasDeLocacao = (painel?.pecas ?? []).filter((peca) => peca.tipo === 'LOCACAO');
  const pecasDisponiveis = pecasDeLocacao.filter((peca) => peca.status === 'DISPONIVEL').length;
  const locacoes = ordensAtivas.filter((ordem) => ordem.tipoFluxo === 'LOCACAO').length;

  const indicadores = ehRecepcao ? [
    ['Compromissos hoje', agendaDeHoje.length, 'Provas, retiradas, entregas e devoluções'],
    ['Retiradas do dia', agendaDeHoje.filter((item) => item.rotulo === 'Retirada').length, 'Locações com retirada prevista'],
    ['Devoluções previstas', agendaDeHoje.filter((item) => item.rotulo === 'Devolução').length, 'Locações com devolução prevista'],
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
              <h2 id="dashboard-agenda">Próximos agendamentos</h2>
              <p className="dashboard-detail">{formatoCompromisso.format(agora)} a {formatoCompromisso.format(ultimoDia)}</p>
              {agenda.length === 0 ? <p>Nenhum compromisso previsto para os próximos sete dias.</p> : (
                <ul className="dashboard-appointments" role="list">
                  {agenda.map(({ id, dataIso, rotulo, cliente }) => (
                    <li key={id}>
                      <time dateTime={dataIso}>{formatoCompromisso.format(new Date(dataIso))} · {dataIso.slice(11, 16)}</time>
                      <strong>{cliente}</strong>
                      <span>{rotulo}</span>
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
