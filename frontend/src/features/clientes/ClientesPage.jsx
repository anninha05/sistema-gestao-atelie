import Catalogo from '../../shared/Catalogo.jsx';

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
      tituloDoItem={(cliente) => cliente.nome}
      detalhesDoItem={(cliente) => [
        ['CPF', cliente.cpf.replace(/^(\d{3})(\d{3})(\d{3})(\d{2})$/, '$1.$2.$3-$4')],
        ['Telefone', cliente.telefone],
        ['E-mail', cliente.email],
      ]}
    />
  );
}
