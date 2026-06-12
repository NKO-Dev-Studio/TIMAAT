/*
 Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

/**
 * @author Jens-Martin Loebel <loebel@bitgilde.de>
 * @author Mirko Scherf <mscherf@uni-mainz.de>
 */
'use strict';
(function (factory, window) {
  /*globals define, module, require*/

  // define an AMD module that relies on 'TIMAAT'
  if (typeof define === 'function' && define.amd) {
    define(['TIMAAT'], factory);


    // define a Common JS module that relies on 'TIMAAT'
  } else if (typeof exports === 'object') {
    module.exports = factory(require('TIMAAT'));
  }

  // attach your plugin to the global 'TIMAAT' variable
  if (typeof window !== 'undefined' && window.TIMAAT) {
    factory(window.TIMAAT);
  }

}(function (TIMAAT) {

  TIMAAT.MediumDatasets = {
    allMediaList: null,
    _transcriptionViewerCleanup: null,
    audios: null,
    container: 'media',
    documents: null,
    images: null,
    media: null,
    mediaLoaded: false,
    mediaTypes: null,
    softwares: null,
    texts: null,
    titles: null,
    videogames: null,
    videos: null,
    producerId: 0,
    authorId: 0,

    init: function () {
      this.initMedia();
      // this.initMediumTypes();
      this.initTitles();
      this.initLanguageTracks();
      this.initActorRoles();
      this.initMusic();
      this.initTranscriptions();

      TIMAAT.EntityUpdate.registerEntityUpdateListener("medium-audio-analysis", this.handleMediumAudioAnalysisUpdateMessage.bind(this))
      TIMAAT.EntityUpdate.registerEntityUpdateListener("medium", this.handleMediumUpdateMessage)
    },

    initMediaComponent: function () {
      // console.log("TCL: initMediaComponent");
      if (!this.mediaLoaded) {
        this.setMediumList();
      }
      if (TIMAAT.UI.component != 'media') {
        TIMAAT.UI.showComponent('media');
        $('#mediumTab').trigger('click');
      }
    },

    initMediumTypes: function () {
      // console.log("TCL: MediumDatasets: initMediumTypes: function()");
      // delete mediaType functionality
      $('#mediaTypeDeleteSubmitButton').on('click', function (ev) {
        var modal = $('#mediumDatasetsMediumTypeDeleteModal');
        let type = modal.data('mediaType');
        if (type) TIMAAT.MediumDatasets._mediaTypeRemoved(type);
        modal.modal('hide');
      });

      // add mediaType button
      $('#mediaTypeAddButton').attr('onclick', 'TIMAAT.MediumDatasets.addMediumType()');

      // add/edit mediaType functionality
      $('#mediumDatasetsMediumTypeMeta').on('show.bs.modal', function (ev) {
        // Create/Edit mediaType window setup
        var modal = $(this);
        var mediaType = modal.data('mediaType');
        var heading = (mediaType) ? "Edit medium type" : "Add medium type";
        var submit = (mediaType) ? "Save" : "Add";
        var type = (mediaType) ? mediaType.model.type : 0;
        var hasVisual = (mediaType) ? mediaType.model.hasVisual : false;
        var hasAudio = (mediaType) ? mediaType.model.hasAudio : false;
        var hasContent = (mediaType) ? mediaType.model.hasContent : false;
        // setup UI
        $('#mediaTypeMetaLabel').html(heading);
        $('#mediumDatasetsMediumTypeMetaSubmit').html(submit);
        $('#mediumDatasetsMediumTypeMetaName').val(type).trigger('input');
        $('#mediumDatasetsMediumTypeMetaHasVisual').val(hasVisual);
        $('#mediumDatasetsMediumTypeMetaHasAudio').val(hasAudio);
        $('#mediumDatasetsMediumTypeMetaHasContent').val(hasContent);
      });

      // Submit mediaType data
      $('#mediumDatasetsMediumTypeMetaSubmit').on('click', function (ev) {
        // Create/Edit mediaType window submitted data validation
        var modal = $('#mediumDatasetsMediumTypeMeta');
        var mediaType = modal.data('mediaType');
        var type = $('#mediumDatasetsMediumTypeMetaName').val();
        var hasVisual = $('#mediumDatasetsMediumTypeMetaHasVisual').val();
        var hasAudio = $('#mediumDatasetsMediumTypeMetaHasAudio').val();
        var hasContent = $('#mediumDatasetsMediumTypeMetaHasContent').val();

        if (mediaType) {
          mediaType.model.medium.mediaTypeTranslations[0].type = type;
          mediaType.model.hasVisual = hasVisual;
          mediaType.model.hasAudio = hasAudio;
          mediaType.model.hasContent = hasContent;
          // mediaType.updateUI();
          TIMAAT.MediumService.updateMediumType(mediaType);
          TIMAAT.MediumService.updateMediumTypeTranslation(mediaType);
        } else { // create new mediaType
          var model = {
            id: 0,
            hasVisual: hasVisual,
            hasAudio: hasAudio,
            hasContent: hasContent,
            mediaTypeTranslations: [],
          };
          var modelTranslation = {
            id: 0,
            type: type,
          }
          TIMAAT.MediumService.createMediumType(model, modelTranslation, TIMAAT.MediumDatasets._mediaTypeAdded); // TODO add mediaType parameters
        }
        modal.modal('hide');
      });

      // validate mediaType data
      // TODO validate all required fields
      $('#mediumDatasetsMediumTypeMetaName').on('input', function (ev) {
        if ($('#mediumDatasetsMediumTypeMetaName').val().length > 0) {
          $('#mediumDatasetsMediumTypeMetaSubmit').prop('disabled', false);
          $('#mediumDatasetsMediumTypeMetaSubmit').removeAttr('disabled');
        } else {
          $('#mediumDatasetsMediumTypeMetaSubmit').prop('disabled', true);
          $('#mediumDatasetsMediumTypeMetaSubmit').attr('disabled');
        }
      });
    },

    initMedia: function () {
      // nav-bar functionality
      $('#mediumTab').on('click', function (event) {
        TIMAAT.MediumDatasets.initMediaComponent();
        TIMAAT.MediumDatasets.loadMedia();
        TIMAAT.UI.displayComponent('medium', 'mediumTab', 'mediumDataTable');
        $('#mediumDatasetsAllMedia').trigger('click');
      });

      $('#audioTab').on('click', function (event) {
        TIMAAT.MediumDatasets.loadMediumSubtype('audio');
        TIMAAT.UI.displayComponent('medium', 'audioTab', 'audioDataTable', null, null, 'audio');
        $('#mediumDatasetsAllAudios').trigger('click');
      });

      $('#documentTab').on('click', function (event) {
        TIMAAT.MediumDatasets.loadMediumSubtype('document');
        TIMAAT.UI.displayComponent('medium', 'documentTab', 'documentDataTable', null, null, 'document');
        $('#mediumDatasetsAllDocuments').trigger('click');
      });

      $('#imageTab').on('click', function (event) {
        TIMAAT.MediumDatasets.loadMediumSubtype('image');
        TIMAAT.UI.displayComponent('medium', 'imageTab', 'imageDataTable', null, null, 'image');
        $('#mediumDatasetsAllImages').trigger('click');
      });

      $('#softwareTab').on('click', function (event) {
        TIMAAT.MediumDatasets.loadMediumSubtype('software');
        TIMAAT.UI.displayComponent('medium', 'softwareTab', 'softwareDataTable', null, null, 'software');
        $('#mediumDatasetsAllSoftwares').trigger('click');
      });

      $('#textTab').on('click', function (event) {
        TIMAAT.MediumDatasets.loadMediumSubtype('text');
        TIMAAT.UI.displayComponent('medium', 'textTab', 'textDataTable', null, null, 'text');
        $('#mediumDatasetsAllTexts').trigger('click');
      });

      $('#videoTab').on('click', function (event) {
        TIMAAT.MediumDatasets.loadMediumSubtype('video');
        TIMAAT.UI.displayComponent('medium', 'videoTab', 'videoDataTable', null, null, 'video');
        $('#mediumDatasetsAllVideos').trigger('click');
      });

      $('#videogameTab').on('click', function (event) {
        TIMAAT.MediumDatasets.loadMediumSubtype('videogame');
        TIMAAT.UI.displayComponent('medium', 'videogameTab', 'videogameDataTable', null, null, 'videogame');
        $('#mediumDatasetsAllVideogames').trigger('click');
      });

      $('#mediumMetadataTab').on('click', function (event) {
        let medium = $('#mediumFormMetadata').data('medium');
        let type = medium.model.mediaType.mediaTypeTranslations[0].type;
        let name = medium.model.displayTitle.name;
        let id = medium.model.id;
        TIMAAT.UI.displayDataSetContentArea('mediumFormMetadata');
        TIMAAT.UI.displayDataSetContent('dataSheet', medium, 'medium');
        if (type == 'medium') {
          TIMAAT.URLHistory.setURL(null, name + ' · Datasets · ' + type[0].toUpperCase() + type.slice(1), '#medium/' + id);
        } else {
          TIMAAT.URLHistory.setURL(null, name + ' · Datasets · ' + type[0].toUpperCase() + type.slice(1), '#medium/' + type + '/' + id);
        }
      });

      $('#mediumPreviewTab').on('click', function (event) {
        let medium = $('#mediumFormMetadata').data('medium');
        let type = medium.model.mediaType.mediaTypeTranslations[0].type;
        let name = medium.model.displayTitle.name;
        let id = medium.model.id;
        TIMAAT.UI.displayDataSetContentArea('mediumFormPreview');
        TIMAAT.UI.displayDataSetContent('preview', medium, 'medium');
        if (type == 'medium') {
          TIMAAT.URLHistory.setURL(null, name + ' · Preview · ' + type[0].toUpperCase() + type.slice(1), '#medium/' + id + '/preview');
        } else {
          TIMAAT.URLHistory.setURL(null, name + ' · Preview · ' + type[0].toUpperCase() + type.slice(1), '#medium/' + type + '/' + id + '/preview');
        }
      });

      $('#mediumDatasetsAllMedia').on('click', async function (event) {
        TIMAAT.UI.displayDataSetContentArea('mediumAllMedia');
        $('#mediumNavTabs').hide();
        if (!TIMAAT.MediumDatasets.dataTableAllMediaList) {
          await TIMAAT.MediumDatasets.setupAllMediaDataTable();
        }
        TIMAAT.MediumDatasets.dataTableAllMediaList.ajax.url('/TIMAAT/api/medium/list')
        TIMAAT.MediumDatasets.dataTableAllMediaList.ajax.reload().draw();
        TIMAAT.UI.clearLastSelection('medium');
        $('#mediumFormMetadata').data('medium', null);
        $('#mediumFormMetadata').data('type', null);
        $('#mediumDatasetsAllMedia').addClass('list-group-item--is-active');
        TIMAAT.URLHistory.setURL(null, 'Media Library', '#medium/list');
      });

      $('#mediumDatasetsAllAudios').on('click', async function (event) {
        TIMAAT.UI.displayDataSetContentArea('mediumAllAudios');
        $('#mediumNavTabs').hide();
        if (!TIMAAT.MediumDatasets.dataTableAllAudiosList) {
          await TIMAAT.MediumDatasets.setupAllAudiosDataTable();
        }
        TIMAAT.MediumDatasets.dataTableAllAudiosList.ajax.url('/TIMAAT/api/medium/audio/list')
        TIMAAT.MediumDatasets.dataTableAllAudiosList.ajax.reload().draw();
        TIMAAT.UI.clearLastSelection('audio');
        $('#mediumFormMetadata').data('medium', null);
        $('#mediumFormMetadata').data('type', null);
        $('#mediumDatasetsAllAudios').addClass('active');
        TIMAAT.URLHistory.setURL(null, 'Audio Library', '#medium/audio/list');
      });

      $('#mediumDatasetsAllDocuments').on('click', async function (event) {
        TIMAAT.UI.displayDataSetContentArea('mediumAllDocuments');
        $('#mediumNavTabs').hide();
        if (!TIMAAT.MediumDatasets.dataTableAllDocumentsList) {
          await TIMAAT.MediumDatasets.setupAllDocumentsDataTable();
        }
        TIMAAT.MediumDatasets.dataTableAllDocumentsList.ajax.url('/TIMAAT/api/medium/document/list')
        TIMAAT.MediumDatasets.dataTableAllDocumentsList.ajax.reload().draw();
        TIMAAT.UI.clearLastSelection('document');
        $('#mediumFormMetadata').data('medium', null);
        $('#mediumFormMetadata').data('type', null);
        $('#mediumDatasetsAllDocuments').addClass('active');
        TIMAAT.URLHistory.setURL(null, 'Document Library', '#medium/document/list');
      });

      $('#mediumDatasetsAllImages').on('click', async function (event) {
        TIMAAT.UI.displayDataSetContentArea('mediumAllImages');
        $('#mediumNavTabs').hide();
        if (!TIMAAT.MediumDatasets.dataTableAllImagesList) {
          await TIMAAT.MediumDatasets.setupAllImagesDataTable();
        }
        TIMAAT.MediumDatasets.dataTableAllImagesList.ajax.url('/TIMAAT/api/medium/image/list')
        TIMAAT.MediumDatasets.dataTableAllImagesList.ajax.reload().draw();
        TIMAAT.UI.clearLastSelection('image');
        $('#mediumFormMetadata').data('medium', null);
        $('#mediumFormMetadata').data('type', null);
        $('#mediumDatasetsAllImages').addClass('active');
        TIMAAT.URLHistory.setURL(null, 'Image Library', '#medium/image/list');
      });

      $('#mediumDatasetsAllSoftwares').on('click', async function (event) {
        TIMAAT.UI.displayDataSetContentArea('mediumAllSoftwares');
        $('#mediumNavTabs').hide();
        if (!TIMAAT.MediumDatasets.dataTableAllSoftwaresList) {
          await TIMAAT.MediumDatasets.setupAllSoftwaresDataTable();
        }
        TIMAAT.MediumDatasets.dataTableAllSoftwaresList.ajax.url('/TIMAAT/api/medium/software/list')
        TIMAAT.MediumDatasets.dataTableAllSoftwaresList.ajax.reload().draw();
        TIMAAT.UI.clearLastSelection('software');
        $('#mediumFormMetadata').data('medium', null);
        $('#mediumFormMetadata').data('type', null);
        $('#mediumDatasetsAllSoftwares').addClass('active');
        TIMAAT.URLHistory.setURL(null, 'Software Library', '#medium/software/list');
      });

      $('#mediumDatasetsAllTexts').on('click', async function (event) {
        TIMAAT.UI.displayDataSetContentArea('mediumAllTexts');
        $('#mediumNavTabs').hide();
        if (!TIMAAT.MediumDatasets.dataTableAllTextsList) {
          await TIMAAT.MediumDatasets.setupAllTextsDataTable();
        }
        TIMAAT.MediumDatasets.dataTableAllTextsList.ajax.url('/TIMAAT/api/medium/text/list')
        TIMAAT.MediumDatasets.dataTableAllTextsList.ajax.reload().draw();
        TIMAAT.UI.clearLastSelection('text');
        $('#mediumFormMetadata').data('medium', null);
        $('#mediumFormMetadata').data('type', null);
        $('#mediumDatasetsAllTexts').addClass('active');
        TIMAAT.URLHistory.setURL(null, 'Text Library', '#medium/text/list');
      });

      $('#mediumDatasetsAllVideos').on('click', async function (event) {
        TIMAAT.UI.displayDataSetContentArea('mediumAllVideos');
        $('#mediumNavTabs').hide();
        if (!TIMAAT.MediumDatasets.dataTableAllVideosList) {
          await TIMAAT.MediumDatasets.setupAllVideosDataTable();
        }
        TIMAAT.MediumDatasets.dataTableAllVideosList.ajax.url('/TIMAAT/api/medium/video/list')
        TIMAAT.MediumDatasets.dataTableAllVideosList.ajax.reload().draw();
        TIMAAT.UI.clearLastSelection('video');
        $('#mediumFormMetadata').data('medium', null);
        $('#mediumFormMetadata').data('type', null);
        $('#mediumDatasetsAllVideos').addClass('active');
        TIMAAT.URLHistory.setURL(null, 'Video Library', '#medium/video/list');
      });

      $('#mediumDatasetsAllVideogames').on('click', async function (event) {
        TIMAAT.UI.displayDataSetContentArea('mediumAllVideogames');
        $('#mediumNavTabs').hide();
        if (!TIMAAT.MediumDatasets.dataTableAllVideogamesList) {
          await TIMAAT.MediumDatasets.setupAllVideogamesDataTable();
        }
        TIMAAT.MediumDatasets.dataTableAllVideogamesList.ajax.url('/TIMAAT/api/medium/videogame/list')
        TIMAAT.MediumDatasets.dataTableAllVideogamesList.ajax.reload().draw();
        TIMAAT.UI.clearLastSelection('videogame');
        $('#mediumFormMetadata').data('medium', null);
        $('#mediumFormMetadata').data('type', null);
        $('#mediumDatasetsAllVideogames').addClass('active');
        TIMAAT.URLHistory.setURL(null, 'Videogame Library', '#medium/videogame/list');
      });

      // add medium button functionality (in medium list - opens data sheet form)
      //* media have to be created via sub-medium
      // $('#mediumDatasetsMediumAddButton').on('click', function(event) {
      // 	$('#mediumFormMetadata').data('type', 'medium');
      // 	$('#mediumFormMetadata').data('medium', null);
      // 	TIMAAT.MediumDatasets.addMedium('medium');
      // });

      // delete medium button (in form) handler
      $('.mediumDataSheetFormDeleteButton').on('click', function (event) {
        event.stopPropagation();
        TIMAAT.UI.hidePopups();
        $('#mediumVideoPreview').get(0).pause();
        $('#mediumDatasetsMediumDeleteModal').data('medium', $('#mediumFormMetadata').data('medium'));
        $('#mediumDatasetsMediumDeleteModal').modal('show');
      });

      $('.mediumDataSheetFormCaptureThumbnail').on('click', async function (event) {
        const mediumId = $('#mediumFormMetadata').data('medium').model.id;

        const currentVideoPreviewTimeSeconds = $('#mediumVideoPreview').get(0).currentTime
        const currentVideoPreviewTimeMillis = Math.round(currentVideoPreviewTimeSeconds * 1000)

        await TIMAAT.MediumService.updateMediumThumbnail(mediumId, currentVideoPreviewTimeMillis)
      })

      // confirm delete medium modal functionality
      $('#mediumDatasetsModalDeleteSubmitButton').on('click', async function (event) {
        let modal = $('#mediumDatasetsMediumDeleteModal');
        let medium = modal.data('medium');
        let type = medium.model.mediaType.mediaTypeTranslations[0].type;
        // console.log("TCL: $ -> type", type);
        if (medium) {
          try {
            await TIMAAT.MediumDatasets._mediumRemoved(medium);
          } catch (error) {
            console.error("ERROR: ", error);
          }
          try {
            if ($('#mediumTab').hasClass('active')) {
              await TIMAAT.UI.refreshDataTable('medium');
            } else {
              await TIMAAT.UI.refreshDataTable(type);
            }
          } catch (error) {
            console.error("ERROR: ", error);
          }
        }
        modal.modal('hide');
        if ($('#mediumTab').hasClass('active')) {
          $('#mediumTab').trigger('click');
        } else {
          $('#' + type + 'Tab').trigger('click');
        }
      });

      // edit content form button handler
      $('.mediumDataSheetFormEditButton').on('click', function (event) {
        event.stopPropagation();
        TIMAAT.UI.hidePopups();
        let medium = $('#mediumFormMetadata').data('medium');
        TIMAAT.UI.displayDataSetContent(TIMAAT.UI.subNavTab, medium, 'medium', 'edit');
      });

      // medium form handlers
      // submit medium metadata button functionality
      $('#mediumFormMetadataSubmitButton').on('click', async function (event) {
        // continue only if client side validation has passed
        event.preventDefault();
        if (!$('#mediumFormMetadata').valid()) return false;

        var medium = $('#mediumFormMetadata').data('medium');
        // console.log("TCL: $ -> medium", medium);
        var type = $('#mediumFormMetadata').data('type');
        // console.log("TCL: $ -> type", type);

        // create/edit medium window submitted data
        TIMAAT.MediumDatasets.disableReadOnlyDataFields(false);
        var formData = $('#mediumFormMetadata').serializeArray();
        TIMAAT.MediumDatasets.disableReadOnlyDataFields(true);
        var formDataObject = {};
        $(formData).each(function (i, field) {
          formDataObject[field.name] = field.value;
        });
        // console.log("TCL: formDataObject", formDataObject);
        // sanitize form data
        var formDataSanitized = formDataObject;
        formDataSanitized.displayTitleLanguageId = Number(formDataObject.displayTitleLanguageId);
        formDataSanitized.isEpisode = (formDataObject.isEpisode == "on") ? true : false;
        formDataSanitized.length = TIMAAT.Util.parseTime(formDataObject.length);
        formDataSanitized.recordingEndDate = moment.utc(formDataObject.recordingEndDate, "YYYY-MM-DD");
        formDataSanitized.recordingStartDate = moment.utc(formDataObject.recordingStartDate, "YYYY-MM-DD");
        formDataSanitized.releaseDate = moment.utc(formDataObject.releaseDate, "YYYY-MM-DD");
        formDataSanitized.sourceIsPrimarySource = (formDataObject.sourceIsPrimarySource == "on") ? true : false;
        formDataSanitized.sourceIsStillAvailable = (formDataObject.sourceIsStillAvailable == "on") ? true : false;
        formDataSanitized.sourceLastAccessed = moment.utc(formDataObject.sourceLastAccessed, "YYYY-MM-DD HH:mm");
        formDataSanitized.sourceUrl = (formDataObject.sourceUrl.length == 0) ? null : formDataObject.sourceUrl;
        // console.log("TCL: formDataSanitized", formDataSanitized);

        if (medium) { // update medium
          // medium data
          medium.model = await TIMAAT.MediumDatasets.updateMediumModelData(medium.model, formDataSanitized);
          // medium subtype data
          switch (type) {
            case "audio":
              if (!medium.model.mediumAudio.audioPostProduction) { //* for existing audio media which were created before audio post production was added
                let audioPostProductionModel = await TIMAAT.MediumService.createAudioPostProduction();
                let audioPostProductionTranslationModel = await TIMAAT.MediumDatasets.createAudioPostProductionTranslationModel(formDataSanitized);
                audioPostProductionTranslationModel.audioPostProduction.id = audioPostProductionModel.id;
                audioPostProductionTranslationModel = await TIMAAT.MediumService.createAudioPostProductionTranslation(audioPostProductionTranslationModel);
                medium.model.mediumAudio.audioPostProduction = {};
                medium.model.mediumAudio.audioPostProduction.id = audioPostProductionModel.id;
                medium.model.mediumAudio.audioPostProduction.audioPostProductionTranslations = [];
                medium.model.mediumAudio.audioPostProduction.audioPostProductionTranslations[0] = audioPostProductionTranslationModel;
              } else {
                medium.model.mediumAudio.audioPostProduction.audioPostProductionTranslations[0].overdubbing = formDataSanitized.overdubbing;
                medium.model.mediumAudio.audioPostProduction.audioPostProductionTranslations[0].reverb = formDataSanitized.reverb;
                medium.model.mediumAudio.audioPostProduction.audioPostProductionTranslations[0].delay = formDataSanitized.delay;
                medium.model.mediumAudio.audioPostProduction.audioPostProductionTranslations[0].panning = formDataSanitized.panning;
                medium.model.mediumAudio.audioPostProduction.audioPostProductionTranslations[0].bass = formDataSanitized.bass;
                medium.model.mediumAudio.audioPostProduction.audioPostProductionTranslations[0].treble = formDataSanitized.treble;
              }
              medium.model.mediumAudio.length = formDataSanitized.length;
              break;
            case "document":
              break;
            case "image":
              medium.model.mediumImage.width = formDataSanitized.width;
              medium.model.mediumImage.height = formDataSanitized.height;
              // medium.model.mediumImage.bitDepth = formDataSanitized.bitDepth;
              break;
            case "software":
              medium.model.mediumSoftware.version = formDataSanitized.version;
              break;
            case "text":
              medium.model.mediumText.content = formDataSanitized.content;
              break;
            case "video":
              medium.model.mediumVideo.length = formDataSanitized.length;
              // medium.model.mediumVideo.videoCodec = formDataSanitized.videoCodec;
              medium.model.mediumVideo.width = formDataSanitized.width;
              medium.model.mediumVideo.height = formDataSanitized.height;
              medium.model.mediumVideo.frameRate = formDataSanitized.frameRate;
              // medium.model.mediumVideo.dataRate = formDataSanitized.dataRate;
              // medium.model.mediumVideo.totalBitrate = formDataSanitized.totalBitrate;
              medium.model.mediumVideo.isEpisode = formDataSanitized.isEpisode;
              break;
            case "videogame":
              medium.model.mediumVideogame.isEpisode = formDataSanitized.isEpisode;
              break;
          }
          medium.model = await TIMAAT.MediumDatasets.updateMedium(type, medium);
          // medium.updateUI();
        } else { // create new medium
          // console.log("TCL: $ -> type", type);
          var mediumModel = await TIMAAT.MediumDatasets.createMediumModel(formDataSanitized, type);
          var displayTitleModel = await TIMAAT.MediumDatasets.createDisplayTitleModel(formDataSanitized);
          var sourceModel = await TIMAAT.MediumDatasets.createSourceModel(formDataSanitized);
          var mediumSubtypeModel = await TIMAAT.MediumDatasets.createMediumSubtypeModel(formDataSanitized, type);
          if (type == 'audio') {
            let audioPostProductionModel = await TIMAAT.MediumService.createAudioPostProduction();
            let audioPostProductionTranslationModel = await TIMAAT.MediumDatasets.createAudioPostProductionTranslationModel(formDataSanitized);
            audioPostProductionTranslationModel.audioPostProduction.id = audioPostProductionModel.id;
            audioPostProductionTranslationModel = await TIMAAT.MediumService.createAudioPostProductionTranslation(audioPostProductionTranslationModel);
            mediumSubtypeModel.audioPostProduction.id = audioPostProductionModel.id;
            mediumSubtypeModel.audioPostProduction.audioPostProductionTranslations[0] = audioPostProductionTranslationModel;
          }
          var newMedium = await TIMAAT.MediumDatasets.createMedium(type, mediumModel, mediumSubtypeModel, displayTitleModel, sourceModel);
          medium = new TIMAAT.Medium(newMedium, type);
          medium.model.fileStatus = 'noFile';
          $('#mediumFormMetadata').data('medium', medium);
          TIMAAT.UI.displayDataSetContentContainer('mediumDataTab', 'mediumFormMetadata', 'medium');
          $('#mediumMetadataTab').trigger('click');
        }
        TIMAAT.MediumDatasets.showAddMediumButton();
        if ($('#mediumTab').hasClass('active')) {
          await TIMAAT.UI.refreshDataTable('medium');
        } else {
          await TIMAAT.UI.refreshDataTable(type);
        }
        // TIMAAT.UI.addSelectedClassToSelectedItem(type, medium.model.id); // is done in displayDataSetContent
        TIMAAT.UI.displayDataSetContent('dataSheet', medium, 'medium');
      });

      // cancel add/edit button in content form functionality
      $('#mediumFormMetadataDismissButton').on('click', async function (event) {
        TIMAAT.MediumDatasets.showAddMediumButton();
        let currentUrlHash = window.location.hash;
        await TIMAAT.URLHistory.setupView(currentUrlHash);
      });

      // category set button handler
      $('#mediumDataSheetFormCategorySetButton').on('click', async function (event) {
        event.stopPropagation();
        TIMAAT.UI.hidePopups();
        var modal = $('#mediumDatasetsMediumCategorySetsModal');
        modal.data('medium', $('#mediumFormMetadata').data('medium'));
        var medium = modal.data('medium');
        modal.find('.modal-body').html(`
					<form role="form" id="mediumCategorySetsForm">
						<div class="form-group">
							<!-- <label for="mediumCategorySetsMultiSelectDropdown">Medium category sets</label> -->
							<div class="col-md-12">
								<select class="form-control form-control-md multi-select-dropdown"
												style="width:100%;"
												id="mediumCategorySetsMultiSelectDropdown"
												name="categorySetId"
												data-role="categorySetId"
												data-placeholder="Select medium category sets"
												multiple="multiple">
								</select>
							</div>
						</div>
					</form>`);
        $('#mediumCategorySetsMultiSelectDropdown').select2({
          closeOnSelect: false,
          scrollAfterSelect: true,
          allowClear: true,
          minimumResultsForSearch: 10,
          ajax: {
            url: 'api/categorySet/selectList/',
            type: 'GET',
            dataType: 'json',
            delay: 250,
            headers: {
              "Authorization": "Bearer " + TIMAAT.Service.token,
              "Content-Type": "application/json",
            },
            // additional parameters
            data: function (params) {
              return {
                search: params.term,
                page: params.page
              };
            },
            processResults: function (data, params) {
              params.page = params.page || 1;
              return {
                results: data
              };
            },
            cache: false
          },
          minimumInputLength: 0,
        });
        TIMAAT.MediumService.getCategorySetList(medium.model.id).then(function (data) {
          // console.log("TCL: then: data", data);
          var categorySetSelect = $('#mediumCategorySetsMultiSelectDropdown');
          if (data.length > 0) {
            data.sort((a, b) => (a.name > b.name) ? 1 : -1);
            // create the options and append to Select2
            var i = 0;
            for (; i < data.length; i++) {
              var option = new Option(data[i].name, data[i].id, true, true);
              categorySetSelect.append(option).trigger('change');
            }
            // manually trigger the 'select2:select' event
            categorySetSelect.trigger({
              type: 'select2:select',
              params: {
                data: data
              }
            });
          }
        });
        modal.modal('show');
      });

      // submit category set modal button functionality
      $('#mediumDatasetsModalCategorySetSubmit').on('click', async function (event) {
        event.preventDefault();
        // console.log("TCL: submit category set list");
        var modal = $('#mediumDatasetsMediumCategorySetsModal');
        if (!$('#mediumCategorySetsForm').valid())
          return false;
        var medium = modal.data('medium');
        // console.log("TCL: medium", medium);
        var formDataRaw = $('#mediumCategorySetsForm').serializeArray();
        // console.log("TCL: formDataRaw", formDataRaw);
        var i = 0;
        var categorySetIdList = [];
        var newCategorySetList = [];
        for (; i < formDataRaw.length; i++) {
          if (isNaN(Number(formDataRaw[i].value))) {
            newCategorySetList.push({id: 0, name: formDataRaw[i].value}); // new category sets that have to be added to the system first
          } else {
            categorySetIdList.push({id: formDataRaw[i].value});
          }
        }
        medium.model = await TIMAAT.MediumDatasets.updateMediumHasCategorySetsList(medium.model, categorySetIdList);
        if (newCategorySetList.length > 0) {
          var updatedMediumModel = await TIMAAT.MediumDatasets.addCategorySetsToMedium(medium.model, newCategorySetList);
          // console.log("TCL: updatedMediumModel", updatedMediumModel);
          medium.model.categorySets = updatedMediumModel.categorySets;
        }
        $('#mediumFormMetadata').data('medium', medium);
        modal.modal('hide');
      });

      // category button handler
      $('#mediumDataSheetFormCategoryButton').on('click', async function (event) {
        event.stopPropagation();
        TIMAAT.UI.hidePopups();
        var modal = $('#mediumDatasetsMediumCategoriesModal');
        modal.data('medium', $('#mediumFormMetadata').data('medium'));
        var medium = modal.data('medium');
        modal.find('.modal-body').html(`
					<form role="form" id="mediumCategoriesForm">
						<div class="form-group">
							<!-- <label for="mediumCategoriesMultiSelectDropdown">Medium categories</label> -->
							<div class="col-md-12">
								<select class="form-control form-control-md multi-select-dropdown"
												style="width:100%;"
												id="mediumCategoriesMultiSelectDropdown"
												name="categoryId"
												data-role="categoryId"
												data-placeholder="Select medium categories"
												multiple="multiple">
								</select>
							</div>
						</div>
					</form>`);
        $('#mediumCategoriesMultiSelectDropdown').select2({
          closeOnSelect: false,
          scrollAfterSelect: true,
          allowClear: true,
          minimumResultsForSearch: 10,
          ajax: {
            url: 'api/medium/' + medium.model.id + '/category/selectList/',
            // url: 'api/category/selectList/',
            type: 'GET',
            dataType: 'json',
            delay: 250,
            headers: {
              "Authorization": "Bearer " + TIMAAT.Service.token,
              "Content-Type": "application/json",
            },
            // additional parameters
            data: function (params) {
              return {
                search: params.term,
                page: params.page
              };
            },
            processResults: function (data, params) {
              params.page = params.page || 1;
              return {
                results: data
              };
            },
            cache: false
          },
          minimumInputLength: 0,
        });
        TIMAAT.MediumService.getSelectedCategories(medium.model.id).then(function (data) {
          // console.log("TCL: then: data", data);
          var categorySelect = $('#mediumCategoriesMultiSelectDropdown');
          if (data.length > 0) {
            data.sort((a, b) => (a.name > b.name) ? 1 : -1);
            // create the options and append to Select2
            var i = 0;
            for (; i < data.length; i++) {
              var option = new Option(data[i].name, data[i].id, true, true);
              categorySelect.append(option).trigger('change');
            }
            // manually trigger the 'select2:select' event
            categorySelect.trigger({
              type: 'select2:select',
              params: {
                data: data
              }
            });
          }
        });
        modal.modal('show');
      });

      // submit category modal button functionality
      $('#mediumDatasetsModalCategorySubmit').on('click', async function (event) {
        event.preventDefault();
        // console.log("TCL: submit category list");
        var modal = $('#mediumDatasetsMediumCategoriesModal');
        if (!$('#mediumCategoriesForm').valid())
          return false;
        var medium = modal.data('medium');
        // console.log("TCL: medium", medium);
        var formDataRaw = $('#mediumCategoriesForm').serializeArray();
        // console.log("TCL: formDataRaw", formDataRaw);
        var i = 0;
        var categoryIdList = [];
        var newCategoryList = [];
        for (; i < formDataRaw.length; i++) {
          if (isNaN(Number(formDataRaw[i].value))) {
            newCategoryList.push({id: 0, name: formDataRaw[i].value}); // new categories that have to be added to the system first
          } else {
            categoryIdList.push({id: formDataRaw[i].value});
          }
        }
        medium.model = await TIMAAT.MediumDatasets.updateMediumHasCategoriesList(medium.model, categoryIdList);
        if (newCategoryList.length > 0) {
          var updatedMediumModel = await TIMAAT.MediumDatasets.addCategoriesToMedium(medium.model, newCategoryList);
          // console.log("TCL: updatedMediumModel", updatedMediumModel);
          medium.model.categories = updatedMediumModel.categories;
        }
        $('#mediumFormMetadata').data('medium', medium);
        modal.modal('hide');
      });

      // tag button handler
      $('#mediumDataSheetFormTagButton').on('click', async function (event) {
        event.stopPropagation();
        TIMAAT.UI.hidePopups();
        var modal = $('#mediumDatasetsMediumTagsModal');
        modal.data('medium', $('#mediumFormMetadata').data('medium'));
        var medium = modal.data('medium');
        modal.find('.modal-body').html(`
					<form role="form" id="mediumTagsForm">
						<div class="form-group">
							<label for="mediumTagsMultiSelectDropdown">Medium tags</label>
							<div class="col-md-12">
								<select class="form-control form-control-md multi-select-dropdown"
												style="width:100%;"
												id="mediumTagsMultiSelectDropdown"
												name="tagId"
												data-role="tagId"
												data-placeholder="Select medium tags"
												multiple="multiple">
								</select>
							</div>
						</div>
					</form>`);
        $('#mediumTagsMultiSelectDropdown').select2({
          closeOnSelect: false,
          scrollAfterSelect: true,
          allowClear: true,
          tags: true,
          tokenSeparators: [',', ' '],
          ajax: {
            url: 'api/tag/selectList/',
            type: 'GET',
            dataType: 'json',
            delay: 250,
            headers: {
              "Authorization": "Bearer " + TIMAAT.Service.token,
              "Content-Type": "application/json",
            },
            // additional parameters
            data: function (params) {
              return {
                search: params.term,
                page: params.page
              };
            },
            processResults: function (data, params) {
              params.page = params.page || 1;
              return {
                results: data
              };
            },
            cache: false
          },
          minimumInputLength: 0,
        });
        await TIMAAT.MediumService.getTagList(medium.model.id).then(function (data) {
          // console.log("TCL: then: data", data);
          var tagSelect = $('#mediumTagsMultiSelectDropdown');
          if (data.length > 0) {
            data.sort((a, b) => (a.name > b.name) ? 1 : -1);
            // create the options and append to Select2
            var i = 0;
            for (; i < data.length; i++) {
              var option = new Option(data[i].name, data[i].id, true, true);
              tagSelect.append(option).trigger('change');
            }
            // manually trigger the 'select2:select' event
            tagSelect.trigger({
              type: 'select2:select',
              params: {
                data: data
              }
            });
          }
        });
        modal.modal('show');
      });

      // submit tag modal button functionality
      $('#mediumDatasetsModalTagSubmit').on('click', async function (event) {
        event.preventDefault();
        var modal = $('#mediumDatasetsMediumTagsModal');
        if (!$('#mediumTagsForm').valid())
          return false;
        var medium = modal.data('medium');
        var formDataRaw = $('#mediumTagsForm').serializeArray();
        var i = 0;
        var tagIdList = [];
        var newTagList = [];
        for (; i < formDataRaw.length; i++) {
          if (isNaN(Number(formDataRaw[i].value))) {
            newTagList.push({id: 0, name: formDataRaw[i].value}); // new tags that have to be added to the system first
          } else {
            tagIdList.push({id: formDataRaw[i].value});
          }
        }
        medium.model = await TIMAAT.MediumDatasets.updateMediumHasTagsList(medium.model, tagIdList);
        if (newTagList.length > 0) {
          var updatedMediumModel = await TIMAAT.MediumDatasets.createNewTagsAndAddToMedium(medium.model, newTagList);
          // console.log("TCL: updatedMediumModel", updatedMediumModel);
          medium.model.tags = updatedMediumModel.tags;
        }
        $('#mediumFormMetadata').data('medium', medium);
        modal.modal('hide');
      });

      $('#mediumDatasetsMetadataTypeId').on('change', function (event) {
        event.stopPropagation();
        let type = $('#mediumDatasetsMetadataTypeId').find('option:selected').html();
        TIMAAT.MediumDatasets.initFormDataSheetData(type);
      });

      // upload button handler
      $('.uploadMediumButton').on('click', async function (event) {
        event.stopPropagation();
        TIMAAT.UI.hidePopups();
        let medium = $('#mediumFormMetadata').data('medium');
        let type = medium.model.mediaType.mediaTypeTranslations[0].type;
        medium = new TIMAAT.Medium(medium.model, type);
        medium.listView.find('.mediumUploadFile').click();
      });

      // file upload success event handler
      $(document).on('success.upload.medium.TIMAAT', async function (event, uploadedMedium) {
        // console.log("TCL: Medium -> constructor -> event, uploadedMedium", event, uploadedMedium);
        if (!uploadedMedium) return;
        var medium = $('#mediumFormMetadata').data('medium');
        if (medium == undefined || medium.model.id != uploadedMedium.id) return;
        var type = medium.model.mediaType.mediaTypeTranslations[0].type;
        medium.model.fileStatus = await TIMAAT.MediumService.updateFileStatus(medium.model, type);
        medium.model.viewToken = await TIMAAT.MediumService.updateViewToken(medium.model);
        // console.log("TCL: (document).one('success.upload.medium.TIMAAT') for id: ", uploadedMedium.id);
        // console.log("TCL: uploadedMedium.fileStatus", uploadedMedium.fileStatus);
        if (event.type == 'success') {
          switch (type) {
            case 'audio':
              medium.model.mediumAudio.length = uploadedMedium.mediumAudio.length;
              if ($('#mediumTab').hasClass('active')) {
                await TIMAAT.UI.refreshDataTable('medium');
              } else {
                await TIMAAT.UI.refreshDataTable('audio');
              }
              break;
            case 'image':
              medium.model.mediumImage.width = uploadedMedium.mediumImage.width;
              medium.model.mediumImage.height = uploadedMedium.mediumImage.height;
              // medium.model.mediumImage.bitDepth = uploadedMedium.mediumImage.bitDepth; // TODO
              if ($('#mediumTab').hasClass('active')) {
                await TIMAAT.UI.refreshDataTable('medium');
              } else {
                await TIMAAT.UI.refreshDataTable('image');
              }
              break;
            case 'video':
              medium.model.mediumVideo.width = uploadedMedium.mediumVideo.width;
              medium.model.mediumVideo.height = uploadedMedium.mediumVideo.height;
              medium.model.mediumVideo.length = uploadedMedium.mediumVideo.length;
              medium.model.mediumVideo.frameRate = uploadedMedium.mediumVideo.frameRate;
              if ($('#mediumTab').hasClass('active')) {
                await TIMAAT.UI.refreshDataTable('medium');
              } else {
                await TIMAAT.UI.refreshDataTable('video');
              }
              break;
          }
        }
        $('.uploadMediumButton').hide();
        $('.uploadMediumButton').prop('disabled', true);
        if (TIMAAT.UI.subNavTab == 'preview') {
          TIMAAT.UI.displayDataSetContent('preview', medium, 'medium');
        } else {
          TIMAAT.UI.displayDataSetContent('dataSheet', medium, 'medium');
        }
      });

      // annotate button handler
      $('.mediumDataSheetFormAnnotateButton').on('click', async function (event) {
        event.stopPropagation();
        TIMAAT.UI.hidePopups();
        var medium = $('#mediumFormMetadata').data('medium');
        TIMAAT.UI.showComponent('videoPlayer');
        // setup medium in player
        TIMAAT.VideoPlayer.setupMedium(medium.model);
        // load medium annotations from server
        let analysisLists = await TIMAAT.AnalysisListService.getMediumAnalysisLists(medium.model.id);
        await TIMAAT.VideoPlayer.setupMediumAnalysisLists(analysisLists);
        TIMAAT.VideoPlayer.loadAnalysisList(0);
      });

      // data table events
      $('.mediumDatasetTable').on('page.dt', function () {
        $('.dataTables_scrollBody').scrollTop(0);
      });

      // Key press events
      $('#mediumFormMetadataSubmitButton').on('keypress', function (event) {
        event.stopPropagation();
        if (event.which == '13') { // == enter
          $('#mediumFormMetadataSubmitButton').trigger('click');
        }
      });

      $('#mediumFormMetadataDismissButton').on('keypress', function (event) {
        event.stopPropagation();
        if (event.which == '13') { // == enter
          $('#mediumFormMetadataDismissButton').trigger('click');
        }
      });

    },

    initMusic: function () {
      $('#mediumMusicTab').on('click', () => {
        let medium = $('#mediumFormMetadata').data('medium');
        let type = medium.model.mediaType.mediaTypeTranslations[0].type;
        let name = medium.model.displayTitle.name;
        let id = medium.model.id;

        TIMAAT.UI.displayDataSetContentArea('mediumFormMusic')
        TIMAAT.UI.displayDataSetContent('music', medium, 'medium');

        if (type == 'medium') {
          TIMAAT.URLHistory.setURL(null, name + ' · Music · ' + type[0].toUpperCase() + type.slice(1), '#medium/' + id + '/music');
        } else {
          TIMAAT.URLHistory.setURL(null, name + ' · Music · ' + type[0].toUpperCase() + type.slice(1), '#medium/' + type + '/' + id + '/music');
        }
      })

      // add medium has music list button click
      // $(document).on('click','[data-role="musicNewMediumHasMusicFields"] > .form-group [data-role="add"]', async function(event) {
      $(document).on('click', '.addMusicToMediumButton', async function (event) {
        // console.log("TCL: add new medium with music(s)");
        event.preventDefault();
        const listEntry = $(this).closest('[data-role="mediumFormMusicNewMusicFields"]');
        let newFormEntry = [];
        let newEntryId = null;
        if (listEntry.find('select').each(function () {
          newEntryId = Number($(this).val());
        })) ;
        if (listEntry.find('input').each(function () {
          newFormEntry.push($(this).val());
        })) ;

        if (!$('#mediumFormMusic').valid()) {
          return false;
        }

        const medium = $('#mediumFormMetadata').data('medium');

        const existingEntryIds = new Set();
        $('[data-role="musicIsInMediumEntry"]').each((index, currentMusicEntry) => {
          const currentId = $(currentMusicEntry).data('music-id');
          existingEntryIds.add(currentId);
        })

        if (!existingEntryIds.has(newEntryId)) {
          $('.disableOnSubmit').prop('disabled', false);
          let newEntryDetails = [];
          let j = 0;

          for (; j < newFormEntry.length - 2; j += 2) {
            newEntryDetails.push({
              type: 'direct',
              mediumHasMusicMusicId: newEntryId,
              mediumHasMusicMediumId: medium.model.id,
              id: 0,
              startTime: newFormEntry[j],
              endTime: newFormEntry[j + 1]
            });
          }

          let musicName = await TIMAAT.MusicService.getDisplayTitle(newEntryId);
          let appendNewFormDataEntry = TIMAAT.MediumDatasets.appendMediumHasMusicDataset(newEntryId, musicName.name, newEntryDetails, true);
          $('#mediumFormMusicDynamicFields').append(appendNewFormDataEntry);
          $('.musicDatasetMusicMediumHasMusicMediumId').prop('disabled', true);

          if (listEntry.find('input').each(function () {
            $(this).val('');
          })) ;
          if (listEntry.find('select').each(function () {
            $(this).val(null).trigger('change');
          })) ;

          $('#mediumFormMusicNewMusicFields').empty()
          $('#mediumFormMusicNewMusicFields').append(TIMAAT.MediumDatasets.appendNewMediumHasMusicFields());

          $('#musicSelectDropdown').select2({
            closeOnSelect: true,
            scrollAfterSelect: true,
            allowClear: true,
            ajax: {
              url: 'api/music/selectList/',
              type: 'GET',
              dataType: 'json',
              delay: 250,
              headers: {
                "Authorization": "Bearer " + TIMAAT.Service.token,
                "Content-Type": "application/json",
              },
              // additional parameters
              data: function (params) {
                // console.log("TCL: data: params", params);
                return {
                  search: params.term,
                };
              },
              processResults: function (data, params) {
                // console.log("TCL: processResults: data", data);
                params.page = params.page || 1;
                return {
                  results: data
                };
              },
              cache: false
            },
            minimumInputLength: 0,
          });
        } else { // duplicate music
          $('#mediumDatasetsMediumHasMusicDuplicateModal').modal('show');
        }
      });

      // add medium has music detail button click
      // $(document).on('click', '.form-group +[data-role="addMediumHasMusicDetail"]', async function(event) {
      $(document).on('click', '.mediumDatasetAddMediumHasMusicDetailButton', async function (event) {
        event.preventDefault();
        const listEntry = $(this).closest('[data-role="newMusicIsInMediumDetailFields"]');
        let newMusicInMediumData = [];
        const musicId = $(this).closest(('[data-role="musicIsInMediumEntry"]')).data('music-id');
        if (listEntry.find('input').each(function () {
          newMusicInMediumData.push($(this).val());
        })) ;

        const newMediumHasMusicDetailEntry = {
          startTime: newMusicInMediumData[0],
          endTime: newMusicInMediumData[1],
          type: "direct"
        };

        $(this).closest('[data-role="newMusicIsInMediumDetailFields"]').before(TIMAAT.MediumDatasets.appendMediumHasMusicDetailFields(0, musicId, newMediumHasMusicDetailEntry));

        if (listEntry.find('input').each(function () {
          $(this).val('');
        })) ;
        if (listEntry.find('select').each(function () {
          $(this).val('');
        })) ;
      });

      // remove medium has music list button click
      // $(document).on('click','[data-role="musicDynamicMediumHasMusicFields"] > .form-group [data-role="remove"]', async function(event) {
      $(document).on('click', '.removeMusicFromMediumButton', async function (event) {
        event.preventDefault();
        $(this).closest('.form-group').remove();
      });

      // remove medium has music detail button click
      $(document).on('click', '.form-group [data-role="mediumDatasetRemoveMediumHasMusicDetail"]', async function (event) {
        event.preventDefault();
        $(this).closest('.form-group').remove();
      });

      // submit medium has music list button functionality
      $('#mediumFormMusicSubmitButton').on('click', async function (event) {
        // console.log("TCL: MediumHasMusic form: submit");
        // add rules to dynamically added form fields
        const form = $('#mediumFormMusic')

        event.preventDefault();
        const newMusicNode = $('#mediumFormMusicNewMusicFields');
        newMusicNode.empty()

        if (!form.valid()) {
          return false
        }


        let medium = $('#mediumFormMetadata').data('medium')
        const mediumHasMusicList = []
        $('#mediumFormMusicDynamicFields [data-role="musicIsInMediumEntry"]').each((index, element) => {
          const $element = $(element)
          const musicId = $element.data('music-id')
          const timeRanges = []

          $element.find('[data-role="mediumHasMusicDetailEntry"][data-type="direct"]').each((j, timeRangesDetailEntry) => {
            const $timeRangesDetailEntry = $(timeRangesDetailEntry);
            const currentStartTimeText = $timeRangesDetailEntry.find('.mediumDatasetsMusicMediumHasMusicListStartTime').val()
            const currentEndTimeText = $timeRangesDetailEntry.find('.mediumDatasetsMusicMediumHasMusicListEndTime').val()

            const parsedStartTime = TIMAAT.Util.parseTime(currentStartTimeText)
            const parsedEndTime = TIMAAT.Util.parseTime(currentEndTimeText)

            timeRanges.push({
              startTime: parsedStartTime,
              endTime: parsedEndTime
            })
          })
          mediumHasMusicList.push({
            musicId,
            timeRanges
          })

        })

        const updatedMediumHasMusicList = await TIMAAT.MediumService.updateMediumHasMusicList(medium.model.id, mediumHasMusicList);
        updatedMediumHasMusicList.sort((a, b) => a.id.musicId - b.id.musicId);

        medium.model.mediumHasMusicList = updatedMediumHasMusicList;
        TIMAAT.UI.displayDataSetContent("music", medium, "medium")

        const type = medium.model.mediaType.mediaTypeTranslations[0].type;
        if ($('#mediumTab').hasClass('active')) {
          await TIMAAT.UI.refreshDataTable('medium');
        } else {
          await TIMAAT.UI.refreshDataTable(type);
        }

        await TIMAAT.UI.refreshDataTable('music')
      });

      // cancel add/edit button in titles form functionality
      $('#mediumFormMusicDismissButton').on('click', function (event) {
        let medium = $('#mediumFormMetadata').data('medium');
        TIMAAT.UI.displayDataSetContent('music', medium, 'medium');
      });
    },

    initTranscriptions: function () {
      TIMAAT.EntityUpdate.registerEntityUpdateListener("transcription", TIMAAT.MediumDatasets.handleTranscriptionEntityUpdateMessage)

      $('#mediumTranscriptionTab').on('click', () => {
        let medium = $('#mediumFormMetadata').data('medium');
        let type = medium.model.mediaType.mediaTypeTranslations[0].type;
        let name = medium.model.displayTitle.name;
        let id = medium.model.id;

        TIMAAT.UI.displayDataSetContentArea('mediumFormTranscriptions');
        TIMAAT.UI.displayDataSetContent('transcriptions', medium, 'medium');

        if (type == 'medium') {
          TIMAAT.URLHistory.setURL(null, name + ' · Transcriptions · ' + type[0].toUpperCase() + type.slice(1), '#medium/' + id + '/transcriptions');
        } else {
          TIMAAT.URLHistory.setURL(null, name + ' · Transcriptions · ' + type[0].toUpperCase() + type.slice(1), '#medium/' + type + '/' + id + '/transcriptions');
        }
      });

      $('#mediumFormTranscriptionsCreateButton').on('click', () => {
        const $mediumTranscriptionCreateSubmitButtonLoadingIndicator = $('#mediumTranscriptionCreateSubmitButtonLoadingIndicator')
        const $mediumTranscriptionCreateModal = $('#mediumTranscriptionCreateModal');
        const $mediumTranscriptionEngineSelect = $('#mediumTranscriptionEngineSelect')
        const $mediumTranscriptionModelSelect = $('#mediumTranscriptionModelSelect')
        const $mediumTranscriptionCreateModalModalBody = $mediumTranscriptionCreateModal.find('.modal-body')
        $mediumTranscriptionCreateModalModalBody.find('.alert').remove()

        $mediumTranscriptionEngineSelect.off()
        $mediumTranscriptionModelSelect.off()
        $mediumTranscriptionCreateModal.find(".modal-body, .modal-footer").find("input, button, select").prop('disabled', true)
        $mediumTranscriptionCreateSubmitButtonLoadingIndicator.addClass('d-none')

        const showAlert = (alertMessage) => {
          $mediumTranscriptionCreateModalModalBody.prepend(`<div class="alert alert-danger" role="alert">${alertMessage}</div>`)
        }

        TIMAAT.TranscriptionService.listAvailableEngines().then(engines => {
          const $mediumFormMetadata = $('#mediumFormMetadata').data('medium')
          const mediumId = $mediumFormMetadata.model.id

          if (engines.length > 0) {
            const engineSelectionData = []
            const modelSelectionDataByEngine = new Map()

            let initialSelectedEngineIdentifier = undefined
            let initialSelectedModelIdentifier = undefined

            for (const currentEngine of engines.sort((a, b) => a.engineName.localeCompare(b.engineName))) {
              engineSelectionData.push({
                id: currentEngine.engineIdentifier,
                text: currentEngine.engineName
              })

              const modelSelectionData = []
              for (const currentModel of currentEngine.models.sort((a, b) => a.modelIdentifier.localeCompare(b.modelIdentifier))) {
                modelSelectionData.push({
                  id: currentModel.modelIdentifier,
                  text: currentModel.modelIdentifier
                })

                if (currentModel.isDefault) {
                  initialSelectedEngineIdentifier = currentEngine.engineIdentifier
                  initialSelectedModelIdentifier = currentModel.modelIdentifier
                }
              }
              modelSelectionDataByEngine.set(currentEngine.engineIdentifier, modelSelectionData)
            }

            initialSelectedEngineIdentifier = initialSelectedEngineIdentifier ?? engines[0].engineIdentifier

            $mediumTranscriptionEngineSelect.select2({
              closeOnSelect: true,
              width: '100%',
              data: engineSelectionData,
              minimumInputLength: 0,
              minimumResultsForSearch: Infinity
            });
            $mediumTranscriptionModelSelect.select2({
              closeOnSelect: true,
              width: '100%',
              data: modelSelectionDataByEngine.get(initialSelectedEngineIdentifier),
              minimumInputLength: 0,
              minimumResultsForSearch: Infinity
            });

            $mediumTranscriptionEngineSelect.val(initialSelectedEngineIdentifier).trigger('change');
            $mediumTranscriptionModelSelect.val(initialSelectedModelIdentifier).trigger('change');

            $mediumTranscriptionEngineSelect.on('select2:select', e => {
              $mediumTranscriptionModelSelect.select2('destroy');
              $mediumTranscriptionModelSelect.empty();
              $mediumTranscriptionModelSelect.select2({
                closeOnSelect: true,
                width: '100%',
                data: modelSelectionDataByEngine.get(e.params.data.id),
                minimumInputLength: 0,
                minimumResultsForSearch: Infinity
              })
            })

            $("#mediumTranscriptionCreateSubmitButton").off('click').on('click', e => {
              // continue only if client side validation has passed
              e.preventDefault();
              if (!$('#mediumTranscriptionCreateForm').valid()) return false;
              $mediumTranscriptionCreateModal.find(".modal-body, .modal-footer").find("input, button, select").prop('disabled', true)
              $mediumTranscriptionCreateSubmitButtonLoadingIndicator.removeClass('d-none')

              const mediumTranscriptionEngineId = $mediumTranscriptionEngineSelect.val()
              const mediumTranscriptionModelId = $mediumTranscriptionModelSelect.val()

              if (mediumTranscriptionEngineId && mediumTranscriptionModelId) {
                TIMAAT.MediumService.createMediumTranscription(mediumId, mediumTranscriptionEngineId, mediumTranscriptionModelId).then(() => {
                  $('#mediumTranscriptionCreateModal').modal('hide');
                }).catch(err => {
                  console.log("Error during creating transcriptions")
                  showAlert("Error during creating transcription")
                  $mediumTranscriptionCreateModal.find(".modal-body, .modal-footer").find("input, button, select").prop('disabled', false)
                  $mediumTranscriptionCreateSubmitButtonLoadingIndicator.addClass('d-none')
                })
              }
            })
            $mediumTranscriptionCreateModal.find(".modal-body, .modal-footer").find("input, button, select").prop('disabled', false)
          } else {
            showAlert("No engine available to execute transcriptions.")
          }
        }).catch(err => {
          console.error("Error during loading engine information", err)
          showAlert("Error during loading engine information.")
        })
        $mediumTranscriptionCreateModal.modal('show');
      });

      const $mediumTranscriptionViewer = $('#mediumTranscriptionViewer')
      const $mediumDatasetsTranscriptionDeleteModal = $("#mediumDatasetsTranscriptionDeleteModal")

      $("#transcriptionViewerDeleteBtn").on('click', e => {
        e.preventDefault()
        const transcription = $mediumTranscriptionViewer.data("transcription")
        let medium = $('#mediumFormMetadata').data('medium');

        if (transcription) {
          $mediumDatasetsTranscriptionDeleteModal.data("transcriptionId", transcription.id)
          $mediumDatasetsTranscriptionDeleteModal.data("mediumId", medium.model.id)
          $mediumDatasetsTranscriptionDeleteModal.modal('show')
        }
      })

      $("#mediumDatasetsTranscriptionDeleteBtn").on('click', e => {
        const transcriptionId = $mediumDatasetsTranscriptionDeleteModal.data("transcriptionId")
        const mediumId = $mediumDatasetsTranscriptionDeleteModal.data("mediumId")

        if (transcriptionId) {
          TIMAAT.MediumService.deleteMediumTranscription(mediumId, transcriptionId)
        }
        $mediumDatasetsTranscriptionDeleteModal.modal('hide')
      })

      $("#transcriptionViewerSetDefaultBtn").on('click', e => {
        e.preventDefault()
        const transcription = $mediumTranscriptionViewer.data("transcription")
        let medium = $('#mediumFormMetadata').data('medium');

        if (transcription) {
          TIMAAT.MediumService.updateMediumDefaultTranscription(medium.model.id, transcription.id)
        }
      })

      $("#transcriptionViewerEditBtn").on('click', e => {
        e.preventDefault()
        const transcription = $mediumTranscriptionViewer.data("transcription")
        if (transcription && transcription.state === "COMPLETED") {
          TIMAAT.MediumDatasets.enterTranscriptionEditMode(transcription)
        }
      })

      $mediumTranscriptionViewer.find('[data-role="transcriptionEditCancelBtn"]').on('click', e => {
        e.preventDefault()
        TIMAAT.MediumDatasets.cancelTranscriptionEditMode()
      })

      $mediumTranscriptionViewer.find('[data-role="transcriptionEditSaveBtn"]').on('click', e => {
        e.preventDefault()
        TIMAAT.MediumDatasets.saveTranscriptionEditMode()
      })

      $mediumTranscriptionViewer.find('[data-role="transcriptionEditNameInput"]').on('input', () => {
        TIMAAT.MediumDatasets.recomputeTranscriptionEditState()
      })

      $('#mediumTranscriptionList').on('click', '.medium-transcription-list-group-item', function (e) {
        const transcription = $(e.currentTarget).data("transcription")
        const $mediumTranscriptionListGroupItems = $('.medium-transcription-list-group-item')

        $mediumTranscriptionListGroupItems.removeClass("active")
        $mediumTranscriptionListGroupItems.filter(`[data-id="${transcription.id}"]`).addClass("active")

        TIMAAT.MediumDatasets.openTranscription(transcription)
      })
    },

    handleTranscriptionEntityUpdateMessage: function (transcriptionUpdateMessage) {
      const $mediumTranscriptionTab = $("#mediumTranscriptionTab")
      const $mediumPreviewTab = $("#mediumPreviewTab")

      if ($mediumTranscriptionTab.hasClass("active")) {
        const $mediumTranscriptionList = $("#mediumTranscriptionList");
        const medium = $('#mediumFormMetadata').data('medium')

        //We only need to consume transcription messages when the tab is currently visible
        if (transcriptionUpdateMessage.type === "CREATE") {
          const mediumId = medium.model.id

          if (mediumId === transcriptionUpdateMessage.entity.mediumId) {
            const isDefault = medium.model.defaultTranscriptionId === transcriptionUpdateMessage.id
            const isActive = $mediumTranscriptionList.find(".medium-transcription-list-group-item").length === 0

            const $createdTranscriptionListItem = TIMAAT.MediumDatasets.createTranscriptionListGroupItem(transcriptionUpdateMessage.entity, isActive, isDefault)
            $mediumTranscriptionList.append($createdTranscriptionListItem)

            if (isActive) {
              if (!editModeActive) {
                TIMAAT.MediumDatasets.openTranscription(transcriptionUpdateMessage.entity)
              } else {

              }
            }
          }
        } else if (transcriptionUpdateMessage.type === "CHANGE") {
          const $matchingTranscriptionListGroupItem = $(`.medium-transcription-list-group-item[data-id="${transcriptionUpdateMessage.id}"`)
          if ($matchingTranscriptionListGroupItem.length > 0) {
            const isActive = $matchingTranscriptionListGroupItem.hasClass("active")
            const transcription = $matchingTranscriptionListGroupItem.data("transcription")
            const editModeActive = TIMAAT.MediumDatasets._transcriptionEdit !== null

            if (transcriptionUpdateMessage.entity.contentChanged) {
              if (isActive) {
                if (!editModeActive) {
                  TIMAAT.MediumDatasets.openTranscription(transcription)
                } else {
                  TIMAAT.MediumService.getTranscriptionContent(transcription.mediumId, transcription.id).then(transcriptionContent => {
                    // Verify current transcription still active
                    if (TIMAAT.MediumDatasets._transcriptionEdit.transcription?.id === transcription.id) {
                      let idCounter = 0;

                      const cues = (transcriptionContent && transcriptionContent.cues ? transcriptionContent.cues : []).map(cue => ({
                        id: idCounter++,
                        start: cue.startTime,
                        end: cue.endTime,
                        text: cue.cue
                      }))

                      TIMAAT.MediumDatasets._transcriptionEdit.initialCue = cues
                      TIMAAT.MediumDatasets._transcriptionEdit.snapshot = TIMAAT.MediumDatasets._transcriptionEditSnapshot(TIMAAT.MediumDatasets._transcriptionEdit.transcription.name, cues)
                      TIMAAT.MediumDatasets.recomputeTranscriptionEditState()
                    }
                  })
                }
              }
            } else {
              const updatedTranscription = {...transcription, ...transcriptionUpdateMessage.entity}
              const isDefault = updatedTranscription.id === medium.model.defaultTranscriptionId

              const $updatedTranscriptionListGroupItem = TIMAAT.MediumDatasets.createTranscriptionListGroupItem(updatedTranscription, isActive, isDefault)
              $matchingTranscriptionListGroupItem.replaceWith($updatedTranscriptionListGroupItem)

              if (isActive) {
                if (!editModeActive) {
                  TIMAAT.MediumDatasets.openTranscription(updatedTranscription)
                } else {
                  TIMAAT.MediumDatasets._transcriptionEdit.transcription = updatedTranscription
                  TIMAAT.MediumDatasets._transcriptionEdit.snapshot = TIMAAT.MediumDatasets._transcriptionEditSnapshot(updatedTranscription.name, TIMAAT.MediumDatasets._transcriptionEdit.initialCues)
                  TIMAAT.MediumDatasets.recomputeTranscriptionEditState()
                }
              }
            }
          }
        } else {
          const $matchingTranscriptionListGroupItem = $(`.medium-transcription-list-group-item[data-id="${transcriptionUpdateMessage.id}"`)
          if ($matchingTranscriptionListGroupItem.length > 0) {
            const isActive = $matchingTranscriptionListGroupItem.hasClass("active")
            $matchingTranscriptionListGroupItem.remove()
            if (isActive) {
              TIMAAT.MediumDatasets.openTranscription(undefined)
            }
          }
        }
      } else if ($mediumPreviewTab.hasClass("active")) {
        const mediumFormMetadataMedium = $('#mediumFormMetadata').data('medium')

        if (transcriptionUpdateMessage.type === "CHANGE" && mediumFormMetadataMedium.model.defaultTranscriptionId === transcriptionUpdateMessage.id && (transcriptionUpdateMessage.entity.state === "COMPLETED" || transcriptionUpdateMessage.entity.contentChanged)) {
          TIMAAT.MediumDatasets.updateFormPreviewSubtitle(mediumFormMetadataMedium)
        }
      }
    },

    updateDefaultTranscriptionBadgePositionToCurrentMediumDefaultTranscription() {
      const medium = $('#mediumFormMetadata').data('medium')
      const defaultTranscriptionId = medium.model.defaultTranscriptionId
      $(".transcription-default-badge").remove()

      const $badges = $(`.medium-transcription-list-group-item[data-id="${defaultTranscriptionId}"] .transcription-list-group-item-badges`)
      if ($badges.length) {
        $badges.append('<span class="badge badge-secondary transcription-default-badge">default</span>')
      }
    },

    createTranscriptionListGroupItem: function (transcription, active, defaultTranscription) {
      const createdAt = TIMAAT.Util.formatDate(transcription.createdAt)

      const $listGroupItem = $(`<li class="list-group-item medium-transcription-list-group-item d-flex justify-content-between align-items-center"></li>`)
      $listGroupItem.data("transcription", transcription)
      $listGroupItem.attr("data-id", transcription.id)


      const $currentListGroupItemTranscriptionInfo = $(`<div class="d-flex flex-column flex-grow-1">${transcription.name}<small>${createdAt}</small></div>`)
      let $currentListGroupItemTranscriptionState = $('<div class="transcription-state-container"></div>')
      if (transcription.state === "COMPLETED") {
        $currentListGroupItemTranscriptionState.append(`<div class="transcription-state-pill completed" title="Transcription creation completed"></div>`)
      } else if (transcription.state === "FAILED") {
        $currentListGroupItemTranscriptionState.append(`<div class="transcription-state-pill failed" title="Transcription creation failed"></div>`)
      } else {
        $currentListGroupItemTranscriptionState.append(`<div class="spinner-border spinner-border-sm text-secondary"></div>`)
      }

      $listGroupItem.append($currentListGroupItemTranscriptionState)
      $listGroupItem.append($currentListGroupItemTranscriptionInfo)


      if (active) {
        $listGroupItem.addClass("active")
      }

      const $currentListGroupItemBadges = $('<div class="d-flex flex-column transcription-list-group-item-badges"></div>')
      if (defaultTranscription) {
        $currentListGroupItemBadges.append('<span class="badge badge-secondary transcription-default-badge">default</span>')
      }
      $listGroupItem.append($currentListGroupItemBadges)

      return $listGroupItem
    },

    mediumFormTranscriptions: function (mode, medium) {
      let defaultTranscriptionId = medium.model.defaultTranscriptionId
      const $mediumFormTranscriptionsLoadingRow = $('#mediumFormTranscriptionsLoadingRow')
      const $mediumFormTranscriptionsContentRow = $('#mediumFormTranscriptionsContentRow')
      const $mediumTranscriptionList = $mediumFormTranscriptionsContentRow.find("#mediumTranscriptionList");

      $mediumFormTranscriptionsLoadingRow.show()
      $mediumFormTranscriptionsContentRow.hide()

      TIMAAT.MediumService.getMediumTranscriptions(medium.model.id).then(transcriptions => {
        $mediumTranscriptionList.empty()
        let activeTranscription
        for (const transcription of transcriptions) {
          let isActive = false
          let isDefault = false

          if (transcription.id === defaultTranscriptionId) {
            activeTranscription = transcription
            isActive = true
            isDefault = true
          } else if (defaultTranscriptionId === null && activeTranscription === null) {
            activeTranscription = transcription
            isActive = true
          }

          const $currentListGroupItem = TIMAAT.MediumDatasets.createTranscriptionListGroupItem(transcription, isActive, isDefault)
          $mediumTranscriptionList.append($currentListGroupItem)
        }

        TIMAAT.MediumDatasets.openTranscription(activeTranscription)

        $mediumFormTranscriptionsLoadingRow.hide()
        $mediumFormTranscriptionsContentRow.show()
      })

    },
    /**
     * Opens the specified transcription inside the transcription viewer
     * @param transcription which will be openend. May be null when deselect opened transcription
     */
    openTranscription: function (transcription) {
      const $mediumTranscriptionViewer = $("#mediumTranscriptionViewer")
      $mediumTranscriptionViewer.data("transcription", transcription)

      const $mediumTranscriptionViewerCardBody = $mediumTranscriptionViewer.find(".card-body")
      const $mediumTranscriptionViewerCardHeader = $mediumTranscriptionViewer.find(".card-header")
      const $mediumTranscriptionDownloadVttButton = $mediumTranscriptionViewerCardHeader.find("#transcriptionViewerDownloadVttBtn")
      const $transcriptionViewerEditBtn = $mediumTranscriptionViewerCardHeader.find("#transcriptionViewerEditBtn")

      $mediumTranscriptionDownloadVttButton.hide()
      $transcriptionViewerEditBtn.prop("disabled", true)

      TIMAAT.MediumDatasets._cleanupTranscriptionViewer()

      $mediumTranscriptionViewerCardBody.children().addClass("d-none")
      $mediumTranscriptionViewerCardHeader.children().addClass("d-none")

      if (transcription) {
        $mediumTranscriptionViewerCardBody.removeClass("d-none")
        $mediumTranscriptionViewerCardHeader.find('[data-role="viewerSelectedState"]').removeClass("d-none")

        $mediumTranscriptionViewerCardHeader.find('.transcriptionViewerNameField').text(transcription.name)
        $mediumTranscriptionViewerCardHeader.find('.transcriptionViewerCreatedAtField').text(TIMAAT.Util.formatDate(transcription.createdAt))

        if (transcription.state === "COMPLETED") {
          $transcriptionViewerEditBtn.prop("disabled", false)
          $mediumTranscriptionDownloadVttButton.off('click.transcriptionDownload').on('click.transcriptionDownload', async function (event) {
            event.preventDefault();
            const blob = await TIMAAT.MediumService.downloadTranscriptionFile(transcription.mediumId, transcription.id);
            if (!blob) return;
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = transcription.name + '.vtt';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
          });

          const $completedState = $mediumTranscriptionViewerCardBody.find('[data-role="viewerCompletedState"]')
          $completedState.removeClass("d-none")
          $mediumTranscriptionDownloadVttButton.show()

          const inAnnotationPerspective = TIMAAT.UI.component === 'videoPlayer'
          $completedState.find('[data-role="transcriptionViewerFollowRow"]').toggleClass('d-none', !inAnnotationPerspective)
          $completedState.find('[data-role="transcriptionViewerCueScroll"]').empty()

          TIMAAT.MediumService.getTranscriptionContent(transcription.mediumId, transcription.id).then(transcriptionContent => {
            if (!transcriptionContent || !transcriptionContent.cues) return
            TIMAAT.MediumDatasets._renderTranscriptionViewer($completedState, transcriptionContent.cues, inAnnotationPerspective)
          })
        } else if (transcription.state === "FAILED") {
          $mediumTranscriptionViewerCardBody.find('[data-role="viewerFailedState"]').removeClass("d-none")
        } else {
          $mediumTranscriptionViewerCardBody.find('[data-role="viewerInProgressState"]').removeClass("d-none")
        }
      } else {
        $mediumTranscriptionViewerCardBody.addClass("d-none")
        $mediumTranscriptionViewerCardHeader.find('[data-role="viewerEmptyState"]').removeClass("d-none")
      }
    },

    _cleanupTranscriptionViewer: function () {
      if (TIMAAT.MediumDatasets._transcriptionViewerCleanup) {
        TIMAAT.MediumDatasets._transcriptionViewerCleanup()
        TIMAAT.MediumDatasets._transcriptionViewerCleanup = null
      }
      TIMAAT.MediumDatasets._transcriptionEdit = null
    },

    _renderTranscriptionViewer: function ($completedState, cues, inAnnotationPerspective) {
      const $cueScroll = $completedState.find('[data-role="transcriptionViewerCueScroll"]')
      const $followToggle = $completedState.find('[data-role="transcriptionViewerFollowToggle"]')

      let followActive = $followToggle.hasClass('active')
      $cueScroll.empty()

      const cueElements = cues.map(function (cue) {
        const startFmt = TIMAAT.Util.formatTime(cue.startTime, true)
        const endFmt = TIMAAT.Util.formatTime(cue.endTime, true)
        const escapedText = $('<div>').text(cue.cue).html()
        const $cueEl = $('<div class="transcription-viewer-cue">' +
            '<div class="transcription-viewer-cue-range">' + startFmt +
            ' <span class="transcription-viewer-cue-arrow">→</span> ' + endFmt + '</div>' +
            '<div class="transcription-viewer-cue-text">' + escapedText + '</div>' +
            '</div>')
        $cueEl.on('click', function () {
          if (TIMAAT.VideoPlayer && TIMAAT.VideoPlayer.jumpTo) {
            TIMAAT.VideoPlayer.jumpTo(cue.startTime)
          }
        })
        $cueScroll.append($cueEl)
        return $cueEl
      })

      if (inAnnotationPerspective) {
        $followToggle.off('click.transcriptionViewer').on('click.transcriptionViewer', function () {
          followActive = !followActive
          $followToggle.toggleClass('active', followActive)
        })

        const media = TIMAAT.VideoPlayer && TIMAAT.VideoPlayer.medium

        const updateActiveCue = function () {
          if (!media) return
          const t = media.currentTime * 1000
          let activeCueEl = null
          for (let i = 0; i < cueElements.length; i++) {
            const isActive = t >= cues[i].startTime && t < cues[i].endTime
            cueElements[i].toggleClass('active', isActive)
            if (isActive) activeCueEl = cueElements[i]
          }
          if (followActive && activeCueEl) {
            const scroll = $cueScroll[0]
            const el = activeCueEl[0]
            const top = el.offsetTop - scroll.clientHeight / 2 + el.offsetHeight / 2
            scroll.scrollTo({top: top, behavior: 'smooth'})
          }
        }

        if (media) {
          media.addEventListener('timeupdate', updateActiveCue)
          updateActiveCue()
        }

        TIMAAT.MediumDatasets._transcriptionViewerCleanup = function () {
          if (media) {
            media.removeEventListener('timeupdate', updateActiveCue)
          }
          $followToggle.off('click.transcriptionViewer')
        }
      }
    },

    /**
     * Enters the edit mode of the transcription viewer for the given (COMPLETED) transcription.
     * The header switches to a name text box plus Save/Cancel and the body shows an editable cue
     * list. The cue content is loaded via the same service method used by the completed view.
     * @param transcription the transcription to edit (must be in state COMPLETED)
     */
    enterTranscriptionEditMode: function (transcription) {
      const $viewer = $("#mediumTranscriptionViewer")
      const $header = $viewer.find(".card-header")
      const $body = $viewer.find(".card-body")

      TIMAAT.MediumDatasets._cleanupTranscriptionViewer()

      $header.children().addClass("d-none")
      $body.children().addClass("d-none")
      $header.find('[data-role="viewerEditState"]').removeClass("d-none")
      $body.find('[data-role="viewerEditState"]').removeClass("d-none")

      $header.find('[data-role="transcriptionEditNameInput"]').val(transcription.name)
      $header.find('[data-role="transcriptionEditDirtyIndicator"]').addClass("d-none")
      $header.find('[data-role="transcriptionEditError"]').addClass("d-none").text("")

      const $editorScroll = $body.find('[data-role="transcriptionEditorScroll"]')
      $editorScroll.empty().append('<div class="text-center text-muted p-2"><div class="spinner-border spinner-border-sm" role="status"></div></div>')

      const state = {
        transcription: transcription,
        cues: [],
        nextId: 0,
        total: 1000,
        snapshot: null
      }
      TIMAAT.MediumDatasets._transcriptionEdit = state

      TIMAAT.MediumService.getTranscriptionContent(transcription.mediumId, transcription.id).then(content => {
        // Guard against a meanwhile-left edit mode (e.g. another transcription selected).
        if (TIMAAT.MediumDatasets._transcriptionEdit !== state) return

        const cues = (content && content.cues ? content.cues : []).map(cue => ({
          id: state.nextId++,
          start: cue.startTime,
          end: cue.endTime,
          text: cue.cue
        }))
        state.cues = cues
        state.initialCues = cues
        state.total = cues.length ? Math.max(1000, ...cues.map(c => c.end)) : 1000
        state.snapshot = TIMAAT.MediumDatasets._transcriptionEditSnapshot(transcription.name, cues)

        TIMAAT.MediumDatasets._renderTranscriptionEditor()
        TIMAAT.MediumDatasets.recomputeTranscriptionEditState()
      }).catch(() => {
        if (TIMAAT.MediumDatasets._transcriptionEdit !== state) return
        $editorScroll.empty().append('<div class="alert alert-danger m-2">Error during loading transcription content.</div>')
      })
    },

    /**
     * Leaves the edit mode without persisting and re-opens the unchanged transcription, restoring
     * the normal viewer state.
     */
    cancelTranscriptionEditMode: function () {
      const state = TIMAAT.MediumDatasets._transcriptionEdit
      TIMAAT.MediumDatasets._exitTranscriptionEditMode()
      if (state && state.transcription) {
        TIMAAT.MediumDatasets.openTranscription(state.transcription)
      }
    },

    /**
     * Persists the edited transcription. Writes the name (if changed) and always the content via
     * the dedicated REST endpoints, then leaves edit mode and re-opens the updated transcription.
     * Aborts when a cue is invalid (end <= start) or the name is blank.
     */
    saveTranscriptionEditMode: function () {
      const state = TIMAAT.MediumDatasets._transcriptionEdit
      if (!state) return

      const $viewer = $("#mediumTranscriptionViewer")
      const $header = $viewer.find(".card-header")
      const $nameInput = $header.find('[data-role="transcriptionEditNameInput"]')
      const $saveBtn = $header.find('[data-role="transcriptionEditSaveBtn"]')
      const $cancelBtn = $header.find('[data-role="transcriptionEditCancelBtn"]')
      const $error = $header.find('[data-role="transcriptionEditError"]')

      const name = $nameInput.val().trim()
      const hasError = state.cues.some(cue => cue.end <= cue.start)
      if (hasError || name.length === 0) return

      $saveBtn.prop("disabled", true)
      $cancelBtn.prop("disabled", true)
      $nameInput.prop("disabled", true)
      $error.addClass("d-none").text("")

      const transcription = state.transcription
      const cues = state.cues.slice().sort((a, b) => a.start - b.start).map(cue => ({
        startTime: cue.start,
        endTime: cue.end,
        cue: cue.text
      }))

      const requests = []
      if (name !== transcription.name) {
        requests.push(TIMAAT.MediumService.updateMediumTranscription(transcription.mediumId, transcription.id, name))
      }
      requests.push(TIMAAT.MediumService.updateMediumTranscriptionContent(transcription.mediumId, transcription.id, {cues: cues}))

      Promise.all(requests).then(() => {
        const updatedTranscription = {...transcription, name: name}
        TIMAAT.MediumDatasets._exitTranscriptionEditMode()
        TIMAAT.MediumDatasets.openTranscription(updatedTranscription)
      }).catch(() => {
        $cancelBtn.prop("disabled", false)
        $nameInput.prop("disabled", false)
        $error.removeClass("d-none").text("Error while saving the transcription.")
        TIMAAT.MediumDatasets.recomputeTranscriptionEditState()
      })
    },

    _exitTranscriptionEditMode: function () {
      const $viewer = $("#mediumTranscriptionViewer")
      const $header = $viewer.find(".card-header")
      const $body = $viewer.find(".card-body")

      $header.find('[data-role="viewerEditState"]').addClass("d-none")
      $body.find('[data-role="viewerEditState"]').addClass("d-none")
      $body.find('[data-role="transcriptionEditorScroll"]').empty()
      $header.find('[data-role="transcriptionEditNameInput"]').prop("disabled", false)
      $header.find('[data-role="transcriptionEditSaveBtn"]').prop("disabled", false)
      $header.find('[data-role="transcriptionEditCancelBtn"]').prop("disabled", false)

      TIMAAT.MediumDatasets._transcriptionEdit = null
    },

    _transcriptionEditSnapshot: function (name, cues) {
      return JSON.stringify({
        name: name,
        cues: cues.map(cue => ({start: cue.start, end: cue.end, text: cue.text}))
      })
    },

    /**
     * Recomputes the dirty/validity state of the edit mode and reflects it in the UI: shows the
     * "Unsaved" indicator when the current state differs from the loaded snapshot and
     * enables/disables the Save button (disabled when nothing changed, a cue is invalid, or the
     * name is blank).
     */
    recomputeTranscriptionEditState: function () {
      const state = TIMAAT.MediumDatasets._transcriptionEdit
      if (!state) return

      const $viewer = $("#mediumTranscriptionViewer")
      const $header = $viewer.find(".card-header")
      const name = $header.find('[data-role="transcriptionEditNameInput"]').val().trim()


      const dirty = state.snapshot !== null &&
          TIMAAT.MediumDatasets._transcriptionEditSnapshot(name, state.cues) !== state.snapshot
      const hasError = state.cues.some(cue => cue.end < cue.start)
      const nameValid = name.length > 0

      $header.find('[data-role="transcriptionEditDirtyIndicator"]').toggleClass("d-none", !dirty)
      $header.find('[data-role="transcriptionEditSaveBtn"]').prop("disabled", !dirty || hasError || !nameValid)
    },

    /**
     * Renders the editable cue list into the body edit state. Each cue offers editable start/end
     * timecode fields (with steppers), a draggable mini timeline, a text area, a delete button and
     * live validation. Centered plus buttons above, below and between the cues insert new cues.
     */
    _renderTranscriptionEditor: function () {
      const state = TIMAAT.MediumDatasets._transcriptionEdit
      if (!state) return

      const $scroll = $("#mediumTranscriptionViewer").find('[data-role="transcriptionEditorScroll"]')
      $scroll.empty()

      const clampPercent = function (value) {
        return Math.max(0, Math.min(100, value))
      }

      // Updates every cue card's DOM in place (positions, durations, validation) without rebuilding.
      const updateAllCues = function () {
        state.cues.forEach((cue, index) => {
          const dom = cue.dom
          if (!dom) return
          const bad = cue.end <= cue.start
          const overlap = index > 0 && cue.start < state.cues[index - 1].end - 1

          dom.$card.toggleClass("error", bad)
          dom.$startField.toggleClass("bad", bad)
          dom.$endField.toggleClass("bad", bad)
          if (!dom.$startInput.is(":focus")) dom.$startInput.val(TIMAAT.Util.formatTime(cue.start, true))
          if (!dom.$endInput.is(":focus")) dom.$endInput.val(TIMAAT.Util.formatTime(cue.end, true))
          dom.$duration.text(bad ? "—" : ((cue.end - cue.start) / 1000).toFixed(1) + " s")

          const left = clampPercent((cue.start / state.total) * 100)
          const right = clampPercent((cue.end / state.total) * 100)
          dom.$span.css({left: left + "%", width: Math.max(0, right - left) + "%"})
          dom.$handleStart.css("left", left + "%")
          dom.$handleEnd.css("left", right + "%")

          if (bad) {
            dom.$errmsg.removeClass("d-none warning").text("")
            dom.$errmsg.append('<span class="fas fa-exclamation-triangle"></span> End must be before start.')
          } else if (overlap) {
            dom.$errmsg.removeClass("d-none").addClass("warning").text("")
            dom.$errmsg.append('<span class="fas fa-exclamation-triangle"></span> Overlapping previous cue.')
          } else {
            dom.$errmsg.addClass("d-none").removeClass("warning").text("")
          }
        })
      }

      const setCueTime = function (cue, start, end) {
        cue.start = Math.max(0, Math.round(start))
        cue.end = Math.max(0, Math.round(end))
        updateAllCues()
        TIMAAT.MediumDatasets.recomputeTranscriptionEditState()
      }

      const insertCueAt = function (index) {
        const prevEnd = index > 0 ? state.cues[index - 1].end : 0
        const nextStart = index < state.cues.length ? state.cues[index].start : prevEnd + 3000
        const start = prevEnd
        const end = nextStart > prevEnd ? nextStart : prevEnd + 2000
        state.cues.splice(index, 0, {id: state.nextId++, start: start, end: end, text: ""})
        if (end > state.total) state.total = end
        TIMAAT.MediumDatasets._renderTranscriptionEditor()
        TIMAAT.MediumDatasets.recomputeTranscriptionEditState()
      }

      const removeCue = function (cue) {
        state.cues = state.cues.filter(c => c.id !== cue.id)
        TIMAAT.MediumDatasets._renderTranscriptionEditor()
        TIMAAT.MediumDatasets.recomputeTranscriptionEditState()
      }

      const buildInserter = function (index, edge) {
        const $inserter = $('<div class="transcription-editor-inserter"></div>')
        if (edge) $inserter.addClass("edge")
        $inserter.attr("title", "Add cue here")
        $inserter.append('<button type="button" class="transcription-editor-inserter-button"><span class="fas fa-plus"></span></button>')
        $inserter.on("click", () => insertCueAt(index))
        return $inserter
      }

      const buildTimecodeField = function (label, cue, isStart) {
        const $field = $('<div class="transcription-editor-timecode"></div>')
        $field.append('<span class="transcription-editor-timecode-label">' + label + '</span>')
        const $input = $('<input type="text" class="transcription-editor-timecode-input">')
        $input.val(TIMAAT.Util.formatTime(isStart ? cue.start : cue.end, true))

        const apply = function (valueMs) {
          if (isStart) setCueTime(cue, valueMs, cue.end)
          else setCueTime(cue, cue.start, valueMs)
        }
        const commit = function () {
          const parsed = TIMAAT.Util.parseTime($input.val())
          if (parsed !== null && !isNaN(parsed)) apply(parsed)
          else $input.val(TIMAAT.Util.formatTime(isStart ? cue.start : cue.end, true))
        }
        const nudge = function (deltaMs) {
          apply(Math.max(0, (isStart ? cue.start : cue.end) + deltaMs))
        }

        $input.on("blur", commit)
        $input.on("keydown", e => {
          if (e.key === "Enter") {
            e.preventDefault()
            e.target.blur()
          } else if (e.key === "ArrowUp") {
            e.preventDefault()
            nudge(e.shiftKey ? 1000 : 100)
          } else if (e.key === "ArrowDown") {
            e.preventDefault()
            nudge(e.shiftKey ? -1000 : -100)
          }
        })

        const $steppers = $('<div class="transcription-editor-steppers"></div>')
        const $up = $('<button type="button" class="transcription-editor-stepper" title="+0,1 s (Shift: +1 s)"><span class="fas fa-chevron-up"></span></button>')
        const $down = $('<button type="button" class="transcription-editor-stepper" title="−0,1 s (Shift: −1 s)"><span class="fas fa-chevron-down"></span></button>')
        $up.on("click", e => nudge(e.shiftKey ? 1000 : 100))
        $down.on("click", e => nudge(e.shiftKey ? -1000 : -100))
        $steppers.append($up).append($down)

        $field.append($input).append($steppers)
        return {$field: $field, $input: $input}
      }

      const buildTimeline = function (cue) {
        const $timeline = $('<div class="transcription-editor-timeline"></div>')
        const $span = $('<div class="transcription-editor-timeline-span"></div>')
        const $handleStart = $('<div class="transcription-editor-timeline-handle start"></div>')
        const $handleEnd = $('<div class="transcription-editor-timeline-handle end"></div>')
        $timeline.append($span).append($handleStart).append($handleEnd)

        const startDrag = function (which) {
          return function (downEvent) {
            downEvent.preventDefault()
            downEvent.stopPropagation()
            const rect = $timeline[0].getBoundingClientRect()
            const move = function (moveEvent) {
              let t = ((moveEvent.clientX - rect.left) / rect.width) * state.total
              t = Math.max(0, Math.min(state.total, t))
              if (which === "start") setCueTime(cue, Math.min(t, cue.end - 100), cue.end)
              else setCueTime(cue, cue.start, Math.max(t, cue.start + 100))
            }
            const up = function () {
              window.removeEventListener("pointermove", move)
              window.removeEventListener("pointerup", up)
              TIMAAT.MediumDatasets.recomputeTranscriptionEditState()
            }
            window.addEventListener("pointermove", move)
            window.addEventListener("pointerup", up)
          }
        }
        $handleStart.on("pointerdown", startDrag("start"))
        $handleEnd.on("pointerdown", startDrag("end"))

        return {$timeline: $timeline, $span: $span, $handleStart: $handleStart, $handleEnd: $handleEnd}
      }

      $scroll.append(buildInserter(0, true))
      if (state.cues.length === 0) {
        $scroll.append('<div class="text-center text-muted py-1" style="font-size:12.5px">No Cues defined. You can add one by pressing +</div>')
      }

      state.cues.forEach((cue, index) => {
        const $card = $('<div class="transcription-editor-cue"></div>')

        const $topbar = $('<div class="transcription-editor-topbar"></div>')
        const startField = buildTimecodeField("Start", cue, true)
        const endField = buildTimecodeField("End", cue, false)
        const $duration = $('<span class="transcription-editor-duration"></span>')
        const $delete = $('<button type="button" class="transcription-editor-delete" title="Delete cue"><span class="fas fa-trash"></span></button>')
        $delete.on("click", () => removeCue(cue))

        $topbar.append(startField.$field)
        $topbar.append('<span class="transcription-editor-arrow">→</span>')
        $topbar.append(endField.$field)
        $topbar.append($duration)
        $topbar.append('<span class="transcription-editor-spacer"></span>')
        $topbar.append($delete)

        const timeline = buildTimeline(cue)

        const $textarea = $('<textarea class="transcription-editor-textarea" placeholder="Transkript dieses Cues…"></textarea>')
        $textarea.val(cue.text)
        $textarea.on("input", () => {
          cue.text = $textarea.val()
          TIMAAT.MediumDatasets.recomputeTranscriptionEditState()
        })

        const $errmsg = $('<div class="transcription-editor-errmsg d-none"></div>')

        $card.append($topbar).append(timeline.$timeline).append($textarea).append($errmsg)
        $scroll.append($card)
        $scroll.append(buildInserter(index + 1, index === state.cues.length - 1))

        cue.dom = {
          $card: $card,
          $startField: startField.$field,
          $startInput: startField.$input,
          $endField: endField.$field,
          $endInput: endField.$input,
          $duration: $duration,
          $span: timeline.$span,
          $handleStart: timeline.$handleStart,
          $handleEnd: timeline.$handleEnd,
          $errmsg: $errmsg
        }
      })

      updateAllCues()
    },

    initTitles: function () {
      $('#mediumTitlesTab').on('click', function (event) {
        let medium = $('#mediumFormMetadata').data('medium');
        let type = medium.model.mediaType.mediaTypeTranslations[0].type;
        let name = medium.model.displayTitle.name;
        let id = medium.model.id;
        TIMAAT.UI.displayDataSetContentArea('mediumFormTitles');
        TIMAAT.MediumDatasets.setMediumTitleList(medium);
        TIMAAT.UI.displayDataSetContent('titles', medium, 'medium');
        if (type == 'medium') {
          TIMAAT.URLHistory.setURL(null, name + ' · Titles · ' + type[0].toUpperCase() + type.slice(1), '#medium/' + id + '/titles');
        } else {
          TIMAAT.URLHistory.setURL(null, name + ' · Titles · ' + type[0].toUpperCase() + type.slice(1), '#medium/' + type + '/' + id + '/titles');
        }
      });

      $(document).on('click', '.isOriginalTitleMedium', function (event) {
        if ($(this).data('wasChecked') == true) {
          $(this).prop('checked', false);
          $('input[name="isOriginalTitleMedium"]').data('wasChecked', false);
        } else {
          $('input[name="isOriginalTitleMedium"]').data('wasChecked', false);
          $(this).data('wasChecked', true);
        }
      });

      // Add title button click
      // $(document).on('click','[data-role="mediumNewTitleFields"] > .form-group [data-role="add"]', function(event) {
      $(document).on('click', '.addTitleButton', function (event) {
        event.preventDefault();
        // console.log("TCL: add title to list");
        var listEntry = $(this).closest('[data-role="mediumNewTitleFields"]');
        var title = '';
        var languageId = null;
        var languageName = '';
        if (listEntry.find('input').each(function () {
          title = $(this).val();
        })) ;
        if (listEntry.find('select').each(function () {
          languageId = $(this).val();
          languageName = $(this).text();
        })) ;
        if (!$('#mediumFormTitles').valid())
          return false;
        if (title != '' && languageId != null) {
          var titlesInForm = $('#mediumFormTitles').serializeArray();
          // console.log("TCL: titlesInForm", titlesInForm);
          var numberOfTitleElements = 2;
          var indexName = titlesInForm[titlesInForm.length - numberOfTitleElements - 1].name; // find last used indexed name
          var indexString = indexName.substring(indexName.lastIndexOf("[") + 1, indexName.lastIndexOf("]"));
          var i = Number(indexString) + 1;
          // console.log("i", i);
          $('#mediumDynamicTitleFields').append(
              `<div class="form-group" data-role="titleEntry">
						<div class="form-row">
							<div class="col-sm-2 col-md-1 text-center">
								<div class="form-check">
									<label class="sr-only" for="isDisplayTitle"></label>
									<input class="form-check-input isDisplayTitle"
												 type="radio"
												 name="isDisplayTitle"
												 data-role="displayTitle"
												 placeholder="Is Display Title">
								</div>
							</div>
							<div class="col-sm-2 col-md-1 text-center">
								<div class="form-check">
									<label class="sr-only" for="isOriginalTitleMedium"></label>
									<input class="form-check-input isOriginalTitleMedium"
												 type="radio"
												 name="isOriginalTitleMedium"
												 data-role="originalTitle"
												 data-wasChecked="false"
												 placeholder="Is Original Title">
								</div>
							</div>
							<div class="col-sm-5 col-md-7">
								<label class="sr-only">Title</label>
								<input class="form-control form-control-sm mediumDatasetsMediumTitlesTitleName"
											 name="newTitle[` + i + `]"
											 data-role="newTitle[` + i + `]"
											 placeholder="[Enter title]"
											 aria-describedby="Title"
											 minlength="3"
											 maxlength="200"
											 rows="1"
											 required>
							</div>
							<div class="col-sm-2 col-md-2">
								<label class="sr-only">Title's Language</label>
								<select class="form-control form-control-sm mediumDatasetsMediumTitlesTitleLanguageId"
												id="mediumNewTitleLanguageSelectDropdown_` + i + `"
												name="newTitleLanguageId[` + i + `]"
												data-role="newTitleLanguageId[` + i + `]"
												required>
								</select>
							</div>
							<div class="col-sm-1 col-md-1 text-center">
								<button class="form-group__button js-form-group__button removeTitleButton btn btn-danger" data-role="remove">
									<i class="fas fa-trash-alt"></i>
								</button>
							</div>
						</div>
					</div>`
          );
          $('#mediumNewTitleLanguageSelectDropdown_' + i).select2({
            closeOnSelect: true,
            scrollAfterSelect: true,
            allowClear: true,
            ajax: {
              url: 'api/language/selectList/',
              type: 'GET',
              dataType: 'json',
              delay: 250,
              headers: {
                "Authorization": "Bearer " + TIMAAT.Service.token,
                "Content-Type": "application/json",
              },
              // additional parameters
              data: function (params) {
                // console.log("TCL: data: params", params);
                return {
                  search: params.term,
                  page: params.page
                };
              },
              processResults: function (data, params) {
                // console.log("TCL: processResults: data", data);
                params.page = params.page || 1;
                return {
                  results: data
                };
              },
              cache: false
            },
            minimumInputLength: 0,
          });
          var languageSelect = $('#mediumNewTitleLanguageSelectDropdown_' + i);
          var option = new Option(languageName, languageId, true, true);
          languageSelect.append(option).trigger('change');
          $('input[name="newTitle[' + i + ']"]').rules('add', {
            required: true,
            minlength: 3,
            maxlength: 200,
          });
          $('input[data-role="newTitle[' + i + ']"]').attr('value', TIMAAT.MediumDatasets.replaceSpecialCharacters(title));
          $('#mediumTitleLanguageSelectDropdown').empty();
          if (listEntry.find('input').each(function () {
            $(this).val('');
          })) ;
          if (listEntry.find('select').each(function () {
            $(this).val('');
          })) ;
        } else {
          // TODO open modal showing error that not all required fields are set.
        }
      });

      // Remove title button click
      // $(document).on('click','[data-role="mediumDynamicTitleFields"] > .form-group [data-role="remove"]', function(event) {
      $(document).on('click', '.removeTitleButton', function (event) {
        event.preventDefault();
        var isDisplayTitle = $(this).closest('.form-group').find('input[name=isDisplayTitle]:checked').val();
        if (isDisplayTitle == "on") {
          // TODO modal informing that display title entry cannot be deleted
          console.info("CANNOT DELETE DISPLAY TITLE");
        } else {
          // TODO consider undo function or popup asking if user really wants to delete a title
          console.info("DELETE TITLE ENTRY");
          $(this).closest('.form-group').remove();
        }
      });

      // Submit medium titles button functionality
      $('#mediumFormTitlesSubmit').on('click', async function (event) {
        // console.log("TCL: Titles form: submit");
        // add rules to dynamically added form fields
        event.preventDefault();
        var node = document.getElementById("mediumNewTitleFields");
        while (node.lastChild) {
          node.removeChild(node.lastChild);
        }
        ;
        // test if form is valid
        if (!$('#mediumFormTitles').valid()) {
          $('[data-role="mediumNewTitleFields"]').append(TIMAAT.MediumDatasets.titleFormTitleToAppend());
          this.getTitleFormLanguageDropdownData();
          return false;
        }
        // console.log("TCL: Titles form: valid");

        // the original medium model (in case of editing an existing medium)
        var medium = $('#mediumFormTitles').data("medium");
        // console.log("TCL: type", type);

        // Create/Edit medium window submitted data
        var formData = $('#mediumFormTitles').serializeArray();
        // console.log("TCL: $ -> formData", formData);
        var formTitleList = [];
        var i = 0;
        while (i < formData.length) {
          var element = {
            isDisplayTitle: false,
            isOriginalTitle: false,
            title: '',
            titleLanguageId: 0,
          };
          if (formData[i].name == 'isDisplayTitle' && formData[i].value == 'on') {
            element.isDisplayTitle = true;
            if (formData[i + 1].name == 'isOriginalTitleMedium' && formData[i + 1].value == 'on') {
              // display title set, original title set
              element.isOriginalTitle = true;
              element.title = TIMAAT.MediumDatasets.replaceSpecialCharacters(formData[i + 2].value);
              element.titleLanguageId = formData[i + 3].value;
              i = i + 4;
            } else { // display title set, original title not set
              element.isOriginalTitle = false;
              element.title = TIMAAT.MediumDatasets.replaceSpecialCharacters(formData[i + 1].value);
              element.titleLanguageId = formData[i + 2].value;
              i = i + 3;
            }
          } else { // display title not set, original title set
            element.isDisplayTitle = false;
            if (formData[i].name == 'isOriginalTitleMedium' && formData[i].value == 'on') {
              element.isOriginalTitle = true;
              element.title = TIMAAT.MediumDatasets.replaceSpecialCharacters(formData[i + 1].value);
              element.titleLanguageId = formData[i + 2].value;
              i = i + 3;
            } else {
              // display title not set, original title not set
              element.isOriginalTitle = false;
              element.title = TIMAAT.MediumDatasets.replaceSpecialCharacters(formData[i].value);
              element.titleLanguageId = formData[i + 1].value;
              i = i + 2;
            }
          }
          ;
          formTitleList[formTitleList.length] = element;
        }
        // console.log("formTitleList", formTitleList);
        // only updates to existing titles
        if (formTitleList.length == medium.model.titles.length) {
          var i = 0;
          for (; i < medium.model.titles.length; i++) { // update existing titles
            var title = {
              id: medium.model.titles[i].id,
              language: {
                id: Number(formTitleList[i].titleLanguageId),
              },
              name: formTitleList[i].title,
            };
            // console.log("TCL: update existing titles");
            // only update if anything changed
            if (title != medium.model.titles[i]) {
              await TIMAAT.MediumDatasets.updateTitle(title, medium);
            }
            // update display title
            var displayTitleChanged = false;
            if (formTitleList[i].isDisplayTitle) {
              medium.model.displayTitle = medium.model.titles[i];
              displayTitleChanged = true;
            }
            // update original title
            var originalTitleChanged = false
            if (formTitleList[i].isOriginalTitle) {
              medium.model.originalTitle = medium.model.titles[i];
              originalTitleChanged = true;
              // }
              // // if form sets title and title was either not set or was set to different entry or title data has changed
              // if (formTitleList[i].isOriginalTitle && (medium.model.originalTitle == null || medium.model.originalTitle.id != medium.model.titles[i].id || medium.model.originalTitle == medium.model.titles[i])) {
              // 	console.log("medium.model.originalTitle", medium.model.originalTitle);
              // 	medium.model.originalTitle = medium.model.titles[i];
              //   console.log("medium.model.originalTitle", medium.model.originalTitle);
              // 	originalTitleChanged = true;
              // else if form does not set title and title was set before and title was set to this entry
            } else if (!formTitleList[i].isOriginalTitle && medium.model.originalTitle != null && medium.model.originalTitle.id == medium.model.titles[i].id) {
              medium.model.originalTitle = null;
              originalTitleChanged = true;
            }
            if (displayTitleChanged || originalTitleChanged) {
              await TIMAAT.MediumDatasets.updateMedium(type, medium);
            }
          }
          ;
        }
        // update existing titles and add new ones
        else if (formTitleList.length > medium.model.titles.length) {
          var i = 0;
          for (; i < medium.model.titles.length; i++) { // update existing titles
            var title = {
              id: medium.model.titles[i].id,
              language: {
                id: Number(formTitleList[i].titleLanguageId),
              },
              name: formTitleList[i].title,
            };
            // only update if anything changed
            if (title != medium.model.titles[i]) {
              // console.log("TCL: update existing titles (and add new ones)");
              await TIMAAT.MediumDatasets.updateTitle(title, medium);
            }
          }
          ;
          i = medium.model.titles.length;
          var newTitles = [];
          for (; i < formTitleList.length; i++) { // create new titles
            var title = {
              id: 0,
              language: {
                id: Number(formTitleList[i].titleLanguageId),
              },
              name: formTitleList[i].title,
            };
            newTitles.push(title);
          }
          // console.log("TCL: (update existing titles and) add new ones");
          await TIMAAT.MediumDatasets.addTitles(medium, newTitles);
          i = 0;
          for (; i < formTitleList.length; i++) {
            // update display title
            var displayTitleChanged = false;
            if (formTitleList[i].isDisplayTitle) {
              medium.model.displayTitle = medium.model.titles[i];
              displayTitleChanged = true;
            }
            // update original title
            var originalTitleChanged = false
            if (formTitleList[i].isOriginalTitle) {
              medium.model.originalTitle = medium.model.titles[i];
              originalTitleChanged = true;
              // if (formTitleList[i].isOriginalTitle && (medium.model.originalTitle == null || medium.model.originalTitle.id != medium.model.titles[i].id || medium.model.originalTitle == medium.model.titles[i])) {
              // 	medium.model.originalTitle = medium.model.titles[i];
              // 	originalTitleChanged = true;
            } else if (!formTitleList[i].isOriginalTitle && medium.model.originalTitle != null && medium.model.originalTitle.id == medium.model.titles[i].id) {
              medium.model.originalTitle = null;
              originalTitleChanged = true;
            }
            if (displayTitleChanged || originalTitleChanged) {
              await TIMAAT.MediumDatasets.updateMedium(type, medium);
            }
          }
        }
        // update existing titles and delete obsolete ones
        else if (formTitleList.length < medium.model.titles.length) {
          var i = 0;
          for (; i < formTitleList.length; i++) { // update existing titles
            var title = {
              id: medium.model.titles[i].id,
              language: {
                id: Number(formTitleList[i].titleLanguageId),
              },
              name: formTitleList[i].title,
            };
            if (title != medium.model.titles[i]) {
              // console.log("TCL: update existing titles (and delete obsolete ones)");
              await TIMAAT.MediumDatasets.updateTitle(title, medium);
            }
            // update display title
            var displayTitleChanged = false;
            if (formTitleList[i].isDisplayTitle) {
              medium.model.displayTitle = medium.model.titles[i];
              displayTitleChanged = true;
            }
            // update original title
            var originalTitleChanged = false
            if (formTitleList[i].isOriginalTitle) {
              medium.model.originalTitle = medium.model.titles[i];
              originalTitleChanged = true;
              // if (formTitleList[i].isOriginalTitle && (medium.model.originalTitle == null || medium.model.originalTitle.id != medium.model.titles[i].id || medium.model.originalTitle == medium.model.titles[i])) {
              // 	medium.model.originalTitle = medium.model.titles[i];
              // 	originalTitleChanged = true;
            } else if (!formTitleList[i].isOriginalTitle && medium.model.originalTitle != null && medium.model.originalTitle.id == medium.model.titles[i].id) {
              medium.model.originalTitle = null;
              originalTitleChanged = true;
            }
            if (displayTitleChanged || originalTitleChanged) {
              await TIMAAT.MediumDatasets.updateMedium(type, medium);
            }
          }
          ;
          var i = medium.model.titles.length - 1;
          for (; i >= formTitleList.length; i--) { // remove obsolete titles
            if (medium.model.originalTitle != null && medium.model.originalTitle.id == medium.model.titles[i].id) {
              medium.model.originalTitle = null;
              // console.log("TCL: remove originalTitle before deleting title");
              await TIMAAT.MediumDatasets.updateMedium(type, medium);
            }
            // console.log("TCL: (update existing titles and) delete obsolete ones");
            TIMAAT.MediumService.removeTitle(medium.model.titles[i]);
            medium.model.titles.pop();
          }
        }
        // console.log("TCL: show medium title form");
        if ($('#mediumTab').hasClass('active')) {
          await TIMAAT.UI.refreshDataTable('medium');
        } else {
          await TIMAAT.UI.refreshDataTable(type);
        }
        // TIMAAT.UI.addSelectedClassToSelectedItem(type, medium.model.id); // is done in displayDataSetContent
        TIMAAT.UI.displayDataSetContent('titles', medium, 'medium');
      });

      // Cancel add/edit button in titles form functionality
      $('#mediumFormTitlesDismiss').on('click', function (event) {
        let medium = $('#mediumFormMetadata').data('medium');
        TIMAAT.UI.displayDataSetContent('titles', medium, 'medium');
      });

      // Key press events
      $('#mediumFormTitlesSubmit').on('keypress', function (event) {
        event.stopPropagation();
        if (event.which == '13') { // == enter
          $('#mediumFormTitlesSubmit').trigger('click');
        }
      });

      $('#mediumFormTitlesDismiss').on('keypress', function (event) {
        event.stopPropagation();
        if (event.which == '13') { // == enter
          $('#mediumFormTitlesDismiss').trigger('click');
        }
      });

      $('#mediumDynamicTitleFields').on('keypress', function (event) {
        // event.stopPropagation();
        if (event.which == '13') { // == enter
          event.preventDefault(); // prevent activating delete button when pressing enter in a field of the row
        }
      });

      $('#mediumNewTitleFields').on('keypress', function (event) {
        event.stopPropagation();
        if (event.which == '13') { // == enter
          event.preventDefault();
          $('#mediumNewTitleFields').find('[data-role="add"]').trigger('click');
        }
      });
    },

    initLanguageTracks: function () {
      // language track tab click handling
      $('#mediumLanguageTracksTab').on('click', function (event) {
        let medium = $('#mediumFormMetadata').data('medium');
        let type = medium.model.mediaType.mediaTypeTranslations[0].type;
        let name = medium.model.displayTitle.name;
        let id = medium.model.id;
        TIMAAT.UI.displayDataSetContentArea('mediumFormLanguageTracks');
        TIMAAT.MediumDatasets.setMediumLanguageTrackList(medium);
        TIMAAT.UI.displayDataSetContent('languageTracks', medium, 'medium');
        if (type == 'medium') {
          TIMAAT.URLHistory.setURL(null, name + ' · Languages · ' + type[0].toUpperCase() + type.slice(1), '#medium/' + id + '/languages');
        } else {
          TIMAAT.URLHistory.setURL(null, name + ' · Languages · ' + type[0].toUpperCase() + type.slice(1), '#medium/' + type + '/' + id + '/languages');
        }
      });

      // Add languageTrack button click
      // $(document).on('click','[data-role="mediumNewLanguageTrackFields"] > .form-group [data-role="add"]', async function(event) {
      $(document).on('click', '.addLanguageTrackButton', async function (event) {
        event.preventDefault();
        var listEntry = $(this).closest('[data-role="mediumNewLanguageTrackFields"]');
        var mediumLanguageTypeId = listEntry.find('[data-role="languageTrackTypeId"]').val();
        var languageId = listEntry.find('[data-role="languageTrackLanguageId"]').val();
        var languageName = listEntry.find('[data-role="languageTrackLanguageId"]').text();
        // if (!$('#mediumFormLanguageTracks').valid()) return false;
        if (mediumLanguageTypeId != null && languageId != null) {
          var medium = $('#mediumFormMetadata').data('medium');
          var languageTracksInForm = $('#mediumFormLanguageTracks').serializeArray();
          var newTrackEntry = {
            mediumId: medium.model.id,
            languageId: Number(languageTracksInForm[languageTracksInForm.length - 1].value),
            mediumLanguageTypeId: Number(languageTracksInForm[languageTracksInForm.length - 2].value),
          };
          var duplicate = false;
          var i = 0;
          while (i < medium.model.mediumHasLanguages.length) {
            if (newTrackEntry.mediumLanguageTypeId == medium.model.mediumHasLanguages[i].id.mediumLanguageTypeId && newTrackEntry.languageId == medium.model.mediumHasLanguages[i].id.languageId) {
              duplicate = true;
              break;
            }
            i++;
          }
          if (!duplicate) {
            var i = Math.floor((languageTracksInForm.length - 2) / 2); // length -2 as the 'add new track' row is still part of the form and has to be removed
            // console.log("TCL: i", i);
            $('#mediumDynamicLanguageTrackFields').append(
                `<div class="form-group" data-role="languageTrackEntry" data-id="` + i + `">
							<div class="form-row">
								<div class="col-md-5">
									<label class="sr-only">Track Type</label>
									<select class="form-control form-control-sm mediumDatasetsMediumLanguageTracksLanguageTrackTypeId"
													name="languageTrackTypeId[` + i + `]"
													data-role="languageTrackTypeId[` + i + `]"
													required
													readonly="true">
										<option value="1">Audio track</option>
										<option value="2">Subtitle track</option>
									</select>
								</div>
								<div class="col-md-5">
									<label class="sr-only">Track Language</label>
									<select class="form-control form-control-sm mediumDatasetsMediumLanguageTracksLanguageTrackLanguageId"
													id="newLanguageTrackLanguageSelectDropdown_` + i + `"
													name="languageTrackLanguageId[` + i + `]"
													data-role="languageTrackLanguageId[` + i + `]"
													required>
									</select>
								</div>
								<div class="col-md-2 text-center">
									<button class="form-group__button js-form-group__button removeLanguageTrackButton btn btn-danger" data-role="remove">
										<i class="fas fa-trash-alt"></i>
									</button>
								</div>
							</div>
						</div>`
            );
            $('#newLanguageTrackLanguageSelectDropdown_' + i).select2({
              closeOnSelect: true,
              scrollAfterSelect: true,
              allowClear: true,
              ajax: {
                url: 'api/language/selectList/',
                type: 'GET',
                dataType: 'json',
                delay: 250,
                headers: {
                  "Authorization": "Bearer " + TIMAAT.Service.token,
                  "Content-Type": "application/json",
                },
                // additional parameters
                data: function (params) {
                  // console.log("TCL: data: params", params);
                  return {
                    search: params.term,
                    page: params.page
                  };
                },
                processResults: function (data, params) {
                  // console.log("TCL: processResults: data", data);
                  params.page = params.page || 1;
                  return {
                    results: data
                  };
                },
                cache: false
              },
              minimumInputLength: 0,
            });
            var languageSelect = $('#newLanguageTrackLanguageSelectDropdown_' + i);
            var option = new Option(languageName, languageId, true, true);
            languageSelect.append(option).trigger('change');
            $('[data-role="languageTrackTypeId[' + i + ']"]').find('option[value=' + mediumLanguageTypeId + ']').attr('selected', true);
            $('select[name="languageTrackTypeId[' + i + ']"]').rules('add', {required: true});
            listEntry.find('[data-role="languageTrackTypeId"]').val('');
            listEntry.find('[data-role="languageTrackLanguageId"]').val('');
            await TIMAAT.MediumDatasets.addLanguageTrack(medium, newTrackEntry);
            // medium.updateUI();
            TIMAAT.UI.displayDataSetContent('languageTracks', medium, 'medium', 'edit');
          } else { // duplicate entry
            $('#mediumDatasetsLanguageTrackDuplicateModal').modal("show");
          }
        } else { // incomplete form data
          // TODO open modal showing error that not all required fields are set.
        }
      });

      // Remove languageTrack button click
      // $(document).on('click','[data-role="mediumDynamicLanguageTrackFields"] > .form-group [data-role="remove"]', async function(event) {
      $(document).on('click', '.removeLanguageTrackButton', async function (event) {
        event.preventDefault();
        var entry = $(this).closest('.form-group').attr('data-id');
        var medium = $('#mediumFormMetadata').data('medium');
        var listEntry = $(this).closest('[data-role="mediumDynamicLanguageTrackFields"]');
        var mediumLanguageTypeId = listEntry.find('[name="languageTrackTypeId[' + entry + ']"]').val();
        var languageId = listEntry.find('[name="languageTrackLanguageId[' + entry + ']"]').val();
        var i = 0;
        for (; i < medium.model.mediumHasLanguages.length; i++) {
          if (medium.model.mediumHasLanguages[i].id.languageId == languageId
              && medium.model.mediumHasLanguages[i].id.mediumLanguageTypeId == mediumLanguageTypeId) {
            await TIMAAT.MediumService.removeLanguageTrack(medium.model.mediumHasLanguages[i]);
            medium.model.mediumHasLanguages.splice(i, 1);
            break;
          }
        }
        TIMAAT.UI.displayDataSetContent('languageTracks', medium, 'medium', 'edit');
      });

      // Done button in languageTracks form functionality
      $('#mediumFormLanguageTracksDoneButton').click(function (event) {
        let medium = $('#mediumFormMetadata').data('medium');
        TIMAAT.UI.displayDataSetContent('languageTracks', medium, 'medium');
      });

    },

    initActorRoles: function () {
      $('#mediumActorWithRolesTab').on('click', function (event) {
        let medium = $('#mediumFormMetadata').data('medium');
        let type = medium.model.mediaType.mediaTypeTranslations[0].type;
        let name = medium.model.displayTitle.name;
        let id = medium.model.id;
        TIMAAT.UI.displayDataSetContentArea('mediumFormActorWithRoles');
        TIMAAT.UI.displayDataSetContent('actorWithRoles', medium, 'medium');
        if (type == 'medium') {
          TIMAAT.URLHistory.setURL(null, name + ' · Actors with Roles · ' + type[0].toUpperCase() + type.slice(1), '#medium/' + id + '/actorsWithRoles');
        } else {
          TIMAAT.URLHistory.setURL(null, name + ' · Actors with Roles · ' + type[0].toUpperCase() + type.slice(1), '#medium/' + type + '/' + id + '/actorsWithRoles');
        }
      });

      // add actor-has-roles button click
      // $(document).on('click','[data-role="mediumNewActorWithRoleFields"] > .form-group [data-role="add"]', async function(event) {
      $(document).on('click', '.addActorHasRolesButton', async function (event) {
        // console.log("TCL: add new actor with role(s)");
        event.preventDefault();
        var listEntry = $(this).closest('[data-role="mediumNewActorWithRoleFields"]');
        var newFormEntry = [];
        if (listEntry.find('select').each(function () {
          newFormEntry.push($(this).val());
        })) ;
        // var newEntryId = newFormEntry[0];
        // console.log("TCL: newFormEntry", newFormEntry);

        if (!$('#mediumFormActorWithRoles').valid() || newFormEntry[1].length == 0) //! temp solution to prevent adding actors without roles
            // if (!$('#mediumFormActorWithRoles').valid())
          return false;

        $('.disableOnSubmit').prop('disabled', true);
        $('[id^="mediumHasActorWithRoleActorId-"').prop('disabled', false);
        var existingEntriesInForm = $('#mediumFormActorWithRoles').serializeArray();
        $('[id^="mediumHasActorWithRoleActorId-"').prop('disabled', true);
        $('.disableOnSubmit').prop('disabled', false);
        // console.log("TCL: existingEntriesInForm", existingEntriesInForm);

        // create list of actorIds that the medium is already connected with
        var existingEntriesIdList = [];
        var i = 0;
        for (; i < existingEntriesInForm.length; i++) {
          if (existingEntriesInForm[i].name == "actorId") {
            existingEntriesIdList.push(Number(existingEntriesInForm[i].value));
          }
        }
        existingEntriesIdList.pop(); // remove new actor id
        // console.log("TCL: existingEntriesIdList", existingEntriesIdList);
        // check for duplicate medium-actor relation. only one allowed
        var duplicate = false;
        i = 0;
        while (i < existingEntriesIdList.length) {
          if (newFormEntry[0] == existingEntriesIdList[i]) {
            duplicate = true;
            // console.log("TCL: duplicate entry found");
            break;
          }
          // console.log("TCL: newEntryId", newEntryId);
          // console.log("TCL: existingEntriesIdList[i]", existingEntriesIdList[i]);
          i++;
        }

        if (!duplicate) {
          // var newActorId = newFormEntry[0];
          var newActorSelectData = $('#mediumHasActorWithRoleActorId').select2('data');
          var newActorId = newActorSelectData[0].id;
          // console.log("TCL: $ -> newActorId", newActorId);
          var newRoleSelectData = $('#mediumActorWithRolesMultiSelectDropdown').select2('data');
          // var actorHasRoleIds = newFormEntry[1];
          $('#mediumDynamicActorWithRoleFields').append(TIMAAT.MediumDatasets.appendActorWithRolesDataset(existingEntriesIdList.length, newActorId));
          TIMAAT.MediumDatasets.getMediumHasActorWithRoleData(newActorId);
          // select actor for new entry
          await TIMAAT.ActorService.getActor(newActorId).then(function (data) {
            // console.log("TCL: newActorId", newActorId);
            var actorSelect = $('#mediumHasActorWithRoleActorId-' + newActorId);
            // console.log("TCL: actorSelect", actorSelect);
            // console.log("TCL: then: data", data);
            var option = new Option(data.displayName.name, data.id, true, true);
            actorSelect.append(option).trigger('change');
            // manually trigger the 'select2:select' event
            actorSelect.trigger({
              type: 'select2:select',
              params: {
                data: data
              }
            });
          });
          $('#mediumHasActorWithRoleActorId-' + newActorId).prop('disabled', true);

          // provide roles list for new actor entry
          TIMAAT.MediumDatasets.getMediumHasActorWithRolesDropdownData(newActorId);

          var roleSelect = $('#mediumActorWithRolesMultiSelectDropdown-' + newActorId);
          var j = 0;
          for (; j < newRoleSelectData.length; j++) {
            var option = new Option(newRoleSelectData[j].text, newRoleSelectData[j].id, true, true);
            roleSelect.append(option).trigger('change');
          }
          roleSelect.trigger({
            type: 'select2:select',
            params: {
              data: newRoleSelectData
            }
          });

          // clear new entry values
          $('#mediumHasActorWithRoleActorId').val(null).trigger('change');
          // $('#mediumHasActorWithRoleActorId').prop('required', true);
          $('#mediumActorWithRolesMultiSelectDropdown').val(null).trigger('change');
          // $('#mediumActorWithRolesMultiSelectDropdown').prop('required', true);
        } else { // duplicate actor
          $('#mediumDatasetsActorWithRoleDuplicateModal').modal('show');
        }
      });

      // remove actor with roles button click
      // $(document).on('click','[data-role="mediumDynamicActorWithRoleFields"] > .form-group [data-role="remove"]', async function(event) {
      $(document).on('click', '.removeActorWithRoleButton', async function (event) {
        // console.log("TCL: remove actor with role(s)");
        event.preventDefault();
        $(this).closest('.form-group').remove();
      });

      // submit actor with roles button functionality
      $('#mediumFormActorWithRolesSubmitButton').on('click', async function (event) {
        // console.log("TCL: ActorWithRole form: submit");
        // add rules to dynamically added form fields
        event.preventDefault();
        var node = document.getElementById("mediumNewActorWithRoleFields");
        while (node.lastChild) {
          node.removeChild(node.lastChild);
        }

        //! temp solution to prevent adding actors without roles
        //TODO

        // test if form is valid
        if (!$('#mediumFormActorWithRoles').valid()) {
          $('[data-role="mediumNewActorWithRoleFields"]').append(this.appendNewActorHasRolesField());
          return false;
        }

        var medium = $('#mediumFormMetadata').data('medium');

        // Create/Edit actor window submitted data
        $('.disableOnSubmit').prop('disabled', true);
        $('[id^="mediumHasActorWithRoleActorId-"').prop('disabled', false);
        var formDataRaw = $('#mediumFormActorWithRoles').serializeArray();
        $('[id^="mediumHasActorWithRoleActorId-"').prop('disabled', true);
        $('.disableOnSubmit').prop('disabled', false);
        // console.log("TCL: formDataRaw", formDataRaw);

        var formDataObject = {};
        $(formDataRaw).each(function (i, field) {
          formDataObject[field.name] = field.value;
        });

        var formEntryIds = []; // List of all actors containing role data for this medium
        var i = 0;
        for (; i < formDataRaw.length; i++) {
          if (formDataRaw[i].name == 'actorId') {
            formEntryIds.push(Number(formDataRaw[i].value));
          }
        }
        // console.log("TCL: Actor Ids in form", formEntryIds);
        // create actor id list for all already existing roles
        i = 0;
        var actorList = await TIMAAT.MediumService.getActorList(medium.model.id);
        // console.log("TCL: Actors of current Medium", actorList);
        var existingEntriesIdList = [];
        for (; i < actorList.length; i++) {
          existingEntriesIdList.push(actorList[i].id);
        }
        // DELETE actor with roles data if id is in existingEntriesIdList but not in formEntryIds
        i = 0;
        for (; i < existingEntriesIdList.length; i++) {
          // console.log("TCL: check for DELETE ACTOR: ", existingEntriesIdList[i]);
          var j = 0;
          var deleteDataset = true;
          for (; j < formEntryIds.length; j++) {
            if (existingEntriesIdList[i] == formEntryIds[j]) {
              deleteDataset = false;
              break; // no need to check further if match was found
            }
          }
          if (deleteDataset) {
            // console.log("TCL: REMOVE actor entries with Id: ", formEntryIds[j]);
            // console.log("TCL: Actor removed: REMOVE medium has actor (with all roles) datasets:", medium.model.id, existingEntriesIdList[i]);
            await TIMAAT.MediumService.removeActorFromMediumHasActorWithRoles(medium.model.id, existingEntriesIdList[i]);
            existingEntriesIdList.splice(i, 1);
            i--; // so the next list item is not jumped over due to the splicing
          }
        }
        // console.log("TCL: DELETE actorWithRole (end)");
        // ADD actor with roles data if id is not in existingEntriesIdList but in formEntryIds
        i = 0;
        for (; i < formEntryIds.length; i++) {
          // console.log("TCL: check for ADD ACTOR: ", formEntryIds[i]);
          var j = 0;
          var datasetExists = false;
          for (; j < existingEntriesIdList.length; j++) {
            if (formEntryIds[i] == existingEntriesIdList[j]) {
              datasetExists = true;
              break; // no need to check further if match was found
            }
          }
          if (!datasetExists) {
            // console.log("TCL: ADD actor entries with id: ", formEntryIds[i]);
            var roleSelectData = $('#mediumActorWithRolesMultiSelectDropdown-' + formEntryIds[i]).select2('data');
            // console.log("TCL: roleSelectData", roleSelectData);
            var k = 0;
            for (; k < roleSelectData.length; k++) {
              // console.log("TCL: New Actor: ADD medium has actor with role dataset: ", medium.model.id, formEntryIds[i], Number(roleSelectData[k].id));
              await TIMAAT.MediumService.addMediumHasActorWithRoles(medium.model.id, formEntryIds[i], Number(roleSelectData[k].id));
            }
            formEntryIds.splice(i, 1);
            // console.log("TCL: formEntryIds", formEntryIds);
            i--; // so the next list item is not jumped over due to the splicing
          }
        }
        // console.log("TCL: ADD new actorWithRole (end)");
        //* the splicing in remove and add sections reduced both id lists to the same entries remaining to compute
        // UPDATE actor with roles data if id is in existingEntriesIdList and in formEntryIds
        i = 0;
        for (; i < existingEntriesIdList.length; i++) {
          // console.log("TCL: check for UPDATE ACTOR: ", existingEntriesIdList[i]);
          var existingRoles = await TIMAAT.MediumService.getActorHasRoleList(medium.model.id, existingEntriesIdList[i]);
          // console.log("TCL: existingRoles", existingRoles);
          var existingRoleIds = [];
          var j = 0;
          for (; j < existingRoles.length; j++) {
            existingRoleIds.push(existingRoles[j].id);
          }
          // console.log("TCL: existing role ids for the current actor", existingRoleIds);
          var roleSelectData = $('#mediumActorWithRolesMultiSelectDropdown-' + existingEntriesIdList[i]).select2('data');
          // console.log("TCL: roleSelectData", roleSelectData);
          if (roleSelectData == undefined) {
            roleSelectData = [];
          }
          var roleSelectIds = [];
          j = 0;
          for (; j < roleSelectData.length; j++) {
            roleSelectIds.push(Number(roleSelectData[j].id));
          }
          // console.log("TCL: form role ids for the current actor: ", roleSelectIds);
          // DELETE role entry if id is in existingRoleIds but not in roleSelectIds
          j = 0;
          for (; j < existingRoleIds.length; j++) {
            // console.log("TCL: check for DELETE ROLE: ", existingRoleIds[j]);
            var k = 0;
            var deleteDataset = true;
            for (; k < roleSelectIds.length; k++) {
              if (existingRoleIds[j] == roleSelectIds[k]) {
                deleteDataset = false;
                break; // no need to check further if match was found
              }
            }
            if (deleteDataset) {
              // console.log("TCL: REMOVE role entry with Id: ", existingRoleIds[j]);
              // console.log("TCL: role removed: REMOVE medium has actor with role dataset: ", medium.model.id, existingEntriesIdList[i], existingRoleIds[j]);
              await TIMAAT.MediumService.removeRoleFromMediumHasActorWithRoles(medium.model.id, existingEntriesIdList[i], existingRoleIds[j]);
              existingRoleIds.splice(j, 1);
              j--; // so the next list item is not jumped over due to the splicing
            }
          }
          // ADD role entry if id is not in existingRoleIds but in roleSelectIds
          j = 0;
          for (; j < roleSelectIds.length; j++) {
            // console.log("TCL: check for ADD ROLE: ", roleSelectIds[j]);
            var k = 0;
            var datasetExists = false;
            for (; k < existingRoleIds.length; k++) {
              if (roleSelectIds[j] == existingRoleIds[k]) {
                datasetExists = true;
                break; // no need to check further if match was found
              }
            }
            if (!datasetExists) {
              // console.log("TCL: ADD actor entries with id: ", roleSelectIds[j]);
              // console.log("TCL: role added: ADD medium has actor with role dataset: ", medium.model.id, existingEntriesIdList[i], roleSelectIds[j]);
              await TIMAAT.MediumService.addMediumHasActorWithRoles(medium.model.id, existingEntriesIdList[i], roleSelectIds[j]);
              roleSelectIds.splice(j, 1);
              // console.log("TCL: roleSelectIds", roleSelectIds);
              j--; // so the next list item is not jumped over due to the splicing
            }
          }
          // no UPDATE as medium-actor-role table only has ids and no information stored
        }
        medium.model = await TIMAAT.MediumService.getMedium(medium.model.id);
        // medium.updateUI();
        TIMAAT.UI.displayDataSetContent('actorWithRoles', medium, 'medium');
      });

      // cancel add/edit button in titles form functionality
      $('#mediumFormActorWithRolesDismissButton').on('click', function (event) {
        let medium = $('#mediumFormMetadata').data('medium');
        TIMAAT.UI.displayDataSetContent('actorWithRoles', medium, 'medium');
      });

    },

    load: function () {
      this.loadMedia();
      this.loadMediaTypes();
      // this.setAudioList();
      // this.setDocumentList();
      // this.setImageList();
      // this.setSoftwareList();
      // this.setTextList();
      // this.setVideoList();
      // this.setVideogameList();
    },

    loadMediaTypes: function () {
      TIMAAT.MediumService.listMediumTypes(this.setMediumTypeList);
    },

    loadMedia: function () {
      $('#mediumFormMetadata').data('type', 'medium');
      $('#mediumVideoPreview').get(0).pause();
      this.setMediumList();
      TIMAAT.UI.addSelectedClassToSelectedItem('medium', null);
      TIMAAT.UI.subNavTab = 'dataSheet';
    },

    loadMediaDataTables: async function () {
      this.setupMediaDataTable();
      this.setupAudioDataTable();
      this.setupDocumentDataTable();
      this.setupImageDataTable();
      this.setupSoftwareDataTable();
      this.setupTextDataTable();
      this.setupVideoDataTable();
      this.setupVideogameDataTable();
    },

    loadMediumSubtype: function (type) {
      // console.log("TCL: $ -> type", type);
      $('#mediumFormMetadata').data('type', type);
      $('#mediumVideoPreview').get(0).pause();
      TIMAAT.UI.addSelectedClassToSelectedItem(type, null);
      TIMAAT.UI.subNavTab = 'dataSheet';
      this.showAddMediumButton();
      // TIMAAT.UI.clearLastSelection(type);
      switch (type) {
        case 'audio':
          this.setAudioList();
          break;
        case 'document':
          this.setDocumentList();
          break;
        case 'image':
          this.setImageList();
          break;
        case 'software':
          this.setSoftwareList();
          break;
        case 'text':
          this.setTextList();
          break;
        case 'video':
          this.setVideoList();
          break;
        case 'videogame':
          this.setVideogameList();
          break;
      }
    },

    loadRequiredRoleIds: async function () {
      this.producerId = await TIMAAT.RoleService.getRoleId('Producer');
      this.authorId = await TIMAAT.RoleService.getRoleId('Author');
    },

    setMediumTypeList: function (mediaTypes) {
      // console.log("TCL: mediaTypes", mediaTypes);
      if (!mediaTypes) return;
      // setup model
      var medTypes = Array();
      mediaTypes.forEach(function (mediaType) {
        if (mediaType.id > 0)
          medTypes.push(new TIMAAT.MediumType(mediaType));
      });
      TIMAAT.MediumDatasets.mediaTypes = medTypes;
      TIMAAT.MediumDatasets.mediaTypes.model = mediaTypes;
    },

    setMediumList: function () {
      if (this.media == null) return;

      // set ajax data source
      if (this.dataTableMedia) {
        this.dataTableMedia.ajax.reload(null, false);
        TIMAAT.UI.clearLastSelection('medium');
      }
      this.mediaLoaded = true;
    },

    setAudioList: function () {
      // console.log("TCL: setAudioList");
      if (this.audios == null) return;

      // set ajax data source
      if (this.dataTableAudio) {
        this.dataTableAudio.ajax.reload(null, false);
        TIMAAT.UI.clearLastSelection('audio');
      }
    },

    setDocumentList: function () {
      // console.log("TCL: setDocumentList");
      if (this.documents == null) return;

      // set ajax data source
      if (this.dataTableDocument) {
        this.dataTableDocument.ajax.reload(null, false);
        TIMAAT.UI.clearLastSelection('document');
      }
    },

    setImageList: function () {
      // console.log("TCL: setImageList");
      if (this.images == null) return;

      // set ajax data source
      if (this.dataTableImage) {
        this.dataTableImage.ajax.reload(null, false);
        TIMAAT.UI.clearLastSelection('image');
      }
    },

    setSoftwareList: function () {
      // console.log("TCL: setSoftwareList");
      if (this.softwares == null) return;

      // set ajax data source
      if (this.dataTableSoftware) {
        this.dataTableSoftware.ajax.reload(null, false);
        TIMAAT.UI.clearLastSelection('software');
      }
    },

    setTextList: function () {
      // console.log("TCL: setTextList");
      if (this.texts == null) return;

      // set ajax data source
      if (this.dataTableText) {
        this.dataTableText.ajax.reload(null, false);
        TIMAAT.UI.clearLastSelection('text');
      }
    },

    setVideoList: function () {
      // console.log("TCL: setVideoList");
      if (this.videos == null) return;

      if (this.dataTableVideo) {
        this.dataTableVideo.ajax.reload(null, false);
        TIMAAT.UI.clearLastSelection('video');
      }
    },

    setVideogameList: function () {
      // console.log("TCL: setVideogameList");
      if (this.videogames == null) return;

      // set ajax data source
      if (this.dataTableVideogame) {
        this.dataTableVideogame.ajax.reload(null, false);
        TIMAAT.UI.clearLastSelection('videogame');
      }
    },

    setMediumTitleList: function (medium) {
      // console.log("TCL: setMediumTitleList -> medium", medium);
      if (!medium) return;
      // setup model
      var mediumTitles = Array();
      medium.model.titles.forEach(function (title) {
        if (title.id > 0)
          mediumTitles.push(title);
      });
      this.titles = mediumTitles;
    },

    setMediumLanguageTrackList: function (medium) {
      // console.log("TCL: setMediumLanguageTrackList -> medium", medium);
      if (!medium) return;
      // setup model
      var mediumLanguageTracks = Array();
      medium.model.mediumHasLanguages.forEach(function (languageTrack) {
        if (languageTrack.mediumId > 0)
          mediumLanguageTracks.model.mediumHasLanguages.push(languageTrack);
      });
    },

    addMedium: function (type) {
      // console.log("TCL: addMedium: type", type);
      TIMAAT.UI.displayDataSetContentContainer('mediumMetadataTab', 'mediumFormMetadata');
      $('.addMediumButton').hide();
      $('.addMediumButton').prop('disabled', true);
      $('.addMediumButton :input').prop('disabled', true);
      $('#mediumFormMetadata').data('type', type);
      $('#mediumFormMetadata').data('medium', null);
      $('#mediumDisplayTitleLanguageSelectDropdown').empty().trigger('change');
      mediumFormMetadataValidator.resetForm();
      $('#mediumVideoPreview').get(0).pause();

      TIMAAT.UI.addSelectedClassToSelectedItem(type, null);
      TIMAAT.UI.subNavTab = 'dataSheet';
      $('#mediumFormMetadata').trigger('reset');

      // setup form
      this.initFormDataSheetData(type);
      this.getMediumFormTitleLanguageDropdownData();
      this.initFormDataSheetForEdit();
      $('#mediumFormMetadataSubmitButton').html('Add');
      $('#mediumFormHeader').html("Add " + type);
      $('#mediumDatasetsMetadataMediumSourceIsPrimarySource').prop('checked', true);
      // $('#mediumDatasetsMetadataMediumSourceIsStillAvailable').prop('checked', false);
    },

    mediumFormDataSheet: async function (action, type, data) {
      // console.log("TCL: action, type, data", action, type, data);
      // TIMAAT.UI.addSelectedClassToSelectedItem(type, data.model.id);
      $('#mediumFormMetadata').trigger('reset');
      this.initFormDataSheetData(type);
      mediumFormMetadataValidator.resetForm();
      $('#mediumVideoPreview').get(0).pause();

      this.getMediumFormTitleLanguageDropdownData();
      var languageSelect = $('#mediumDisplayTitleLanguageSelectDropdown');
      var option = new Option(data.model.displayTitle.language.name, data.model.displayTitle.language.id, true, true);
      languageSelect.append(option).trigger('change');

      if (action == 'show') {
        $('#mediumFormMetadata :input').prop('disabled', true);
        $('.formButtons').prop('disabled', false);
        $('.formButtons :input').prop('disabled', false);
        $('.formButtons').show();
        this.initFormForShow(data.model);
        $('#mediumFormMetadataSubmitButton').hide();
        $('#mediumFormMetadataDismissButton').hide();
        $('#mediumFormHeader').html(type + " Data Sheet (#" + data.model.id + ')');
        $('#mediumDisplayTitleLanguageSelectDropdown').select2('destroy').attr("readonly", true);
      } else if (action == 'edit') {
        this.initFormDataSheetForEdit();
        this.disableReadOnlyDataFields(true);
        $('.addMediumButton').hide();
        $('.addMediumButton').prop('disabled', true);
        $('.addMediumButton :input').prop('disabled', true);
        $('#mediumFormMetadataSubmitButton').html('Save');
        $('#mediumFormHeader').html("Edit " + type);
      }

      // setup UI
      // medium data
      $('#mediumDatasetsMetadataMediumTypeId').val(data.model.mediaType.id);
      $('#mediumDatasetsMetadataMediumCopyright').val(data.model.copyright);
      $('#mediumDatasetsMetadataMediumRemark').val(data.model.remark);
      if (data.model.releaseDate != null && !(isNaN(data.model.releaseDate)))
        $('#mediumDatasetsMetadataMediumReleaseDate').val(moment.utc(data.model.releaseDate).format('YYYY-MM-DD'));
      else $('#mediumDatasetsMetadataMediumReleaseDate').val('');
      if (data.model.recordingStartDate != null && !(isNaN(data.model.recordingStartDate)))
        $('#mediumDatasetsMetadataMediumRecordingStartDate').val(moment.utc(data.model.recordingStartDate).format('YYYY-MM-DD'));
      else $('#mediumDatasetsMetadataMediumRecordingStartDate').val('');
      if (data.model.recordingEndDate != null && !(isNaN(data.model.recordingEndDate)))
        $('#mediumDatasetsMetadataMediumRecordingEndDate').val(moment.utc(data.model.recordingEndDate).format('YYYY-MM-DD'));
      else $('#mediumDatasetsMetadataMediumRecordingEndDate').val('');
      // display-title data
      $('#mediumDatasetsMetadataMediumTitle').val(data.model.displayTitle.name);
      // source data
      if (data.model.sources[0].isPrimarySource)
        $('#mediumDatasetsMetadataMediumSourceIsPrimarySource').prop('checked', true);
      else $('#mediumDatasetsMetadataMediumSourceIsPrimarySource').prop('checked', false);
      $('#mediumDatasetsMetadataMediumSourceURL').val(data.model.sources[0].url);
      if (data.model.sources[0].lastAccessed != null && !(isNaN(data.model.sources[0].lastAccessed)))
        $('#mediumDatasetsMetadataMediumSourceLastAccessed').val(moment.utc(data.model.sources[0].lastAccessed).format('YYYY-MM-DD HH:mm'));
      else $('#mediumDatasetsMetadataMediumSourceLastAccessed').val('');
      if (data.model.sources[0].isStillAvailable)
        $('#mediumDatasetsMetadataMediumSourceIsStillAvailable').prop('checked', true);
      else $('#mediumDatasetsMetadataMediumSourceIsStillAvailable').prop('checked', false);

      // medium subtype specific data
      switch (type) {
        case 'audio':
          $('#mediumDatasetsMetadataAudioLength').val(TIMAAT.Util.formatTime(data.model.mediumAudio.length));
          if (data.model.mediumAudio.audioPostProduction) {
            $('#mediumDatasetsMetadataAudioAudioPostProductionOverdubbing').val(data.model.mediumAudio.audioPostProduction.audioPostProductionTranslations[0].overdubbing);
            $('#mediumDatasetsMetadataAudioAudioPostProductionReverb').val(data.model.mediumAudio.audioPostProduction.audioPostProductionTranslations[0].reverb);
            $('#mediumDatasetsMetadataAudioAudioPostProductionDelay').val(data.model.mediumAudio.audioPostProduction.audioPostProductionTranslations[0].delay);
            $('#mediumDatasetsMetadataAudioAudioPostProductionPanning').val(data.model.mediumAudio.audioPostProduction.audioPostProductionTranslations[0].panning);
            $('#mediumDatasetsMetadataAudioAudioPostProductionBass').val(data.model.mediumAudio.audioPostProduction.audioPostProductionTranslations[0].bass);
            $('#mediumDatasetsMetadataAudioAudioPostProductionTreble').val(data.model.mediumAudio.audioPostProduction.audioPostProductionTranslations[0].treble);
          }
          break;
        case "mediumDocument":
          break;
        case 'image':
          $('#mediumDatasetsMetadataImageWidth').val(data.model.mediumImage.width);
          $('#mediumDatasetsMetadataImageHeight').val(data.model.mediumImage.height);
          // $('#mediumDatasetsMetadataImageBitDepth').val(data.model.mediumImage.bitDepth);
          break;
        case 'software':
          $('#mediumDatasetsMetadataSoftwareVersion').val(data.model.mediumSoftware.version);
          break;
        case 'text':
          $('#mediumDatasetsMetadataTextContent').val(data.model.mediumText.content);
          break;
        case 'video':
          $('#mediumDatasetsMetadataVideoLength').val(TIMAAT.Util.formatTime(data.model.mediumVideo.length));
          // $('#mediumDatasetsMetadataVideoVideoCodec').val(data.model.mediumVideo.videoCodec);
          $('#mediumDatasetsMetadataVideoWidth').val(data.model.mediumVideo.width);
          $('#mediumDatasetsMetadataVideoHeight').val(data.model.mediumVideo.height);
          $('#mediumDatasetsMetadataVideoFrameRate').val(data.model.mediumVideo.frameRate);
          // $('#mediumDatasetsMetadataVideoDataRate').val(data.model.mediumVideo.dataRate);
          // $('#mediumDatasetsMetadataVideoTotalBitrate').val(data.model.mediumVideo.totalBitrate);
          if (data.model.mediumVideo.isEpisode)
            $('#mediumDatasetsMetadataVideoIsEpisode').prop('checked', true);
          else $('#mediumDatasetsMetadataVideoIsEpisode').prop('checked', false);
          break;
        case 'videogame':
          if (data.model.mediumVideogame.isEpisode)
            $('#mediumDatasetsMetadataVideogameIsEpisode').prop('checked', true);
          else $('#mediumDatasetsMetadataVideogameIsEpisode').prop('checked', false);
          break;
      }
      $('#mediumFormMetadata').data('medium', data);
    },

    mediumFormPreview: function (type, data) {
      // console.log("TCL: mediumFormPreview - type, data", type, data);
      // TIMAAT.UI.addSelectedClassToSelectedItem(type, data.model.id);
      $('#mediumFormPreview').trigger('reset');
      // mediumFormMetadataValidator.resetForm();

      // handling if type is 'medium' and user is in all media view
      if (type == 'medium') type = data.model.mediaType.mediaTypeTranslations[0].type;

      $('#mediumFormPreview :input').prop('disabled', true);
      $('.videoPreview').hide();
      $('.imagePreview').hide();
      $('.audioPreview').hide();

      if (data.model.fileStatus == 'noFile' || !data.model.fileStatus) {
        if (data.model.mediumVideo || data.model.mediumImage || data.model.mediumAudio) {
          $('.uploadMediumButton').prop('disabled', false);
          $('.uploadMediumButton').show();
        }
        $('.mediumDataSheetFormAnnotateButton').hide();
        $('.mediumDataSheetFormAnnotateButton').prop('disabled', true);
        $('.imagePreview').show();
        $('#mediumImagePreview').attr('title', 'placeholder');
        $('#mediumImagePreview').attr('alt', 'placeholder');
        switch (type) {
          case 'image':
            $('#mediumImagePreview').attr('src', 'img/preview-placeholder.png');
            break;
          default:
            $('#mediumImagePreview').attr('src', 'img/preview-placeholder.png');
            break;
        }
      } else {
        $('.uploadMediumButton').hide();
        $('.uploadMediumButton').prop('disabled', true);
        if (type == 'video' || type == 'image' || type == 'audio') {
          $('.mediumDataSheetFormAnnotateButton').prop('disabled', false);
          $('.mediumDataSheetFormAnnotateButton').show();
        } else {
          $('.mediumDataSheetFormAnnotateButton').hide();
          $('.mediumDataSheetFormAnnotateButton').prop('disabled', true);
        }
        switch (type) {
          case 'audio': // TODO check audioPreview functionality
            $('#mediumAudioPreview').attr('src', '/TIMAAT/api/medium/audio/' + data.model.id + '/download' + '?token=' + data.model.viewToken);
            $('.audioPreview').show();
            TIMAAT.MediumDatasets.updateFormPreviewSubtitle(data)
            break;
          case 'image':
            $('#mediumImagePreview').attr('src', '/TIMAAT/api/medium/image/' + data.model.id + '/preview' + '?token=' + data.model.viewToken);
            $('#mediumImagePreview').attr('title', data.model.displayTitle.name);
            $('#mediumImagePreview').attr('alt', data.model.displayTitle.name);
            $('.imagePreview').show();
            break;
          case 'video':
            if (data.model.fileStatus && data.model.fileStatus != 'ready' && data.model.fileStatus != 'transcoding' && data.model.fileStatus != 'waiting') {
              $('#mediumImagePreview').attr('src', 'img/preview-placeholder.png');
              $('#mediumImagePreview').attr('title', 'placeholder');
              $('#mediumImagePreview').attr('alt', 'placeholder');
              $('.imagePreview').show();
            } else {
              const $mediumVideoPreview = $('#mediumVideoPreview')
              $mediumVideoPreview.attr('src', '/TIMAAT/api/medium/video/' + data.model.id + '/download' + '?token=' + data.model.viewToken);

              TIMAAT.MediumDatasets.updateFormPreviewSubtitle(data)

              $('.videoPreview').show();
              $('.mediumDataSheetFormCaptureThumbnail').prop('disabled', false);
            }
            break;
          default:
            $('#mediumImagePreview').attr('src', 'img/preview-placeholder.png');
            $('#mediumImagePreview').attr('title', 'placeholder');
            $('#mediumImagePreview').attr('alt', 'placeholder');
            $('.imagePreview').show();
            break;
        }
      }
      $('.mediumDataSheetFormDeleteButton').prop('disabled', false);
      $('.mediumDataSheetFormDeleteButton :input').prop('disabled', false);
      $('.mediumDataSheetFormDeleteButton').show();
      $('#mediumFormPreviewHeader').html(type + " Preview (#" + data.model.id + ')');
    },

    updateFormPreviewSubtitle: function (medium) {
      if (medium.model.defaultTranscriptionId) {
        const type = medium.model.mediaType.mediaTypeTranslations[0].type


        TIMAAT.MediumService.downloadTranscriptionFile(medium.model.id, medium.model.defaultTranscriptionId).then(blob => {
          const blobUrl = URL.createObjectURL(blob);
          const track = $("<track default/>")
          track.attr('src', blobUrl);

          if (type === "video") {
            const $mediumVideoPreview = $('#mediumVideoPreview')
            $mediumVideoPreview.empty()
            $mediumVideoPreview.append(track);
          } else if (type === "audio") {
            const $mediumAudioPreview = $('#mediumAudioPreview')
            $mediumAudioPreview.empty()
            $mediumAudioPreview.append(track);
          }

        }).catch(error => {
          console.error("Error during loading transcription file.", error)

          if (type === "video") {
            const $mediumVideoPreview = $('#mediumVideoPreview')
            $mediumVideoPreview.empty()
          } else if (type === "audio") {
            const $mediumAudioPreview = $('#mediumAudioPreview')
            $mediumAudioPreview.empty()
          }
        })
      }
    },

    mediumFormTitles: function (action, medium) {
      // console.log("TCL: mediumFormTitles: (action, medium): ", action, medium);
      // TIMAAT.UI.addSelectedClassToSelectedItem(medium.model.mediaType.mediaTypeTranslations[0].type, medium.model.id);
      var node = document.getElementById("mediumDynamicTitleFields");
      while (node.lastChild) {
        node.removeChild(node.lastChild);
      }
      ;
      var node = document.getElementById("mediumNewTitleFields");
      while (node.lastChild) {
        node.removeChild(node.lastChild);
      }
      ;
      $('#mediumFormTitles').trigger('reset');
      mediumFormTitlesValidator.resetForm();
      $('#mediumVideoPreview').get(0).pause();

      // setup UI
      // display-title data
      var i = 0;
      var numTitles = medium.model.titles.length;
      // console.log("TCL: medium.model.titles", medium.model.titles);
      for (; i < numTitles; i++) {
        // console.log("TCL: medium.model.titles[i].language.id", medium.model.titles[i].language.id);
        $('[data-role="mediumDynamicTitleFields"]').append(
            `<div class="form-group" data-role="titleEntry">
						<div class="form-row">
							<div class="col-sm-2 col-md-1 text-center">
								<div class="form-check">
									<label class="sr-only" for="isDisplayTitle"></label>
									<input class="form-check-input isDisplayTitle"
												 type="radio"
												 name="isDisplayTitle"
												 data-role="displayTitle[` + medium.model.titles[i].id + `]"
												 placeholder="Is display title">
								</div>
							</div>
							<div class="col-sm-2 col-md-1 text-center">
								<div class="form-check">
									<label class="sr-only" for="isOriginalTitleMedium"></label>
									<input class="form-check-input isOriginalTitleMedium"
												 type="radio"
												 name="isOriginalTitleMedium"
												 data-role="originalTitle[` + medium.model.titles[i].id + `]"
												 data-wasChecked="true"
												 placeholder="Is original title">
								</div>
							</div>
							<div class="col-sm-5 col-md-7">
								<label class="sr-only">Title</label>
								<input class="form-control form-control-sm mediumDatasetsMediumTitlesTitleName"
											 name="title[` + i + `]"
											 data-role="title[` + medium.model.titles[i].id + `]"
											 placeholder="[Enter title]"
											 aria-describedby="Title"
											 minlength="3"
											 maxlength="200"
											 rows="1"
											 required>
							</div>
							<div class="col-sm-2 col-md-2">
								<label class="sr-only">Title's Language</label>
								<select class="form-control form-control-sm mediumDatasetsMediumTitlesTitleLanguageId"
												id="mediumTitleLanguageSelectDropdown_` + medium.model.titles[i].id + `"
												name="titleLanguageId[` + i + `]"
												data-role="titleLanguageId[` + medium.model.titles[i].id + `]"
												data-placeholder="Select title language"
												required>
								</select>
							</div>
							<div class="col-sm-1 col-md-1 text-center">
								<button class="form-group__button js-form-group__button removeTitleButton btn btn-danger"
												data-role="remove">
									<i class="fas fa-trash-alt"></i>
								</button>
							</div>
						</div>
					</div>`
        );
        $('#mediumTitleLanguageSelectDropdown_' + medium.model.titles[i].id).select2({
          closeOnSelect: true,
          scrollAfterSelect: true,
          allowClear: true,
          ajax: {
            url: 'api/language/selectList/',
            type: 'GET',
            dataType: 'json',
            delay: 250,
            headers: {
              "Authorization": "Bearer " + TIMAAT.Service.token,
              "Content-Type": "application/json",
            },
            // additional parameters
            data: function (params) {
              // console.log("TCL: data: params", params);
              return {
                search: params.term,
                page: params.page
              };
            },
            processResults: function (data, params) {
              // console.log("TCL: processResults: data", data);
              params.page = params.page || 1;
              return {
                results: data
              };
            },
            cache: false
          },
          minimumInputLength: 0,
        });
        let title = medium.model.titles[i];
        var languageSelect = $('#mediumTitleLanguageSelectDropdown_' + title.id);
        var option = new Option(title.language.name, title.language.id, true, true);
        languageSelect.append(option).trigger('change');
        if (medium.model.displayTitle.id == title.id) {
          $('[data-role="displayTitle[' + title.id + ']"]').prop('checked', true);
        }
        if (medium.model.originalTitle && medium.model.originalTitle.id == title.id) {
          $('[data-role="originalTitle[' + title.id + ']"]').prop('checked', true);
        }
        $('input[name="title[' + i + ']"]').rules("add", {required: true, minlength: 3, maxlength: 200,});
        $('input[data-role="title[' + title.id + ']"]').attr('value', TIMAAT.MediumDatasets.replaceSpecialCharacters(title.name));
      }
      ;

      if (action == 'show') {
        $('#mediumFormTitles :input').prop('disabled', true);
        this.initFormForShow(medium.model);
        $('#mediumFormTitlesSubmit').hide();
        $('#mediumFormTitlesDismiss').hide();
        $('[data-role="mediumNewTitleFields"').hide();
        $('.titleFormDivider').hide();
        // $('[data-role="remove"]').hide();
        // $('[data-role="add"]').hide();
        $('.js-form-group__button').hide();
        $('#mediumTitlesLabel').html("Medium titles");
        let i = 0;
        for (; i < numTitles; i++) {
          $('#mediumTitleLanguageSelectDropdown_' + medium.model.titles[i].id).select2('destroy').attr("readonly", true);
        }
      } else if (action == 'edit') {
        $('#mediumFormTitles :input').prop('disabled', false);
        this.hideFormButtons();
        $('#mediumFormTitlesSubmit').html("Save");
        $('#mediumFormTitlesSubmit').show();
        $('#mediumFormTitlesDismiss').show();
        $('#mediumTitlesLabel').html("Edit medium titles");
        $('[data-role="mediumNewTitleFields"').show();
        $('.titleFormDivider').show();
        $('#mediumDatasetsMetadataMediumTitle').focus();

        // fields for new title entry
        $('[data-role="mediumNewTitleFields"]').append(this.titleFormTitleToAppend());
        this.getTitleFormLanguageDropdownData();

        $('#mediumFormTitles').data('medium', medium);
      }
    },

    mediumFormLanguageTracks: function (action, medium) {
      // console.log("TCL: mediumFormLanguageTracks: action, medium", action, medium);
      // TIMAAT.UI.addSelectedClassToSelectedItem(medium.model.mediaType.mediaTypeTranslations[0].type, medium.model.id);
      var node = document.getElementById("mediumDynamicLanguageTrackFields");
      while (node.lastChild) {
        node.removeChild(node.lastChild);
      }
      ;
      var node = document.getElementById("mediumNewLanguageTrackFields");
      while (node.lastChild) {
        node.removeChild(node.lastChild);
      }
      ;
      $('#mediumFormLanguageTracks').trigger('reset');
      mediumFormLanguageTracksValidator.resetForm();
      $('#mediumVideoPreview').get(0).pause();

      // setup UI
      // languageTrack data
      var i = 0;
      // console.log("TCL: medium", medium);
      var numLanguageTracks = medium.model.mediumHasLanguages.length;
      for (; i < numLanguageTracks; i++) {
        $('[data-role="mediumDynamicLanguageTrackFields"]').append(
            `<div class="form-group" data-role="languageTrackEntry" data-id="` + i + `">
						<div class="form-row">
							<div class="col-md-5">
								<label class="sr-only">Track Type</label>
								<select class="form-control form-control-sm mediumDatasetsMediumLanguageTracksLanguageTrackTypeId"
												name="languageTrackTypeId[` + i + `]"
												data-role="languageTrackTypeId[` + medium.model.mediumHasLanguages[i].mediumLanguageType.id + `]"
												required>
									<!-- <option value="" disabled selected hidden>[Choose Track type...]</option> -->
									<option value="1">Audio Track</option>
									<option value="2">Subtitle Track</option>
								</select>
							</div>
							<div class="col-md-5">
								<label class="sr-only">Track Language</label>
								<select class="form-control form-control-sm mediumDatasetsMediumLanguageTracksLanguageTrackLanguageId"
												id="languageTrackLanguageSelectDropdown_` + i + `"
												name="languageTrackLanguageId[` + i + `]"
												data-role="languageTrackLanguageId[` + medium.model.mediumHasLanguages[i].language.id + `]"
												required>
								</select>
							</div>
							<div class="col-md-2 text-center">
								<button class="form-group__button js-form-group__button removeLanguageTrackButton btn btn-danger" data-role="remove">
									<i class="fas fa-trash-alt"></i>
								</button>
							</div>
						</div>
					</div>`
        );
        $('#languageTrackLanguageSelectDropdown_' + i).select2({
          closeOnSelect: true,
          scrollAfterSelect: true,
          allowClear: true,
          ajax: {
            url: 'api/language/selectList/',
            type: 'GET',
            dataType: 'json',
            delay: 250,
            headers: {
              "Authorization": "Bearer " + TIMAAT.Service.token,
              "Content-Type": "application/json",
            },
            // additional parameters
            data: function (params) {
              // console.log("TCL: data: params", params);
              return {
                search: params.term,
                page: params.page
              };
            },
            processResults: function (data, params) {
              // console.log("TCL: processResults: data", data);
              params.page = params.page || 1;
              return {
                results: data
              };
            },
            cache: false
          },
          minimumInputLength: 0,
        });
        var languageSelect = $('#languageTrackLanguageSelectDropdown_' + i);
        var option = new Option(medium.model.mediumHasLanguages[i].language.name, medium.model.mediumHasLanguages[i].language.id, true, true);
        languageSelect.append(option).trigger('change');
        $('[data-role="languageTrackTypeId[' + medium.model.mediumHasLanguages[i].mediumLanguageType.id + ']"')
            .find('option[value=' + medium.model.mediumHasLanguages[i].mediumLanguageType.id + ']')
            .attr('selected', true);
        $('select[name="languageTrackTypeId[' + i + ']"]').rules("add", {required: true});
        $('#languageTrackLanguageSelectDropdown_' + i).select2('destroy').attr("readonly", true);
      }
      ;

      if (action == 'show') {
        $('#mediumFormLanguageTracks :input').prop('disabled', true);
        this.initFormForShow(medium.model);
        $('#mediumFormLanguageTracksDoneButton').hide();
        $('[data-role="mediumNewLanguageTrackFields"').hide();
        $('.languageTrackFormDivider').hide();
        // $('[data-role="remove"]').hide();
        // $('[data-role="add"]').hide();
        $('.js-form-group__button').hide();
        $('#mediumLanguageTracksLabel').html("Medium track list");
      } else if (action == 'edit') {
        $('.mediumDatasetsMediumLanguageTracksLanguageTrackTypeId').prop('disabled', true);
        $('#mediumFormLanguageTracks :input').prop('disabled', false);
        this.initFormDataSheetForEdit();
        this.hideFormButtons();
        $('#mediumFormLanguageTracksDoneButton').html("Done");
        $('#mediumFormLanguageTracksDoneButton').show();
        $('[data-role="mediumNewLanguageTrackFields"').show();
        $('.languageTrackFormDivider').show();
        $('#mediumLanguageTracksLabel').html("Edit medium track list");

        // fields for new languageTrack entry
        // add empty 'add new track' row to form when edit mode is enabled
        $('[data-role="mediumNewLanguageTrackFields"]').append(
            `<div class="form-group" data-role="languageTrackEntry">
						<div class="form-row">
							<div class="col-md-12 text-left">
								<div class="form-check">
									<span>Add new Track:</span>
								</div>
							</div>
						</div>
						<div class="form-row">
							<div class="col-md-5">
								<label class="sr-only">Track type</label>
								<select class="form-control form-control-sm mediumDatasetsMediumLanguageTracksLanguageTrackTypeId"
												name="languageTrackTypeId"
												data-role="languageTrackTypeId"
												required>
									<option value="" disabled selected hidden>[Choose Track type...]</option>
									<option value="1">Audio Track</option>
									<option value="2">Subtitle Track</option>
								</select>
							</div>
							<div class="col-md-5">
								<label class="sr-only">Track language</label>
								<select class="form-control form-control-sm mediumDatasetsMediumLanguageTracksLanguageTrackLanguageId"
												id="languageTrackLanguageSelectDropdown"
												name="languageTrackLanguageId"
												data-role="languageTrackLanguageId"
												data-placeholder="Select language"
												required>
								</select>
							</div>
							<div class="col-md-2 text-center">
								<button class="form-group__button form-group__button--add js-form-group__button addLanguageTrackButton btn btn-primary" data-role="add">
									<i class="fas fa-plus"></i>
								</button>
							</div>
						</div>
					</div>`
        );
        $('#languageTrackLanguageSelectDropdown').select2({
          closeOnSelect: true,
          scrollAfterSelect: true,
          allowClear: true,
          ajax: {
            url: 'api/language/selectList/',
            type: 'GET',
            dataType: 'json',
            delay: 250,
            headers: {
              "Authorization": "Bearer " + TIMAAT.Service.token,
              "Content-Type": "application/json",
            },
            // additional parameters
            data: function (params) {
              // console.log("TCL: data: params", params);
              return {
                search: params.term,
                page: params.page
              };
            },
            processResults: function (data, params) {
              // console.log("TCL: processResults: data", data);
              params.page = params.page || 1;
              return {
                results: data
              };
            },
            cache: false
          },
          minimumInputLength: 0,
        });

        $('#mediumFormLanguageTracks').data('medium', medium);
      }
    },

    mediumFormMusic: async function (action, medium) {
      const mediumFormMusicDynamicFields = $('#mediumFormMusicDynamicFields')
      const mediumFormMusicNewMusicFields = $('#mediumFormMusicNewMusicFields')
      const editMode = action === 'edit';

      mediumFormMusicDynamicFields.empty()
      mediumFormMusicNewMusicFields.empty()

      // Extract direct and indirect relations to music into a uniform format
      const mediumTimeRangeDetailsByMusicId = new Map()
      const musicTitlesByMusicId = new Map()
      const musicIds = []

      for (const currentMediumHasMusic of medium.model.mediumHasMusicList) {
        const currentMusicId = currentMediumHasMusic.id.musicId
        const musicTitle = (await TIMAAT.MusicService.getDisplayTitle(currentMusicId)).name

        musicIds.push(currentMusicId)
        musicTitlesByMusicId.set(currentMusicId, musicTitle)

        const mediumTimeRangeDetails = []
        for (const currentMediumHasMusicDetail of currentMediumHasMusic.mediumHasMusicDetailList) {
          mediumTimeRangeDetails.push({
            type: "direct",
            startTime: currentMediumHasMusicDetail.startTime,
            endTime: currentMediumHasMusicDetail.endTime
          })
        }
        mediumTimeRangeDetailsByMusicId.set(currentMusicId, mediumTimeRangeDetails)
      }

      // Because the analysisLists are not be delivered by the medium listing endpoint we need to load all analysis lists to receive annotation information
      const mediumAnalysisList = await TIMAAT.AnalysisListService.getMediumAnalysisLists(medium.model.id)
      const annotationsList = mediumAnalysisList.flatMap(currentAnalysis => currentAnalysis.annotations)

      for (const currentAnnotation of annotationsList) {
        const currentStartTime = currentAnnotation.startTime
        const currentEndTime = currentAnnotation.endTime
        for (const currentAnnotationHasMusic of currentAnnotation.annotationHasMusic) {
          const currentMusicId = currentAnnotationHasMusic.music.id
          if (!mediumTimeRangeDetailsByMusicId.has(currentMusicId)) {
            const musicName = currentAnnotationHasMusic.music.displayTitle.name
            mediumTimeRangeDetailsByMusicId.set(currentMusicId, [])
            musicTitlesByMusicId.set(currentMusicId, musicName)
            musicIds.push(currentMusicId)
          }

          const currentMediumTimeRangeDetails = mediumTimeRangeDetailsByMusicId.get(currentMusicId)
          currentMediumTimeRangeDetails.push({
            type: "annotation",
            annotationId: currentAnnotation.id,
            startTime: currentStartTime,
            endTime: currentEndTime
          })
        }
      }
      for (const currentMusicId of musicIds) {
        const currentMusicTitle = musicTitlesByMusicId.get(currentMusicId)
        const currentMediumTimeRangeDetails = mediumTimeRangeDetailsByMusicId.get(currentMusicId)

        const mediumHasMusicFormData = this.appendMediumHasMusicDataset(currentMusicId, currentMusicTitle, currentMediumTimeRangeDetails, editMode);
        mediumFormMusicDynamicFields.append(mediumHasMusicFormData);

        let j = 0;
        for (; j < currentMediumTimeRangeDetails.length; j++) {
          const currentMediumTimeRangeDetail = currentMediumTimeRangeDetails[j];
          if (currentMediumTimeRangeDetail.startTime != null && !(isNaN(currentMediumTimeRangeDetail.startTime)))
            $('[data-role="startTime[' + currentMusicId + '][' + j + ']"]').val(TIMAAT.Util.formatTime(currentMediumTimeRangeDetail.startTime, true));
          else $('[data-role="startTime[' + currentMusicId + '][' + j + ']"]').val('00:00:00.000');
          if (currentMediumTimeRangeDetail.endTime != null && !(isNaN(currentMediumTimeRangeDetail.endTime)))
            $('[data-role="endTime[' + currentMusicId + '][' + j + ']"]').val(TIMAAT.Util.formatTime(currentMediumTimeRangeDetail.endTime, true));
          else $('[data-role="endTime[' + currentMusicId + '][' + j + ']"]').val('00:00:00.000');
        }
      }

      $('.mediumDatasetMusicMediumHasMusicListAnnotationIndicator').on("click", async (event) => {
        const annotationId = $(event.target).data("id")
        const mediumId = $('#mediumFormMetadata').data('medium').model.id

        await TIMAAT.VideoPlayer.initializeAnnotationModeWithAnnotationByIds(mediumId, annotationId);
      })

      if (action === "show") {
        $('#mediumFormMusicDynamicFields :input').prop('disabled', true);
        $('#mediumFormMusicSubmitButton').hide();
        $('#mediumFormMusicDismissButton').hide();
        $('[data-role="mediumDatasetRemoveMediumHasMusicDetail"]').hide()
        $('[data-role="musicNewMediumHasMusicFields"]').hide();
        $('.mediumFormMusicFormDivider').hide();
        $('[data-role="removeMediumHasMusicDetail"]').hide();
        $('.js-form-group__button').hide();
        $('#mediumMusicLabel').html("Music of medium");
        $('.mediumDataSheetFormEditButton').attr("disabled", false);
      } else if (action === "edit") {
        $('[data-role="mediumFormMusicNewMusicFields"]').append(this.appendNewMediumHasMusicFields());
        $('#mediumFormMusicSubmitButton').show();
        $('#mediumFormMusicDismissButton').show();
        $('[data-role="mediumDatasetRemoveMediumHasMusicDetail"]').show()
        $('[data-role="musicNewMediumHasMusicFields"]').show();
        $('.mediumFormMusicFormDivider').show();
        $('[data-role="removeMediumHasMusicDetail"]').show();
        $('.js-form-group__button').show();
        $('#mediumMusicLabel').html("Edit music of medium");
        $('.mediumDataSheetFormEditButton').attr("disabled", true);

        $('#musicSelectDropdown').select2({
          closeOnSelect: true,
          scrollAfterSelect: true,
          allowClear: true,
          ajax: {
            url: 'api/music/selectList/',
            type: 'GET',
            dataType: 'json',
            delay: 250,
            headers: {
              "Authorization": "Bearer " + TIMAAT.Service.token,
              "Content-Type": "application/json",
            },
            // additional parameters
            data: function (params) {
              // console.log("TCL: data: params", params);
              return {
                search: params.term,
              };
            },
            processResults: function (data, params) {
              // console.log("TCL: processResults: data", data);
              params.page = params.page || 1;
              return {
                results: data
              };
            },
            cache: false
          },
          minimumInputLength: 0,
        });
      }
    },
    appendMediumHasMusicDataset: function (musicId, musicTitle, mediumHasMusicListData, editMode) {
      let mediumHasMusicListFormData =
          `<div class="form-group" data-role="musicIsInMediumEntry" data-music-id=` + musicId + `>
				<div class="form-row">
					<div class="col-md-11">
						<fieldset>
							<legend>` + musicTitle + `</legend>
							<div class="form-row">
								<div class="hidden" aria-hidden="true">
									<input type="hidden"
												class="form-control form-control-sm"
												name="mediumId"
												value="` + musicId + `">
								</div>
								<div class="col-md-6">
									<input type="text" class="form-control form-control-sm mediumDatasetMusicMediumHasMusicMediumId"
													id="musicTitle` + musicId + `"
													name="musicTitle‚"
													value="` + musicTitle + `"
													data-role="musicId[` + musicId + `]"
													placeholder="Select music"
													aria-describedby="Music name"
													disabled
													required>
								</div>
								<div class="col-md-6">
									<div data-role="musicMediumHasMusicListDetailEntries">`;
      // append list of time details
      var j = 0;
      let containsAnnotationReferences = false
      for (; j < mediumHasMusicListData.length; j++) {
        containsAnnotationReferences = containsAnnotationReferences || mediumHasMusicListData[j].type === "annotation"
        mediumHasMusicListFormData += this.appendMediumHasMusicDetailFields(j, musicId, mediumHasMusicListData[j]);
      }
      mediumHasMusicListFormData +=
          `</div>
									<div class="form-group" data-role="newMusicIsInMediumDetailFields" data-detail-id="` + j + `">`;
      if (editMode) {
        mediumHasMusicListFormData += this.appendMediumHasMusicNewDetailFields();
      }


      const removeButtonContent = containsAnnotationReferences ? "" :
          `<div class="col-md-1 align-items--vertically">
                    <button type="button"
                            class="form-group__button js-form-group__button removeMusicFromMediumButton btn btn-danger"
                            data-role="remove">
                        <i class="fas fa-trash-alt"></i>
                    </button>
                </div>`
      mediumHasMusicListFormData +=
          `<!-- form sheet: one row for new medium has music list detail information that shall be added to the medium has music list -->
									</div>
								</div>
							</div>
						</fieldset>
					</div>
					${removeButtonContent}
				</div>
			</div>`;


      return mediumHasMusicListFormData;
    },

    /** adds empty fields for new mediumHasMusic dataset */
    appendNewMediumHasMusicFields: function () {
      // console.log("TCL: appendNewMediumHasMusicFields");
      let mediumHasMusicToAppend =
          `<div class="form-group" data-role="newMusicIsInMediumEntry" data-id=-1>
				<div class="form-row">
					<div class="col-md-11">
						<fieldset>
							<legend>Add new music:</legend>
							<div class="form-row">
								<div class="col-md-6">
									<label class="sr-only">Music appears in medium</label>
									<select class="form-control form-control-sm"
													id="musicSelectDropdown"
													name="musicId"
													data-role="musicId"
													data-placeholder="Select music"
													required>
									</select>
								</div>
								<div class="col-md-6">
									<div class="form-group"
											 data-role="newMusicIsInMediumDetailFields"
											 data-detail-id="0">`;
      mediumHasMusicToAppend += this.appendMediumHasMusicNewDetailFields();
      mediumHasMusicToAppend +=
          `</div>
								</div>
							</div>
						</fieldset>
					</div>
					<div class="col-md-1 align-items--vertically">
						<button type="button" class="form-group__button js-form-group__button addMusicToMediumButton btn btn-primary" data-role="add">
							<i class="fas fa-plus"></i>
						</button>
					</div>
				</div>
			</div>`;
      return mediumHasMusicToAppend;
    },

    appendMediumHasMusicNewDetailFields: function () {
      // console.log("TCL: appendMediumHasMusicNewDetailFields");
      let newMediumHasMusicDetail =
          `<div class="form-row">
				<div class="col-md-5">
					<label class="sr-only">Starts at</label>
					<input type="text"
								class="form-control disableOnSubmit form-control-sm mediumDatasetsMusicMediumHasMusicListStartTime"
								name="startTime"
								data-role="startTime"
								placeholder="00:00:00.000"
								value="00:00:00.000"
								aria-describedby="Music starts at">
				</div>
				<div class="col-md-5">
					<label class="sr-only">Ends at</label>
					<input type="text"
								class="form-control disableOnSubmit form-control-sm mediumDatasetsMusicMediumHasMusicListEndTime"
								name="endTime"
								data-role="endTime"
								placeholder="00:00:00.000"
								value="00:00:00.000"
								aria-describedby="Music ends at">
				</div>
				<div class="col-md-2 align-items--vertically">
					<button type="button" class="form-group__button js-form-group__button mediumDatasetAddMediumHasMusicDetailButton btn btn-primary" data-role="addMediumHasMusicDetail">
						<i class="fas fa-plus"></i>
					</button>
				</div>
			</div>`;
      return newMediumHasMusicDetail;
    },

    /** adds fields for details of mediumHasMusic data */
    appendMediumHasMusicDetailFields: function (j, musicId, mediumHasMusicData) {
      let mediumHasMusicDetailList =
          `<div class="form-group" data-role="mediumHasMusicDetailEntry" data-detail-id="` + j + `" data-type="${mediumHasMusicData.type}">
					<div class="form-row">
						<div class="col-md-5">
							<label class="sr-only">Starts at</label>
							<input type="text"
										 class="form-control form-control-sm mediumDatasetsMusicMediumHasMusicListStartTime"
										 name="startTime[` + musicId + `][` + j + `]"
										 data-role="startTime[` + musicId + `][` + j + `]"`;
      if (mediumHasMusicData != null) {
        mediumHasMusicDetailList += `value="` + mediumHasMusicData.startTime + `"`;
      }
      mediumHasMusicDetailList +=
          `placeholder="00:00:00.000"
										aria-describedby="Music starts at">
						</div>
						<div class="col-md-5">
							<label class="sr-only">Ends at</label>
							<input type="text"
										 class="form-control form-control-sm mediumDatasetsMusicMediumHasMusicListEndTime"
										 name="endTime[` + musicId + `][` + j + `]"
										 data-role="endTime[` + musicId + `][` + j + `]"`;
      if (mediumHasMusicData != null) {
        mediumHasMusicDetailList += `value="` + mediumHasMusicData.endTime + `"`;
      }
      mediumHasMusicDetailList +=
          `placeholder="00:00:00.000"
										aria-describedby="Music ends at">
						</div>
						<div class="col-md-2 align-items--vertically">`

      if (mediumHasMusicData.type === "annotation") {
        mediumHasMusicDetailList += `<i class="fas fa-draw-polygon fa-fw mediumDatasetMusicMediumHasMusicListAnnotationIndicator" data-id="${mediumHasMusicData.annotationId}" title="Open linked annotation"></i>`
      } else {
        mediumHasMusicDetailList += `<button type="button" class="btn btn-danger" data-role="mediumDatasetRemoveMediumHasMusicDetail">
								<i class="fas fa-trash-alt"></i>
							</button>`
      }

      mediumHasMusicDetailList += `</div>
					</div>
				</div>`;
      return mediumHasMusicDetailList;
    },

    mediumFormActorRoles: async function (action, medium) {
      // console.log("TCL: mediumFormActorRoles: action, medium", action, medium);
      // TIMAAT.UI.addSelectedClassToSelectedItem(medium.model.mediaType.mediaTypeTranslations[0].type, medium.model.id);
      var node = document.getElementById("mediumDynamicActorWithRoleFields");
      while (node.lastChild) {
        node.removeChild(node.lastChild);
      }
      ;
      var node = document.getElementById("mediumNewActorWithRoleFields");
      while (node.lastChild) {
        node.removeChild(node.lastChild);
      }
      ;
      $('#mediumFormActorWithRoles').trigger('reset');
      // mediumFormActorRolesValidator.resetForm();
      $('#mediumVideoPreview').get(0).pause();

      // setup UI
      // actor roles data
      var actorIdList = [];
      var i = 0;
      for (; i < medium.model.mediumHasActorWithRoles.length; i++) {
        if (actorIdList[actorIdList.length - 1] != medium.model.mediumHasActorWithRoles[i].actor.id) {
          actorIdList.push(medium.model.mediumHasActorWithRoles[i].actor.id);
        }
      }
      // console.log("TCL: actorIdList", actorIdList);

      // set up form content structure
      i = 0;
      for (; i < actorIdList.length; i++) {
        // console.log("TCL: mediumFormActorRoles:function -> actorIdList", actorIdList);
        $('[data-role="mediumDynamicActorWithRoleFields"]').append(this.appendActorWithRolesDataset(i, actorIdList[i]));

        // provide list of actors that already have a medium_has_actor_with_role entry, filter by role_group
        this.getMediumHasActorWithRoleData(actorIdList[i]);
        // select actor for each entry
        // await TIMAAT.MediumService.getActorList(medium.model.id).then(function (data) {
        await TIMAAT.ActorService.getActor(actorIdList[i]).then(function (data) {
          // console.log("TCL: actorIdList[i]", actorIdList[i]);
          var actorSelect = $('#mediumHasActorWithRoleActorId-' + actorIdList[i]);
          // console.log("TCL: actorSelect", actorSelect);
          // console.log("TCL: then: data", data);
          var option = new Option(data.displayName.name, data.id, true, true);
          actorSelect.append(option).trigger('change');
          // manually trigger the 'select2:select' event
          actorSelect.trigger({
            type: 'select2:select',
            params: {
              data: data
            }
          });
        });

        // url for role fetch needs to change on actor change
        // provide roles list for new selected actor
        this.getMediumHasActorWithRolesDropdownData(actorIdList[i]);

        var roleSelect = $('#mediumActorWithRolesMultiSelectDropdown-' + actorIdList[i]);
        // console.log("TCL: roleSelect", roleSelect);
        await TIMAAT.MediumService.getActorHasRoleList(medium.model.id, actorIdList[i]).then(function (data) {
          // console.log("TCL: then: data", data);
          if (data.length > 0) {
            data.sort((a, b) => (a.roleTranslations[0].name > b.roleTranslations[0].name) ? 1 : -1);
            // create the options and append to Select2
            var j = 0;
            for (; j < data.length; j++) {
              var option = new Option(data[j].roleTranslations[0].name, data[j].id, true, true);
              roleSelect.append(option).trigger('change');
            }
            // manually trigger the 'select2:select' event
            roleSelect.trigger({
              type: 'select2:select',
              params: {
                data: data
              }
            });
          }
        });
      }

      if (action == 'show') {
        $('#mediumFormActorWithRoles :input').prop('disabled', true);
        this.initFormForShow(medium.model);
        $('#mediumFormActorWithRolesSubmitButton').hide();
        $('#mediumFormActorWithRolesDismissButton').hide();
        $('[data-role="mediumNewActorWithRoleFields"]').hide();
        $('.actorWithRoleFormDivider').hide();
        // $('[data-role="remove"]').hide();
        // $('[data-role="add"]').hide();
        $('.js-form-group__button').hide();
        $('#mediumActorRolesLabel').html("Medium actor roles");
      } else if (action == 'edit') {
        $('#mediumFormActorWithRoles :input').prop('disabled', false);
        $('[id^="mediumHasActorWithRoleActorId-"').prop('disabled', true);
        this.hideFormButtons();
        $('#mediumFormActorWithRolesSubmitButton').html("Save");
        $('#mediumFormActorWithRolesSubmitButton').show();
        $('#mediumFormActorWithRolesDismissButton').show();
        $('#mediumActorRolesLabel').html("Edit medium actor roles");
        $('[data-role="mediumNewActorWithRoleFields"]').show();
        $('.actorWithRoleFormDivider').show();
        // $('#mediumDatasetsMetadataMediumActorWithRole').focus();

        // fields for new title entry
        $('[data-role="mediumNewActorWithRoleFields"]').append(this.appendNewActorHasRolesField());

        // provide list of actors that already have a medium_has_actor_with_role entry, filter by role_group
        $('#mediumHasActorWithRoleActorId').select2({
          closeOnSelect: true,
          scrollAfterSelect: true,
          allowClear: true,
          ajax: {
            url: 'api/actor/selectList/', // TODO limit list to actors that already have roles associations
            type: 'GET',
            dataType: 'json',
            delay: 250,
            headers: {
              "Authorization": "Bearer " + TIMAAT.Service.token,
              "Content-Type": "application/json",
            },
            // additional parameters
            data: function (params) {
              // console.log("TCL: data: params", params);
              return {
                search: params.term,
                page: params.page
              };
            },
            processResults: function (data, params) {
              // console.log("TCL: processResults: data", data);
              params.page = params.page || 1;
              return {
                results: data
              };
            },
            cache: false
          },
          minimumInputLength: 0,
        });

        // url for role fetch needs to change on actor change
        $('#mediumHasActorWithRoleActorId').on('change', function (event) {
          // console.log("TCL: actor selection changed");
          // console.log("TCL: selected Actor Id", $(this).val());
          if (!($(this).val() == null)) {
            $('#mediumActorWithRolesMultiSelectDropdown').val(null).trigger('change');
            // provide roles list for new selected actor
            $('#mediumActorWithRolesMultiSelectDropdown').select2({
              closeOnSelect: false,
              scrollAfterSelect: true,
              allowClear: true,
              ajax: {
                url: 'api/medium/hasActor/' + $(this).val() + '/withRoles/selectList',
                type: 'GET',
                dataType: 'json',
                delay: 250,
                headers: {
                  "Authorization": "Bearer " + TIMAAT.Service.token,
                  "Content-Type": "application/json",
                },
                // additional parameters
                data: function (params) {
                  // console.log("TCL: data: params", params);
                  return {
                    search: params.term,
                    page: params.page
                  };
                },
                processResults: function (data, params) {
                  // console.log("TCL: processResults: data", data);
                  params.page = params.page || 1;
                  return {
                    results: data
                  };
                },
                cache: false
              },
              minimumInputLength: 0,
            });
          }
        });

        $('#mediumFormActorWithRoles').data('medium', medium);
      }
    },

    createMedium: async function (mediumSubtype, mediumModel, mediumSubtypeModel, title, source) {
      // console.log("TCL: createMedium: mediumSubtype, mediumModel, mediumSubtypeModel, title, source", mediumSubtype, mediumModel, mediumSubtypeModel, title, source);
      try { // TODO needs to be called after createMedium once m-n-table is refactored to 1-n table (sure?)
        // create display title
        var newDisplayTitle = await TIMAAT.MediumService.createTitle(title);
      } catch (error) {
        console.error("ERROR: ", error);
      }
      ;

      try {
        // create medium
        var tempMediumModel = mediumModel;
        tempMediumModel.displayTitle = newDisplayTitle;
        tempMediumModel.originalTitle = newDisplayTitle;
        tempMediumModel.source = source;
        var newMediumModel = await TIMAAT.MediumService.createMedium(tempMediumModel);
      } catch (error) {
        console.error("ERROR: ", error);
      }
      ;

      try {
        // update source (createMedium created an empty source)
        source.id = newMediumModel.sources[0].id;
        var updatedSource = await TIMAAT.MediumService.updateSource(source);
        newMediumModel.sources[0] = updatedSource; // TODO refactor once several sources can be added
      } catch (error) {
        console.error("ERROR: ", error);
      }
      ;

      try {
        // create mediumSubtype with medium id
        mediumSubtypeModel.mediumId = newMediumModel.id;
        var newMediumSubtypeModel = await TIMAAT.MediumService.createMediumSubtype(mediumSubtype, newMediumModel, mediumSubtypeModel);
        // console.log("TCL: newMediumSubtypeModel", newMediumSubtypeModel);
      } catch (error) {
        console.error("ERROR: ", error);
      }
      ;

      try {
        // push new medium to dataset model
        // console.log("TCL: newMediumModel", newMediumModel);
        switch (mediumSubtype) {
          case 'audio':
            newMediumModel.mediumAudio = mediumSubtypeModel;
            break;
          case 'document':
            newMediumModel.mediumDocument = mediumSubtypeModel;
            break;
          case 'image':
            newMediumModel.mediumImage = mediumSubtypeModel;
            break;
          case 'software':
            newMediumModel.mediumSoftware = mediumSubtypeModel;
            break;
          case 'text':
            newMediumModel.mediumText = mediumSubtypeModel;
            break;
          case 'video':
            newMediumModel.mediumVideo = mediumSubtypeModel;
            break;
          case 'videogame':
            newMediumModel.mediumVideogame = mediumSubtypeModel;
            break;
        }
        ;
        // console.log("TCL: newMediumModel", newMediumModel);
      } catch (error) {
        console.error("ERROR: ", error);
      }
      ;
      return (newMediumModel);
    },

    createTitle: async function (titleModel) {
      // console.log("TCL: createTitle: async function -> titleModel", titleModel);
      try {
        // create title
        var newTitleModel = await TIMAAT.MediumService.createTitle(titleModel.model);
        // console.log("TCL: newTitleModel", newTitleModel);
      } catch (error) {
        console.error("ERROR: ", error);
      }
      ;
    },

    addTitles: async function (medium, newTitles) {
      // console.log("TCL: addTitles: async function -> medium, newTitles", medium, newTitles);
      try {
        // create title
        var i = 0;
        for (; i < newTitles.length; i++) {
          // var newTitle = await TIMAAT.MediumService.createTitle(newTitles[i]);
          var addedTitleModel = await TIMAAT.MediumService.addTitle(medium.model.id, newTitles[i]);
          medium.model.titles.push(addedTitleModel);
        }
        // await this.updateMedium(type, medium);
      } catch (error) {
        console.error("ERROR: ", error);
      }
      ;
    },

    addLanguageTrack: async function (medium, newLanguageTrack) {
      // console.log("TCL: addLanguageTrack: async function -> newLanguageTrack", newLanguageTrack);
      try {
        var addedLanguageTrackModel = await TIMAAT.MediumService.addLanguageTrack(newLanguageTrack);
        addedLanguageTrackModel.id = newLanguageTrack;
        medium.model.mediumHasLanguages.push(addedLanguageTrackModel);
      } catch (error) {
        console.error("ERROR: ", error);
      }
      ;
    },

    setMediumStatus: function (medium) {
      if (!medium || !medium.ui) return;
      // clear ui status
      medium.ui.find('.js-medium-file-status').hide();
      medium.ui.find('.js-medium-file-status__icon').removeClass('fa-cog');
      medium.ui.find('.js-medium-file-status__icon').removeClass('fa-hourglass-half');
      medium.ui.find('.js-medium-file-status__icon').addClass('fa-cog');
      medium.ui.find('.js-medium-file-status--transcoding').hide();

      if (medium.fileStatus == 'unavailable' || medium.fileStatus == 'ready')
        window.clearInterval(medium.poll);

      if (medium.fileStatus == 'unavailable') {
        medium.ui.find('.js-medium-file-status--transcoding').html('<i class="medium-file-status__icon--transcoding fas fa-eye-slash"></i> not available');
        medium.ui.find('.js-medium-file-status--transcoding').show();
      }

      if (medium.fileStatus != 'ready' && medium.fileStatus != 'noFile') medium.ui.find('.js-medium-file-status').show();
      if (medium.fileStatus == 'waiting') medium.ui.find('.js-medium-file-status__icon').removeClass('fa-cog').addClass('fa-hourglass-half');
      if (medium.fileStatus == 'noFile') {
        medium.ui.find('.mediumAnnotate').hide(); // TODO wrong class?

        // user selected file, trigger form submit / upload
        medium.ui.find('.mediumUploadFile').off('change').on('change', function (ev) {
          let fileList = medium.ui.find('.mediumUploadFile')[0].files;
          if (fileList.length > 0) TIMAAT.UploadManager.queueUpload(medium, medium.ui.find('form'));
        });
      }
    },

    updateMedium: async function (mediumSubtype, medium) {
      // console.log("TCL: updateMedium: async function -> medium at beginning of update process: ", mediumSubtype, medium);
      try { // update display title
        var tempDisplayTitle = await TIMAAT.MediumService.updateTitle(medium.model.displayTitle);
        // console.log("tempDisplayTitle", tempDisplayTitle);
        medium.model.displayTitle = tempDisplayTitle;
      } catch (error) {
        console.error("ERROR: ", error);
      }
      ;

      try { // update original title
        if (medium.model.originalTitle) { // medium initially has no original title set
          // for changes in data sheet form that impact data in original title
          if (medium.model.displayTitle.id == medium.model.originalTitle.id) {
            medium.model.originalTitle = medium.model.displayTitle;
          }
          var tempOriginalTitle = await TIMAAT.MediumService.updateTitle(medium.model.originalTitle);
          // console.log("tempOriginalTitle", tempOriginalTitle);
          medium.model.originalTitle = tempOriginalTitle;
        }
      } catch (error) {
        console.error("ERROR: ", error);
      }
      ;

      try { // update source
        var tempSource = await TIMAAT.MediumService.updateSource(medium.model.sources[0]);
        medium.model.sources[0] = tempSource;
      } catch (error) {
        console.error("ERROR: ", error);
      }
      ;

      try { // update subtype
        var tempSubtypeModel;
        switch (mediumSubtype) {
          case 'audio':
            tempSubtypeModel = medium.model.mediumAudio;
            tempSubtypeModel.audioPostProduction.audioPostProductionTranslations[0] = await TIMAAT.MediumService.updateAudioPostProductionTranslation(medium.model.mediumAudio.audioPostProduction.audioPostProductionTranslations[0]); // TODO multi-language
            break;
          case 'document':
            tempSubtypeModel = medium.model.mediumDocument;
            break;
          case 'image':
            tempSubtypeModel = medium.model.mediumImage;
            break;
          case 'software':
            tempSubtypeModel = medium.model.mediumSoftware;
            break;
          case 'text':
            tempSubtypeModel = medium.model.mediumText;
            break;
          case 'video':
            tempSubtypeModel = medium.model.mediumVideo;
            break;
          case 'videogame':
            tempSubtypeModel = medium.model.mediumVideogame;
            break;
        }
        var tempMediumSubtypeModel = await TIMAAT.MediumService.updateMediumSubtype(mediumSubtype, tempSubtypeModel);
      } catch (error) {
        console.error("ERROR: ", error);
      }
      ;

      try { // update medium
        var tempMediumModel = await TIMAAT.MediumService.updateMedium(medium.model);
        // console.log("TCL: updateMedium:function -> tempMediumModel", tempMediumModel);
      } catch (error) {
        console.error("ERROR: ", error);
      }
      ;

      // try { // update media lists
      // 	await this._mediumUpdated(mediumSubtype, medium);
      // } catch(error) {
      // 	console.error("ERROR: ", error);
      // };
      return tempMediumModel;
      // medium.updateUI();
    },

    updateTitle: async function (title, medium) {
      // console.log("TCL: updateTitle: async function -> title at beginning of update process: ", title, medium);
      try {
        // update title
        var tempTitle = await TIMAAT.MediumService.updateTitle(title);
        var i = 0;
        for (; i < medium.model.titles.length; i++) {
          if (medium.model.titles[i].id == title.id)
            medium.model.titles[i] = tempTitle;
        }

        // update data that is part of medium (includes updating last edited by/at)
        // console.log("TCL: updateMedium: async function - medium.model", medium.model);
        // var tempMediumModel = await TIMAAT.MediumService.updateMedium(medium.model);
      } catch (error) {
        console.error("ERROR: ", error);
      }
      ;
    },

    updateMediumHasCategorySetsList: async function (mediumModel, categorySetIdList) {
      // console.log("TCL: mediumModel, categorySetIdList", mediumModel, categorySetIdList);
      try {
        var existingMediumHasCategorySetsEntries = await TIMAAT.MediumService.getCategorySetList(mediumModel.id);
        // console.log("TCL: existingMediumHasCategorySetsEntries", existingMediumHasCategorySetsEntries);
        if (categorySetIdList == null) { //* all entries will be deleted
          mediumModel.categorySets = [];
          await TIMAAT.MediumService.updateMedium(mediumModel);
        } else if (existingMediumHasCategorySetsEntries.length == 0) { //* all entries will be added
          mediumModel.categorySets = categorySetIdList;
          await TIMAAT.MediumService.updateMedium(mediumModel);
        } else { //* delete removed entries
          var entriesToDelete = [];
          var i = 0;
          for (; i < existingMediumHasCategorySetsEntries.length; i++) {
            var deleteId = true;
            var j = 0;
            for (; j < categorySetIdList.length; j++) {
              if (existingMediumHasCategorySetsEntries[i].id == categorySetIdList[j].id) {
                deleteId = false;
                break; // no need to check further if match was found
              }
            }
            if (deleteId) { // id is in existingMediumHasCategorySetEntries but not in categorySetIdList
              // console.log("TCL: deleteId", deleteId);
              entriesToDelete.push(existingMediumHasCategorySetsEntries[i]);
              existingMediumHasCategorySetsEntries.splice(i, 1); // remove entry so it won't have to be checked again in the next step when adding new ids
              i--; // so the next list item is not jumped over due to the splicing
            }
          }
          if (entriesToDelete.length > 0) { // anything to delete?
            var i = 0;
            for (; i < entriesToDelete.length; i++) {
              var index = mediumModel.categorySets.findIndex(({id}) => id === entriesToDelete[i].id);
              mediumModel.categorySets.splice(index, 1);
              await TIMAAT.MediumService.removeCategorySet(mediumModel.id, entriesToDelete[i].id);
            }
          }
          //* add existing categorySets
          var idsToCreate = [];
          i = 0;
          for (; i < categorySetIdList.length; i++) {
            var idExists = false;
            var item = {id: 0};
            var j = 0;
            for (; j < existingMediumHasCategorySetsEntries.length; j++) {
              if (categorySetIdList[i].id == existingMediumHasCategorySetsEntries[j].id) {
                idExists = true;
                break; // no need to check further if match was found
              }
            }
            if (!idExists) {
              item.id = categorySetIdList[i].id;
              idsToCreate.push(item);
            }
          }
          // console.log("TCL: idsToCreate", idsToCreate);
          if (idsToCreate.length > 0) { // anything to add?
            // console.log("TCL: idsToCreate", idsToCreate);
            var i = 0;
            for (; i < idsToCreate.length; i++) {
              mediumModel.categorySets.push(idsToCreate[i]);
              await TIMAAT.MediumService.addCategorySet(mediumModel.id, idsToCreate[i].id);
            }
          }
        }
      } catch (error) {
        console.error("ERROR: ", error);
      }
      return mediumModel;
    },

    addCategorySetsToMedium: async function (mediumModel, newCategorySetList) {
      // console.log("TCL: addCategorySetsToMedium:function -> mediumModel, newCategorySetList", mediumModel, newCategorySetList);
      var i = 0;
      for (; i < newCategorySetList.length; i++) {
        await TIMAAT.MediumService.addCategorySet(mediumModel.id, newCategorySetList[i].id);
        mediumModel.categorySets.push(newCategorySetList[i]);
      }
      return mediumModel;
    },

    updateMediumHasCategoriesList: async function (mediumModel, categoryIdList) {
      // console.log("TCL: mediumModel, categoryIdList", mediumModel, categoryIdList);
      try {
        var existingMediumHasCategoriesEntries = await TIMAAT.MediumService.getSelectedCategories(mediumModel.id);
        // console.log("TCL: existingMediumHasCategoriesEntries", existingMediumHasCategoriesEntries);
        if (categoryIdList == null) { //* all entries will be deleted
          mediumModel.categories = [];
          await TIMAAT.MediumService.updateMedium(mediumModel);
        } else if (existingMediumHasCategoriesEntries.length == 0) { //* all entries will be added
          mediumModel.categories = categoryIdList;
          await TIMAAT.MediumService.updateMedium(mediumModel);
        } else { //* delete removed entries
          var entriesToDelete = [];
          var i = 0;
          for (; i < existingMediumHasCategoriesEntries.length; i++) {
            var deleteId = true;
            var j = 0;
            for (; j < categoryIdList.length; j++) {
              if (existingMediumHasCategoriesEntries[i].id == categoryIdList[j].id) {
                deleteId = false;
                break; // no need to check further if match was found
              }
            }
            if (deleteId) { // id is in existingMediumHasCategoryEntries but not in categoryIdList
              // console.log("TCL: deleteId", deleteId);
              entriesToDelete.push(existingMediumHasCategoriesEntries[i]);
              existingMediumHasCategoriesEntries.splice(i, 1); // remove entry so it won't have to be checked again in the next step when adding new ids
              i--; // so the next list item is not jumped over due to the splicing
            }
          }
          if (entriesToDelete.length > 0) { // anything to delete?
            var i = 0;
            for (; i < entriesToDelete.length; i++) {
              var index = mediumModel.categories.findIndex(({id}) => id === entriesToDelete[i].id);
              mediumModel.categories.splice(index, 1);
              await TIMAAT.MediumService.removeCategory(mediumModel.id, entriesToDelete[i].id);
            }
          }
          //* add existing categories
          var idsToCreate = [];
          i = 0;
          for (; i < categoryIdList.length; i++) {
            var idExists = false;
            var item = {id: 0};
            var j = 0;
            for (; j < existingMediumHasCategoriesEntries.length; j++) {
              if (categoryIdList[i].id == existingMediumHasCategoriesEntries[j].id) {
                idExists = true;
                break; // no need to check further if match was found
              }
            }
            if (!idExists) {
              item.id = categoryIdList[i].id;
              idsToCreate.push(item);
            }
          }
          // console.log("TCL: idsToCreate", idsToCreate);
          if (idsToCreate.length > 0) { // anything to add?
            // console.log("TCL: idsToCreate", idsToCreate);
            var i = 0;
            for (; i < idsToCreate.length; i++) {
              mediumModel.categories.push(idsToCreate[i]);
              await TIMAAT.MediumService.addCategory(mediumModel.id, idsToCreate[i].id);
            }
          }
        }
      } catch (error) {
        console.error("ERROR: ", error);
      }
      return mediumModel;
    },

    addCategoriesToMedium: async function (mediumModel, newCategoryList) {
      // console.log("TCL: addCategoriesToMedium:function -> mediumModel, newCategoryList", mediumModel, newCategoryList);
      var i = 0;
      for (; i < newCategoryList.length; i++) {
        await TIMAAT.MediumService.addCategory(mediumModel.id, newCategoryList[i].id);
        mediumModel.categories.push(newCategoryList[i]);
      }
      return mediumModel;
    },

    updateMediumHasTagsList: async function (mediumModel, tagIdList) {
      // console.log("TCL: mediumModel, tagIdList", mediumModel, tagIdList);
      try {
        var existingMediumHasTagsEntries = await TIMAAT.MediumService.getTagList(mediumModel.id);
        // console.log("TCL: existingMediumHasTagsEntries", existingMediumHasTagsEntries);
        if (tagIdList == null) { //* all entries will be deleted
          mediumModel.tags = [];
          await TIMAAT.MediumService.updateMedium(mediumModel);
        } else if (existingMediumHasTagsEntries.length == 0) { //* all entries will be added
          mediumModel.tags = tagIdList;
          await TIMAAT.MediumService.updateMedium(mediumModel);
        } else { //* delete removed entries
          var entriesToDelete = [];
          var i = 0;
          for (; i < existingMediumHasTagsEntries.length; i++) {
            var deleteId = true;
            var j = 0;
            for (; j < tagIdList.length; j++) {
              if (existingMediumHasTagsEntries[i].id == tagIdList[j].id) {
                deleteId = false;
                break; // no need to check further if match was found
              }
            }
            if (deleteId) { // id is in existingMediumHasTagEntries but not in tagIdList
              // console.log("TCL: deleteId", deleteId);
              entriesToDelete.push(existingMediumHasTagsEntries[i]);
              existingMediumHasTagsEntries.splice(i, 1); // remove entry so it won't have to be checked again in the next step when adding new ids
              i--; // so the next list item is not jumped over due to the splicing
            }
          }
          if (entriesToDelete.length > 0) { // anything to delete?
            var i = 0;
            for (; i < entriesToDelete.length; i++) {
              var index = mediumModel.tags.findIndex(({id}) => id === entriesToDelete[i].id);
              mediumModel.tags.splice(index, 1);
              await TIMAAT.MediumService.removeTag(mediumModel.id, entriesToDelete[i].id);
            }
          }
          //* add existing tags
          var idsToCreate = [];
          i = 0;
          for (; i < tagIdList.length; i++) {
            var idExists = false;
            var item = {id: 0};
            var j = 0;
            for (; j < existingMediumHasTagsEntries.length; j++) {
              if (tagIdList[i].id == existingMediumHasTagsEntries[j].id) {
                idExists = true;
                break; // no need to check further if match was found
              }
            }
            if (!idExists) {
              item.id = tagIdList[i].id;
              idsToCreate.push(item);
            }
          }
          // console.log("TCL: idsToCreate", idsToCreate);
          if (idsToCreate.length > 0) { // anything to add?
            // console.log("TCL: idsToCreate", idsToCreate);
            var i = 0;
            for (; i < idsToCreate.length; i++) {
              mediumModel.tags.push(idsToCreate[i]);
              await TIMAAT.MediumService.addTag(mediumModel.id, idsToCreate[i].id);
            }
          }
        }
      } catch (error) {
        console.error("ERROR: ", error);
      }
      return mediumModel;
    },

    createNewTagsAndAddToMedium: async function (mediumModel, newTagList) {
      // console.log("TCL: createNewTagsAndAddToMedium:function -> mediumModel, newTagList", mediumModel, newTagList);
      var i = 0;
      for (; i < newTagList.length; i++) {
        newTagList[i] = await TIMAAT.Service.createTag(newTagList[i].name);
        await TIMAAT.MediumService.addTag(mediumModel.id, newTagList[i].id);
        mediumModel.tags.push(newTagList[i]);
      }
      return mediumModel;
    },

    _mediumRemoved: async function (medium) {
      // console.log("TCL: _mediumRemoved", medium);
      // sync to server
      try {
        if (medium.model.mediumAudio && medium.model.mediumAudio.audioPostProduction) await TIMAAT.MediumService.deleteAudioPostProduction(medium.model.mediumAudio.audioPostProduction.id);
        await TIMAAT.MediumService.removeMedium(medium);
      } catch (error) {
        console.error("ERROR: ", error);
      }

      // remove all titles from medium
      var i = 0;
      for (; i < medium.model.titles.length; i++) { // remove obsolete titles
        if (medium.model.titles[i].id != medium.model.displayTitle.id) {
          TIMAAT.MediumService.removeTitle(medium.model.titles[i]);
          medium.model.titles.splice(i, 1);
        }
      }
      $('#mediumFormMetadata').data('medium', null);
    },

    updateMediumModelData: async function (model, formDataObject) {
      // console.log("TCL: updateMediumModelData: model, formDataObject", model, formDataObject);
      // medium data
      model.releaseDate = formDataObject.releaseDate;
      model.recordingStartDate = formDataObject.recordingStartDate;
      model.recordingEndDate = formDataObject.recordingEndDate;
      model.copyright = formDataObject.copyright;
      model.remark = formDataObject.remark;
      // display-title data
      model.displayTitle.name = formDataObject.displayTitle;
      model.displayTitle.language.id = formDataObject.displayTitleLanguageId;
      var i = 0;
      for (; i < model.titles.length; i++) {
        if (model.displayTitle.id == model.titles[i].id) {
          model.titles[i] = model.displayTitle;
          break;
        }
      }
      // source data
      model.sources[0].url = formDataObject.sourceUrl;
      model.sources[0].isPrimarySource = formDataObject.sourceIsPrimarySource;
      model.sources[0].lastAccessed = formDataObject.sourceLastAccessed;
      model.sources[0].isStillAvailable = formDataObject.sourceIsStillAvailable;

      return model;
    },

    createMediumModel: async function (formDataObject, type) {
      // console.log("TCL: formDataObject, type", formDataObject, type);
      let typeId = 0;
      switch (type) {
        case 'audio':
          typeId = 1;
          break;
        case 'document':
          typeId = 2;
          break;
        case 'image':
          typeId = 3;
          break;
        case 'software':
          typeId = 4;
          break;
        case 'text':
          typeId = 5;
          break;
        case 'video':
          typeId = 6;
          break;
        case 'videogame':
          typeId = 7;
          break;
      }
      var model = {
        id: 0,
        remark: formDataObject.remark,
        copyright: formDataObject.copyright,
        releaseDate: formDataObject.releaseDate,
        recordingStartDate: formDataObject.recordingStartDate,
        recordingEndDate: formDataObject.recordingEndDate,
        mediaType: {
          id: typeId,
        },
        titles: [{
          id: 0,
          language: {
            id: formDataObject.displayTitleLanguageId,
          },
          name: formDataObject.displayTitle,
        }],
      };
      return model;
    },

    createMediumSubtypeModel: async function (formDataObject, mediaType) {
      var model = {};
      switch (mediaType) {
        case 'audio':
          model = {
            mediumId: 0,
            audioPostProduction: {
              id: 0,
              audioPostProductionTranslations: [{
                id: 0,
              }],
            },
            length: formDataObject.length,
          };
          break;
        case 'document':
          model = {
            mediumId: 0,
          };
          break;
        case 'image':
          model = {
            mediumId: 0,
            width: formDataObject.width,
            height: formDataObject.height,
            bitDepth: 0, // TODO bit depth neither implemented nor displayed in UI
          };
          break;
        case 'software':
          model = {
            mediumId: 0,
            version: formDataObject.version,
          };
          break;
        case 'text':
          model = {
            mediumId: 0,
            content: formDataObject.content,
          };
          break;
        case 'video':
          model = {
            mediumId: 0,
            length: formDataObject.length,
            videoCodec: "", // TODO video codec neither implemented nor displayed in UI
            width: formDataObject.width,
            height: formDataObject.height,
            frameRate: formDataObject.frameRate,
            // dataRate: formDataObject.dataRate,
            // totalBitrate: formDataObject.totalBitrate,
            isEpisode: formDataObject.isEpisode,
          };
          break;
        case 'videogame':
          model = {
            mediumId: 0,
            isEpisode: formDataObject.isEpisode,
          };
          break;
      }
      return model;
    },

    createDisplayTitleModel: async function (formDataObject) {
      var model = {
        id: 0,
        language: {
          id: formDataObject.displayTitleLanguageId,
        },
        name: formDataObject.displayTitle,
      };
      return model;
    },

    createSourceModel: async function (formDataObject) {
      // console.log("TCL: formDataObject", formDataObject);
      var model = {
        id: 0,
        medium: {
          id: 0,
        },
        isPrimarySource: formDataObject.sourceIsPrimarySource,
        url: formDataObject.sourceUrl,
        lastAccessed: formDataObject.sourceLastAccessed,
        isStillAvailable: formDataObject.sourceIsStillAvailable,
      };
      return model;
    },

    createAudioPostProductionTranslationModel: async function (formDataObject) {
      let model = {
        id: 0,
        audioPostProduction: {
          id: 0
        },
        language: {
          id: 1 // TODO
        },
        overdubbing: formDataObject.overdubbing,
        reverb: formDataObject.reverb,
        delay: formDataObject.delay,
        panning: formDataObject.panning,
        bass: formDataObject.bass,
        treble: formDataObject.treble
      };
      return model;
    },

    titleFormTitleToAppend: function () {
      var titleToAppend =
          `<div class="form-group" data-role="titleEntry">
				<div class="form-row">
					<div class="col-md-2 text-center">
						<div class="form-check">
							<span>Add new title:</span>
						</div>
					</div>
					<div class="col-md-6">
						<label class="sr-only">Title</label>
						<input class="form-control form-control-sm mediumDatasetsMediumTitlesTitleName"
									 name="title"
									 data-role="title"
									 placeholder="[Enter title]"
									 aria-describedby="Title"
									 minlength="3"
									 maxlength="200"
									 rows="1"
									 required>
					</div>
					<div class="col-md-3">
						<label class="sr-only">Title's Language</label>
						<select class="form-control form-control-sm mediumDatasetsMediumTitlesTitleLanguageId"
										id="mediumTitleLanguageSelectDropdown"
										name="titleLanguageId"
										data-role="titleLanguageId"
										data-placeholder="Select title language"
										required>
						</select>
					</div>
					<div class="col-md-1 text-center">
						<button class="form-group__button form-group__button--add js-form-group__button addTitleButton btn btn-primary" data-role="add">
							<i class="fas fa-plus"></i>
						</button>
					</div>
				</div>
			</div>`;
      return titleToAppend;
    },

    appendActorWithRolesDataset: function (i, actorId) {
      // console.log("TCL: appendActorWithRolesDataset (i, actorId): ", i, actorId);
      var entryToAppend =
          `<div class="form-group" data-role="mediumHasActorWithRoleEntry" data-id="` + i + `" data-actor-id=` + actorId + `>
					<div class="form-row">
						<div class="col-md-11">
							<div class="form-row">
								<div class="col-md-4">
									<label class="sr-only">Actor</label>
									<select class="form-control form-control-sm mediumHasActorWithRoleActorId"
													id="mediumHasActorWithRoleActorId-` + actorId + `"
													name="actorId"
													data-placeholder="Select actor"
													data-role="actorId-` + actorId + `"
													required>
									</select>
								</div>
								<div class="col-md-8">
									<label class="sr-only">Has Role(s)</label>
									<select class="form-control form-control-sm"
													id="mediumActorWithRolesMultiSelectDropdown-` + actorId + `"
													name="roleId"
													data-placeholder="Select role(s)"
													data-role="actorWithRoles-` + actorId + `"
													multiple="multiple"
													required>
									</select>
								</div>
							</div>
						</div>
						<div class="col-md-1 text-center">
							<button class="form-group__button js-form-group__button removeActorWithRoleButton btn btn-danger" data-role="remove">
								<i class="fas fa-trash-alt"></i>
							</button>
						</div>
					</div>
				</div>`;
      return entryToAppend;
    },

    appendNewActorHasRolesField: function () {
      let entryToAppend =
          `<div class="form-group" data-role="mediumHasActorWithRoleEntry" data-id="-1">
					<div class="form-row">
						<div class="col-md-11">
							<fieldset>
								<legend>Add new Actor with role(s):</legend>
								<div class="form-row">
									<div class="col-md-4">
										<label class="sr-only">Actor</label>
										<select class="form-control form-control-sm mediumHasActorWithRoleActorId"
														id="mediumHasActorWithRoleActorId"
														name="actorId"
														data-placeholder="Select actor"
														data-role="actorId"
														required>
										</select>
									</div>
									<div class="col-md-8">
										<label class="sr-only">Has Role(s)</label>
										<select class="form-control form-control-sm"
														id="mediumActorWithRolesMultiSelectDropdown"
														name="roleId"
														data-placeholder="Select role(s)"
														multiple="multiple"
														readonly="true"
														required>
										</select>
									</div>
								</div>
							</fieldset>
						</div>
						<div class="col-md-1 align-items--vertically">
							<button type="button" class="form-group__button js-form-group__button addActorHasRolesButton btn btn-primary" data-role="add">
								<i class="fas fa-plus"></i>
							</button>
						</div>
					</div>
				</div>`;
      return entryToAppend;
    },

    initFormDataSheetForEdit: function () {
      $('#mediumDatasetsMetadataMediumReleaseDate').datetimepicker({
        timepicker: false,
        changeMonth: true,
        changeYear: true,
        scrollInput: false,
        format: 'YYYY-MM-DD',
        yearStart: 1900,
        yearEnd: new Date().getFullYear()
      });
      $('#mediumDatasetsMetadataMediumRecordingStartDate').datetimepicker({
        timepicker: false,
        changeMonth: true,
        changeYear: true,
        scrollInput: false,
        format: 'YYYY-MM-DD',
        yearStart: 1900,
        yearEnd: new Date().getFullYear()
      });
      $('#mediumDatasetsMetadataMediumRecordingEndDate').datetimepicker({
        timepicker: false,
        changeMonth: true,
        changeYear: true,
        scrollInput: false,
        format: 'YYYY-MM-DD',
        yearStart: 1900,
        yearEnd: new Date().getFullYear()
      });
      $('#mediumDatasetsMetadataMediumSourceLastAccessed').datetimepicker({
        format: 'YYYY-MM-DD HH:mm',
        yearStart: 1900,
        yearEnd: new Date().getFullYear()
      });
      $('#mediumFormMetadata :input').prop('disabled', false);
      this.hideFormButtons();
      $('#mediumFormMetadataSubmitButton').show();
      $('#mediumFormMetadataDismissButton').show();
      $('#mediumDatasetsMetadataMediumTitle').focus();
    },

    initFormForShow: function (model) {
      $('.mediumDataSheetFormEditButton').prop('disabled', false);
      $('.mediumDataSheetFormEditButton :input').prop('disabled', false);
      $('.mediumDataSheetFormEditButton').show();
      $('.uploadMediumButton').hide();
      $('.uploadMediumButton').prop('disabled', true);
      if (model.fileStatus == 'noFile' || !model.fileStatus) {
        if (model.mediumVideo || model.mediumImage || model.mediumAudio) {
          $('.uploadMediumButton').prop('disabled', false);
          $('.uploadMediumButton').show();
        }
        $('.mediumDataSheetFormAnnotateButton').hide();
        $('.mediumDataSheetFormAnnotateButton').prop('disabled', true);
      } else {
        if (model.mediaType.mediaTypeTranslations[0].type == 'video' || model.mediaType.mediaTypeTranslations[0].type == 'image' || model.mediaType.mediaTypeTranslations[0].type == 'audio') {
          $('.mediumDataSheetFormAnnotateButton').prop('disabled', false);
          $('.mediumDataSheetFormAnnotateButton').show();
        } else {
          $('.mediumDataSheetFormAnnotateButton').hide();
          $('.mediumDataSheetFormAnnotateButton').prop('disabled', true);
        }
      }
      if (this.container == 'videoPlayer') {
        $('.mediumDataSheetFormAnnotateButton').hide();
        $('.mediumDataSheetFormAnnotateButton').prop('disabled', true);
      }
    },

    initFormDataSheetData: function (type) {
      $('.dataSheetData').hide();
      $('.titleData').show();
      $('.mediumData').show();
      $('.sourceData').show();
      $('.' + type + 'Data').show();
    },

    hideFormButtons: function () {
      $('.formButtons').hide();
      $('.formButtons').prop('disabled', true);
      $('.formButtons :input').prop('disabled', true);
    },

    disableReadOnlyDataFields: function (disabled) {
      $('#mediumDatasetsMetadataAudioLength').prop('disabled', disabled);
      $('#mediumDatasetsMetadataAudioCodec').prop('disabled', disabled);
      $('#mediumDatasetsMetadataImageWidth').prop('disabled', disabled);
      $('#mediumDatasetsMetadataImageHeight').prop('disabled', disabled);
      // $('#mediumDatasetsMetadataImageBitDepth').prop('disabled', disabled);
      $('#mediumDatasetsMetadataVideoLength').prop('disabled', disabled);
      // $('#mediumDatasetsMetadataVideoVideoCodec').prop('disabled', disabled);
      $('#mediumDatasetsMetadataVideoWidth').prop('disabled', disabled);
      $('#mediumDatasetsMetadataVideoHeight').prop('disabled', disabled);
      $('#mediumDatasetsMetadataVideoFrameRate').prop('disabled', disabled);
      // $('#mediumDatasetsMetadataVideoDataRate').prop('disabled', disabled);
      // $('#mediumDatasetsMetadataVideoTotalBitrate').prop('disabled', disabled);
    },

    showAddMediumButton: function () {
      $('.addMediumButton').prop('disabled', false);
      $('.addMediumButton :input').prop('disabled', false);
      $('.addMediumButton').show();
    },

    getMediumFormTitleLanguageDropdownData: function () {
      $('#mediumDisplayTitleLanguageSelectDropdown').select2({
        closeOnSelect: true,
        scrollAfterSelect: true,
        allowClear: true,
        ajax: {
          url: 'api/language/selectList/',
          type: 'GET',
          dataType: 'json',
          delay: 250,
          headers: {
            "Authorization": "Bearer " + TIMAAT.Service.token,
            "Content-Type": "application/json",
          },
          // additional parameters
          data: function (params) {
            // console.log("TCL: data: params", params);
            return {
              search: params.term,
              page: params.page
            };
          },
          processResults: function (data, params) {
            // console.log("TCL: processResults: data", data);
            params.page = params.page || 1;
            return {
              results: data
            };
          },
          cache: false
        },
        minimumInputLength: 0,
      });
    },

    getTitleFormLanguageDropdownData: function () {
      $('#mediumTitleLanguageSelectDropdown').select2({
        closeOnSelect: true,
        scrollAfterSelect: true,
        allowClear: true,
        ajax: {
          url: 'api/language/selectList/',
          type: 'GET',
          dataType: 'json',
          delay: 250,
          headers: {
            "Authorization": "Bearer " + TIMAAT.Service.token,
            "Content-Type": "application/json",
          },
          // additional parameters
          data: function (params) {
            // console.log("TCL: data: params", params);
            return {
              search: params.term,
              page: params.page
            };
          },
          processResults: function (data, params) {
            // console.log("TCL: processResults: data", data);
            params.page = params.page || 1;
            return {
              results: data
            };
          },
          cache: false
        },
        minimumInputLength: 0,
      });
    },

    getMediumHasActorWithRoleData: function (id) {
      // console.log("TCL: getMediumHasActorWithRoleData:function -> id", id);
      $('#mediumHasActorWithRoleActorId-' + id).select2({
        closeOnSelect: true,
        scrollAfterSelect: true,
        allowClear: false,
        ajax: {
          url: 'api/actor/' + id + '/select',
          type: 'GET',
          dataType: 'json',
          delay: 250,
          headers: {
            "Authorization": "Bearer " + TIMAAT.Service.token,
            "Content-Type": "application/json",
          },
          // additional parameters
          data: function (params) {
            // console.log("TCL: data: params", params);
            return {
              // search: params.term,
              // page: params.page
            };
          },
          processResults: function (data, params) {
            // console.log("TCL: processResults: data", data);
            params.page = params.page || 1;
            return {
              results: data
            };
          },
          cache: false
        },
        minimumInputLength: 0,
      });
    },

    getMediumHasActorWithRolesDropdownData: function (id) {
      $('#mediumActorWithRolesMultiSelectDropdown-' + id).select2({
        closeOnSelect: false,
        scrollAfterSelect: true,
        allowClear: true,
        ajax: {
          url: 'api/medium/hasActor/' + id + '/withRoles/selectList',
          type: 'GET',
          dataType: 'json',
          delay: 250,
          headers: {
            "Authorization": "Bearer " + TIMAAT.Service.token,
            "Content-Type": "application/json",
          },
          // additional parameters
          data: function (params) {
            // console.log("TCL: data: params", params);
            return {
              search: params.term,
              page: params.page
            };
          },
          processResults: function (data, params) {
            // console.log("TCL: processResults: data", data);
            params.page = params.page || 1;
            return {
              results: data
            };
          },
          cache: false
        },
        minimumInputLength: 0,
      });
    },

    getMediumDatasetRoleFieldData: function (medium, roleId) {
      if (!medium || !medium.mediumHasActorWithRoles) return "-";
      var actorList = [];
      medium.mediumHasActorWithRoles.forEach(function (actorWithRole) {
        if (actorWithRole.role.id == roleId) actorList.push(actorWithRole.actor);
      });
      if (actorList.length == 0) return "-";
      var roleFieldData = "";
      var i = 0;
      for (; i < actorList.length; i++) {
        if (roleFieldData.length == 0) {
          roleFieldData += actorList[i].displayName.name;
          if (actorList[i].birthName && actorList[i].birthName.name != actorList[i].displayName.name) {
            roleFieldData += " <i>(" + actorList[i].birthName.name + ")<i>";
          }
        } else {
          roleFieldData += ",<br>" + actorList[i].displayName.name;
          if (actorList[i].birthName && actorList[i].birthName.name != actorList[i].displayName.name) {
            roleFieldData += " <i>(" + actorList[i].birthName.name + ")<i>";
          }
        }
      }
      // console.log("TCL: producer", producer);
      return roleFieldData;
    },

    replaceSpecialCharacters: function (unsafe) {
      return unsafe
          .replace(/</g, "&lt;")
          .replace(/>/g, "&gt;")
          .replace(/"/g, "\"")
          .replace(/'/g, "\'");
      // .replace(/&/g, "&amp;")
      // .replace(/</g, "&lt;")
      // .replace(/>/g, "&gt;")
      // .replace(/"/g, "&quot;")
      // .replace(/'/g, "&#039;");
    },

    setupAllMediaDataTable: async function () {
      // console.log("TCL: setupAllMediaDataTable");
      this.dataTableAllMediaList = $('#mediumDatasetsAllMediaDataTable').DataTable({
        "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
        "order": [[1, 'asc']],
        "pagingType": "full", // "simple_numbers",
        "processing": true,
        "stateSave": true,
        "scrollX": false,
        "scrollY": "60vh",
        "scrollCollapse": true,
        "rowId": 'id',
        "serverSide": true,
        "ajax": {
          "url": "api/medium/list",
          "contentType": "application/json; charset=utf-8",
          "dataType": "json",
          "data": function (data) {
            // console.log("TCL: data", data);
            let serverData = {
              draw: data.draw,
              start: data.start,
              length: data.length,
              orderby: data.columns[data.order[0].column].name,
              dir: data.order[0].dir,
            }
            if (data.search && data.search.value && data.search.value.length > 0)
              serverData.search = data.search.value;
            return serverData;
          },
          "beforeSend": function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
          "dataSrc": function (data) {
            // console.log("TCL: dataSrc - data", data);
            // setup model
            TIMAAT.MediumDatasets.allMediaList = data.data;
            return data.data;
          }
        },
        "createdRow": function (row, data, dataIndex) {
          // console.log("TCL: row, data, dataIndex", row, data, dataIndex);
          let rowItem = $(row);
          let medium = data;
          let type = medium.mediaType.mediaTypeTranslations[0].type;
          medium.ui = rowItem;
          rowItem.data('medium', medium);

          if (medium.fileStatus != "noFile") TIMAAT.VideoPlayer.loadThumbnail(medium);
          TIMAAT.MediumDatasets.setMediumStatus(medium);

          // set up events
          // rowItem.on('click', function(event) {
          // });

          rowItem.on('click', '.mediumItemUploadButton', async function (event) {
            event.stopPropagation();
            TIMAAT.UI.hidePopups();
            let item = new TIMAAT.Medium(medium, type);
            item.listView.find('.mediumUploadFile').click();
          });

          rowItem.on('click', '.mediumItemAnnotateButton', async function (event) {
            let currentMedium = rowItem.data('medium');
            event.stopPropagation();
            TIMAAT.UI.hidePopups();
            if (!medium.mediumVideo && !medium.mediumImage && !medium.mediumAudio) return; //* allow annotating only for videos, images and audio
            if (medium.fileStatus && medium.fileStatus != 'ready' && medium.fileStatus != 'transcoding' && medium.fileStatus != 'waiting') return;
            await TIMAAT.VideoPlayer.initializeAnnotationMode(medium);
          });

          rowItem.on('click', '.mediumDatasetsMediumMetadata', async function (event) {
            event.stopPropagation();
            let mediumModel = {};
            mediumModel.model = medium;
            TIMAAT.UI.displayComponent('medium', type + 'Tab', type + 'DataTable', 'mediumMetadataTab', 'mediumFormMetadata');
            TIMAAT.UI.displayDataSetContent('dataSheet', mediumModel, 'medium');
            TIMAAT.MediumDatasets.setDataTableOnItemSelect(type, mediumModel);
          });

          rowItem.find('.card-img-top').bind("mouseenter mousemove", function (ev) {
            //* only videos have thumbnail slideshow
            if (!medium.mediumVideo) return;
            if (medium.fileStatus && medium.fileStatus == "noFile") return;
            let length = medium.mediumVideo.length;
            let timeCode = Math.round((ev.originalEvent.offsetX / 254) * length);
            timeCode = Math.min(Math.max(0, timeCode), length);
            rowItem.find('.mediumThumbnail').attr('src', "/TIMAAT/api/medium/video/" + medium.id + "/thumbnail" + "?time=" + timeCode + "&token=" + medium.viewToken);
          });

          rowItem.find('.card-img-top').bind("mouseleave", function (ev) {
            //* only videos have thumbnail slideshow
            if (!medium.mediumVideo) return;
            if (medium.fileStatus && medium.fileStatus == "noFile") return;
            rowItem.find('.card-img-top').attr('src', "/TIMAAT/api/medium/video/" + medium.id + "/thumbnail" + "?token=" + medium.viewToken);
          });

          if (type == 'video' && medium.fileStatus != "ready" && medium.fileStatus != "unavailable" && medium.fileStatus != "noFile")
            TIMAAT.MediumDatasets.updateVideoStatus(medium);

        },
        "columns": [
          {
            data: null,
            className: 'mediumPreview',
            orderable: false,
            width: '20%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllMediaDataTable:function -> data, type, mediumItem, meta", data, type, mediumItem, meta);
              let ui;
              if (mediumItem.mediumVideo || mediumItem.mediumImage || mediumItem.mediumAudio) {
                ui = `<div class="medium-file-status js-medium-file-status">
											<i class="js-medium-file-status__icon fas fa-cog fa-spin"></i>
											</div>
										<img class="card-img-top center mediumThumbnail" src="img/preview-placeholder.png" width="150" height="85" alt="Medium preview"/>`;
              } else {
                ui = `<div class="display--flex"></div>`;
              }
              return ui;
            }
          },
          {
            data: 'id',
            name: 'title',
            className: 'title',
            width: '35%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllMediaDataTable:function -> data, type, mediumItem, meta", data, type, mediumItem, meta);
              let displayMediumTypeIcon = '';
              switch (mediumItem.mediaType.mediaTypeTranslations[0].type) {
                case 'audio':
                  displayMediumTypeIcon = '  <i class="far fa-file-audio" title="Audio"></i> ';
                  break;
                case 'document':
                  displayMediumTypeIcon = '  <i class="far fa-file-pdf" title="Document"></i> ';
                  break;
                case 'image':
                  displayMediumTypeIcon = '  <i class="far fa-file-image" title="Image"></i> ';
                  break;
                case 'software':
                  displayMediumTypeIcon = '  <i class="fas fa-compact-disc" title="Software"></i> ';
                  break;
                case 'text':
                  displayMediumTypeIcon = '  <i class="far fa-file-alt" title="Text"></i> ';
                  break;
                case 'video':
                  displayMediumTypeIcon = '  <i class="far fa-file-video" title="Video"></i> ';
                  break;
                case 'videogame':
                  displayMediumTypeIcon = '  <i class="fas fa-gamepad" title="Videogame"></i> ';
                  break;
              }
              let analysisListIcon = ' ';
              if (mediumItem.mediumAnalysisLists.length > 0) {
                analysisListIcon = '<i class="far fa-eye" title="Analysis available"></i> ';
              }

              let titleDisplay = `<p>` + displayMediumTypeIcon + analysisListIcon + mediumItem.displayTitle.name + `</p>`;
              if (mediumItem.originalTitle != null && mediumItem.displayTitle.id != mediumItem.originalTitle.id) {
                titleDisplay += `<p><i>(OT: ` + mediumItem.originalTitle.name + `)</i></p>`;
              }
              // TODO not working anymore due to server side dataTable data search
              mediumItem.titles.forEach(function (title) { // make additional titles searchable in media library
                if (title.id != mediumItem.displayTitle.id && (mediumItem.originalTitle == null || title.id != mediumItem.originalTitle.id)) {
                  titleDisplay += `<div class="display--none">` + title.name + `</div>`;
                }
              });
              return titleDisplay;
            }
          },
          {
            data: null,
            name: 'duration',
            className: 'duration',
            orderable: false,
            width: '10%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: data, type, mediumItem, meta - ", data, type, mediumItem, meta);
              if (mediumItem.mediumVideo && mediumItem.mediumVideo.length > 0) {
                return TIMAAT.Util.formatTime(mediumItem.mediumVideo.length);
              } else if (mediumItem.mediumAudio && mediumItem.mediumAudio.length > 0) {
                return TIMAAT.Util.formatTime(mediumItem.mediumAudio.length);
              } else {
                return "-";
              }
            }
          },
          {
            data: 'medium.mediumHasActorWithRoles',
            name: 'producer',
            className: 'producer',
            orderable: false,
            width: '10%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllMediaDataTable:function -> data, type, mediumItem, met", data, type, mediumItem, met);
              return TIMAAT.MediumDatasets.getMediumDatasetRoleFieldData(mediumItem, TIMAAT.MediumDatasets.producerId);
            }
          },
          {
            data: 'releaseDate',
            name: 'releaseDate',
            className: 'date',
            width: '10%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: data, type, mediumItem, meta", data, type, mediumItem, meta);
              if (mediumItem.releaseDate) {
                return moment.utc(mediumItem.releaseDate).format('YYYY-MM-DD');
              } else {
                return "-";
              }
            }
          },
          {
            data: 'mediumHasLanguages',
            name: 'language',
            className: 'language',
            orderable: false,
            width: '10%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: data, type, mediumItem, meta", data, type, mediumItem, meta);
              if (mediumItem.mediumHasLanguages && mediumItem.mediumHasLanguages.length > 0) {
                let i = 0;
                let languageDisplay = '';
                data.sort((a, b) => a.mediumLanguageType.id > b.mediumLanguageType.id ? 1 : ((b.mediumLanguageType.id > a.mediumLanguageType.id) ? -1 : 0));
                for (; i < data.length; i++) {
                  if (data[i].mediumLanguageType.id == 1) {
                    languageDisplay += `<p>` + data[i].language.name + ` (AT)</p>`;
                  } else if (data[i].mediumLanguageType.id == 2) {
                    languageDisplay += `<p>` + data[i].language.name + ` (STT)</p>`;
                  }
                }
                return languageDisplay;
              } else {
                return "-";
              }
            }
          },
          {
            data: null,
            className: 'actions text-align--center',
            orderable: false,
            width: '5%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllMediaDataTable:function -> data, type, mediumItem, meta", data, type, mediumItem, meta);
              let ui = `<div class="btn-group-vertical" role="group">`;
              if (mediumItem.mediumVideo) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload video" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate video" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              } else if (mediumItem.mediumAudio) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload audio" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate audio" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              } else if (mediumItem.mediumImage) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload image" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate image" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              }
              ui += `<button type="button" title="Edit data sheet" class="btn btn-outline-secondary btn-sm mediumDatasetsMediumMetadata"><i class="fas fa-file-alt"></i></button>
									</div>`;
              return ui;
            },
          }
        ],
        "language": {
          "decimal": ",",
          "thousands": ".",
          "search": "Search",
          "lengthMenu": "Show _MENU_ entries",
          "zeroRecords": "No records found.",
          "info": "Page _PAGE_ of _PAGES_ &middot; (_MAX_ items total)",
          "infoEmpty": "No items available.",
          "infoFiltered": "(&mdash; _TOTAL_ of _MAX_ items)",
          "paginate": {
            "first": "<<",
            "previous": "<",
            "next": ">",
            "last": ">>"
          },
        },
      });
    },

    setupAllAudiosDataTable: async function () {
      // console.log("TCL: setupAllAudiosDataTable");
      this.dataTableAllAudiosList = $('#mediumDatasetsMediumAllAudiosDataTable').DataTable({
        "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
        "order": [[1, 'asc']],
        "pagingType": "full", // "simple_numbers",
        "processing": true,
        "stateSave": true,
        "scrollX": false,
        "scrollY": "60vh",
        "scrollCollapse": true,
        "rowId": 'id',
        "serverSide": true,
        "ajax": {
          "url": "api/medium/audio/list",
          "contentType": "application/json; charset=utf-8",
          "dataType": "json",
          "data": function (data) {
            // console.log("TCL: data", data);
            let serverData = {
              draw: data.draw,
              start: data.start,
              length: data.length,
              orderby: data.columns[data.order[0].column].name,
              dir: data.order[0].dir,
            }
            if (data.search && data.search.value && data.search.value.length > 0)
              serverData.search = data.search.value;
            return serverData;
          },
          "beforeSend": function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
          "dataSrc": function (data) {
            // console.log("TCL: dataSrc - data", data);
            // setup model
            TIMAAT.MediumDatasets.allAudiosList = data.data;
            return data.data;
          }
        },
        "createdRow": function (row, data, dataIndex) {
          // console.log("TCL: row, data, dataIndex", row, data, dataIndex);
          let rowItem = $(row);
          let medium = data;
          let type = medium.mediaType.mediaTypeTranslations[0].type;
          medium.ui = rowItem;
          rowItem.data('medium', medium);

          if (medium.fileStatus != "noFile") TIMAAT.VideoPlayer.loadThumbnail(medium);
          TIMAAT.MediumDatasets.setMediumStatus(medium);

          // set up events
          rowItem.on('click', '.mediumItemUploadButton', async function (event) {
            event.stopPropagation();
            TIMAAT.UI.hidePopups();
            let item = new TIMAAT.Medium(medium, type);
            item.listView.find('.mediumUploadFile').click();
          });

          rowItem.on('click', '.mediumItemAnnotateButton', async function (event) {
            event.stopPropagation();
            TIMAAT.UI.hidePopups();
            if (!medium.mediumVideo && !medium.mediumImage && !medium.mediumAudio) return; //* allow annotating only for videos, images and audio
            if (medium.fileStatus && medium.fileStatus != 'ready' && medium.fileStatus != 'transcoding' && medium.fileStatus != 'waiting') return;
            await TIMAAT.VideoPlayer.initializeAnnotationMode(medium);
          });

          rowItem.on('click', '.mediumDatasetsMediumMetadata', async function (event) {
            event.stopPropagation();
            let mediumModel = {};
            mediumModel.model = medium;
            TIMAAT.UI.displayComponent('medium', type + 'Tab', type + 'DataTable', 'mediumMetadataTab', 'mediumFormMetadata');
            TIMAAT.UI.displayDataSetContent('dataSheet', mediumModel, 'medium');
            TIMAAT.MediumDatasets.setDataTableOnItemSelect(type, mediumModel);
          });

          if (type == 'video' && medium.fileStatus != "ready" && medium.fileStatus != "unavailable" && medium.fileStatus != "noFile")
            TIMAAT.MediumDatasets.updateVideoStatus(medium);

        },
        "columns": [
          {
            data: 'id',
            name: 'title',
            className: 'title',
            width: '35%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllAudiosDataTable:function -> data, type, mediumItem, meta", data, type, mediumItem, meta);
              let displayMediumTypeIcon = '';
              let analysisListIcon = ' ';
              if (mediumItem.mediumAnalysisLists.length > 0) {
                analysisListIcon = '<i class="far fa-eye" title="Analysis available"></i> ';
              }

              let titleDisplay = `<p>` + displayMediumTypeIcon + analysisListIcon + mediumItem.displayTitle.name + `</p>`;
              if (mediumItem.originalTitle != null && mediumItem.displayTitle.id != mediumItem.originalTitle.id) {
                titleDisplay += `<p><i>(OT: ` + mediumItem.originalTitle.name + `)</i></p>`;
              }
              // TODO not working anymore due to server side dataTable data search
              mediumItem.titles.forEach(function (title) { // make additional titles searchable in audios library
                if (title.id != mediumItem.displayTitle.id && (mediumItem.originalTitle == null || title.id != mediumItem.originalTitle.id)) {
                  titleDisplay += `<div class="display--none">` + title.name + `</div>`;
                }
              });
              return titleDisplay;
            }
          },
          {
            data: null,
            name: 'duration',
            className: 'duration',
            width: '15%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: data, type, mediumItem, meta - ", data, type, mediumItem, meta);
              if (mediumItem.mediumVideo && mediumItem.mediumVideo.length > 0) {
                return TIMAAT.Util.formatTime(mediumItem.mediumVideo.length);
              } else if (mediumItem.mediumAudio && mediumItem.mediumAudio.length > 0) {
                return TIMAAT.Util.formatTime(mediumItem.mediumAudio.length);
              } else {
                return "-";
              }
            }
          },
          {
            data: 'medium.mediumHasActorWithRoles',
            name: 'producer',
            className: 'producer',
            orderable: false,
            width: '20%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllAudiosDataTable:function -> data, type, mediumItem, met", data, type, mediumItem, met);
              return TIMAAT.MediumDatasets.getMediumDatasetRoleFieldData(mediumItem, TIMAAT.MediumDatasets.producerId);
            }
          },
          {
            data: 'releaseDate',
            name: 'releaseDate',
            className: 'date',
            width: '15%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: data, type, mediumItem, meta", data, type, mediumItem, meta);
              if (mediumItem.releaseDate) {
                return moment.utc(mediumItem.releaseDate).format('YYYY-MM-DD');
              } else {
                return "-";
              }
            }
          },
          {
            data: 'mediumHasLanguages',
            name: 'language',
            className: 'language',
            orderable: false,
            width: '10%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: data, type, mediumItem, meta", data, type, mediumItem, meta);
              if (mediumItem.mediumHasLanguages && mediumItem.mediumHasLanguages.length > 0) {
                let i = 0;
                let languageDisplay = '';
                data.sort((a, b) => a.mediumLanguageType.id > b.mediumLanguageType.id ? 1 : ((b.mediumLanguageType.id > a.mediumLanguageType.id) ? -1 : 0));
                for (; i < data.length; i++) {
                  if (data[i].mediumLanguageType.id == 1) {
                    languageDisplay += `<p>` + data[i].language.name + ` (AT)</p>`;
                  } else if (data[i].mediumLanguageType.id == 2) {
                    languageDisplay += `<p>` + data[i].language.name + ` (STT)</p>`;
                  }
                }
                return languageDisplay;
              } else {
                return "-";
              }
            }
          },
          {
            data: null,
            className: 'actions text-align--center',
            orderable: false,
            width: '5%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllAudiosDataTable:function -> data, type, mediumItem, meta", data, type, mediumItem, meta);
              let ui = `<div class="btn-group-vertical" role="group">`;
              if (mediumItem.mediumVideo) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload video" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate video" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              } else if (mediumItem.mediumAudio) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload audio" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate audio" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              } else if (mediumItem.mediumImage) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload image" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate image" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              }
              ui += `<button type="button" title="Edit data sheet" class="btn btn-outline-secondary btn-sm mediumDatasetsMediumMetadata"><i class="fas fa-file-alt"></i></button>
									</div>`;
              return ui;
            },
          }
        ],
        "language": {
          "decimal": ",",
          "thousands": ".",
          "search": "Search",
          "lengthMenu": "Show _MENU_ entries",
          "zeroRecords": "No records found.",
          "info": "Page _PAGE_ of _PAGES_ &middot; (_MAX_ items total)",
          "infoEmpty": "No items available.",
          "infoFiltered": "(&mdash; _TOTAL_ of _MAX_ items)",
          "paginate": {
            "first": "<<",
            "previous": "<",
            "next": ">",
            "last": ">>"
          },
        },
      });
    },

    setupAllDocumentsDataTable: async function () {
      // console.log("TCL: setupAllDocumentsDataTable");
      this.dataTableAllDocumentsList = $('#mediumDatasetsMediumAllDocumentsDataTable').DataTable({
        "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
        "order": [[1, 'asc']],
        "pagingType": "full", // "simple_numbers",
        "processing": true,
        "stateSave": true,
        "scrollX": false,
        "scrollY": "60vh",
        "scrollCollapse": true,
        "rowId": 'id',
        "serverSide": true,
        "ajax": {
          "url": "api/medium/document/list",
          "contentType": "application/json; charset=utf-8",
          "dataType": "json",
          "data": function (data) {
            // console.log("TCL: data", data);
            let serverData = {
              draw: data.draw,
              start: data.start,
              length: data.length,
              orderby: data.columns[data.order[0].column].name,
              dir: data.order[0].dir,
            }
            if (data.search && data.search.value && data.search.value.length > 0)
              serverData.search = data.search.value;
            return serverData;
          },
          "beforeSend": function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
          "dataSrc": function (data) {
            // console.log("TCL: dataSrc - data", data);
            // setup model
            TIMAAT.MediumDatasets.allDocumentsList = data.data;
            return data.data;
          }
        },
        "createdRow": function (row, data, dataIndex) {
          // console.log("TCL: row, data, dataIndex", row, data, dataIndex);
          let rowItem = $(row);
          let medium = data;
          let type = medium.mediaType.mediaTypeTranslations[0].type;
          medium.ui = rowItem;
          rowItem.data('medium', medium);

          if (medium.fileStatus != "noFile") TIMAAT.VideoPlayer.loadThumbnail(medium);
          TIMAAT.MediumDatasets.setMediumStatus(medium);

          // set up events
          rowItem.on('click', '.mediumItemUploadButton', async function (event) {
            event.stopPropagation();
            TIMAAT.UI.hidePopups();
            let item = new TIMAAT.Medium(medium, type);
            item.listView.find('.mediumUploadFile').click();
          });

          rowItem.on('click', '.mediumItemAnnotateButton', async function (event) {
            event.stopPropagation();
            TIMAAT.UI.hidePopups();
            if (!medium.mediumVideo && !medium.mediumImage && !medium.mediumAudio) return; //* allow annotating only for videos, images and audio
            if (medium.fileStatus && medium.fileStatus != 'ready' && medium.fileStatus != 'transcoding' && medium.fileStatus != 'waiting') return;
            await TIMAAT.VideoPlayer.initializeAnnotationMode(medium);
          });

          rowItem.on('click', '.mediumDatasetsMediumMetadata', async function (event) {
            event.stopPropagation();
            let mediumModel = {};
            mediumModel.model = medium;
            TIMAAT.UI.displayComponent('medium', type + 'Tab', type + 'DataTable', 'mediumMetadataTab', 'mediumFormMetadata');
            TIMAAT.UI.displayDataSetContent('dataSheet', mediumModel, 'medium');
            TIMAAT.MediumDatasets.setDataTableOnItemSelect(type, mediumModel);
          });

          if (type == 'video' && medium.fileStatus != "ready" && medium.fileStatus != "unavailable" && medium.fileStatus != "noFile")
            TIMAAT.MediumDatasets.updateVideoStatus(medium);

        },
        "columns": [
          {
            data: 'id',
            name: 'title',
            className: 'title',
            width: '50%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllDocumentsDataTable:function -> data, type, mediumItem, meta", data, type, mediumItem, meta);
              let displayMediumTypeIcon = '';
              let analysisListIcon = ' ';
              if (mediumItem.mediumAnalysisLists.length > 0) {
                analysisListIcon = '<i class="far fa-eye" title="Analysis available"></i> ';
              }

              let titleDisplay = `<p>` + displayMediumTypeIcon + analysisListIcon + mediumItem.displayTitle.name + `</p>`;
              if (mediumItem.originalTitle != null && mediumItem.displayTitle.id != mediumItem.originalTitle.id) {
                titleDisplay += `<p><i>(OT: ` + mediumItem.originalTitle.name + `)</i></p>`;
              }
              // TODO not working anymore due to server side dataTable data search
              mediumItem.titles.forEach(function (title) { // make additional titles searchable in documents library
                if (title.id != mediumItem.displayTitle.id && (mediumItem.originalTitle == null || title.id != mediumItem.originalTitle.id)) {
                  titleDisplay += `<div class="display--none">` + title.name + `</div>`;
                }
              });
              return titleDisplay;
            }
          },
          {
            data: 'medium.mediumHasActorWithRoles',
            name: 'author',
            className: 'author',
            orderable: false,
            width: '30%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllDocumentsDataTable:function -> data, type, mediumItem, met", data, type, mediumItem, met);
              return TIMAAT.MediumDatasets.getMediumDatasetRoleFieldData(mediumItem, TIMAAT.MediumDatasets.authorId);
            }
          },
          {
            data: 'releaseDate',
            name: 'releaseDate',
            className: 'date',
            width: '15%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: data, type, mediumItem, meta", data, type, mediumItem, meta);
              if (mediumItem.releaseDate) {
                return moment.utc(mediumItem.releaseDate).format('YYYY-MM-DD');
              } else {
                return "-";
              }
            }
          },
          {
            data: null,
            className: 'actions text-align--center',
            orderable: false,
            width: '5%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllDocumentsDataTable:function -> data, type, mediumItem, meta", data, type, mediumItem, meta);
              let ui = `<div class="btn-group-vertical" role="group">`;
              if (mediumItem.mediumVideo) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload video" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate video" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              } else if (mediumItem.mediumAudio) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload audio" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate audio" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              } else if (mediumItem.mediumImage) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload image" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate image" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              }
              ui += `<button type="button" title="Edit data sheet" class="btn btn-outline-secondary btn-sm mediumDatasetsMediumMetadata"><i class="fas fa-file-alt"></i></button>
									</div>`;
              return ui;
            },
          }
        ],
        "language": {
          "decimal": ",",
          "thousands": ".",
          "search": "Search",
          "lengthMenu": "Show _MENU_ entries",
          "zeroRecords": "No records found.",
          "info": "Page _PAGE_ of _PAGES_ &middot; (_MAX_ items total)",
          "infoEmpty": "No items available.",
          "infoFiltered": "(&mdash; _TOTAL_ of _MAX_ items)",
          "paginate": {
            "first": "<<",
            "previous": "<",
            "next": ">",
            "last": ">>"
          },
        },
      });
    },

    setupAllImagesDataTable: async function () {
      // console.log("TCL: setupAllImagesDataTable");
      this.dataTableAllImagesList = $('#mediumDatasetsMediumAllImagesDataTable').DataTable({
        "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
        "order": [[1, 'asc']],
        "pagingType": "full", // "simple_numbers",
        "processing": true,
        "stateSave": true,
        "scrollX": false,
        "scrollY": "60vh",
        "scrollCollapse": true,
        "rowId": 'id',
        "serverSide": true,
        "ajax": {
          "url": "api/medium/image/list",
          "contentType": "application/json; charset=utf-8",
          "dataType": "json",
          "data": function (data) {
            // console.log("TCL: data", data);
            let serverData = {
              draw: data.draw,
              start: data.start,
              length: data.length,
              orderby: data.columns[data.order[0].column].name,
              dir: data.order[0].dir,
            }
            if (data.search && data.search.value && data.search.value.length > 0)
              serverData.search = data.search.value;
            return serverData;
          },
          "beforeSend": function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
          "dataSrc": function (data) {
            // console.log("TCL: dataSrc - data", data);
            // setup model
            TIMAAT.MediumDatasets.allImagesList = data.data;
            return data.data;
          }
        },
        "createdRow": function (row, data, dataIndex) {
          // console.log("TCL: row, data, dataIndex", row, data, dataIndex);
          let rowItem = $(row);
          let medium = data;
          let type = medium.mediaType.mediaTypeTranslations[0].type;
          medium.ui = rowItem;
          rowItem.data('medium', medium);

          if (medium.fileStatus != "noFile") TIMAAT.VideoPlayer.loadThumbnail(medium);
          TIMAAT.MediumDatasets.setMediumStatus(medium);

          // set up events
          // rowItem.on('click', function(event) {
          // });

          rowItem.on('click', '.mediumItemUploadButton', async function (event) {
            event.stopPropagation();
            TIMAAT.UI.hidePopups();
            let item = new TIMAAT.Medium(medium, type);
            item.listView.find('.mediumUploadFile').click();
          });

          rowItem.on('click', '.mediumItemAnnotateButton', async function (event) {
            event.stopPropagation();
            TIMAAT.UI.hidePopups();
            if (!medium.mediumVideo && !medium.mediumImage && !medium.mediumAudio) return; //* allow annotating only for videos, images and audio
            if (medium.fileStatus && medium.fileStatus != 'ready' && medium.fileStatus != 'transcoding' && medium.fileStatus != 'waiting') return;
            await TIMAAT.VideoPlayer.initializeAnnotationMode(medium);
          });

          rowItem.on('click', '.mediumDatasetsMediumMetadata', async function (event) {
            event.stopPropagation();
            let mediumModel = {};
            mediumModel.model = medium;
            TIMAAT.UI.displayComponent('medium', type + 'Tab', type + 'DataTable', 'mediumMetadataTab', 'mediumFormMetadata');
            TIMAAT.UI.displayDataSetContent('dataSheet', mediumModel, 'medium');
            TIMAAT.MediumDatasets.setDataTableOnItemSelect(type, mediumModel);
          });

          if (type == 'video' && medium.fileStatus != "ready" && medium.fileStatus != "unavailable" && medium.fileStatus != "noFile")
            TIMAAT.MediumDatasets.updateVideoStatus(medium);

        },
        "columns": [
          {
            data: null,
            className: 'mediumPreview',
            orderable: false,
            width: '20%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllImagesDataTable:function -> data, type, mediumItem, meta", data, type, mediumItem, meta);
              let ui;
              if (mediumItem.mediumVideo) {
                ui = `<div class="medium-file-status js-medium-file-status">
											<i class="js-medium-file-status__icon fas fa-cog fa-spin"></i>
											</div>
										<img class="card-img-top center mediumThumbnail" src="img/preview-placeholder.png" width="150" height="85" alt="Video preview"/>`;
              } else if (mediumItem.mediumImage) {
                ui = `<div class="display--flex">
											<img class="card-img-top center mediumThumbnail" src="img/preview-placeholder.png" width="150" height="85" alt="Image preview"/>
										</div>`;
              } else if (mediumItem.mediumAudio) {
                ui = `<div class="display--flex">
											<i class="center fas fa-file-audio fa-5x"></i>
										</div>`;
              } else {
                ui = `<div class="display--flex">
											<img class="card-img-top center" src="img/preview-placeholder.png" width="150" height="85" alt="No preview available"/>
										</div>`;
              }
              return ui;
            }
          },
          {
            data: 'id',
            name: 'title',
            className: 'title',
            width: '40%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllImagesDataTable:function -> data, type, mediumItem, meta", data, type, mediumItem, meta);
              let displayMediumTypeIcon = '';
              let analysisListIcon = ' ';
              if (mediumItem.mediumAnalysisLists.length > 0) {
                analysisListIcon = '<i class="far fa-eye" title="Analysis available"></i> ';
              }

              let titleDisplay = `<p>` + displayMediumTypeIcon + analysisListIcon + mediumItem.displayTitle.name + `</p>`;
              if (mediumItem.originalTitle != null && mediumItem.displayTitle.id != mediumItem.originalTitle.id) {
                titleDisplay += `<p><i>(OT: ` + mediumItem.originalTitle.name + `)</i></p>`;
              }
              // TODO not working anymore due to server side dataTable data search
              mediumItem.titles.forEach(function (title) { // make additional titles searchable in images library
                if (title.id != mediumItem.displayTitle.id && (mediumItem.originalTitle == null || title.id != mediumItem.originalTitle.id)) {
                  titleDisplay += `<div class="display--none">` + title.name + `</div>`;
                }
              });
              return titleDisplay;
            }
          },
          {
            data: 'medium.mediumHasActorWithRoles',
            name: 'producer',
            className: 'producer',
            orderable: false,
            width: '20%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllImagesDataTable:function -> data, type, mediumItem, met", data, type, mediumItem, met);
              return TIMAAT.MediumDatasets.getMediumDatasetRoleFieldData(mediumItem, TIMAAT.MediumDatasets.producerId);
            }
          },
          {
            data: 'releaseDate',
            name: 'releaseDate',
            className: 'date',
            width: '15%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: data, type, mediumItem, meta", data, type, mediumItem, meta);
              if (mediumItem.releaseDate) {
                return moment.utc(mediumItem.releaseDate).format('YYYY-MM-DD');
              } else {
                return "-";
              }
            }
          },
          {
            data: null,
            className: 'actions text-align--center',
            orderable: false,
            width: '5%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllImagesDataTable:function -> data, type, mediumItem, meta", data, type, mediumItem, meta);
              let ui = `<div class="btn-group-vertical" role="group">`;
              if (mediumItem.mediumVideo) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload video" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate video" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              } else if (mediumItem.mediumAudio) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload audio" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate audio" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              } else if (mediumItem.mediumImage) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload image" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate image" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              }
              ui += `<button type="button" title="Edit data sheet" class="btn btn-outline-secondary btn-sm mediumDatasetsMediumMetadata"><i class="fas fa-file-alt"></i></button>
									</div>`;
              return ui;
            },
          }
        ],
        "language": {
          "decimal": ",",
          "thousands": ".",
          "search": "Search",
          "lengthMenu": "Show _MENU_ entries",
          "zeroRecords": "No records found.",
          "info": "Page _PAGE_ of _PAGES_ &middot; (_MAX_ items total)",
          "infoEmpty": "No items available.",
          "infoFiltered": "(&mdash; _TOTAL_ of _MAX_ items)",
          "paginate": {
            "first": "<<",
            "previous": "<",
            "next": ">",
            "last": ">>"
          },
        },
      });
    },

    setupAllSoftwaresDataTable: async function () {
      // console.log("TCL: setupAllSoftwaresDataTable");
      this.dataTableAllSoftwaresList = $('#mediumDatasetsMediumAllSoftwaresDataTable').DataTable({
        "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
        "order": [[1, 'asc']],
        "pagingType": "full", // "simple_numbers",
        "processing": true,
        "stateSave": true,
        "scrollX": false,
        "scrollY": "60vh",
        "scrollCollapse": true,
        "rowId": 'id',
        "serverSide": true,
        "ajax": {
          "url": "api/medium/software/list",
          "contentType": "application/json; charset=utf-8",
          "dataType": "json",
          "data": function (data) {
            // console.log("TCL: data", data);
            let serverData = {
              draw: data.draw,
              start: data.start,
              length: data.length,
              orderby: data.columns[data.order[0].column].name,
              dir: data.order[0].dir,
            }
            if (data.search && data.search.value && data.search.value.length > 0)
              serverData.search = data.search.value;
            return serverData;
          },
          "beforeSend": function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
          "dataSrc": function (data) {
            // console.log("TCL: dataSrc - data", data);
            // setup model
            TIMAAT.MediumDatasets.allSoftwaresList = data.data;
            return data.data;
          }
        },
        "createdRow": function (row, data, dataIndex) {
          // console.log("TCL: row, data, dataIndex", row, data, dataIndex);
          let rowItem = $(row);
          let medium = data;
          let type = medium.mediaType.mediaTypeTranslations[0].type;
          medium.ui = rowItem;
          rowItem.data('medium', medium);

          if (medium.fileStatus != "noFile") TIMAAT.VideoPlayer.loadThumbnail(medium);
          TIMAAT.MediumDatasets.setMediumStatus(medium);

          // set up events
          // rowItem.on('click', function(event) {
          // });

          rowItem.on('click', '.mediumItemUploadButton', async function (event) {
            event.stopPropagation();
            TIMAAT.UI.hidePopups();
            let item = new TIMAAT.Medium(medium, type);
            item.listView.find('.mediumUploadFile').click();
          });

          rowItem.on('click', '.mediumItemAnnotateButton', async function (event) {
            event.stopPropagation();
            TIMAAT.UI.hidePopups();
            if (!medium.mediumVideo && !medium.mediumImage && !medium.mediumAudio) return; //* allow annotating only for videos, images and audio
            if (medium.fileStatus && medium.fileStatus != 'ready' && medium.fileStatus != 'transcoding' && medium.fileStatus != 'waiting') return;
            await TIMAAT.VideoPlayer.initializeAnnotationMode(medium);
          });

          rowItem.on('click', '.mediumDatasetsMediumMetadata', async function (event) {
            event.stopPropagation();
            let mediumModel = {};
            mediumModel.model = medium;
            TIMAAT.UI.displayComponent('medium', type + 'Tab', type + 'DataTable', 'mediumMetadataTab', 'mediumFormMetadata');
            TIMAAT.UI.displayDataSetContent('dataSheet', mediumModel, 'medium');
            TIMAAT.MediumDatasets.setDataTableOnItemSelect(type, mediumModel);
          });

          if (type == 'video' && medium.fileStatus != "ready" && medium.fileStatus != "unavailable" && medium.fileStatus != "noFile")
            TIMAAT.MediumDatasets.updateVideoStatus(medium);

        },
        "columns": [
          {
            data: 'id',
            name: 'title',
            className: 'title',
            width: '40%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllSoftwaresDataTable:function -> data, type, mediumItem, meta", data, type, mediumItem, meta);
              let displayMediumTypeIcon = '';
              let analysisListIcon = ' ';
              if (mediumItem.mediumAnalysisLists.length > 0) {
                analysisListIcon = '<i class="far fa-eye" title="Analysis available"></i> ';
              }

              let titleDisplay = `<p>` + displayMediumTypeIcon + analysisListIcon + mediumItem.displayTitle.name + `</p>`;
              if (mediumItem.originalTitle != null && mediumItem.displayTitle.id != mediumItem.originalTitle.id) {
                titleDisplay += `<p><i>(OT: ` + mediumItem.originalTitle.name + `)</i></p>`;
              }
              // TODO not working anymore due to server side dataTable data search
              mediumItem.titles.forEach(function (title) { // make additional titles searchable in softwares library
                if (title.id != mediumItem.displayTitle.id && (mediumItem.originalTitle == null || title.id != mediumItem.originalTitle.id)) {
                  titleDisplay += `<div class="display--none">` + title.name + `</div>`;
                }
              });
              return titleDisplay;
            }
          },
          {
            data: 'medium.mediumHasActorWithRoles',
            name: 'producer',
            className: 'producer',
            orderable: false,
            width: '25%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllSoftwaresDataTable:function -> data, type, mediumItem, met", data, type, mediumItem, met);
              return TIMAAT.MediumDatasets.getMediumDatasetRoleFieldData(mediumItem, TIMAAT.MediumDatasets.producerId);
            }
          },
          {
            data: 'releaseDate',
            name: 'releaseDate',
            className: 'date',
            width: '15%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: data, type, mediumItem, meta", data, type, mediumItem, meta);
              if (mediumItem.releaseDate) {
                return moment.utc(mediumItem.releaseDate).format('YYYY-MM-DD');
              } else {
                return "-";
              }
            }
          },
          {
            data: 'mediumHasLanguages',
            name: 'language',
            className: 'language',
            orderable: false,
            width: '15%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: data, type, mediumItem, meta", data, type, mediumItem, meta);
              if (mediumItem.mediumHasLanguages && mediumItem.mediumHasLanguages.length > 0) {
                let i = 0;
                let languageDisplay = '';
                data.sort((a, b) => a.mediumLanguageType.id > b.mediumLanguageType.id ? 1 : ((b.mediumLanguageType.id > a.mediumLanguageType.id) ? -1 : 0));
                for (; i < data.length; i++) {
                  if (data[i].mediumLanguageType.id == 1) {
                    languageDisplay += `<p>` + data[i].language.name + ` (AT)</p>`;
                  } else if (data[i].mediumLanguageType.id == 2) {
                    languageDisplay += `<p>` + data[i].language.name + ` (STT)</p>`;
                  }
                }
                return languageDisplay;
              } else {
                return "-";
              }
            }
          },
          {
            data: null,
            className: 'actions text-align--center',
            orderable: false,
            width: '5%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllSoftwaresDataTable:function -> data, type, mediumItem, meta", data, type, mediumItem, meta);
              let ui = `<div class="btn-group-vertical" role="group">`;
              if (mediumItem.mediumVideo) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload video" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate video" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              } else if (mediumItem.mediumAudio) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload audio" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate audio" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              } else if (mediumItem.mediumImage) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload image" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate image" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              }
              ui += `<button type="button" title="Edit data sheet" class="btn btn-outline-secondary btn-sm mediumDatasetsMediumMetadata"><i class="fas fa-file-alt"></i></button>
									</div>`;
              return ui;
            },
          }
        ],
        "language": {
          "decimal": ",",
          "thousands": ".",
          "search": "Search",
          "lengthMenu": "Show _MENU_ entries",
          "zeroRecords": "No records found.",
          "info": "Page _PAGE_ of _PAGES_ &middot; (_MAX_ items total)",
          "infoEmpty": "No items available.",
          "infoFiltered": "(&mdash; _TOTAL_ of _MAX_ items)",
          "paginate": {
            "first": "<<",
            "previous": "<",
            "next": ">",
            "last": ">>"
          },
        },
      });
    },

    setupAllTextsDataTable: async function () {
      // console.log("TCL: setupAllTextsDataTable");
      this.dataTableAllTextsList = $('#mediumDatasetsMediumAllTextsDataTable').DataTable({
        "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
        "order": [[1, 'asc']],
        "pagingType": "full", // "simple_numbers",
        "processing": true,
        "stateSave": true,
        "scrollX": false,
        "scrollY": "60vh",
        "scrollCollapse": true,
        "rowId": 'id',
        "serverSide": true,
        "ajax": {
          "url": "api/medium/text/list",
          "contentType": "application/json; charset=utf-8",
          "dataType": "json",
          "data": function (data) {
            // console.log("TCL: data", data);
            let serverData = {
              draw: data.draw,
              start: data.start,
              length: data.length,
              orderby: data.columns[data.order[0].column].name,
              dir: data.order[0].dir,
            }
            if (data.search && data.search.value && data.search.value.length > 0)
              serverData.search = data.search.value;
            return serverData;
          },
          "beforeSend": function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
          "dataSrc": function (data) {
            // console.log("TCL: dataSrc - data", data);
            // setup model
            TIMAAT.MediumDatasets.allTextsList = data.data;
            return data.data;
          }
        },
        "createdRow": function (row, data, dataIndex) {
          // console.log("TCL: row, data, dataIndex", row, data, dataIndex);
          let rowItem = $(row);
          let medium = data;
          let type = medium.mediaType.mediaTypeTranslations[0].type;
          medium.ui = rowItem;
          rowItem.data('medium', medium);

          if (medium.fileStatus != "noFile") TIMAAT.VideoPlayer.loadThumbnail(medium);
          TIMAAT.MediumDatasets.setMediumStatus(medium);

          // set up events
          // rowItem.on('click', function(event) {
          // });

          rowItem.on('click', '.mediumItemUploadButton', async function (event) {
            event.stopPropagation();
            TIMAAT.UI.hidePopups();
            let item = new TIMAAT.Medium(medium, type);
            item.listView.find('.mediumUploadFile').click();
          });

          rowItem.on('click', '.mediumItemAnnotateButton', async function (event) {
            event.stopPropagation();
            TIMAAT.UI.hidePopups();
            if (!medium.mediumVideo && !medium.mediumImage && !medium.mediumAudio) return; //* allow annotating only for videos, images and audio
            if (medium.fileStatus && medium.fileStatus != 'ready' && medium.fileStatus != 'transcoding' && medium.fileStatus != 'waiting') return;
            await TIMAAT.VideoPlayer.initializeAnnotationMode(medium);
          });

          rowItem.on('click', '.mediumDatasetsMediumMetadata', async function (event) {
            event.stopPropagation();
            let mediumModel = {};
            mediumModel.model = medium;
            TIMAAT.UI.displayComponent('medium', type + 'Tab', type + 'DataTable', 'mediumMetadataTab', 'mediumFormMetadata');
            TIMAAT.UI.displayDataSetContent('dataSheet', mediumModel, 'medium');
            TIMAAT.MediumDatasets.setDataTableOnItemSelect(type, mediumModel);
          });

          if (type == 'video' && medium.fileStatus != "ready" && medium.fileStatus != "unavailable" && medium.fileStatus != "noFile")
            TIMAAT.MediumDatasets.updateVideoStatus(medium);

        },
        "columns": [
          {
            data: 'id',
            name: 'title',
            className: 'title',
            width: '40%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllTextsDataTable:function -> data, type, mediumItem, meta", data, type, mediumItem, meta);
              let displayMediumTypeIcon = '';
              let analysisListIcon = ' ';
              if (mediumItem.mediumAnalysisLists.length > 0) {
                analysisListIcon = '<i class="far fa-eye" title="Analysis available"></i> ';
              }

              let titleDisplay = `<p>` + displayMediumTypeIcon + analysisListIcon + mediumItem.displayTitle.name + `</p>`;
              if (mediumItem.originalTitle != null && mediumItem.displayTitle.id != mediumItem.originalTitle.id) {
                titleDisplay += `<p><i>(OT: ` + mediumItem.originalTitle.name + `)</i></p>`;
              }
              // TODO not working anymore due to server side dataTable data search
              mediumItem.titles.forEach(function (title) { // make additional titles searchable in texts library
                if (title.id != mediumItem.displayTitle.id && (mediumItem.originalTitle == null || title.id != mediumItem.originalTitle.id)) {
                  titleDisplay += `<div class="display--none">` + title.name + `</div>`;
                }
              });
              return titleDisplay;
            }
          },
          {
            data: 'medium.mediumHasActorWithRoles',
            name: 'author',
            className: 'author',
            orderable: false,
            width: '25%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllTextsDataTable:function -> data, type, mediumItem, met", data, type, mediumItem, met);
              return TIMAAT.MediumDatasets.getMediumDatasetRoleFieldData(mediumItem, TIMAAT.MediumDatasets.producerId);
            }
          },
          {
            data: 'releaseDate',
            name: 'releaseDate',
            className: 'date',
            width: '15%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: data, type, mediumItem, meta", data, type, mediumItem, meta);
              if (mediumItem.releaseDate) {
                return moment.utc(mediumItem.releaseDate).format('YYYY-MM-DD');
              } else {
                return "-";
              }
            }
          },
          {
            data: 'mediumHasLanguages',
            name: 'language',
            className: 'language',
            orderable: false,
            width: '15%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: data, type, mediumItem, meta", data, type, mediumItem, meta);
              if (mediumItem.mediumHasLanguages && mediumItem.mediumHasLanguages.length > 0) {
                let i = 0;
                let languageDisplay = '';
                data.sort((a, b) => a.mediumLanguageType.id > b.mediumLanguageType.id ? 1 : ((b.mediumLanguageType.id > a.mediumLanguageType.id) ? -1 : 0));
                for (; i < data.length; i++) {
                  if (data[i].mediumLanguageType.id == 1) {
                    languageDisplay += `<p>` + data[i].language.name + ` (AT)</p>`;
                  } else if (data[i].mediumLanguageType.id == 2) {
                    languageDisplay += `<p>` + data[i].language.name + ` (STT)</p>`;
                  }
                }
                return languageDisplay;
              } else {
                return "-";
              }
            }
          },
          {
            data: null,
            className: 'actions text-align--center',
            orderable: false,
            width: '5%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllTextsDataTable:function -> data, type, mediumItem, meta", data, type, mediumItem, meta);
              let ui = `<div class="btn-group-vertical" role="group">`;
              if (mediumItem.mediumVideo) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload video" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate video" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              } else if (mediumItem.mediumAudio) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload audio" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate audio" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              } else if (mediumItem.mediumImage) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload image" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate image" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              }
              ui += `<button type="button" title="Edit data sheet" class="btn btn-outline-secondary btn-sm mediumDatasetsMediumMetadata"><i class="fas fa-file-alt"></i></button>
									</div>`;
              return ui;
            },
          }
        ],
        "language": {
          "decimal": ",",
          "thousands": ".",
          "search": "Search",
          "lengthMenu": "Show _MENU_ entries",
          "zeroRecords": "No records found.",
          "info": "Page _PAGE_ of _PAGES_ &middot; (_MAX_ items total)",
          "infoEmpty": "No items available.",
          "infoFiltered": "(&mdash; _TOTAL_ of _MAX_ items)",
          "paginate": {
            "first": "<<",
            "previous": "<",
            "next": ">",
            "last": ">>"
          },
        },
      });
    },

    setupAllVideosDataTable: async function () {
      // console.log("TCL: setupAllVideosDataTable");
      this.dataTableAllVideosList = $('#mediumDatasetsMediumAllVideosDataTable').DataTable({
        "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
        "order": [[1, 'asc']],
        "pagingType": "full", // "simple_numbers",
        "processing": true,
        "stateSave": true,
        "scrollX": false,
        "scrollY": "60vh",
        "scrollCollapse": true,
        "rowId": 'id',
        "serverSide": true,
        "ajax": {
          "url": "api/medium/video/list",
          "contentType": "application/json; charset=utf-8",
          "dataType": "json",
          "data": function (data) {
            // console.log("TCL: data", data);
            let serverData = {
              draw: data.draw,
              start: data.start,
              length: data.length,
              orderby: data.columns[data.order[0].column].name,
              dir: data.order[0].dir,
            }
            if (data.search && data.search.value && data.search.value.length > 0)
              serverData.search = data.search.value;
            return serverData;
          },
          "beforeSend": function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
          "dataSrc": function (data) {
            // console.log("TCL: dataSrc - data", data);
            // setup model
            TIMAAT.MediumDatasets.allVideosList = data.data;
            return data.data;
          }
        },
        "createdRow": function (row, data, dataIndex) {
          // console.log("TCL: row, data, dataIndex", row, data, dataIndex);
          let rowItem = $(row);
          let medium = data;
          let type = medium.mediaType.mediaTypeTranslations[0].type;
          medium.ui = rowItem;
          rowItem.data('medium', medium);

          if (medium.fileStatus != "noFile") TIMAAT.VideoPlayer.loadThumbnail(medium);
          TIMAAT.MediumDatasets.setMediumStatus(medium);

          // set up events
          // rowItem.on('click', function(event) {
          // });

          rowItem.on('click', '.mediumItemUploadButton', async function (event) {
            event.stopPropagation();
            TIMAAT.UI.hidePopups();
            let item = new TIMAAT.Medium(medium, type);
            item.listView.find('.mediumUploadFile').click();
          });

          rowItem.on('click', '.mediumItemAnnotateButton', async function (event) {
            event.stopPropagation();
            TIMAAT.UI.hidePopups();
            if (!medium.mediumVideo && !medium.mediumImage && !medium.mediumAudio) return; //* allow annotating only for videos, images and audio
            if (medium.fileStatus && medium.fileStatus != 'ready' && medium.fileStatus != 'transcoding' && medium.fileStatus != 'waiting') return;
            await TIMAAT.VideoPlayer.initializeAnnotationMode(medium);
          });

          rowItem.on('click', '.mediumDatasetsMediumMetadata', async function (event) {
            event.stopPropagation();
            let mediumModel = {};
            mediumModel.model = medium;
            TIMAAT.UI.displayComponent('medium', type + 'Tab', type + 'DataTable', 'mediumMetadataTab', 'mediumFormMetadata');
            TIMAAT.UI.displayDataSetContent('dataSheet', mediumModel, 'medium');
            TIMAAT.MediumDatasets.setDataTableOnItemSelect(type, mediumModel);
          });

          rowItem.find('.card-img-top').bind("mouseenter mousemove", function (ev) {
            //* only videos have thumbnail slideshow
            if (!medium.mediumVideo) return;
            if (medium.fileStatus && medium.fileStatus == "noFile") return;
            let length = medium.mediumVideo.length;
            let timeCode = Math.round((ev.originalEvent.offsetX / 254) * length);
            timeCode = Math.min(Math.max(0, timeCode), length);
            rowItem.find('.mediumThumbnail').attr('src', "/TIMAAT/api/medium/video/" + medium.id + "/thumbnail" + "?time=" + timeCode + "&token=" + medium.viewToken);
          });

          rowItem.find('.card-img-top').bind("mouseleave", function (ev) {
            //* only videos have thumbnail slideshow
            if (!medium.mediumVideo) return;
            if (medium.fileStatus && medium.fileStatus == "noFile") return;
            rowItem.find('.card-img-top').attr('src', "/TIMAAT/api/medium/video/" + medium.id + "/thumbnail" + "?token=" + medium.viewToken);
          });

          if (type == 'video' && medium.fileStatus != "ready" && medium.fileStatus != "unavailable" && medium.fileStatus != "noFile")
            TIMAAT.MediumDatasets.updateVideoStatus(medium);

        },
        "columns": [
          {
            data: null,
            className: 'mediumPreview',
            orderable: false,
            width: '150px',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllVideosDataTable:function -> data, type, mediumItem, meta", data, type, mediumItem, meta);
              let ui;
              if (mediumItem.mediumVideo) {
                ui = `<div class="medium-file-status js-medium-file-status">
											<i class="js-medium-file-status__icon fas fa-cog fa-spin"></i>
											</div>
										<img class="card-img-top center mediumThumbnail" src="img/preview-placeholder.png" width="150" height="85" alt="Video preview"/>`;
              } else if (mediumItem.mediumImage) {
                ui = `<div class="display--flex">
											<img class="card-img-top center mediumThumbnail" src="img/preview-placeholder.png" width="150" height="85" alt="Image preview"/>
										</div>`;
              } else if (mediumItem.mediumAudio) {
                ui = `<div class="display--flex">
											<i class="center fas fa-file-audio fa-5x"></i>
										</div>`;
              } else {
                ui = `<div class="display--flex">
											<img class="card-img-top center" src="img/preview-placeholder.png" width="150" height="85" alt="No preview available"/>
										</div>`;
              }
              return ui;
            }
          },
          {
            data: 'id',
            name: 'title',
            className: 'title',
            width: '50%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllVideosDataTable:function -> data, type, mediumItem, meta", data, type, mediumItem, meta);
              let displayMediumTypeIcon = '';
              let analysisListIcon = ' ';
              if (mediumItem.mediumAnalysisLists.length > 0) {
                analysisListIcon = '<i class="far fa-eye" title="Analysis available"></i> ';
              }

              let titleDisplay = `<p>` + displayMediumTypeIcon + analysisListIcon + mediumItem.displayTitle.name + `</p>`;
              if (mediumItem.originalTitle != null && mediumItem.displayTitle.id != mediumItem.originalTitle.id) {
                titleDisplay += `<p><i>(OT: ` + mediumItem.originalTitle.name + `)</i></p>`;
              }
              // TODO not working anymore due to server side dataTable data search
              mediumItem.titles.forEach(function (title) { // make additional titles searchable in videos library
                if (title.id != mediumItem.displayTitle.id && (mediumItem.originalTitle == null || title.id != mediumItem.originalTitle.id)) {
                  titleDisplay += `<div class="display--none">` + title.name + `</div>`;
                }
              });
              return titleDisplay;
            }
          },
          {
            data: null,
            name: 'duration',
            className: 'duration',
            width: '10%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: data, type, mediumItem, meta - ", data, type, mediumItem, meta);
              if (mediumItem.mediumVideo && mediumItem.mediumVideo.length > 0) {
                return TIMAAT.Util.formatTime(mediumItem.mediumVideo.length);
              } else if (mediumItem.mediumAudio && mediumItem.mediumAudio.length > 0) {
                return TIMAAT.Util.formatTime(mediumItem.mediumAudio.length);
              } else {
                return "-";
              }
            }
          },
          {
            data: 'medium.mediumHasActorWithRoles',
            name: 'producer',
            className: 'producer',
            orderable: false,
            width: '15%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllVideosDataTable:function -> data, type, mediumItem, met", data, type, mediumItem, met);
              return TIMAAT.MediumDatasets.getMediumDatasetRoleFieldData(mediumItem, TIMAAT.MediumDatasets.producerId);
            }
          },
          {
            data: 'releaseDate',
            name: 'releaseDate',
            className: 'date',
            width: '10%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: data, type, mediumItem, meta", data, type, mediumItem, meta);
              if (mediumItem.releaseDate) {
                return moment.utc(mediumItem.releaseDate).format('YYYY-MM-DD');
              } else {
                return "-";
              }
            }
          },
          {
            data: 'mediumHasLanguages',
            name: 'language',
            className: 'language',
            orderable: false,
            width: '10%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: data, type, mediumItem, meta", data, type, mediumItem, meta);
              if (mediumItem.mediumHasLanguages && mediumItem.mediumHasLanguages.length > 0) {
                let i = 0;
                let languageDisplay = '';
                data.sort((a, b) => a.mediumLanguageType.id > b.mediumLanguageType.id ? 1 : ((b.mediumLanguageType.id > a.mediumLanguageType.id) ? -1 : 0));
                for (; i < data.length; i++) {
                  if (data[i].mediumLanguageType.id == 1) {
                    languageDisplay += `<p>` + data[i].language.name + ` (AT)</p>`;
                  } else if (data[i].mediumLanguageType.id == 2) {
                    languageDisplay += `<p>` + data[i].language.name + ` (STT)</p>`;
                  }
                }
                return languageDisplay;
              } else {
                return "-";
              }
            }
          },
          {
            data: null,
            className: 'actions text-align--center',
            orderable: false,
            width: '5%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllVideosDataTable:function -> data, type, mediumItem, meta", data, type, mediumItem, meta);
              let ui = `<div class="btn-group-vertical" role="group">`;
              if (mediumItem.mediumVideo) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload video" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate video" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              } else if (mediumItem.mediumAudio) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload audio" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate audio" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              } else if (mediumItem.mediumImage) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload image" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate image" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              }
              ui += `<button type="button" title="Edit data sheet" class="btn btn-outline-secondary btn-sm mediumDatasetsMediumMetadata"><i class="fas fa-file-alt"></i></button>
									</div>`;
              return ui;
            },
          }
        ],
        "language": {
          "decimal": ",",
          "thousands": ".",
          "search": "Search",
          "lengthMenu": "Show _MENU_ entries",
          "zeroRecords": "No records found.",
          "info": "Page _PAGE_ of _PAGES_ &middot; (_MAX_ items total)",
          "infoEmpty": "No items available.",
          "infoFiltered": "(&mdash; _TOTAL_ of _MAX_ items)",
          "paginate": {
            "first": "<<",
            "previous": "<",
            "next": ">",
            "last": ">>"
          },
        },
      });
    },

    setupAllVideogamesDataTable: async function () {
      // console.log("TCL: setupAllVideogamesDataTable");
      this.dataTableAllVideogamesList = $('#mediumDatasetsMediumAllVideogamesDataTable').DataTable({
        "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
        "order": [[1, 'asc']],
        "pagingType": "full", // "simple_numbers",
        "processing": true,
        "stateSave": true,
        "scrollX": false,
        "scrollY": "60vh",
        "scrollCollapse": true,
        "rowId": 'id',
        "serverSide": true,
        "ajax": {
          "url": "api/medium/videogame/list",
          "contentType": "application/json; charset=utf-8",
          "dataType": "json",
          "data": function (data) {
            // console.log("TCL: data", data);
            let serverData = {
              draw: data.draw,
              start: data.start,
              length: data.length,
              orderby: data.columns[data.order[0].column].name,
              dir: data.order[0].dir,
            }
            if (data.search && data.search.value && data.search.value.length > 0)
              serverData.search = data.search.value;
            return serverData;
          },
          "beforeSend": function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
          "dataSrc": function (data) {
            // console.log("TCL: dataSrc - data", data);
            // setup model
            TIMAAT.MediumDatasets.allVideogamesList = data.data;
            return data.data;
          }
        },
        "createdRow": function (row, data, dataIndex) {
          // console.log("TCL: row, data, dataIndex", row, data, dataIndex);
          let rowItem = $(row);
          let medium = data;
          let type = medium.mediaType.mediaTypeTranslations[0].type;
          medium.ui = rowItem;
          rowItem.data('medium', medium);

          if (medium.fileStatus != "noFile") TIMAAT.VideoPlayer.loadThumbnail(medium);
          TIMAAT.MediumDatasets.setMediumStatus(medium);

          // set up events
          // rowItem.on('click', function(event) {
          // });

          rowItem.on('click', '.mediumItemUploadButton', async function (event) {
            event.stopPropagation();
            TIMAAT.UI.hidePopups();
            let item = new TIMAAT.Medium(medium, type);
            item.listView.find('.mediumUploadFile').click();
          });

          rowItem.on('click', '.mediumItemAnnotateButton', async function (event) {
            event.stopPropagation();
            TIMAAT.UI.hidePopups();
            if (!medium.mediumVideo && !medium.mediumImage && !medium.mediumAudio) return; //* allow annotating only for videos, images and audio
            if (medium.fileStatus && medium.fileStatus != 'ready' && medium.fileStatus != 'transcoding' && medium.fileStatus != 'waiting') return;
            await TIMAAT.VideoPlayer.initializeAnnotationMode(medium);
          });

          rowItem.on('click', '.mediumDatasetsMediumMetadata', async function (event) {
            event.stopPropagation();
            let mediumModel = {};
            mediumModel.model = medium;
            TIMAAT.UI.displayComponent('medium', type + 'Tab', type + 'DataTable', 'mediumMetadataTab', 'mediumFormMetadata');
            TIMAAT.UI.displayDataSetContent('dataSheet', mediumModel, 'medium');
            TIMAAT.MediumDatasets.setDataTableOnItemSelect(type, mediumModel);
          });

          if (type == 'video' && medium.fileStatus != "ready" && medium.fileStatus != "unavailable" && medium.fileStatus != "noFile")
            TIMAAT.MediumDatasets.updateVideoStatus(medium);

        },
        "columns": [
          {
            data: 'id',
            name: 'title',
            className: 'title',
            width: '40%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllVideogamesDataTable:function -> data, type, mediumItem, meta", data, type, mediumItem, meta);
              let displayMediumTypeIcon = '';
              let analysisListIcon = ' ';
              if (mediumItem.mediumAnalysisLists.length > 0) {
                analysisListIcon = '<i class="far fa-eye" title="Analysis available"></i> ';
              }

              let titleDisplay = `<p>` + displayMediumTypeIcon + analysisListIcon + mediumItem.displayTitle.name + `</p>`;
              if (mediumItem.originalTitle != null && mediumItem.displayTitle.id != mediumItem.originalTitle.id) {
                titleDisplay += `<p><i>(OT: ` + mediumItem.originalTitle.name + `)</i></p>`;
              }
              // TODO not working anymore due to server side dataTable data search
              mediumItem.titles.forEach(function (title) { // make additional titles searchable in videogames library
                if (title.id != mediumItem.displayTitle.id && (mediumItem.originalTitle == null || title.id != mediumItem.originalTitle.id)) {
                  titleDisplay += `<div class="display--none">` + title.name + `</div>`;
                }
              });
              return titleDisplay;
            }
          },
          {
            data: 'medium.mediumHasActorWithRoles',
            name: 'producer',
            className: 'producer',
            orderable: false,
            width: '25%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllVideogamesDataTable:function -> data, type, mediumItem, met", data, type, mediumItem, met);
              return TIMAAT.MediumDatasets.getMediumDatasetRoleFieldData(mediumItem, TIMAAT.MediumDatasets.producerId);
            }
          },
          {
            data: 'releaseDate',
            name: 'releaseDate',
            className: 'date',
            width: '15%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: data, type, mediumItem, meta", data, type, mediumItem, meta);
              if (mediumItem.releaseDate) {
                return moment.utc(mediumItem.releaseDate).format('YYYY-MM-DD');
              } else {
                return "-";
              }
            }
          },
          {
            data: 'mediumHasLanguages',
            name: 'language',
            className: 'language',
            orderable: false,
            width: '15%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: data, type, mediumItem, meta", data, type, mediumItem, meta);
              if (mediumItem.mediumHasLanguages && mediumItem.mediumHasLanguages.length > 0) {
                let i = 0;
                let languageDisplay = '';
                data.sort((a, b) => a.mediumLanguageType.id > b.mediumLanguageType.id ? 1 : ((b.mediumLanguageType.id > a.mediumLanguageType.id) ? -1 : 0));
                for (; i < data.length; i++) {
                  if (data[i].mediumLanguageType.id == 1) {
                    languageDisplay += `<p>` + data[i].language.name + ` (AT)</p>`;
                  } else if (data[i].mediumLanguageType.id == 2) {
                    languageDisplay += `<p>` + data[i].language.name + ` (STT)</p>`;
                  }
                }
                return languageDisplay;
              } else {
                return "-";
              }
            }
          },
          {
            data: null,
            className: 'actions text-align--center',
            orderable: false,
            width: '5%',
            render: function (data, type, mediumItem, meta) {
              // console.log("TCL: setupAllVideogamesDataTable:function -> data, type, mediumItem, meta", data, type, mediumItem, meta);
              let ui = `<div class="btn-group-vertical" role="group">`;
              if (mediumItem.mediumVideo) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload video" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate video" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              } else if (mediumItem.mediumAudio) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload audio" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate audio" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              } else if (mediumItem.mediumImage) {
                if (!mediumItem.fileStatus || mediumItem.fileStatus == 'noFile') {
                  ui += `<button type="button" title="Upload image" class="btn btn-outline-secondary btn-sm mediumItemUploadButton"><i class="fas fa-file-upload"></i></button>`;
                } else {
                  ui += `<button type="button" title="Annotate image" class="btn btn-outline-secondary btn-sm mediumItemAnnotateButton"><i class="fas fa-draw-polygon"></i></button>`;
                }
              }
              ui += `<button type="button" title="Edit data sheet" class="btn btn-outline-secondary btn-sm mediumDatasetsMediumMetadata"><i class="fas fa-file-alt"></i></button>
									</div>`;
              return ui;
            },
          }
        ],
        "language": {
          "decimal": ",",
          "thousands": ".",
          "search": "Search",
          "lengthMenu": "Show _MENU_ entries",
          "zeroRecords": "No records found.",
          "info": "Page _PAGE_ of _PAGES_ &middot; (_MAX_ items total)",
          "infoEmpty": "No items available.",
          "infoFiltered": "(&mdash; _TOTAL_ of _MAX_ items)",
          "paginate": {
            "first": "<<",
            "previous": "<",
            "next": ">",
            "last": ">>"
          },
        },
      });
    },

    setupMediaDataTable: function () {
      // console.log("TCL: setupMediaDataTable");
      // setup dataTable
      this.dataTableMedia = $('#mediumDatasetsMediaListDataTable').DataTable({
        "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
        "order": [[0, 'asc']],
        "pagingType": "full", // "simple_numbers",
        "dom": '<lf<t>ip>',
        "processing": true,
        "stateSave": true,
        "scrollY": "60vh",
        "scrollCollapse": true,
        "scrollX": false,
        "rowId": 'id',
        "serverSide": true,
        "ajax": {
          "url": "api/medium/list",
          "contentType": "application/json; charset=utf-8",
          "dataType": "json",
          "data": function (data) {
            let serverData = {
              draw: data.draw,
              start: data.start,
              length: data.length,
              orderby: data.columns[data.order[0].column].name,
              dir: data.order[0].dir,
              // mediumSubtype: ''
            }
            if (data.search && data.search.value && data.search.value.length > 0)
              serverData.search = data.search.value;
            return serverData;
          },
          "beforeSend": function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
          "dataSrc": function (data) {
            // console.log("TCL: data", data);
            // setup model
            var allMediaList = Array();
            data.data.forEach(function (medium) {
              if (medium.id > 0) {
                allMediaList.push(new TIMAAT.Medium(medium, 'medium'));
              }
            });
            TIMAAT.MediumDatasets.media = allMediaList;
            TIMAAT.MediumDatasets.media.model = data.data;
            return data.data; // data.map(medium => new TIMAAT.Medium(medium));
          }
        },
        "initComplete": async function (settings, json) {
          TIMAAT.MediumDatasets.dataTableMedia.draw(); //* to scroll to selected row
        },
        "drawCallback": function (settings) {
          let api = this.api();
          let i = 0;
          for (; i < api.context[0].aoData.length; i++) {
            if ($(api.context[0].aoData[i].nTr).hasClass('selected')) {
              let index = i + 1;
              let position = $('table tbody > tr:nth-child(' + index + ')').position();
              if (position) {
                $('.dataTables_scrollBody').animate({
                  scrollTop: api.context[0].aoData[i].nTr.offsetTop
                }, 100);
              }
            }
          }
        },
        "rowCallback": function (row, data) {
          // console.log("TCL: row, data", row, data);
          if (data.id == TIMAAT.UI.selectedMediumId) {
            TIMAAT.UI.clearLastSelection('medium');
            $(row).addClass('selected');
            TIMAAT.UI.selectedMediumId = data.id; //* as it is set to null in clearLastSelection
          }
        },
        "createdRow": function (row, data, dataIndex) {
          // console.log("TCL: Media dataTable -  createdRow");
          // console.log("TCL: row, data, dataIndex", row, data, dataIndex);
          let mediumElement = $(row);
          let medium = data;
          medium.ui = mediumElement;
          mediumElement.data('medium', medium);

          mediumElement.on('click', '.title', function (event) {
            event.stopPropagation();
            $('#mediumDatasetsAllMedia').removeClass('list-group-item--is-active');
            let mediumModel = {};
            mediumModel.model = medium;
            let subtype = mediumModel.model.mediaType.mediaTypeTranslations[0].type;
            TIMAAT.MediumDatasets.setDataTableOnItemSelect('medium', mediumModel, subtype);
          });

          // if ( medium.fileStatus != "ready" && medium.fileStatus != "unavailable" && medium.fileStatus != "noFile" )
          // 	medium.fileStatus = TIMAAT.MediumService.updateFileStatus(medium, medium.mediaType.mediaTypeTranslations[0].type);
        },
        "columns": [
          {
            data: 'id', name: 'title', className: 'title', render: function (data, type, medium, meta) {
              // console.log("TCL: data", data); // == mediumId
              // console.log("TCL: medium", medium);
              // console.log("TCL: medium.fileStatus", medium.fileStatus);
              let displayMediumTypeIcon = '';
              switch (medium.mediaType.mediaTypeTranslations[0].type) {
                case 'audio':
                  displayMediumTypeIcon = '  <i class="far fa-file-audio" title="Audio"></i> ';
                  break;
                case 'document':
                  displayMediumTypeIcon = '  <i class="far fa-file-pdf" title="Document"></i> ';
                  break;
                case 'image':
                  displayMediumTypeIcon = '  <i class="far fa-file-image" title="Image"></i> ';
                  break;
                case 'software':
                  displayMediumTypeIcon = '  <i class="fas fa-compact-disc" title="Software"></i> ';
                  break;
                case 'text':
                  displayMediumTypeIcon = '  <i class="far fa-file-alt" title="Text"></i> ';
                  break;
                case 'video':
                  displayMediumTypeIcon = '  <i class="far fa-file-video" title="Video"></i> ';
                  break;
                case 'videogame':
                  displayMediumTypeIcon = '  <i class="fas fa-gamepad" title="Videogame"></i> ';
                  break;
              }
              let noFileIcon = ' ';
              if (medium.fileStatus == 'noFile') {
                noFileIcon = '<i class="fas fa-file-upload" title="No file uploaded"></i> ';
              }
              let commentIcon = ' ';
              if (medium.remark.length > 0) {
                commentIcon = '<i class="fas fa-comment-alt" title="Remark available"></i> ';
              }
              let titleDisplay = `<p>` + displayMediumTypeIcon + noFileIcon + commentIcon + medium.displayTitle.name + `</p>`;
              if (medium.originalTitle != null && medium.displayTitle.id != medium.originalTitle.id) {
                titleDisplay += `<p><i>(OT: ` + medium.originalTitle.name + `)</i></p>`;
              }
              medium.titles.forEach(function (title) { // make additional titles searchable in media library
                if (title.id != medium.displayTitle.id && (medium.originalTitle == null || title.id != medium.originalTitle.id)) {
                  titleDisplay += `<div class="display--none">` + title.name + `</div>`;
                }
              });
              // console.log("TCL: medium", medium);
              return titleDisplay;
            }
          },
        ],
        "language": {
          "decimal": ",",
          "thousands": ".",
          "search": "Search",
          "lengthMenu": "Show _MENU_ entries",
          "zeroRecords": "No media found.",
          "info": "Page _PAGE_ of _PAGES_ &middot; (_MAX_ media total)",
          "infoEmpty": "No media available.",
          "infoFiltered": "(&mdash; _TOTAL_ of _MAX_ media)",
          "paginate": {
            "first": "<<",
            "previous": "<",
            "next": ">",
            "last": ">>"
          },
        },
      });
    },

    setupAudioDataTable: function () {
      // console.log("TCL: setupDataTable");
      // setup dataTable
      this.dataTableAudio = $('#mediumDatasetsAudioTable').DataTable({
        "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
        "order": [[0, 'asc']],
        "pagingType": "full", // "simple_numbers",
        "dom": '<lf<t>ip>',
        "processing": true,
        "stateSave": true,
        "scrollY": "60vh",
        "scrollCollapse": true,
        "scrollX": false,
        "rowId": 'id',
        "serverSide": true,
        "ajax": {
          "url": "api/medium/audio/list",
          "contentType": "application/json; charset=utf-8",
          "dataType": "json",
          "data": function (data) {
            let serverData = {
              draw: data.draw,
              start: data.start,
              length: data.length,
              orderby: data.columns[data.order[0].column].name,
              dir: data.order[0].dir,
              // mediumSubtype: 'audio'
            }
            if (data.search && data.search.value && data.search.value.length > 0)
              serverData.search = data.search.value;
            return serverData;
          },
          "beforeSend": function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
          "dataSrc": function (data) {
            // console.log("TCL: data", data);
            // console.log("TCL: TIMAAT.MediumDatasets.audios (last)", TIMAAT.MediumDatasets.audios);
            // setup model
            var audioList = Array();
            data.data.forEach(function (medium) {
              if (medium.id > 0) {
                audioList.push(new TIMAAT.Medium(medium, 'audio'));
              }
            });
            TIMAAT.MediumDatasets.audios = audioList;
            TIMAAT.MediumDatasets.audios.model = data.data;
            // console.log("TCL: TIMAAT.MediumDatasets.audios (current)", TIMAAT.MediumDatasets.audios);
            return data.data;
          }
        },
        "initComplete": async function (settings, json) {
          TIMAAT.MediumDatasets.dataTableAudio.draw(); //* to scroll to selected row
        },
        "drawCallback": function (settings) {
          let api = this.api();
          let i = 0;
          for (; i < api.context[0].aoData.length; i++) {
            if ($(api.context[0].aoData[i].nTr).hasClass('selected')) {
              let position = $('table tbody > tr:nth-child(' + i + ')').position();
              if (position) {
                $('.dataTables_scrollBody').animate({
                  scrollTop: api.context[0].aoData[i].nTr.offsetTop
                }, 100);
              }
            }
          }
        },
        "rowCallback": function (row, data) {
          // console.log("TCL: row, data", row, data);
          if (data.id == TIMAAT.UI.selectedMediumId) {
            TIMAAT.UI.clearLastSelection('audio');
            $(row).addClass('selected');
            TIMAAT.UI.selectedMediumId = data.id; //* as it is set to null in clearLastSelection
          }
        },
        "createdRow": function (row, data, dataIndex) {
          // console.log("TCL: row, data, dataIndex", row, data, dataIndex);
          let mediumElement = $(row);
          let medium = data;
          medium.ui = mediumElement;
          mediumElement.data('medium', medium);

          mediumElement.on('click', '.title', function (event) {
            event.stopPropagation();
            $('#mediumDatasetsAllAudios').removeClass('active');
            let mediumModel = {};
            mediumModel.model = medium;
            TIMAAT.MediumDatasets.setDataTableOnItemSelect('audio', mediumModel);
          });

          // if ( medium.fileStatus != "ready" && medium.fileStatus != "unavailable" && medium.fileStatus != "noFile" )
          // 	medium.fileStatus = TIMAAT.MediumService.updateFileStatus(medium, 'audio');
        },
        "columns": [
          {
            data: 'id', name: 'title', className: 'title', render: function (data, type, medium, meta) {
              // console.log("TCL: medium.fileStatus", medium.fileStatus);
              let noFileIcon = '';
              if (medium.fileStatus == 'noFile') {
                noFileIcon = '<i class="fas fa-file-upload" title="No file uploaded"></i> ';
              }
              let commentIcon = ' ';
              if (medium.remark.length > 0) {
                commentIcon = '<i class="fas fa-comment-alt" title="Remark available"></i> ';
              }
              let titleDisplay = `<p>` + noFileIcon + commentIcon + medium.displayTitle.name + `</p>`;
              if (medium.originalTitle != null && medium.displayTitle.id != medium.originalTitle.id) {
                titleDisplay += `<p><i>(OT: ` + medium.originalTitle.name + `)</i></p>`;
              }
              medium.titles.forEach(function (title) { // make additional titles searchable in media library
                if (title.id != medium.displayTitle.id && (medium.originalTitle == null || title.id != medium.originalTitle.id)) {
                  titleDisplay += `<div class="display--none">` + title.name + `</div>`;
                }
              });
              return titleDisplay;
            }
          },
        ],
        "language": {
          "decimal": ",",
          "thousands": ".",
          "search": "Search",
          "lengthMenu": "Show _MENU_ entries",
          "zeroRecords": "No audios found.",
          "info": "Page _PAGE_ of _PAGES_ &middot; (_MAX_ audios total)",
          "infoEmpty": "No audios available.",
          "infoFiltered": "(&mdash; _TOTAL_ of _MAX_ audio(s))",
          "paginate": {
            "first": "<<",
            "previous": "<",
            "next": ">",
            "last": ">>"
          },
        },
      });
    },

    setupDocumentDataTable: function () {
      // console.log("TCL: setupDataTable");
      // setup dataTable
      this.dataTableDocument = $('#mediumDatasetsDocumentTable').DataTable({
        "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
        "order": [[0, 'asc']],
        "pagingType": "full", // "simple_numbers",
        "dom": '<lf<t>ip>',
        "processing": true,
        "stateSave": true,
        "scrollY": "60vh",
        "scrollCollapse": true,
        "scrollX": false,
        "rowId": 'id',
        "serverSide": true,
        "ajax": {
          "url": "api/medium/document/list",
          "contentType": "application/json; charset=utf-8",
          "dataType": "json",
          "data": function (data) {
            let serverData = {
              draw: data.draw,
              start: data.start,
              length: data.length,
              orderby: data.columns[data.order[0].column].name,
              dir: data.order[0].dir,
              // mediumSubtype: 'document'
            }
            if (data.search && data.search.value && data.search.value.length > 0)
              serverData.search = data.search.value;
            return serverData;
          },
          "beforeSend": function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
          "dataSrc": function (data) {
            // console.log("TCL: data", data);
            // console.log("TCL: TIMAAT.MediumDatasets.documents (last)", TIMAAT.MediumDatasets.documents);
            // setup model
            var documentList = Array();
            data.data.forEach(function (medium) {
              if (medium.id > 0) {
                documentList.push(new TIMAAT.Medium(medium, 'document'));
              }
            });
            TIMAAT.MediumDatasets.documents = documentList;
            TIMAAT.MediumDatasets.documents.model = data.data;
            // console.log("TCL: TIMAAT.MediumDatasets.documents (current)", TIMAAT.MediumDatasets.documents);
            return data.data;
          }
        },
        "initComplete": async function (settings, json) {
          // console.log("TCL: setupDocumentDataTable:function -> settings, json", settings, json);
          TIMAAT.MediumDatasets.dataTableDocument.draw(); //* to scroll to selected row
        },
        "drawCallback": function (settings) {
          // console.log("TCL_ DataTables has redrawn the table");
          let api = this.api();
          let i = 0;
          for (; i < api.context[0].aoData.length; i++) {
            if ($(api.context[0].aoData[i].nTr).hasClass('selected')) {
              let position = $('table tbody > tr:nth-child(' + i + ')').position();
              if (position) {
                $('.dataTables_scrollBody').animate({
                  scrollTop: api.context[0].aoData[i].nTr.offsetTop
                }, 100);
              }
            }
          }
        },
        "rowCallback": function (row, data) {
          // console.log("TCL: row, data", row, data);
          if (data.id == TIMAAT.UI.selectedMediumId) {
            TIMAAT.UI.clearLastSelection('document');
            $(row).addClass('selected');
            TIMAAT.UI.selectedMediumId = data.id; //* as it is set to null in clearLastSelection
          }
        },
        "createdRow": function (row, data, dataIndex) {
          // console.log("TCL: row, data, dataIndex", row, data, dataIndex);
          let mediumElement = $(row);
          let medium = data;
          medium.ui = mediumElement;
          mediumElement.data('medium', medium);

          mediumElement.on('click', '.title', function (event) {
            event.stopPropagation();
            $('#mediumDatasetsAllDocuments').removeClass('active');
            let mediumModel = {};
            mediumModel.model = medium;
            TIMAAT.MediumDatasets.setDataTableOnItemSelect('document', mediumModel);
          });

          // if ( medium.fileStatus != "ready" && medium.fileStatus != "unavailable" && medium.fileStatus != "noFile" )
          // 	medium.fileStatus = TIMAAT.MediumService.updateFileStatus(medium, 'document');
        },
        "columns": [
          {
            data: 'id', name: 'title', className: 'title', render: function (data, type, medium, meta) {
              // console.log("TCL: medium", medium);
              let noFileIcon = '';
              if (medium.fileStatus == 'noFile') {
                noFileIcon = '<i class="fas fa-file-upload" title="No file uploaded"></i> ';
              }
              let commentIcon = ' ';
              if (medium.remark.length > 0) {
                commentIcon = '<i class="fas fa-comment-alt" title="Remark available"></i> ';
              }
              let titleDisplay = `<p>` + noFileIcon + commentIcon + medium.displayTitle.name + `</p>`;
              if (medium.originalTitle != null && medium.displayTitle.id != medium.originalTitle.id) {
                titleDisplay += `<p><i>(OT: ` + medium.originalTitle.name + `)</i></p>`;
              }
              medium.titles.forEach(function (title) { // make additional titles searchable in media library
                if (title.id != medium.displayTitle.id && (medium.originalTitle == null || title.id != medium.originalTitle.id)) {
                  titleDisplay += `<div class="display--none">` + title.name + `</div>`;
                }
              });
              return titleDisplay;
            }
          },
        ],
        "language": {
          "decimal": ",",
          "thousands": ".",
          "search": "Search",
          "lengthMenu": "Show _MENU_ entries",
          "zeroRecords": "No documents found.",
          "info": "Page _PAGE_ of _PAGES_ &middot; (_MAX_ documents total)",
          "infoEmpty": "No documents available.",
          "infoFiltered": "(&mdash; _TOTAL_ of _MAX_ documents(s))",
          "paginate": {
            "first": "<<",
            "previous": "<",
            "next": ">",
            "last": ">>"
          },
        },
      });
    },

    setupImageDataTable: function () {
      // console.log("TCL: setupDataTable");
      // setup dataTable
      this.dataTableImage = $('#mediumDatasetsImageTable').DataTable({
        "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
        "order": [[0, 'asc']],
        "pagingType": "full", // "simple_numbers",
        "dom": '<lf<t>ip>',
        "processing": true,
        "stateSave": true,
        "scrollY": "60vh",
        "scrollCollapse": true,
        "scrollX": false,
        "rowId": 'id',
        "serverSide": true,
        "ajax": {
          "url": "api/medium/image/list",
          "contentType": "application/json; charset=utf-8",
          "dataType": "json",
          "data": function (data) {
            let serverData = {
              draw: data.draw,
              start: data.start,
              length: data.length,
              orderby: data.columns[data.order[0].column].name,
              dir: data.order[0].dir,
              // mediumSubtype: 'image'
            }
            if (data.search && data.search.value && data.search.value.length > 0)
              serverData.search = data.search.value;
            return serverData;
          },
          "beforeSend": function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
          "dataSrc": function (data) {
            // console.log("TCL: data", data);
            // console.log("TCL: TIMAAT.MediumDatasets.images (last)", TIMAAT.MediumDatasets.images);
            // setup model
            var imageList = Array();
            data.data.forEach(function (medium) {
              if (medium.id > 0) {
                imageList.push(new TIMAAT.Medium(medium, 'image'));
              }
            });
            TIMAAT.MediumDatasets.images = imageList;
            TIMAAT.MediumDatasets.images.model = data.data;
            // console.log("TCL: TIMAAT.MediumDatasets.images (current)", TIMAAT.MediumDatasets.images);
            return data.data;
          }
        },
        "initComplete": async function (settings, json) {
          // console.log("TCL: setupImageDataTable:function -> settings, json", settings, json);
          TIMAAT.MediumDatasets.dataTableImage.draw(); //* to scroll to selected row
        },
        "drawCallback": function (settings) {
          // console.log("TCL_ DataTables has redrawn the table");
          let api = this.api();
          let i = 0;
          for (; i < api.context[0].aoData.length; i++) {
            if ($(api.context[0].aoData[i].nTr).hasClass('selected')) {
              let position = $('table tbody > tr:nth-child(' + i + ')').position();
              if (position) {
                $('.dataTables_scrollBody').animate({
                  scrollTop: api.context[0].aoData[i].nTr.offsetTop
                }, 100);
              }
            }
          }
        },
        "rowCallback": function (row, data) {
          // console.log("TCL: row, data", row, data);
          if (data.id == TIMAAT.UI.selectedMediumId) {
            TIMAAT.UI.clearLastSelection('image');
            $(row).addClass('selected');
            TIMAAT.UI.selectedMediumId = data.id; //* as it is set to null in clearLastSelection
          }
        },
        "createdRow": function (row, data, dataIndex) {
          // console.log("TCL: row, data, dataIndex", row, data, dataIndex);
          let mediumElement = $(row);
          let medium = data;
          medium.ui = mediumElement;
          mediumElement.data('medium', medium);

          mediumElement.on('click', '.title', function (event) {
            event.stopPropagation();
            $('#mediumDatasetsAllImages').removeClass('active');
            let mediumModel = {};
            mediumModel.model = medium;
            TIMAAT.MediumDatasets.setDataTableOnItemSelect('image', mediumModel);
          });

          // if ( medium.fileStatus != "ready" && medium.fileStatus != "unavailable" && medium.fileStatus != "noFile" )
          // 	medium.fileStatus = TIMAAT.MediumService.updateFileStatus(medium, 'image');
        },
        "columns": [
          {
            data: 'id', name: 'title', className: 'title', render: function (data, type, medium, meta) {
              // console.log("TCL: medium", medium);
              let noFileIcon = '';
              if (medium.fileStatus == 'noFile') {
                noFileIcon = '<i class="fas fa-file-upload" title="No file uploaded"></i> ';
              }
              let commentIcon = ' ';
              if (medium.remark.length > 0) {
                commentIcon = '<i class="fas fa-comment-alt" title="Remark available"></i> ';
              }
              let titleDisplay = `<p>` + noFileIcon + commentIcon + medium.displayTitle.name + `</p>`;
              if (medium.originalTitle != null && medium.displayTitle.id != medium.originalTitle.id) {
                titleDisplay += `<p><i>(OT: ` + medium.originalTitle.name + `)</i></p>`;
              }
              medium.titles.forEach(function (title) { // make additional titles searchable in media library
                if (title.id != medium.displayTitle.id && (medium.originalTitle == null || title.id != medium.originalTitle.id)) {
                  titleDisplay += `<div class="display--none">` + title.name + `</div>`;
                }
              });
              return titleDisplay;
            }
          },
        ],
        "language": {
          "decimal": ",",
          "thousands": ".",
          "search": "Search",
          "lengthMenu": "Show _MENU_ entries",
          "zeroRecords": "No images found.",
          "info": "Page _PAGE_ of _PAGES_ &middot; (_MAX_ images total)",
          "infoEmpty": "No images available.",
          "infoFiltered": "(&mdash; _TOTAL_ of _MAX_ image(s))",
          "paginate": {
            "first": "<<",
            "previous": "<",
            "next": ">",
            "last": ">>"
          },
        },
      });
    },

    setupSoftwareDataTable: function () {
      // console.log("TCL: setupDataTable");
      // setup dataTable
      this.dataTableSoftware = $('#mediumDatasetsSoftwareTable').DataTable({
        "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
        "order": [[0, 'asc']],
        "pagingType": "full", // "simple_numbers",
        "dom": '<lf<t>ip>',
        "processing": true,
        "stateSave": true,
        "scrollY": "60vh",
        "scrollCollapse": true,
        "scrollX": false,
        "rowId": 'id',
        "serverSide": true,
        "ajax": {
          "url": "api/medium/software/list",
          "contentType": "application/json; charset=utf-8",
          "dataType": "json",
          "data": function (data) {
            let serverData = {
              draw: data.draw,
              start: data.start,
              length: data.length,
              orderby: data.columns[data.order[0].column].name,
              dir: data.order[0].dir,
              // mediumSubtype: 'software'
            }
            if (data.search && data.search.value && data.search.value.length > 0)
              serverData.search = data.search.value;
            return serverData;
          },
          "beforeSend": function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
          "dataSrc": function (data) {
            // console.log("TCL: data", data);
            // console.log("TCL: TIMAAT.MediumDatasets.softwares (last)", TIMAAT.MediumDatasets.softwares);
            // setup model
            var softwareList = Array();
            data.data.forEach(function (medium) {
              if (medium.id > 0) {
                softwareList.push(new TIMAAT.Medium(medium, 'software'));
              }
            });
            TIMAAT.MediumDatasets.softwares = softwareList;
            TIMAAT.MediumDatasets.softwares.model = data.data;
            // console.log("TCL: TIMAAT.MediumDatasets.softwares (current)", TIMAAT.MediumDatasets.softwares);
            return data.data;
          }
        },
        "initComplete": async function (settings, json) {
          // console.log("TCL: setupSoftwareDataTable:function -> settings, json", settings, json);
          TIMAAT.MediumDatasets.dataTableSoftware.draw(); //* to scroll to selected row
        },
        "drawCallback": function (settings) {
          // console.log("TCL_ DataTables has redrawn the table");
          let api = this.api();
          let i = 0;
          for (; i < api.context[0].aoData.length; i++) {
            if ($(api.context[0].aoData[i].nTr).hasClass('selected')) {
              let position = $('table tbody > tr:nth-child(' + i + ')').position();
              if (position) {
                $('.dataTables_scrollBody').animate({
                  scrollTop: api.context[0].aoData[i].nTr.offsetTop
                }, 100);
              }
            }
          }
        },
        "rowCallback": function (row, data) {
          // console.log("TCL: row, data", row, data);
          if (data.id == TIMAAT.UI.selectedMediumId) {
            TIMAAT.UI.clearLastSelection('software');
            $(row).addClass('selected');
            TIMAAT.UI.selectedMediumId = data.id; //* as it is set to null in clearLastSelection
          }
        },
        "createdRow": function (row, data, dataIndex) {
          // console.log("TCL: row, data, dataIndex", row, data, dataIndex);
          let mediumElement = $(row);
          let medium = data;
          medium.ui = mediumElement;
          mediumElement.data('medium', medium);

          mediumElement.on('click', '.title', function (event) {
            event.stopPropagation();
            $('#mediumDatasetsAllSoftwares').removeClass('active');
            let mediumModel = {};
            mediumModel.model = medium;
            TIMAAT.MediumDatasets.setDataTableOnItemSelect('software', mediumModel);
          });

          // if ( medium.fileStatus != "ready" && medium.fileStatus != "unavailable" && medium.fileStatus != "noFile" )
          // 	medium.fileStatus = TIMAAT.MediumService.updateFileStatus(medium, 'software');
        },
        "columns": [
          {
            data: 'id', name: 'title', className: 'title', render: function (data, type, medium, meta) {
              // console.log("TCL: medium", medium);
              let noFileIcon = '';
              if (medium.fileStatus == 'noFile') {
                noFileIcon = '<i class="fas fa-file-upload" title="No file uploaded"></i> ';
              }
              let commentIcon = ' ';
              if (medium.remark.length > 0) {
                commentIcon = '<i class="fas fa-comment-alt" title="Remark available"></i> ';
              }
              let titleDisplay = `<p>` + noFileIcon + commentIcon + medium.displayTitle.name + `</p>`;
              if (medium.originalTitle != null && medium.displayTitle.id != medium.originalTitle.id) {
                titleDisplay += `<p><i>(OT: ` + medium.originalTitle.name + `)</i></p>`;
              }
              medium.titles.forEach(function (title) { // make additional titles searchable in media library
                if (title.id != medium.displayTitle.id && (medium.originalTitle == null || title.id != medium.originalTitle.id)) {
                  titleDisplay += `<div class="display--none">` + title.name + `</div>`;
                }
              });
              return titleDisplay;
            }
          },
        ],
        "language": {
          "decimal": ",",
          "thousands": ".",
          "search": "Search",
          "lengthMenu": "Show _MENU_ entries",
          "zeroRecords": "No software found.",
          "info": "Page _PAGE_ of _PAGES_ &middot; (_MAX_ software total)",
          "infoEmpty": "No software available.",
          "infoFiltered": "(&mdash; _TOTAL_ of _MAX_ software(s))",
          "paginate": {
            "first": "<<",
            "previous": "<",
            "next": ">",
            "last": ">>"
          },
        },
      });
    },

    setupTextDataTable: function () {
      // console.log("TCL: setupDataTable");
      // setup dataTable
      this.dataTableText = $('#mediumDatasetsTextTable').DataTable({
        "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
        "order": [[0, 'asc']],
        "pagingType": "full", // "simple_numbers",
        "dom": '<lf<t>ip>',
        "processing": true,
        "stateSave": true,
        "scrollY": "60vh",
        "scrollCollapse": true,
        "scrollX": false,
        "rowId": 'id',
        "serverSide": true,
        "ajax": {
          "url": "api/medium/text/list",
          "contentType": "application/json; charset=utf-8",
          "dataType": "json",
          "data": function (data) {
            let serverData = {
              draw: data.draw,
              start: data.start,
              length: data.length,
              orderby: data.columns[data.order[0].column].name,
              dir: data.order[0].dir,
              // mediumSubtype: 'text'
            }
            if (data.search && data.search.value && data.search.value.length > 0)
              serverData.search = data.search.value;
            return serverData;
          },
          "beforeSend": function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
          "dataSrc": function (data) {
            // console.log("TCL: data", data);
            // console.log("TCL: TIMAAT.MediumDatasets.texts (last)", TIMAAT.MediumDatasets.texts);
            // setup model
            var textList = Array();
            data.data.forEach(function (medium) {
              if (medium.id > 0) {
                textList.push(new TIMAAT.Medium(medium, 'text'));
              }
            });
            TIMAAT.MediumDatasets.texts = textList;
            TIMAAT.MediumDatasets.texts.model = data.data;
            // console.log("TCL: TIMAAT.MediumDatasets.texts (current)", TIMAAT.MediumDatasets.texts);
            return data.data;
          }
        },
        "initComplete": async function (settings, json) {
          // console.log("TCL: setupTextDataTable:function -> settings, json", settings, json);
          TIMAAT.MediumDatasets.dataTableText.draw(); //* to scroll to selected row
        },
        "drawCallback": function (settings) {
          // console.log("TCL_ DataTables has redrawn the table");
          let api = this.api();
          let i = 0;
          for (; i < api.context[0].aoData.length; i++) {
            if ($(api.context[0].aoData[i].nTr).hasClass('selected')) {
              let position = $('table tbody > tr:nth-child(' + i + ')').position();
              if (position) {
                $('.dataTables_scrollBody').animate({
                  scrollTop: api.context[0].aoData[i].nTr.offsetTop
                }, 100);
              }
            }
          }
        },
        "rowCallback": function (row, data) {
          // console.log("TCL: row, data", row, data);
          if (data.id == TIMAAT.UI.selectedMediumId) {
            TIMAAT.UI.clearLastSelection('text');
            $(row).addClass('selected');
            TIMAAT.UI.selectedMediumId = data.id; //* as it is set to null in clearLastSelection
          }
        },
        "createdRow": function (row, data, dataIndex) {
          // console.log("TCL: row, data, dataIndex", row, data, dataIndex);
          let mediumElement = $(row);
          let medium = data;
          medium.ui = mediumElement;
          mediumElement.data('medium', medium);

          mediumElement.on('click', '.title', function (event) {
            event.stopPropagation();
            $('#mediumDatasetsAllTexts').removeClass('active');
            let mediumModel = {};
            mediumModel.model = medium;
            TIMAAT.MediumDatasets.setDataTableOnItemSelect('text', mediumModel);
          });

          // if ( medium.fileStatus != "ready" && medium.fileStatus != "unavailable" && medium.fileStatus != "noFile" )
          // 	medium.fileStatus = TIMAAT.MediumService.updateFileStatus(medium, 'text');
        },
        "columns": [
          {
            data: 'id', name: 'title', className: 'title', render: function (data, type, medium, meta) {
              // console.log("TCL: medium", medium);
              let noFileIcon = '';
              if (medium.fileStatus == 'noFile') {
                noFileIcon = '<i class="fas fa-file-upload" title="No file uploaded"></i> ';
              }
              let commentIcon = ' ';
              if (medium.remark.length > 0) {
                commentIcon = '<i class="fas fa-comment-alt" title="Remark available"></i> ';
              }
              let titleDisplay = `<p>` + noFileIcon + commentIcon + medium.displayTitle.name + `</p>`;
              if (medium.originalTitle != null && medium.displayTitle.id != medium.originalTitle.id) {
                titleDisplay += `<p><i>(OT: ` + medium.originalTitle.name + `)</i></p>`;
              }
              medium.titles.forEach(function (title) { // make additional titles searchable in media library
                if (title.id != medium.displayTitle.id && (medium.originalTitle == null || title.id != medium.originalTitle.id)) {
                  titleDisplay += `<div class="display--none">` + title.name + `</div>`;
                }
              });
              return titleDisplay;
            }
          },
        ],
        "language": {
          "decimal": ",",
          "thousands": ".",
          "search": "Search",
          "lengthMenu": "Show _MENU_ entries",
          "zeroRecords": "No texts found.",
          "info": "Page _PAGE_ of _PAGES_ &middot; (_MAX_ texts total)",
          "infoEmpty": "No texts available.",
          "infoFiltered": "(&mdash; _TOTAL_ of _MAX_ text(s))",
          "paginate": {
            "first": "<<",
            "previous": "<",
            "next": ">",
            "last": ">>"
          },
        },
      });
    },

    setupVideoDataTable: function () {
      // console.log("TCL: setupDataTable");
      // setup dataTable
      this.dataTableVideo = $('#mediumDatasetsVideoTable').DataTable({
        "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
        "order": [[0, 'asc']],
        "pagingType": "full", // "simple_numbers",
        "dom": '<lf<t>ip>',
        "processing": true,
        "stateSave": true,
        "scrollY": "60vh",
        "scrollCollapse": true,
        "scrollX": false,
        "rowId": 'id',
        "serverSide": true,
        "ajax": {
          "url": "api/medium/video/list",
          "contentType": "application/json; charset=utf-8",
          "dataType": "json",
          "data": function (data) {
            let serverData = {
              draw: data.draw,
              start: data.start,
              length: data.length,
              orderby: data.columns[data.order[0].column].name,
              dir: data.order[0].dir,
              // mediumSubtype: 'video'
            }
            if (data.search && data.search.value && data.search.value.length > 0)
              serverData.search = data.search.value;
            return serverData;
          },
          "beforeSend": function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
          "dataSrc": function (data) {
            // console.log("TCL: data", data);
            // setup model
            var videoList = Array();
            data.data.forEach(function (medium) {
              if (medium.id > 0) {
                videoList.push(new TIMAAT.Medium(medium, 'video'));
              }
            });
            TIMAAT.MediumDatasets.videos = videoList;
            TIMAAT.MediumDatasets.videos.model = data.data;
            return data.data;
          }
        },
        "initComplete": async function (settings, json) {
          // console.log("TCL: setupVideoDataTable:function -> settings, json", settings, json);
          TIMAAT.MediumDatasets.dataTableVideo.draw(); //* to scroll to selected row
        },
        "drawCallback": function (settings) {
          // console.log("TCL_ DataTables has redrawn the table");
          let api = this.api();
          let i = 0;
          for (; i < api.context[0].aoData.length; i++) {
            if ($(api.context[0].aoData[i].nTr).hasClass('selected')) {
              let position = $('table tbody > tr:nth-child(' + i + ')').position();
              if (position) {
                $('.dataTables_scrollBody').animate({
                  scrollTop: api.context[0].aoData[i].nTr.offsetTop
                }, 100);
              }
            }
          }
        },
        "rowCallback": function (row, data) {
          // console.log("TCL: row, data", row, data);
          if (data.id == TIMAAT.UI.selectedMediumId) {
            TIMAAT.UI.clearLastSelection('video');
            $(row).addClass('selected');
            TIMAAT.UI.selectedMediumId = data.id; //* as it is set to null in clearLastSelection
          }
        },
        "createdRow": function (row, data, dataIndex) {
          // console.log("TCL: row, data, dataIndex", row, data, dataIndex);
          let mediumElement = $(row);
          let medium = data;
          medium.ui = mediumElement;
          mediumElement.data('medium', medium);

          mediumElement.on('click', '.title', function (event) {
            event.stopPropagation();
            $('#mediumDatasetsAllVideos').removeClass('active');
            let mediumModel = {};
            mediumModel.model = medium;
            TIMAAT.MediumDatasets.setDataTableOnItemSelect('video', mediumModel);
          });

          // if ( medium.fileStatus != "ready" && medium.fileStatus != "unavailable" && medium.fileStatus != "noFile" ) {
          // 	console.log("TCL: medium.fileStatus", medium.fileStatus);
          // 	medium.fileStatus = TIMAAT.MediumService.updateFileStatus(medium, 'video');
          // }
          // console.log("TCL: medium.fileStatus", medium.fileStatus);
        },
        "columns": [
          {
            data: 'id', name: 'title', className: 'title', render: function (data, type, medium, meta) {
              // console.log("TCL: medium", medium);
              let noFileIcon = '';
              if (medium.fileStatus == 'noFile') {
                noFileIcon = '<i class="fas fa-file-upload" title="No file uploaded"></i> ';
              }
              let commentIcon = ' ';
              if (medium.remark.length > 0) {
                commentIcon = '<i class="fas fa-comment-alt" title="Remark available"></i> ';
              }
              let titleDisplay = `<p>` + noFileIcon + commentIcon + medium.displayTitle.name + `</p>`;
              if (medium.originalTitle != null && medium.displayTitle.id != medium.originalTitle.id) {
                titleDisplay += `<p><i>(OT: ` + medium.originalTitle.name + `)</i></p>`;
              }
              medium.titles.forEach(function (title) { // make additional titles searchable in media library
                if (title.id != medium.displayTitle.id && (medium.originalTitle == null || title.id != medium.originalTitle.id)) {
                  titleDisplay += `<div class="display--none">` + title.name + `</div>`;
                }
              });
              return titleDisplay;
            }
          },
        ],
        "language": {
          "decimal": ",",
          "thousands": ".",
          "search": "Search",
          "lengthMenu": "Show _MENU_ entries",
          "zeroRecords": "No videos found.",
          "info": "Page _PAGE_ of _PAGES_ &middot; (_MAX_ video total)",
          "infoEmpty": "No videos available.",
          "infoFiltered": "(&mdash; _TOTAL_ of _MAX_ video(s))",
          "paginate": {
            "first": "<<",
            "previous": "<",
            "next": ">",
            "last": ">>"
          },
        },
      });
    },

    updateMediumInAllPlaces: function (mediumId, mediumUpdateFunction) {
      const allMediumVideoRow = this.dataTableAllVideosList?.row('#' + mediumId);
      const allAudioRow = this.dataTableAllAudiosList?.row('#' + mediumId)
      const allMediaRow = this.dataTableAllMediaList?.row('#' + mediumId)
      const mediumVideoRow = this.dataTableVideo?.row('#' + mediumId)
      const mediumAudioRow = this.dataTableAudio?.row('#' + mediumId)
      const mediaRow = this.dataTableMedia?.row('#' + mediumId)

      if (allMediumVideoRow?.length) {
        const currentData = allMediumVideoRow.data()
        allMediumVideoRow.data(mediumUpdateFunction(currentData))
      }
      if (allAudioRow?.length) {
        const currentData = allAudioRow.data()
        allAudioRow.data(mediumUpdateFunction(currentData))
      }
      if (allMediaRow?.length) {
        const currentData = allMediaRow.data()
        allMediaRow.data(mediumUpdateFunction(currentData))
      }

      if (mediumVideoRow?.length) {
        const currentData = mediumVideoRow.data()
        mediumVideoRow.data(mediumUpdateFunction(currentData))
      }
      if (mediumAudioRow?.length) {
        const currentData = mediumAudioRow.data()
        mediumAudioRow.data(mediumUpdateFunction(currentData))
      }
      if (mediaRow?.length) {
        const currentData = mediaRow.data()
        mediaRow.data(mediumUpdateFunction(currentData))
      }

      const currentVideo = this.videos?.find(video => video.model.id === mediumId)
      if (currentVideo) {
        currentVideo.model = mediumUpdateFunction(currentVideo.model)
      }

      const currentMedia = this.media?.find(media => media.model.id === mediumId)
      if (currentMedia) {
        currentMedia.model = mediumUpdateFunction(currentMedia.model)
      }
      const currentAudio = this.audios?.find(audio => audio.model.id === mediumId)
      if (currentAudio) {
        currentAudio.model = mediumUpdateFunction(currentAudio.model)
      }

      const mediumFormMetadataMedium = $('#mediumFormMetadata').data('medium')
      if (mediumFormMetadataMedium?.model.id === mediumId) {
        mediumFormMetadataMedium.model = mediumUpdateFunction(mediumFormMetadataMedium.model)
      }
    },

    handleMediumUpdateMessage: function (mediumUpdateMessage) {
      /**
       * Currently we only consume CHANGE messages updating the default transcription id of a medium
       */
      if (mediumUpdateMessage.type === "CHANGE" && "defaultTranscriptionId" in mediumUpdateMessage.entity) {
        const mediumUpdateFunction = (currentMedium) => {
          return {...currentMedium, ...mediumUpdateMessage.entity}
        }

        TIMAAT.MediumDatasets.updateMediumInAllPlaces(mediumUpdateMessage.id, mediumUpdateFunction)
        const mediumFormMetadataMedium = $('#mediumFormMetadata').data('medium')

        if (mediumFormMetadataMedium.model.id === mediumUpdateMessage.id) {
          const $mediumTranscriptionTab = $("#mediumTranscriptionTab")
          const $mediumPreviewTab = $("#mediumPreviewTab")

          if ($mediumTranscriptionTab.hasClass("active")) {
            TIMAAT.MediumDatasets.updateDefaultTranscriptionBadgePositionToCurrentMediumDefaultTranscription()
          } else if ($mediumPreviewTab.hasClass("active")) {
            console.log("Update transcription of preview")
            TIMAAT.MediumDatasets.updateFormPreviewSubtitle(mediumFormMetadataMedium)
          }
        }
      }
    },

    handleMediumAudioAnalysisUpdateMessage: function (mediumAudioAnalysisUpdateMessage) {
      if (mediumAudioAnalysisUpdateMessage.type === "CHANGE") {
        const mediumAudioAnalysisUpdateFunction = (currentMedium) => {
          const updatedMedium = {...currentMedium}
          updatedMedium.mediumAudioAnalysis = {...updatedMedium.mediumAudioAnalysis, ...mediumAudioAnalysisUpdateMessage.entity}
          return updatedMedium
        }

        TIMAAT.MediumDatasets.updateMediumInAllPlaces(mediumAudioAnalysisUpdateMessage.id, mediumAudioAnalysisUpdateFunction)
      }
    },

    setupVideogameDataTable: function () {
      // console.log("TCL: setupDataTable");
      // setup dataTable
      this.dataTableVideogame = $('#mediumDatasetsVideogameTable').DataTable({
        "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
        "order": [[0, 'asc']],
        "pagingType": "full", // "simple_numbers",
        "dom": '<lf<t>ip>',
        "processing": true,
        "stateSave": true,
        "scrollY": "60vh",
        "scrollCollapse": true,
        "scrollX": false,
        "rowId": 'id',
        "serverSide": true,
        "ajax": {
          "url": "api/medium/videogame/list",
          "contentType": "application/json; charset=utf-8",
          "dataType": "json",
          "data": function (data) {
            let serverData = {
              draw: data.draw,
              start: data.start,
              length: data.length,
              orderby: data.columns[data.order[0].column].name,
              dir: data.order[0].dir,
              // mediumSubtype: 'videogame'
            }
            if (data.search && data.search.value && data.search.value.length > 0)
              serverData.search = data.search.value;
            return serverData;
          },
          "beforeSend": function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
          "dataSrc": function (data) {
            // console.log("TCL: data", data);
            // console.log("TCL: TIMAAT.MediumDatasets.videogames (last)", TIMAAT.MediumDatasets.videogames);
            // setup model
            var videogameList = Array();
            data.data.forEach(function (medium) {
              if (medium.id > 0) {
                videogameList.push(new TIMAAT.Medium(medium, 'videogame'));
              }
            });
            TIMAAT.MediumDatasets.videogames = videogameList;
            TIMAAT.MediumDatasets.videogames.model = data.data;
            // console.log("TCL: TIMAAT.MediumDatasets.videogames (current)", TIMAAT.MediumDatasets.videogames);
            return data.data;
          }
        },
        "initComplete": async function (settings, json) {
          // console.log("TCL: setupVideogameDataTable:function -> settings, json", settings, json);
          TIMAAT.MediumDatasets.dataTableVideogame.draw(); //* to scroll to selected row
        },
        "drawCallback": function (settings) {
          // console.log("TCL_ DataTables has redrawn the table");
          let api = this.api();
          let i = 0;
          for (; i < api.context[0].aoData.length; i++) {
            if ($(api.context[0].aoData[i].nTr).hasClass('selected')) {
              let position = $('table tbody > tr:nth-child(' + i + ')').position();
              if (position) {
                $('.dataTables_scrollBody').animate({
                  scrollTop: api.context[0].aoData[i].nTr.offsetTop
                }, 100);
              }
            }
          }
        },
        "rowCallback": function (row, data) {
          // console.log("TCL: row, data", row, data);
          if (data.id == TIMAAT.UI.selectedMediumId) {
            TIMAAT.UI.clearLastSelection('videogame');
            $(row).addClass('selected');
            TIMAAT.UI.selectedMediumId = data.id; //* as it is set to null in clearLastSelection
          }
        },
        "createdRow": function (row, data, dataIndex) {
          // console.log("TCL: row, data, dataIndex", row, data, dataIndex);
          let mediumElement = $(row);
          let medium = data;
          medium.ui = mediumElement;
          mediumElement.data('medium', medium);

          mediumElement.on('click', '.title', function (event) {
            event.stopPropagation();
            $('#mediumDatasetsAllVideogames').removeClass('active');
            let mediumModel = {};
            mediumModel.model = medium;
            TIMAAT.MediumDatasets.setDataTableOnItemSelect('videogame', mediumModel);
          });

          // if ( medium.fileStatus != "ready" && medium.fileStatus != "unavailable" && medium.fileStatus != "noFile" )
          // 	medium.fileStatus = TIMAAT.MediumService.updateFileStatus(medium, 'videogame');
        },
        "columns": [
          {
            data: 'id', name: 'title', className: 'title', render: function (data, type, medium, meta) {
              // console.log("TCL: medium", medium);
              let noFileIcon = '';
              if (medium.fileStatus == 'noFile') {
                noFileIcon = '<i class="fas fa-file-upload" title="No file uploaded"></i> ';
              }
              let commentIcon = ' ';
              if (medium.remark.length > 0) {
                commentIcon = '<i class="fas fa-comment-alt" title="Remark available"></i> ';
              }
              let titleDisplay = `<p>` + noFileIcon + commentIcon + medium.displayTitle.name + `</p>`;
              if (medium.originalTitle != null && medium.displayTitle.id != medium.originalTitle.id) {
                titleDisplay += `<p><i>(OT: ` + medium.originalTitle.name + `)</i></p>`;
              }
              medium.titles.forEach(function (title) { // make additional titles searchable in media library
                if (title.id != medium.displayTitle.id && (medium.originalTitle == null || title.id != medium.originalTitle.id)) {
                  titleDisplay += `<div class="display--none">` + title.name + `</div>`;
                }
              });
              return titleDisplay;
            }
          },
        ],
        "language": {
          "decimal": ",",
          "thousands": ".",
          "search": "Search",
          "lengthMenu": "Show _MENU_ entries",
          "zeroRecords": "No videogames found.",
          "info": "Page _PAGE_ of _PAGES_ &middot; (_MAX_ videogames total)",
          "infoEmpty": "No videogames available.",
          "infoFiltered": "(&mdash; _TOTAL_ of _MAX_ videogame(s))",
          "paginate": {
            "first": "<<",
            "previous": "<",
            "next": ">",
            "last": ">>"
          },
        },
      });
    },

    setDataTableOnItemSelect: function (type, selectedItem, subtype = null) {
      // console.log("TCL: setDataTableOnItemSelect:function -> type, selectedItem", type, selectedItem);
      // show tag editor - trigger popup
      TIMAAT.UI.hidePopups();
      $('#mediumDatasetsMediumTabsContainer').append($('#mediumDatasetsMediumTabs'));
      $('#mediumModalsContainer').append($('#mediumModals'));
      this.container = 'media';
      $('#mediumPreviewDataTab').removeClass('annotationMode');
      switch (TIMAAT.UI.subNavTab) {
        case 'dataSheet':
          TIMAAT.UI.displayDataSetContentContainer('mediumDataTab', 'mediumFormMetadata', 'medium', subtype);
          break;
        case 'preview':
          TIMAAT.UI.displayDataSetContentContainer('mediumDataTab', 'mediumFormPreview', 'medium', subtype);
          break;
        case 'titles':
          TIMAAT.UI.displayDataSetContentContainer('mediumDataTab', 'mediumFormTitles', 'medium', subtype);
          break;
        case 'languageTracks':
          TIMAAT.UI.displayDataSetContentContainer('mediumDataTab', 'mediumFormLanguageTracks', 'medium', subtype);
          break;
        case 'actorWithRoles':
          TIMAAT.UI.displayDataSetContentContainer('mediumDataTab', 'mediumFormActorWithRoles', 'medium', subtype);
          break;
      }
      TIMAAT.UI.clearLastSelection(type);
      // TIMAAT.UI.addSelectedClassToSelectedItem(type, selectedItem.model.id); // is done in displayDataSetContent
      if (type == 'medium') {
        if (TIMAAT.UI.subNavTab == 'dataSheet') {
          TIMAAT.URLHistory.setURL(null, selectedItem.model.displayTitle.name + ' · Datasets · ' + type[0].toUpperCase() + type.slice(1), '#medium/' + selectedItem.model.id);
        } else {
          TIMAAT.URLHistory.setURL(null, selectedItem.model.displayTitle.name + ' · Datasets · ' + type[0].toUpperCase() + type.slice(1), '#medium/' + selectedItem.model.id + '/' + TIMAAT.UI.subNavTab);
        }
        type = selectedItem.model.mediaType.mediaTypeTranslations[0].type;
      } else {
        if (TIMAAT.UI.subNavTab == 'dataSheet') {
          TIMAAT.URLHistory.setURL(null, selectedItem.model.displayTitle.name + ' · Datasets · ' + type[0].toUpperCase() + type.slice(1), '#medium/' + type + '/' + selectedItem.model.id);
        } else {
          TIMAAT.URLHistory.setURL(null, selectedItem.model.displayTitle.name + ' · Datasets · ' + type[0].toUpperCase() + type.slice(1), '#medium/' + type + '/' + selectedItem.model.id + '/' + TIMAAT.UI.subNavTab);
        }
      }
      $('#mediumFormMetadata').data('type', type);
      $('#mediumFormMetadata').data('medium', selectedItem);
      this.showAddMediumButton();
      TIMAAT.UI.displayDataSetContent(TIMAAT.UI.subNavTab, selectedItem, 'medium');
    },

    updateVideoStatus: function (medium) {
      // console.log("TCL: updateVideoStatus: function(medium)", medium);
      medium.poll = window.setInterval(function () {
        if (medium.ui && !medium.ui.is(':visible')) return;
        jQuery.ajax({
          url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/video/" + medium.id + '/status',
          type: "GET",
          beforeSend: function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
        }).done(function (data) {
          // console.log("TCL: data", data);
          if (medium.fileStatus && medium.fileStatus == data) return;
          medium.fileStatus = data;

          this.setMediumStatus(medium);

          if (medium.fileStatus == 'unavailable' || medium.fileStatus == 'ready')
            window.clearInterval(medium.poll);
          // console.log("TCL: medium.fileStatus", medium.fileStatus);
        })
            .fail(function (error) {
              // TODO handle error
              window.clearInterval(medium.poll);
              medium.ui.find('.js-medium-file-status').html('<i class="fas fa-eye-slash"></i> not available');
              console.error("ERROR: ", error);
            });

      }, Math.round(30000 + (Math.random() * 15000)));

    },

  }

}, window));
