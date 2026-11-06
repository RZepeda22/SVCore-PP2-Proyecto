-- Esquema de SV-Core (MySQL 8). Lo aplica Flyway al arrancar la API.
-- Los identificadores son UUID guardados como CHAR(36).

CREATE TABLE rol (
    id   VARCHAR(4)  NOT NULL,
    name VARCHAR(50) NOT NULL,
    CONSTRAINT pk_rol PRIMARY KEY (id)
);

CREATE TABLE app_user (
    id              CHAR(36)     NOT NULL,
    username        VARCHAR(50)  NOT NULL,
    email           VARCHAR(100) NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    active          BIT(1)       NOT NULL DEFAULT b'1',
    failed_attempts INT          NOT NULL DEFAULT 0,
    locked_until    DATETIME(6)  NULL,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_app_user PRIMARY KEY (id),
    CONSTRAINT uq_app_user_username UNIQUE (username),
    CONSTRAINT uq_app_user_email UNIQUE (email)
);

CREATE TABLE user_rol (
    user_id CHAR(36)   NOT NULL,
    rol_id  VARCHAR(4) NOT NULL,
    CONSTRAINT pk_user_rol PRIMARY KEY (user_id, rol_id),
    CONSTRAINT fk_user_rol_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_user_rol_rol  FOREIGN KEY (rol_id)  REFERENCES rol (id)
);

-- Clasificación fiscal del cliente (decisión de diseño 5)
CREATE TABLE customer (
    id            CHAR(36)     NOT NULL,
    name          VARCHAR(150) NOT NULL,
    taxpayer_type VARCHAR(20)  NOT NULL,
    nit           VARCHAR(20)  NULL,
    nrc           VARCHAR(10)  NULL,
    email         VARCHAR(100) NULL,
    address       VARCHAR(200) NULL,
    created_at    DATETIME(6)  NOT NULL,
    CONSTRAINT pk_customer PRIMARY KEY (id),
    CONSTRAINT uq_customer_nrc UNIQUE (nrc)
);

CREATE TABLE product_category (
    id   CHAR(36)    NOT NULL,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(50) NOT NULL,
    CONSTRAINT pk_product_category PRIMARY KEY (id),
    CONSTRAINT uq_product_category_code UNIQUE (code)
);

CREATE TABLE product (
    id          CHAR(36)      NOT NULL,
    code        VARCHAR(25)   NOT NULL,
    name        VARCHAR(100)  NOT NULL,
    price       DECIMAL(12,2) NOT NULL,
    stock       INT           NOT NULL,
    min_stock   INT           NOT NULL,
    category_id CHAR(36)      NOT NULL,
    active      BIT(1)        NOT NULL DEFAULT b'1',
    created_at  DATETIME(6)   NOT NULL,
    CONSTRAINT pk_product PRIMARY KEY (id),
    CONSTRAINT uq_product_code UNIQUE (code),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES product_category (id),
    -- Última barrera: aunque falle la validación de la API, la base nunca deja stock negativo
    CONSTRAINT ck_product_stock CHECK (stock >= 0),
    CONSTRAINT ck_product_price CHECK (price >= 0)
);

CREATE TABLE sale (
    id             CHAR(36)      NOT NULL,
    sale_number    INT           NOT NULL,
    seller_id      CHAR(36)      NOT NULL,
    customer_id    CHAR(36)      NULL,
    cash_register  VARCHAR(50)   NOT NULL,
    doc_type       VARCHAR(20)   NOT NULL,
    payment_method VARCHAR(20)   NOT NULL,
    subtotal       DECIMAL(12,2) NOT NULL,
    discount       DECIMAL(12,2) NOT NULL,
    tax            DECIMAL(12,2) NOT NULL,
    total          DECIMAL(12,2) NOT NULL,
    created_at     DATETIME(6)   NOT NULL,
    CONSTRAINT pk_sale PRIMARY KEY (id),
    CONSTRAINT uq_sale_number UNIQUE (sale_number),
    CONSTRAINT fk_sale_seller   FOREIGN KEY (seller_id)   REFERENCES app_user (id),
    CONSTRAINT fk_sale_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
);

CREATE TABLE sale_detail (
    id         CHAR(36)      NOT NULL,
    sale_id    CHAR(36)      NOT NULL,
    product_id CHAR(36)      NOT NULL,
    quantity   INT           NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    line_total DECIMAL(12,2) NOT NULL,
    CONSTRAINT pk_sale_detail PRIMARY KEY (id),
    CONSTRAINT fk_sale_detail_sale    FOREIGN KEY (sale_id)    REFERENCES sale (id),
    CONSTRAINT fk_sale_detail_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT ck_sale_detail_quantity CHECK (quantity > 0)
);

-- Trazabilidad de entradas y salidas de inventario (decisión de diseño 4)
CREATE TABLE inventory_movement (
    id            CHAR(36)    NOT NULL,
    product_id    CHAR(36)    NOT NULL,
    quantity      INT         NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    sale_id       CHAR(36)    NULL,
    user_id       CHAR(36)    NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    CONSTRAINT pk_inventory_movement PRIMARY KEY (id),
    CONSTRAINT fk_inventory_movement_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_inventory_movement_sale    FOREIGN KEY (sale_id)    REFERENCES sale (id),
    CONSTRAINT fk_inventory_movement_user    FOREIGN KEY (user_id)    REFERENCES app_user (id)
);

-- Documento tributario electrónico. Guarda una copia de los datos del receptor
-- para que un DTE sellado no cambie si después se edita el cliente.
CREATE TABLE dte (
    id                     CHAR(36)     NOT NULL,
    sale_id                CHAR(36)     NOT NULL,
    dte_type               VARCHAR(20)  NOT NULL,
    generation_code        VARCHAR(36)  NOT NULL,
    control_number         VARCHAR(31)  NOT NULL,
    status                 VARCHAR(20)  NOT NULL,
    environment            VARCHAR(2)   NOT NULL,
    receiver_name          VARCHAR(150) NOT NULL,
    receiver_taxpayer_type VARCHAR(20)  NULL,
    receiver_nit           VARCHAR(20)  NULL,
    receiver_nrc           VARCHAR(10)  NULL,
    receiver_email         VARCHAR(100) NULL,
    receiver_address       VARCHAR(200) NULL,
    reception_stamp        VARCHAR(100) NULL,
    rejection_code         VARCHAR(10)  NULL,
    rejection_message      VARCHAR(500) NULL,
    attempts               INT          NOT NULL,
    issued_at              DATETIME(6)  NOT NULL,
    last_attempt_at        DATETIME(6)  NULL,
    stamped_at             DATETIME(6)  NULL,
    CONSTRAINT pk_dte PRIMARY KEY (id),
    CONSTRAINT uq_dte_sale UNIQUE (sale_id),
    CONSTRAINT uq_dte_generation_code UNIQUE (generation_code),
    CONSTRAINT uq_dte_control_number UNIQUE (control_number),
    CONSTRAINT fk_dte_sale FOREIGN KEY (sale_id) REFERENCES sale (id)
);

CREATE INDEX ix_dte_status_issued_at ON dte (status, issued_at);
