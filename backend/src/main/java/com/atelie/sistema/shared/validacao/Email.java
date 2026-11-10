package com.atelie.sistema.shared.validacao;

import java.util.regex.Pattern;

public final class Email {

    private static final String PARTE_LOCAL = "[a-zA-Z0-9!#$%&'*+/=?^_\u0060{|}~-]+";
    private static final String ROTULO_DOMINIO = "[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?";
    private static final Pattern FORMATO = Pattern.compile(
            PARTE_LOCAL + "(?:\\." + PARTE_LOCAL + ")*@"
                    + ROTULO_DOMINIO + "(?:\\." + ROTULO_DOMINIO + ")+");

    private Email() {
    }

    public static boolean valido(String valor) {
        return valor != null && valor.length() <= 254 && valor.indexOf('@') <= 64
                && FORMATO.matcher(valor).matches();
    }
}
