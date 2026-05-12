/**
 * The execution of this script is necessary if an already running instance of TIMAAT is to be updated to the latest
 * version and a database update has occurred in this version.
 * The required database schema version is documented within the README. The schema version currently installed on the database
 * can be read in the db_version table. If this table is not available, this is the initial schema version, which must be updated in any case.
 *
 * IMPORTANT: Please make a backup of your database before executing this update script!
 */
DELIMITER $$
CREATE PROCEDURE update_to_0_14_0()
BEGIN
    DECLARE database_name VARCHAR(64) DEFAULT 'FIPOP';
    DECLARE db_version_table_name VARCHAR(64) DEFAULT 'db_version';
    DECLARE db_version_exists INT DEFAULT 0;

    SELECT COUNT(*)
    INTO db_version_exists
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = database_name
      AND TABLE_NAME = db_version_table_name;

    IF db_version_exists = 0 THEN
        SELECT 'execute update to 0.14.0' AS log_message;
        CREATE TABLE `FIPOP`.`db_version`
        (
            `major_version` INT       NOT NULL,
            `minor_version` INT       NOT NULL,
            `patch_version` INT       NOT NULL,
            `created_at`    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (`major_version`, `minor_version`, `patch_version`)
        )
            ENGINE = InnoDB;

        CREATE TABLE `FIPOP`.`audio_analysis`
        (
            `id`                         INT           NOT NULL AUTO_INCREMENT,
            `audio_codec`                VARCHAR(45)   NOT NULL,
            `channels`                   int           NOT NULL,
            `sample_rate`                int           NOT NULL,
            `bitrate`                    int           NOT NULL,
            `sample_count`               bigint        NOT NULL,
            `waveform_path`              varchar(4000) NOT NULL,
            `frequency_information_path` varchar(4000) NOT NULL,
            PRIMARY KEY (`id`)
        )
            ENGINE = InnoDB;

        CREATE TABLE `FIPOP`.`audio_analysis_state`
        (
            `id` INT NOT NULL AUTO_INCREMENT,
            PRIMARY KEY (`id`)
        )
            ENGINE = InnoDB;

        CREATE TABLE `FIPOP`.`audio_analysis_state_translation`
        (
            `id`                      INT         NOT NULL AUTO_INCREMENT,
            `audio_analysis_state_id` INT         NOT NULL,
            `language_id`             INT         NOT NULL,
            `state`                   VARCHAR(45) NOT NULL,
            PRIMARY KEY (`id`),
            CONSTRAINT `fk_audio_analysis_state_translation_audio_analysis_state1`
                FOREIGN KEY (`audio_analysis_state_id`)
                    REFERENCES `FIPOP`.`audio_analysis_state` (`id`)
                    ON DELETE CASCADE
                    ON UPDATE NO ACTION,
            CONSTRAINT `fk_audio_analysis_state_translation_language`
                FOREIGN KEY (`language_id`)
                    REFERENCES `FIPOP`.`language` (`id`)
                    ON DELETE CASCADE
                    ON UPDATE NO ACTION
        )
            ENGINE = InnoDB;
        CREATE INDEX fk_audio_analysis_state_translation_audio_analysis_state1_idx ON `FIPOP`.`audio_analysis_state_translation` (audio_analysis_state_id asc);
        CREATE INDEX fk_audio_analysis_state_translation_language ON `FIPOP`.`audio_analysis_state_translation` (language_id);

        CREATE TABLE `FIPOP`.`medium_audio_analysis`
        (
            `medium_id`               INT NOT NULL PRIMARY KEY,
            `audio_analysis_state_id` INT NOT NULL,
            `audio_analysis_id`       INT,
            CONSTRAINT `fk_medium_audio_analysis_medium1`
                FOREIGN KEY (`medium_id`)
                    REFERENCES `FIPOP`.`medium` (`id`)
                    ON DELETE CASCADE
                    ON UPDATE NO ACTION,
            CONSTRAINT `fk_medium_audio_analysis_audio_analysis_state1`
                FOREIGN KEY (`audio_analysis_state_id`)
                    REFERENCES `FIPOP`.`audio_analysis_state` (`id`)
                    ON DELETE CASCADE
                    ON UPDATE NO ACTION,
            CONSTRAINT `fk_medium_audio_analysis_audio_analysis1`
                FOREIGN KEY (`audio_analysis_id`)
                    REFERENCES `FIPOP`.`audio_analysis` (`id`)
                    ON DELETE SET NULL
                    ON UPDATE NO ACTION
        )
            ENGINE = InnoDB;
        CREATE INDEX fk_medium_audio_analysis_medium1_idx ON `FIPOP`.`medium_audio_analysis` (medium_id ASC);
        CREATE INDEX fk_medium_audio_analysis_audio_analysis_state1_idx ON `FIPOP`.`medium_audio_analysis` (audio_analysis_state_id ASC);
        CREATE INDEX fk_medium_audio_analysis_audio_analysis1_idx ON `FIPOP`.`medium_audio_analysis` (audio_analysis_id ASC);

        START TRANSACTION;
        INSERT INTO `FIPOP`.`db_version` (major_version, minor_version, patch_version) VALUES (0, 14, 0);
        COMMIT;

        START TRANSACTION;
        INSERT INTO `FIPOP`.`audio_analysis_state` (`id`)
        VALUES (1);
        INSERT INTO `FIPOP`.`audio_analysis_state` (`id`)
        VALUES (2);
        INSERT INTO `FIPOP`.`audio_analysis_state` (`id`)
        VALUES (3);
        INSERT INTO `FIPOP`.`audio_analysis_state` (`id`)
        VALUES (4);
        COMMIT;

        START TRANSACTION;
        INSERT INTO `FIPOP`.`audio_analysis_state_translation` (audio_analysis_state_id, language_id, state)
        VALUES (1, 1, 'pending');
        INSERT INTO `FIPOP`.`audio_analysis_state_translation` (audio_analysis_state_id, language_id, state)
        VALUES (2, 1, 'running');
        INSERT INTO `FIPOP`.`audio_analysis_state_translation` (audio_analysis_state_id, language_id, state)
        VALUES (3, 1, 'failed');
        INSERT INTO `FIPOP`.`audio_analysis_state_translation` (audio_analysis_state_id, language_id, state)
        VALUES (4, 1, 'done');
        COMMIT;

        START TRANSACTION;
        INSERT INTO `FIPOP`.`medium_audio_analysis` (medium_id, audio_analysis_state_id)
        SELECT id, 1
        FROM medium
        where media_type_id in (1, 6);
        COMMIT;

        # TODO: Remove audio_codec_information and referencing columns on medium_audio and medium_video
        ALTER TABLE medium_audio
            DROP FOREIGN KEY fk_medium_audio_audio_codec_information1;
        ALTER TABLE medium_audio
            DROP COLUMN audio_codec_information_id;
        ALTER TABLE medium_video
            DROP FOREIGN KEY fk_medium_video_audio_codec_information1;
        ALTER TABLE medium_video
            DROP COLUMN audio_codec_information_id;

        DROP TABLE audio_codec_information;

        SELECT 'finished update to 0.14.0' AS log_message;
    END IF;
