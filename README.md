# PMEI — Plataforma de Gestão Empresarial Inteligente

Backend de uma plataforma SaaS para gestão de pequenas empresas no Brasil.
Desenvolvido com Java 21 + Spring Boot.

## 🧠 Problema que resolve

Pequenas empresas no Brasil frequentemente:

- Não sabem seu lucro real
- Não conseguem prever falta de dinheiro
- Misturam dinheiro pessoal com empresarial
- Perdem controle de estoque
- Esquecem vencimento de contratos
- Fazem processos manualmente

Isso gera prejuízo, desorganização e quebra da empresa.

## 💡 Proposta de valor

Sistema modular e inteligente que conecta financeiro, estoque, vendas e contratos
em uma única plataforma — com alertas automáticos para prevenir problemas antes que aconteçam.

## 📦 Módulos

### 💰 Financeiro
- Registro de receitas e despesas (fixas e variáveis)
- Cálculo de saldo atual e lucro mensal
- Relatório por período
- Média mensal e projeção futura de caixa
- Alertas inteligentes:
    - Despesas aumentaram acima do threshold configurável
    - Projeção futura indica saldo negativo
    - Comprometimento de receita com despesas fixas acima de 70%

### 📦 Estoque
- Cadastro de produtos com estoque mínimo
- Entradas e saídas com histórico de movimentações
- Saída automática via venda
- Alerta de reposição quando estoque < mínimo

### 🧾 Vendas
- Registro de vendas com múltiplos itens
- Integração automática: baixa estoque + registra receita no financeiro
- Isolamento multi-tenant por empresa

### 📑 Contratos
- Cadastro de contratos com clientes (INCOME) e fornecedores (EXPENSE)
- Impacto automático no financeiro ao criar ou reajustar contrato
- Alertas de vencimento configuráveis por contrato
- Status automático (ACTIVE → EXPIRED) baseado na data
- Histórico completo de reajustes com percentual calculado
- Endpoint de impacto mensal no fluxo de caixa

### 🔐 Segurança
- Autenticação JWT
- Multi-tenancy real — cada empresa enxerga apenas seus próprios dados
- Autorização por perfil (ADMIN, USER)

## 🛠️ Stack

| Tecnologia | Uso |
|---|---|
| Java 21 | Linguagem principal |
| Spring Boot 3 | Framework |
| Spring Security + JWT | Autenticação e autorização |
| JPA + Hibernate | Persistência |
| PostgreSQL | Banco de dados |
| Bean Validation | Validação de entrada |
| Swagger / OpenAPI | Documentação da API |
| JUnit 5 + Mockito | Testes unitários |
| Docker + Docker Compose | Containerização |

## 🚀 Como rodar

### Com Docker (recomendado)
```bash
docker-compose up --build
```

### Local

Configure o `src/main/resources/application.yml` com suas credenciais PostgreSQL e rode:
```bash
mvn spring-boot:run
```

### Documentação da API

Acesse o Swagger UI em:
```
http://localhost:8080/swagger-ui/index.html
```

## 🧪 Testes
```bash
mvn test
```

55 testes unitários cobrindo todos os serviços com JUnit 5 + Mockito.

## 📊 Arquitetura
```
src/
├── auth/          # Autenticação JWT
├── company/       # Gestão de empresas e usuários
├── financial/     # Módulo financeiro e analytics
├── stock/         # Estoque e produtos
├── sales/         # Vendas
├── contracts/     # Contratos
├── config/        # Configurações (Security, Swagger, Clock)
└── exception/     # Exception handler global
```

## 🎯 Diferenciais técnicos

- Modelagem complexa com relacionamentos entre múltiplos domínios
- Regras de negócio reais com integração entre módulos
- Multi-tenancy implementado via JWT — sem filtros globais, seguro por design
- Clock injetado em todos os serviços — testes determinísticos sem mocks de data
- Separação clara entre service transacional e analítico no módulo financeiro