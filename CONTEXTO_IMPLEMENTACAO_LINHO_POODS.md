# CONTEXTO DE IMPLEMENTAÇÃO — LINHO / POODS

> **Documento de referência para um agente de programação**  
> Projeto: **Linho - Sistema de Gestão do Ateliê**  
> Base inspecionada: `sistema-gestao-atelie-main.zip`, snapshot dos arquivos de **10/11/2026**.  
> Objetivo: concluir um **MVP web, seguro, orientado a objetos, integrado e fácil de usar**, respeitando o fluxo real do Atelier e o módulo de IA/PID solicitado na disciplina.

## 0. INSTRUÇÕES OBRIGATÓRIAS PARA O AGENTE QUE LER ESTE ARQUIVO

1. **Leia este documento e inspecione o código real antes de editar.** Este documento expressa o **comportamento-alvo**, não significa que tudo já esteja implementado.
2. **Não recomece o projeto, não substitua a stack e não faça uma reescrita geral.** Preserve a estrutura existente e refatore incrementalmente.
3. **Implemente uma tarefa por vez**, começando pela integridade do domínio, com teste de sucesso e teste de falha. Após cada tarefa, informe arquivos alterados, regras atendidas, testes executados e pendências.
4. **Fonte da verdade:** regras de negócio pertencem ao backend; o React apresenta dados e permite ações, mas não decide se uma OS pode ser retirada, uma peça reservada ou um preço aprovado.
5. **Não invente regras comerciais.** Quando houver decisão pendente (taxa de cancelamento, desconto, custo, sinal, tempo de lavagem etc.), implemente de modo configurável ou use a política provisória explicitamente identificada neste documento; sinalize para validação com o Atelier.
6. **Não force conceitos de OO**: use encapsulamento, entidades coesas, objetos de valor, serviços de domínio e o padrão State onde já existe; **não crie heranças ou interfaces artificiais** só para demonstrar polimorfismo.
7. **Não adicione complexidade sem necessidade**: evite microsserviços, filas, integrações externas, predições treinadas fictícias e bibliotecas novas sem justificativa.
8. **Mantenha compatibilidade com banco e dados existentes**: novas mudanças de esquema devem ir para novas migrações Flyway (`V4__...`, `V5__...` etc.), sem reescrever V1–V3.
9. **Nunca contorne autenticação, CSRF ou regras do domínio** para facilitar uma tela ou teste.
10. **Não declare uma tarefa pronta sem testá-la**. Se não houver banco/ambiente disponível, marque como "implementada, ainda não validada", em vez de "concluída".
11. Responda e documente em **português brasileiro claro e objetivo**. Nomes de classes e endpoints devem seguir as convenções atuais do repositório.
12. **Prioridade:** consistência das reservas, do ciclo de vida da OS, do pagamento, do estoque e do acesso aos dados > refinamento visual > recursos avançados.

---

## 1. PROPÓSITO DO SISTEMA

O Atelier Lucinéia Padilha atende locações de roupas e acessórios de festa e encomendas sob medida (vestidos, ternos e peças relacionadas). A operação envolve atendimento, escolha da peça, reserva, prova de roupa, ajustes, passadoria, retirada ou entrega, devolução, lavagem, pagamentos e acompanhamento administrativo.

### Problemas levantados no projeto

- Uma mesma peça pode acabar sendo prometida a dois clientes na mesma data quando a disponibilidade é controlada manualmente.
- As medidas e instruções de costura anotadas em papel podem se perder ou ficar desatualizadas.
- O controle do sinal, das parcelas e do saldo restante é pouco centralizado.
- A equipe precisa saber **quem vem hoje** para prova, retirada, entrega ou devolução.
- O sistema legado/auxiliares de papel exigem trabalho duplicado e dificultam o uso cotidiano.
- Cada setor deve visualizar somente o necessário para suas funções.

### Princípio de produto

> **A Ordem de Serviço (OS) é o centro da contratação.** A partir dela deve ser possível compreender a cliente, o que foi contratado, as peças, as datas, o estado da produção, o valor negociado, os recebimentos, o saldo, o documento e a próxima ação.

**Critério de sucesso do MVP:** uma atendente consegue concluir uma locação do cadastro à lavagem sem manipular manualmente estados contraditórios; a produção acompanha provas/ajustes; a gerência acompanha pagamentos/resultado financeiro e recebe uma sugestão explicável do PID.

### Escopo mínimo do relatório inicial consolidado

- **RF01:** cadastro, consulta, edição e histórico de clientes.
- **RF02:** criação, consulta, atualização/reagendamento e acompanhamento das OS, com disponibilidade por período.
- **RF03:** visão diária de provas, retiradas, entregas e devoluções.
- **RF04:** fila digital da produção, medidas, ajustes e etapas.
- **RF05:** total, sinal, recebimentos, saldo e visão financeira.
- **RF06:** contrato de locação e recibos em PDF.
- **RNF:** aplicação web, interface simples e responsiva, suporte básico a PWA, arquitetura OO organizada, armazenamento PostgreSQL e segurança por perfil.
- **Exigência acadêmica adicional:** agente de precificação com **controlador PID implementado manualmente**, sem biblioteca pronta que realize os cálculos do PID.

> **Limite documental:** a íntegra do primeiro relatório não estava anexada junto ao ZIP nesta revisão. Os itens acima reúnem os requisitos consolidados nas discussões do projeto e devem ser conferidos contra o relatório original caso ele traga alguma regra mais específica.

### Fora do MVP, salvo exigência explícita posterior

Machine learning preditivo treinado, envio de WhatsApp, pagamentos por gateway, cadastro avançado de fornecedores, emissão fiscal, integração com ERP, sincronização offline com conflitos, motor complexo de precificação, controle granular de cada tarefa de cada costureira e dashboards sofisticados.

---

## 2. TECNOLOGIAS E ORGANIZAÇÃO DO REPOSITÓRIO (CONFIRMADAS NO ZIP)

- **Backend:** Java 21, Spring Boot, Spring MVC, Spring Security, Spring Data JPA/Hibernate, Flyway, Maven.
- **Banco:** PostgreSQL (imagem `postgres:15-alpine` no `docker-compose.yml`).
- **Frontend:** React 18, Vite 5, React Router 6, Axios, `vite-plugin-pwa`.
- **Infraestrutura:** Dockerfile no backend e no frontend; Compose atualmente inicia **apenas o banco**, não toda a aplicação.
- **Autenticação:** formulário de login por sessão Spring Security, senha BCrypt e token CSRF; não é JWT.
- **Versão do projeto:** usar as versões efetivamente presentes no `pom.xml` e `package.json`; não atualizar dependências por iniciativa própria durante a correção funcional.

```text
sistema-gestao-atelie-main/
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/atelie/sistema/
│       │   ├── cliente/                # Cliente, Service, Controller, DTOs
│       │   ├── peca/                   # Peça, catálogo, estados e disponibilidade
│       │   │   └── state/
│       │   ├── ordemservico/           # OS, Item, Pagamento, Recebimento, DTOs
│       │   │   └── state/
│       │   ├── financeiro/             # Despesa
│       │   ├── usuario/                # Login, perfil, usuários
│       │   └── shared/                 # Segurança, validação e exceções
│       └── resources/
│           ├── application.properties
│           └── db/migration/
│               ├── V1__criar_cadastros.sql
│               ├── V2__criar_ordens_servico.sql
│               └── V3__criar_financeiro.sql
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── app/                        # Rotas, layout
│       ├── features/
│       │   ├── auth/                 # Login/sessão
│       │   ├── clientes/
│       │   ├── pecas/
│       │   ├── ordens-servico/
│       │   ├── dashboard/
│       │   ├── financeiro/
│       │   └── usuarios/
│       └── shared/                     # api.js, Catalogo.jsx, componentes
├── docker-compose.yml
├── .env.example
└── README.md                         # Está vazio no snapshot
```

### Estado atual confirmado (não confundir com objetivo)

**Atualização de 17/11/2026 — T01–T11 implementadas e validadas no código atual:** retirada/devolução sincronizam OS e peças transacionalmente; disponibilidade, reagendamento e cancelamento preservam reservas e recebimentos. Clientes e peças podem ser editados, a ficha do cliente exibe o histórico de OS e cada peça da ordem pode ter medidas/instruções próprias. A produção consulta e altera a fila usando respostas técnicas sem valores financeiros, inclusive nas respostas das mutações. O dashboard consulta compromissos do dia no backend; contrato e recibo são gerados em PDF com dados persistidos; o consolidado apresenta recebimentos, despesas pagas, resultado de caixa e saldo a receber. A V5 adiciona medidas/instruções em `ordens_servico_pecas` e migra para os itens os dados anteriormente gerais na OS. A suíte completa executou 79 testes sem falhas, e o build do frontend passou. Pendências comerciais: não há registro de estorno, então recebimentos de ordens canceladas continuam no caixa; o PDF é um resumo informativo sem cláusulas, que ainda precisam ser fornecidas pelo Atelier. A tabela abaixo preserva o diagnóstico original do ZIP.