END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE update_to_0_14_1()
BEGIN
    DECLARE db_major_version int;
    DECLARE db_minor_version int;
    DECLARE db_patch_version int;

    SELECT major_version, minor_version, patch_version
    INTO db_major_version, db_minor_version, db_patch_version
    FROM db_version
    LIMIT 1;

    IF db_major_version = 0 AND db_minor_version = 14 AND db_patch_version = 0 THEN
        SELECT 'execute update to 0.14.1' AS log_message;
        DELETE FROM db_version;
        INSERT INTO db_version (major_version, minor_version, patch_version) VALUES (0, 14, 1);

        CREATE TABLE `FIPOP`.`music_translation`
        (
            `music_id`    INT NOT NULL,
            `language_id` INT NOT NULL,
            `translation` TEXT,
            CONSTRAINT `fk_music_translation_music1`
                FOREIGN KEY (`music_id`)
                    REFERENCES `FIPOP`.`music` (`id`)
                    ON DELETE CASCADE
                    ON UPDATE NO ACTION,
            CONSTRAINT `fk_music_translation_language1`
                FOREIGN KEY (`language_id`)
                    REFERENCES `FIPOP`.`language` (`id`)
                    ON DELETE CASCADE
                    ON UPDATE NO ACTION,
            CONSTRAINT `pk_music_translation1` PRIMARY KEY (music_id, language_id)
        ) ENGINE = InnoDB;
        CREATE INDEX fk_music_translation_music1_idx ON `FIPOP`.`music_translation` (music_id ASC);
        CREATE INDEX fk_music_translation_language1_idx ON `FIPOP`.`music_translation` (language_id ASC);

        SELECT 'finished update to 0.14.1' AS log_message;
    END IF;
