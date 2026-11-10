import { useEffect, useId, useLayoutEffect, useRef, useState } from 'react';

const desenhos = {
  adicionar: 'M12 5v14 M5 12h14',
  fechar: 'M6 6l12 12 M18 6L6 18',
  seta: 'm6 9 6 6 6-6',
  aviso: 'M12 8v4 M12 16h.01 M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0z',
  sucesso: 'm7 12 3 3 7-7 M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0z',
};

export function Icone({ nome }) {
  return (
    <svg className="icone" viewBox="0 0 24 24" fill="none" stroke="currentColor"
      strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d={desenhos[nome]} />
    </svg>
  );
}

export function Aviso({ children, tipo = 'erro' }) {
  return (
    <div className={'aviso aviso-' + tipo} role={tipo === 'erro' ? 'alert' : 'status'}>
      <Icone nome={tipo === 'sucesso' ? 'sucesso' : 'aviso'} />
      <div>{children}</div>
    </div>
  );
}

export function Modal({ titulo, aoFechar, ocupado = false, children }) {
  const referencia = useRef(null);
  const tituloId = useId();

  useLayoutEffect(() => {
    const janela = referencia.current;
    const focoAnterior = document.activeElement;
    janela.showModal();
    return () => {
      janela.close();
      if (focoAnterior instanceof HTMLElement && focoAnterior.isConnected) focoAnterior.focus();
    };
  }, []);

  return (
    <dialog ref={referencia} className="modal" aria-labelledby={tituloId} onCancel={(evento) => {
      evento.preventDefault();
      if (!ocupado) aoFechar();
    }}>
      <div className="modal-cabecalho">
        <h2 id={tituloId}>{titulo}</h2>
        <button className="botao-secundario modal-fechar" type="button" aria-label="Fechar janela"
          onClick={aoFechar} disabled={ocupado}><Icone nome="fechar" /></button>
      </div>
      {children}
    </dialog>
  );
}

function normalizarBusca(texto) {
  return texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('pt-BR');
}

export function Selecao({ rotulo, opcoes, nome, valor, valorInicial = '', aoSelecionar, obrigatorio = false,
  pesquisar, multipla = false,
  textoInicial = 'Selecione' }) {
  const [busca, setBusca] = useState('');
  const [valorInterno, setValorInterno] = useState(multipla ? [] : valorInicial);
  const [aberta, setAberta] = useState(false);
  const botao = useRef(null);
  const caixa = useRef(null);
  const id = useId();
  const atual = valor ?? valorInterno;
  const termo = normalizarBusca(busca.trim());
  const opcoesVisiveis = opcoes.filter(([, texto]) =>
    normalizarBusca(texto).includes(termo)
    || (/^[\d.\-\s]+$/.test(termo) && texto.replace(/\D/g, '').includes(termo.replace(/\D/g, ''))));

  useEffect(() => {
    if (!aberta) return;
    function fecharAoClicarFora(evento) {
      if (!caixa.current.contains(evento.target)) setAberta(false);
    }
    document.addEventListener('pointerdown', fecharAoClicarFora);
    return () => document.removeEventListener('pointerdown', fecharAoClicarFora);
  }, [aberta]);

  function selecionar(novoValor) {
    if (multipla) {
      const selecionados = atual.includes(novoValor)
        ? atual.filter((item) => item !== novoValor) : [...atual, novoValor];
      setValorInterno(selecionados);
      aoSelecionar?.(selecionados);
      return;
    }
    setValorInterno(novoValor);
    aoSelecionar?.(novoValor);
    setAberta(false);
    botao.current.focus();
  }

  return (
    <div ref={caixa} className="selecao" onBlur={(evento) => {
      if (evento.relatedTarget && !evento.currentTarget.contains(evento.relatedTarget)) setAberta(false);
    }} onKeyDown={(evento) => {
      if (aberta && evento.key === 'Escape') {
        evento.preventDefault();
        evento.stopPropagation();
        setAberta(false);
        botao.current.focus();
      }
    }}>
      <label htmlFor={id}>{rotulo}</label>
      <button className="selecao-gatilho" id={id} ref={botao} type="button" aria-expanded={aberta}
        aria-controls={aberta ? id + '-opcoes' : undefined} onClick={() => {
          botao.current.focus();
          setBusca('');
          setAberta(!aberta);
        }}>
        <span>{multipla
          ? (atual.length ? atual.length + (atual.length === 1 ? ' peça selecionada' : ' peças selecionadas') : textoInicial)
          : (opcoes.find(([chave]) => chave === atual)?.[1] ?? textoInicial)}</span>
        <Icone nome="seta" />
      </button>
      {nome && multipla && atual.map((chave) => <input key={chave} type="hidden" name={nome} value={chave} />)}
      {nome && !multipla && <input type="hidden" name={nome} value={atual} data-obrigatorio={obrigatorio || undefined}
        data-rotulo={rotulo} />}
      {aberta && (
        <div id={id + '-opcoes'} className="selecao-opcoes" role="group" aria-label={rotulo}>
          {pesquisar && (
            <div className="selecao-busca">
              <input type="search" aria-label={pesquisar} placeholder={pesquisar} value={busca}
                onChange={(evento) => setBusca(evento.target.value)}
                onKeyDown={(evento) => {
                  if (evento.key === 'Enter') evento.preventDefault();
                }} />
            </div>
          )}
          {opcoesVisiveis.length === 0 && <p className="selecao-vazia" role="status">Nenhum resultado encontrado.</p>}
          {opcoesVisiveis.map(([chave, texto]) => (
            <button key={chave} type="button" aria-pressed={multipla ? atual.includes(chave) : atual === chave}
              onClick={() => selecionar(chave)}>
              {multipla && <span aria-hidden="true">{atual.includes(chave) ? '✓ ' : '+ '}</span>}
              {texto}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export function validarCampos(formulario) {
  for (const campo of formulario.elements) {
    if (campo.dataset.obrigatorio && !campo.value) {
      campo.parentElement.querySelector('button').focus();
      return 'Selecione uma opção em “' + campo.dataset.rotulo + '”.';
    }
    if (!campo.willValidate || campo.validity.valid) continue;

    campo.focus();
    const rotulo = campo.labels?.[0]?.textContent.trim() ?? 'este campo';
    if (campo.validity.valueMissing) return 'Preencha o campo “' + rotulo + '”.';
    if (campo.validity.typeMismatch && campo.type === 'email') return 'Informe um e-mail válido.';
    if (campo.name === 'cpf' && campo.validity.patternMismatch) {
      return 'Digite os 11 números do CPF, com ou sem pontuação.';
    }
    if (campo.validity.rangeUnderflow) return 'O campo “' + rotulo + '” deve ser de pelo menos ' + campo.min + '.';
    if (campo.validity.rangeOverflow) return 'O campo “' + rotulo + '” aceita até ' + campo.max + '.';
    return 'Confira o valor do campo “' + rotulo + '”.';
  }
  return '';
}
