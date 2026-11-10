import { useContext, useState } from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { ContextoDeAutenticacao } from '../features/auth/AuthContext.jsx';
import { Aviso, Modal } from '../shared/Interface.jsx';
import './workspace.css';

const desenhos = {
  painel: 'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z',
  clientes: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0z M22 21v-2a4 4 0 0 0-3-4 M17 3a4 4 0 0 1 0 8',
  pecas: 'M10 7a2 2 0 1 1 3.4 1.4L12 10v2 M12 12l9 5v3H3v-3z',
  ordens: 'M9 4H5v17h14V4h-4 M9 2h6v5H9z M8 12h8 M8 16h6',
  financeiro: 'M3 5h18v14H3z M3 9h18 M7 15h4',
  costura: 'M6 3a3 3 0 1 0 0 6 3 3 0 0 0 0-6z M6 15a3 3 0 1 0 0 6 3 3 0 0 0 0-6z M8 8l13 13 M8 16L21 3',
  perfil: 'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2 M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0z',
  sair: 'M10 4H4v16h6 M10 12h11 M17 8l4 4-4 4',
};

function IconeNavegacao({ nome }) {
  return (
    <svg className="workspace-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
      strokeWidth="1.65" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d={desenhos[nome]} />
    </svg>
  );
}

export default function LayoutDoAtelie({ perfil, paginas, children }) {
  const { usuario, sair } = useContext(ContextoDeAutenticacao);
  const [saindo, setSaindo] = useState(false);
  const [erro, setErro] = useState('');
  const [perfilAberto, setPerfilAberto] = useState(false);

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

  return (
    <div className="workspace">
      <a className="workspace-skip" href="#workspace-main">Ir para o conteúdo</a>
      <header className="workspace-sidebar">
        <div className="workspace-brand">
          <div className="brand">
            <span className="brand-name">Linho</span>
            <span className="eyebrow">Gestão de ateliê</span>
          </div>
          <button type="button" className="workspace-perfil botao-secundario botao-com-icone"
            aria-haspopup="dialog" onClick={() => {
              setErro('');
              setPerfilAberto(true);
            }}>
            <IconeNavegacao nome="perfil" /> <span>{perfil}</span>
          </button>
        </div>

        <div className="workspace-menu">
          {paginas.length > 0 && (
            <nav aria-label="Área de trabalho">
              <ul>
                {paginas.map(({ caminho, rotulo, rotuloCurto, icone }) => (
                  <li key={caminho}>
                    <NavLink to={caminho}>
                      <IconeNavegacao nome={icone} />
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
        </div>
      </header>

      {perfilAberto && (
        <Modal titulo="Meu perfil" aoFechar={() => setPerfilAberto(false)} ocupado={saindo}>
          <div className="workspace-account">
            <strong>{usuario.nome}</strong>
            <span>{usuario.email}</span>
            <span>{perfil}</span>
          </div>
          {erro && <Aviso>{erro}</Aviso>}
          <div className="modal-acoes">
            {usuario.perfil === 'ROLE_ADMIN' && <Link className="botao-link" to="/usuarios"
              onClick={() => setPerfilAberto(false)}>Gerenciar usuários</Link>}
            <button className="botao-secundario botao-com-icone" type="button" onClick={sairDaConta} disabled={saindo}>
              <IconeNavegacao nome="sair" />
              {saindo ? 'Saindo…' : 'Sair'}
            </button>
          </div>
        </Modal>
      )}

      <main className="workspace-content" id="workspace-main" tabIndex={-1}>
        <span className="eyebrow">{perfil}</span>
        {children ?? <Outlet />}
      </main>
    </div>
  );
}
