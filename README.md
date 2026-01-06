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

User (1, n) Order
Order (1, n) OrderItem
OrdemItem (n, 1) Product
User (n, m) Role

{Um usuário pode ter várias orderns.
Uma ordem pode ter apenas um usuário.}

{Uma order pode ter vários itens order.
Um item order pode ter apenas uma order}

{Um item order pode ter apenas um produto real.
Um produto real pode ter vários ietm order.}

{Um user pode ter uma ou várias permissões.
Uma permissão pode ter nenhum ou vários users.}

## Schema Database

user {
    id uuid
    username
    passwordHash
    email
    revoked boolean
    updated_at
    created_at
}

role {
    id uuid
    name
    updated_at
    created_at
}

product {
    id uuid
    user_id uuid
    name
    description
    price
    stock
    status default 'active' (pending, active, disable)
    updated_at
    created_at
}

order {
    id
    user_id uuid
    status (pending, canceled, payed)
    total
    updated_at
    created_at
}

order_item {
    id
    order_id
    product_id
    quantity
    price
}

-- habilitar extensão para gerar UUID (pgcrypto)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- enums para status
CREATE TYPE product_status AS ENUM ('pending','active','disabled');
CREATE TYPE order_status AS ENUM ('pending','canceled','paid');

-- tabela de usuários
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  username VARCHAR(50) NOT NULL,
  password_hash TEXT NOT NULL,
  email VARCHAR(255) NOT NULL,
  revoked BOOLEAN NOT NULL DEFAULT FALSE, -- se quer marcar tokens/usuário revogado
  is_active BOOLEAN NOT NULL DEFAULT TRUE, -- recomendo para soft-delete
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT users_username_unique UNIQUE (username),
  CONSTRAINT users_email_unique UNIQUE (email)
);

-- roles e tabela de relacionamento muitos-para-muitos
CREATE TABLE roles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(50) NOT NULL UNIQUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_roles (
  user_id UUID NOT NULL,
  role_id UUID NOT NULL,
  granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, role_id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT
);

-- products
CREATE TABLE products (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id UUID NOT NULL, -- user_id do dono/vendedor
  name VARCHAR(200) NOT NULL,
  description TEXT,
  price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
  stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
  status product_status NOT NULL DEFAULT 'active',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE INDEX idx_products_owner ON products(owner_id);
CREATE INDEX idx_products_status ON products(status);

-- orders
CREATE TABLE orders (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL, -- quem fez o pedido
  status order_status NOT NULL DEFAULT 'pending',
  total NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total >= 0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);

-- order_items (guarda snapshot do preço no momento do pedido)
CREATE TABLE order_items (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id UUID NOT NULL,
  product_id UUID NOT NULL,
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
  subtotal NUMERIC(12,2) GENERATED ALWAYS AS (unit_price * quantity) STORED,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);

---

-- função genérica para atualizar updated_at
CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- anexar trigger às tabelas que usam updated_at
CREATE TRIGGER trg_users_updated BEFORE UPDATE ON users
  FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

CREATE TRIGGER trg_roles_updated BEFORE UPDATE ON roles
  FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

CREATE TRIGGER trg_products_updated BEFORE UPDATE ON products
  FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

CREATE TRIGGER trg_orders_updated BEFORE UPDATE ON orders
  FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();


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