END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE update_to_0_14_2()
BEGIN
    DECLARE db_major_version int;
    DECLARE db_minor_version int;
    DECLARE db_patch_version int;

    SELECT major_version, minor_version, patch_version
    INTO db_major_version, db_minor_version, db_patch_version
    FROM db_version
    LIMIT 1;

    IF db_major_version = 0 AND db_minor_version = 14 AND db_patch_version = 1 THEN
        SELECT 'execute update to 0.14.2' AS log_message;
        DELETE FROM db_version;
        INSERT INTO db_version (major_version, minor_version, patch_version) VALUES (0, 14, 2);

        CREATE TABLE `FIPOP`.`annotation_has_music`
        (
            `music_id`      INT NOT NULL,
            `annotation_id` INT NOT NULL,
            CONSTRAINT `fk_annotation_has_music_music1`
                FOREIGN KEY (`music_id`)
                    REFERENCES `FIPOP`.`music` (`id`)
                    ON DELETE CASCADE
                    ON UPDATE NO ACTION,
            CONSTRAINT `fk_annotation_has_music_annotation1`
                FOREIGN KEY (`annotation_id`)
                    REFERENCES `FIPOP`.`annotation` (`id`)
                    ON DELETE CASCADE
                    ON UPDATE NO ACTION,
            CONSTRAINT `pk_annotation_has_music1` PRIMARY KEY (music_id, annotation_id)
        ) ENGINE = InnoDB;
        CREATE INDEX fk_annotation_has_music_music1_idx ON `FIPOP`.`annotation_has_music` (music_id ASC);
        CREATE INDEX fk_annotation_has_music_annotation1_idx ON `FIPOP`.`annotation_has_music` (annotation_id ASC);


        CREATE TABLE `FIPOP`.`annotation_has_music_translation_area`
        (
            `music_id`      INT NOT NULL,
            `annotation_id` INT NOT NULL,
            `language_id`   INT NOT NULL,
            `start_index`   INT NOT NULL,
            `end_index`     INT NOT NULL,
            CONSTRAINT `fk_annotation_has_music_translation_area_annotation_has_music1`
                FOREIGN KEY (`music_id`, `annotation_id`)
                    REFERENCES `FIPOP`.`annotation_has_music` (`music_id`, `annotation_id`)
                    ON DELETE CASCADE
                    ON UPDATE NO ACTION,
            CONSTRAINT `fk_annotation_has_music_translation_area_music_translation1`
                FOREIGN KEY (`music_id`, `language_id`)
                    REFERENCES `FIPOP`.`music_translation` (`music_id`, `language_id`)
                    ON DELETE CASCADE
                    ON UPDATE NO ACTION,
            CONSTRAINT `pk_annotation_has_music_translation_area1` PRIMARY KEY (music_id, annotation_id, language_id)
        ) ENGINE = InnoDB;
        CREATE INDEX fk_annotation_has_music_translation_area_music1_idx ON `FIPOP`.`annotation_has_music_translation_area` (music_id ASC);
        CREATE INDEX fk_annotation_has_music_translation_area_annotation1_idx ON `FIPOP`.`annotation_has_music_translation_area` (annotation_id ASC);
        CREATE INDEX fk_annotation_has_music_translation_area_language1_idx ON `FIPOP`.`annotation_has_music_translation_area` (language_id ASC);

        SELECT 'finished update to 0.14.2' AS log_message;
    END IF;
END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE update_to_0_15_0()
BEGIN
    DECLARE db_major_version int;
    DECLARE db_minor_version int;
    DECLARE db_patch_version int;

    SELECT major_version, minor_version, patch_version
    INTO db_major_version, db_minor_version, db_patch_version
    FROM db_version
    LIMIT 1;

    IF db_major_version = 0 AND db_minor_version = 14 AND db_patch_version = 2 THEN
        SELECT 'execute update to 0.15.0' AS log_message;
        DELETE FROM db_version;
        INSERT INTO db_version (major_version, minor_version, patch_version) VALUES (0, 15, 0);

        ALTER TABLE `fipop`.`medium_video`
            ADD COLUMN thumbnail_position_ms INT;
        ALTER TABLE `fipop`.`annotation`
            ADD COLUMN thumbnail_position_ms INT;

        UPDATE `fipop`.`annotation`
        set thumbnail_position_ms=start_time
        where medium_analysis_list_id in (select medium_analysis_list.id
                                          from medium_analysis_list
                                                   join medium on medium_analysis_list.medium_id = medium.id
                                          where medium.media_type_id = 6);
    END IF;
