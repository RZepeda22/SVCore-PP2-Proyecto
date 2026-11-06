package com.svcore.api_demo.service;

/**
 * Firma electrónica de los documentos que se envían a Hacienda: JWS compacto con RS512
 * (el mismo formato que produce el firmador del Ministerio de Hacienda).
 */
public interface SignatureService {

	/** Firma el JSON y devuelve el JWS compacto (encabezado.contenido.firma). */
	String sign(String json);

	/** true si la firma corresponde a la llave pública del emisor. */
	boolean verify(String jws);

	/** Contenido (JSON) de un JWS. */
	String payload(String jws);

	String algorithm();

	String keyId();
}
