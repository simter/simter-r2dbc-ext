package tech.simter.r2dbc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceInitializationMode;

import java.util.List;
import java.util.Map;

/**
 * See [DataSourceProperties]
 * See ['50.5 @ConfigurationProperties'](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-kotlin.html#boot-features-kotlin-configuration-properties)
 *
 * @author RJ
 * @deprecated since 3.0.0-M3 for removal in 3.0.0 in favor of use
 * {@link org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties}
 */
@Deprecated
@ConfigurationProperties(prefix = "spring.datasource")
public class R2dbcProperties {
  private String protocol; // for h2: file|mem
  private String platform;
  private String name;
  private String host;
  private Integer port;
  private String username;
  private String password;
  private String url; // for h2
  private DataSourceInitializationMode initializationMode;
  private List<String> schema;
  private List<String> data;
  private Map<String, String> options;

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public String getPlatform() {
    return platform;
  }

  public void setPlatform(String platform) {
    this.platform = platform;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public DataSourceInitializationMode getInitializationMode() {
    return initializationMode;
  }

  public void setInitializationMode(DataSourceInitializationMode initializationMode) {
    this.initializationMode = initializationMode;
  }

  public List<String> getSchema() {
    return schema;
  }

  public void setSchema(List<String> schema) {
    this.schema = schema;
  }

  public List<String> getData() {
    return data;
  }

  public void setData(List<String> data) {
    this.data = data;
  }

  public Map<String, String> getOptions() {
    return options;
  }

  public void setOptions(Map<String, String> options) {
    this.options = options;
  }

  @Override
  public String toString() {
    return "R2dbcProperties{" +
      "platform='" + platform + "'" +
      ", protocol='" + protocol + "'" +
      ", name='" + name + "'" +
      ", host='" + host + "'" +
      ", port=" + port +
      ", username='" + username + "'" +
      ", password='***'" +
      ", url='" + url + "'" +
      ", initializationMode=" + initializationMode +
      ", schema=" + schema +
      ", options=" + options +
      ", data=" + data +
      '}';
  }
}