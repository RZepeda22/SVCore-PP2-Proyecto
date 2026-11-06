package com.svcore.api_demo.utils;

import java.util.List;
import java.util.stream.Collectors;

/** CSV compatible con Excel (BOM UTF-8) y protegido contra inyección de fórmulas. */
public final class CsvUtils {

	private static final String BOM = "﻿";

	private CsvUtils() {
	}

	public static String cell(Object value) {
		String text = value == null ? "" : String.valueOf(value);
		// Un valor que empieza con = + - @ se ejecutaría como fórmula al abrirlo en Excel
		if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
			text = "'" + text;
		}
		return "\"" + text.replace("\"", "\"\"") + "\"";
	}

	public static String document(List<String> header, List<List<Object>> rows) {
		StringBuilder csv = new StringBuilder(BOM);
		csv.append(header.stream().map(CsvUtils::cell).collect(Collectors.joining(","))).append("\r\n");
		rows.forEach(row -> csv.append(row.stream().map(CsvUtils::cell).collect(Collectors.joining(","))).append("\r\n"));
		return csv.toString();
	}
}