END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE update_to_0_15_1()
BEGIN
    DECLARE db_major_version int;
    DECLARE db_minor_version int;
    DECLARE db_patch_version int;

    SELECT major_version, minor_version, patch_version
    INTO db_major_version, db_minor_version, db_patch_version
    FROM db_version
    LIMIT 1;

    IF db_major_version = 0 AND db_minor_version = 15 AND db_patch_version = 0 THEN
        SELECT 'execute update to 0.15.1' AS log_message;
        DELETE FROM db_version;
        INSERT INTO db_version (major_version, minor_version, patch_version) VALUES (0, 15, 1);

        ALTER TABLE `fipop`.`annotation_has_category`
            DROP COLUMN category_set_id;

        CREATE TABLE `fipop`.`actor_has_category_set`
        (
            `actor_id`        INT NOT NULL,
            `category_set_id` INT NOT NULL,
            CONSTRAINT `fk_actor_has_category_set_actor`
                FOREIGN KEY (`actor_id`)
                    REFERENCES `fipop`.`actor` (`id`)
                    ON DELETE CASCADE
                    ON UPDATE NO ACTION,
            CONSTRAINT `fk_actor_has_category_set_category_set`
                FOREIGN KEY (`category_set_id`)
                    REFERENCES `fipop`.`category_set` (`id`)
                    ON DELETE CASCADE
                    ON UPDATE NO ACTION,
            CONSTRAINT `pk_actor_has_category_set` PRIMARY KEY (actor_id, category_set_id)
        );

        CREATE INDEX fk_actor_has_category_set_actor1_idx ON `fipop`.`actor_has_category_set` (actor_id ASC);
        CREATE INDEX fk_actor_has_category_set_category_set1_idx ON `fipop`.`actor_has_category_set` (category_set_id ASC);

        CREATE TABLE `fipop`.`actor_has_category`
        (
            `actor_id`    INT NOT NULL,
            `category_id` INT NOT NULL,
            CONSTRAINT `fk_actor_has_category_actor`
                FOREIGN KEY (`actor_id`)
                    REFERENCES `fipop`.`actor` (`id`)
                    ON DELETE CASCADE
                    ON UPDATE NO ACTION,
            CONSTRAINT `fk_actor_has_category_category`
                FOREIGN KEY (`category_id`)
                    REFERENCES `fipop`.`category` (`id`)
                    ON DELETE CASCADE
                    ON UPDATE NO ACTION,
            CONSTRAINT `pk_actor_has_category_set` PRIMARY KEY (actor_id, category_id)
        );

        CREATE INDEX fk_actor_has_category_actor1_idx ON `fipop`.`actor_has_category` (actor_id ASC);
        CREATE INDEX fk_actor_has_category_category1_idx ON `fipop`.`actor_has_category` (category_id ASC);


        create view `fipop`.`analysis_segment_structure_element` as
        (select 'SEGMENT'                        as structure_element_type,
                analysisSegment.id               as id,
                analysisSegment.analysis_list_id as analysis_list_id,
                analysisSegment.start_time       as start_time,
                analysisSegment.end_time         as end_time,
                translation.name                 as name
         from analysis_segment analysisSegment
                  join analysis_segment_translation translation
                       on analysisSegment.id = translation.analysis_segment_id)
        UNION
        (select 'SEQUENCE'                       as structure_element_type,
                analysisSequence.id              as id,
                analysisSegment.analysis_list_id as analysis_list_id,
                analysisSequence.start_time      as start_time,
                analysisSequence.end_time        as end_time,
                translation.name                 as name
         from analysis_sequence analysisSequence
                  join analysis_segment analysisSegment on analysisSequence.analysis_segment_id = analysisSegment.id
                  join analysis_sequence_translation translation
                       on analysisSequence.id = translation.analysis_sequence_id)
        UNION
        (select 'TAKE'                           as structure_element_type,
                analysisTake.id                  as id,
                analysisSegment.analysis_list_id as analysis_list_id,
                analysisTake.start_time          as start_time,
                analysisTake.end_time            as end_time,
                translation.name                 as name
         from analysis_take analysisTake
                  join analysis_sequence analysisSequence on analysisTake.analysis_sequence_id = analysisSequence.id
                  join analysis_segment analysisSegment on analysisSequence.analysis_segment_id = analysisSegment.id
                  join analysis_take_translation translation
                       on analysisTake.id = translation.analysis_take_id)
        UNION
        (select 'SCENE'                          as structure_element_type,
                analysisScene.id                 as id,
                analysisSegment.analysis_list_id as analysis_list_id,
                analysisScene.start_time         as start_time,
                analysisScene.end_time           as end_time,
                translation.name                 as name
         from analysis_scene analysisScene
                  join analysis_segment analysisSegment on analysisScene.analysis_segment_id = analysisSegment.id
                  join analysis_scene_translation translation
                       on analysisScene.id = translation.analysis_scene_id)
        UNION
        (select 'ACTION'                         as structure_element_type,
                analysisAction.id                as id,
                analysisSegment.analysis_list_id as analysis_list_id,
                analysisAction.start_time        as start_time,
                analysisAction.end_time          as end_time,
                translation.name                 as name
         from analysis_action analysisAction
                  join analysis_scene analysisScene on analysisAction.analysis_scene_id = analysisScene.id
                  join analysis_segment analysisSegment on analysisScene.analysis_segment_id = analysisSegment.id
                  join analysis_action_translation translation
                       on analysisAction.id = translation.analysis_action_id);

        create view `fipop`.`analysis_segment_structure_element_has_category` as
        (select structureElement.id                     as id,
                structureElement.structure_element_type as structure_element_type,
                hasCategory.category_id                 as category_id
         from analysis_segment_structure_element structureElement
                  join analysis_segment_has_category hasCategory
                       on structureElement.id = hasCategory.analysis_segment_id
         where structure_element_type = 'SEGMENT')
        UNION
        (select structureElement.id                     as id,
                structureElement.structure_element_type as structure_element_type,
                hasCategory.category_id                 as category_id
         from analysis_segment_structure_element structureElement
                  join analysis_sequence_has_category hasCategory
                       on structureElement.id = hasCategory.analysis_sequence_id
         where structure_element_type = 'SEQUENCE')
        UNION
        (select structureElement.id                     as id,
                structureElement.structure_element_type as structure_element_type,
                hasCategory.category_id                 as category_id
         from analysis_segment_structure_element structureElement
                  join analysis_take_has_category hasCategory on structureElement.id = hasCategory.analysis_take_id
         where structure_element_type = 'TAKE')
        UNION
        (select structureElement.id                     as id,
                structureElement.structure_element_type as structure_element_type,
                hasCategory.category_id                 as category_id
         from analysis_segment_structure_element structureElement
                  join analysis_scene_has_category hasCategory on structureElement.id = hasCategory.analysis_scene_id
         where structure_element_type = 'SCENE')
        UNION
        (select structureElement.id                     as id,
                structureElement.structure_element_type as structure_element_type,
                hasCategory.category_id                 as category_id
         from analysis_segment_structure_element structureElement
                  join analysis_action_has_category hasCategory on structureElement.id = hasCategory.analysis_action_id
         where structure_element_type = 'ACTION');
    END IF;
