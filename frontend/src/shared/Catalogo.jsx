import { useEffect, useState } from 'react';
import { api, enviarComCsrf, requisitarComCsrf } from './api.js';
import { Aviso, CabecalhoPagina, Etiqueta, Icone, Modal, validarCampos } from './Interface.jsx';
import './styles/catalogo.css';

export default function Catalogo({
  titulo, descricao, caminho, rotuloCadastro, rotuloBusca, correspondeABusca, filtros,
  campos, montarDados, tituloDoItem, detalhesDoItem, acoesDoItem, aoCadastrar, validar, cadastroDesabilitado = false, abrirAoEntrar = false,
  camposEdicao, montarDadosEdicao, etiquetaDoItem, subtituloDoItem, secao = false,
}) {
  const [itens, setItens] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erroAoCarregar, setErroAoCarregar] = useState(false);
  const [tentativa, setTentativa] = useState(0);
  const [busca, setBusca] = useState('');
  const [cadastroAberto, setCadastroAberto] = useState(abrirAoEntrar);
  const [salvando, setSalvando] = useState(false);
  const [erroAoSalvar, setErroAoSalvar] = useState('');
  const [confirmacao, setConfirmacao] = useState('');
  const [itemEmEdicao, setItemEmEdicao] = useState(null);

  useEffect(() => {
    const controlador = new AbortController();
    api.get(caminho, { signal: controlador.signal })
      .then(({ data }) => setItens(data))
      .catch(() => {
        if (!controlador.signal.aborted) setErroAoCarregar(true);
      })
      .finally(() => {
        if (!controlador.signal.aborted) setCarregando(false);
      });

    return () => controlador.abort();
  }, [caminho, tentativa]);

  function tentarNovamente() {
    setCarregando(true);
    setErroAoCarregar(false);
    setTentativa((anteriores) => anteriores + 1);
  }

  async function salvar(evento) {
    evento.preventDefault();
    const formulario = new FormData(evento.currentTarget);
    const erroDeValidacao = validarCampos(evento.currentTarget) || validar?.(formulario);
    if (erroDeValidacao) {
      setConfirmacao('');
      setErroAoSalvar(erroDeValidacao);
      return;
    }
    setSalvando(true);
    setErroAoSalvar('');
    setConfirmacao('');

    try {
      const { data } = await enviarComCsrf(caminho, montarDados(formulario));
      setItens((anteriores) => [data, ...anteriores]);
      setBusca('');
      aoCadastrar?.();
      setCadastroAberto(false);
      setConfirmacao('Cadastro concluído.');
    } catch (falha) {
      setErroAoSalvar(falha.response?.data?.mensagem || 'Não foi possível salvar o cadastro. Tente novamente.');
    } finally {
      setSalvando(false);
    }
  }

  async function salvarEdicao(evento) {
    evento.preventDefault();
    const formulario = new FormData(evento.currentTarget);
    const erroDeValidacao = validarCampos(evento.currentTarget);
    if (erroDeValidacao) {
      setErroAoSalvar(erroDeValidacao);
      return;
    }
    setSalvando(true);
    setErroAoSalvar('');
    try {
      const { data } = await requisitarComCsrf('patch', caminho + '/' + itemEmEdicao.id, montarDadosEdicao(formulario, itemEmEdicao));
      atualizarItem(data);
      setItemEmEdicao(null);
      setConfirmacao('Cadastro atualizado.');
    } catch (falha) {
      setErroAoSalvar(falha.response?.data?.mensagem || 'Não foi possível atualizar o cadastro. Tente novamente.');
    } finally {
      setSalvando(false);
    }
  }

  function atualizarItem(itemAtualizado) {
    setItens((anteriores) => anteriores.map((registro) => registro.id === itemAtualizado.id ? itemAtualizado : registro));
  }

  function fecharCadastro() {
    setCadastroAberto(false);
    setErroAoSalvar('');
  }

  const itensVisiveis = itens.filter((registro) => correspondeABusca(registro, busca.trim()));

  const botaoCadastro = (
    <button type="button" className="botao-com-icone" aria-haspopup="dialog"
      disabled={carregando || erroAoCarregar || salvando || cadastroDesabilitado} onClick={() => {
        setCadastroAberto(true);
        setErroAoSalvar('');
        setConfirmacao('');
      }}>
      <Icone nome="adicionar" /> {rotuloCadastro}
    </button>
  );
  const TituloItem = secao ? 'h3' : 'h2';

  return (
    <section className={'catalogo' + (secao ? ' catalogo-secao' : '')} aria-labelledby="catalogo-title">
      {secao ? (
        <header className="cabecalho-pagina">
          <div><h2 id="catalogo-title">{titulo}</h2><p className="intro">{descricao}</p></div>
          {botaoCadastro}
        </header>
      ) : (
        <CabecalhoPagina id="catalogo-title" titulo={titulo} descricao={descricao}>{botaoCadastro}</CabecalhoPagina>
      )}

      {cadastroAberto && !carregando && !erroAoCarregar && !cadastroDesabilitado && (
        <Modal titulo={rotuloCadastro} aoFechar={fecharCadastro} ocupado={salvando}>
          <form className="catalogo-form" onSubmit={salvar} aria-busy={salvando} noValidate>
            {erroAoSalvar && <Aviso>{erroAoSalvar}</Aviso>}
            <fieldset disabled={salvando}>{campos}</fieldset>
            <div className="modal-acoes">
              <button className="botao-secundario" type="button" onClick={fecharCadastro} disabled={salvando}>
                Cancelar
              </button>
              <button type="submit" disabled={salvando}>{salvando ? 'Salvando…' : 'Salvar'}</button>
            </div>
          </form>
        </Modal>
      )}
      {itemEmEdicao && camposEdicao && !cadastroDesabilitado && (
        <Modal titulo={'Editar ' + tituloDoItem(itemEmEdicao)} aoFechar={() => setItemEmEdicao(null)} ocupado={salvando}>
          <form className="catalogo-form" onSubmit={salvarEdicao} aria-busy={salvando} noValidate>
            {erroAoSalvar && <Aviso>{erroAoSalvar}</Aviso>}
            <fieldset disabled={salvando}>{camposEdicao(itemEmEdicao)}</fieldset>
            <div className="modal-acoes">
              <button className="botao-secundario" type="button" onClick={() => setItemEmEdicao(null)} disabled={salvando}>Cancelar</button>
              <button type="submit" disabled={salvando}>{salvando ? 'Salvando…' : 'Salvar alterações'}</button>
            </div>
          </form>
        </Modal>
      )}
      {confirmacao && <Aviso tipo="sucesso">{confirmacao}</Aviso>}

      <div className="catalogo-barra">
        <label className="catalogo-busca">
          <span className="sr-only">{rotuloBusca}</span>
          <Icone nome="busca" />
          <input type="search" placeholder={rotuloBusca} value={busca} onChange={(evento) => setBusca(evento.target.value)} />
        </label>
        {filtros}
        {!carregando && !erroAoCarregar && (
          <p className="catalogo-total">{itensVisiveis.length} {itensVisiveis.length === 1 ? 'registro' : 'registros'}</p>
        )}
      </div>

      {carregando && <p className="catalogo-vazio" role="status">Carregando…</p>}
      {erroAoCarregar && (
        <div>
          <Aviso>Não foi possível carregar a lista.</Aviso>
          <button type="button" onClick={tentarNovamente}>Tentar novamente</button>
        </div>
      )}
      {!carregando && !erroAoCarregar && (itensVisiveis.length === 0 ? (
        <p className="catalogo-vazio">{itens.length === 0 ? 'Nenhum cadastro por enquanto.' : 'Nenhum registro corresponde aos filtros.'}</p>
      ) : (
        <ul className="catalogo-lista" role="list">
          {itensVisiveis.map((registro) => {
            const etiqueta = etiquetaDoItem?.(registro);
            const subtitulo = subtituloDoItem?.(registro);
            return (
              <li key={registro.id} id={registro.id} className="cartao catalogo-item">
                <div className="catalogo-item-topo">
                  <div>
                    <TituloItem>{tituloDoItem(registro)}</TituloItem>
                    {subtitulo && <span className="catalogo-item-subtitulo">{subtitulo}</span>}
                  </div>
                  {etiqueta && <Etiqueta tom={etiqueta[1]}>{etiqueta[0]}</Etiqueta>}
                </div>
                <dl>{detalhesDoItem(registro).filter(([, valor]) => valor != null && valor !== '').map(([rotulo, valor]) => (
                  <div key={rotulo}><dt>{rotulo}</dt><dd>{valor}</dd></div>
                ))}</dl>
                <div className="catalogo-item-acoes">
                  {camposEdicao && !cadastroDesabilitado && <button type="button" className="botao-secundario"
                    onClick={() => { setErroAoSalvar(''); setItemEmEdicao(registro); }}>Editar</button>}
                  {acoesDoItem?.(registro, atualizarItem)}
                </div>
              </li>
            );
          })}
        </ul>
      ))}
    </section>
  );
}
