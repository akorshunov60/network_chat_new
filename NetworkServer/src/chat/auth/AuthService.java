package chat.auth;

public interface AuthService {

    String getUsernameByLoginAndPassword(String login, String password);
}