| Área | Existe no ZIP | Lacunas importantes |
|---|---|---|
| Clientes | Cadastro e consulta, validação de CPF | Edição e histórico acessível |
| Peças | Cadastro/listagem, tipo, estado físico e preço-base | Edição; integrar operações com OS |
| OS | Criação, consulta, itens, estados, datas, verificação de conflito | Reagendar, sincronizar estados, cancelamento financeiro |
| Disponibilidade | Query de sobreposição e bloqueio pessimista ao criar OS | Estado físico/manutenção e reagendamento transacional |
| Pagamento | Sinal, recebimentos, valor pago/saldo, histórico | Bloqueio de retirada, cancelamento/estornos |
| Produção | Fila por OS e etapas de State | Ações coerentes por tipo; ajustes por peça quando necessário |
| Financeiro | Cadastro/baixa de despesas; dados de recebimento | Consolidado mensal e lucro verdadeiro |
| Documentos | Não foi identificada geração de PDF | Contrato e recibo |
| Segurança | Sessão, BCrypt, CSRF, rotas por perfil | Evitar vazamento de campos financeiros na produção; auditoria |
| PWA/Docker | Plugin PWA e Dockerfiles; PostgreSQL via Compose | Validar deploy, HTTPS, API/proxy e app responsiva |
| PID | **Não identificado no ZIP** | Criar algoritmo, módulo, integração e testes |

### Detalhes atuais importantes

- `OrdemServico` é entidade JPA; `ItemOrdemServico` é um **`record @Embeddable`** com `pecaId`, valor contratado e, após V5, medidas/instruções específicas, armazenado em `ordens_servico_pecas`. Não é uma entidade independente.
- `OrdemServico` tem `@Version`, assim como `Pagamento` e `Despesa`; `PecaRepository.buscarParaReserva()` utiliza `PESSIMISTIC_WRITE`.
- `OrdemServicoService.abrirOrdemServico()` bloqueia e ordena IDs das peças, verifica reservas conflitantes para `LOCACAO`, preserva valor unitário no item e calcula desconto.
- No snapshot, `OrdemServicoService.retirar()` e `devolver()` mudavam apenas a OS. Após T01/T02, ambas sincronizam as peças em transação, usando bloqueio pessimista na OS e nas peças; a retirada exige quitação e a devolução encaminha para lavagem.
- `PecaService` ainda oferece a ação independente de alugar (revisão geral na T04). Como dependência da T02, uma peça `ALUGADA` não pode ir diretamente para lavagem nem voltar ao estoque por esse serviço: a devolução deve ocorrer pela OS. As mutações físicas usam o bloqueio existente para evitar sobrescritas concorrentes.
- `OrdemServicoRepository.contarReservas()` considera sobreposição semiaberta e exclui OS `DEVOLVIDA`/`CANCELADA`. A prontidão física é validada no início imediato e novamente na retirada. Não existe duração de lavagem/preparo configurada; portanto, o sistema não inventa janela entre reservas futuras.
- `Pagamento.registrarRecebimento()` registra valor, data, método e sinal; o total é calculado separadamente da OS e deve permanecer coerente com ela.
- A produção consulta `/ordens-servico/producao` e `/pecas/producao`, usando DTOs técnicos; as rotas gerais de OS/peças são bloqueadas para esse perfil. As ações de produção também retornam DTOs sem valores financeiros.
- `application.properties` tem `server.port=8080`, enquanto o proxy de `vite.config.js` aponta `/api` para `127.0.0.1:8081`; `frontend/src/shared/api.js` usa `http://localhost:8080` como padrão. **Padronizar sem mudar aleatoriamente o servidor**.

---

## 3. PERFIS E AUTORIZAÇÃO

Os perfis **reais** do projeto são:

| Perfil (enum existente) | Atribuição | Operações previstas |
|---|---|---|
| `ROLE_ADMIN` | Gerência/administração | Gestão geral, financeiro, metas/PID, usuários, relatórios e aprovações |
| `ROLE_ATENDENTE` | Recepção/atendimento | Clientes, peças e OS; pagamentos, documentos, retirada/devolução |
| `ROLE_PRODUCAO` | Costura/produção | Consultar fila, dados necessários das peças e OS, avançar etapas de produção |

**Regra:** o frontend pode ocultar seções por perfil para facilitar o uso, mas **cada endpoint** deve autorizar no backend. A equipe de produção **não deve receber** preço, desconto, saldo, CPF completo ou outras informações desnecessárias em seu DTO.

**Atenção:** o modelo atual usa `Usuario` com `Perfil` enum, **não** subclasses reais `Atendente`, `Costureira` e `Gerente`. **Não adicionar herança apenas para satisfazer POO.** O polimorfismo já aparece nos estados de OS e de peça via interfaces do padrão State.

### Matriz funcional alvo

| Recurso | ADMIN | ATENDENTE | PRODUCAO |
|---|:---:|:---:|:---:|
| Clientes: criar/editar/consultar | Sim | Sim | Não |
| Peças: cadastrar/editar | Sim | Sim | Consultar dados técnicos |
| Criar/reagendar/cancelar OS | Sim | Sim | Não |
| Ver fila, medidas e instruções | Sim | Conforme necessidade | Sim |
| Avançar prova/ajuste/passadoria | Sim | Não, salvo regra validada | Sim |
| Registrar pagamentos | Sim | Sim | Não |
| Retirada, entrega e devolução | Sim | Sim | Não |
| Financeiro geral e despesas | Sim | Não | Não |
| Meta mensal e PID | Sim | Exibir sugestão aprovada, se necessário | Não |
| Gerenciar usuários | Sim | Não | Não |

Decisões de acesso que precisem ser ajustadas devem ser refletidas nos testes. Nunca liberar um endpoint porque o botão não aparece na interface.

---

## 4. CONCEITOS FUNDAMENTAIS DO DOMÍNIO

### Cliente

Pessoa contratante. Nome, telefone, e-mail e CPF; uma cliente pode ter **zero ou muitas OS**. O histórico é obtido das OS relacionadas. Atualização não pode substituir CPF de forma inválida ou duplicada. Não apagar clientes com histórico, preferir inativação quando necessário.

### Peça física

Unidade identificável do inventário (etiqueta única), com descrição, tamanho, cor, valor-base e condição física atual. Uma peça pode participar de várias OS **em períodos distintos**, mas **não** em locações conflitantes. Reservar para o futuro **não significa** marcar imediatamente o estado físico como `ALUGADA`.

### Ordem de Serviço (OS)

Contrato/atendimento que conecta **um cliente**, **um tipo de fluxo**, **um ou mais itens**, **datas**, **produção**, **valor negociado** e **pagamento**. OS pode ser locação ou sob medida; cada tipo tem conclusão diferente.

### Item da OS

Snapshot da peça/modelo/serviço contratado e de seu valor. Mudanças posteriores no preço-base do catálogo **não devem reescrever preços já negociados**. No snapshot existe `ItemOrdemServico(pecaId, valorUnitario)`; ampliar apenas se necessário e com migração.

### Pagamento / Recebimento

`Pagamento` é o resumo financeiro associado à OS (valor total, total pago, saldo e situação). Cada `Recebimento` é o lançamento individual com data, valor, método e indicação de sinal. **Sinal é parte do total pago, não é valor extra a somar outra vez.**

### Produção

Preparação da peça contratada, que pode incluir prova, ajustes e passadoria. No snapshot as etapas pertencem à OS inteira, com medidas e instruções textuais. Para várias peças com ajustes diferentes, o modelo deverá identificar a qual peça cada instrução pertence — sem criar um ERP de fábrica.

### Agenda

Para o MVP, provas, entrega, retirada e devolução podem ser derivadas das datas existentes na OS e apresentadas em um **painel diário**. Não criar uma entidade `Agendamento` sem necessidade concreta de múltiplos compromissos/reagendamentos independentes. Se criada, relacioná-la à OS com regras claras.

### Resultado financeiro

- **Recebimento realizado:** dinheiro efetivamente recebido, **por data de pagamento**.
- **Despesa paga:** saída efetivamente paga, **por data de pagamento**.
- **Resultado de caixa:** recebimentos realizados menos despesas pagas no período.
- **Lucro:** exige custos e critérios de reconhecimento coerentes; **não afirmar que resultado de caixa equivale automaticamente a lucro**.
- **Saldo a receber:** valor contratado ainda não pago; não entra como recebimento realizado.

---

## 5. REGRAS DE NEGÓCIO — INVARIANTES QUE NUNCA PODEM SER VIOLADAS

**RN01 — Reserva exclusiva por período.** Duas locações ativas da mesma peça física não podem ter intervalos sobrepostos.

**RN02 — Intervalo válido.** `inicioLocacao < fimLocacao`. Utilizar a convenção de intervalo semiaberto **`[início, fim)`** para permitir que o fim de uma OS coincida com o início da próxima apenas quando o tempo de preparação exigido também tiver sido respeitado.

**RN03 — Concorrência.** A verificação definitiva da reserva ocorre **dentro da transação de gravação**, com bloqueio/isolamento apropriado, inclusive no reagendamento. Uma consulta feita minutos antes **não garante** a reserva.

**RN04 — Estado físico.** Peça efetivamente retirada para locação passa a `ALUGADA`; peça devolvida segue para `EM_LAVAGEM` (ou conferência/manutenção conforme procedimento); somente volta a `DISPONIVEL` após liberação explícita. Não usar `DISPONIVEL` como indicador único de ausência de reservas futuras.

