package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.StandaloneEntry;
import edu.illinois.library.cantaloupe.ApplicationServer;
import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Client;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.SocketUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Abstract base class for functional HTTP endpoint tests.
 */
public abstract class ResourceTest extends BaseTest {

    protected static int HTTP_PORT = SocketUtils.getOpenPort();
    protected static int HTTPS_PORT;

    protected static ApplicationServer appServer;

    protected Client client;

    static {
        do {
            HTTPS_PORT = SocketUtils.getOpenPort();
        } while (HTTPS_PORT == HTTP_PORT);
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.ADMIN_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());
        config.setProperty(Key.PROCESSOR_SELECTION_STRATEGY,
                "ManualSelectionStrategy");
        config.setProperty("processor.ManualSelectionStrategy.pdf",
                "PdfBoxProcessor");
        config.setProperty(Key.PROCESSOR_FALLBACK, "Java2dProcessor");
        config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
        config.setProperty(Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                TestUtil.getFixturePath() + "/images/");

        new CacheFacade().purge();

        appServer = StandaloneEntry.getAppServer();
        appServer.setHTTPEnabled(true);
        appServer.setHTTPPort(HTTP_PORT);

        appServer.setHTTPSEnabled(true);
        appServer.setHTTPSPort(HTTPS_PORT);
        appServer.setHTTPSKeyStoreType("JKS");
        appServer.setHTTPSKeyStorePath(
                TestUtil.getFixture("keystore-password.jks").toString());
        appServer.setHTTPSKeyStorePassword("password");
        appServer.setHTTPSKeyPassword("password");

        appServer.start();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        appServer.stop();

        if (client != null) {
            client.stop();
        }
    }

    abstract protected String getEndpointPath();

    /**
     * @param path URI path relative to {@link #getEndpointPath()}.
     */
    protected URI getHTTPURI(String path) {
        try {
            return new URI(getHTTPURIString(path));
        } catch (URISyntaxException e) {
            fail(e.getMessage());
        }
        return null;
    }

    /**
     * @param path URI path relative to {@link #getEndpointPath()}.
     */
    protected String getHTTPURIString(String path) {
        return "http://localhost:" + appServer.getHTTPPort() +
                getEndpointPath() + path;
    }

    /**
     * @param path URI path relative to {@link #getEndpointPath()}.
     * @return HTTPS URI.
     */
    protected URI getHTTPSURI(String path) {
        try {
            URI uri = getHTTPURI(path);
            return new URI("https", uri.getUserInfo(), uri.getHost(),
                    appServer.getHTTPSPort(), uri.getPath(), uri.getQuery(),
                    uri.getFragment());
        } catch (URISyntaxException e) {
            fail(e.getMessage());
        }
        return null;
    }

    /**
     * @param subpath URI path to use, relative to {@link #getEndpointPath()}.
     * @return New client instance. Clients should call {@link Client#stop()}
     *         on it when they are done with it. Or, if they assign it to
     *         {@link #client}, {@link #tearDown()} will take care of it.
     */
    protected Client newClient(String subpath) {
        return new Client().builder().uri(getHTTPURI(subpath)).build();
    }

    /**
     * @param subpath URI path to use, relative to {@link #getEndpointPath()}.
     * @param user
     * @param secret
     * @param realm
     * @return New client instance, initialized to use HTTP Basic
     *         authentication. Clients should call {@link Client#stop()}
     *         on it when they are done. Or, if they assign it to
     *         {@link #client}, {@link #tearDown()} will take care of it.
     */
    protected Client newClient(String subpath, String user, String secret,
                               String realm) {
        return new Client().builder().
                uri(getHTTPURI(subpath)).
                realm(realm).
                username(user).
                secret(secret).
                build();
    }

}
