#!/bin/bash
set -e

# mandatory env check
required_vars=(DATABASE_USER DATABASE_PASSWORD_FILE DATABASE_HOST DATABASE_PORT STT_ENABLED)
DATABASE_SCHEMA_NAME="fipop"

if [ -f "$DATABASE_PASSWORD_FILE" ]; then
  export DATABASE_PASSWORD=$(cat "$DATABASE_PASSWORD_FILE")
fi


for var in "${required_vars[@]}"; do
  value="${!var:-}"
  if [ -z "$value" ]; then
    echo "Error: Environment variable $var is required but not set or empty."
    exit 1
  fi
done

if [ "$STT_ENABLED" = "true" ]; then
    required_stt_vars=(STT_HOST STT_PORT STT_TRANSFER_TYPE STT_TRUSTED_SERVER_CERTIFICATE_PATH STT_AUTH_ENABLED)
    for var in "${required_stt_vars[@]}"; do
      value="${!var:-}"
      if [ -z "$value" ]; then
        echo "Error: Environment variable $var is required when speech to text functionality is enabled, but not set or empty."
        exit 1
      fi
    done

    if [ "$STT_TRANSFER_TYPE" = "SHARED_STORAGE" ]; then
      required_stt_shared_storage_vars=(STT_TRANSFER_SHARED_AUDIO_STORAGE_PATH STT_TRANSFER_SHARED_RESULT_STORAGE_PATH)
      for var in "${required_stt_shared_storage_vars[@]}"; do
        value="${!var:-}"
        if [ -z "$value" ]; then
          echo "Error: Environment variable $var is required when speech to text with shared storage option is configured, but not set or empty."
          exit 1
        fi
      done
    fi

    if [ "$STT_AUTH_ENABLED" = "true" ]; then
       required_stt_auth_vars=(STT_AUTH_CERTIFICATE_PATH STT_AUTH_PRIVATE_KEY_PATH)
       for var in "${required_stt_auth_vars[@]}"; do
         value="${!var:-}"
         if [ -z "$value" ]; then
           echo "Error: Environment variable $var is required when speech to text with auth option is configured, but not set or empty."
           exit 1
         fi
       done
    fi
fi

echo "Verify database and create or upgrade schema (if necessary)"
if [ "$(mysql -u"${DATABASE_USER}" -p"${DATABASE_PASSWORD}" -h"${DATABASE_HOST}" -P"${DATABASE_PORT}" -N -s -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DATABASE_SCHEMA_NAME}' AND table_name='db_version';")" -eq 1 ]; then
echo "Database schema already exists. Executing schema update when necessary."
  mysql -u"${DATABASE_USER}" -p"${DATABASE_PASSWORD}" -h"${DATABASE_HOST}" -P"${DATABASE_PORT}" ${DATABASE_SCHEMA_NAME} < /var/lib/timmat/sql/db_update.sql
else
   echo "Database schema doesn't exists. Executing schema creation"
   mysql -u"${DATABASE_USER}" -p"${DATABASE_PASSWORD}" -h"${DATABASE_HOST}" -P"${DATABASE_PORT}" < /var/lib/timaat/sql/fipop.sql
fi

# Here we can add timaat filesystem migration when necessary
STORAGE_LOCATION="/var/lib/timaat/storage"
STORAGE_TEMP_LOCATION="/tmp/timaat"

echo "Create necessary directories"
mkdir -p /root/.timaat
mkdir -p "${STORAGE_LOCATION}"
mkdir -p "${STORAGE_TEMP_LOCATION}"

echo "Write TIMAAT configuration file"
cat > /root/.timaat/timaat.properties <<EOF
storage.location=${STORAGE_LOCATION}
storage.temp.location=${STORAGE_TEMP_LOCATION}

database.driver=com.mysql.cj.jdbc.Driver
database.url=jdbc:mysql://${DATABASE_HOST}:${DATABASE_PORT}/${DATABASE_SCHEMA_NAME}?useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&useUnicode=yes&characterEncoding=UTF-8&maxIdleTime=180&useMysqlMetadata=true
database.user=${DATABASE_USER}
database.password=${DATABASE_PASSWORD}

app.ffmpeg.location=/usr/bin/
app.imagemagick.location=/usr/bin/

app.task.maxParallelCount=${APP_TASK_MAXPARALLELCOUNT}
app.task.coreParallelCount=${APP_TASK_COREPARALLELCOUNT}
app.task.queueSize=${APP_TASK_QUEUESIZE}

stt.enabled=${STT_ENABLED}
EOF

if [ "$STT_ENABLED" = "true" ]; then
  echo "Append TIMAAT STT configuration"
  cat >> /root/.timaat/timaat.properties <<EOF
stt.host=${STT_HOST}
stt.port=${STT_PORT}
stt.transfer.type=${STT_TRANSFER_TYPE}
stt.trustedServerCertificatePath=${STT_TRUSTED_SERVER_CERTIFICATE_PATH}
stt.auth.enabled=${STT_AUTH_ENABLED}
EOF

  if [ "$STT_TRANSFER_TYPE" = "SHARED_STORAGE" ]; then
      cat >> /root/.timaat/timaat.properties <<EOF
stt.transfer.sharedAudioStoragePath=${STT_TRANSFER_SHARED_AUDIO_STORAGE_PATH}
stt.transfer.sharedResultStoragePath=${STT_TRANSFER_SHARED_RESULT_STORAGE_PATH}
EOF
  fi

  if [ "$STT_AUTH_ENABLED" = "true" ]; then
      cat >> /root/.timaat/timaat.properties <<EOF
stt.auth.certificatePath=${STT_AUTH_CERTIFICATE_PATH}
stt.auth.privateKeyPath=${STT_AUTH_PRIVATE_KEY_PATH}
EOF
  fi
fi


echo "Starting tomcat server"
/usr/local/tomcat/bin/catalina.sh run