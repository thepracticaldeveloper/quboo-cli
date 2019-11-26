import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.Scanner;

public class QubooScriptPlugin {

    private static final String ENV_ACCESS_KEY = "QUBOO_ACCESS_KEY";
    private static final String ENV_SECRET_KEY = "QUBOO_SECRET_KEY";

    private static final String ENV_QUBOO_USERNAME = "QUBOO_PLAYER_USERNAME";
    private static final String ENV_GITLAB_USERNAME = "GITLAB_USER_LOGIN";
    private static final String ENV_CIRCLE_USERNAME = "CIRCLE_USERNAME";
    private static final String ENV_QUBOO_UNIQUE_ID = "QUBOO_UNIQUE_ID";

    // Pass this variable as 'true' if you want to use always git usernames in Quboo
    private static final String ENV_ALWAYS_USE_GIT = "QUBOO_CONFIG_ALWAYS_USE_GIT";

    private static final String QUBOO_API_SERVER = "https://api.quboo.io";

    public static void main(String[] args) {
        // Check if the auth env vars are properly set
        final String accessKey = System.getenv(ENV_ACCESS_KEY);
        final String secretKey = System.getenv(ENV_SECRET_KEY);
        if (accessKey == null || secretKey == null || accessKey.isBlank() || secretKey.isBlank()) {
            System.err.println("You have to provide valid Quboo access and secret keys via environment variables " +
                    ENV_ACCESS_KEY + " and " + ENV_SECRET_KEY);
            System.exit(1);
        }

        // Configuration
        final boolean alwaysUseGit = Optional.ofNullable(System.getenv(ENV_ALWAYS_USE_GIT))
                .map(Boolean::parseBoolean).orElse(false);

        // Detect the player and unique id using the CI tool vars or manual env var
        final String genericUsername = System.getenv(ENV_QUBOO_USERNAME);
        final String gitlabUsername = System.getenv(ENV_GITLAB_USERNAME);
        final String circleUsername = System.getenv(ENV_CIRCLE_USERNAME);

        String playerName;
        if (alwaysUseGit) { // if set to true, it overrides any other env var
            playerName = getLastGitCommitterFromGit();
        } else if (genericUsername != null) {
            playerName = genericUsername;
        } else if (gitlabUsername != null && !gitlabUsername.isBlank()) {
            playerName = gitlabUsername;
        } else if (circleUsername != null && !circleUsername.isBlank()) {
            // In CircleCI it's possible that you get a blank user if the committer is not a CircleCI user as well
            playerName = circleUsername;
        } else {
            playerName = getLastGitCommitterFromGit();
        }

        String uniqueId;
        final String qubooUniqueId = System.getenv(ENV_QUBOO_UNIQUE_ID);
        if (qubooUniqueId != null && !qubooUniqueId.isBlank()) {
            uniqueId = qubooUniqueId;
        } else {
            uniqueId = getLastGitHashFromGit();
        }

        if (playerName == null || playerName.isBlank()) {
            System.out.println("The script could not derive the player name from environment variables. Please use " +
                    ENV_QUBOO_USERNAME + " to specify the player that will get the Quboo score.");
            System.exit(1);
        }
        if (uniqueId == null || uniqueId.isBlank()) {
            uniqueId = String.valueOf(System.nanoTime());
            System.out.println("WARNING: The script could not get a unique id from environment variable or git. Please use " +
                    ENV_QUBOO_UNIQUE_ID + ", otherwise you will be duplicating the score in future executions.");
        }

        // Check if args are properly set
        String typeOrScore = null, description = null;
        if (args.length == 2) {
            typeOrScore = args[0];
            description = args[1];
        } else {
            System.out.println("Wrong parameters. Usage: \n\n" +
                    "quboo [release|doc|(numeric score)] \"description\"\n\n" +
                    "Examples: \n" +
                    "  - quboo release \"Backend release\"\n" +
                    "  - quboo 50 \"Helping a buddy\"");
            System.exit(1);
        }

        // The type of score determines the endpoint to call
        final String endpoint;
        boolean isScore = false;
        if (typeOrScore.equalsIgnoreCase("release")) {
            endpoint = QUBOO_API_SERVER + "/score/release";
        } else if (typeOrScore.equalsIgnoreCase("doc")) {
            endpoint = QUBOO_API_SERVER + "/score/documentation";
        } else {
            endpoint = QUBOO_API_SERVER + "/score/generic";
            isScore = true;
        }

        // Calls Quboo Server
        final HttpClient http = HttpClient.newHttpClient();
        final HttpRequest request = HttpRequest.newBuilder().header("x-quboo-access-key", accessKey)
                .header("x-quboo-secret-key", secretKey)
                .header("Content-Type", "application/json")
                .uri(URI.create(endpoint))
                .PUT(HttpRequest.BodyPublishers.ofString(
                        "{ \"playerLogin\":\"" + playerName + "\"," +
                                " \"uniqueId\":\"" + uniqueId + "\"," +
                                " \"description\":\"" + description + "\"," +
                                " \"score\":\"" + (isScore ? typeOrScore : 1) + "\"" + // score is ignored if not generic
                                "}"
                )).build();
        try {
            System.out.println("Sending request to " + request.uri());
            final HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                System.out.println("Score added succesfully. Response: " + response.body());
            } else if (response.statusCode() == 403) {
                System.err.println("Forbidden access to Quboo server. Please verify your access and secret keys.");
            } else {
                System.err.println("Got error response from server (" + response.statusCode() + "). Body: " + response.body());
                System.exit(1);
            }
        } catch (final Exception e) {
            System.err.println("Could not send score to Quboo. Please check your connectivity and also proper usage of CLI.");
            System.err.println("Reason: " + e.getMessage());
            System.exit(1);
        }
    }

    static String getLastGitCommitterFromGit() {
        try {
            final InputStream is = Runtime.getRuntime().exec("git log -1 --pretty=format:%an").getInputStream();
            final Scanner scanner = new Scanner(is).useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next().replaceAll(" ", "_") : null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static String getLastGitHashFromGit() {
        try {
            final InputStream is = Runtime.getRuntime().exec("git log -1 --pretty=format:%h").getInputStream();
            final Scanner scanner = new Scanner(is).useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
