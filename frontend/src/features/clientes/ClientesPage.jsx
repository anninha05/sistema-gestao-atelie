import { useState } from 'react';
import { Link } from 'react-router-dom';
import Catalogo from '../../shared/Catalogo.jsx';
import { api } from '../../shared/api.js';
import { Aviso, Modal } from '../../shared/Interface.jsx';

const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
const estados = { RESERVADA: 'Reservada', CONFECCAO: 'Confecção', PROVA: 'Prova', EM_PROVA: 'Em prova',
  AJUSTES: 'Ajustes', PASSADORIA: 'Passadoria', CONCLUIDO: 'Concluída', RETIRADA: 'Retirada',
  DEVOLVIDA: 'Devolvida', ENTREGUE: 'Entregue', CANCELADA: 'Cancelada' };

function HistoricoCliente({ cliente }) {
  const [aberto, setAberto] = useState(false);
  const [ordens, setOrdens] = useState(null);
  const [erro, setErro] = useState(false);
  async function carregar() {
    setAberto(true); setErro(false); setOrdens(null);
    try { const { data } = await api.get('/clientes/' + cliente.id + '/ordens-servico'); setOrdens(data); }
    catch { setErro(true); }
  }
  return <>
    <button type="button" className="botao-secundario" onClick={carregar}>Histórico de ordens</button>
    {aberto && <Modal titulo={'Ordens de ' + cliente.nome} aoFechar={() => setAberto(false)}>
      {erro && <Aviso>Não foi possível consultar o histórico deste cliente.</Aviso>}
      {!ordens && !erro && <p role="status">Consultando histórico…</p>}
      {ordens && ordens.length === 0 && <p>Este cliente ainda não possui ordens de serviço.</p>}
      {ordens?.length > 0 && <ul className="catalogo-lista" role="list">{ordens.map((ordem) => <li key={ordem.id}>
        <h3>{ordem.tipoFluxo === 'LOCACAO' ? 'Locação' : 'Sob medida'} · {estados[ordem.statusAtual] || ordem.statusAtual}</h3>
        <p>Ordem {ordem.id}</p>
        <p>{ordem.inicioLocacao ? dataHora.format(new Date(ordem.inicioLocacao)) : ordem.dataEntrega ? dataHora.format(new Date(ordem.dataEntrega)) : 'Sem data informada'}</p>
        <Link to={'/ordens-servico#' + ordem.id} onClick={() => setAberto(false)}>Abrir na lista de ordens</Link>
      </li>)}</ul>}
    </Modal>}
  </>;
}

function correspondeABusca(cliente, busca) {
  const termo = busca.toLocaleLowerCase('pt-BR');
  const digitos = busca.replace(/\D/g, '');
  return [cliente.nome, cliente.email].some((valor) => valor.toLocaleLowerCase('pt-BR').includes(termo))
    || (/^[\d.\-\s]+$/.test(busca) && digitos && cliente.cpf.replace(/\D/g, '').includes(digitos));
}

export default function ClientesPage({ titulo }) {
  return (
    <Catalogo titulo={titulo} descricao="Consulte e cadastre os contatos do ateliê."
      caminho="/clientes" rotuloCadastro="Novo cliente" rotuloBusca="Buscar por nome, CPF ou e-mail"
      correspondeABusca={correspondeABusca}
      campos={
        <>
          <label>Nome<input name="nome" autoComplete="name" maxLength={255} required /></label>
          <label>CPF<input name="cpf" inputMode="numeric" placeholder="000.000.000-00"
            pattern={'[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2}'}
            title="Digite os 11 números do CPF, com ou sem pontuação." maxLength={14} required /></label>
          <label>Telefone<input name="telefone" type="tel" autoComplete="tel" maxLength={20} required /></label>
          <label>E-mail<input name="email" type="email" autoComplete="email" maxLength={100} required /></label>
        </>
      }
      montarDados={(data) => ({
        nome: data.get('nome').trim(),
        cpf: data.get('cpf').trim(),
        telefone: data.get('telefone').trim(),
        email: data.get('email').trim(),
      })}
      camposEdicao={(cliente) => <>
        <label>Nome<input name="nome" defaultValue={cliente.nome} maxLength={255} required /></label>
        <label>CPF<input name="cpf" defaultValue={cliente.cpf} inputMode="numeric" maxLength={14} required /></label>
        <label>Telefone<input name="telefone" defaultValue={cliente.telefone} type="tel" maxLength={20} required /></label>
        <label>E-mail<input name="email" defaultValue={cliente.email} type="email" maxLength={100} required /></label>
      </>}
      montarDadosEdicao={(data) => ({
        nome: data.get('nome').trim(), cpf: data.get('cpf').trim(),
        telefone: data.get('telefone').trim(), email: data.get('email').trim(),
      })}
      acoesDoItem={(cliente) => <HistoricoCliente cliente={cliente} />}
      tituloDoItem={(cliente) => cliente.nome}
      detalhesDoItem={(cliente) => [
        ['CPF', cliente.cpf.replace(/^(\d{3})(\d{3})(\d{3})(\d{2})$/, '$1.$2.$3-$4')],
        ['Telefone', cliente.telefone],
        ['E-mail', cliente.email],
      ]}
    />
  );
}
