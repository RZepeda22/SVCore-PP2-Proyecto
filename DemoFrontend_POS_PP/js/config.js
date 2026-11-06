// Dirección de la API de SV-Core.
// Por defecto usa el mismo host desde el que se abrió el frontend, en el puerto 8080
// (localhost:5500 -> localhost:8080). Cámbiala aquí si la API corre en otro lugar.
window.SVCORE_CONFIG = {
    apiUrl: window.location.protocol === 'file:'
        ? 'http://localhost:8080'
        : window.location.protocol + '//' + window.location.hostname + ':8080'
};
