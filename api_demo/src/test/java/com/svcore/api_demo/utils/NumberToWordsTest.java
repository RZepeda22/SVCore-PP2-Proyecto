package com.svcore.api_demo.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class NumberToWordsTest {

	private static String words(String amount) {
		return NumberToWords.amount(new BigDecimal(amount));
	}

	@Test
	void unidadesYApocope() {
		assertEquals("CERO 00/100 USD", words("0"));
		assertEquals("UN 00/100 USD", words("1"));
		assertEquals("QUINCE 50/100 USD", words("15.5"));
		assertEquals("VEINTIÚN 00/100 USD", words("21"));
		assertEquals("TREINTA Y UN 10/100 USD", words("31.10"));
	}

	@Test
	void centenas() {
		assertEquals("CIEN 00/100 USD", words("100"));
		assertEquals("CIENTO UN 00/100 USD", words("101"));
		assertEquals("QUINIENTOS 00/100 USD", words("500"));
		assertEquals("OCHOCIENTOS DOCE 45/100 USD", words("812.45"));
	}

	@Test
	void milesYMillones() {
		assertEquals("MIL OCHOCIENTOS CUARENTA Y OCHO 02/100 USD", words("1848.02"));
		assertEquals("VEINTIÚN MIL 00/100 USD", words("21000"));
		assertEquals("UN MILLÓN 00/100 USD", words("1000000"));
		assertEquals("DOS MILLONES QUINIENTOS MIL 99/100 USD", words("2500000.99"));
	}

	@Test
	void redondeaLosCentavos() {
		assertEquals("DIEZ 01/100 USD", words("10.005"));
	}

	@Test
	void rechazaMontosFueraDeRango() {
		assertThrows(IllegalArgumentException.class, () -> words("1000000000"));
	}
}
