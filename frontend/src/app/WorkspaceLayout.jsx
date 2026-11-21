import { useContext, useState } from 'react';
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom';
import { ContextoDeAutenticacao } from '../features/auth/AuthContext.jsx';
import { Aviso, Icone, Marca, Modal } from '../shared/Interface.jsx';
import './workspace.css';

const formatoData = new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: '2-digit', month: 'short', year: 'numeric' });

function dataDeHoje() {
  const texto = formatoData.format(new Date());
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

export default function LayoutDoAtelie({ perfil, paginas, children }) {
  const { usuario, sair } = useContext(ContextoDeAutenticacao);
  const { pathname } = useLocation();
  const [saindo, setSaindo] = useState(false);
  const [erro, setErro] = useState('');
  const [perfilAberto, setPerfilAberto] = useState(false);
  const pagina = pathname === '/usuarios' ? { caminho: pathname, rotulo: 'Usuários' }
    : paginas.find(({ caminho }) => pathname === caminho || pathname.startsWith(caminho + '/'));
  const emDetalhe = pagina && pathname !== pagina.caminho;

  async function sairDaConta() {
    setErro('');
    setSaindo(true);
    try {
      await sair();
    } catch {
      setErro('Não foi possível sair. Tente novamente.');
      setSaindo(false);
    }
  }

  function abrirPerfil() {
    setErro('');
    setPerfilAberto(true);
  }

  return (
    <div className="workspace">
      <a className="workspace-skip" href="#workspace-main">Ir para o conteúdo</a>
      <header className="workspace-sidebar">
        <div className="workspace-brand">
          <Marca />
          <button type="button" className="workspace-perfil-chip botao-secundario" aria-haspopup="dialog" onClick={abrirPerfil}>
            {perfil}
          </button>
        </div>

        {paginas.length > 0 && (
          <nav className="workspace-menu" aria-label="Área de trabalho">
            <span className="eyebrow workspace-secao">Espaço de trabalho</span>
            <ul>
              {paginas.map(({ caminho, rotulo, rotuloCurto, icone }) => (
                <li key={caminho}>
                  <NavLink to={caminho}>
                    <Icone nome={icone} />
                    {rotuloCurto ? <>
                      <span className="workspace-label-desktop">{rotulo}</span>
                      <span className="workspace-label-mobile">{rotuloCurto}</span>
                    </> : <span>{rotulo}</span>}
                  </NavLink>
                </li>
              ))}
            </ul>
          </nav>
        )}

        <button type="button" className="workspace-usuario" aria-haspopup="dialog" onClick={abrirPerfil}>
          <span className="workspace-avatar" aria-hidden="true">{usuario.nome.trim().charAt(0)}</span>
          <span className="workspace-usuario-texto">
            <strong>{usuario.nome}</strong>
            <span>{perfil}</span>
          </span>
          <Icone nome="avancar" />
        </button>
      </header>

      {perfilAberto && (
        <Modal titulo="Meu perfil" aoFechar={() => setPerfilAberto(false)} ocupado={saindo}>
          <div className="workspace-account">
            <span className="workspace-avatar" aria-hidden="true">{usuario.nome.trim().charAt(0)}</span>
            <div>
              <strong>{usuario.nome}</strong>
              <span>{usuario.email}</span>
              <span>{perfil}</span>
            </div>
          </div>
          {erro && <Aviso>{erro}</Aviso>}
          <div className="modal-acoes">
            {usuario.perfil === 'ROLE_ADMIN' && <Link className="botao-link botao-secundario" to="/usuarios"
              onClick={() => setPerfilAberto(false)}><Icone nome="clientes" /> Gerenciar usuários</Link>}
            <button className="botao-com-icone" type="button" onClick={sairDaConta} disabled={saindo}>
              <Icone nome="sair" />
              {saindo ? 'Saindo…' : 'Sair'}
            </button>
          </div>
        </Modal>
      )}

      <div className="workspace-area">
        <div className="workspace-topo">
          <nav aria-label="Você está em" className="workspace-trilha">
            <span>Ateliê Lucinéia Padilha</span>
            {pagina && <><Icone nome="avancar" />{emDetalhe
              ? <><Link to={pagina.caminho}>{pagina.rotulo}</Link><Icone nome="avancar" /><strong>Detalhes</strong></>
              : <strong>{pagina.rotulo}</strong>}</>}
          </nav>
          <span className="workspace-data">{dataDeHoje()}</span>
        </div>
        <main className="workspace-content" id="workspace-main" tabIndex={-1}>
          {children ?? <Outlet />}
        </main>
      </div>
    </div>
  );
}
