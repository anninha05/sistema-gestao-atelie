import { useContext, useState } from 'react';
import { ContextoDeAutenticacao } from './AuthContext.jsx';
import { Aviso, validarCampos } from '../../shared/Interface.jsx';
import fotoAtelie from './atelie.webp';

export default function LoginPage() {
  const { entrar, erroDaSessao } = useContext(ContextoDeAutenticacao);
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);

  async function enviar(evento) {
    evento.preventDefault();
    const mensagem = validarCampos(evento.currentTarget);
    if (mensagem) {
      setErro(mensagem);
      return;
    }
    const campos = new FormData(evento.currentTarget);
    setErro('');
    setEnviando(true);

    try {
      await entrar(campos.get('email'), campos.get('password'));
    } catch (falha) {
      setErro(falha.response?.status === 401 && falha.config?.url === '/auth/login'
        ? 'E-mail ou senha incorretos.'
        : 'Não foi possível estabelecer a sessão. Verifique a conexão e tente novamente.');
    } finally {
      setEnviando(false);
    }
  }

  return (
    <main className="login-layout">
      <section className="login-content" aria-labelledby="login-title">
        <div className="login-card">

          <h1 id="login-title">Bem-vinda ao<br />seu ateliê!</h1>
          <p className="intro">Entre para acompanhar o dia a dia do ateliê.</p>

          <form className="login-form" onSubmit={enviar} aria-busy={enviando} noValidate>
            <label htmlFor="email">E-mail</label>
            <input id="email" name="email" type="email" placeholder="seuemail@exemplo.com"
              autoComplete="username" required />
            <label htmlFor="password">Senha</label>
            <input id="password" name="password" type="password"
              autoComplete="current-password" required />
            {(erro || erroDaSessao) && <Aviso>{erro || erroDaSessao}</Aviso>}
            <button type="submit" disabled={enviando}>{enviando ? 'Entrando…' : 'Entrar'}</button>
          </form>
        </div>
      </section>

      <aside className="login-story">
        <div className="story-photo"><img src={fotoAtelie} alt="" /></div>
        <div className="story-copy">
          <span className="eyebrow">Feito com cuidado, em cada detalhe</span>
          <p>Mais tempo para criar.<br />Mais leveza para gerir.</p>
        </div>
      </aside>
    </main>
  );
}
