import Catalogo from '../../shared/Catalogo.jsx';
import { Selecao } from '../../shared/Interface.jsx';

const perfis = [['ROLE_ADMIN', 'Administração'], ['ROLE_ATENDENTE', 'Atendimento'], ['ROLE_PRODUCAO', 'Produção']];

export default function UsuariosPage() {
  return <Catalogo titulo="Usuários" descricao="Cadastre as pessoas que terão acesso ao ateliê."
    caminho="/usuarios" rotuloCadastro="Novo usuário" rotuloBusca="Buscar por nome ou e-mail"
    correspondeABusca={(usuario, busca) => [usuario.nome, usuario.email].some((valor) =>
      valor.toLocaleLowerCase('pt-BR').includes(busca.toLocaleLowerCase('pt-BR')))}
    campos={<>
      <label>Nome<input name="nome" maxLength={255} required /></label>
      <label>E-mail<input name="email" type="email" maxLength={254} required /></label>
      <label>Senha inicial<input name="senha" type="password" autoComplete="new-password" minLength={8} required /></label>
      <Selecao rotulo="Perfil" nome="perfil" obrigatorio opcoes={perfis} />
    </>}
    montarDados={(formulario) => ({
      nome: formulario.get('nome').trim(), email: formulario.get('email').trim(),
      senha: formulario.get('senha'), perfil: formulario.get('perfil'),
    })}
    tituloDoItem={(usuario) => usuario.nome}
    detalhesDoItem={(usuario) => [['E-mail', usuario.email], ['Perfil', perfis.find(([valor]) => valor === usuario.perfil)?.[1]]]}
  />;
}
