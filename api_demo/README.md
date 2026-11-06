# SV-Core API

API de ventas, inventario y facturación electrónica (DTE) para Adventure Works.
Spring Boot 4 · Java 21 · MySQL 8.4. El frontend está en `../DemoFrontend_POS_PP`.

## Levantar todo en local

1. Copiar `.env.example` como `.env` y completar `DB_ROOT_PASSWORD`, `DB_PASS`, `JWT_SECRET` y `DEMO_PASSWORD`.
2. Levantar la base de datos (MySQL, puerto 3306) y el frontend (nginx, puerto 5500):

   ```
   docker compose up -d
   ```

3. Arrancar la API (puerto 8080; aplica las migraciones y crea los datos de demostración):

   ```
   .\gradlew.bat bootRun
   ```

4. Abrir http://localhost:5500 e iniciar sesión con la contraseña de `DEMO_PASSWORD`:

   | Usuario | Rol | Puede |
   |---|---|---|
   | `ricardo` | Vendedor (VEND) | Vender, clientes, consultar y reintentar DTE, notas de crédito |
   | `marco` | Administrador (ADMN) | Todo: usuarios, bitácora, reportes, configuración fiscal, invalidar DTE |
   | `josemaria` | Encargado de inventario (INVE) | Catálogo, entradas, ajustes y kardex |

Swagger: http://localhost:8080/swagger-ui.html

> El frontend también funciona con Live Server de VS Code (puerto 5500). Si se sirve en otro origen,
> agregarlo a `CORS_ORIGINS` en el `.env`.

## Endpoints por sprint

Todas las respuestas usan `{ "message": ..., "data": ... }`. Requieren `Authorization: Bearer <token>` salvo el login.

### Sprint 1 · Autenticación, usuarios y bitácora

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| POST | `/api/auth/login` | público | JWT de 15 min; bloqueo tras 5 intentos fallidos |
| GET | `/api/user/me` | todos | Datos del usuario actual |
| PATCH | `/api/user/change-password` | todos | Cambiar la propia contraseña |
| GET · POST · PATCH | `/api/user/all` · `/save` · `/edit` · `/reset-password` · `/unlock` | ADMN | CU-10 Usuarios y roles |
| GET | `/api/rol/all` | ADMN | Roles |
| GET | `/api/audit/all?username&action&from&to&page&size` | ADMN | RNF-06 Bitácora |
| GET | `/api/audit/verify` | ADMN | Recalcula la cadena de hashes y detecta alteraciones |

Política de contraseñas: mínimo 10 caracteres con mayúscula, minúscula, número y símbolo, sin contener el usuario.

### Sprints 2-3 · Productos, clientes e inventario

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/product/all?category&search&includeInactive` | todos | Catálogo |
| GET | `/api/product/?productId=` | todos | Un producto |
| POST · PATCH | `/api/product/save` · `/api/product/edit?productId=` | INVE, ADMN | CU-05 Crear y editar productos |
| GET · POST | `/api/category/all` · `/api/category/save` | todos · INVE, ADMN | Categorías |
| GET · POST · PATCH | `/api/customer/all` · `/` · `/save` · `/edit` | todos · VEND, ADMN | CU-08 Clientes |
| POST | `/api/inventory/entry` | INVE, ADMN | Entrada de mercadería |
| POST | `/api/inventory/adjustment` | INVE, ADMN | Ajuste por conteo físico |
| GET | `/api/inventory/movements?productId&from&to&limit` | INVE, ADMN | Kardex |
| GET | `/api/inventory/alerts` | INVE, ADMN | Productos bajo el mínimo |

### Sprint 4 · Ventas

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| POST | `/api/sale/save` | VEND, ADMN | CU-02: venta + salida de inventario + DTE en un clic |

### Sprints 5-6 · DTE generado, firmado y enviado

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/dte/all?status` · `/api/dte/?dteId=` | VEND, ADMN | Consulta |
| POST | `/api/dte/retry?dteId=` | VEND, ADMN | Reintentar rechazado (con datos corregidos) o en contingencia |
| GET | `/api/dte/json?dteId=` | VEND, ADMN | JSON del DTE según el esquema de Hacienda |
| GET | `/api/dte/signed?dteId=` | VEND, ADMN | Documento firmado (JWS RS512) tal como se transmitió |
| GET | `/api/dte/verify-signature?dteId=` | VEND, ADMN | Verifica la firma y que el JSON no se alteró |
| GET · PUT | `/api/config/emitter` | todos · ADMN | CU-09 Configuración fiscal del emisor |
| GET · PUT | `/api/config/hacienda-simulator?available=` | ADMN | Encender o apagar la simulación de Hacienda |

Flujo de emisión: generar JSON → firmar (JWS RS512) → transmitir → SELLADO / RECHAZADO / CONTINGENCIA.
Si Hacienda no responde, el DTE pasa a modelo diferido dentro de un **evento de contingencia**; la cola
(cada `HACIENDA_RETRY_MS`) transmite primero el evento y luego el lote de documentos.

### Sprint 7 · Anulación, notas de crédito y reportes

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| POST | `/api/dte/invalidate` | ADMN | CU-04: evento de invalidación firmado; revierte el inventario |
| POST | `/api/dte/credit-note` | VEND, ADMN | Nota de Crédito (devolución) sobre un CCF sellado |
| GET | `/api/report/summary?from&to` | ADMN | CU-06: ventas netas, notas de crédito y anulados |
| GET | `/api/report/sales-by-day` · `/sales-by-payment` · `/top-products` | ADMN | Reportes |
| GET | `/api/report/iva-book` · `/api/report/iva-book/csv` | ADMN | Libro de ventas (IVA), también en CSV |

### Sprint 8 · Pruebas

```
.\gradlew.bat test
```

31 pruebas: unitarias (monto en letras, política de contraseñas, JSON del DTE, firma, simulación de Hacienda)
y de integración contra un **MySQL real en Docker** (Testcontainers) que recorren todos los sprints por HTTP:
seguridad por rol, usuarios, catálogo y kardex, factura sellada con firma válida, CCF rechazado y corregido,
contingencia con evento y lote, nota de crédito, invalidación con reingreso de inventario, reportes y
detección de alteraciones en la bitácora. Requieren Docker en ejecución.

## Base de datos

- Las tablas las crea Flyway desde `src/main/resources/db/migration`. Hibernate solo valida
  (`ddl-auto: validate`). Un cambio de esquema = un archivo `V4__...sql` nuevo.
- La API se conecta con `svcore_app` (`docker/mysql/01-app-user.sh`): no puede borrar registros (DELETE)
  ni eliminar tablas (DROP). `root` solo lo usa el contenedor. La conexión va cifrada con TLS.
- `docker compose down` detiene los contenedores sin borrar datos; `docker compose down -v` borra la base.

## Pendiente para producción

- **Firma:** configurar `DTE_KEYSTORE_PATH` con el certificado del emisor. Sin él, se firma con una llave
  de demostración guardada en `.svcore/` (fuera del repositorio), sin validez tributaria.
- **Hacienda real:** `HACIENDA_MODE=api` con las credenciales de la API. El cliente (`HaciendaServiceApiImpl`)
  no se ha probado contra Hacienda; confirmar rutas y campos con el manual técnico vigente.
- **Esquemas JSON:** validar los documentos generados contra los esquemas oficiales vigentes de Hacienda.
  Los códigos de actividad, departamento y municipio de los datos de demostración son ilustrativos.
- **Nota de Débito:** el tipo existe en el modelo, pero no tiene endpoint de emisión.
