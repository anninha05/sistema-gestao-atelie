import { useContext } from 'react';
import { Link, Navigate, Route, Routes } from 'react-router-dom';
import { ContextoDeAutenticacao } from '../features/auth/AuthContext.jsx';
import LoginPage from '../features/auth/LoginPage.jsx';
import ClientesPage from '../features/clientes/ClientesPage.jsx';
import RelatoriosPage from '../features/financeiro/RelatoriosPage.jsx';
import FilaCosturaPage from '../features/ordens-servico/FilaCosturaPage.jsx';
import OrdensServicoPage from '../features/ordens-servico/OrdensServicoPage.jsx';
import DashboardPage from '../features/dashboard/DashboardPage.jsx';
import PecasPage from '../features/pecas/PecasPage.jsx';
import LayoutDoAtelie from './WorkspaceLayout.jsx';
import UsuariosPage from '../features/usuarios/UsuariosPage.jsx';

const paginasDoAtendimento = [
  { caminho: '/clientes', rotulo: 'Clientes', icone: 'clientes', Pagina: ClientesPage },
  { caminho: '/pecas', rotulo: 'Inventário de peças', rotuloCurto: 'Peças', icone: 'pecas', Pagina: PecasPage },
  { caminho: '/ordens-servico', rotulo: 'Ordens de serviço', rotuloCurto: 'Ordens', icone: 'ordens', Pagina: OrdensServicoPage },
];

const areasDeTrabalho = {
  ROLE_ADMIN: {
    rotulo: 'Administração',
    paginas: [
      { caminho: '/painel', rotulo: 'Dashboard', rotuloCurto: 'Início', icone: 'painel', Pagina: DashboardPage },
      ...paginasDoAtendimento,
      { caminho: '/financeiro', rotulo: 'Financeiro', icone: 'financeiro', Pagina: RelatoriosPage },
    ],
  },
  ROLE_ATENDENTE: {
    rotulo: 'Atendimento',
    paginas: [
      { caminho: '/painel', rotulo: 'Recepção', icone: 'painel', Pagina: DashboardPage },
      ...paginasDoAtendimento,
    ],
  },
  ROLE_PRODUCAO: {
    rotulo: 'Produção',
    paginas: [
      { caminho: '/producao', rotulo: 'Fila de costura', icone: 'costura', Pagina: FilaCosturaPage },
      { caminho: '/pecas', rotulo: 'Inventário de peças', rotuloCurto: 'Peças', icone: 'pecas', Pagina: PecasPage },
    ],
  },
};

const caminhosPrivados = [...new Set(Object.values(areasDeTrabalho).flatMap(({ paginas }) => paginas.map(({ caminho }) => caminho)))];

function PaginaNaoEncontrada({ destino }) {
  return (
    <section className="pagina-404" aria-labelledby="titulo-404">
      <span className="eyebrow">Erro 404</span>
      <h1 id="titulo-404">Página não encontrada</h1>
      <p className="intro">Confira o endereço ou volte para o início do Linho.</p>
      <Link className="botao-link" to={destino}>Voltar ao início</Link>
    </section>
  );
}

export default function AppRoutes() {
  const { usuario, carregando } = useContext(ContextoDeAutenticacao);

  if (carregando) {
    return <main className="session-layout" role="status">Carregando sessão…</main>;
  }

  if (!usuario) {
    return (
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/usuarios" element={<Navigate to="/login" replace />} />
        <Route index element={<Navigate to="/login" replace />} />
        {caminhosPrivados.map((caminho) => (
          <Route key={caminho} path={caminho} element={<Navigate to="/login" replace />} />
        ))}
        <Route path="*" element={<main className="session-layout"><PaginaNaoEncontrada destino="/login" /></main>} />
      </Routes>
    );
  }

  const areaDeTrabalho = areasDeTrabalho[usuario.perfil];

  if (!areaDeTrabalho) {
    return (
      <LayoutDoAtelie perfil={usuario.perfil} paginas={[]}>
        <h1>Acesso indisponível</h1>
        <p className="intro">Este perfil não tem uma área de trabalho configurada.</p>
      </LayoutDoAtelie>
    );
  }

  return (
    <Routes>
      <Route element={<LayoutDoAtelie perfil={areaDeTrabalho.rotulo} paginas={areaDeTrabalho.paginas} />}>
        {usuario.perfil === 'ROLE_ADMIN' && <Route path="/usuarios" element={<UsuariosPage />} />}
        {areaDeTrabalho.paginas.map(({ caminho, rotulo, Pagina }) => (
          <Route key={caminho} path={caminho} element={<Pagina titulo={rotulo} />} />
        ))}
      </Route>
        <Route index element={<Navigate to={areaDeTrabalho.paginas[0].caminho} replace />} />
        <Route path="/login" element={<Navigate to={areaDeTrabalho.paginas[0].caminho} replace />} />
        <Route path="*" element={<PaginaNaoEncontrada destino={areaDeTrabalho.paginas[0].caminho} />} />
    </Routes>
  );
}
