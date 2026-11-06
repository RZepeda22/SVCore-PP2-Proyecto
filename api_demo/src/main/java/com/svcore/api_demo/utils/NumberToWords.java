package com.svcore.api_demo.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Monto en letras para el campo "totalLetras" del DTE.
 * Ejemplo: 1848.02 -> "MIL OCHOCIENTOS CUARENTA Y OCHO 02/100 USD". Admite montos menores a mil millones.
 */
public final class NumberToWords {

	private static final String[] UNITS = { "", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO",
			"NUEVE", "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISÉIS", "DIECISIETE", "DIECIOCHO",
			"DIECINUEVE", "VEINTE", "VEINTIUNO", "VEINTIDÓS", "VEINTITRÉS", "VEINTICUATRO", "VEINTICINCO",
			"VEINTISÉIS", "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE" };
	private static final String[] TENS = { "", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA",
			"OCHENTA", "NOVENTA" };
	private static final String[] HUNDREDS = { "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS",
			"QUINIENTOS", "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS" };

	private NumberToWords() {
	}

	public static String amount(BigDecimal value) {
		BigDecimal rounded = value.setScale(2, RoundingMode.HALF_UP);
		boolean negative = rounded.signum() < 0;
		BigDecimal absolute = rounded.abs();
		long integer = absolute.longValue();
		if (integer >= 1_000_000_000L) {
			throw new IllegalArgumentException("Monto fuera de rango: " + value);
		}
		int cents = absolute.subtract(BigDecimal.valueOf(integer)).movePointRight(2).intValue();
		String words = integer == 0 ? "CERO" : apocope(convert(integer));
		return (negative ? "MENOS " : "") + words + " " + String.format("%02d", cents) + "/100 USD";
	}

	private static String convert(long n) {
		StringBuilder text = new StringBuilder();
		int millions = (int) (n / 1_000_000);
		int thousands = (int) (n / 1_000 % 1_000);
		int rest = (int) (n % 1_000);

		if (millions > 0) {
			text.append(millions == 1 ? "UN MILLÓN" : apocope(belowThousand(millions)) + " MILLONES");
		}
		if (thousands > 0) {
			if (!text.isEmpty()) text.append(' ');
			text.append(thousands == 1 ? "MIL" : apocope(belowThousand(thousands)) + " MIL");
		}
		if (rest > 0) {
			if (!text.isEmpty()) text.append(' ');
			text.append(belowThousand(rest));
		}
		return text.toString();
	}

	private static String belowThousand(int n) {
		if (n == 100) return "CIEN";
		String hundreds = HUNDREDS[n / 100];
		String rest = belowHundred(n % 100);
		if (hundreds.isEmpty()) return rest;
		return rest.isEmpty() ? hundreds : hundreds + " " + rest;
	}

	private static String belowHundred(int n) {
		if (n < 30) return UNITS[n];
		int units = n % 10;
		return units == 0 ? TENS[n / 10] : TENS[n / 10] + " Y " + UNITS[units];
	}

	/** "UNO" se apocopa delante de MIL, MILLONES y de la moneda: VEINTIÚN MIL, TREINTA Y UN USD. */
	private static String apocope(String words) {
		if (words.endsWith("VEINTIUNO")) return words.substring(0, words.length() - "VEINTIUNO".length()) + "VEINTIÚN";
		if (words.endsWith("UNO")) return words.substring(0, words.length() - 1);
		return words;
	}
}
