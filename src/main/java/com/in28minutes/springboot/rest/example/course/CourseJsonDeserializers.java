package com.in28minutes.springboot.rest.example.course;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public final class CourseJsonDeserializers {

  private CourseJsonDeserializers() {}

  public static class Text extends ValueDeserializer<String> {

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) {
      if (parser.currentToken() != JsonToken.VALUE_STRING) {
        return context.reportInputMismatch(String.class, "Une chaîne de caractères est requise.");
      }
      return parser.getString();
    }
  }

  public static class Date extends ValueDeserializer<LocalDate> {

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext context) {
      if (parser.currentToken() != JsonToken.VALUE_STRING) {
        return context.reportInputMismatch(LocalDate.class, "Une date YYYY-MM-DD est requise.");
      }
      String value = parser.getString();
      if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
        return context.reportInputMismatch(LocalDate.class, "Une date YYYY-MM-DD est requise.");
      }
      try {
        return LocalDate.parse(value);
      } catch (DateTimeParseException exception) {
        return context.reportInputMismatch(LocalDate.class, "La date doit être valide.");
      }
    }
  }
}
