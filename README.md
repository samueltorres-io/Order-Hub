# Order Hub

API REST para gerenciar produtos e pedidos.
Quando um pedido é criado, ele publica um evento em kafka.
Um consumidor atualiza inventário.
Para emails/notificação, uma fila RabbitMQ recebe mensagens e simula o envio de um e-mail.
Autenticação com Spring Security + JWT e papeis RBAC.

## Stack

- Java 21
- Spring Boot (Web, Data JPA, Security, Kafka)
- Spring Data JPA + PostgreSQL
- Kafka (events) + RabbitMQ (event queue)
- JWT + Redis para sessão (Access e Refresh)
- Swagger para documentação da API
- JUnit + Mockito para testes unitários
- Docker / docker compose (Postgres, Kafka, Zookeeper, RabbitMQ)

## Modelos de Domínio

User <-> Roles {
  - (N:M)
  - Um user pode ser Admin e Editor
  - A regra "Admin pertence a vários usuários"
}

User <-> Product {
  - (1:N)
  - Usuário age como o vendedor do anúncio
}

User <-> Order {
  - (1:N)
  - Usuário age como o comprador
}

Order <-> OrderItem {
  - (1:N)
  - Composição
}

Product <-> OrderItem {
  - (1:N)
  - Um produto pode aparecer em várias vendas diferentes
}

## Schema Database

**src/main/resources/schema.sql**

## Endpoints Principais (REST)

POST /api/auth/register — registrar usuário

POST /api/auth/login — autenticar (retorna JWT)

GET /api/products — listar produtos (público)

GET /api/products/{name} — listar produto (público)

POST /api/products — criar produto (admin)

POST /api/orders — criar pedido (usuário autenticado)

GET /api/orders/{id} — ver pedido (user/admin)

GET /api/admin/orders — lista pedidos (admin)

GET /api/docs — Swagger UI (springdoc)

## Fluxo de Mensageria

