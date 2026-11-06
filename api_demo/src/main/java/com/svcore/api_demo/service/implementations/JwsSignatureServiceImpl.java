package com.svcore.api_demo.service.implementations;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.svcore.api_demo.service.SignatureService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Firma RS512. La llave sale de:
 * <ol>
 *   <li>Un almacén PKCS12 con el certificado del emisor (DTE_KEYSTORE_PATH), para producción.</li>
 *   <li>Si no hay almacén: una llave RSA de demostración generada la primera vez y guardada en
 *       DTE_DEMO_KEY_PATH (fuera del repositorio). Sirve para mostrar el flujo, no tiene validez tributaria.</li>
 * </ol>
 */
@Slf4j
@Service
public class JwsSignatureServiceImpl implements SignatureService {

	private static final JWSAlgorithm ALGORITHM = JWSAlgorithm.RS512;
	private static final String DEMO_KEY_ID = "svcore-demo";

	private final String keystorePath;
	private final String keystorePassword;
	private final String keyAlias;
	private final String demoKeyPath;

	private RSAPrivateKey privateKey;
	private RSAPublicKey publicKey;
	private String keyId;

	public JwsSignatureServiceImpl(@Value("${svcore.dte.keystore.path:}") String keystorePath,
								   @Value("${svcore.dte.keystore.password:}") String keystorePassword,
								   @Value("${svcore.dte.keystore.alias:}") String keyAlias,
								   @Value("${svcore.dte.demo-key-path:.svcore/dte-demo-key.json}") String demoKeyPath) {
		this.keystorePath = keystorePath;
		this.keystorePassword = keystorePassword;
		this.keyAlias = keyAlias;
		this.demoKeyPath = demoKeyPath;
	}

	@PostConstruct
	public void loadKey() {
		try {
			if (keystorePath != null && !keystorePath.isBlank()) {
				loadKeystore();
			} else {
				loadOrCreateDemoKey();
			}
		} catch (Exception e) {
			throw new IllegalStateException("No se pudo cargar la llave de firma de DTE: " + e.getMessage(), e);
		}
	}

	private void loadKeystore() throws Exception {
		KeyStore keystore = KeyStore.getInstance("PKCS12");
		char[] password = keystorePassword.toCharArray();
		try (InputStream in = Files.newInputStream(Path.of(keystorePath))) {
			keystore.load(in, password);
		}
		String alias = keyAlias == null || keyAlias.isBlank() ? keystore.aliases().nextElement() : keyAlias;
		PrivateKey key = (PrivateKey) keystore.getKey(alias, password);
		Certificate certificate = keystore.getCertificate(alias);
		if (!(key instanceof RSAPrivateKey rsaPrivate) || !(certificate.getPublicKey() instanceof RSAPublicKey rsaPublic)) {
			throw new IllegalStateException("El certificado del emisor debe usar una llave RSA");
		}
		privateKey = rsaPrivate;
		publicKey = rsaPublic;
		keyId = alias;
		log.info("Firma de DTE con el certificado del almacén {} (alias {})", keystorePath, alias);
	}

	private void loadOrCreateDemoKey() throws IOException, ParseException, JOSEException {
		Path path = Path.of(demoKeyPath);
		RSAKey key;
		if (Files.exists(path)) {
			key = RSAKey.parse(Files.readString(path, StandardCharsets.UTF_8));
		} else {
			key = new RSAKeyGenerator(2048).keyID(DEMO_KEY_ID).generate();
			if (path.getParent() != null) Files.createDirectories(path.getParent());
			Files.writeString(path, key.toJSONString(), StandardCharsets.UTF_8);
		}
		privateKey = key.toRSAPrivateKey();
		publicKey = key.toRSAPublicKey();
		keyId = key.getKeyID();
		log.warn("Firma de DTE con llave de DEMOSTRACIÓN ({}). Configura DTE_KEYSTORE_PATH con el certificado del emisor "
				+ "para producción.", path.toAbsolutePath());
	}

	@Override
	public String sign(String json) {
		try {
			JWSObject jws = new JWSObject(new JWSHeader.Builder(ALGORITHM).keyID(keyId).build(), new Payload(json));
			jws.sign(new RSASSASigner(privateKey));
			return jws.serialize();
		} catch (JOSEException e) {
			throw new IllegalStateException("No se pudo firmar el documento", e);
		}
	}

	@Override
	public boolean verify(String jws) {
		if (jws == null || jws.isBlank()) return false;
		try {
			return JWSObject.parse(jws).verify(new RSASSAVerifier(publicKey));
		} catch (ParseException | JOSEException e) {
			return false;
		}
	}

	@Override
	public String payload(String jws) {
		try {
			return JWSObject.parse(jws).getPayload().toString();
		} catch (ParseException e) {
			throw new IllegalArgumentException("El documento firmado no tiene formato JWS", e);
		}
	}

	@Override
	public String algorithm() {
		return ALGORITHM.getName();
	}

	@Override
	public String keyId() {
		return keyId;
	}
}
