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
- JWT para autenticação
- Swagger para documentação da API
- JUnit + Mockito para testes unitários
- Docker / docker compose (Postgres, Kafka, Zookeeper, RabbitMQ)

