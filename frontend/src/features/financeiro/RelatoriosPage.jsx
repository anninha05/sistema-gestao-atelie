import { useState } from 'react';
import Catalogo from '../../shared/Catalogo.jsx';
import { Aviso, Modal, Selecao, validarCampos } from '../../shared/Interface.jsx';
import { requisitarComCsrf } from '../../shared/api.js';

const caminhoDespesas = '/financeiro/despesas';
const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const metodos = {
  PIX: 'Pix',
  CARTAO: 'Cartão',
  DINHEIRO: 'Dinheiro',
  TRANSFERENCIA: 'Transferência',
};

function formatarData(dataIso) {
  return dataIso ? dataIso.split('-').reverse().join('/') : null;
}

function BaixaDespesa({ despesa, aoAtualizar }) {
  const [aberta, setAberta] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');

  async function salvar(evento) {
    evento.preventDefault();
    const mensagem = validarCampos(evento.currentTarget);
    if (mensagem) {
      setErro(mensagem);
      return;
    }
    const formulario = new FormData(evento.currentTarget);
    setSalvando(true);
    setErro('');

    try {
      const { data } = await requisitarComCsrf('patch', caminhoDespesas + '/' + despesa.id + '/baixa', {
        data: formulario.get('data'),
        metodo: formulario.get('metodo'),
      });
      aoAtualizar(data);
    } catch (falha) {
      setErro(falha.response?.data?.mensagem || 'Não foi possível registrar o pagamento.');
    } finally {
      setSalvando(false);
    }
  }

  return (
    <div className="despesa-baixa">
      <button type="button" aria-haspopup="dialog"
        disabled={salvando} onClick={() => {
          setAberta(true);
          setErro('');
        }}>
        Dar baixa
      </button>
      {aberta && (
        <Modal titulo="Dar baixa na despesa" aoFechar={() => setAberta(false)} ocupado={salvando}>
          <form className="catalogo-form" onSubmit={salvar} aria-busy={salvando} noValidate>
            {erro && <Aviso>{erro}</Aviso>}
            <fieldset disabled={salvando}>
              <label>Data do pagamento<input type="date" name="data" required /></label>
              <Selecao rotulo="Método de pagamento" nome="metodo" obrigatorio opcoes={Object.entries(metodos)} />
            </fieldset>
            <div className="modal-acoes">
              <button className="botao-secundario" type="button" onClick={() => setAberta(false)} disabled={salvando}>
                Cancelar
              </button>
              <button type="submit" disabled={salvando}>{salvando ? 'Registrando…' : 'Confirmar pagamento'}</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}

export default function RelatoriosPage({ titulo }) {
  const [situacao, setSituacao] = useState('');

  return (
    <Catalogo titulo={titulo} descricao="Cadastre despesas e acompanhe os pagamentos do ateliê."
      caminho={caminhoDespesas} rotuloCadastro="Nova despesa" rotuloBusca="Buscar por descrição ou categoria"
      correspondeABusca={(despesa, busca) => {
        const paga = Boolean(despesa.dataPagamento);
        const termo = busca.toLocaleLowerCase('pt-BR');
        return (!situacao || paga === (situacao === 'paga'))
          && [despesa.descricao, despesa.categoria].some((valor) => valor.toLocaleLowerCase('pt-BR').includes(termo));
      }}
      filtros={
        <Selecao rotulo="Situação" valor={situacao} aoSelecionar={setSituacao}
          opcoes={[[ '', 'Todas as despesas'], ['aberta', 'Em aberto'], ['paga', 'Pagas']]} />
      }
      campos={
        <>
          <label>Descrição<input name="descricao" maxLength={255} required /></label>
          <label>Categoria<input name="categoria" maxLength={100} required /></label>
          <label>Valor (R$)<input name="valor" type="number" inputMode="decimal" min="0.01"
            max="99999999.99" step="0.01" required /></label>
          <label>Vencimento<input name="vencimento" type="date" required /></label>
        </>
      }
      montarDados={(formulario) => ({
        descricao: formulario.get('descricao').trim(),
        categoria: formulario.get('categoria').trim(),
        valor: Number(formulario.get('valor')),
        vencimento: formulario.get('vencimento'),
      })}
      aoCadastrar={() => setSituacao('')}
      tituloDoItem={(despesa) => despesa.descricao}
      detalhesDoItem={(despesa) => [
        ['Categoria', despesa.categoria],
        ['Valor', moeda.format(despesa.valor)],
        ['Vencimento', formatarData(despesa.vencimento)],
        ['Situação', despesa.dataPagamento ? 'Paga' : 'Em aberto'],
        ...(despesa.dataPagamento ? [
          ['Pagamento', formatarData(despesa.dataPagamento)],
          ['Método', metodos[despesa.metodo] ?? despesa.metodo],
        ] : []),
      ]}
      acoesDoItem={(despesa, aoAtualizar) => !despesa.dataPagamento && (
        <BaixaDespesa despesa={despesa} aoAtualizar={(atualizada) => {
          aoAtualizar(atualizada);
          setSituacao('');
        }} />
      )}
    />
  );
}
