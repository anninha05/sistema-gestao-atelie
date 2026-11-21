export const rotasDasAcoes = {
  INICIAR_CONFECCAO: 'confeccao',
  ENVIAR_PARA_PROVA: 'prova',
  INICIAR_PASSADORIA: 'passadoria',
  CONCLUIR: 'concluir',
  RETIRAR: 'retirar',
  DEVOLVER: 'devolver',
  CANCELAR: 'cancelar',
};

export function oferece(ordem, acao) {
  return ordem.acoes?.some((item) => item.acao === acao) ?? false;
}

export function acoesDiretas(ordem) {
  return (ordem.acoes ?? []).filter((item) => item.acao in rotasDasAcoes);
}
