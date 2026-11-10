import { useEffect, useState } from 'react';
import { api, requisitarComCsrf } from '../../shared/api.js';
import { Aviso } from '../../shared/Interface.jsx';
import '../../shared/styles/catalogo.css';

const inicio = [['confeccao', 'Iniciar trabalho'], ['prova', 'Enviar para prova']];
const emProva = [['confeccao', 'Iniciar ajustes'], ['passadoria', 'Iniciar passadoria']];
const etapas = [
  { rotulo: 'Aguardando início', estados: ['PENDENTE', 'RESERVADA'], acoes: inicio },
  { rotulo: 'Confecção', estados: ['CONFECCAO'], acoes: [['prova', 'Enviar para prova']] },
  { rotulo: 'Em prova', estados: ['PROVA', 'EM_PROVA'], acoes: emProva },
  { rotulo: 'Ajustes', estados: ['AJUSTES'], acoes: [['prova', 'Enviar para prova'], ['passadoria', 'Iniciar passadoria']] },
  { rotulo: 'Passadoria', estados: ['PASSADORIA'], acoes: [['concluir', 'Concluir produção']] },
];
const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

export default function FilaCosturaPage({ titulo }) {
  const [fila, setFila] = useState(null);
  const [erroAoCarregar, setErroAoCarregar] = useState(false);
  const [tentativa, setTentativa] = useState(0);
  const [atualizando, setAtualizando] = useState(null);
  const [mensagem, setMensagem] = useState(null);

  useEffect(() => {
    const controlador = new AbortController();
    const configuracao = { signal: controlador.signal };
    Promise.all([api.get('/ordens-servico', configuracao), api.get('/pecas', configuracao)])
      .then(([ordens, pecas]) => setFila({
        ordens: ordens.data,
        nomesDasPecas: new Map(pecas.data.map((peca) => [peca.id, peca.descricao])),
      }))
      .catch(() => {
        if (!controlador.signal.aborted) setErroAoCarregar(true);
      });
    return () => controlador.abort();
  }, [tentativa]);

  async function avancar(ordem, acao) {
    setAtualizando(ordem.id);
    setMensagem(null);
    try {
      const { data } = await requisitarComCsrf('patch', '/ordens-servico/' + ordem.id + '/' + acao);
      setFila((anterior) => ({
        ...anterior,
        ordens: anterior.ordens.map((item) => item.id === data.id ? data : item),
      }));
      setMensagem({ texto: 'Etapa atualizada para a ordem de ' + ordem.clienteNome + '.' });
    } catch (erro) {
      setMensagem({
        erro: true,
        texto: erro.response?.data?.mensagem || 'Não foi possível atualizar a etapa. Tente novamente.',
      });
    } finally {
      setAtualizando(null);
    }
  }

  const secoes = etapas.map((etapa) => ({
    ...etapa,
    ordens: fila?.ordens.filter((ordem) => etapa.estados.includes(ordem.statusAtual)) ?? [],
  })).filter((etapa) => etapa.ordens.length > 0);

  return (
    <section className="catalogo" aria-labelledby="fila-title">
      <h1 id="fila-title">{titulo}</h1>
      <p className="intro">Consulte as peças e avance a produção de cada ordem.</p>
      {!fila && !erroAoCarregar && <p role="status">Carregando fila…</p>}
      {erroAoCarregar && (
        <div>
          <Aviso>Não foi possível carregar a fila.</Aviso>
          <button type="button" onClick={() => {
            setErroAoCarregar(false);
            setTentativa((anterior) => anterior + 1);
          }}>Tentar novamente</button>
        </div>
      )}
      {mensagem && <Aviso tipo={mensagem.erro ? 'erro' : 'sucesso'}>{mensagem.texto}</Aviso>}
      {fila && secoes.length === 0 && <p>Nenhuma ordem em produção ou aguardando início.</p>}
      {secoes.map((etapa) => (
        <section key={etapa.rotulo}>
          <h2>{etapa.rotulo} ({etapa.ordens.length})</h2>
          <ul className="catalogo-lista" role="list">
            {etapa.ordens.map((ordem) => {
              const prazo = ordem.dataEntrega || ordem.inicioLocacao || ordem.dataEvento;
              return (
                <li key={ordem.id} aria-busy={atualizando === ordem.id}>
                  <h3>{ordem.clienteNome}</h3>
                  <dl>
                    <div><dt>Peças</dt><dd>{ordem.itens?.map((item) =>
                      fila.nomesDasPecas.get(item.pecaId) ?? item.pecaId).join(', ') || '—'}</dd></div>
                    <div><dt>Prazo</dt><dd>{prazo ? dataHora.format(new Date(prazo)) : '—'}</dd></div>
                    <div><dt>Prova</dt><dd>{ordem.dataProva ? dataHora.format(new Date(ordem.dataProva)) : '—'}</dd></div>
                    <div><dt>Medidas</dt><dd>{ordem.medidas || '—'}</dd></div>
                    <div><dt>Instruções de costura</dt><dd>{ordem.instrucoesCostura || '—'}</dd></div>
                  </dl>
                  <div className="fila-acoes">
                    {etapa.acoes.filter(([acao]) => ordem.statusAtual !== 'PROVA' || acao !== 'passadoria')
                      .map(([acao, rotulo]) => (
                        <button key={acao} type="button" disabled={atualizando !== null}
                          onClick={() => avancar(ordem, acao)}>{rotulo}</button>
                      ))}
                  </div>
                </li>
              );
            })}
          </ul>
        </section>
      ))}
    </section>
  );
}