**RN05 — Manutenção.** Peça fisicamente indisponível para a data prometida não pode ser entregue. Para novas reservas futuras, considerar o período previsto de manutenção e/ou impedir confirmação se a data de liberação não for conhecida. Evitar bloquear indiscriminadamente todo o calendário futuro só porque a peça está em lavagem **hoje**.

**RN06 — Fluxo transacional.** Retirar, devolver, cancelar e reagendar devem sincronizar OS, peças, disponibilidade, pagamentos e registros necessários **em transação única**, sem ficar com metade das mudanças persistida.

**RN07 — Quitação antes da entrega.** Política provisória do projeto: `saldoDevedor == 0` para retirar uma locação ou entregar sob medida. Aplicar no backend antes de concluir a operação. Se o Atelier aprovar exceção, ela deve ser **explícita, autorizada, registrada e testada**, nunca implícita.

**RN08 — Valores positivos e coerentes.** Total contratado > 0; recebimento > 0; recebido acumulado ≤ valor contratado; saldo = total − recebido; não duplicar sinal. Usar `BigDecimal` e arredondamento monetário definido.

**RN09 — Imutabilidade econômica.** Alterar preço do catálogo não altera OS passadas. Alterar preço de OS já negociada exige autorização, registro e recalcular `Pagamento` sem ficar abaixo do já recebido; após entrega, tratar via procedimento financeiro próprio.

**RN10 — Cancelamento.** Cancelar não apaga a OS nem os recebimentos. Deve liberar reserva futura, impedir novas operações incompatíveis e resolver explicitamente valores já recebidos (estorno, retenção autorizada ou pendência de acerto). **Percentual de multa/retensão está indefinido: não inventar.**

**RN11 — Estados válidos.** Uma OS não pode pular etapas obrigatórias, ser retirada duas vezes, devolvida sem retirada ou cancelada após estado terminal sem regra excepcional.

**RN12 — Sob medida não é locação.** Roupa sob medida encomendada não deve exigir devolução nem ocupar calendário de reserva de peça física de locação. Não tratar uma encomenda ainda não confeccionada como unidade pronta em estoque.

**RN13 — Privacidade.** Acesso a cliente, medidas, CPF e informações financeiras é restrito por necessidade do setor; toda operação valida perfil no servidor.

**RN14 — Idempotência e concorrência financeira.** Uma falha de rede e o reenvio da requisição não podem registrar dois recebimentos acidentais; implementar proteção apropriada ou, no mínimo, confirmação/reconciliação e teste de duplicidade. Manter controle de versão/lock.

**RN15 — Auditoria mínima.** Ações relevantes (alteração de preço, cancelamento, aprovação de sugestão, entrega e devolução) devem guardar usuário, horário e referência da OS, se ainda não houver tal registro.

**RN16 — Data e horário.** Definir claramente fuso operacional `America/Sao_Paulo`, regra de comparação e formato entre browser e Java; não depender de conversões implícitas que mudem o dia da prova ou da retirada.

**RN17 — Uma fonte de verdade.** Não duplicar estados de negócio em variáveis do frontend nem manter dois módulos capazes de alterar a mesma realidade sem sincronização. Exemplo: um botão independente de peça **não pode** fazer uma locação parecer devolvida sem atualizar a OS.

---

## 6. FLUXO ALVO DE PONTA A PONTA

### 6.1. Entrar no sistema

1. Usuário acessa página de login.
2. React consulta token CSRF quando necessário, envia credenciais pelo mecanismo Spring Security.
3. Backend valida senha BCrypt, cria sessão e identifica perfil.
4. Frontend redireciona para área compatível com o perfil.
5. Sessão expirada retorna 401, falta de autorização retorna 403; React explica e não mantém ações indevidamente disponíveis.
6. Logout encerra sessão no servidor. **Não armazenar senha em localStorage.**

**Critério de aceite:** usuário de produção não recebe dados financeiros ao consultar a fila, mesmo que chame diretamente a API.

### 6.2. Cadastrar / localizar cliente

1. Atendimento busca por nome ou CPF antes de cadastrar.
2. Cadastra nome, telefone, e-mail e CPF válido; impede CPF duplicado.
3. Permite editar contatos, preservando validação e histórico.
4. Na ficha do cliente, mostra OS passadas e atuais, identificando tipos, datas e estados.
5. Não expõe CPF completo em listagens ou à produção sem necessidade.

**Critério de aceite:** editar telefone não altera OS antigas nem cria duplicidade de cliente.

### 6.3. Cadastrar / manter peça

1. Atendimento registra peça física com etiqueta/código único, descrição, cor, tamanho, preço-base e tipo.
2. Consulta estado físico e reservas por data; **não confundir estoque atual com calendário futuro**.
3. Pode editar dados do catálogo; **não** altera valor de itens de OS já concluídas.
4. Produção pode alterar situação técnica (costura/lavagem) **somente se compatível** com reserva/retirada.
5. Liberação física após limpeza/conferência retorna a `DISPONIVEL`.
6. Retirar de operação sem apagar histórico: usar inativação/bloqueio quando necessário (evolução mínima, se requerida).

**Critério de aceite:** duas peças com mesmo modelo mas etiquetas diferentes podem ser reservadas independentemente.

### 6.4. Criar OS de locação

1. Atendimento seleciona cliente existente.
2. Seleciona tipo `LOCACAO`.
3. Informa início e fim da locação, data do evento, prova/entrega quando aplicável.
4. Escolhe uma ou mais peças físicas (sem repetição).
5. Interface **consulta** disponibilidade e sinaliza conflitos, sem prometer que a reserva já está garantida.
6. Backend, na operação de salvar, bloqueia as peças em **ordem determinística**, reconsulta conflito, valida estado/manutenção aplicável e registra OS + itens com valores congelados.
7. Registra desconto autorizado, valores e próximo passo. OS entra no estado inicial definido e passa a ocupar o calendário.
8. React mostra a OS criada, datas, valor total, saldo e ações possíveis.

**Critério de aceite:** duas reservas simultâneas sobrepostas da mesma etiqueta não podem ser confirmadas.

### 6.5. Organizar prova e ajustes

1. Dados da OS incluem data de prova, medidas e instruções.
2. Dashboard mostra próximos compromissos; produção vê sua fila priorizada por data/prazo.
3. Costureira consulta cliente/peça/medidas/instruções necessárias, sem campo financeiro.
4. Registra etapas: prova → ajustes/confecção quando necessários → passadoria → concluído/pronto.
5. Se houver várias peças e instruções diferentes, identificar instruções por item (mínimo necessário).
6. Estados e botões devem refletir o fluxo real; não exigir provas inexistentes por mecanismo técnico arbitrário.
7. Prova reagendada atualiza os compromissos exibidos no painel.

**Critério de aceite:** produção não consegue marcar como concluída uma OS que ainda tem etapa obrigatória pendente.

### 6.6. Receber sinal / pagamentos

1. Atendimento abre seção financeira da OS.
2. Sistema mostra total contratado, valor recebido, saldo e histórico.
3. Registra sinal ou pagamento parcial: **valor positivo**, data, método (`PIX`, `CARTAO`, `DINHEIRO`, `TRANSFERENCIA`).
4. Backend rejeita soma acima do total e operações indevidas em OS cancelada ou financeiramente encerrada.
5. Recebimento é persistido uma única vez; painel e OS são atualizados após salvar.
6. Sinal entra no valor pago; não é cobrado novamente como adicional.
7. Em caso de erro de rede, frontend consulta situação atual antes de reenviar.

**Critério de aceite:** OS de R$ 600, com sinal R$ 200 e pagamento R$ 300, exibe saldo R$ 100; retirada é bloqueada.

### 6.7. Retirada de locação / entrega sob medida

1. Atendimento abre a OS concluída e pronta.
2. Backend valida: estado permitido, tipo de fluxo, quitação, peça(s) fisicamente apta(s), integridade da reserva e ausência de operação duplicada.
3. Para locação, registra retirada na OS e marca as peças físicas `ALUGADA` na mesma transação.
4. Para sob medida, registra `ENTREGUE` e **não** cria obrigação de devolução.
5. Sistema registra data/usuário e atualiza todas as telas.
6. Quando não permitido, backend devolve erro de negócio claro; frontend explica o motivo e a ação necessária.

**Critério de aceite:** não pode haver OS `RETIRADA` com peça ainda `DISPONIVEL` por falta de sincronização.

### 6.8. Devolução de locação e retorno ao estoque

1. Atendimento identifica OS `RETIRADA`.
2. Registra devolução efetiva, data e eventuais ocorrências (dano/atraso) sem inventar cobrança automática.
3. Na mesma transação, OS vira `DEVOLVIDA`, peças passam a `EM_LAVAGEM`/conferência.
4. Produção conclui lavagem e, se necessário, manutenção; só depois libera a peça para a próxima locação.
5. Reservas futuras já existentes continuam registradas, mas a entrega da próxima OS verifica prontidão real.
6. Não permitir duas devoluções da mesma OS.

**Critério de aceite:** após devolver, a peça **não** aparece como fisicamente pronta até sua liberação.

### 6.9. Cancelar OS

1. Atendimento solicita cancelamento e informa motivo (se definido no MVP).
2. Backend valida etapa atual; não permite cancelamento normal após retirada/entrega/devolução.
3. Verifica pagamentos: se houve recebimento, registrar **pendência de acerto** ou aplicar política expressamente autorizada; não apagar recebimentos.
4. Marca OS cancelada e libera reserva futura, de forma transacional.
5. Mantém histórico e auditoria; frontend apresenta situação final e eventual pendência financeira.

