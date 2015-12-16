import java.util.Collection;
import org.sonar.api.security.ExternalGroupsProvider;

public class FakeGroupsProvider extends ExternalGroupsProvider {

  private final FakeAuthenticator instance;

  public FakeGroupsProvider(FakeAuthenticator instance) {
    this.instance = instance;
  }

  @Override
  public Collection<String> doGetGroups(String username) {
    return instance.doGetGroups(username);
  }

}
