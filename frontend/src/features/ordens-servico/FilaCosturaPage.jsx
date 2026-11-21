import { useEffect, useState } from 'react';
import { api, requisitarComCsrf } from '../../shared/api.js';
import { Aviso, CabecalhoPagina, Etiqueta, Icone } from '../../shared/Interface.jsx';
import { dataHora, tiposFluxo } from '../../shared/formatos.js';
import { acoesDiretas, rotasDasAcoes } from './acoes.js';
import '../../shared/styles/catalogo.css';

const etapas = [
  { rotulo: 'Aguardando início', estados: ['PENDENTE', 'RESERVADA'] },
  { rotulo: 'Confecção', estados: ['CONFECCAO'] },
  { rotulo: 'Em prova', estados: ['PROVA', 'EM_PROVA'] },
  { rotulo: 'Ajustes', estados: ['AJUSTES'] },
  { rotulo: 'Passadoria', estados: ['PASSADORIA'] },
];

export default function FilaCosturaPage({ titulo }) {
  const [fila, setFila] = useState(null);
  const [erroAoCarregar, setErroAoCarregar] = useState(false);
  const [tentativa, setTentativa] = useState(0);
  const [atualizando, setAtualizando] = useState(null);
  const [mensagem, setMensagem] = useState(null);
  const [etapaAtiva, setEtapaAtiva] = useState(null);

  useEffect(() => {
    const controlador = new AbortController();
    const configuracao = { signal: controlador.signal };
    Promise.all([api.get('/ordens-servico/producao', configuracao), api.get('/pecas/producao', configuracao)])
      .then(([ordens, pecas]) => setFila({
        ordens: ordens.data,
        nomesDasPecas: new Map(pecas.data.map((peca) => [peca.id, peca.descricao + ' · ' + peca.codigoEtiqueta])),
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
      await requisitarComCsrf('patch', '/ordens-servico/' + ordem.id + '/' + rotasDasAcoes[acao]);
      setTentativa((anterior) => anterior + 1);
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
  }));
  const secaoAtual = secoes.find((etapa) => etapa.rotulo === etapaAtiva && etapa.ordens.length)
    ?? secoes.find((etapa) => etapa.ordens.length);

  return (
    <section className="catalogo" aria-labelledby="fila-title">
      <CabecalhoPagina id="fila-title" titulo={titulo}
        descricao={fila ? fila.ordens.length + (fila.ordens.length === 1 ? ' ordem na bancada' : ' ordens na bancada') : 'Consulte as peças e avance a produção de cada ordem.'} />
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
      {fila && !secaoAtual && <p className="catalogo-vazio">Nenhuma ordem em produção ou aguardando início.</p>}
      {secaoAtual && <>
        <div className="fila-etapas" role="group" aria-label="Etapas da produção">
          {secoes.map((etapa) => (
            <button key={etapa.rotulo} type="button" aria-pressed={etapa === secaoAtual}
              disabled={!etapa.ordens.length} onClick={() => setEtapaAtiva(etapa.rotulo)}>
              {etapa.rotulo} <span>{etapa.ordens.length}</span>
            </button>
          ))}
        </div>
        <h2 className="fila-titulo">{secaoAtual.rotulo}</h2>
        <ul className="catalogo-lista" role="list">
          {secaoAtual.ordens.map((ordem) => {
            const prazo = ordem.dataEntrega || ordem.inicioLocacao || ordem.dataEvento;
            return (
              <li key={ordem.id} className="cartao catalogo-item" aria-busy={atualizando === ordem.id}>
                <div className="catalogo-item-topo">
                  <div>
                    <h3>{ordem.clienteNome}</h3>
                    {prazo && <span className="fila-prazo"><Icone nome="prazo" /> Prazo: {dataHora.format(new Date(prazo))}</span>}
                  </div>
                  <Etiqueta tom={ordem.tipoFluxo === 'LOCACAO' ? 'azul' : 'rosa'}>{tiposFluxo[ordem.tipoFluxo] ?? ordem.tipoFluxo}</Etiqueta>
                </div>
                {ordem.dataProva && <p className="fila-prova">Prova: {dataHora.format(new Date(ordem.dataProva))}</p>}
                {ordem.pecas?.map((item) => (
                  <div className="fila-peca" key={item.pecaId}>
                    <strong>{fila.nomesDasPecas.get(item.pecaId) ?? 'Peça'}</strong>
                    <dl>
                      <div><dt>Medidas</dt><dd>{item.medidas || '—'}</dd></div>
                      <div><dt>Instruções de costura</dt><dd>{item.instrucoesCostura || '—'}</dd></div>
                    </dl>
                  </div>
                ))}
                <div className="catalogo-item-acoes">
                  {acoesDiretas(ordem).map((item) => (
                    <button key={item.acao} type="button" className={item.acao === 'CANCELAR' ? 'botao-secundario' : 'botao-com-icone'}
                      disabled={atualizando !== null || !item.habilitada} title={item.motivoBloqueio ?? undefined}
                      onClick={() => avancar(ordem, item.acao)}>{item.rotulo}</button>
                  ))}
                </div>
              </li>
            );
          })}
        </ul>
      </>}
    </section>
  );
}