**Critério de aceite:** cancelar OS reservada libera o calendário; cancelar OS com sinal não faz o sinal desaparecer do financeiro.

### 6.10. Reagendar OS

1. Atendimento propõe novo período e/ou data da prova/evento.
2. Backend verifica se a OS aceita mudanças.
3. Bloqueia itens físicos envolvidos, revalida novas datas **desconsiderando a própria OS** na consulta de conflito.
4. Se estiver livre, altera datas e atualiza agenda; se não, preserva integralmente os dados anteriores.
5. Se necessário, sinaliza ajuste de produção e revisão do documento já emitido.

**Critério de aceite:** um reagendamento com conflito não modifica nem perde a reserva anterior.

### 6.11. Encomenda sob medida

1. Atendimento seleciona cliente e tipo `SOB_MEDIDA`.
2. Registra descrição/modelo contratado, medidas, prova(s), prazo de entrega, valor e instruções.
3. O fluxo é: contratação → confecção → prova/ajustes quando necessários → passadoria → pronto → `ENTREGUE`.
4. Pagamentos seguem as mesmas regras financeiras.
5. **Não** reservar intervalo de uso de peça física nem exigir devolução.
6. No snapshot `Peca.tipo = SOB_MEDIDA` é usado como catálogo de modelos; tratá-lo **como referência comercial**, não como roupa pronta identificável para aluguel. Se for necessária separação definitiva, criar um `ServicoCatalogo`/`ModeloCatalogo` em etapa posterior, com migração cuidadosa.

**Critério de aceite:** criar encomenda sob medida não bloqueia a agenda de locação de uma peça física.

### 6.12. Agenda / dashboard diário

1. Mostrar provas, retiradas, entregas e devoluções previstas para a data selecionada (hoje por padrão).
2. Exibir nome do cliente, referência da OS, tipo de compromisso, horário e situação.
3. Ordenar por horário; sinalizar atrasos/pendências sem confundir previsão e execução real.
4. Atualizar após alterações de datas, estados e pagamentos; permitir navegar à OS.
5. O dashboard do atendimento não precisa exibir todo o financeiro geral.

### 6.13. Documentos

1. Para locação, gerar PDF de contrato com identificador da OS, cliente, peças/etiquetas, períodos, valor, condições aprovadas e referência de emissão.
2. Para recebimento, gerar PDF de recibo do **lançamento individual**, contendo valor, data, método, cliente e OS.
3. Documentos devem refletir os dados **confirmados**, não formulário ainda não salvo; não expor PDFs a perfis não autorizados.
4. Se documentos precisarem ser reemitidos após reagendamento, indicar versão/data de emissão; **não inventar cláusulas legais de multa**.

### 6.14. Relatórios financeiros

1. Total de recebimentos realmente efetivados por período.
2. Total de despesas realmente pagas por período.
3. Resultado de caixa do período, saldo total a receber e, se houver custos completos, lucro.
4. Distinguir valores previstos de valores realizados; cancelamentos e estornos devem ser contabilizados corretamente.
5. Somente gerência acessa consolidados e meta/PID.

---

## 7. ESTADOS — MAPEAR O ATUAL SEM QUEBRAR O DOMÍNIO

### 7.1. Estados de OS existentes no código

`PENDENTE`, `RESERVADA`, `CONFECCAO`, `PROVA`, `EM_PROVA`, `AJUSTES`, `PASSADORIA`, `CONCLUIDO`, `RETIRADA`, `ENTREGUE`, `CANCELADA`, `DEVOLVIDA`.

O backend já usa **State Pattern**: `EstadoOS` com `Reservada`, `EmProva`, `EtapaProducao`, `Retirada`, `Devolvida`. Há estados com sobreposição aparente (`PROVA`/`EM_PROVA`) e outros que não são produzidos no fluxo principal (`PENDENTE`). **Não apagar valores do enum nem de dados legados antes de verificar migrações e testes.**

### 7.2. Fluxo alvo simplificado — locação

```text
RESERVADA
   ├── [se há prova] EM_PROVA -> AJUSTES -> PASSADORIA
   └── [se não há ajustes] preparação/conferência -> PASSADORIA
PASSADORIA -> CONCLUIDO (pronta)
CONCLUIDO --[quitada + peça apta]--> RETIRADA
RETIRADA --[devolução]--> DEVOLVIDA
RESERVADA/ETAPAS INICIAIS --[cancelar com regras]--> CANCELADA
DEVOLVIDA (OS encerrada) -> lavagem da peça -> DISPONIVEL (somente peça)
```

**Decisão a validar:** quais etapas são obrigatórias em todos os atendimentos e quais podem ser dispensadas de forma explícita. O código atual exige passadoria para concluir; mantenha essa regra enquanto não for validada outra política, ou crie um caminho de exceção **expresso e testado**, não um salto informal.

### 7.3. Fluxo alvo simplificado — sob medida

```text
RESERVADA -> CONFECCAO -> EM_PROVA -> AJUSTES (se necessários)
         -> PASSADORIA -> CONCLUIDO -> ENTREGUE
```

Sob medida **não** transita para `RETIRADA`/`DEVOLVIDA` no sentido da locação.

### 7.4. Estados físicos existentes da peça

`DISPONIVEL`, `EM_COSTURA`, `EM_LAVAGEM`, `ALUGADA`, `MANUTENCAO`.

`MANUTENCAO` atualmente é mapeado para a implementação `EmCostura`; se houver necessidade de distinguir tipos de manutenção, fazê-lo cuidadosamente. Não confundir a mudança do estado físico **atual** com a reserva **futura** por intervalo.

---

## 8. ARQUITETURA ORIENTADA A OBJETOS RECOMENDADA

### Camadas

```text
React (páginas, componentes, estado local e mensagens)
    -> Axios + sessão/CSRF -> Controller (autorização, DTO, validação)
    -> Service / serviço de domínio (coordenação transacional)
    -> Entidades + objetos de valor + regras de estado
    -> Repository / JPA
    -> PostgreSQL
```

### Responsabilidade de cada área

- `cliente`: identidade e contatos do cliente.
- `peca`: catálogo, unidade física, estado físico e capacidade de reserva.
- `ordemservico`: contrato, itens, ciclo, datas, pagamento/recebimentos e integração das operações.
- `financeiro`: despesas e indicadores financeiros.
- `usuario`: autenticação e gestão de usuários.
- `shared`: segurança, exceções, validações genéricas.
- **NOVO** `precificacao`: meta mensal, PID manual, política comercial, simulação e sugestões auditáveis.

### Regras de design

- Entidade encapsula invariantes locais e métodos de domínio; não deve expor setters genéricos para todo atributo.
- Service coordena **regras que dependem de vários agregados** e controla transações; controller não acessa repositório para decidir regra de negócio.
- DTOs explícitos: entrada, resposta interna, resposta para produção, relatório; não retornar entidade JPA diretamente.
- Valores monetários em `BigDecimal` com escala 2; `double` apenas no **núcleo numérico do PID**, com conversão controlada nas bordas.
- Respeitar `@Version`, lock e validações da OS e pagamento; evitar sessão de persistência inconsistente.
- Componentes React são representação e interação, não uma segunda implementação de fluxo.
- Manter State Pattern atual e melhorar transições, em vez de criar novo sistema paralelo de status.
- **Não criar microserviços** nem uma entidade para cada tela.

### Classes propostas somente se houver necessidade confirmada

- `AcaoPermitida`/`AcoesPermitidasOS`: calculada no backend para a interface exibir o que pode ser feito; **o endpoint sempre revalida**.
- `AjusteItemOS` ou campos por item: só se o fluxo exigir instruções individuais para várias peças.
- `BloqueioPeca` por período: se o controle de manutenção futura precisar de agenda própria; caso contrário, regra mais simples e conservadora.
- `DocumentoService`: gera contrato e recibo a partir de dados persistidos.
- `RegistroAuditoria`: usuário, ação, entidade, data, valores relevantes sem incluir senhas.

---

## 9. CONTRATO ENTRE BACKEND E FRONTEND

### Endpoints EXISTENTES no snapshot (base paths sem `/api` no Spring)

| Endpoint | Operações existentes |
|---|---|
| `/auth/csrf`, `/auth/me`, `/auth/login`, `/auth/logout` | Sessão / segurança (login/logout tratados pelo Spring Security) |
| `/clientes` e `/clientes/{id}` | `POST`, `GET`; `GET` por ID |
| `/pecas`, `/pecas/{id}`, `/pecas/disponiveis` | `POST`, `GET`; `GET` por ID e disponíveis |
| `/pecas/{id}/alugar`, `/costura`, `/lavagem`, `/devolver` | `PATCH`; revisar para impedir bypass da OS |
| `/ordens-servico`, `/ordens-servico/{id}`, `/ordens-servico/status/{status}` | `POST`, `GET`; consultas |
| `/ordens-servico/disponibilidade` | `GET` com peça/início/fim |
| `/ordens-servico/{id}/confeccao`, `/prova`, `/passadoria`, `/concluir`, `/retirar`, `/devolver`, `/cancelar` | `PATCH` |
| `/ordens-servico/{id}/pagamentos`, `/pagamentos/sinal` | `GET` e `POST` recebimentos |
| `/financeiro/despesas`, `/financeiro/despesas/{id}/baixa` | Cadastro, listagem e baixa |
| `/usuarios` | Gerência de usuários conforme controller existente |

