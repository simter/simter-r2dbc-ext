package tech.simter.r2dbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.domain.Persistable;
import org.springframework.lang.NonNull;

public class Sample implements Persistable<Integer> {
  @Id
  private final Integer id;
  private final String name;

  @PersistenceConstructor
  public Sample(@NonNull Integer id, @NonNull String name) {
    this.id = id;
    this.name = name;
  }

  public Sample withId(Integer id) {
    return id.equals(this.id) ? this : new Sample(id, this.name);
  }

  @Override
  public boolean isNew() {
    return true;
  }

  public Integer getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Sample)) return false;
    Sample other = (Sample) obj;
    return this.id.equals(other.getId()) && this.name.equals(other.getName());
  }
}
