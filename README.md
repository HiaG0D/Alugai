# Alugaí - Gerenciador de Aluguel de Equipamentos

## Integrantes 
    - Hiago Lima
    - Hiram Bahia
    - Derick Sales 
    - Ediney Novaes
    - Paulo Felipe

## Descrição do Projeto

O **Alugaí** é um aplicativo Android desenvolvido para gerenciar o aluguel de equipamentos, permitindo o controle de locações, devoluções e o gerenciamento de locais e clientes de forma simples e eficiente. O projeto utiliza o Google Firebase Firestore como backend para armazenamento de dados em tempo real.

---

## Funcionalidades Principais

O aplicativo é dividido em dois grandes módulos: Gerenciamento de Locais e Gerenciamento de Aluguéis.

### 1. Gerenciamento de Locais

- **Criação de Locais:** Permite cadastrar novos locais (obras, clientes, etc.) informando nome e endereço.
- **Listagem de Locais:** Exibe todos os locais cadastrados em uma lista clara e organizada.
- **Edição e Exclusão:**
    - Cada item na lista de locais possui um menu de opções (`Editar`, `Excluir`).
    - **Editar:** Abre a tela de cadastro pré-preenchida com os dados do local para atualização.
    - **Excluir:** Solicita uma confirmação e, se positivo, remove o local e **todos os seus registros associados** (equipamentos alugados e histórico) do banco de dados.

### 2. Gerenciamento de Aluguéis de Equipamentos

- **Locação de Equipamentos:**
    - Um formulário completo permite registrar um novo aluguel, associando-o a um local existente.
    - **Cálculo de Valor Flexível:** O usuário pode informar se o valor inserido é **unitário** (o sistema calcula `quantidade x valor`) ou se já é o **valor total** do contrato.
    - **Seleção de Período Flexível:** O usuário pode escolher entre um **Período Simples** (`Diário`, `Semanal`, `Mensal`) ou um **Período Customizado** (ex: 20 dias, 3 semanas, etc.).
    - **Previsão de Devolução:** O formulário exibe em tempo real a data de devolução calculada com base na data de início e no período selecionado.

- **Visualização de Detalhes do Local:**
    - Ao clicar em um local da lista, uma tela de detalhes é exibida, contendo:
        - O **valor total** somado de todos os equipamentos atualmente alugados.
        - A **próxima data de devolução** entre todos os equipamentos.
        - Uma lista de **equipamentos atualmente alugados**.
        - Uma lista de **histórico de itens já devolvidos**.

- **Gerenciamento de Itens Alugados:**
    - Cada item na lista de "equipamentos alugados" possui um menu de opções:
    - **Editar:** Abre o formulário de locação pré-preenchido para alterar os detalhes do aluguel.
    - **Excluir Registro:** Remove permanentemente o registro do aluguel.
    - **Marcar como Devolvido:**
        - **Devolução Total:** Move o registro do aluguel para o **histórico de devoluções**, adicionando a data em que a devolução foi feita.
        - **Devolução Parcial:**
            - Se a quantidade for maior que 1, a opção de devolução abre um diálogo para inserir a quantidade a ser devolvida.
            - Se o aluguel foi registrado com **valor unitário**, o valor total é recalculado automaticamente com base na quantidade restante.
            - Se o aluguel foi registrado com **valor total**, o sistema abre um segundo diálogo para que o usuário informe o **novo valor total do contrato** para os itens restantes.

---

## Estrutura do Banco de Dados (Firestore)

A estrutura de dados foi pensada para ser escalável e organizada:

```
locais/ (Coleção)
  |-- {localId_1}/ (Documento)
  |     |-- nome: "Obra Central"
  |     |-- endereco: "Rua Principal, 123"
  |     |
  |     |-- equipamentos_locados/ (Subcoleção)
  |     |     |-- {equipamentoId_A}/ (Documento)
  |     |     |      |-- nomeEquipamento: "Betoneira"
  |     |     |      |-- quantidadeLocada: 2
  |     |     |      |-- valorTotalAluguel: 600.00
  |     |     |      |-- isValorUnitario: true
  |     |     |      |-- dataAluguel: "20/05/2024"
  |     |     |      |-- prazoQuantidade: 1
  |     |     |      |-- prazoUnidade: "meses"
  |     |
  |     |-- historico_devolucoes/ (Subcoleção)
  |           |-- {historicoId_X}/ (Documento)
  |                 |-- nomeEquipamento: "Andaime"
  |                 |-- quantidadeDevolvida: 5
  |                 |-- dataDaDevolucao: Timestamp
  |                 |-- (todos os outros dados originais do aluguel...)
  |
  |-- {localId_2}/ (Documento)
        |-- ...
```

---

## Tecnologias Utilizadas

- **Linguagem:** Java
- **Backend:** Google Firebase Firestore
- **Arquitetura:** Model-View-Adapter
- **Bibliotecas Principais:**
    - AndroidX (AppCompat, ConstraintLayout, RecyclerView)
    - Google Material Components

## Como Executar

1. Clone este repositório.
2. Abra o projeto no Android Studio.
3. Conecte o projeto a um projeto Firebase no [console do Firebase](https://console.firebase.google.com/).
4. Faça o download do arquivo `google-services.json` do seu projeto Firebase e coloque-o no diretório `app/`.
5. Sincronize o Gradle e execute o aplicativo em um emulador ou dispositivo físico.

