import { useEffect, useState } from 'react';
import { api, enviarComCsrf } from './api.js';
import { Aviso, Icone, Modal, validarCampos } from './Interface.jsx';
import './styles/catalogo.css';

export default function Catalogo({
  titulo, descricao, caminho, rotuloCadastro, rotuloBusca, correspondeABusca, filtros,
  campos, montarDados, tituloDoItem, detalhesDoItem, acoesDoItem, aoCadastrar, validar, cadastroDesabilitado = false, abrirAoEntrar = false,
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

  function atualizarItem(itemAtualizado) {
    setItens((anteriores) => anteriores.map((registro) => registro.id === itemAtualizado.id ? itemAtualizado : registro));
  }

  function fecharCadastro() {
    setCadastroAberto(false);
    setErroAoSalvar('');
  }

  const itensVisiveis = itens.filter((registro) => correspondeABusca(registro, busca.trim()));

  return (
    <section className="catalogo" aria-labelledby="catalogo-title">
      <div className="catalogo-header">
        <div>
          <h1 id="catalogo-title">{titulo}</h1>
          <p className="intro">{descricao}</p>
        </div>
        <button type="button" className="botao-com-icone" aria-haspopup="dialog"
          disabled={carregando || erroAoCarregar || salvando || cadastroDesabilitado} onClick={() => {
            setCadastroAberto(true);
            setErroAoSalvar('');
            setConfirmacao('');
          }}>
          <Icone nome="adicionar" /> {rotuloCadastro}
        </button>
      </div>

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
      {confirmacao && <Aviso tipo="sucesso">{confirmacao}</Aviso>}

      <div className="catalogo-filtros">
        <label>
          {rotuloBusca}
          <input type="search" value={busca} onChange={(evento) => setBusca(evento.target.value)} />
        </label>
        {filtros}
      </div>

      {carregando && <p role="status">Carregando…</p>}
      {erroAoCarregar && (
        <div>
          <Aviso>Não foi possível carregar a lista.</Aviso>
          <button type="button" onClick={tentarNovamente}>Tentar novamente</button>
        </div>
      )}
      {!carregando && !erroAoCarregar && (
        <>
          <p className="catalogo-total">{itensVisiveis.length} {itensVisiveis.length === 1 ? 'registro' : 'registros'}</p>
          {itensVisiveis.length === 0 ? (
            <p>{itens.length === 0 ? 'Nenhum cadastro por enquanto.' : 'Nenhum registro corresponde aos filtros.'}</p>
          ) : (
            <ul className="catalogo-lista" role="list">
              {itensVisiveis.map((registro) => (
                <li key={registro.id}>
                  <h2>{tituloDoItem(registro)}</h2>
                  <dl>{detalhesDoItem(registro).map(([rotulo, valor]) => (
                    <div key={rotulo}><dt>{rotulo}</dt><dd>{valor ?? '—'}</dd></div>
                  ))}</dl>
                  {acoesDoItem?.(registro, atualizarItem)}
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </section>
  );
}