### Endpoints SUGERIDOS (ainda NÃO implementados)

Estes caminhos são sugestões de contrato, não exigência de nomes exatos. Preservar consistência com as rotas reais.

```http
PATCH /clientes/{id}
GET   /clientes/{id}/ordens-servico
PATCH /pecas/{id}
PATCH /ordens-servico/{id}/reagendar
GET   /ordens-servico/{id}/acoes-permitidas
GET   /ordens-servico/producao                 # DTO enxuto sem valores
GET   /dashboard/compromissos?data=YYYY-MM-DD
GET   /ordens-servico/{id}/contrato.pdf
GET   /ordens-servico/{id}/pagamentos/{recebimentoId}/recibo.pdf
GET   /financeiro/indicadores?mes=YYYY-MM
PUT   /precificacao/metas/{mes}
GET   /precificacao/metas/{mes}
POST  /precificacao/simular
POST  /ordens-servico/{id}/aprovar-preco       # se existir renegociação permitida
GET   /precificacao/historico
```

**Importante:** o frontend pode consumir `/api/...` e o proxy/Nginx remover `/api` antes de encaminhar ao backend, **ou** o backend pode adotar prefixo; escolher **uma** abordagem consistente. O projeto atual usa caminhos Java **sem `/api`**.

### Respostas e tratamento de erro

- Sucesso devolve dados suficientes para atualizar a tela; após mutação, preferir um refetch central da OS e pagamentos, ou invalidar cache, em vez de manter informações conflitantes.
- Erro de domínio deve ter `mensagem` compreensível (o `GlobalExceptionHandler` atual já devolve esse campo).
- Concorrência conflitante deve ser tratada com status apropriado (ex.: 409), explicação e atualização da UI; erros de validação de negócio podem usar 400 conforme o contrato atual, mas padronizar.
- 401: sessão não autenticada; 403: falta de permissão; 404: registro inexistente; 409: conflito de estado/concorrência.
- A lista `acoesPermitidas` pode ser calculada por usuário/OS, mas **não equivale a autorização definitiva**. Revalidar no endpoint PATCH/POST.

---

## 10. FRONTEND — COMO DEVE SER A EXPERIÊNCIA

### Navegação simples, sem duplicação

- **Painel:** compromissos do dia e alertas operacionais.
- **Clientes:** pesquisa, cadastro, edição e histórico.
- **Peças:** inventário, etiqueta, preço, estado físico e disponibilidade por período.
- **Ordens:** ficha central da contratação, etapas, datas, peças, pagamentos, documentos e ações permitidas.
- **Produção:** fila de prova/ajustes/passadoria, medidas e prazos (sem financeiro).
- **Financeiro:** recebimentos/despesas, resultado por período, meta e módulo PID (gerência).
- **Usuários:** administração de acessos (gerência).

### Tela da OS (mais importante)

1. Cabeçalho: número/ID curto, cliente, tipo, estado e próximas ações.
2. Seção **Peças contratadas**: descrições, etiquetas, valores congelados quando permitido e período.
3. Seção **Datas**: evento, prova, início/fim da locação, entrega, reagendamento.
4. Seção **Produção**: medidas, instruções, andamento e responsáveis quando houver.
5. Seção **Financeiro** para atendimento/gerência: total, sinal, recebido, saldo e histórico; produção não vê.
6. Seção **Documentos**: contrato/recibos disponíveis.
7. Ações com mensagens explicativas: registrar pagamento, avançar produção, retirar, devolver, cancelar, reagendar.
8. Se houver sugestão PID, mostrar **sugestão, justificativa e botão de aprovação** somente a quem puder alterar preço.

### Padrões de interação obrigatórios

- Mostrar **carregando**, **vazio**, **erro** e **sucesso**, não apenas lista vazia após falha.
- Bloquear envio duplo enquanto requisição está em andamento.
- Após receber pagamento, atualizar **saldo, ações e dados da OS**, além do modal de pagamento.
- Evitar esconder erros do backend. Apresentar `response.data.mensagem` quando aplicável.
- Confirmações claras para cancelar e operações irreversíveis.
- Datas em pt-BR com horário consistente, adaptadas a telas móveis.
- Não expor informações financeiras em card ou resposta consumida pela produção.
- Evitar modais gigantes; priorizar acessibilidade (rótulos, navegação por teclado e mensagens).

### Arquivos prioritários do frontend existentes

```text
frontend/src/shared/api.js
frontend/src/shared/Catalogo.jsx
frontend/src/app/AppRoutes.jsx
frontend/src/features/ordens-servico/OrdensServicoPage.jsx
frontend/src/features/ordens-servico/AcoesDaOrdem.jsx
frontend/src/features/ordens-servico/PagamentosDaOrdem.jsx
frontend/src/features/ordens-servico/FilaCosturaPage.jsx
frontend/src/features/pecas/AcoesDaPeca.jsx
frontend/src/features/dashboard/DashboardPage.jsx
frontend/src/features/financeiro/RelatoriosPage.jsx
```

**Correções pontuais identificadas:**

- Rever baseURL/proxy (8080 x 8081) e `withCredentials`/CSRF em mesma origem ou configuração CORS segura.
- `DashboardPage.jsx`: propriedade de `Intl.DateTimeFormat` deve ser `day`, não `dia`.
- Rever `PagamentosDaOrdem.jsx` para atualizar dados da OS depois do recebimento.
- Remover ações diretas do inventário que permitam alugar/devolver peça sem OS, ou restringi-las à manutenção administrativa.
- `FilaCosturaPage.jsx` atualmente reúne OS por status e carrega todo o DTO financeiro. Criar endpoint restrito e usar ações válidas calculadas pelo backend.

---

## 11. MÓDULO DE IA — PID MANUAL PARA APOIO À PRECIFICAÇÃO

### 11.1. Objetivo e limites

A gerência informa uma **meta financeira mensal**. O módulo monitora o desempenho realizado no período, calcula um percentual conservador de ajuste de preços e gera uma **sugestão explicável** para a nova OS, considerando uma política de desconto por fidelidade e alertas de histórico de cancelamentos.

**Não é** predição treinada, nem otimização com garantia matemática de lucro. O PID **clássico é controle por feedback, não aprendizado de máquina**. Descrevê-lo corretamente como núcleo de um agente de decisão baseado em controle e regras. Se a professora exigir aprendizado, documentar limitação e não fingir que ele existe.

**Regra acadêmica:** implementar **P, I, D, limitação da saída, anti-windup, persistência/atualização do estado e testes manualmente**. Não usar biblioteca pronta de controle PID/IA. Spring Boot e JPA são infraestrutura, não fazem o cálculo.

### 11.2. Entrada e indicador correto

- `metaMensal`: valor positivo de referência, mês/ano e usuário responsável.
- `resultadoRealizadoAteDia`: **não confundir** saldo a receber com recebimento pago.
- **Enquanto não houver custos adequados:** denominar indicador de **resultado de caixa** = recebimentos realizados no mês − despesas pagas no mês. A interface e o relatório não devem rotular isso como lucro contábil.
- Para meta de **lucro**, introduzir custos por OS e despesas sem dupla contagem; validar critério financeiro com a gerência.
- `diaAtual`, `diasNoMes` (ou trajetória projetada baseada em dias úteis, apenas se houver dados para tanto).
- Preço-base congelado/cotado da nova OS; fidelidade e risco em regras comerciais separadas do PID.

### 11.3. Equações do PID discreto

Para o dia `d` de um mês de `D` dias, meta positiva `L`:

```text
referenciaAcumulada(d) = L * d / D
erroNormalizado(d)   = (referenciaAcumulada(d) - resultadoRealizado(d)) / L

P(d) = Kp * erroNormalizado(d)
I(d) = I(d-1) + erroNormalizado(d) * deltaDias
D(d) = (erroNormalizado(d) - erroNormalizado(d-1)) / deltaDias

saidaBruta = P(d) + Ki * I(d) + Kd * D(d)
ajustePID  = limitar(saidaBruta, ajusteMinimo, ajusteMaximo)
```

- `deltaDias > 0`; **uma atualização efetiva por dia/meta**, não uma por abertura da tela ou simulação.
- Primeira amostra: termo D = 0 e estado inicial controlado; início de cada mês reinicia estado da meta correspondente.
- Proteger `I` com anti-windup (não acumular adicionalmente quando saída saturada e erro empurra mais na direção da saturação, ou técnica equivalente), limites e validação numérica.
- Não dividir por meta 0; meta deve ser positiva.
- `Kp`, `Ki`, `Kd`, `ajusteMinimo`, `ajusteMaximo` são **parâmetros configuráveis e calibráveis**. Intervalos ilustrativos, como -10% a +10%, precisam da aprovação comercial do Atelier.
- Validar resultado não finito e intervalo inválido; calcular de modo determinístico.
- Um percentual positivo sugere aumento global quando o resultado fica atrás da trajetória, mas **não há garantia de que subir preço aumente lucro**, pois pode reduzir demanda.

