import { useContext, useState } from 'react';
import { ContextoDeAutenticacao } from '../auth/AuthContext.jsx';
import Catalogo from '../../shared/Catalogo.jsx';
import { Selecao } from '../../shared/Interface.jsx';
import AcoesDaPeca from './AcoesDaPeca.jsx';

const tipos = { LOCACAO: 'Locação', SOB_MEDIDA: 'Sob medida' };
const estados = {
  DISPONIVEL: 'Disponível',
  ALUGADA: 'Alugada',
  EM_COSTURA: 'Em costura',
  EM_LAVAGEM: 'Em lavagem',
  MANUTENCAO: 'Manutenção',
};
const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export default function PecasPage({ titulo }) {
  const { usuario } = useContext(ContextoDeAutenticacao);
  const [tipo, setTipo] = useState('');
  const [estado, setEstado] = useState('');

  return (
    <Catalogo titulo={titulo} descricao="Consulte e cadastre as peças do ateliê."
      caminho="/pecas" rotuloCadastro="Nova peça" rotuloBusca="Buscar por descrição ou etiqueta"
      cadastroDesabilitado={usuario.perfil === 'ROLE_PRODUCAO'}
      correspondeABusca={(peca, busca) => {
        const termo = busca.toLocaleLowerCase('pt-BR');
        return (!tipo || peca.tipo === tipo) && (!estado || peca.status === estado)
          && [peca.descricao, peca.codigoEtiqueta].some((valor) => valor?.toLocaleLowerCase('pt-BR').includes(termo));
      }}
      filtros={
        <>
          <Selecao rotulo="Tipo" valor={tipo} aoSelecionar={setTipo}
            opcoes={[[ '', 'Todos os tipos'], ...Object.entries(tipos)]} />
          <Selecao rotulo="Estado" valor={estado} aoSelecionar={setEstado}
            opcoes={[[ '', 'Todos os estados'], ...Object.entries(estados)]} />
        </>
      }
      campos={
        <>
          <label>Descrição<input name="descricao" maxLength={255} required /></label>
          <Selecao rotulo="Tipo" nome="tipo" valorInicial="LOCACAO" opcoes={Object.entries(tipos)} />
          <label>Tamanho<input name="tamanho" maxLength={10} /></label>
          <label>Cor<input name="cor" maxLength={50} /></label>
          <label>Valor base (R$)<input name="valorBase" type="number" inputMode="decimal"
            min="0" max="99999999.99" step="0.01" required /></label>
        </>
      }
      montarDados={(formulario) => ({
        descricao: formulario.get('descricao').trim(),
        tipo: formulario.get('tipo'),
        tamanho: formulario.get('tamanho').trim() || null,
        cor: formulario.get('cor').trim() || null,
        valorBase: Number(formulario.get('valorBase')),
      })}
      aoCadastrar={() => {
        setTipo('');
        setEstado('');
      }}
      tituloDoItem={(peca) => peca.descricao}
      acoesDoItem={(peca, aoAtualizar) => <AcoesDaPeca peca={peca} aoAtualizar={aoAtualizar} />}
      detalhesDoItem={(peca) => [
        ['Etiqueta', peca.codigoEtiqueta],
        ['Tipo', tipos[peca.tipo] || peca.tipo],
        ['Estado', estados[peca.status] || peca.status],
        ['Tamanho', peca.tamanho],
        ['Cor', peca.cor],
        ['Valor base', peca.valorBase == null ? null : moeda.format(peca.valorBase)],
      ]}
    />
  );
}
