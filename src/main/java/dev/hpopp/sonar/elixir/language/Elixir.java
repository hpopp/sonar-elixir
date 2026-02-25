package dev.hpopp.sonar.elixir.language;

import org.sonar.api.resources.AbstractLanguage;

public class Elixir extends AbstractLanguage {

    public static final String KEY = "elixir";
    public static final String NAME = "Elixir";
    public static final String[] FILE_SUFFIXES = { ".ex", ".exs" };

    public Elixir() {
        super(KEY, NAME);
    }

    @Override
    public String[] getFileSuffixes() {
        return FILE_SUFFIXES;
    }
}