### 11.4. Precificação da OS

```text
precoAjustado = precoBase * (1 + ajustePID)
precoComFidelidade = precoAjustado * (1 - descontoFidelidade)
precoSugerido = max(precoMinimoPermitido, arredondar2(precoComFidelidade))
```

- Desconto de fidelidade é **regra comercial independente** (ex.: quantidade de OS concluídas), não termo P/I/D.
- Histórico de cancelamentos gera **alerta ou recomendação de sinal**; não inventar probabilidade estatística nem aplicar penalização arbitrária ao cliente.
- `precoMinimoPermitido` deve respeitar custo e margem mínima conforme política definida, **não** um número fixo inventado.
- Sugestão não modifica preço contratado sozinha; gerência aprova ou rejeita, com auditoria.
- Nunca reajustar contrato fechado automaticamente. Alterar preço de OS exige regra de alteração de valor + `Pagamento` consistente.

### 11.5. Modelo OO mínimo do módulo (proposto)

```text
precificacao/
├── MetaMensal.java                   # entidade: mes, meta, indicador, responsável
├── EstadoPID.java                    # estado persistido: integral, erroAnterior, atualização
├── ControladorPID.java               # ALGORITMO MANUAL, classe isolada e testável
├── PoliticaPrecificacao.java         # limites e descontos, sem matemática do PID
├── AgentePrecificacaoService.java    # orquestra regra financeira + PID + cotação
├── SugestaoPreco.java                # resposta: base, percentual, piso, final, motivo
├── ...Repository.java                # persistência necessária
├── ...Controller.java                # endpoints somente gerência
└── dto/                               # contrato de entrada/saída
```

**Observação:** um arquivo auxiliar `ControladorPID.java` foi produzido em conversa anterior; se for incluído, revisar package, teste e persistência antes de integrá-lo. Não presumir que já está dentro do ZIP.

### 11.6. Sequência operacional do PID

1. Gerência cadastra meta e limites autorizados.
2. Sistema apura resultado realizado por período a partir de fontes financeiras confiáveis.
3. Em atualização diária idempotente, calcula erro e executa **uma vez** o algoritmo PID, persistindo estado/saída.
4. Ao abrir cotação da nova OS, consulta o **último ajuste diário válido** e o aplica ao preço-base para produzir proposta.
5. Regra de fidelidade/piso mínimo é aplicada.
6. Sistema exibe memória de cálculo simples: meta, realizado, erro, percentual PID, desconto, piso e preço final.
7. Gerência decide aceitar/rejeitar. Apenas a aceitação válida altera a OS, respeitando regras de pagamento e auditoria.
8. Próximos resultados financeiros alimentam o ciclo futuro.

### 11.7. Testes mínimos de PID

- Erro = 0 gera saída coerente para estado inicial.
- Erro positivo/negativo produz reação de sinal esperado quando não saturado.
- Termo derivativo na primeira amostra = 0.
- Integral acumula corretamente por intervalo e não explode em saturação.
- Saída respeita limites superior e inferior.
- Repetir consulta no mesmo dia não altera estado.
- Nova competência mensal não herda integral indevidamente.
- Meta 0, NaN/Infinity, deltaDias ≤ 0, preço-base inválido: erros controlados.
- Fidelidade e piso mínimo geram resultado explicável.
- Usuário de produção/atendimento não consegue configurar meta nem aprovar preço sem permissão.
- Cotação/simulação **não gera recebimento nem altera OS por engano**.

---

## 12. SEGURANÇA, BANCO, DOCKER E PWA

### Segurança

1. Preservar Spring Security com sessão, BCrypt e CSRF.
2. Exigir permissão no backend em **cada** operação, inclusive documentos e precificação.
3. DTOs de **OS e peças** para produção sem valores financeiros ou informações pessoais desnecessárias.
4. Validação de dados e erros consistentes, sem stack trace sensível ao usuário.
5. Evitar segredo/credencial no repositório; usar `.env` local e `.env.example` sem valores reais.
6. Não desabilitar proteção CSRF sem substituto seguro; validar CORS/origens e cookies de sessão.
7. Auditoria mínima de ações sensíveis e não registrar senhas nem dados pessoais completos em logs.
8. Em produção, restringir consultas administrativas, desligar `spring.jpa.show-sql=true`, configurar HTTPS, cookies seguros e backup.

### Banco e migrações

- V1: clientes, peças e usuários; V2: OS e itens; V3: pagamentos, recebimentos, despesas.
- **Nunca editar migrações antigas já aplicadas**. Criar `V4__...` e posteriores.
- Usar constraints, índices e relacionamento coerentes, especialmente itens e reservas.
- Para alteração do modelo `ItemOrdemServico`, planejar conversão de `@Embeddable` com cuidado para preservar dados.
- Banco de testes separado do banco de produção. Rodar migrações em banco vazio **e** em banco com dados fictícios anteriores.

### Docker / rede

- O Compose atual inicia só PostgreSQL; os Dockerfiles de frontend e backend não constituem um deploy completo.
- Padronizar acesso da aplicação e backend pela mesma origem (ex.: `/api` no proxy) para facilitar cookie e CSRF.
- Se backend rodar em container, **`localhost` não é o serviço `db`**: usar endereço por DNS do Compose (`db`) e variáveis corretas.
- Não expor PostgreSQL publicamente sem necessidade; configurar volumes e backup.
- Documentar comandos verificáveis no README e distinguir rodar localmente de rodar tudo em Docker.

### PWA

- Preservar `vite-plugin-pwa` e manifest já configurados.
- Testar instalação em celular e layout responsivo.
- Não fazer cache offline de CPF, dados financeiros, contratos ou API autenticada sem estratégia segura.
- A PWA é **um modo de apresentação/instalação da aplicação web**, não um segundo backend.

---

## 13. PLANO DE IMPLEMENTAÇÃO — UMA TAREFA POR VEZ

**Instrução:** implementar cada tarefa como mudança pequena, rodar testes relevantes e só então avançar. A ordem abaixo é a prioridade técnica recomendada; não representa que alguma tarefa já esteja concluída.

### Fase A — Integridade do núcleo (P0)

- [x] **T01 — Retirada segura:** validar quitação, estado e prontidão; atualizar OS e peças na mesma transação.
  - Arquivos iniciais: `OrdemServicoService`, `OrdemServico`, `Peca`, `PecaRepository` e testes.
  - Aceite: bloqueia retirada com saldo > 0; após quitar, muda OS e peça; rollback se uma mudança falhar.
  - Validada em 17/11/2026: `./mvnw.cmd '-Dspring.jpa.show-sql=false' test` — 39 testes, zero falhas/erros/ignorados, incluindo 13 cenários de integração no PostgreSQL `atelie_test`; `npm.cmd run build` — sucesso, incluindo geração do PWA.
  - Evidências: `deveRetirarTodasAsPecasQuitadasERejeitarRepeticao`, `deveReverterOrdemEPrimeiraPecaSeSegundaNaoEstiverPronta` (costura, lavagem, manutenção e alugada), `deveConfirmarSomenteUmaRetiradaSimultanea`, `deveEntregarSobMedidaQuitadaSemAlugarModelo`, `deveExporErroDeQuitacaoNaApiEAutorizarSomenteAtendimento`, `deveRejeitarReservaLegadaConflitanteSemAlterarEstados`, `deveRejeitarLocacaoSemItens` e testes de estado/cancelamento.
  - Frontend: confirmação explica os requisitos; mantém o endpoint, CSRF, atualização da ordem e apresentação de erros existentes. Validação visual em navegador/celular não executada nesta tarefa. Nenhuma migração necessária.
- [x] **T02 — Devolução + lavagem:** ao devolver, OS encerra locação e peças vão para lavagem; liberação posterior é explícita.
  - Aceite: não libera estoque imediatamente e não permite devolver duas vezes.
  - Validada em 17/11/2026: `./mvnw.cmd '-Dspring.jpa.show-sql=false' test` compilou o backend e executou 62 testes, incluindo 31 cenários de integração no PostgreSQL `atelie_test`, com zero falhas, erros ou ignorados. A suíte inclui a regressão da T01. `npm.cmd run build` passou, com geração do PWA; `git diff --check` passou.
  - Evidências: `deveDevolverTodasAsPecasParaLavagemSemLiberarEstoque`, `deveRejeitarDevolucaoForaDoEstadoRetirada` (todos os demais estados), `deveReverterDevolucaoSeUmaPecaEstiverInconsistente`, `deveConfirmarSomenteUmaDevolucaoSimultanea`, `devePreservarReservaFuturaEBloquearRetiradaAteLiberacao`, `deveManterManutencaoIndisponivelAteConferenciaExplicita` e `deveIntegrarDevolucaoELiberacaoPelaApiComPermissoesECsrf`. Testes de domínio cobrem lavagem/conferência obrigatórias e rejeição de devolução sob medida, inclusive com estado legado inconsistente.
  - Contrato: `PATCH /ordens-servico/{id}/devolver` permanece sem corpo. `PATCH /pecas/{id}/devolver` agora exige `{"lavagemConcluida":true,"condicoesConferidas":true}`; dados ausentes ou confirmação incompleta retornam erro de negócio. A operação registra a liberação física, sem alterar reservas. Administração, atendimento e produção podem registrar a conferência; devolução da OS permanece restrita a administração/atendimento.
  - React: confirmação e sucesso da devolução explicam o encaminhamento à lavagem; inventário oferece **Conferir e liberar peça**, com confirmações desmarcadas por padrão, e mantém o encaminhamento à costura/manutenção. Os cards usam os estados retornados pela API.
  - Sem migrações ou tempo de lavagem inventado: a conclusão e a aptidão física são declaradas explicitamente pelo usuário responsável. Nenhuma validação visual em navegador/celular foi executada nesta tarefa. T03 não implementada; T04 segue pendente quanto à revisão geral das ações comerciais diretas.
