import java.util.Arrays;
import java.util.List;
import org.sonar.api.SonarPlugin;

public class SecurityPlugin extends SonarPlugin {

  public List getExtensions() {
    return Arrays.asList(FakeRealm.class, FakeAuthenticator.class);
  }

}
