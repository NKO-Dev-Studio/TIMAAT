FROM tomcat:10.1.47-jre25-temurin-noble
LABEL org.opencontainers.image.title="TIMAAT Web Application"
LABEL org.opencontainers.image.description="Java-based web application running on Tomcat, providing TIMAAT services."
LABEL org.opencontainers.image.version="${TIMAAT_VERSION}"
LABEL org.opencontainers.image.authors="Nico Kotlenga <nico@kotlenga.dev>"
LABEL org.opencontainers.image.source="https://github.com/bitgilde/TIMAAT"
LABEL org.opencontainers.image.licenses="Apache License 2.0"

ARG TIMAAT_VERSION
ENV TIMAAT_VERSION=$TIMAAT_VERSION

# TIMAAT Configuration variables
ENV APP_TASK_QUEUESIZE=1000
ENV APP_TASK_COREPARALLELCOUNT=2
ENV APP_TASK_MAXPARALLELCOUNT=4


# Copy entrypoint script into container
COPY docker/timaat-entrypoint.sh /usr/local/bin/timaat-entrypoint.sh
COPY src/main/resources/database/db_update.sql /var/lib/timmat/sql/db_update.sql
COPY src/main/resources/database/fipop.sql /var/lib/timaat/sql/fipop.sql
COPY src/main/resources/scripts/timaat-cron.sh /var/lib/timaat/scripts/timaat-cron.sh
COPY src/main/resources/scripts/timaat-encoder.sh /var/lib/timaat/scripts/timaat-encoder.sh

# Copy webapp to tomcat webapps directory
COPY target/TIMAAT.war /usr/local/tomcat/webapps

# Install required packages
RUN apt update && \
    apt install -y --no-install-recommends ca-certificates && \
    apt install -y --no-install-recommends imagemagick && \
    apt install -y --no-install-recommends ffmpeg && \
    apt install -y --no-install-recommends mysql-client && \
    rm -rf /var/lib/apt/lists/*

ENTRYPOINT ["/usr/local/bin/timaat-entrypoint.sh"]