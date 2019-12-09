package tech.simter.r2dbc;

import org.springframework.core.convert.converter.Converter;

/**
 * A marker converter interface same with {@link Converter}.
 *
 * @param <S> the source type
 * @param <T> the target type
 */
public interface R2dbcCustomConverter<S, T> extends Converter<S, T> {
}