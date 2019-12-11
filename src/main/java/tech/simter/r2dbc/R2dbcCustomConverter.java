package tech.simter.r2dbc;

import org.springframework.core.convert.converter.Converter;

/**
 * A marker converter interface same with {@link Converter}.
 * <p>
 * Just for {@link R2dbcConfiguration} to register it automatically.
 *
 * @param <S> the source type
 * @param <T> the target type
 */
public interface R2dbcCustomConverter<S, T> extends Converter<S, T> {
}