- [ ] **T03 — Disponibilidade correta:** checagem de reservas + bloqueios físicos aplicáveis com concorrência; não confundir disponível hoje com intervalo futuro.
  - Aceite: duas reservas simultâneas conflitantes não entram; intervalos não sobrepostos funcionam.
- [x] **T03 — Disponibilidade correta:** checagem transacional das reservas e prontidão física, preservando reservas futuras.
  - Validada em 17/11/2026 na suíte completa `./mvnw.cmd clean '-Dspring.jpa.show-sql=false' test`: 67 testes, zero falhas/erros/ignorados; PostgreSQL `atelie_test` executou a V4. Evidências: `deveRejeitarReservaLegadaConflitanteSemAlterarEstados`, `devePermitirReservasSemSobreposicaoParaMesmaPeca`, `devePermitirReservaFuturaComPecaEmLavagemMasImpedirInicioImediato`, `devePreservarReservaFuturaEBloquearRetiradaAteLiberacao`, `deveConfirmarSomenteUmaReservaSimultanea` e `deveImpedirDuasReservasSimultaneasDaMesmaPeca`.
  - Sem política de duração do preparo/lavagem: períodos continuam semiabertos como RN02. A prontidão é conferida no início imediato e na retirada; as reservas futuras são mantidas sem estimativa de liberação física. Essa regra temporal ainda deve ser validada pelo Atelier se quiser impor antecedência entre locações.
- [x] **T04 — Ações diretas de peça:** rotas comerciais fora da OS removidas/restritas; manutenção e liberação mantidas por perfil.
  - Aceite: nenhum endpoint faz peça ficar `ALUGADA` sem correspondente OS válida.
- [x] **T05 — Cancelamento coerente:** cancela estados permitidos, libera reserva, preserva pagamentos e registra pendência de acerto, sem estorno.
  - Validadas junto à suíte acima; evidências: `deveCancelarComPendenciaPreservarRecebimentosELiberarReserva`, `deveRejeitarRetiradaDeOrdemCanceladaMesmoQuitada`, verificações de operações incompatíveis após cancelamento e regressão T01/T02.
  - V4 adiciona `ordens_servico.pendencia_acerto` e inicializa pendência para ordens históricas canceladas com valores recebidos. A resposta da OS expõe o indicador; o React informa a atendente. Pagamentos novos em OS cancelada são bloqueados; recebimentos existentes permanecem consultáveis. Nenhuma regra de multa/estorno foi criada.
  - Frontend: `npm.cmd run build` passou em 17/11/2026, incluindo geração do PWA. Sem conferência visual em navegador/celular. `git diff --check` passou.
  - Aceite: cancelamento libera calendário, mantém pagamentos históricos e não fabrica estorno.

### Fase B — Completar fluxo de atendimento (P1)

- [x] **T06 — Editar clientes e peças:** validações mantidas no backend; histórico por cliente acessível; preço negociado da OS preservado.
  - Evidências: `ClienteServiceTest`, `PecaEdicaoTest` e `SistemaApplicationTests.devePersistirReservaFluxoERecebimento`. Edição de peça não troca tipo/estado físico nem altera itens de OS existentes.
- [x] **T07 — Reagendar OS:** bloqueia a ordem e as peças em transação, valida período/conflitos excluindo a própria OS; falha não altera datas originais.
  - Evidências: `ReagendamentoTest.reagendaReservaSemContarAPropriaOrdemComoConflito` e `rejeitaConflitoSemAlterarDatasOriginais`; regressão executada na suíte completa.
- [x] **T08 — Produção e itens:** o padrão State continua impondo as etapas; medidas/instruções são específicas por peça; DTOs de consulta e mutação não expõem valores financeiros ao perfil de produção.
  - V5 migra os dados existentes da OS para os itens. Evidências: `devePersistirReservaFluxoERecebimento`, `OrdemServicoStateTest`, `deveExigirLoginESepararPermissoes` e `deveIntegrarDevolucaoELiberacaoPelaApiComPermissoesECsrf`.
- [x] **T09 — Dashboard diário:** compromissos derivados no backend, ordenados por horário, com OS canceladas excluídas e link para a ordem.
  - Evidência: `DashboardCompromissosTest.listaCompromissosDoDiaEmOrdemEExcluiOrdensCanceladas`.
- [x] **T10 — Documentos:** contrato de locação e recibo individual em PDF, gerados de dados persistidos e protegidos por perfil.
  - `DocumentoPdfServiceTest` lê e verifica conteúdo dos PDFs; a API retorna 403 para o perfil de produção. O PDF não inventa cláusulas legais.
- [x] **T11 — Consolidado financeiro:** totais mensais por data efetiva, resultado de caixa e saldo total a receber; endpoint restrito à gerência.
  - Evidências: `IndicadoresFinanceirosServiceTest` e `SistemaApplicationTests.consolidadoFinanceiroMensalERestritoAGerencia`. Resultado não é tratado como lucro. Como não há registro de estorno, pagamentos recebidos de ordens canceladas permanecem no caixa; política de reembolso segue pendente de definição comercial.
  - Verificação final em 17/11/2026: `./mvnw clean test` — 79 testes, zero falhas/erros/ignorados; Flyway validou V1–V5 no PostgreSQL `atelie_test`. `npm run build` passou com geração do PWA; `git diff --check` passou. Não houve verificação visual em navegador/celular.

### Fase C — Agente PID (P1, exigência de IA)

- [ ] **T12 — Modelo e migração:** MetaMensal, EstadoPID e política configurável.
- [ ] **T13 — Algoritmo matemático:** P/I/D manual, normalização, limites, anti-windup e testes unitários.
- [ ] **T14 — Atualização diária:** apurar indicador, atualizar estado **uma vez por dia**, armazenar resultado auditável.
- [ ] **T15 — Sugestão:** preço-base, fidelidade, piso, justificativa e endpoints.
- [ ] **T16 — Aprovação:** política, permissão da gerência, ajuste seguro no valor contratado, sem mutação automática.
- [ ] **T17 — Tela PID:** meta e indicadores, simulação, explicação e aprovação.

### Fase D — Refinamento de integração e segurança (P1)

- [ ] **T18 — Contrato API:** DTO por perfil, mensagens de erro, ações permitidas e atualização das telas.
- [ ] **T19 — React da OS:** detalhes integrados, forms de edição, reagendamento, pagamentos atualizados.
- [ ] **T20 — Configuração:** corrigir baseURL/proxy e configurar ambiente executável/reprodutível.
- [ ] **T21 — Segurança e PWA:** permissões, CSRF, dados sensíveis, mobile e instalação.
- [ ] **T22 — Testes de ponta a ponta, README e diagramas:** demonstrar fluxos e documentar decisões.

### Se o tempo do dia acabar

A ordem prática para entrega é: **T01–T05 → T12–T15 (PID demonstrável) → T18–T19 → testes → demais pendências**. O módulo PID não substitui a integridade da OS. Não sacrificar testes de reserva, pagamento e retirada para produzir apenas telas bonitas.

---

## 14. CENÁRIOS DE ACEITAÇÃO / TESTES ESSENCIAIS

| ID | Cenário | Esperado |
|---|---|---|
| A01 | Cliente com CPF inválido | Rejeita, sem salvar |
| A02 | Novo cliente com CPF já existente | Rejeita duplicidade |
| A03 | Duas OS da mesma etiqueta com período sobreposto | Segunda rejeitada; nenhuma gravação parcial |
| A04 | Mesma peça em períodos compatíveis | Reservas permitidas se prontidão e intervalo garantidos |
| A05 | Duas requisições simultâneas de reserva | No máximo uma confirma período conflitante |
| A06 | Alterar preço de catálogo após OS | OS preserva valor contratado |
| A07 | Criar OS locação com fim ≤ início | Rejeita |
| A08 | Reagendar OS para horário ocupado | Rejeita sem perder datas/reserva originais |
| A09 | Sinal + pagamento parcial | Saldo e histórico corretos |
| A10 | Pagamento acima do saldo | Rejeita |
| A11 | Repetir envio de pagamento após falha de rede | Não duplica recebimento silenciosamente |
| A12 | Retirada com saldo > 0 | Rejeita, sem mudar estados |
| A13 | Retirada quitada e pronta | OS = `RETIRADA`, peças físicas = `ALUGADA` |
| A14 | Devolução de OS retirada | OS = `DEVOLVIDA`, peças = `EM_LAVAGEM` |
| A15 | Repetir devolução | Rejeita |
| A16 | Finalizar lavagem | Peça = `DISPONIVEL` quando liberada e apta |
| A17 | OS cancelada com sinal | Histórico preservado, acerto explícito |
| A18 | Sob medida concluída | `ENTREGUE`, sem `DEVOLVIDA` |
| A19 | Produção acessa financeiro/PID | 403, sem dados no corpo |
| A20 | Produção consulta sua fila | Vê peças, medidas e prazo; não vê valores/CPF desnecessários |
| A21 | Recebimento registrado na modal | OS e saldo atualizam no frontend |
| A22 | Contrato/recibo gerado | Dados corretos e acesso autorizado |
| A23 | PID erro positivo/negativo e saturação | Resposta matemática dentro dos limites |
| A24 | Abrir tela de simulação PID repetidas vezes | Estado integral não muda |
| A25 | Meta muda de mês | Estado PID reiniciado/separado corretamente |
| A26 | Usuário sem permissão tenta aprovar preço | Rejeita no backend |
| A27 | Erro de reserva durante salvamento | Nenhum item/OS/pagamento parcial persiste |
| A28 | Front em viewport móvel | Formulários, listas e ações permanecem utilizáveis |