END
$$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE update_to_1_0_0()
BEGIN
    DECLARE db_major_version int;
    DECLARE db_minor_version int;
    DECLARE db_patch_version int;

    SELECT major_version, minor_version, patch_version
    INTO db_major_version, db_minor_version, db_patch_version
    FROM db_version
    LIMIT 1;

    IF db_major_version = 0 AND db_minor_version = 15 AND db_patch_version = 1 THEN
        SELECT 'execute update to 1.0.0' AS log_message;
        DELETE FROM db_version;
        INSERT INTO db_version (major_version, minor_version, patch_version) VALUES (1, 0, 0);

        CREATE TABLE `fipop`.`transcription_engine`
        (
            `engine_identifier` VARCHAR(45) primary key,
            `engine_name`       VARCHAR(45) not null,
            `active`            BOOL        not null default true
        )
            ENGINE = InnoDB;

        CREATE TABLE `fipop`.`transcription_model`
        (
            `model_identifier`  VARCHAR(45) primary key,
            `engine_identifier` VARCHAR(45) not null,
            `default`           bool        not null default false,
            `active`            bool        not null default true,
            CONSTRAINT `fk_transcription_model_transcription_engine`
                FOREIGN KEY (`engine_identifier`)
                    REFERENCES `fipop`.`transcription_engine` (`engine_identifier`)
                    ON DELETE CASCADE
                    ON UPDATE NO ACTION
        )
            ENGINE = InnoDB;
        CREATE INDEX fk_transcription_model_engine_identifier_idx ON `fipop`.`transcription_model` (engine_identifier ASC);

        CREATE TABLE `fipop`.`transcription_state`
        (
            `id` int primary key
        )
            ENGINE = InnoDB;

        CREATE TABLE `fipop`.`transcription_state_translation`
        (
            `id`                     int primary key,
            `transcription_state_id` int not null,
            `language_id`            int not null,
            `state_name`             varchar(20),
            CONSTRAINT `fk_transcription_state_translation_transcription_state`
                FOREIGN KEY (`transcription_state_id`)
                    REFERENCES `fipop`.`transcription_state` (`id`)
                    ON DELETE CASCADE
                    ON UPDATE NO ACTION,
            CONSTRAINT `fk_transcription_state_translation_language`
                FOREIGN KEY (`language_id`)
                    REFERENCES `fipop`.`language` (`id`)
                    ON DELETE CASCADE
                    ON UPDATE NO ACTION
        )
            ENGINE = InnoDB;
        CREATE INDEX fk_transcription_state_translation_transcription_state_idx on `fipop`.`transcription_state_translation` (`transcription_state_id` ASC);
        CREATE INDEX fk_transcription_state_translation_language_idx on `fipop`.`transcription_state_translation` (`language_id` ASC);

        CREATE TABLE `fipop`.`transcription_type`
        (
            `id` int primary key
        )
            ENGINE = InnoDB;

        CREATE TABLE `fipop`.`transcription_type_translation`
        (
            `id`                    int primary key,
            `transcription_type_id` int not null,
            `language_id`           int not null,
            `state_name`            varchar(20),
            CONSTRAINT `fk_transcription_type_translation_transcription_state`
                FOREIGN KEY (`transcription_type_id`)
                    REFERENCES `fipop`.`transcription_type` (`id`)
                    ON DELETE CASCADE
                    ON UPDATE NO ACTION,
            CONSTRAINT `fk_transcription_type_translation_language`
                FOREIGN KEY (`language_id`)
                    REFERENCES `fipop`.`language` (`id`)
                    ON DELETE CASCADE
                    ON UPDATE NO ACTION
        )
            ENGINE = InnoDB;
        CREATE INDEX fk_transcription_type_translation_transcription_type_idx on `fipop`.`transcription_type_translation` (`transcription_type_id` ASC);
        CREATE INDEX fk_transcription_type_translation_language_idx on `fipop`.`transcription_type_translation` (`language_id` ASC);

        CREATE TABLE `fipop`.`transcription`
        (
            `id`                             int primary key,
            `name`                           varchar(150) not null,
            `model_identifier`               varchar(45),
            `engine_identifier`              varchar(45),
            `medium_id`                      int          not null,
            `transcription_state_id`         int          not null,
            `transcription_type_id`          int          not null ,
            `transcription_task_id`          bigint,
            `created_at`                     timestamp    not null default CURRENT_TIMESTAMP,
            `last_edited_at`                 timestamp,
            `created_by_user_account_id`     int,
            `last_edited_by_user_account_id` int,
            CONSTRAINT `fk_transcription_transcription_model`
                FOREIGN KEY (`model_identifier`)
                    REFERENCES `fipop`.`transcription_model` (`model_identifier`)
                    ON DELETE SET NULL
                    ON UPDATE NO ACTION,
            CONSTRAINT `fk_transcription_transcription_engine`
                FOREIGN KEY (`engine_identifier`)
                    REFERENCES `fipop`.`transcription_engine` (`engine_identifier`)
                    ON DELETE SET NULL
                    ON UPDATE NO ACTION,
            CONSTRAINT `fk_transcription_medium`
                FOREIGN KEY (`medium_id`)
                    REFERENCES `fipop`.`medium` (`id`)
                    ON DELETE CASCADE
                    ON UPDATE NO ACTION,
            CONSTRAINT `fk_transcription_transcription_state`
                FOREIGN KEY (`transcription_state_id`)
                    REFERENCES `fipop`.`transcription_state` (`id`),
            CONSTRAINT `fk_transcription_transcription_type`
                FOREIGN KEY (`transcription_type_id`)
                    REFERENCES `fipop`.`transcription_type` (`id`),
            CONSTRAINT `fk_transcription_creation_user`
                FOREIGN KEY (`created_by_user_account_id`)
                    REFERENCES `fipop`.`user_account` (`id`)
                    ON DELETE SET NULL
                    ON UPDATE NO ACTION,
            CONSTRAINT `fk_transcription_edit_user`
                FOREIGN KEY (`last_edited_by_user_account_id`)
                    REFERENCES `fipop`.`user_account` (`id`)
                    ON DELETE SET NULL
                    ON UPDATE NO ACTION
        )
            ENGINE = InnoDB;
        CREATE INDEX `fk_transcription_transcription_model_idx` on `fipop`.`transcription` (`model_identifier` ASC);
        CREATE INDEX `fk_transcription_transcription_engine_idx` on `fipop`.`transcription` (`engine_identifier` ASC);
        CREATE INDEX `fk_transcription_transcription_medium_idx` on `fipop`.`transcription` (`medium_id` ASC);
        CREATE INDEX `fk_transcription_transcription_transcription_state_idx` on `fipop`.`transcription` (`transcription_state_id` ASC);
        CREATE INDEX `fk_transcription_transcription_transcription_type_idx` on `fipop`.`transcription` (`transcription_type_id` ASC);
        CREATE INDEX `fk_transcription_creation_user_idx` on `fipop`.`transcription` (`created_by_user_account_id` ASC);
        CREATE INDEX `fk_transcription_edit_user_idx` on `fipop`.`transcription` (`last_edited_by_user_account_id` ASC);

        CREATE TABLE `fipop`.`system_settings`
        (
            `id`                             smallint primary key default 1,
            `auto_transcribe_uploads`        bool      not null   default false,
            `created_at`                     timestamp not null   default CURRENT_TIMESTAMP,
            `last_edited_at`                 timestamp,
            `last_edited_by_user_account_id` int,
            CONSTRAINT `fk_system_settings_user`
                FOREIGN KEY (`last_edited_by_user_account_id`)
                    REFERENCES `fipop`.`user_account` (`id`)
                    ON DELETE SET NULL
                    ON UPDATE NO ACTION,
            CONSTRAINT system_settings_singleton CHECK (id = 1)
        )
            ENGINE = InnoDB;
        CREATE INDEX `fk_system_settings_user_idx` on `fipop`.`system_settings` (`last_edited_by_user_account_id` ASC);

        ALTER TABLE `fipop`.`medium`
            ADD COLUMN `default_transcription_id` int;
        ALTER TABLE `fipop`.`medium`
            ADD CONSTRAINT `fk_medium_transcription` FOREIGN KEY (`default_transcription_id`)
                REFERENCES `fipop`.`transcription` (`id`)
                ON DELETE SET NULL
                ON UPDATE NO ACTION;
        CREATE INDEX `fk_medium_transcription_idx` on `fipop`.`medium` (`default_transcription_id` ASC);

        INSERT INTO transcription_state VALUES (1), (2), (3), (4), (5);
        INSERT INTO transcription_state_translation (id, transcription_state_id, language_id, state_name)
        VALUES (1, 1, 1, 'prepare'),
               (2, 1, 1, 'pending'),
               (3, 2, 1, 'running'),
               (4, 3, 1, 'completed'),
               (5, 4, 1, 'failed');

        INSERT INTO transcription_type VALUES (1), (2);
        INSERT INTO transcription_type_translation (id, transcription_type_translation.transcription_type_id, language_id, state_name)
        VALUES (1, 1, 1, 'generated'),
               (2, 1, 1, 'imported');


        INSERT INTO system_settings(id, auto_transcribe_uploads) VALUES (1, TRUE);

    END IF;
END $$
DELIMITER ;

/**
 * Call update functions
 */
CALL update_to_0_14_0();
CALL update_to_0_14_1();
CALL update_to_0_14_2();
CALL update_to_0_15_0();
CALL update_to_0_15_1();
CALL update_to_1_0_0();

/*
 * Delete update functions after finishing update script
 */
DROP PROCEDURE update_to_0_14_0;
DROP PROCEDURE update_to_0_14_1;
DROP PROCEDURE update_to_0_14_2;
DROP PROCEDURE update_to_0_15_0;
DROP PROCEDURE update_to_0_15_1;
DROP PROCEDURE update_to_1_0_0;