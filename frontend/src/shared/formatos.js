export const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
export const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
export const tiposFluxo = { LOCACAO: 'Locação', SOB_MEDIDA: 'Sob medida' };

export const estadosOrdem = {
  PENDENTE: ['Pendente', 'amarelo'],
  RESERVADA: ['Reservada', 'amarelo'],
  CONFECCAO: ['Confecção', 'azul'],
  PROVA: ['Prova', 'azul'],
  EM_PROVA: ['Em prova', 'azul'],
  AJUSTES: ['Ajustes', 'roxo'],
  PASSADORIA: ['Passadoria', 'roxo'],
  CONCLUIDO: ['Concluída', 'verde'],
  RETIRADA: ['Retirada', 'rosa'],
  ENTREGUE: ['Entregue', 'neutro'],
  DEVOLVIDA: ['Devolvida', 'neutro'],
  CANCELADA: ['Cancelada', 'vermelho'],
};

export function estadoDaOrdem(status) {
  return estadosOrdem[status] ?? [status, 'neutro'];
}
