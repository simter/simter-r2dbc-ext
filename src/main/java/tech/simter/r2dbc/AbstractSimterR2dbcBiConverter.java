package tech.simter.r2dbc;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.function.Function;

/**
 * An abstract bidirectional converter between source and target.
 *
 * @param <S> the source type
 * @param <T> the target type
 */
public abstract class AbstractSimterR2dbcBiConverter<S, T> implements SimterR2dbcBiConverter<S, T> {
  private final Function<? super S, ? extends T> sourceToTarget;
  private final Function<? super T, ? extends S> targetToSource;
  private final Class<S> sourceType;
  private final Class<T> targetType;

  public AbstractSimterR2dbcBiConverter(
    @NonNull Class<S> sourceType,
    @NonNull Class<T> targetType,
    @NonNull Function<? super S, ? extends T> sourceToTarget,
    @NonNull Function<? super T, ? extends S> targetToSource
  ) {
    Assert.notNull(sourceType, "Source type must not be null!");
    Assert.notNull(targetType, "Target type must not be null!");
    Assert.notNull(sourceToTarget, "Writing conversion function must not be null!");
    Assert.notNull(targetToSource, "Reading conversion function must not be null!");
    this.sourceType = sourceType;
    this.targetType = targetType;
    this.sourceToTarget = sourceToTarget;
    this.targetToSource = targetToSource;
  }

  @Override
  public Class<S> getSourceType() {
    return sourceType;
  }

  @Override
  public Class<T> getTargetType() {
    return targetType;
  }

  @Override
  public Function<? super S, ? extends T> getSourceToTarget() {
    return sourceToTarget;
  }

  @Override
  public Function<? super T, ? extends S> getTargetToSource() {
    return targetToSource;
  }

  @Override
  public T convert(S sourceValue) {
    return sourceToTarget.apply(sourceValue);
  }

  @Override
  public S convertInverted(T targetValue) {
    return targetToSource.apply(targetValue);
  }
}