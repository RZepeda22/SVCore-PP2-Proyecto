#!/bin/bash
# Usuario de la aplicación con privilegio mínimo (Microsoft SDL: requisitos de diseño).
# La API nunca se conecta como root. Puede leer, insertar y actualizar, y crear tablas para las
# migraciones de Flyway, pero NO puede borrar registros (DELETE) ni eliminar tablas (DROP):
# una venta o un DTE emitido no se pueden borrar desde la aplicación.
# Lo ejecuta la imagen oficial de MySQL solo la primera vez que se inicializa el volumen.

mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE USER IF NOT EXISTS '${APP_USER}'@'%' IDENTIFIED BY '${APP_PASSWORD}';
GRANT SELECT, INSERT, UPDATE, CREATE, ALTER, INDEX, REFERENCES
    ON \`${MYSQL_DATABASE}\`.* TO '${APP_USER}'@'%';
FLUSH PRIVILEGES;
SQL

echo "Usuario de la aplicación ${APP_USER} creado con privilegio mínimo."
