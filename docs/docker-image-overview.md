# Overview

The image includes nearly everything to successfully run the timaat webservice. It only requires a mysql or mariadb
database which connection information must be passed as environment variable.

The correct initialization of the database or necessary migration steps are handled automatically during container
startup. So no database need to get created or created beforehand.

Tomcat will bind to port 8080 of the container. You have to bind this port to a host port to make timaat available
outside. If you bind container port 8080 to host port 8080 TIMAAT will be available under following
URL: http://localhost:8080/TIMAAT

A default account with following credentials has been created

* **Username**: admin
* **Password**: admin

# Tags

The image tags are named by following convention:

* **latest**: Contains highest released version
* **latest-snapshot**: Contains the highest unreleased snapshot version
* **{major}.{minor}.{patch}**: Contains the specified released version

# Environment Variables

The configuration of the image is handled by environment variables. This includes the database configuration and general
application behaviour.

The speech to text functionality requires a running instance of the
[speech-to-text-uni-erfurt-service](https://github.com/NKO-Dev-Studio/speech-to-text-uni-erfurt). Details about this
service and how to deploy it can be found in the linked repository.

| Environment Variable                      | Required                                                           | Version added | Default Value | Example value                      | Description                                                                                                                       |
|-------------------------------------------|--------------------------------------------------------------------|---------------|---------------|------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| `DATABASE_USER`                           | ✅                                                                 | 0.14.0        | -             | `root`                             | Name of the user used for the database access. (Must have privileges to create and modify databases).                             |
| `DATABASE_PASSWORD_FILE`                  | ✅                                                                 | 0.14.0        | -             | `/run/secrets/dbpassword.txt`      | Path to the file containing the password of the specified database user.                                                          |
| `DATABASE_HOST`                           | ✅                                                                 | 0.14.0        | -             | `localhost`                        | Host on which the database is running and can be accessed.                                                                        |   
| `DATABASE_PORT`                           | ✅                                                                 | 0.14.0        | -             | `3304`                             | The port on which the database is bind to.                                                                                        |
| `APP_TASK_MAXPARALLELCOUNT`               | ❌                                                                 | 0.14.0        | `4`           | `4`                                | Specifies how many async tasks can run in parallel.                                                                               |
| `APP_TASK_COREPARALLELCOUNT`              | ❌                                                                 | 0.14.0        | `2`           | `2`                                | Specifies how many async runners will be kept warm.                                                                               |
| `APP_TASK_QUEUESIZE`                      | ❌                                                                 | 0.14.0        | `100`         | `1000`                             | Specifies the size of the queue of async tasks.                                                                                   |
| `STT_ENABLED`                             | ❌                                                                 | 1.0.0         | `false`       | `false`                            | Toggle property to enable or disable speech to text functionality.                                                                |
| `STT_HOST`                                | When `STT_ENABLED`=`true`                                          | 1.0.0         | -             | `speech-to-text.uni-erfurt.de`     | Host on which the speech to text service is deployed on.                                                                          |
| `STT_PORT`                                | When `STT_ENABLED`=`true`                                          | 1.0.0         | -             | `8081`                             | Port on which the speech to text service is binded to.                                                                            |
| `STT_TRANSFER_TYPE`                       | When `STT_ENABLED`=`true`                                          | 1.0.0         | -             | `STREAMING`                        | Defines how audio and result files are exchanged with the speech to text service. Possible values: `STREAMING`, `SHARED_STORAGE`. |
| `STT_TRUSTED_SERVER_CERTIFICATE_PATH`     | When `STT_ENABLED`=`true`                                          | 1.0.0         | -             | `/run/secrets/stt-server-cert.pem` | Path to the trusted certificate used to establish a secure connection to the speech to text service.                              |
| `STT_TRANSFER_SHARED_AUDIO_STORAGE_PATH`  | When `STT_ENABLED`=`true` and `STT_TRANSFER_TYPE`=`SHARED_STORAGE` | 1.0.0         | -             | `/var/lib/timaat/stt/audio`        | Path to the shared storage location used to provide audio files to the speech to text service.                                    |
| `STT_TRANSFER_SHARED_RESULT_STORAGE_PATH` | When `STT_ENABLED`=`true` and `STT_TRANSFER_TYPE`=`SHARED_STORAGE` | 1.0.0         | -             | `/var/lib/timaat/stt/results`      | Path to the shared storage location from which the transcription results of the speech to text service are read.                  |
| `STT_AUTH_ENABLED`                        | ❌                                                                 | 1.0.0         | `false`       | `true`                             | Toggle property to enable or disable certificate based authentication against the speech to text service.                         |
| `STT_AUTH_CERTIFICATE_PATH`               | When `STT_ENABLED`=`true` and `STT_AUTH_ENABLED`=`true`            | 1.0.0         | -             | `/run/secrets/stt-client-cert.pem` | Path to the certificate used for the authentication against the speech to text service.                                           |
| `STT_AUTH_PRIVATE_KEY_PATH`               | When `STT_ENABLED`=`true` and `STT_AUTH_ENABLED`=`true`            | 1.0.0         | -             | `/run/secrets/stt-client-key.pem`  | Path to the private key belonging to the authentication certificate.                                                              |

# Files

All durable files of TIMAAT are persistent inside `/var/lib/timaat/storage`. Make sure to create a volume or filesystem
mount to keep this files even after deletion of the container.