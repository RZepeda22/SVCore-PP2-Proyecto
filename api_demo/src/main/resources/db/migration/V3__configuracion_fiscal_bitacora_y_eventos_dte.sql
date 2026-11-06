-- Sprints 1, 5-6 y 7: configuración fiscal, bitácora, documento firmado, contingencia, invalidación y notas de crédito.

-- CU-09: configuración fiscal del emisor (una sola fila, id = 1)
CREATE TABLE emitter_config (
    id                     INT          NOT NULL,
    business_name          VARCHAR(250) NOT NULL,
    trade_name             VARCHAR(150) NOT NULL,
    nit                    VARCHAR(17)  NOT NULL,
    nrc                    VARCHAR(10)  NOT NULL,
    economic_activity_code VARCHAR(6)   NOT NULL,
    economic_activity      VARCHAR(150) NOT NULL,
    establishment_type     VARCHAR(2)   NOT NULL,
    establishment_name     VARCHAR(100) NOT NULL,
    department_code        VARCHAR(2)   NOT NULL,
    municipality_code      VARCHAR(2)   NOT NULL,
    address_detail         VARCHAR(200) NOT NULL,
    phone                  VARCHAR(30)  NOT NULL,
    email                  VARCHAR(100) NOT NULL,
    establishment_code     VARCHAR(4)   NOT NULL,
    point_of_sale_code     VARCHAR(4)   NOT NULL,
    cash_register          VARCHAR(50)  NOT NULL,
    environment            VARCHAR(2)   NOT NULL,
    updated_at             DATETIME(6)  NOT NULL,
    CONSTRAINT pk_emitter_config PRIMARY KEY (id),
    CONSTRAINT ck_emitter_config_single CHECK (id = 1),
    CONSTRAINT ck_emitter_config_environment CHECK (environment IN ('00', '01'))
);

-- Datos de demostración del emisor (códigos de actividad y ubicación ilustrativos)
INSERT INTO emitter_config VALUES (
    1, 'Adventure Works, S.A. de C.V.', 'Adventure Works', '0614-010120-001-2', '123456-7',
    '47630', 'Venta al por menor de artículos deportivos', '02', 'Sucursal Central', '06', '14',
    'Colonia Escalón, San Salvador', '2200-0000', 'facturacion@adventureworks.demo',
    'M001', 'P002', 'Caja 02', '00', NOW(6)
);

-- RNF-06: bitácora. Cada registro guarda el hash del anterior (cadena SHA-256):
-- si alguien modifica o borra un registro directamente en la base, la verificación lo detecta.
CREATE TABLE audit_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    occurred_at   DATETIME(6)  NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    action        VARCHAR(40)  NOT NULL,
    entity        VARCHAR(40)  NULL,
    entity_id     VARCHAR(36)  NULL,
    detail        VARCHAR(500) NULL,
    ip_address    VARCHAR(45)  NULL,
    previous_hash VARCHAR(64)  NOT NULL,
    hash          VARCHAR(64)  NOT NULL,
    CONSTRAINT pk_audit_log PRIMARY KEY (id)
);

CREATE INDEX ix_audit_log_occurred_at ON audit_log (occurred_at);
CREATE INDEX ix_audit_log_username ON audit_log (username);

-- Evento de contingencia: agrupa los DTE emitidos mientras Hacienda no respondía
CREATE TABLE contingency_event (
    id               CHAR(36)     NOT NULL,
    generation_code  VARCHAR(36)  NOT NULL,
    contingency_type INT          NOT NULL,
    reason           VARCHAR(500) NULL,
    started_at       DATETIME(6)  NOT NULL,
    ended_at         DATETIME(6)  NULL,
    status           VARCHAR(20)  NOT NULL,
    reception_stamp  VARCHAR(100) NULL,
    json_document    LONGTEXT     NULL,
    signed_document  LONGTEXT     NULL,
    created_at       DATETIME(6)  NOT NULL,
    CONSTRAINT pk_contingency_event PRIMARY KEY (id),
    CONSTRAINT uq_contingency_event_code UNIQUE (generation_code)
);

-- Documento JSON, firma, modelo de facturación y relaciones del DTE
ALTER TABLE dte
    ADD COLUMN model_type           INT      NOT NULL DEFAULT 1,
    ADD COLUMN operation_type       INT      NOT NULL DEFAULT 1,
    ADD COLUMN related_dte_id       CHAR(36) NULL,
    ADD COLUMN contingency_event_id CHAR(36) NULL,
    ADD COLUMN json_document        LONGTEXT NULL,
    ADD COLUMN signed_document      LONGTEXT NULL,
    ADD CONSTRAINT fk_dte_related FOREIGN KEY (related_dte_id) REFERENCES dte (id),
    ADD CONSTRAINT fk_dte_contingency_event FOREIGN KEY (contingency_event_id) REFERENCES contingency_event (id);

-- CU-04: evento de invalidación (anulación) de un DTE sellado
CREATE TABLE invalidation_event (
    id                CHAR(36)     NOT NULL,
    dte_id            CHAR(36)     NOT NULL,
    generation_code   VARCHAR(36)  NOT NULL,
    invalidation_type INT          NOT NULL,
    reason            VARCHAR(250) NOT NULL,
    requested_by      CHAR(36)     NOT NULL,
    reception_stamp   VARCHAR(100) NULL,
    json_document     LONGTEXT     NOT NULL,
    signed_document   LONGTEXT     NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    CONSTRAINT pk_invalidation_event PRIMARY KEY (id),
    CONSTRAINT uq_invalidation_event_dte UNIQUE (dte_id),
    CONSTRAINT uq_invalidation_event_code UNIQUE (generation_code),
    CONSTRAINT fk_invalidation_event_dte FOREIGN KEY (dte_id) REFERENCES dte (id),
    CONSTRAINT fk_invalidation_event_user FOREIGN KEY (requested_by) REFERENCES app_user (id),
    CONSTRAINT ck_invalidation_event_type CHECK (invalidation_type IN (1, 2, 3))
);

-- Motivo de la nota de crédito u otras observaciones de la transacción
ALTER TABLE sale ADD COLUMN notes VARCHAR(250) NULL;

-- Datos del receptor que exige el JSON del Crédito Fiscal
ALTER TABLE customer
    ADD COLUMN economic_activity_code VARCHAR(6)   NULL,
    ADD COLUMN economic_activity      VARCHAR(150) NULL,
    ADD COLUMN phone                  VARCHAR(30)  NULL,
    ADD COLUMN department_code        VARCHAR(2)   NULL,
    ADD COLUMN municipality_code      VARCHAR(2)   NULL;

-- Completa los clientes de demostración (valores ilustrativos)
UPDATE customer SET economic_activity_code = '47190', economic_activity = 'Venta al por menor en comercios no especializados',
    department_code = '06', municipality_code = '14' WHERE nrc = '220145-3';
UPDATE customer SET nit = '0614-150395-101-4', economic_activity_code = '62010', economic_activity = 'Programación informática',
    department_code = '06', municipality_code = '14', address = 'San Salvador, San Salvador' WHERE nrc = '118934-7';
UPDATE customer SET nit = '0614-220788-103-9', economic_activity_code = '46900', economic_activity = 'Venta al por mayor de otros productos',
    department_code = '06', municipality_code = '14', address = 'Antiguo Cuscatlán, La Libertad' WHERE nrc = '187654-2';
UPDATE customer SET nit = '0614-010199-105-2', economic_activity_code = '47520', economic_activity = 'Venta al por menor de artículos de ferretería',
    department_code = '06', municipality_code = '14', address = 'Soyapango, San Salvador' WHERE nrc = '990000-0';
