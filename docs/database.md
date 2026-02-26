# Database

## Prerequisites 
* Installed dbms (mysql or mariadb)
* A database 'FIPOP' with collation set to 'utf8mb4_general_ci'

## Installation
* Create a database 'FIPOP' and make sure the collation is set to 'utf8mb4_general_ci'
* To install the database schema execute the sql-file [fipop.sql](../src/main/resources/database/fipop.sql)
* The default application username and password are admin/admin

## Update
* Create a dump of the current database as backup
* Execute the sql-file [db_update.sql](../src/main/resources/database/db_update.sql) 

## Versioning
The current version of the database schemas can be read from the `db_version` table. This table has existed since TIMAAT version >= 0.14.x.
The versioning of the database schema follows the following pattern:

* Major version is the same as TIMAAT major version
* Minor version is the same as TIMAAT minor version
* Patch version is incremented after each update of the database schema for the same TIMAAT major and minor version

## Releases

### 0.14.0

* Added table `db_version` to persist the current database schema version
* Added tables `audio_analysis`, `medium_audio_analysis`, `audio_analysis_state`, `audio_analysis_state_translation` which are used to persist the state and results of async audio analysis tasks
  ![db_update_0_14_0_async_audio_analysis.png](img/db_update_0_14_0_async_audio_analysis.png)

### 0.14.1

* Added table `music_translation` responsible to persist the lyrics or spoken text of music entries
![db_update_0_14_1_transcription_for.png](img/db_update_0_14_1_transcription_for.png)

### 0.14.2

* Added table `annotation_has_music` to persist reference between music and annotation
* Added table `annotation_has_music_translation_area` to persist the hearable transcription range
![db_update_0_14_2_annotation_to_music.png](img/db_update_0_14_2_annotation_to_music.png)

### 0.15.0

* Added column `thumbnail_position_ms` to table `medium_video` to persist thumbnail position of videos
* Added column `thumbnail_position_ms` to table `annotation` to persist thumbnail position of annotations 

### 0.15.1

* Removed unused column `category_set_id` from table `annotation_has_category`
* Added tables `actor_has_category_set` and `actor_has_category` to link actors with categories
* Added view `analysis_segment_structure_element` which combines information of all segment structure type tables

### 0.15.2

* Renamed all `*.has_category_set`-table to  `*.restricted_category_set`