package tech.simter.r2dbc;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.data.convert.ConverterBuilder;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Set;
import java.util.function.Function;

/**
 * A bidirectional converter between source and target.
 *
 * <p>
 * For {@link R2dbcConfiguration} to register it automatically.
 *
 * @param <S> the source type
 * @param <T> the target type
 */
public interface SimterR2dbcBiConverter<S, T> extends SimterR2dbcConverter<S, T> {
  /**
   * The source type
   *
   * @return the source type
   */
  @NonNull
  Class<S> getSourceType();

  /**
   * The target type
   *
   * @return the target type
   */
  @NonNull
  Class<T> getTargetType();

  /**
   * A function for convert source type value to target type value
   *
   * @return the function
   */
  @NonNull
  Function<? super S, ? extends T> getSourceToTarget();

  /**
   * A function for convert target type value to source type value
   *
   * @return the function
   */
  @NonNull
  Function<? super T, ? extends S> getTargetToSource();

  /**
   * Convert source type value to target type value.
   *
   * @param sourceValue the source value
   * @return the converted target type value
   */
  @Override
  @Nullable
  T convert(@Nullable S sourceValue);

  /**
   * Convert target type value to source type value.
   *
   * @param targetValue the target value
   * @return the converted source type value
   */
  @Nullable
  S convertInverted(@Nullable T targetValue);

  /**
   * Generate this bidirectional converter to a {@link GenericConverter} collection
   *
   * @return the converters
   */
  @NonNull
  default Set<GenericConverter> getConverters() {
    return ConverterBuilder
      .writing(getSourceType(), getTargetType(), getSourceToTarget())
      .andReading(getTargetToSource())
      .getConverters();
  }

  /**
   * Generate this bidirectional's forward-converter to a spring {@link Converter} for writing source value
   *
   * @return the forward-converter
   */
  @NonNull
  default Converter<S, T> getWritingConverter() {
    return sourceValue -> getSourceToTarget().apply(sourceValue);
  }

  /**
   * Generate this bidirectional's inverted-converter to a spring {@link Converter} for reading target value
   *
   * @return the inverted-converter
   */
  @NonNull
  default Converter<T, S> getReadingConverter() {
    return targetValue -> getTargetToSource().apply(targetValue);
  }
}