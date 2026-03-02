package dev.hpopp.sonar.elixir.language;

import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

public class Elixir extends AbstractLanguage {

  public static final String KEY = "elixir";
  public static final String NAME = "Elixir";
  public static final String FILE_SUFFIXES_KEY = "sonar.elixir.file.suffixes";
  public static final String FILE_SUFFIXES_DEFAULT = ".ex,.exs";

  private final Configuration configuration;

  public Elixir(Configuration configuration) {
    super(KEY, NAME);
    this.configuration = configuration;
  }

  @Override
  public String[] getFileSuffixes() {
    String[] suffixes = configuration.getStringArray(FILE_SUFFIXES_KEY);
    if (suffixes.length == 0) {
      return FILE_SUFFIXES_DEFAULT.split(",");
    }
    return suffixes;
  }
}
