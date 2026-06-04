/*
 * Copyright 2026 bitGilde IT Solutions UG (haftungsbeschränkt)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.bitgilde.TIMAAT;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Releases JVM-wide resources held by the web application when its servlet
 * context is destroyed (undeploy or Tomcat shutdown).
 * <p>
 * Without this listener the JPA {@link EntityManagerFactory} stays open and
 * MySQL Connector/J's {@code mysql-cj-abandoned-connection-cleanup} thread keeps
 * running after the web application has stopped. The lingering thread tries to
 * use the already-stopped web application class loader, which floods the log
 * with {@code IllegalStateException}s and prevents the container from shutting
 * down cleanly. The listener closes the factory and stops the cleanup thread to
 * avoid those leaks.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 29.05.26
 */
public class ApplicationShutdownListener implements ServletContextListener {

  private static final Logger LOGGER = Logger.getLogger(ApplicationShutdownListener.class.getName());

  /**
   * Releases the resources that outlive the web application class loader once
   * the servlet context is destroyed.
   *
   * @param sce the servlet context lifecycle event, unused but required by the
   *            {@link ServletContextListener} contract
   */
  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    closeEntityManagerFactory(TIMAATApp.emf);
    shutdownMysqlCleanupThread();
    deregisterJdbcDrivers();
  }

  /**
   * Closes the given {@link EntityManagerFactory} if it is still open. A
   * {@code null} or already-closed factory is ignored, and any error raised
   * while closing is logged and swallowed so that the remaining shutdown steps
   * still run.
   *
   * @param emf the factory to close, may be {@code null}
   */
  void closeEntityManagerFactory(@Nullable EntityManagerFactory emf) {
    if (emf == null || !emf.isOpen()) {
      return;
    }
    try {
      emf.close();
    } catch (RuntimeException e) {
      LOGGER.log(Level.WARNING, "Failed to close the EntityManagerFactory during shutdown.", e);
    }
  }

  /**
   * Stops MySQL Connector/J's abandoned connection cleanup thread if it was
   * started. Any error raised by the driver is logged and swallowed so that it
   * cannot break the container shutdown.
   */
  void shutdownMysqlCleanupThread() {
    try {
      AbandonedConnectionCleanupThread.checkedShutdown();
    } catch (Throwable t) {
      LOGGER.log(Level.WARNING, "Failed to stop the MySQL abandoned connection cleanup thread during shutdown.", t);
    }
  }

  /**
   * Deregisters every JDBC {@link Driver} that was loaded by this web
   * application's class loader. JDBC drivers register themselves in the
   * JVM-wide {@link DriverManager} on class load; if they are not removed when
   * the web application stops, the container reports a memory leak and has to
   * force the deregistration. Drivers owned by other class loaders are left
   * untouched, and any error is logged and swallowed so the shutdown can
   * continue.
   */
  void deregisterJdbcDrivers() {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      Driver driver = drivers.nextElement();
      if (driver.getClass().getClassLoader() != contextClassLoader) {
        continue;
      }
      try {
        DriverManager.deregisterDriver(driver);
      } catch (SQLException e) {
        LOGGER.log(Level.WARNING, "Failed to deregister JDBC driver " + driver + " during shutdown.", e);
      }
    }
  }
}