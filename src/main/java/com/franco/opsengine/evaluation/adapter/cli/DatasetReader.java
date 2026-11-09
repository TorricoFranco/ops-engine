package com.franco.opsengine.evaluation.adapter.cli;

import com.franco.opsengine.evaluation.domain.LabelledTicket;
import com.franco.opsengine.shared.domain.Skill;

import java.util.ArrayList;
import java.util.List;

/**
 * Lee el dataset en CSV: {@code id,title,description,expected_skill}.
 *
 * <p>Se parsea a mano en vez de sumar una librería: el formato es de cuatro columnas y el
 * único caso que importa es que la descripción traiga comas, que se resuelve con comillas.
 *
 * <p>Una fila sin etiqueta no se saltea en silencio: se informa con su número de línea. Un
 * dataset a medias daría un resultado que parece válido y no lo es.
 */
public final class DatasetReader {

    private static final String HEADER = "id,title,description,expected_skill";

    private DatasetReader() {
    }

    public static class InvalidDatasetException extends RuntimeException {
        public InvalidDatasetException(String message) {
            super(message);
        }
    }

    public static List<LabelledTicket> read(String content) {
        List<String> lines = content.lines()
                .filter(line -> !line.isBlank())
                .filter(line -> !line.stripLeading().startsWith("#"))
                .toList();

        if (lines.isEmpty()) {
            throw new InvalidDatasetException("el dataset está vacío");
        }
        if (!normalizeHeader(lines.getFirst()).equals(HEADER)) {
            throw new InvalidDatasetException(
                    "la primera línea tiene que ser el encabezado: " + HEADER);
        }

        List<LabelledTicket> dataset = new ArrayList<>();
        List<String> problems = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            int lineNumber = i + 1;
            try {
                dataset.add(parse(lines.get(i)));
            } catch (RuntimeException e) {
                problems.add("línea " + lineNumber + ": " + e.getMessage());
            }
        }

        if (!problems.isEmpty()) {
            throw new InvalidDatasetException(
                    "el dataset tiene " + problems.size() + " fila(s) con problemas:\n  "
                            + String.join("\n  ", problems));
        }
        if (dataset.isEmpty()) {
            throw new InvalidDatasetException("el dataset no tiene ninguna fila etiquetada");
        }
        return List.copyOf(dataset);
    }

    private static String normalizeHeader(String line) {
        return line.replace(" ", "").replace("﻿", "").toLowerCase(java.util.Locale.ROOT);
    }

    private static LabelledTicket parse(String line) {
        List<String> fields = splitCsv(line);
        if (fields.size() != 4) {
            throw new IllegalArgumentException("se esperaban 4 columnas y hay " + fields.size());
        }
        String expected = fields.get(3).trim();
        if (expected.isEmpty()) {
            throw new IllegalArgumentException("falta la etiqueta expected_skill");
        }
        return new LabelledTicket(fields.get(0), fields.get(1), fields.get(2), new Skill(expected));
    }

    /** CSV mínimo: comillas dobles para proteger comas, y dos comillas para una literal. */
    static List<String> splitCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    boolean escapedQuote = i + 1 < line.length() && line.charAt(i + 1) == '"';
                    if (escapedQuote) {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException("hay una comilla sin cerrar");
        }
        fields.add(current.toString().trim());
        return fields;
    }
}