**Regra de conclusão:** apresentar evidência do teste realizado (nome do teste, execução e resultado). Nunca confundir "código escrito" com "comportamento comprovado".

---

## 15. CONTRATO DE TRABALHO PARA CADA TAREFA COM O AGENTE

Ao receber a solicitação `Execute TNN`, o agente deve responder seguindo esta sequência:

1. **Releitura:** localizar tarefa TNN neste arquivo e inspecionar classes, DTOs, migrações e testes relacionados.
2. **Diagnóstico objetivo:** o que já existe, qual defeito/falta concreta e qual regra será garantida.
3. **Plano mínimo:** listar arquivos que pretende criar/editar e por quê; sem reescrever módulos sem relação.
4. **Implementação:** alterar backend primeiro quando houver regra de negócio; migration se necessária; adaptar o React somente para o novo contrato.
5. **Testes:** incluir teste de sucesso, transição inválida, concorrência ou limite correspondente à tarefa.
6. **Verificação:** rodar testes pertinentes (`./mvnw test`, `npm run build`) quando o ambiente permitir; reportar falhas reais.
7. **Resultado:** explicar arquivos alterados, regra atendida, riscos e pendências; **não afirmar que testou o que não executou**.
8. **Próximo passo:** sugerir somente a tarefa dependente seguinte, sem implementar outras sem pedido.

### Exemplo de prompt para o agente

> Leia `CONTEXTO_IMPLEMENTACAO_LINHO_POODS.md`, abra o código correspondente e execute **T01 — Retirada segura**. Não altere T02–T22. Preserve Java/Spring Boot/React/PostgreSQL e o padrão State atual. Faça mudanças mínimas, explique as regras, crie testes para retirada com saldo pendente/quitada e mostre os arquivos alterados e resultados dos comandos executados.

### Comandos-base (confirmar ambiente e variáveis)

```bash
# A partir da raiz do repositório, com .env local configurado:
docker compose up -d db

# Backend
cd backend
./mvnw test
./mvnw spring-boot:run

# Frontend (outro terminal)
cd frontend
npm install
npm run dev
npm run build
```

**Atenção:** a configuração `application.properties` e o proxy Vite precisam ser conciliados antes de assumir que a API funciona; os testes de integração atuais dependem de um PostgreSQL de teste. Não rodar scripts destrutivos contra banco com dados reais.

---

## 16. DEFINIÇÃO DE PRONTO DO MVP

O sistema só está **funcionalmente pronto para demonstração acadêmica** quando:

- [ ] É possível autenticar os três perfis; cada um visualiza apenas informações permitidas.
- [ ] É possível cadastrar/editar cliente e consultar seu histórico.
- [ ] É possível cadastrar/consultar/editar peça e visualizar disponibilidade por data.
- [ ] Uma locação real é criada sem conflito e preserva preços negociados.
- [ ] Provas, ajustes e passadoria são acompanhados com dados coerentes.
- [ ] Sinal, recebimentos, saldo e quitação estão corretos.
- [ ] Não se entrega/retira uma OS com saldo devedor fora de exceção autorizada.
- [ ] Retirada/devolução/lavagem sincronizam OS e peças sem ações manuais contraditórias.
- [ ] Cancelamento e reagendamento preservam histórico e consistência.
- [ ] Sob medida chega a `ENTREGUE` sem necessidade de devolução.
- [ ] O dashboard mostra os compromissos reais do dia.
- [ ] Contrato e recibos em PDF podem ser gerados conforme dados da OS.
- [ ] O financeiro distingue recebimentos, despesas, resultado de caixa e lucro.
- [ ] O controlador PID foi escrito manualmente, possui testes e gera sugestão explicável, aprovada por gerência.
- [ ] Frontend reflete estado atualizado após cada operação e funciona em viewport móvel.
- [ ] Testes automatizados e principais cenários manuais foram executados.
- [ ] README permite rodar o projeto; limitações conhecidas estão documentadas.

**Importante:** pronto para demonstração **não equivale** a aprovado para produção com dados reais. Operação comercial exige validação do Atelier, segurança, backup, deploy e testes adicionais.

---

## 17. DECISÕES DE NEGÓCIO QUE AINDA PRECISAM DE CONFIRMAÇÃO

Não tomar estas decisões arbitrariamente durante a implementação:

| Tema | Pergunta em aberto | Tratamento provisório recomendado |
|---|---|---|
| Quitação | Pode retirar sem 100% pago em casos excepcionais? | Bloquear por padrão; eventual exceção explícita e auditada |
| Cancelamento | O sinal é estornável? Existe multa? | Registrar acerto pendente sem aplicar taxa automática |
| Lavagem | Qual tempo mínimo entre devolução e nova retirada? | Verificar prontidão real; tempo fixo só se validado |
| Provas | Toda OS exige prova? Quantas? | Preservar etapas atuais; permitir dispensa apenas com regra explícita |
| Sob medida | Modelos são itens de catálogo ou serviços? | Tratar registros atuais como modelos; não como peças físicas de aluguel |
| Custos | Como calcular custo direto da peça/serviço e despesas gerais? | Exibir resultado de caixa, não lucro, até definir método |
| Fidelidade | Quantas OS e qual desconto máximo? | Configuração pela gerência; não inventar percentuais finais |
| Cancelamento provável | Existe histórico suficiente para previsão? | Apenas alerta de histórico, não probabilidade de ML inventada |
| PID | Ganhos e limites comercialmente aceitáveis? | Parâmetros de demonstração identificados como experimentais |
| Documento | Quais cláusulas devem constar no contrato? | Usar dados básicos reais; obter cláusulas do Atelier antes de firmar política |
| Horários | Quais horários de atendimento e antecedências são aceitos? | Não criar bloqueios arbitrários; validar datas e conflitos |

---

## 18. MAPA RÁPIDO — ONDE PROCURAR CADA REGRA NO CÓDIGO

```text
CLIENTES
  backend/.../cliente/ClienteService.java
  backend/.../cliente/ClienteController.java
  frontend/src/features/clientes/ClientesPage.jsx

PEÇAS / ESTADO FÍSICO
  backend/.../peca/Peca.java
  backend/.../peca/PecaService.java
  backend/.../peca/PecaRepository.java
  backend/.../peca/state/*.java
  frontend/src/features/pecas/AcoesDaPeca.jsx
  frontend/src/features/pecas/PecasPage.jsx

ORDEM / RESERVA / PRODUÇÃO
  backend/.../ordemservico/OrdemServico.java
  backend/.../ordemservico/OrdemServicoService.java
  backend/.../ordemservico/OrdemServicoRepository.java
  backend/.../ordemservico/OrdemServicoController.java
  backend/.../ordemservico/ItemOrdemServico.java
  backend/.../ordemservico/state/*.java
  frontend/src/features/ordens-servico/OrdensServicoPage.jsx
  frontend/src/features/ordens-servico/AcoesDaOrdem.jsx
  frontend/src/features/ordens-servico/FilaCosturaPage.jsx

PAGAMENTOS / FINANCEIRO
  backend/.../ordemservico/Pagamento.java
  backend/.../ordemservico/Recebimento.java
  backend/.../financeiro/DespesaService.java
  frontend/src/features/ordens-servico/PagamentosDaOrdem.jsx
  frontend/src/features/financeiro/RelatoriosPage.jsx

AUTENTICAÇÃO / PERFIS
  backend/.../shared/config/SecurityConfig.java
  backend/.../usuario/*.java
  frontend/src/features/auth/*
  frontend/src/app/AppRoutes.jsx

INTEGRAÇÃO
  frontend/src/shared/api.js
  frontend/vite.config.js
  backend/src/main/resources/application.properties
  docker-compose.yml

PID (A CRIAR)
  backend/src/main/java/com/atelie/sistema/precificacao/
  frontend/src/features/precificacao/

TESTES
  backend/src/test/java/com/atelie/sistema/
```

### Regra final para o agente

**Não basta que cada tela ou classe funcione isoladamente. O sistema está correto quando uma operação real do Atelier atualiza todos os dados envolvidos de modo consistente, respeita as permissões e apresenta uma resposta clara para a usuária.**

> Este arquivo deve ser revisado conforme tarefas forem concluídas. Para cada alteração comprovada, marque a respectiva tarefa como concluída, registre o teste executado e atualize a seção **"Estado atual confirmado"**. Nunca reescreva este documento como se funcionalidades planejadas já estivessem funcionando.
