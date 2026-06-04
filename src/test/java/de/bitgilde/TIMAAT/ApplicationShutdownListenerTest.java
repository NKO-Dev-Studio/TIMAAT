package de.bitgilde.TIMAAT;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletContextEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that the {@link ApplicationShutdownListener} releases the JPA
 * {@link EntityManagerFactory} and stops MySQL Connector/J's JVM-wide abandoned
 * connection cleanup thread when the servlet context is destroyed, so that the
 * web application can be undeployed without leaking threads or blocking the
 * Tomcat shutdown.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 29.05.26
 */
class ApplicationShutdownListenerTest {

  private ApplicationShutdownListener listener;
  private EntityManagerFactory originalEmf;

  @BeforeEach
  void setUp() {
    this.listener = new ApplicationShutdownListener();
    this.originalEmf = TIMAATApp.emf;
  }

  @AfterEach
  void tearDown() {
    TIMAATApp.emf = this.originalEmf;
  }

  @Test
  void closeEntityManagerFactoryClosesOpenFactory() {
    EntityManagerFactory emf = mock(EntityManagerFactory.class);
    when(emf.isOpen()).thenReturn(true);

    listener.closeEntityManagerFactory(emf);

    verify(emf).close();
  }

  @Test
  void closeEntityManagerFactorySkipsAlreadyClosedFactory() {
    EntityManagerFactory emf = mock(EntityManagerFactory.class);
    when(emf.isOpen()).thenReturn(false);

    listener.closeEntityManagerFactory(emf);

    verify(emf, never()).close();
  }

  @Test
  void closeEntityManagerFactoryIgnoresNull() {
    assertThatNoException().isThrownBy(() -> listener.closeEntityManagerFactory(null));
  }

  @Test
  void closeEntityManagerFactorySwallowsExceptions() {
    EntityManagerFactory emf = mock(EntityManagerFactory.class);
    when(emf.isOpen()).thenReturn(true);
    doThrowOnClose(emf);

    assertThatNoException().isThrownBy(() -> listener.closeEntityManagerFactory(emf));
  }

  @Test
  void shutdownMysqlCleanupThreadInvokesCheckedShutdown() {
    try (MockedStatic<AbandonedConnectionCleanupThread> cleanup = mockStatic(AbandonedConnectionCleanupThread.class)) {
      listener.shutdownMysqlCleanupThread();

      cleanup.verify(AbandonedConnectionCleanupThread::checkedShutdown);
    }
  }

  @Test
  void shutdownMysqlCleanupThreadSwallowsExceptions() {
    try (MockedStatic<AbandonedConnectionCleanupThread> cleanup = mockStatic(AbandonedConnectionCleanupThread.class)) {
      cleanup.when(AbandonedConnectionCleanupThread::checkedShutdown).thenThrow(new RuntimeException("boom"));

      assertThatNoException().isThrownBy(() -> listener.shutdownMysqlCleanupThread());
    }
  }

  @Test
  void contextDestroyedClosesFactoryAndStopsCleanupThread() {
    EntityManagerFactory emf = mock(EntityManagerFactory.class);
    when(emf.isOpen()).thenReturn(true);
    TIMAATApp.emf = emf;

    try (MockedStatic<AbandonedConnectionCleanupThread> cleanup = mockStatic(AbandonedConnectionCleanupThread.class)) {
      listener.contextDestroyed(mock(ServletContextEvent.class));

      verify(emf).close();
      cleanup.verify(AbandonedConnectionCleanupThread::checkedShutdown);
    }
  }

  @Test
  void contextDestroyedDoesNotThrowWhenFactoryIsNull() {
    TIMAATApp.emf = null;

    try (MockedStatic<AbandonedConnectionCleanupThread> cleanup = mockStatic(AbandonedConnectionCleanupThread.class)) {
      assertThatNoException().isThrownBy(() -> listener.contextDestroyed(mock(ServletContextEvent.class)));
    }
  }

  @Test
  void deregisterJdbcDriversRemovesDriversLoadedByTheContextClassLoader() throws SQLException {
    Driver driver = new FakeDriver();
    DriverManager.registerDriver(driver);
    try {
      listener.deregisterJdbcDrivers();

      assertThat(Collections.list(DriverManager.getDrivers())).doesNotContain(driver);
    } finally {
      deregisterQuietly(driver);
    }
  }

  @Test
  void deregisterJdbcDriversDoesNotThrowWhenNoDriversArePresent() {
    assertThatNoException().isThrownBy(() -> listener.deregisterJdbcDrivers());
  }

  private static void deregisterQuietly(Driver driver) {
    try {
      List<Driver> registered = Collections.list(DriverManager.getDrivers());
      if (registered.contains(driver)) {
        DriverManager.deregisterDriver(driver);
      }
    } catch (SQLException ignored) {
      // best effort cleanup
    }
  }

  private static void doThrowOnClose(EntityManagerFactory emf) {
    org.mockito.Mockito.doThrow(new RuntimeException("close failed")).when(emf).close();
  }

  /**
   * Minimal {@link Driver} implementation used to register a driver that is
   * loaded by the test class loader, so that the deregistration logic can be
   * exercised against a real {@link DriverManager} entry.
   *
   * @author Nico Kotlenga (nico@nko-dev.studio)
   * @since 29.05.26
   */
  private static final class FakeDriver implements Driver {
    @Override
    public Connection connect(String url, Properties info) {
      return null;
    }

    @Override
    public boolean acceptsURL(String url) {
      return false;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
      return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
      return 0;
    }

    @Override
    public int getMinorVersion() {
      return 0;
    }

    @Override
    public boolean jdbcCompliant() {
      return false;
    }

    @Override
    public Logger getParentLogger() {
      return Logger.getLogger(FakeDriver.class.getName());
    }
  }
}