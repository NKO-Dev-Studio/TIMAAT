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

  // define an AMD module that relies on 'TIMAAT' and wavesurfer
  if (typeof define === 'function' && define.amd) {
    define(['TIMAAT', 'wavesurfer'], factory);


    // define a Common JS module that relies on 'TIMAAT'
  } else if (typeof exports === 'object') {
    module.exports = factory(require('TIMAAT'), require('wavesurfer'));
  }

  // attach your plugin to the global 'TIMAAT' variable
  if (typeof window !== 'undefined' && window.TIMAAT && window.WaveSurfer) {
    factory(window.TIMAAT, window.WaveSurfer);
  }

}(function (TIMAAT, WaveSurfer) {

  TIMAAT.VideoPlayer = {
    UI: Object(),
    annotationList: [],
    curAction: null,
    curAnalysisList: null,
    curAnnotation: null,
    curCategorySet: null,
    curMusic: null,
    curMusicArticulationElement: null,
    curMusicChangeInTempoElement: null,
    curMusicDynamicsElement: null,
    curMusicFormElement: null,
    curMusicTextSettingElement: null,
    curScene: null,
    curSegment: null,
    curSequence: null,
    curTake: null,
    currentPermissionLevel: null,
    duration: 1,
    markerList: [],
    model: Object(),
    overlay: null,
    repeatMode: "NONE", //one of NONE | ANNOTATION | SELECTION
    subtitleEnabled: false,
    selectedElementType: null,
    selectedMedium: null,
    tagAutocomplete: [],
    userPermissionList: null,
    volume: 1,
    currentTemporaryWaveformMarker: null,
    mediumFrequencyInformation: null,
    selectionFrequencyInformation: null,

    init: function () {
      // init UI
      $('.mediaPlayerNoMediumAvailable').show();
      $('.mediaPlayerAnalysisArea').hide();
      $('.mediumPlayerTimelineArea').hide();
      $('.mediumPlayerMediumDatasetArea').hide();

      this.viewer = L.map('mediaPlayerViewer', {
        attributionControl: false,
        center: [0, 0],
        crs: L.CRS.Simple,
        editable: true,
        keyboard: false,
        maxZoom: 0.0,
        minZoom: 0.0,
        zoomControl: false,
        zoomSnap: 0.000001,
      });

      let bounds = [[450, 0], [0, 800]];
      TIMAAT.VideoPlayer.viewer.setMaxBounds(bounds);
      TIMAAT.VideoPlayer.viewer.fitBounds(bounds);
      TIMAAT.VideoPlayer.viewer.dragging.disable();
      TIMAAT.VideoPlayer.viewer.touchZoom.disable();
      TIMAAT.VideoPlayer.viewer.doubleClickZoom.disable();
      TIMAAT.VideoPlayer.viewer.scrollWheelZoom.disable();

      // adjust timeline view upon window resize
      $(window).resize(function () {
        TIMAAT.VideoPlayer.timeline.invalidateSize();
        let selected = TIMAAT.VideoPlayer.selectedElementType;
        // TIMAAT.VideoPlayer.refreshTimelineElementsStructure();
        let index;
        TIMAAT.VideoPlayer.selectedElementType = selected;
        if (TIMAAT.VideoPlayer.curAnalysisList != null) {
          for (let marker of TIMAAT.VideoPlayer.markerList) marker._updateElementOffset();
          for (let anno of TIMAAT.VideoPlayer.annotationList) for (let keyframe of anno.svg.keyframes) keyframe._updateOffsetUI();
        }
      });

      this.initNotifications();
      this.initMenu();
      this.initAnimationControls();
      this.initInspectorControls();
      this.initTimeLineControls();
      this.initVideoPlayerControls();

      this.waveformContainer = $(".timeline__audio-waveform")
      this.waveformContainer.one("mousedown", TIMAAT.VideoPlayer.handleTemporaryWaveformDragStart)

      TIMAAT.EntityUpdate.registerEntityUpdateListener("medium-audio-analysis", this.handleMediumAudioAnalysisEntityUpdateMessage.bind(this))
      TIMAAT.EntityUpdate.registerEntityUpdateListener("medium", this.handleMediumUpdateMessage.bind(this))
      TIMAAT.EntityUpdate.registerEntityUpdateListener("transcription", this.handleTranscriptionUpdateMessage.bind(this))

      $('#retryWaveformGenerationButton').attr('onclick', 'TIMAAT.VideoPlayer.triggerWaveformGeneration()');

      $('#audioWaveformTimelineCollapseWidgetIcon').on("click", () => {
        TIMAAT.VideoPlayer.drawWaveform()
      })
    },

    initNotifications: function () {
      // segment created remotely
      $(document).on('addSegment.notification.TIMAAT', function (ev, notification) {
        let segment = new TIMAAT.AnalysisSegment(notification.data);
        if (segment && TIMAAT.VideoPlayer.curAnalysisList && TIMAAT.VideoPlayer.curAnalysisList.id == notification.dataID) {
          TIMAAT.VideoPlayer._segmentAdded(segment, false);
        }
      });

      // segment edited remotely
      $(document).on('editSegment.notification.TIMAAT', function (ev, notification) {
        let segment = notification.data;
        if (segment && TIMAAT.VideoPlayer.curAnalysisList && TIMAAT.VideoPlayer.curAnalysisList.id == notification.dataID) {
          let localSegment = null;
          for (let seg of TIMAAT.VideoPlayer.curAnalysisList.analysisSegmentsUI) if (seg.model.id == segment.id) localSegment = seg;
          if (localSegment) {
            // update local segment
            localSegment.model = segment;
            TIMAAT.VideoPlayer.sort(TIMAAT.VideoPlayer.curAnalysisList.analysisSegmentsUI);
            // TIMAAT.VideoPlayer.sortSegments();
            // update UI list view
            localSegment.updateUI();
            // console.log("TCL: $ -> TIMAAT.VideoPlayer.updateListUI()");
            TIMAAT.VideoPlayer.updateListUI();
            TIMAAT.VideoPlayer.sortListUI();
            TIMAAT.VideoPlayer.inspector.updateItem();
          }
        }
      });

      // segment deleted remotely
      $(document).on('removeSegment.notification.TIMAAT', function (ev, notification) {
        let segment = new TIMAAT.AnalysisSegment(notification.data);
        if (segment && TIMAAT.VideoPlayer.curAnalysisList && TIMAAT.VideoPlayer.curAnalysisList.id == notification.dataID) {
          let localSegment = null;
          for (let seg of TIMAAT.VideoPlayer.curAnalysisList.analysisSegmentsUI) if (seg.model.id == segment.model.id) localSegment = seg;
          if (localSegment) {
            // remove local segment
            TIMAAT.VideoPlayer.curAnalysisList.analysisSegmentsUI.splice(TIMAAT.VideoPlayer.curAnalysisList.analysisSegmentsUI.indexOf(localSegment), 1);
            localSegment.removeUI();
            if (TIMAAT.VideoPlayer.inspector.state.item == localSegment) TIMAAT.VideoPlayer.inspector.setItem(null);
          }
        }
      });

      // annotation created remotely
      $(document).on('addAnnotation.notification.TIMAAT', function (ev, notification) {
        let annotation = new TIMAAT.Annotation(notification.data);
        if (annotation && TIMAAT.VideoPlayer.curAnalysisList && TIMAAT.VideoPlayer.curAnalysisList.id == notification.dataID) {
          TIMAAT.VideoPlayer._annotationAdded(annotation, false);
        }
      });

      // annotation edited remotely
      $(document).on('editAnnotation.notification.TIMAAT', function (ev, notification) {
        // console.log("trigger edit annotation");
        let annotation = notification.data;
        // console.log("remote annotation", annotation);
        if (annotation && TIMAAT.VideoPlayer.curAnalysisList && TIMAAT.VideoPlayer.curAnalysisList.id == notification.dataID) {
          // console.log("in local list");
          let localAnno = null;
          for (let anno of TIMAAT.VideoPlayer.annotationList) if (anno.model.id == annotation.id) localAnno = anno;
          if (localAnno) {
            // console.log("local annotation found", localAnno);
            // update local annotation
            localAnno.model = annotation;
            localAnno.setChanged();
            localAnno.discardChanges();
            // update UI list view
            localAnno.updateUI();
            TIMAAT.VideoPlayer.updateUI();
            // console.log("TCL: $ -> TIMAAT.VideoPlayer.updateListUI()");
            TIMAAT.VideoPlayer.updateListUI();
            TIMAAT.VideoPlayer.sortListUI();
            if (TIMAAT.VideoPlayer.inspector.state.item == localAnno) TIMAAT.VideoPlayer.inspector.setItem(localAnno, 'annotation');
          }
        }
      });

      // annotation deleted remotely
      $(document).on('removeAnnotation.notification.TIMAAT', function (ev, notification) {
        let annotation = notification.data;
        if (annotation && TIMAAT.VideoPlayer.curAnalysisList && TIMAAT.VideoPlayer.curAnalysisList.id == notification.dataID) {
          let localAnno = null;
          for (let anno of TIMAAT.VideoPlayer.annotationList) if (anno.model.id == annotation.id) localAnno = anno;
          if (localAnno) {
            // remove local annotation
            TIMAAT.VideoPlayer.annotationList.splice(TIMAAT.VideoPlayer.annotationList.indexOf(localAnno), 1);
            // remove from model list
            let anno = TIMAAT.VideoPlayer.curAnalysisList.annotations.find(x => x.id === localAnno.model.id);
            let index = TIMAAT.VideoPlayer.curAnalysisList.annotations.indexOf(anno);
            if (index > -1) TIMAAT.VideoPlayer.curAnalysisList.annotations.splice(index, 1);

            // update UI list view
            localAnno.remove();
            // console.log("TCL: $ -> TIMAAT.VideoPlayer.updateListUI()");
            // TODO Why is sorting again necessary?
            TIMAAT.VideoPlayer.updateListUI();
            TIMAAT.VideoPlayer.sortListUI();
            if (TIMAAT.VideoPlayer.inspector.state.item == localAnno) TIMAAT.VideoPlayer.selectAnnotation(null);
          }
        }
      });
    },

    initLogging: function () {
      $('#analysisUserLog').popover({
        container: 'body',
        html: true,
        title: '<i class="fas fa-user"></i> Editing log',
        content: '<div class="userLogDetails">Loading...</div>',
        placement: 'bottom',
        trigger: 'manual',
      });

      $('#analysisUserLog').on('inserted.bs.popover', function () {
        if (!TIMAAT.VideoPlayer.curAnalysisList) {
          $('.userLogDetails').html("No analysis selected");
          return;
        }
        $('.userLogDetails').html(
            '<b><i class="fas fa-plus-square"></i> Created by <span class="userId" data-user-id="' + TIMAAT.VideoPlayer.curAnalysisList.createdByUserAccountId + '">[ID ' + TIMAAT.VideoPlayer.curAnalysisList.createdByUserAccountId + ']</span></b><br>\
						 ' + TIMAAT.Util.formatDate(TIMAAT.VideoPlayer.curAnalysisList.createdAt) + '<br>'
        );
        $('.userLogDetails').find('.userId').each(function (index, item) {
          TIMAAT.Util.resolveUserID(item, "me")
        });
      });
    },

    initMenu: function () {
      // setup analysis lists UI and events
      $('#analysisChooser').on('change', async function (ev) {
        TIMAAT.VideoPlayer.inspector.reset();
        var list = TIMAAT.VideoPlayer.model.analysisLists.find(x => x.id === parseInt($(this).val()));
        if (list && TIMAAT.VideoPlayer.curAnalysisList && list.id != TIMAAT.VideoPlayer.curAnalysisList.id || list && !TIMAAT.VideoPlayer.curAnalysisList) {
          await TIMAAT.VideoPlayer.loadAnalysisList(list.id);
          TIMAAT.URLHistory.setURL(null, 'Analysis · ' + list.mediumAnalysisListTranslations[0].title, '#analysis/' + list.id);
        }
      });

      $('#analysisManageModal').on('change paste keyup', '#userAccountForNewPermission', function (event) {
        $('#adminCanNotBeAddedInfo').hide();
        $('#userAccountDoesNotExistInfo').hide();
        $('#userAccountAlreadyInList').hide();
      });

      $('#analysisManageModal').on('blur', '.custom-select', function (event) {
        $('[id^="adminCannotBeChanged_"]').hide();
      });

      $(document).on('click', '[data-role="newUserPermissionAnalysisList"] > [data-role="add"]', async function (event) {
        event.preventDefault();
        let listEntry = $(this).closest('[data-role="newPermission"]');
        let displayName = '';
        let permissionId = null;
        if (listEntry.find('input').each(function () {
          displayName = $(this).val();
        })) ;
        if (listEntry.find('select').each(function () {
          permissionId = $(this).val();
        })) ;

        if (displayName == '') return; // no data entered

        // 'admin' can't be added as admin always has access
        if (displayName.toLowerCase() == 'admin') {
          $('#adminCanNotBeAddedInfo').show();
          return;
        }
        // check if name exists
        // TODO make check case insensitive
        let displayNameExists = await TIMAAT.Service.displayNameExists(displayName);
        if (!displayNameExists) {
          $('#userAccountDoesNotExistInfo').show();
          return;
        }

        // check if name is already in the list
        let displayNameDuplicate = false;
        let i = 0;
        for (; i < TIMAAT.VideoPlayer.userPermissionList.length; i++) {
          if ($('#userAccountForNewPermission').val() == TIMAAT.VideoPlayer.userPermissionList[i].displayName) {
            displayNameDuplicate = true;
            break;
          }
        }
        if (displayNameDuplicate) {
          $('#userAccountAlreadyInList').show();
          return;
        }

        // add new entry to the list
        let userAccountId = await TIMAAT.Service.getUserAccountIdByDisplayName(displayName);
        await TIMAAT.AnalysisListService.addUserAccountHasMediumAnalysisListWithPermission(userAccountId, TIMAAT.VideoPlayer.curAnalysisList.id, permissionId);
        TIMAAT.VideoPlayer.manageAnalysisList();
      });

      $(document).on('change', '[data-role="changeUserPermissionAnalysisList"] > [data-role="select"]', async function (event) {
        event.preventDefault();
        let userId = $(this).closest('.permissionContainer')[0].dataset.userId;
        let permissionId = $(this).closest('.custom-select').val();

        // prevent removal of the last admin. One admin has to exist at any time
        let adminCount = 0;
        $('.permissionContainer [data-role="select"]').each(function () {
          if ($(this).val() == 4) {
            adminCount++;
          }
        });
        if (adminCount <= 0) {
          $(this).closest('.custom-select').val(4); // return invalidly changed option back to 'Administrate'
          $('#adminCannotBeChanged_' + userId).show();
          return;
        }

        await TIMAAT.AnalysisListService.updateUserAccountHasMediumAnalysisListWithPermission(userId, TIMAAT.VideoPlayer.curAnalysisList.id, permissionId);
        TIMAAT.VideoPlayer.userPermissionList = await TIMAAT.AnalysisListService.getDisplayNamesAndPermissions(TIMAAT.VideoPlayer.curAnalysisList.id);
      });

      $(document).on('change', 'input[type=radio][name=globalPermissionAnalysisList]', async function (event) {
        event.preventDefault();
        let globalPermissionValue = Number($(this).val());
        if (!globalPermissionValue || globalPermissionValue == null || globalPermissionValue > 2) globalPermissionValue = 0;
        let analysisList = TIMAAT.VideoPlayer.curAnalysisList;
        analysisList.globalPermission = globalPermissionValue;
        TIMAAT.AnalysisListService.updateMediumAnalysisList(analysisList);
      });

      $(document).on('click', '[data-role="removeUserPermissionAnalysisList"] > [data-role="remove"]', async function (event) {
        event.preventDefault();
        let userId = $(this).closest('.permissionContainer')[0].dataset.userId;
        let index = TIMAAT.VideoPlayer.userPermissionList.findIndex(({userAccountId}) => userAccountId == userId);
        let userPermissionId = TIMAAT.VideoPlayer.userPermissionList[index].permissionId;
        if (!userPermissionId) return;

        // if the to be removed user has administrate permission, make sure that she is not the only one
        if (userPermissionId == 4) {
          // prevent removal of the last admin. One admin has to exist at any time
          let adminCount = 0;
          $('.permissionContainer [data-role="select"]').each(function () {
            if ($(this).val() == 4) {
              adminCount++;
            }
          });
          if (adminCount <= 1) {
            $('#adminCannotBeChanged_' + userId).show();
            return;
          }
        }

        await TIMAAT.AnalysisListService.removeUserAccountHasMediumAnalysisList(userId, TIMAAT.VideoPlayer.curAnalysisList.id);
        $(this).closest('.permissionContainer').remove();
        TIMAAT.VideoPlayer.userPermissionList.splice(index, 1);
      });

      // publication dialog events
      $('#publishAnalysisSwitch, #publicationProtectedSwitch').on('change', ev => {
        // TIMAAT.VideoPlayer._setupPublicationDialog($('#publishAnalysisSwitch').prop('checked'), $('#publicationProtectedSwitch').prop('checked'));
        let enabled = $('#publishAnalysisSwitch').prop('checked');
        let restricted = $('#publicationProtectedSwitch').prop('checked');
        let credentials = {};
        try {
          credentials = JSON.parse(TIMAAT.VideoPlayer.publication.credentials);
        } catch (e) {
          credentials = {};
        }
        let dialog = $('#analysisListPublicationModal');
        let username = (credentials.user && enabled) ? credentials.user : '';
        let password = (credentials.password && enabled) ? credentials.password : '';
        dialog.find('.icon--protected').removeClass('fa-lock').removeClass('fa-lock-open');
        if (restricted) dialog.find('.icon--protected').addClass('fa-lock'); else dialog.find('.icon--protected').addClass('fa-lock-open');
        dialog.find('#publicationProtectedSwitch').prop('disabled', !enabled);
        dialog.find('.username').prop('disabled', !enabled || !restricted);
        dialog.find('.username').val(username);
        dialog.find('.password').prop('disabled', !enabled || !restricted);
        dialog.find('.password').val(password);
        dialog.find('.password').attr('type', 'password');
        $('#publicationSettingsSubmitButton').prop('disabled', enabled && restricted && username == '' && password == '');
      });

      let dialog = $('#analysisListPublicationModal');

      dialog.find('.reveal').on('click', ev => {
        if (dialog.find('.password').attr('type') === 'password')
          dialog.find('.password').attr('type', 'text');
        else dialog.find('.password').attr('type', 'password');
      });

      dialog.find('.username, .password').on('change input', ev => {
        let enabled = $('#publishAnalysisSwitch').prop('checked');
        let restricted = $('#publicationProtectedSwitch').prop('checked');
        let username = dialog.find('.username').val();
        let password = dialog.find('.password').val();
        $('#publicationSettingsSubmitButton').prop('disabled', enabled && restricted && username == '' && password == '');
      });

      $('#publicationSettingsSubmitButton').on('click', ev => {
        TIMAAT.VideoPlayer._updatePublicationSettings();
      })
    },

    initAnimationControls: function () {
      // animation player shape updater
      let animFrameRate = 20;
      TIMAAT.VideoPlayer.animInterval = setInterval(this._updateAnimations, 1000 / animFrameRate);

      // animation keyframe control
      TIMAAT.VideoPlayer.animCtrl = L.control.custom({
        position: 'topleft',
        content: `<div class="text-center bg-light border-bottom" onclick="TIMAAT.VideoPlayer.inspector.open('inspectorAnimation')">Keyframes</div>
								<div class="animation-keyframe-controls">
									<div class="btn-group btn-group-sm">
										<button title="select previous keyframe" id="videoPlayerPreviousKeyframeButton" onclick="void(0)" type="button" class="btn btn-light">
											<i class="fas fa-arrow-left"></i>
										</button>
									<div title="current keyframe" class="btn btn-light active" ondblclick="TIMAAT.VideoPlayer.inspector.open('inspectorAnimation')">
										<i class="fas fa-fw keyframeInfo">2</i>
									</div>
									<button title="select next keyframe" id="videoPlayerNextKeyframeButton" onclick="void(0)" type="button" class="btn btn-light" disabled="">
										<i class="fas fa-arrow-right"></i>
										</button>
									</div>
								</div>
								<button title="new keyframe at this timecode" id="videoPlayerAddKeyframeButton" class="btn btn-block btn-sm btn-success d-none keyframeButton">New <i class="fas fa-plus-circle fa-fw"></i></button>
								<button title="remove current keyframe" id="videoPlayerRemoveKeyframeButton" class="btn btn-block btn-sm btn-danger d-none keyframeButton">Delete <i class="fas fa-trash-alt fa-fw"></i></button>`,
        classes: 'leaflet-bar',
        id: 'animationControlWidget',
        style: {margin: '10px', padding: '0px 0 0 0',},
      });

      TIMAAT.VideoPlayer.animCtrl.updateUI = function () {
        let anno = TIMAAT.VideoPlayer.curAnnotation;
        if (!anno || !anno.isAnimation()) {
          $(this.getContainer()).hide();
          return;
        }
        $(this.getContainer()).show();
        let index = anno.getKeyframeIndex(anno.currentKeyframe);
        $('#videoPlayerPreviousKeyframeButton').prop('disabled', (index == 0) && anno.isOnKeyframe());
        $('#videoPlayerNextKeyframeButton').prop('disabled', (index >= (anno.svg.keyframes.length - 1)) && anno.isOnKeyframe());
        let addButton = $('#videoPlayerAddKeyframeButton');
        let removeButton = $('#videoPlayerRemoveKeyframeButton');
        let info = $(this.getContainer()).find('.keyframeInfo');
        addButton.addClass('d-none');
        removeButton.addClass('d-none');
        if (anno.isOnKeyframe()) {
          info.removeClass('fa-ellipsis-h');
          info.text(index + 1);
          removeButton.removeClass('d-none');
          removeButton.prop('disabled', anno.svg.keyframes.length <= 2 || anno.currentKeyframe.time == 0);
        } else {
          info.addClass('fa-ellipsis-h');
          info.text('');
          addButton.removeClass('d-none');
          addButton.prop('disabled', !(anno.isSelected() && anno.isActive()));
        }
      };

      TIMAAT.VideoPlayer.animCtrl.addTo(this.viewer);

      $('#videoPlayerAddKeyframeButton').on('click', function (ev) {
        ev.stopPropagation();
        if (!TIMAAT.VideoPlayer.curAnnotation) return;
        TIMAAT.VideoPlayer.pause();
        let anno = TIMAAT.VideoPlayer.curAnnotation;
        anno.updateStatus(TIMAAT.VideoPlayer.medium.currentTime);
        anno.addKeyframeAt(TIMAAT.VideoPlayer.medium.currentTime);
      });

      $('#videoPlayerRemoveKeyframeButton').on('click', function (ev) {
        ev.stopPropagation();
        if (!TIMAAT.VideoPlayer.curAnnotation) return;
        TIMAAT.VideoPlayer.pause();
        let anno = TIMAAT.VideoPlayer.curAnnotation;
        anno.updateStatus(TIMAAT.VideoPlayer.medium.currentTime);
        anno.removeCurrentKeyframe();
      });

      $('#videoPlayerPreviousKeyframeButton').on('click', function (ev) {
        ev.stopPropagation();
        if (!TIMAAT.VideoPlayer.curAnnotation) return;
        TIMAAT.VideoPlayer.pause();
        let anno = TIMAAT.VideoPlayer.curAnnotation;
        anno.updateStatus(TIMAAT.VideoPlayer.medium.currentTime);
        if (!anno.isOnKeyframe()) TIMAAT.VideoPlayer.jumpTo(anno.startTime + anno.currentKeyframe.time);
        else {
          let index = anno.getKeyframeIndex(anno.currentKeyframe) - 1;
          if (index < 0) index = 0;
          TIMAAT.VideoPlayer.jumpTo(anno.startTime + anno.svg.keyframes[index].time);
        }
      });

      $('#videoPlayerNextKeyframeButton').on('click', function (ev) {
        ev.stopPropagation();
        if (!TIMAAT.VideoPlayer.curAnnotation) return;
        TIMAAT.VideoPlayer.pause();
        let anno = TIMAAT.VideoPlayer.curAnnotation;
        anno.updateStatus(TIMAAT.VideoPlayer.medium.currentTime);
        TIMAAT.VideoPlayer.jumpTo(anno.startTime + anno.nextKeyframe.time);
      });

      // bookmark / add / remove annotation control
      L.control.custom({
        position: 'topleft',
        content: `<button type="button"
													 class="annotation-controls__button--disabled btn btn-light"
													 id="videoPlayerAnnotationQuickAddButton"
													 title="Quick annotate"
													 onclick="TIMAAT.VideoPlayer.addQuickAnnotation()">
										<i class="fas fa-bookmark fa-fw"></i>
									</button>
									<button type="button"
													class="annotation-controls__button--disabled btn btn-light ml-0"
													id="videoPlayerAnnotationAddButton"
													title="Create new annotation"
													onclick="TIMAAT.VideoPlayer.addAnnotation()">
										<i class="fa fa-plus fa-fw"></i>
									</button>
									<button type="button"
													class="annotation-controls__button--disabled btn btn-light ml-0"
													id="videoPlayerAnnotationRemoveButton"
													title="Delete annotation"
													onclick="TIMAAT.VideoPlayer.removeAnnotation()"
													disabled>
										<i class="fa fa-trash-alt fa-fw"></i>
									</button>`,
        classes: 'btn-group btn-group-sm btn-group-vertical leaflet-bar',
        style: {margin: '10px', padding: '0px 0 0 0',},
      })
          .addTo(TIMAAT.VideoPlayer.viewer);

      // save polygon changes control
      TIMAAT.VideoPlayer.savePolygonCtrl = L.control.custom({
        enabled: false,
        position: 'topleft',
        content: `<button type="button"
													 data-type"save"
													 class="btn btn-light"
													 id="videoPlayerSavePolygonsButton"
													 title="Save changes to annotation"
													 onclick="TIMAAT.VideoPlayer.updateAnnotations()"
													 disabled>
										<i class="fa fa-save fa-fw"></i>
									</button>`,
        classes: 'btn-group btn-group-sm btn-group-vertical leaflet-bar',
        style: {margin: '10px', padding: '0px 0 0 0',},
      });

      TIMAAT.VideoPlayer.savePolygonCtrl.setEnabled = function (enabled) {
        if (this.options.enabled == enabled) return;
        this.options.enabled = enabled;
        let button = $('#videoPlayerSavePolygonsButton');
        button.prop('disabled', !enabled);
        if (enabled) button.removeClass('btn-light').addClass('btn-success');
        else button.removeClass('btn-success').addClass('btn-light');
      };

      TIMAAT.VideoPlayer.savePolygonCtrl.addTo(this.viewer);

      TIMAAT.VideoPlayer.savePolygonCtrl.setEnabled(false);

      // shape editing control
      TIMAAT.VideoPlayer.editShapesCtrl = L.control.custom({
        position: 'topleft',
        enabled: true,
        content: `<button type="button"
													 data-type="rectangle"
													 class="rectangle btn btn-sm btn-light"
													 title="Create rectangle annotation"
													 onclick="TIMAAT.VideoPlayer.createShape('rectangle')">
										<i class="fas fa-vector-square fa-fw"></i>
									</button>
									<button type="button"
													data-type="polygon"
													class="polygon ml-0 btn btn-sm btn-light"
													title="Create polygon annotation"
													onclick="TIMAAT.VideoPlayer.createShape('polygon')">
										<i class="fas fa-draw-polygon fa-fw"></i>
									</button>
									<button type="button"
													data-type="line"
													class="line ml-0 btn btn-sm btn-light"
													title="Create line annotation"
													onclick="TIMAAT.VideoPlayer.createShape('line')">
										<i class="fas fa-slash fa-fw"></i>
									</button>
									<button type="button"
													data-type="circle"
													class="circle ml-0 btn btn-sm btn-light"
													title="Create circle annotation"
													onclick="TIMAAT.VideoPlayer.createShape('circle')">
										<i class="far fa-circle fa-fw"></i>
									</button>`,
        classes: 'btn-group btn-group-sm btn-group-vertical leaflet-bar',
        style: {margin: '10px', padding: '0px 0 0 0',},
      });

      TIMAAT.VideoPlayer.editShapesCtrl.setEnabled = function (enabled) {
        if (this.options.enabled == enabled) return;
        this.options.enabled = enabled;
        $(this.getContainer()).find('button').prop('disabled', !enabled);
        if (!enabled) {
          $(this.getContainer()).find('button').removeClass('btn-success').addClass('btn-light');
          try {
            TIMAAT.VideoPlayer.viewer.editTools.stopDrawing();
          } catch (err) {
          }
          ;
        }
      };

      TIMAAT.VideoPlayer.editShapesCtrl.addTo(this.viewer);

      TIMAAT.VideoPlayer.editShapesCtrl.setEnabled(false);

      // ------------------------------------------------------------------------------------

      // polygon layer
      var annoLayer = new L.LayerGroup();
      TIMAAT.VideoPlayer.viewer.annoLayer = annoLayer;
      TIMAAT.VideoPlayer.viewer.addLayer(annoLayer);

      TIMAAT.VideoPlayer.viewer.on('layeradd', function (ev) {
        if (ev.layer.options.data)
          ev.layer.eachLayer(function (layer) {
            if (ev.layer.options.annotation) {
              if (ev.layer.options.annotation.isSelected()) {
                layer.enableEdit();
                if (TIMAAT.VideoPlayer.curAnnotation && TIMAAT.VideoPlayer.curAnnotation.isAnimation()) {
                  layer.editor.options.skipMiddleMarkers = true;
                  layer.editor.reset();
                }

              } else {
                layer.disableEdit();
                layer.dragging.disable();
                layer.dragging._draggable = null;
              }
            }
          });
      });

      TIMAAT.VideoPlayer.viewer.on('editable:editing', function (e) {
        e.layer.setStyle({weight: 1, fillOpacity: 0.2});
      });

      TIMAAT.VideoPlayer.viewer.on('editable:drawing:start', function (x) {
        if (!TIMAAT.VideoPlayer.curAnnotation) {
          try {
            TIMAAT.VideoPlayer.viewer.editTools.stopDrawing();
          } catch (e) {
          }
          return;
        }
        TIMAAT.VideoPlayer.pause();
        x.layer.setStyle({
          color: '#' + TIMAAT.VideoPlayer.curAnnotation.svg.colorHex,
          weight: TIMAAT.VideoPlayer.curAnnotation.svg.strokeWidth
        });
      });

      TIMAAT.VideoPlayer.viewer.on('editable:vertex:dragend', function (ev) {
        if (TIMAAT.VideoPlayer.curAnnotation) {
          TIMAAT.VideoPlayer.curAnnotation.setChanged();
          // sync keyframe
          if (ev.layer) TIMAAT.VideoPlayer.curAnnotation.syncShape(ev.layer);
          // console.log("TIMAAT.VideoPLayer.updateUI() - editable:vertex:dragend");
          TIMAAT.VideoPlayer.updateUI();
        }
      });

      TIMAAT.VideoPlayer.viewer.on('editable:dragend', function (ev) {
        if (ev.layer && ev.layer instanceof L.Circle) {
          ev.layer.disableEdit();
          ev.layer.enableEdit();
          if (TIMAAT.VideoPlayer.curAnnotation && TIMAAT.VideoPlayer.curAnnotation.isAnimation()) {
            ev.layer.editor.options.skipMiddleMarkers = true;
            ev.layer.editor.reset();
          }
        }
        // sync keyframe
        if (TIMAAT.VideoPlayer.curAnnotation && ev.layer) TIMAAT.VideoPlayer.curAnnotation.syncShape(ev.layer);

      });

      TIMAAT.VideoPlayer.viewer.on('editable:drag', function (ev) {
        var bounds = TIMAAT.VideoPlayer.confineBounds(ev.layer.getBounds(), ev.offset.x, ev.offset.y);
        if (ev.layer.setBounds) ev.layer.setBounds(L.latLngBounds(bounds.getNorthEast(), bounds.getSouthWest())); else {
          // TODO refactor
          var latLngs = (ev.layer.getLatLngs != null) ? ev.layer.getLatLngs() : [ev.layer.getLatLng()];
          $(latLngs[0]).each(function (item, latLng) {
            var minLat = (ev.layer instanceof L.Circle) ? ev.layer.getRadius() : 0;
            var minLng = (ev.layer instanceof L.Circle) ? ev.layer.getRadius() : 0;
            var maxLat = (ev.layer instanceof L.Circle) ? (TIMAAT.VideoPlayer.mediumBounds.getNorthEast().lat - ev.layer.getRadius()) : TIMAAT.VideoPlayer.mediumBounds.getNorthEast().lat;
            var maxLng = (ev.layer instanceof L.Circle) ? (TIMAAT.VideoPlayer.mediumBounds.getNorthEast().lng - ev.layer.getRadius()) : TIMAAT.VideoPlayer.mediumBounds.getNorthEast().lng;
            if (latLng.lng < minLng) latLng.lng = minLng;
            if (latLng.lat < minLat) latLng.lat = minLat;
            if (latLng.lng > maxLng) latLng.lng = maxLng;
            if (latLng.lat > maxLat) latLng.lat = maxLat;
            if (ev.layer instanceof L.Circle) ev.layer.setLatLng(latLng);
          });
        }
      });

      // Animation Events
      $(document).on('keyFrameAdded.annotation.TIMAAT', function (event, anno) {
        if (!anno) return;
        anno.updateStatus(TIMAAT.VideoPlayer.medium.currentTime);
        TIMAAT.VideoPlayer.updateUI();
        TIMAAT.VideoPlayer.inspector.updateItem();
      });

      $(document).on('keyFrameRemoved.annotation.TIMAAT', function (event, anno) {
        if (!anno) return;
        anno.updateStatus(TIMAAT.VideoPlayer.medium.currentTime);
        TIMAAT.VideoPlayer.updateUI();
        TIMAAT.VideoPlayer.inspector.updateItem();
      });

      $(document).on('keyFrameChanged.annotation.TIMAAT', function (event, anno) {
        if (!anno) return;
        anno.updateStatus(TIMAAT.VideoPlayer.medium.currentTime);
        TIMAAT.VideoPlayer.updateUI();
        TIMAAT.VideoPlayer.inspector.updateItem();
      });

      TIMAAT.VideoPlayer.viewer.on('editable:vertex:click', function (ev) {
        if (TIMAAT.VideoPlayer.curAnnotation && TIMAAT.VideoPlayer.curAnnotation.isAnimation() && !ev.layer.editor.drawing()) ev.cancel();
      });

      TIMAAT.VideoPlayer.viewer.on('editable:vertex:new', function (ev) {
        // Animation Event
        let shouldDelete = TIMAAT.VideoPlayer.curAnnotation && TIMAAT.VideoPlayer.curAnnotation.isAnimation() && !ev.layer.editor.drawing();
        if (shouldDelete) {
          ev.vertex.delete();
          return;
        }
        var latLng = ev.latlng;
        if (latLng.lng < 0) latLng.lng = 0;
        if (latLng.lat < 0) latLng.lat = 0;
        if (latLng.lng > TIMAAT.VideoPlayer.mediumBounds.getNorthEast().lng) latLng.lng = TIMAAT.VideoPlayer.mediumBounds.getNorthEast().lng;
        if (latLng.lat > TIMAAT.VideoPlayer.mediumBounds.getNorthEast().lat) latLng.lat = TIMAAT.VideoPlayer.mediumBounds.getNorthEast().lat;
        ev.vertex.setLatLng(latLng);
      });

      TIMAAT.VideoPlayer.viewer.on('editable:vertex:drag', function (ev) {
        var latLng = ev.latlng;
        if (latLng.lng < 0) latLng.lng = 0;
        if (latLng.lat < 0) latLng.lat = 0;
        if (latLng.lng > TIMAAT.VideoPlayer.mediumBounds.getNorthEast().lng) latLng.lng = TIMAAT.VideoPlayer.mediumBounds.getNorthEast().lng;
        if (latLng.lat > TIMAAT.VideoPlayer.mediumBounds.getNorthEast().lat) latLng.lat = TIMAAT.VideoPlayer.mediumBounds.getNorthEast().lat;
        ev.vertex.setLatLng(latLng);

        if (TIMAAT.VideoPlayer.curTool == 'circle' || (ev.vertex.editor.feature.getRadius != null)) {
          if (ev.vertex.editor.feature.getLatLng() == ev.vertex.latlng) return;
          var radius = TIMAAT.VideoPlayer.viewer.distance(ev.vertex.editor.feature.getLatLng(), ev.vertex.latlng);
          radius = Math.max(5, radius);
          ev.vertex.editor.feature.setRadius(radius);
        }


      });

      TIMAAT.VideoPlayer.viewer.on('editable:drawing:end', function (ev) {
        $(TIMAAT.VideoPlayer.editShapesCtrl.getContainer()).find('button').removeClass('btn-success').addClass('btn-light');
        TIMAAT.VideoPlayer.curTool = null;
        ev.layer.dragging.enable();
        TIMAAT.VideoPlayer.viewer.removeLayer(ev.layer);
        if (TIMAAT.VideoPlayer.curAnnotation) {
          TIMAAT.VideoPlayer.curAnnotation.addSVGItem(ev.layer);
          TIMAAT.VideoPlayer.updateUI();
          ev.layer.dragging._draggable = null;
          ev.layer.dragging.addHooks();
          ev.layer.enableEdit();
          if (TIMAAT.VideoPlayer.curAnnotation.isAnimation()) {
            ev.layer.editor.options.skipMiddleMarkers = true;
            ev.layer.editor.reset();
          }
        }
      });

      // -----------------------------------------------------

      // Zoom Control for non time-based media (images)
      L.Control.zoomHome = L.Control.extend({
        options: {
          position: 'topleft',
          zoomInText: '<i class="fas fa-search-plus zoom__in-text"></i>',
          zoomInTitle: 'Zoom in',
          zoomOutText: '<i class="fas fa-search-minus"></i>',
          zoomOutTitle: 'zoom out',
          zoomHomeText: '<i class="fas fa-image zoom_home-text"></i>',
          zoomHomeTitle: 'Display whole image'
        }, onAdd: function (map) {
          var controlName = 'leaflet-control-zoom', container = L.DomUtil.create('div', controlName + ' leaflet-bar'),
              options = this.options;
          this._zoomInButton = this._createButton(options.zoomInText, options.zoomInTitle, controlName + '-in-ctrl', container, this._zoomIn);
          this._zoomHomeButton = this._createButton(options.zoomHomeText, options.zoomHomeTitle, controlName + '-home-ctrl', container, this._zoomHome);
          this._zoomOutButton = this._createButton(options.zoomOutText, options.zoomOutTitle, controlName + '-out-ctrl', container, this._zoomOut);
          this._updateDisabled();
          map.on('zoomend zoomlevelschange', this._updateDisabled, this);
          return container;
        }, onRemove: function (map) {
          map.off('zoomend zoomlevelschange', this._updateDisabled, this);
        },
        _zoomIn: function (e) {
          this._map.zoomIn(e.shiftKey ? 1.5 : 0.5);
        },
        _zoomOut: function (e) {
          this._map.zoomOut(e.shiftKey ? 1.5 : 0.5);
        },
        _zoomHome: function (e) {
          TIMAAT.VideoPlayer.viewer.fitBounds(TIMAAT.VideoPlayer.mediumBounds);
        },
        _createButton: function (html, title, className, container, fn) {
          var link = L.DomUtil.create('a', className, container);
          link.innerHTML = html;
          link.href = "javascript:void()";
          link.title = title;
          L.DomEvent.on(link, 'mousedown dblclick', L.DomEvent.stopPropagation)
              .on(link, 'click', L.DomEvent.stop).on(link, 'click', fn, this).on(link, 'click', this._refocusOnMap, this);
          return link;
        }, _updateDisabled: function () {
          var map = this._map, className = 'leaflet-disabled';
          L.DomUtil.removeClass(this._zoomInButton, className);
          L.DomUtil.removeClass(this._zoomOutButton, className);
          if (map._zoom === map.getMinZoom()) L.DomUtil.addClass(this._zoomOutButton, className);
          if (map._zoom === map.getMaxZoom()) L.DomUtil.addClass(this._zoomInButton, className);
        }
      });
      TIMAAT.VideoPlayer.zoomCtrl = new L.Control.zoomHome();
      TIMAAT.VideoPlayer.zoomCtrl.addTo(TIMAAT.VideoPlayer.viewer);


    },

    initInspectorControls: function () {
      // setup sidebar inspector controls
      TIMAAT.VideoPlayer.inspector = new TIMAAT.Inspector(TIMAAT.VideoPlayer.viewer);

      // setup timeline controls
      TIMAAT.VideoPlayer.timeline = new TIMAAT.Timeline();

      // init summernote fields
      $('#inspectorComment').summernote({
        toolbar: [
          ['style', ['bold', 'italic', 'underline', 'clear']],
        ],
        disableDragAndDrop: true,
        tabDisable: false,
        codeviewFilter: true,
        codeviewIframeFilter: true,
        codeviewIframeWhitelistSrc: []
      });

      $('#inspectorTranscript').summernote({
        toolbar: [
          ['style', ['bold', 'italic', 'underline', 'clear']],
        ],
        disableDragAndDrop: true,
        tabDisable: false,
        codeviewFilter: true,
        codeviewIframeFilter: true,
        codeviewIframeWhitelistSrc: []
      });

      $('#inspectorLyrics').summernote({
        toolbar: [
          ['style', ['bold', 'italic', 'underline', 'clear']],
        ],
        disableDragAndDrop: true,
        tabDisable: false,
        codeviewFilter: true,
        codeviewIframeFilter: true,
        codeviewIframeWhitelistSrc: []
      });

      $('#annotationDeleteSubmitButton').on('click', function (ev) {
        var modal = $('#annotationDeleteModal');
        var anno = modal.data('annotation');
        if (anno) TIMAAT.VideoPlayer._annotationRemoved(anno);
        modal.modal('hide');
      });

      $('#analysisDeleteSubmitButton').on('click', function (ev) {
        var modal = $('#analysisDeleteModal');
        var list = modal.data('analysisList');
        if (list) TIMAAT.VideoPlayer._analysisListRemoved(list);
        modal.modal('hide');
      });

      $('#segmentElementDeleteSubmitButton').on('click', async function (ev) {
        var modal = $('#segmentElementDeleteModal');
        var type = modal.data('type');
        // console.log("TCL: $ -> type", type);
        var model = modal.data('model');
        // console.log("TCL: $ -> model", model);
        // if ( TIMAAT.VideoPlayer.inspector.state.type != 'segment' ) return;
        if (type && model) {
          await TIMAAT.AnalysisListService.removeSegmentElement(type, model.id);
          if (TIMAAT.VideoPlayer.curAnalysisList) {
            TIMAAT.VideoPlayer.refreshTimelineElementsStructure();
          }
        }
        TIMAAT.VideoPlayer.inspector.setItem(null);
        switch (type) {
          case 'segment':
            TIMAAT.VideoPlayer.curSegment = null;
            break;
          case 'sequence':
            TIMAAT.VideoPlayer.curSequence = null;
            break;
          case 'take':
            TIMAAT.VideoPlayer.curTake = null;
            break;
          case 'scene':
            TIMAAT.VideoPlayer.curScene = null;
            break;
          case 'action':
            TIMAAT.VideoPlayer.curAction = null;
            break;
        }
        modal.modal('hide');
      });

      $('#musicFormElementDeleteSubmitButton').on('click', async function (ev) {
        var modal = $('#musicFormElementDeleteModal');
        var model = $('#musicFormElementDeleteModal').data('model');
        // console.log("TCL: $ -> model", model);
        if (model) {
          await TIMAAT.MusicService.removeMusicFormElement(model.id);
          if (TIMAAT.VideoPlayer.curMusic) {
            TIMAAT.VideoPlayer.refreshTimelineElementsStructure();
          }
        }
        TIMAAT.VideoPlayer.inspector.setItem(null);
        TIMAAT.VideoPlayer.curMusicFormElement = null;
        modal.modal('hide');
      });

      $('#musicChangeInTempoElementDeleteSubmitButton').on('click', async function (ev) {
        var modal = $('#musicChangeInTempoElementDeleteModal');
        var model = $('#musicChangeInTempoElementDeleteModal').data('model');
        // console.log("TCL: $ -> model", model);
        if (model) {
          await TIMAAT.MusicService.removeMusicChangeInTempoElement(model.id);
          if (TIMAAT.VideoPlayer.curMusic) {
            TIMAAT.VideoPlayer.refreshTimelineElementsStructure();
          }
        }
        TIMAAT.VideoPlayer.inspector.setItem(null);
        TIMAAT.VideoPlayer.curMusicChangeInTempoElement = null;
        modal.modal('hide');
      });

      $('#musicArticulationElementDeleteSubmitButton').on('click', async function (ev) {
        var modal = $('#musicArticulationElementDeleteModal');
        var model = $('#musicArticulationElementDeleteModal').data('model');
        // console.log("TCL: $ -> model", model);
        if (model) {
          await TIMAAT.MusicService.removeMusicArticulationElement(model.id);
          if (TIMAAT.VideoPlayer.curMusic) {
            TIMAAT.VideoPlayer.refreshTimelineElementsStructure();
          }
        }
        TIMAAT.VideoPlayer.inspector.setItem(null);
        TIMAAT.VideoPlayer.curMusicArticulationElement = null;
        modal.modal('hide');
      });

      $('#musicDynamicsElementDeleteSubmitButton').on('click', async function (ev) {
        var modal = $('#musicDynamicsElementDeleteModal');
        var model = $('#musicDynamicsElementDeleteModal').data('model');
        // console.log("TCL: $ -> model", model);
        if (model) {
          await TIMAAT.MusicService.removeMusicDynamicsElement(model.id);
          if (TIMAAT.VideoPlayer.curMusic) {
            TIMAAT.VideoPlayer.refreshTimelineElementsStructure();
          }
        }
        TIMAAT.VideoPlayer.inspector.setItem(null);
        TIMAAT.VideoPlayer.curMusicDynamicsElement = null;
        modal.modal('hide');
      });

      $('#musicTextSettingElementDeleteSubmitButton').on('click', async function (ev) {
        var modal = $('#musicTextSettingElementDeleteModal');
        var model = $('#musicTextSettingElementDeleteModal').data('model');
        // console.log("TCL: $ -> model", model);
        if (model) {
          await TIMAAT.MusicService.removeMusicTextSettingElement(model.id);
          if (TIMAAT.VideoPlayer.curMusic) {
            TIMAAT.VideoPlayer.refreshTimelineElementsStructure();
          }
        }
        TIMAAT.VideoPlayer.inspector.setItem(null);
        TIMAAT.VideoPlayer.curMusicTextSettingElement = null;
        modal.modal('hide');
      });

      // close select2 drop-downs when clicking outside
      $(document).on('click', function (event) {
        var $target = $(event.target);
        if (!$target.closest('#annotationCategoriesMultiSelectDropdownContainer').length
            && (!$target.is('[class^=select2'))
            && $('#annotationCategoriesMultiSelectDropdown').hasClass("select2-hidden-accessible")) {
          $('#annotationCategoriesMultiSelectDropdown').select2('close');
        }
        if (!$target.closest('#annotationTagsMultiSelectDropdownContainer').length
            && (!$target.is('[class^=select2'))
            && $('#annotationTagsMultiSelectDropdown').hasClass("select2-hidden-accessible")) {
          $('#annotationTagsMultiSelectDropdown').select2('close');
        }
        if (!$target.closest('#AnalysisCategorySetsMultiSelectDropdownContainer').length
            && (!$target.is('[class^=select2'))
            && $('#AnalysisCategorySetsMultiSelectDropdown').hasClass("select2-hidden-accessible")) {
          $('#AnalysisCategorySetsMultiSelectDropdown').select2('close');
        }
        if (!$target.closest('#mediumAnalysisTagsMultiSelectDropdownContainer').length
            && (!$target.is('[class^=select2'))
            && $('#mediumAnalysisTagsMultiSelectDropdown').hasClass("select2-hidden-accessible")) {
          $('#mediumAnalysisTagsMultiSelectDropdown').select2('close');
        }
        if (!$target.closest('#segmentElementCategoriesMultiSelectDropdownContainer').length
            && (!$target.is('[class^=select2'))
            && $('#segmentElementCategoriesMultiSelectDropdown').hasClass("select2-hidden-accessible")) {
          $('#segmentElementCategoriesMultiSelectDropdown').select2('close');
        }
      });
    },
    updateTimeLineLayerConnectionVisibility: function () {
      const timeLineLayerConnectionElement = $('#timelineLayerConnection')
      const timeLineLayerConnectionChecked = timeLineLayerConnectionElement.is(':checked')

      let timeLineLayerConnectionVisible = false
      if (timeLineLayerConnectionChecked) {
        const timeLineLayerAudioChecked = $('#timelineAudioLayer').is(':checked')
        const timeLineLayerVideoChecked = $('#timelineVisualLayer').is(':checked')

        if (timeLineLayerAudioChecked && timeLineLayerVideoChecked) {
          timeLineLayerConnectionVisible = true
        }
      }

      if (timeLineLayerConnectionVisible) {
        $("#annotation_connection_layer").show()
      } else {
        $("#annotation_connection_layer").hide()
      }
    },

    initTimeLineControls: function () {
      // setup timeline view events
      $('#timelineVisualLayer').on('click', function (ev) {
        if ($('#timelineVisualLayer').is(':checked')) {
          $("#timeline__video_annotation_section").show()
        } else {
          $("#timeline__video_annotation_section").hide()
        }
        TIMAAT.VideoPlayer.updateTimeLineLayerConnectionVisibility();
        TIMAAT.VideoPlayer.sortListUI();
      });

      $('#timelineAudioLayer').on('click', function (ev) {
        if ($('#timelineAudioLayer').is(':checked')) {
          $(".timeline__audio_annotation").show()
        } else {
          $(".timeline__audio_annotation").hide()
        }
        TIMAAT.VideoPlayer.updateTimeLineLayerConnectionVisibility();
        TIMAAT.VideoPlayer.sortListUI();
      });

      $('#timelineLayerConnection').on('click', function (ev) {
        TIMAAT.VideoPlayer.updateTimeLineLayerConnectionVisibility();
        TIMAAT.VideoPlayer.sortListUI();
      });

      // setup timeline preview
      var preview = $('#videoSeekBarPreview');
      preview.removeClass('show');
      preview.hide();

      $('#videoSeekBar').on('input', function (ev) {
        this.style.background = "linear-gradient(to right, #ed1e24 0%,#ed1e24 " + this.value + "%,#939393 " + this.value + "%,#939393 100%)";
      });

      $('#videoSeekBar').on('click change', function (ev) {
        var time = TIMAAT.VideoPlayer.medium.duration * (this.value / 100) * 1000;
        TIMAAT.VideoPlayer.jumpTo(time);
      });

      $('#videoSeekBar').mouseenter(function (ev) {
        if (!TIMAAT.VideoPlayer.medium) return;
        var preview = $('#videoSeekBarPreview');
        preview.addClass('show');
        preview.show();
      });

      $('#videoSeekBar').mouseleave(function (ev) {
        var preview = $('#videoSeekBarPreview');
        preview.removeClass('show');
        preview.hide();
      });

      $('#videoSeekBar').mousemove(function (ev) {
        if (!TIMAAT.VideoPlayer.medium) return;
        var token = TIMAAT.VideoPlayer.model.medium.viewToken;
        var bar = $(this);
        var time = Math.round(ev.originalEvent.offsetX / bar.width() * TIMAAT.VideoPlayer.duration);
        var preview = $('#videoSeekBarPreview');
        $('#videoSeekBarPreview').css('left', ev.originalEvent.pageX - (preview.width() / 2) + 'px');
        $('#videoSeekBarPreview').css('top', bar.offset().top - preview.height() - 7 + 'px');
        preview.find('img').attr('src', "/TIMAAT/api/medium/video/" + TIMAAT.VideoPlayer.model.medium.id + "/thumbnail?token=" + token + "&time=" + time);
      });

      $('.timelineCollapseWidget').on('click', function (event) {
        if ($(this).closest('.timeline__section').hasClass('timeline__section--collapsed')) {
          $(this.closest('.timeline__section')).removeClass('timeline__section--collapsed').removeClass('collapsed');
          $(this).find('.timelineCollapseWidgetIcon').removeClass('fa-caret-right').addClass('fa-caret-down');
        } else {
          $(this.closest('.timeline__section')).addClass('timeline__section--collapsed').addClass('collapsed');
          $(this).find('.timelineCollapseWidgetIcon').removeClass('fa-caret-down').addClass('fa-caret-right');
        }
      })

    },

    initVideoPlayerControls: function () {
      // setup keyboard video controls
      $([document.body, TIMAAT.VideoPlayer.viewer]).keydown(function (ev) {
        if (TIMAAT.UI.component != 'videoPlayer') return;
        if (!TIMAAT.VideoPlayer.medium) return;
        if (ev.target != document.body && ev.target != window.map) return;

        var key;
        if (ev.originalEvent.key) key = ev.originalEvent.key;
        else key = ev.originalEvent.originalEvent.key;

        switch (key) {
          case " ":
            ev.preventDefault();
            $('.togglePlayButton').click();
            break;
          case "ArrowLeft":
            ev.preventDefault();
            $('.stepBackButton').click();
            break;
          case "ArrowRight":
            ev.preventDefault();
            $('.stepForwardButton').click();
            break;
          case "r":
            ev.preventDefault();
            $('.repeatButtonSelection').click();
            break;
          case "m":
            ev.preventDefault();
            $('.volumeControlIcon').click();
            break;
          case "s":
            ev.preventDefault();
            $('#mediumPlayerPlaybackSpeed').click();
            break;
          case "a":
            ev.preventDefault();
            $('.repeatButtonAnnotation').click();
            break;
        }
      });

      // setup video controls UI events
      // TODO refactor
      $('.togglePlayButton').on('click', function (ev) {
        ev.preventDefault();
        $(this).toggleClass('active');
        if ($(this).hasClass('active')) TIMAAT.VideoPlayer.play(); else TIMAAT.VideoPlayer.pause();
      });

      $('.stepBackButton').on('click dblclick', function (ev) {
        ev.preventDefault();
        ev.stopPropagation();
        TIMAAT.VideoPlayer.pause();
        let frameTime = 1000 / TIMAAT.VideoPlayer.curFrameRate;
        TIMAAT.VideoPlayer.jumpTo(Math.max(0, (Math.round(TIMAAT.VideoPlayer.medium.currentTime * 1000 / frameTime) * frameTime) - frameTime));
      });

      $('.stepForwardButton').on('click dblclick', function (ev) {
        ev.preventDefault();
        ev.stopPropagation();
        TIMAAT.VideoPlayer.pause();
        let frameTime = 1000 / TIMAAT.VideoPlayer.curFrameRate;
        TIMAAT.VideoPlayer.jumpTo(Math.min(TIMAAT.VideoPlayer.medium.duration * 1000, (Math.round(TIMAAT.VideoPlayer.medium.currentTime * 1000 / frameTime) * frameTime) + frameTime));
      });

      const updateRepeatButtonStates = () => {
        $(".repeatButtonAnnotation, .repeatButtonSelection").removeClass('btn-outline-secondary').removeClass('btn-primary');
        switch (TIMAAT.VideoPlayer.repeatMode) {
          case "NONE":
            $(".repeatButtonAnnotation, .repeatButtonSelection").addClass('btn-outline-secondary')
            break;
          case "ANNOTATION":
            $(".repeatButtonAnnotation").addClass('btn-primary')
            $(".repeatButtonSelection").addClass('btn-outline-secondary')
            break
          case "SELECTION":
            $('.repeatButtonAnnotation').addClass('btn-outline-secondary')
            $('.repeatButtonSelection').addClass('btn-primary')
            break
        }
      }

      $('.repeatButtonAnnotation').on('click', function (ev) {
        ev.preventDefault();
        ev.stopPropagation();

        if (TIMAAT.VideoPlayer.repeatMode === "ANNOTATION") {
          TIMAAT.VideoPlayer.repeatMode = "NONE";
        } else {
          TIMAAT.VideoPlayer.repeatMode = "ANNOTATION";
        }

        updateRepeatButtonStates()
      });


      $('#mediumPlayerSubtitle').on('click', function (ev) {
        TIMAAT.VideoPlayer.subtitleEnabled = !TIMAAT.VideoPlayer.subtitleEnabled
        const $mediumPlayerSubtitle = $('#mediumPlayerSubtitle')
        const videoElement = TIMAAT.VideoPlayer.overlay.getElement()

        if (TIMAAT.VideoPlayer.subtitleEnabled) {
          $mediumPlayerSubtitle.removeClass('btn-outline-secondary')
          $mediumPlayerSubtitle.addClass('btn-primary')

          if (videoElement && videoElement.textTracks.length) {
            videoElement.textTracks[0].mode = "showing"
          }
        } else {
          $mediumPlayerSubtitle.addClass('btn-outline-secondary')
          $mediumPlayerSubtitle.removeClass('btn-primary')

          if (videoElement && videoElement.textTracks.length) {
            videoElement.textTracks[0].mode = "hidden"
          }
        }
      })

      $('.repeatButtonSelection').on('click', function (ev) {
        ev.preventDefault();
        ev.stopPropagation();

        if (TIMAAT.VideoPlayer.repeatMode === "SELECTION") {
          TIMAAT.VideoPlayer.repeatMode = "NONE";
        } else {
          TIMAAT.VideoPlayer.repeatMode = "SELECTION";
        }

        updateRepeatButtonStates()
      });

      $('#mediumPlayerSetThumbnailButton').on('click', async function (ev) {
        const currentPlayerPositionMillis = Math.round(TIMAAT.VideoPlayer.medium.currentTime * 1000)
        const mediumId = TIMAAT.VideoPlayer.model.medium.id
        await TIMAAT.MediumService.updateMediumThumbnail(mediumId, currentPlayerPositionMillis)
      })

      $('.volumeControlInput').on('input change', function () {
        if (!TIMAAT.VideoPlayer.medium) return;
        TIMAAT.VideoPlayer.medium.volume = $(this).val() / 100;
        if (TIMAAT.VideoPlayer.medium.volume > 0) {
          $('.volumeControlIcon').removeClass('fa-volume-mute').addClass('fa-volume-up');
        } else {
          $('.volumeControlIcon').removeClass('fa-volume-up').addClass('fa-volume-mute');
        }
      });

      $('.volumeControlIcon').on('click', function () {
        if (!TIMAAT.VideoPlayer.medium) return;
        if (TIMAAT.VideoPlayer.medium.volume > 0) {
          TIMAAT.VideoPlayer.volume = TIMAAT.VideoPlayer.medium.volume;
          $('.volumeControlInput').val(0);
        } else {
          $('.volumeControlInput').val(TIMAAT.VideoPlayer.volume * 100);
        }
        $('.volumeControlInput').change();
      });

      $('#mediumPlayerPlaybackSpeed').on('click', function () {
        if (!TIMAAT.VideoPlayer.medium) return;
        var playbackSpeeds = [1, 2, 0.5, 0.25]; // TODO move to config

        var speed = playbackSpeeds.indexOf(TIMAAT.VideoPlayer.medium.playbackRate);
        if (speed < 0) TIMAAT.VideoPlayer.medium.playbackRate = 1;
        else {
          speed++;
          if (speed > playbackSpeeds.length - 1) speed = 0;
          TIMAAT.VideoPlayer.medium.playbackRate = playbackSpeeds[speed];
        }
        let rateInfo = TIMAAT.VideoPlayer.medium.playbackRate;
        if (rateInfo == 0.5) rateInfo = "&frac12;";
        if (rateInfo == 0.25) rateInfo = "&frac14;";
        // update UI
        $(this).find('.playbackSpeedInfo').html(rateInfo + "&times;");
        if (TIMAAT.VideoPlayer.medium.playbackRate != 1) $(this).addClass('active'); else $(this).removeClass('active');

      });

      $('#mediaPlayerViewer').on('contextmenu', function (event) {
        event.stopPropagation();
        event.preventDefault();
        return false;
      });
    },

    initializeAnnotationMode: async function (medium) {
      if (!medium && !TIMAAT.VideoPlayer.mediaType == 'image') return;
      TIMAAT.UI.showComponent('videoPlayer');

      // setup video in player
      TIMAAT.VideoPlayer.setupMedium(medium);
      // load video annotations from server
      let analysisLists = await TIMAAT.AnalysisListService.getMediumAnalysisLists(medium.id);
      await TIMAAT.VideoPlayer.setupMediumAnalysisLists(analysisLists);
      TIMAAT.VideoPlayer.loadAnalysisList(0);
    },

    initializeAnnotationModeWithAnnotationByIds: async function (mediumId, annotationId) {
      const mediumPromise = TIMAAT.MediumService.getMedium(mediumId)
      const annotationPromise = TIMAAT.AnnotationService.getAnnotation(annotationId)

      const [medium, annotation] = await Promise.all([mediumPromise, annotationPromise])
      if (medium && annotation) {
        await TIMAAT.VideoPlayer.initializeAnnotationMode(medium)
        const mediumAnalysisListId = annotation.mediumAnalysisListId

        await TIMAAT.VideoPlayer.loadAnalysisList(mediumAnalysisListId);
        const requiredAnnotation = TIMAAT.VideoPlayer.annotationList.find(currentAnnotation => currentAnnotation.model.id === annotationId)

        if (requiredAnnotation) {
          TIMAAT.VideoPlayer.selectAnnotation(requiredAnnotation)
          TIMAAT.VideoPlayer.inspector.open('inspectorMetadata');
        }
      }
    },

    initializeAnnotationModeWithSegmentPreselected: async function (mediumId, mediumAnalysisListId, segmentId, segmentType) {
      const medium = await TIMAAT.MediumService.getMedium(mediumId)
      await TIMAAT.VideoPlayer.initializeAnnotationMode(medium)
      const loadedList = await TIMAAT.VideoPlayer.loadAnalysisList(mediumAnalysisListId);

      switch (segmentType) {
        case "SEGMENT":
          const analysisSegment = TIMAAT.VideoPlayer.curAnalysisList.analysisSegmentsUI.find(currentSegment => currentSegment.model.id === segmentId)
          analysisSegment.openInInspector(true)
          break
        case "SEQUENCE":
          const analysisSequence = TIMAAT.VideoPlayer.curAnalysisList.analysisSequencesUI.find(currentSequence => currentSequence.model.id === segmentId)
          analysisSequence.openInInspector(true)
          break
        case "TAKE":
          const analysisTake = TIMAAT.VideoPlayer.curAnalysisList.analysisTakesUI.find(currentTake => currentTake.model.id === segmentId)
          analysisTake.openInInspector(true)
          break
        case "SCENE":
          const analysisScene = TIMAAT.VideoPlayer.curAnalysisList.analysisScenesUI.find(currentScene => currentScene.model.id === segmentId)
          analysisScene.openInInspector(true)
          break
        case "ACTION":
          const analysisAction = TIMAAT.VideoPlayer.curAnalysisList.analysisActionsUI.find(currentAction => currentAction.model.id === segmentId)
          analysisAction.openInInspector(true)
          break
      }

      //TIMAAT.VideoPlayer.inspector.setItem(take, 'take');
    },

    sort: function (elements) {
      if (!elements) return;
      elements.sort(function (a, b) {
        if (b.model.startTime < a.model.startTime) return 1;
        if (b.model.startTime > a.model.startTime) return -1;
        return 0;
      })
    },

    loadThumbnail: function (medium) {
      if (!medium || !medium.ui) return;
      var img = $('<img />').appendTo('body').hide();
      img.data('video', medium);
      img.on('load', function (ev) {
        var medium = $(ev.target).data('video');
        if (medium.mediumVideo) {
          medium.ui.find('.card-img-top').attr('src', "api/medium/video/" + medium.id + "/thumbnail" + "?token=" + medium.viewToken);
        }
        if (medium.mediumImage) {
          medium.ui.find('.card-img-top').attr('src', "api/medium/image/" + medium.id + "/thumbnail" + "?token=" + medium.viewToken);
        }
        $(ev.target).remove();
      });
      img.on('error', function (ev) {
        ev.preventDefault();
        ev.stopPropagation();
        $(ev.target).remove();
      });
      if (medium.mediumVideo) {
        img.attr('src', "api/medium/video/" + medium.id + "/thumbnail" + "?token=" + medium.viewToken);
      }
      if (medium.mediumImage) {
        img.attr('src', "api/medium/image/" + medium.id + "/thumbnail" + "?token=" + medium.viewToken);
      }
    },

    createShape: function (type) {
      // console.log("TCL: createShape: function(type)");
      // console.log("TCL: type", type);

      if (TIMAAT.VideoPlayer.viewer.editTools.drawing()) {
        let oldTool = TIMAAT.VideoPlayer.curTool;
        try {
          TIMAAT.VideoPlayer.viewer.editTools.stopDrawing();
        } catch (err) {
        }
        ;
        $(TIMAAT.VideoPlayer.editShapesCtrl.getContainer()).find('button').removeClass('btn-success').addClass('btn-light');
        if (type == oldTool) return;
      }

      switch (type) {
        case 'rectangle':
          TIMAAT.VideoPlayer.curTool = type;
          TIMAAT.VideoPlayer.viewer.editTools.startRectangle();
          break;
        case 'polygon':
          TIMAAT.VideoPlayer.curTool = type;
          TIMAAT.VideoPlayer.viewer.editTools.startPolygon();
          break;
        case 'circle':
          TIMAAT.VideoPlayer.curTool = type;
          TIMAAT.VideoPlayer.viewer.editTools.startCircle();
          break;
        case 'line':
          TIMAAT.VideoPlayer.curTool = type;
          TIMAAT.VideoPlayer.viewer.editTools.startPolyline();
          break;
      }
      // update UI
      $(TIMAAT.VideoPlayer.editShapesCtrl.getContainer()).find('button.' + type).removeClass('btn-light').addClass('btn-success');
    },

    confineBounds: function (bounds, xOff, yOff) {
      // console.log("TCL: confineBounds: function(bounds, xOff, yOff)");
      // check bounds
      if (bounds.getSouthWest().lng + xOff < 0)
        xOff -= bounds.getSouthWest().lng + xOff;
      if (bounds.getNorthEast().lat - yOff > TIMAAT.VideoPlayer.mediumBounds.getNorthEast().lat)
        yOff += bounds.getNorthEast().lat - TIMAAT.VideoPlayer.mediumBounds.getNorthEast().lat - yOff;
      if (bounds.getNorthEast().lng + xOff > TIMAAT.VideoPlayer.mediumBounds.getNorthEast().lng)
        xOff -= bounds.getNorthEast().lng + xOff - TIMAAT.VideoPlayer.mediumBounds.getNorthEast().lng;
      if (bounds.getSouthWest().lat + yOff < 0)
        yOff += bounds.getSouthWest().lat + yOff;
      bounds.getSouthWest().lng += xOff;
      bounds.getNorthEast().lng += xOff;
      bounds.getSouthWest().lat -= yOff;
      bounds.getNorthEast().lat -= yOff;
      return bounds;
    },
    handleMediumAudioAnalysisEntityUpdateMessage: function (mediumAudioAnalysisUpdateMessage) {
      if (mediumAudioAnalysisUpdateMessage.type === "CHANGE" && TIMAAT.VideoPlayer.model.medium?.id === mediumAudioAnalysisUpdateMessage.id) {
        TIMAAT.VideoPlayer.model.medium.mediumAudioAnalysis = {...TIMAAT.VideoPlayer.model.medium.mediumAudioAnalysis, ...mediumAudioAnalysisUpdateMessage.entity}
        TIMAAT.VideoPlayer.drawWaveform()
        TIMAAT.VideoPlayer.loadMediumFrequencyInformation()
      }
    },

    handleTranscriptionUpdateMessage: function (transcriptionUpdateMessage) {
      if (transcriptionUpdateMessage.type === "CHANGE" && transcriptionUpdateMessage.id === TIMAAT.VideoPlayer.model.medium?.defaultTranscriptionId && (transcriptionUpdateMessage.entity.state === "COMPLETED" || transcriptionUpdateMessage.entity.contentChanged)) {
        TIMAAT.VideoPlayer.updateVideoPlayerSubtitles()
      }
    },

    handleMediumUpdateMessage: function (mediumUpdateMessage) {
      if (mediumUpdateMessage.type === "CHANGE" && mediumUpdateMessage.id === TIMAAT.VideoPlayer.model.medium?.id) {
        const previousDefaultTranscriptionId = TIMAAT.VideoPlayer.model.medium.defaultTranscriptionId
        TIMAAT.VideoPlayer.model.medium = {...TIMAAT.VideoPlayer.model.medium, ...mediumUpdateMessage.entity}

        if (TIMAAT.VideoPlayer.model.medium.defaultTranscriptionId !== previousDefaultTranscriptionId) {
          TIMAAT.VideoPlayer.updateVideoPlayerSubtitles()
        }
      }
    },

    setupMedium: async function (medium) {
      // console.log("TCL: setupMedium: -> medium", medium);
      this.mediaType = medium.mediaType.mediaTypeTranslations[0].type;

      // setup model
      this.curFrameRate = 25; // required for step forward and step backward functionality
      this.model.medium = medium;
      this.mediumFrequencyInformation = null;

      TIMAAT.VideoPlayer.drawFrequencyInformation()
      TIMAAT.VideoPlayer.loadMediumFrequencyInformation()

      // remove all annotations and markers
      this.annotationList.forEach(function (annotation) {
        annotation.remove()
      });
      this.annotationList = [];
      this.curAnnotation = null;
      if (this.curMusic) {
        this.clearTimelineElementsStructure();
        this.curMusic = null;
      }
      $('.timelineSectionMusicStructure').hide();
      $('.analysisMusicDropdown').hide();

      // remove old medium
      if (TIMAAT.VideoPlayer.medium) {
        $(TIMAAT.VideoPlayer.medium).off('canplay');
        TIMAAT.VideoPlayer.pause();
        TIMAAT.VideoPlayer.jumpTo(0);
        $('#videoSeekBar').val(0);
        TIMAAT.VideoPlayer.medium.currentTime = 0;
      }
      if (this.overlay) TIMAAT.VideoPlayer.viewer.removeLayer(this.overlay);

      // setup annotation UI
      $('#videoPlayerAnnotationAddButton').prop('disabled', true);
      $('#videoPlayerAnnotationAddButton').attr('disabled');
      $('#videoPlayerAnnotationAddButton').addClass('annotation-controls__button--disabled');
      switch (this.mediaType) {
        case 'audio':
        case 'video':
          $('#videoPlayerAnnotationQuickAddButton').prop('disabled', false);
          $('#videoPlayerAnnotationQuickAddButton').removeAttr('disabled');
          $('#videoPlayerAnnotationQuickAddButton').removeClass('annotation-controls__button--disabled');
          break;
        case 'image':
          $('#videoPlayerAnnotationQuickAddButton').prop('disabled', true);
          $('#videoPlayerAnnotationQuickAddButton').attr('disabled');
          $('#videoPlayerAnnotationQuickAddButton').addClass('annotation-controls__button--disabled');
          break;
      }

      // setup analysis list UI
      $('#analysisChooser').empty();
      TIMAAT.VideoPlayer.initEmptyAnalysisListMenu();

      switch (this.mediaType) {
        case 'audio':
        case 'video':
          // setup timeline UI
          $('.mediumPlayerTimelineArea').show();
          $('.mediaPlayerAnalysisArea').show();
          $('.mediumPlayerMediumDatasetArea').show();
          switch (this.mediaType) {
            case 'audio':
              $('#timelineLayerCheckboxes').hide();
              $('#mediumPlayerSetThumbnailButton').hide();
              break;
            case 'video':
              $('#timelineLayerCheckboxes').show();
              $('#mediumPlayerSetThumbnailButton').show();
              break;
          }

          //$('.timeline__audio-waveform').css('background-image', 'url("img/preview-placeholder.png")');
          //$('.timeline__audio-waveform').css('background-image', 'url("/TIMAAT/api/medium/'+TIMAAT.VideoPlayer.model.medium.id+'/audio/combined?token='+token+'")');

          if (this.model.medium.music) {
            this.curMusic = await TIMAAT.MusicService.getMusic(this.model.medium.music.id);
            $('.timelineSectionMusicStructure').show();
            $('.analysisMusicDropdown').show();
          }

          // setup timeline
          TIMAAT.VideoPlayer.timeline.initMedium(medium);

          // setup video overlay and UI
          $(TIMAAT.VideoPlayer.viewer.getContainer()).removeClass('media-player')
              .addClass('media-player');
          TIMAAT.VideoPlayer.viewer.invalidateSize();
          $('.mediaPlayerNoMediumAvailable').hide();
          $('.sidebarTabVideoPlayer').removeClass('sidebar__item--isDisabled');
          $('.mediaPlayerControls').show();
          var newMedium = new TIMAAT.Medium(medium, this.mediaType);
          TIMAAT.VideoPlayer.selectedMedium = newMedium;
          $('.sidebarTabVideoPlayer a').attr('onclick', 'TIMAAT.VideoPlayer.initializeAnnotationMode(TIMAAT.VideoPlayer.selectedMedium.model);');
          $('.sidebarTabVideoPlayer a').attr('title', 'Annotate ' + this.mediaType);
          $('.sidebarTabVideoPlayer a').attr('data-original-title', 'Annotate ' + this.mediaType);

          $('.js-media-player__title').html(medium.displayTitle.name);
          let timeProgressDisplay = "00:00:00.000 / 00:00:00.000";
          switch (this.mediaType) {
            case 'audio':
              timeProgressDisplay = "00:00:00.000 / " + TIMAAT.Util.formatTime(this.model.medium.mediumAudio.length, true);
              break;
            case 'video':
              timeProgressDisplay = "00:00:00.000 / " + TIMAAT.Util.formatTime(this.model.medium.mediumVideo.length, true);
              break;
          }
          $('.mediumDuration').html(timeProgressDisplay);
          $('#timelineSliderPane').show();

          var mediumUrl = '/TIMAAT/api/medium/' + this.mediaType + '/' + this.model.medium.id + '/download' + '?token=' + medium.viewToken;
          switch (this.mediaType) {
            case 'audio':
              this.mediumBounds = L.latLngBounds([[450, 0], [0, 450]]);
              break;
            case 'video':
              this.mediumBounds = L.latLngBounds([[450, 0], [0, 450 / medium.mediumVideo.height * medium.mediumVideo.width]]);
              break;
          }
          TIMAAT.VideoPlayer.viewer.setMaxBounds(this.mediumBounds);
          TIMAAT.VideoPlayer.viewer.setMinZoom(0.0);
          TIMAAT.VideoPlayer.viewer.fitBounds(this.mediumBounds);
          TIMAAT.VideoPlayer.viewer.setMaxZoom(0);
          this.overlay = L.videoOverlay(mediumUrl, this.mediumBounds, {
            autoplay: false,
            loop: false
          }).addTo(TIMAAT.VideoPlayer.viewer);
          this.medium = this.overlay.getElement();
          TIMAAT.VideoPlayer.updateVideoPlayerSubtitles()

          TIMAAT.VideoPlayer.drawWaveform()

          // setup viewer controls
          TIMAAT.VideoPlayer.viewer.dragging.disable();
          TIMAAT.VideoPlayer.viewer.touchZoom.disable();
          TIMAAT.VideoPlayer.viewer.doubleClickZoom.disable();
          TIMAAT.VideoPlayer.viewer.scrollWheelZoom.disable();
          $(TIMAAT.VideoPlayer.zoomCtrl.getContainer()).hide();
          $(TIMAAT.VideoPlayer.savePolygonCtrl.getContainer()).show();
          $(TIMAAT.VideoPlayer.editShapesCtrl.getContainer()).show();
          $(TIMAAT.VideoPlayer.animCtrl.getContainer()).show();
          $('.volumeControlInput').change();

          // setup music form element model
          // TODO make sure that timeline elements will be refreshed once even if no analysis list exists
          // if (this.curMusic) {
          // 	console.log("TCL: refresh element structure")
          // 	this.refreshTimelineElementsStructure();
          // }
          break;
        case 'image':
          // disable timeline UI
          $('.mediaPlayerAnalysisArea').show();
          $('.mediumPlayerMediumDatasetArea').show();
          $('.mediumPlayerTimelineArea').hide();
          $('.mediaPlayerControls').hide();

          // setup image overlay and UI
          $(TIMAAT.VideoPlayer.viewer.getContainer()).removeClass('media-player')
              .addClass('media-player');
          TIMAAT.VideoPlayer.viewer.invalidateSize();
          $('.mediaPlayerNoMediumAvailable').hide();
          $('.sidebarTabVideoPlayer').removeClass('sidebar__item--isDisabled');
          var newMedium = new TIMAAT.Medium(medium, 'image');
          TIMAAT.VideoPlayer.selectedMedium = newMedium;
          $('.sidebarTabVideoPlayer a').attr('onclick', 'TIMAAT.VideoPlayer.initializeAnnotationMode(TIMAAT.VideoPlayer.selectedMedium.model);');
          $('.sidebarTabVideoPlayer a').attr('title', 'Annotate image');
          $('.sidebarTabVideoPlayer a').attr('data-original-title', 'Annotate image');

          $('.js-media-player__title').html(medium.displayTitle.name);
          $('#timelineSliderPane').hide();
          $('.mediumDuration').html('N/A');

          var mediumUrl = '/TIMAAT/api/medium/image/' + this.model.medium.id + '/download' + '?token=' + medium.viewToken;
          this.mediumBounds = L.latLngBounds([[medium.mediumImage.height, 0], [0, medium.mediumImage.width]]);
          TIMAAT.VideoPlayer.viewer.setMaxBounds(this.mediumBounds);
          TIMAAT.VideoPlayer.viewer.setMinZoom(-100);
          let minZoom = TIMAAT.VideoPlayer.viewer.getBoundsZoom(TIMAAT.VideoPlayer.mediumBounds);
          TIMAAT.VideoPlayer.viewer.setMinZoom(minZoom);
          TIMAAT.VideoPlayer.viewer.fitBounds(this.mediumBounds);
          TIMAAT.VideoPlayer.viewer.setMaxZoom(1);
          this.overlay = L.imageOverlay(mediumUrl, this.mediumBounds, {interactive: false}).addTo(TIMAAT.VideoPlayer.viewer);
          this.medium = this.overlay.getElement();

          // setup viewer controls
          TIMAAT.VideoPlayer.viewer.dragging.enable();
          TIMAAT.VideoPlayer.viewer.touchZoom.enable();
          TIMAAT.VideoPlayer.viewer.doubleClickZoom.enable();
          TIMAAT.VideoPlayer.viewer.scrollWheelZoom.enable();
          $(TIMAAT.VideoPlayer.zoomCtrl.getContainer()).show();
          $(TIMAAT.VideoPlayer.savePolygonCtrl.getContainer()).show();
          $(TIMAAT.VideoPlayer.editShapesCtrl.getContainer()).show();
          $(TIMAAT.VideoPlayer.animCtrl.getContainer()).hide();
          break;
      }

      // setup inspector
      TIMAAT.VideoPlayer.inspector.reset();

      // setup medium data sheet
      // TIMAAT.UI.clearLastSelection('medium');
      $('#mediumFormMetadata').data('medium', medium);
      $('#mediumPreviewDataTab').addClass('annotationMode');
      $('#mediumDatasetsMediumTabs').show();
      TIMAAT.UI.displayDataSetContentContainer('mediumDataTab', 'mediumFormMetadata', 'medium');
      let mediumModel = {};
      mediumModel.model = medium;
      TIMAAT.UI.displayDataSetContent('dataSheet', mediumModel, 'medium');
      $('#mediumPlayerMediumTabsContainer').append($('#mediumDatasetsMediumTabs'));
      $('#videoPlayerMediumModalsContainer').append($('#mediumModals'));
      TIMAAT.MediumDatasets.container = 'videoPlayer';
      $('.mediumDataSheetFormAnnotateButton').hide();
      $('.mediumDataSheetFormAnnotateButton').prop('disabled', true);

      switch (this.mediaType) {
        case 'audio':
          this.duration = medium.mediumAudio.length;
          $('#timelineAudioLayer').prop('checked', true);
          break;
        case 'image':
          this.duration = 0; // disable time-based annotations
          $('#timelineVisualLayer').prop('checked', true);
          break;
        case 'video':
          this.duration = medium.mediumVideo.length;
          $('#timelineVisualLayer').prop('checked', true);
          $('#timelineAudioLayer').prop('checked', true);
          break;
        default:
          console.error("TCL: setupMedium: ERROR: Don't know how to handle media of type >" + type + "<", medium);
          alert("setupMedium: ERROR: Don't know how to handle media of type >" + type + "<");
          throw "setupMedium: ERROR: Don't know how to handle media of type >" + type + "<";
      }

      // attach event handlers for UI elements
      let curMedium = this.medium;

      $(curMedium).on('canplay', function (ev) {
        TIMAAT.VideoPlayer.medium.currentTime = 0;
        $('#videoSeekBar').val(0);
        TIMAAT.VideoPlayer.viewer.invalidateSize(true);
        TIMAAT.UI.setWaiting(false);
        $(curMedium).off('canplay');
      });

      $(curMedium).on('timeupdate', function (ev) {
        if (TIMAAT.VideoPlayer.duration == 0) return;
        let currentTime = TIMAAT.Util.formatTime(TIMAAT.VideoPlayer.medium.currentTime * 1000, true);
        $('.timelineCurrentTime').val(currentTime);
        let timeProgressDisplay = currentTime + " / " + TIMAAT.Util.formatTime(TIMAAT.VideoPlayer.duration, true);
        $('.mediumDuration').html(timeProgressDisplay);

        // update timeline
        TIMAAT.VideoPlayer.timeline.updateIndicator();

        var value = (100 / TIMAAT.VideoPlayer.medium.duration) * TIMAAT.VideoPlayer.medium.currentTime;
        $('#videoSeekBar').val(value);
        $('#videoSeekBar').css('background', "linear-gradient(to right, #ed1e24 0%, #ed1e24 " + value + "%,#939393 " + value + "%,#939393 100%)");
        // update annotation list UI
        TIMAAT.VideoPlayer.updateListUI("timeupdate");
        if (TIMAAT.VideoPlayer.curAnnotation) TIMAAT.VideoPlayer.animCtrl.updateUI();
        TIMAAT.VideoPlayer.updateUI();
      });
    },
    triggerWaveformGeneration: function () {
      if (TIMAAT.VideoPlayer.model.medium) {
        const mediumId = TIMAAT.VideoPlayer.model.medium.id
        const token = TIMAAT.VideoPlayer.model.medium.viewToken;
        fetch("/TIMAAT/api/medium/" + mediumId + "/mediumAudioAnalysis/start", {
          method: "POST",
          body: token
        })
      }
    },
    drawWaveform: function () {
      if (TIMAAT.VideoPlayer.model.medium) {
        $('.timeline-loading-content__generation_in_progress').hide()
        $('.timeline-error-content').hide()
        $('.timeline-loading-content').show()
        $('.timeline__audio-waveform').empty()
        switch (TIMAAT.VideoPlayer.model.medium.mediumAudioAnalysis?.audioAnalysisState?.id) {
          case 1:
          case 2:
            $('.timeline-loading-content__generation_in_progress').show()
            break
          case 3:
            $('.timeline-error-content').show()
            $('.timeline-loading-content').hide()
            break
          case 4:
            //Integration of wavesurfer js
            const token = TIMAAT.VideoPlayer.model.medium.viewToken;
            const mediumId = TIMAAT.VideoPlayer.model.medium.id
            fetch("/TIMAAT/api/medium/" + mediumId + "/audio/combined?token=" + token)
                .then(response => response.json())
                .then(waveformData => {
                  $('.timeline-loading-content').hide()
                  const wavesurfer = WaveSurfer.create({
                    container: ".timeline__audio-waveform",
                    waveColor: 'rgb(237, 30, 36)',
                    cursorWidth: 0,
                    fillParent: true,
                    height: "auto",
                    media: this.overlay.getElement(),
                    peaks: waveformData,
                    interact: false
                  })
                }).catch(error => {
              console.error("Error during loading waveform. Reason:", error)
            })
        }
      }
    },

    updateVideoPlayerSubtitles: function () {
      const $medium = $(TIMAAT.VideoPlayer.medium)
      $medium.find("track").remove()

      if (TIMAAT.VideoPlayer.model.medium.defaultTranscriptionId) {
        TIMAAT.MediumService.downloadTranscriptionFile(TIMAAT.VideoPlayer.model.medium.id, TIMAAT.VideoPlayer.model.medium.defaultTranscriptionId).then(blob => {
          const blobUrl = URL.createObjectURL(blob);
          const isDefault = TIMAAT.VideoPlayer.subtitleEnabled

          const track = $("<track/>")
          track.attr('src', blobUrl);
          if (isDefault) {
            track.attr('default', true);
          }
          $medium.append(track);
        })
      }

    },

    drawFrequencyInformation: function () {
      if (TIMAAT.VideoPlayer.mediumFrequencyInformation) {
        const minimumFrequency = Math.round(TIMAAT.VideoPlayer.mediumFrequencyInformation.minimumFrequency);
        const maximumFrequency = Math.round(TIMAAT.VideoPlayer.mediumFrequencyInformation.maximumFrequency);

        let frequencyText = `${minimumFrequency} - ${maximumFrequency} HZ`
        if (TIMAAT.VideoPlayer.selectionFrequencyInformation) {
          const selectionMinimumFrequency = Math.round(TIMAAT.VideoPlayer.selectionFrequencyInformation.minimumFrequency);
          const selectionMaximumFrequency = Math.round(TIMAAT.VideoPlayer.selectionFrequencyInformation.maximumFrequency);

          frequencyText += ` [${selectionMinimumFrequency} - ${selectionMaximumFrequency} HZ]`;
        }

        $('#timelineFrequencyInformation').html(frequencyText);
      } else {
        $('#timelineFrequencyInformation').html("")
      }
    },
    loadMediumFrequencyInformation: function () {
      const token = TIMAAT.VideoPlayer.model.medium.viewToken;
      const mediumId = TIMAAT.VideoPlayer.model.medium.id

      if (TIMAAT.VideoPlayer.mediumFrequencyInformation == null && TIMAAT.VideoPlayer.model.medium.mediumAudioAnalysis?.audioAnalysisState?.id === 4) {
        fetch("/TIMAAT/api/medium/" + mediumId + "/mediumAudioAnalysis/frequencyInformation?token=" + token)
            .then(response => response.json())
            .then(frequencyInformation => {
              TIMAAT.VideoPlayer.mediumFrequencyInformation = frequencyInformation;
              TIMAAT.VideoPlayer.drawFrequencyInformation()
            })
      }
    },
    loadSelectionMediumFrequencyInformation: function () {
      if (TIMAAT.VideoPlayer.currentTemporaryWaveformMarker) {
        const token = TIMAAT.VideoPlayer.model.medium.viewToken;
        const mediumId = TIMAAT.VideoPlayer.model.medium.id
        const startXPercentage = TIMAAT.VideoPlayer.currentTemporaryWaveformMarker.startXPercentage
        const endXPercentage = TIMAAT.VideoPlayer.currentTemporaryWaveformMarker.endXPercentage
        const startPositionMs = Math.round(TIMAAT.VideoPlayer.duration * startXPercentage)
        const endPositionMs = Math.round(TIMAAT.VideoPlayer.duration * endXPercentage)

        if (TIMAAT.VideoPlayer.model.medium.mediumAudioAnalysis?.audioAnalysisState?.id === 4) {
          fetch("/TIMAAT/api/medium/" + mediumId + "/mediumAudioAnalysis/frequencyInformation?token=" + token + "&startPositionMs=" + startPositionMs + "&endPositionMs=" + endPositionMs)
              .then(response => response.json())
              .then(frequencyInformation => {
                if (TIMAAT.VideoPlayer.currentTemporaryWaveformMarker.startXPercentage === startXPercentage && TIMAAT.VideoPlayer.currentTemporaryWaveformMarker.endXPercentage === endXPercentage) {
                  TIMAAT.VideoPlayer.selectionFrequencyInformation = frequencyInformation;
                  TIMAAT.VideoPlayer.drawFrequencyInformation()
                }
              })
        }
      }

    },
    handleTemporaryWaveformDragStart: function (ev) {
      if (TIMAAT.VideoPlayer.currentTemporaryWaveformMarker) {
        TIMAAT.VideoPlayer.currentTemporaryWaveformMarker.removeFromUi();
      }

      const waveformContainerRect = TIMAAT.VideoPlayer.waveformContainer.get(0).getBoundingClientRect();
      const relativeX = (ev.clientX - waveformContainerRect.left + TIMAAT.VideoPlayer.waveformContainer.scrollLeft()) / TIMAAT.VideoPlayer.waveformContainer.width();
      TIMAAT.VideoPlayer.currentTemporaryWaveformMarker = new TIMAAT.TemporaryWaveformMarker(relativeX, TIMAAT.VideoPlayer.waveformContainer)
      TIMAAT.VideoPlayer.selectionFrequencyInformation = null;
      $(document).on("mousemove", TIMAAT.VideoPlayer.handleTemporaryWaveformDrag)
      TIMAAT.VideoPlayer.drawFrequencyInformation()
      TIMAAT.VideoPlayer.timeline.indicatorHidden = true

      $(document).one("mouseup", function (ev) {
        $(document).off("mousemove", TIMAAT.VideoPlayer.handleTemporaryWaveformDrag)
        TIMAAT.VideoPlayer.waveformContainer.one("mousedown", TIMAAT.VideoPlayer.handleTemporaryWaveformDragStart)
        TIMAAT.VideoPlayer.loadSelectionMediumFrequencyInformation()
        TIMAAT.VideoPlayer.timeline.indicatorHidden = false
      })
    },

    handleTemporaryWaveformDrag: function (ev) {
      const waveformContainerRect = TIMAAT.VideoPlayer.waveformContainer.get(0).getBoundingClientRect();
      const relativeX = (ev.clientX - waveformContainerRect.left + TIMAAT.VideoPlayer.waveformContainer.scrollLeft()) / TIMAAT.VideoPlayer.waveformContainer.width();
      const playbackTimeMs = TIMAAT.VideoPlayer.duration * relativeX

      TIMAAT.VideoPlayer.currentTemporaryWaveformMarker.endXPercentage = relativeX
      TIMAAT.VideoPlayer.jumpTo(playbackTimeMs)
    },

    setupMediumAnalysisLists: async function (lists) {
      // console.log("TCL: setupMediumAnalysisLists: ", lists);
      // clear old lists if any
      $('#analysisChooser').empty();
      // setup model
      TIMAAT.VideoPlayer.model.analysisLists = lists;
      lists.forEach(function (list) {
        list.ui = $('<option value="' + list.id + '">' + TIMAAT.Util.getDefaultTranslation(list, 'mediumAnalysisListTranslations', 'title') + '</option>');
        list.ui.data('list', list);
        $('#analysisChooser').append(list.ui);
      });

      // update UI
      $('#mediumAnalysisOptions').prop('disabled', false);
      $('#mediumAnalysisOptions').removeAttr('disabled');
      $('#analysisChooser').prop('disabled', false);
      $('#analysisChooser').removeAttr('disabled');
      $('#analysisChooser').removeClass("item--disabled");
    },

    loadAnalysisList: async function (listId) {
      $('#analysisLoader').show();

      if (TIMAAT.VideoPlayer.model.analysisLists.length === 0) {
        await TIMAAT.VideoPlayer.setupAnalysisList(null);
        TIMAAT.VideoPlayer.initEmptyAnalysisListMenu();
        $('#analysisLoader').hide();
        return null;
      }

      let index = TIMAAT.VideoPlayer.model.analysisLists.findIndex(({id}) => id === listId);

      const list =
          index >= 0
              ? TIMAAT.VideoPlayer.model.analysisLists[index]
              : TIMAAT.VideoPlayer.model.analysisLists[0];

      await TIMAAT.VideoPlayer.setupAnalysisList(list);
      $('#analysisChooser').val(list?.id);
      TIMAAT.VideoPlayer.initAnalysisListMenu();
      $('#analysisLoader').hide();

      return list;
    },

    setupAnalysisList: async function (analysisList) {
      // console.log("TCL: setupAnalysisList:function -> analysisList", analysisList);
      if (analysisList) this.currentPermissionLevel = await TIMAAT.AnalysisListService.getMediumAnalysisListPermissionLevel(analysisList.id);
      else this.currentPermissionLevel = null;

      if (analysisList === this.curAnalysisList) return; // no new analysis list loaded
      // console.log("TCL: setupAnalysisList: ", analysisList);
      if (this.curAnnotation) this.curAnnotation.setSelected(false);

      // setup model
      this.model.analysisList = analysisList;
      // close UI tag editors if any
      TIMAAT.UI.hidePopups();
      // clear polygon UI
      this.viewer.annoLayer.eachLayer(function (layer) {
        layer.remove()
      });
      this.viewer.editTools.editLayer.eachLayer(function (layer) {
        layer.remove()
      });

      // clear old list contents if any
      if (this.curAnalysisList != null) {
        this.annotationList.forEach(function (anno) {
          anno.remove();
        });
        this.clearTimelineElementsStructure();
      }
      this.annotationList = [];
      this.curAnalysisList = analysisList;
      // build annotation list from model
      if (analysisList) {
        analysisList.annotations.forEach(function (annotation) {
          TIMAAT.VideoPlayer.annotationList.push(new TIMAAT.Annotation(annotation));
        });
      }
      TIMAAT.VideoPlayer.clearTimelineElementsStructure();

      TIMAAT.VideoPlayer.createTimelineElementsStructure();
      TIMAAT.VideoPlayer.sortTimelineElementsStructure();
      TIMAAT.VideoPlayer.showTimelineElementsStructure();
      TIMAAT.VideoPlayer.updateListUI();
      TIMAAT.VideoPlayer.sortListUI();

      // this.selectAnnotation(null);
      this.inspector.setItem(this.curAnalysisList, 'analysisList');
      this.selectedElementType = 'analysisList';

      // setup analysisList UI
      $('#analysisLoader').hide();
      $('#videoPlayerAnnotationAddButton').prop('disabled', this.curAnalysisList == null);
      if (this.curAnalysisList) {
        $('#videoPlayerAnnotationAddButton').removeAttr('disabled');
        $('#videoPlayerAnnotationAddButton').removeClass('annotation-controls__button--disabled');
      } else {
        $('#videoPlayerAnnotationAddButton').attr('disabled');
        $('#videoPlayerAnnotationAddButton').addClass('annotation-controls__button--disabled');
      }
      $('#analysisUserLog').prop('disabled', this.curAnalysisList == null);
      if (this.curAnalysisList) {
        $('#analysisUserLog').removeAttr('disabled');
        // send notification to server
        TIMAAT.UI.sendNotification('subscribe-list', this.curAnalysisList.id);
      } else {
        $('#analysisUserLog').attr('disabled');
      }

      if (this.curAnalysisList) {
        TIMAAT.URLHistory.setURL(null, 'Analysis · ' + this.curAnalysisList.mediumAnalysisListTranslations[0].title, '#analysis/' + this.curAnalysisList.id);
        // DEBUG
        $('#timelineListTitle').text('"' + this.curAnalysisList.mediumAnalysisListTranslations[0].title + '"');
      } else {
        TIMAAT.URLHistory.setURL(null, 'Video Player', '#analysis');
        // DEBUG
        $('#timelineListTitle').text('N/A');
      }
    },

    userLogForList: function () {
      // console.log("TCL: userLogForList: function()");
      $('#analysisUserLog').popover('show');
    },

    addAnalysisList: function () {
      // console.log("TCL: addAnalysisList: function()");
      TIMAAT.VideoPlayer.pause();
      TIMAAT.VideoPlayer.selectAnnotation(null);
      TIMAAT.VideoPlayer.inspector.setItem(null, 'analysisList');
    },

    updateAnalysisList: async function (analysisList) {
      // console.log("TCL: updateAnalysisList - analysisList", analysisList);
      // sync to server
      TIMAAT.VideoPlayer.curAnalysisList = await TIMAAT.AnalysisListService.updateMediumAnalysisList(analysisList);
      analysisList.ui = $('<option value="' + analysisList.id + '">' + TIMAAT.Util.getDefaultTranslation(analysisList, 'mediumAnalysisListTranslations', 'title') + '</option>');
      analysisList.ui.data('list', analysisList);

      let index = TIMAAT.VideoPlayer.model.analysisLists.findIndex(({id}) => id === analysisList.id);
      TIMAAT.VideoPlayer.model.analysisLists[index] = analysisList;
      $('#analysisChooser').find('[value="' + analysisList.id + '"]').html(TIMAAT.Util.getDefaultTranslation(TIMAAT.VideoPlayer.curAnalysisList, 'mediumAnalysisListTranslations', 'title'));
    },

    editAnalysisList: function () {
      // console.log("TCL: editAnalysisList: function()");
      TIMAAT.VideoPlayer.pause();
      TIMAAT.VideoPlayer.selectAnnotation(null);
      TIMAAT.VideoPlayer.inspector.setItem(TIMAAT.VideoPlayer.curAnalysisList, 'analysisList');
      TIMAAT.VideoPlayer.inspector.open('inspectorMetadata');
    },

    removeAnalysisList: function () {
      // console.log("TCL: removeAnalysisList: function()");
      if (!this.curAnalysisList) return;
      TIMAAT.VideoPlayer.pause();
      $('#analysisDeleteModal').data('analysisList', this.curAnalysisList);
      $('#analysisDeleteModal').modal('show');
    },

    manageAnalysisList: async function () {
      // console.log("TCL: manageAnalysisList: function()");
      // check if user is moderator or administrator of the analysis list.
      let permissionLevel = await TIMAAT.AnalysisListService.getMediumAnalysisListPermissionLevel(this.curAnalysisList.id);
      if (permissionLevel == 3 || permissionLevel == 4) {
        TIMAAT.VideoPlayer.pause();
        let modal = $('#analysisManageModal');
        // get all display names and permissions for this analysis
        let userDisplayNameAndPermissionList = await TIMAAT.AnalysisListService.getDisplayNamesAndPermissions(this.curAnalysisList.id);
        TIMAAT.VideoPlayer.userPermissionList = userDisplayNameAndPermissionList;
        let modalBodyText = `<div class="col-12">
					<div class="row">
						<div class="col-7">
							<h6>User</h6>
							<div id="analysisPermissionUserName">
							</div>
						</div>
						<div class="col-4">
							<h6>Access rights</h6>
							<div id="analysisPermissionLevel">
							</div>
						</div>
						<div class="col-1">
						</div>
					</div>`;
        let i = 0;
        for (; i < userDisplayNameAndPermissionList.length; i++) {
          modalBodyText += `<div class="permissionContainer" data-user-id="` + userDisplayNameAndPermissionList[i].userAccountId + `">
						<hr>
						<div class="row align-items--vertically">
							<div class="col-7">
								` + userDisplayNameAndPermissionList[i].displayName + `
							</div>
							<div class="col-4" data-role="changeUserPermissionAnalysisList">`;
          if (permissionLevel == 3) {
            switch (userDisplayNameAndPermissionList[i].permissionId) {
              case 1:
                modalBodyText += `<select class="custom-select" data-role="select">
													<option value="1" selected>Read</option>
													<option value="2">Read+Write</option>
												</select>
											</div>
											<div class="col-1" data-role="removeUserPermissionAnalysisList">
												<button class="removePermission badge btn btn-sm btn-danger p-1 float-right" data-role="remove" data-user-id="` + userDisplayNameAndPermissionList[i].userAccountId + `" data-listId="` + userDisplayNameAndPermissionList[i].permissionId + `">
													<i class="fas fa-minus fa-fw"></i>
												</button>
											</div>
										</div>
									</div>`;
                break;
              case 2:
                modalBodyText += `<select class="custom-select" data-role="select">
													<option value="1">Read</option>
													<option value="2" selected>Read+Write</option>
												</select>
											</div>
											<div class="col-1" data-role="removeUserPermissionAnalysisList">
												<button class="removePermission badge btn btn-sm btn-danger p-1 float-right" data-role="remove" data-user-id="` + userDisplayNameAndPermissionList[i].userAccountId + `" data-listId="` + userDisplayNameAndPermissionList[i].permissionId + `">
													<i class="fas fa-minus fa-fw"></i>
												</button>
											</div>
										</div>
									</div>`;
                break;
              case 3:
                modalBodyText += `<select class="custom-select" data-role="select" disabled>
													<option value="3" selected>Moderate</option>
												</select>
											</div>
											<div class="col-1" data-role="removeUserPermissionAnalysisList">
											</div>
										</div>
									</div>`;
                break;
              case 4:
                modalBodyText += `<select class="custom-select" data-role="select" disabled>
													<option value="4" selected>Administrate</option>
												</select>
											</div>
											<div class="col-1" data-role="removeUserPermissionAnalysisList">
											</div>
										</div>
									</div>`;
                break;
              default:
                modalBodyText += `An error occurred!`; // should never occur
                break;
            }
          } else { // permissionLevel == 4
            switch (userDisplayNameAndPermissionList[i].permissionId) {
              case 1:
                modalBodyText += `<select class="custom-select" data-role="select">
										<option value="1" selected>Read</option>
										<option value="2">Read+Write</option>
										<option value="3">Moderate</option>
										<option value="4">Administrate</option>
									</select>
									<small class="danger-text" id="adminCannotBeChanged_` + userDisplayNameAndPermissionList[i].userAccountId + `">At least one administrator needs to exist.</small>`;
                break;
              case 2:
                modalBodyText += `<select class="custom-select" data-role="select">
										<option value="1">Read</option>
										<option value="2" selected>Read+Write</option>
										<option value="3">Moderate</option>
										<option value="4">Administrate</option>
									</select>
									<small class="danger-text" id="adminCannotBeChanged_` + userDisplayNameAndPermissionList[i].userAccountId + `">At least one administrator needs to exist.</small>`;
                break;
              case 3:
                modalBodyText += `<select class="custom-select" data-role="select">
										<option value="1">Read</option>
										<option value="2">Read+Write</option>
										<option value="3" selected>Moderate</option>
										<option value="4">Administrate</option>
									</select>
									<small class="danger-text" id="adminCannotBeChanged_` + userDisplayNameAndPermissionList[i].userAccountId + `">At least one administrator needs to exist.</small>`;
                break;
              case 4:
                modalBodyText += `<select class="custom-select" data-role="select">
										<option value="1">Read</option>
										<option value="2">Read+Write</option>
										<option value="3">Moderate</option>
										<option value="4" selected>Administrate</option>
									</select>
									<small class="danger-text" id="adminCannotBeChanged_` + userDisplayNameAndPermissionList[i].userAccountId + `">At least one administrator needs to exist.</small>`;
                break;
              default:
                modalBodyText += `An error occurred!`; // should never occur
                break;
            }
            modalBodyText += `
									</div>
									<div class="col-1" data-role="removeUserPermissionAnalysisList">
										<button class="removePermission badge btn btn-sm btn-danger p-1 float-right" data-role="remove" data-user-id="` + userDisplayNameAndPermissionList[i].userAccountId + `" data-listId="` + userDisplayNameAndPermissionList[i].permissionId + `">
											<i class="fas fa-minus fa-fw"></i>
										</button>
									</div>
								</div>
							</div>`;
          }
        }
        modalBodyText += `<div id="newPermissionContainer">
					<hr>
					<div class="row align-items--vertically" data-role="newPermission">
						<div class="col-2">
							<h6>Add user</h6>
						</div>
						<div class="col-5">
							<input type="text" id="userAccountForNewPermission" class="form-control username" placeholder="Username" aria-label="Username">
							<small class="danger-text" id="userAccountDoesNotExistInfo"">This user name does not exist. Please check your spelling and try again.</small>
							<small class="danger-text" id="userAccountAlreadyInList"">This user already has a permission level.</small>
							<small class="danger-text" id="adminCanNotBeAddedInfo"">Admin can not be added.</small>
						</div>
						<div class="col-4">
							<select class="custom-select" id="newAccessRightsSelect">
								<option value="1" selected>Read</option>
								<option value="2">Read+Write</option>`;
        if (permissionLevel == 4) {	 // only admins can create mods and admins
          modalBodyText += `<option value="3">Moderate</option>
						<option value="4">Administrate</option>`;
        }
        modalBodyText += `</select>
							</div>
							<div class="col-1" data-role="newUserPermissionAnalysisList">
								<button class="addNewPermission btn btn-sm btn-primary p-1 float-right" data-role="add" data-user-id="0" data-listId="0">
									<i class="fas fa-plus fa-fw"></i>
								</button>
							</div>
						</div>
						<div class="globalPermissionContainer">
							<hr>
							<div class="row align-items--vertically" data-role="globalUserPermission">
								<fieldset>
									<legend>You can grant all users access to this analysis</legend>
									<div id="globalPermission" class="radio-buttons__horizontal--evenly-spaced" data-role="select">
										<label>
											<input id="globalPermission_0" type="radio" name="globalPermissionAnalysisList" value="0"> No global access
										</label>
										<label>
											<input id="globalPermission_1" type="radio" name="globalPermissionAnalysisList" value="1"> Read
										</label>
										<label>
											<input id="globalPermission_2" type="radio" name="globalPermissionAnalysisList" value="2"> Read+Write
										</label>
									</div>
								</fieldset>
							</div>
						</div>
					</div>
				</div>`;
        modal.find('.modal-body').html(modalBodyText);
        if (TIMAAT.VideoPlayer.curAnalysisList.globalPermission == null) TIMAAT.VideoPlayer.curAnalysisList.globalPermission = 0;
        $('#globalPermission_' + TIMAAT.VideoPlayer.curAnalysisList.globalPermission).prop('checked', true);
        modal.modal('show');
      }
      // TODO else show popup 'you have no rights'
    },

    addQuickAnnotation: function () {
      // console.log("TCL: addQuickAnnotation: function()");
      if (!TIMAAT.VideoPlayer.curAnalysisList) return;
      let model = {};
      if (TIMAAT.VideoPlayer.curAnalysisList.mediumType == 'video' || TIMAAT.VideoPlayer.curAnalysisList.mediumType == 'audio') {
        TIMAAT.VideoPlayer.pause();
        model = {
          id: 0,
          startTime: TIMAAT.VideoPlayer.medium.currentTime * 1000,
          endTime: TIMAAT.VideoPlayer.medium.currentTime * 1000,
          layerVisual: true,
          layerAudio: true,
          // actors: [],
          // annotations1: [],
          // annotations2: [],
          // categories: [],
          // events: [],
          // locations: [],
          // mediums: [],
          annotationTranslations: [{
            id: 0,
            comment: "Bookmark to edit",
            title: "Annotation at " + TIMAAT.Util.formatTime(TIMAAT.VideoPlayer.medium.currentTime * 1000, true),
          }],
          selectorSvgs: [{
            id: 0,
            colorHex: "555555",
            opacity: 30, //* 0..1 is stored as 0..100 (Byte)
            svgData: "{\"keyframes\":[{\"time\":0,\"shapes\":[]}]}",
            strokeWidth: 1,
            svgShapeType: {
              id: 5
            }
          }]
        }
        // } else if (TIMAAT.VideoPlayer.curAnalysisList.mediumType == 'image') {
        // 	model = {
        // 		id: 0,
        // 		startTime: 0,
        // 		endTime: 0,
        // 		layerVisual: true,
        // 		layerAudio: false,
        // 		annotationTranslations: [{
        // 			id: 0,
        // 			comment: "Bookmark to edit",
        // 			title: "Quick annotation",
        // 		}],
        // 		selectorSvgs: [{
        // 			id: 0,
        // 			colorHex: "555555",
        // 			opacity: 30, //* 0..1 is stored as 0..100 (Byte)
        // 			svgData: "{\"keyframes\":[{\"time\":0,\"shapes\":[]}]}",
        // 			strokeWidth: 1,
        // 			svgShapeType: {
        // 				id: 5
        // 			}
        // 		}]
        // 	}
      } else return; // for now only videos can receive quick annotations
      TIMAAT.AnnotationService.createAnnotation(model, TIMAAT.VideoPlayer.curAnalysisList.id, TIMAAT.VideoPlayer._annotationAdded);
    },

    addAnnotation: function () {
      // console.log("TCL: addAnnotation: function()");
      TIMAAT.VideoPlayer.pause();
      TIMAAT.VideoPlayer.inspector.setItem(null, 'annotation');
    },

    addAnnotationWithTemporaryMarkerInformation() {
      TIMAAT.VideoPlayer.pause();

      let overrides = undefined
      if (TIMAAT.VideoPlayer.currentTemporaryWaveformMarker) {
        const start = TIMAAT.VideoPlayer.duration * TIMAAT.VideoPlayer.currentTemporaryWaveformMarker.startXPercentage
        const end = TIMAAT.VideoPlayer.duration * TIMAAT.VideoPlayer.currentTemporaryWaveformMarker.endXPercentage
        overrides = {
          start,
          end
        }
      }
      TIMAAT.VideoPlayer.inspector.setItem(null, 'annotation', overrides);
    },

    updateAnnotation: async function (annotation) {
      // console.log("TCL: updateAnnotation: annotation", annotation);
      // sync to server
      await TIMAAT.VideoPlayer.persistAnnotation(annotation);
      annotation.updateUI();
      this.updateUI();
      this.updateListUI();
      this.sortListUI();
    },

    updateAnnotations: function () {
      // console.log("TCL: updateAnnotations: function()");
      if (this.annotationList == null) return;
      this.annotationList.forEach(function (annotation) {
        if (annotation.isSelected() && annotation.hasChanges()) {
          annotation.saveChanges();
          TIMAAT.VideoPlayer.persistAnnotation(annotation);
          // update UI
          annotation.updateUI();
          // console.log("TCL: annotation.updateUI()");
        }
      });

      // update UI list view
      this.updateListUI();
      this.sortListUI();
      this.updateUI();
    },

    persistAnnotation: async function (annotation) {
      const annotationBaseInformation = {
        title: annotation.model.annotationTranslations[0].title,
        comment: annotation.model.annotationTranslations[0].comment,
        startTime: annotation.model.startTime,
        endTime: annotation.model.endTime,
        layerVisual: annotation.model.layerVisual,
        layerAudio: annotation.model.layerAudio,
        selectorSvg: {
          colorHex: annotation.model.selectorSvgs[0].colorHex,
          opacity: annotation.model.selectorSvgs[0].opacity,
          strokeWidth: annotation.model.selectorSvgs[0].strokeWidth,
          svgData: annotation.model.selectorSvgs[0].svgData
        }
      }
      annotation.model = await TIMAAT.AnnotationService.updateAnnotation(annotation.model.id, annotationBaseInformation)
    },

    removeAnnotation: function () {
      // console.log("TCL: removeAnnotation: function()");
      if (!this.curAnnotation) return;
      TIMAAT.VideoPlayer.pause();
      $('#annotationDeleteModal').data('annotation', this.curAnnotation);
      $('#annotationDeleteModal').modal('show');
    },

    selectAnnotation: function (annotation) {
      // console.log("TCL: selectAnnotation: function(annotation)", annotation);
      if (annotation) {
        this.inspector.setItem(annotation, 'annotation');
        this.selectedElementType = 'annotation';
      } else {
        if (this.selectedElementType == 'annotation') {
          this.selectedElementType = null;
        }
        this.inspector.setItem(null);
      }
      if (this.curAnnotation == annotation && annotation != null) return;
      if (this.curAnnotation) this.curAnnotation.setSelected(false);
      this.curAnnotation = annotation;
      if (this.curAnnotation) {
        this.curAnnotation.setSelected(true);
        $('#videoPlayerAnnotationRemoveButton').prop('disabled', false);
        $('#videoPlayerAnnotationRemoveButton').removeAttr('disabled');
        $('#videoPlayerAnnotationRemoveButton').removeClass('annotation-controls__button--disabled');
      } else {
        $('#videoPlayerAnnotationRemoveButton').prop('disabled', true);
        $('#videoPlayerAnnotationRemoveButton').attr('disabled');
        $('#videoPlayerAnnotationRemoveButton').addClass('annotation-controls__button--disabled');
        // stop editing in progress
        if (TIMAAT.VideoPlayer.viewer.editTools.drawing()) {
          try {
            TIMAAT.VideoPlayer.viewer.editTools.stopDrawing();
          } catch (err) {
          }
          ;
        }
      }
      this.updateUI();
    },

    setCategorySet: function (categorySet) {
      // console.log("TCL: setCategorySet: function(categorySet)");
      // console.log("TCL: categorySet", categorySet);
      TIMAAT.VideoPlayer.curCategorySet = categorySet;
      TIMAAT.VideoPlayer.categoryAutocomplete.length = 0;
      if (categorySet) {
        $(categorySet.model.categories).each(function (index, category) {
          TIMAAT.VideoPlayer.categoryAutocomplete.push(category.name);
        });
      }
    },

    addAnalysisSegmentElement: function (type) {
      // console.log("TCL: addAnalysisSegmentElement: function("+type+")");
      TIMAAT.VideoPlayer.pause();
      TIMAAT.VideoPlayer.inspector.setItem(null, type);
    },

    removeAnalysisSegmentElement: function (type, data) {
      // console.log("TCL: removeAnalysisSegmentElement: type, data", type, data);
      if (!data) return;
      TIMAAT.VideoPlayer.pause();
      $('#segmentElementDeleteModal').data('type', type);
      $('#segmentElementDeleteModal').data('model', data.model);
      $('#segmentElementDeleteModal').find('.modal-title').html("Delete " + type);
      $('#segmentElementDeleteModal').find('.modal-body').html("Do you want to delete the selected " + type + "?");
      $('#segmentElementDeleteModal').modal('show');
    },

    updateAnalysisSegmentElement: async function (type, element) {
      // console.log("TCL: updateAnalysisSegmentElement:function -> element", element);
      // sync to server
      await TIMAAT.AnalysisListService.updateSegmentElement(type, element.model);
      switch (type) {
        case 'segment':
          await TIMAAT.VideoPlayer.sort(TIMAAT.VideoPlayer.curAnalysisList.analysisSegmentsUI);
          break;
        case 'sequence':
          await TIMAAT.AnalysisListService.updateSegmentElementTranslation(type, element.model.analysisSequenceTranslations[0]);
          TIMAAT.VideoPlayer.sort(TIMAAT.VideoPlayer.curAnalysisList.analysisSequencesUI);
          break;
        case 'take':
          await TIMAAT.AnalysisListService.updateSegmentElementTranslation(type, element.model.analysisTakeTranslations[0]);
          TIMAAT.VideoPlayer.sort(TIMAAT.VideoPlayer.curAnalysisList.analysisTakesUI);
          break;
        case 'scene':
          await TIMAAT.AnalysisListService.updateSegmentElementTranslation(type, element.model.analysisSceneTranslations[0]);
          TIMAAT.VideoPlayer.sort(TIMAAT.VideoPlayer.curAnalysisList.analysisScenesUI);
          break;
        case 'action':
          await TIMAAT.AnalysisListService.updateSegmentElementTranslation(type, element.model.analysisActionTranslations[0]);
          TIMAAT.VideoPlayer.sort(TIMAAT.VideoPlayer.curAnalysisList.analysisActionsUI);
          break;
      }

      // update UI list view
      element.updateUI();
      this.updateListUI();
      if (type == 'segment') {
        this.sortListUI();
      }
    },

    addMusicElement: function (type) {
      TIMAAT.VideoPlayer.pause();
      TIMAAT.VideoPlayer.inspector.setItem(null, type);
    },

    updateMusicFormElement: async function (musicFormElement) {
      // console.log("TCL: updateMusicFormElement:function -> musicFormElement: ", musicFormElement);
      // sync to server
      musicFormElement.model.musicFormElementType = await TIMAAT.MusicService.getMusicFormElementType(musicFormElement.model.musicFormElementType.id);
      await TIMAAT.MusicService.updateMusicFormElement(musicFormElement.model);
      await TIMAAT.VideoPlayer.sort(TIMAAT.VideoPlayer.curMusic.musicFormElementsUI);

      // update UI list view
      musicFormElement.updateUI();
      this.updateListUI();
      // this.sortListUI();
    },

    updateMusicChangeInTempoElement: async function (musicChangeInTempoElement) {
      // console.log("TCL: updateMusicChangeInTempoElement:function -> musicChangeInTempoElement: ", musicChangeInTempoElement);
      // sync to server
      musicChangeInTempoElement.model.changeInTempo = await TIMAAT.MusicService.getChangeInTempo(musicChangeInTempoElement.model.changeInTempo.id);
      await TIMAAT.MusicService.updateMusicChangeInTempoElement(musicChangeInTempoElement.model);
      await TIMAAT.VideoPlayer.sort(TIMAAT.VideoPlayer.curMusic.musicChangeInTempoElementsUI);

      // update UI list view
      musicChangeInTempoElement.updateUI();
      this.updateListUI();
      // this.sortListUI();
    },

    updateMusicArticulationElement: async function (musicArticulationElement) {
      // console.log("TCL: updateMusicArticulationElement:function -> musicArticulationElement: ", musicArticulationElement);
      // sync to server
      musicArticulationElement.model.articulation = await TIMAAT.MusicService.getArticulation(musicArticulationElement.model.articulation.id);
      await TIMAAT.MusicService.updateMusicArticulationElement(musicArticulationElement.model);
      await TIMAAT.VideoPlayer.sort(TIMAAT.VideoPlayer.curMusic.musicArticulationElementsUI);

      // update UI list view
      musicArticulationElement.updateUI();
      this.updateListUI();
      // this.sortListUI();
    },

    updateMusicDynamicsElement: async function (musicDynamicsElement) {
      // console.log("TCL: updateMusicDynamicsElement:function -> musicDynamicsElement: ", musicDynamicsElement);
      // sync to server
      musicDynamicsElement.model.dynamics = await TIMAAT.MusicService.getDynamics(musicDynamicsElement.model.dynamics.id);
      await TIMAAT.MusicService.updateMusicDynamicsElement(musicDynamicsElement.model);
      await TIMAAT.VideoPlayer.sort(TIMAAT.VideoPlayer.curMusic.musicDynamicsElementsUI);

      // update UI list view
      musicDynamicsElement.updateUI();
      this.updateListUI();
      // this.sortListUI();
    },

    updateMusicTextSettingElement: async function (musicTextSettingElement) {
      // console.log("TCL: updateMusicTextSettingElement:function -> musicTextSettingElement: ", musicTextSettingElement);
      // sync to server
      musicTextSettingElement.model.musicTextSettingElementType = await TIMAAT.MusicService.getMusicTextSettingElementType(musicTextSettingElement.model.musicTextSettingElementType.id);
      await TIMAAT.MusicService.updateMusicTextSettingElement(musicTextSettingElement.model);
      await TIMAAT.VideoPlayer.sort(TIMAAT.VideoPlayer.curMusic.musicTextSettingElementsUI);

      // update UI list view
      musicTextSettingElement.updateUI();
      this.updateListUI();
      // this.sortListUI();
    },

    removeMusicFormElement: function (data) {
      // console.log("TCL: removeMusicFormElement: data", data);
      if (!data) return;
      TIMAAT.VideoPlayer.pause();
      $('#musicFormElementDeleteModal').data('model', data.model);
      $('#musicFormElementDeleteModal').find('.modal-title').html("Delete music form element");
      $('#musicFormElementDeleteModal').find('.modal-body').html("Do you want to delete the selected element?");
      $('#musicFormElementDeleteModal').modal('show');
    },

    removeMusicChangeInTempoElement: function (data) {
      // console.log("TCL: removeMusicChangeInTempoElement: data", data);
      if (!data) return;
      TIMAAT.VideoPlayer.pause();
      $('#musicChangeInTempoElementDeleteModal').data('model', data.model);
      $('#musicChangeInTempoElementDeleteModal').find('.modal-title').html("Delete music change in tempo element");
      $('#musicChangeInTempoElementDeleteModal').find('.modal-body').html("Do you want to delete the selected element?");
      $('#musicChangeInTempoElementDeleteModal').modal('show');
    },

    removeMusicArticulationElement: function (data) {
      // console.log("TCL: removeMusicArticulationElement: data", data);
      if (!data) return;
      TIMAAT.VideoPlayer.pause();
      $('#musicArticulationElementDeleteModal').data('model', data.model);
      $('#musicArticulationElementDeleteModal').find('.modal-title').html("Delete music articulation element");
      $('#musicArticulationElementDeleteModal').find('.modal-body').html("Do you want to delete the selected element?");
      $('#musicArticulationElementDeleteModal').modal('show');
    },

    removeMusicDynamicsElement: function (data) {
      // console.log("TCL: removeMusicDynamicsElement: data", data);
      if (!data) return;
      TIMAAT.VideoPlayer.pause();
      $('#musicDynamicsElementDeleteModal').data('model', data.model);
      $('#musicDynamicsElementDeleteModal').find('.modal-title').html("Delete music dynamics element");
      $('#musicDynamicsElementDeleteModal').find('.modal-body').html("Do you want to delete the selected element?");
      $('#musicDynamicsElementDeleteModal').modal('show');
    },

    removeMusicTextSettingElement: function (data) {
      // console.log("TCL: removeMusicTextSettingElement: data", data);
      if (!data) return;
      TIMAAT.VideoPlayer.pause();
      $('#musicTextSettingElementDeleteModal').data('model', data.model);
      $('#musicTextSettingElementDeleteModal').find('.modal-title').html("Delete music text setting element");
      $('#musicTextSettingElementDeleteModal').find('.modal-body').html("Do you want to delete the selected element?");
      $('#musicTextSettingElementDeleteModal').modal('show');
    },

    offLinePublication: function () {
      let modal = $('#downloadPublicationModal');
      if (TIMAAT.VideoPlayer.mediaType == 'video' || TIMAAT.VideoPlayer.mediaType == 'image' || TIMAAT.VideoPlayer.mediaType == 'audio') {
        modal.find('a.downloadPlayerLink').attr('href', 'api/publication/offline/' + TIMAAT.VideoPlayer.curAnalysisList.id + '?authToken=' + TIMAAT.Service.session.token);
        modal.find('a.downloadMediumLink').attr('href', 'api/medium/' + TIMAAT.VideoPlayer.mediaType + '/' + this.model.medium.id + '/download' + '?token=' + this.model.medium.viewToken + '&force=true');
      }
      modal.modal('show');
    },

    managePublication: function () {
      TIMAAT.PublicationService.getSinglePublication(TIMAAT.VideoPlayer.curAnalysisList.id).then(publication => {
        // console.log("TCL: TIMAAT.VideoPlayer.viewer.on -> publication", publication);
        let modal = $('#analysisListPublicationModal');
        TIMAAT.VideoPlayer.publication = publication;
        TIMAAT.VideoPlayer._setupPublicationDialog(publication != null, publication != null && publication.access == 'protected');
        modal.find('.save-info').hide();
        modal.modal('show');
      }).catch(publication => {
        let modal = $('#analysisListPublicationModal');
        // console.log("managePublication:fail", publication);
        TIMAAT.VideoPlayer.publication = publication;
        TIMAAT.VideoPlayer._setupPublicationDialog(publication != null, publication != null && publication.access == 'protected');
        modal.find('.save-info').hide();
        modal.modal('show');
      });
    },

    sortListUI: function () {
      // console.log("TCL: sortListUI: function()");
      $(".analysis__list--li").sort(function (a, b) {
        if ((parseFloat($(b).attr('data-start-time'))) < (parseFloat($(a).attr('data-start-time')))) return 1;
        if ((parseFloat($(b).attr('data-start-time'))) > (parseFloat($(a).attr('data-start-time')))) return -1;
        if (!$(b).hasClass('annotationListSegment') && $(a).hasClass('annotationListSegment')) return -1;
        return 0;
      }).appendTo('#analysisList');

      const annotationSortFunction = (a, b) => {
        if (b.startTime < a.startTime) return 1;
        if (b.startTime > a.startTime) return -1;
        return 0;
      }

      // sort annotation markers in timeline
      const sortedVideoAnnotationList = TIMAAT.VideoPlayer.annotationList.filter(currentAnnotation => currentAnnotation.layerVisual).sort(annotationSortFunction)
      const sortedAudioAnnotationList = TIMAAT.VideoPlayer.annotationList.filter(currentAnnotation => currentAnnotation.layerAudio).sort(annotationSortFunction)

      const positionAnnotationMarker = (videoAnnotationList, markerFunction) => {
        let maxOffset = 0;
        if (videoAnnotationList.length > 0) {
          const marker = markerFunction(videoAnnotationList[0])
          marker.UIOffset = 0;
          for (let i = 1; i < videoAnnotationList.length; i++) {
            const iMarker = markerFunction(videoAnnotationList[i]);
            let curOffset = 0;
            let occupiedOffsets = [];

            for (var a = 0; a < i; a++) {
              if (videoAnnotationList[a].endTime >= videoAnnotationList[i].startTime) {
                const aMarker = markerFunction(videoAnnotationList[a]);
                occupiedOffsets.push(aMarker.UIOffset);
                while (occupiedOffsets.indexOf(curOffset) >= 0) curOffset++;
              }
            }
            iMarker.UIOffset = curOffset;
            if (curOffset > maxOffset) maxOffset = curOffset;
            iMarker.updateView();
          }
        }
        let minHeight = 30 + (maxOffset * 12);
        if (minHeight < 50) minHeight = 50;

        return minHeight
      }

      // position annotation markers in timeline
      const minHeightVideoAnnotationContainer = positionAnnotationMarker(sortedVideoAnnotationList, annotation => annotation.videoMarker)
      let minHeightAudioAnnotationContainer = positionAnnotationMarker(sortedAudioAnnotationList, annotation => annotation.audioMarker)

      if (!$('#timelineAudioLayer').is(':checked')) {
        minHeightAudioAnnotationContainer = 50
      }

      $('#timelineMarkerPane').css('min-height', minHeightVideoAnnotationContainer + 'px');

      let currentAudioMinHeight = parseInt($('.timeline__audio_annotation').css('min-height'))
      if (currentAudioMinHeight !== minHeightAudioAnnotationContainer) {
        $('.timeline__audio_annotation').css('min-height', minHeightAudioAnnotationContainer + 'px');
        this.drawWaveform()
      }
    },

    updateListUI: function (viaTimeUpdate = null) {
      if (TIMAAT.VideoPlayer.curAnalysisList != null && TIMAAT.VideoPlayer.curAnalysisList.analysisSegmentsUI != null) {
        TIMAAT.VideoPlayer.curAnalysisList.analysisSegmentsUI.forEach(function (segment) {
          segment.updateStatus(TIMAAT.VideoPlayer.medium.currentTime, viaTimeUpdate);
          if (TIMAAT.VideoPlayer.curAnalysisList.analysisSequencesUI != null)
            TIMAAT.VideoPlayer.curAnalysisList.analysisSequencesUI.forEach(function (sequence) {
              sequence.updateStatus(TIMAAT.VideoPlayer.medium.currentTime, viaTimeUpdate);
              if (TIMAAT.VideoPlayer.curAnalysisList.analysisTakesUI != null)
                TIMAAT.VideoPlayer.curAnalysisList.analysisTakesUI.forEach(function (take) {
                  take.updateStatus(TIMAAT.VideoPlayer.medium.currentTime, viaTimeUpdate);
                });
            });
          if (TIMAAT.VideoPlayer.curAnalysisList.analysisScenesUI != null)
            TIMAAT.VideoPlayer.curAnalysisList.analysisScenesUI.forEach(function (scene) {
              scene.updateStatus(TIMAAT.VideoPlayer.medium.currentTime, viaTimeUpdate);
              if (TIMAAT.VideoPlayer.curAnalysisList.analysisActionsUI != null)
                TIMAAT.VideoPlayer.curAnalysisList.analysisActionsUI.forEach(function (action) {
                  action.updateStatus(TIMAAT.VideoPlayer.medium.currentTime, viaTimeUpdate);
                });
            });
        });
      }

      if (TIMAAT.VideoPlayer.annotationList) for (let annotation of TIMAAT.VideoPlayer.annotationList) if (TIMAAT.VideoPlayer.duration > 0) annotation.updateStatus(TIMAAT.VideoPlayer.medium.currentTime);

      if (TIMAAT.VideoPlayer.curMusic != null && TIMAAT.VideoPlayer.medium) {
        if (TIMAAT.VideoPlayer.curMusic.musicFormElementsUI != null) {
          TIMAAT.VideoPlayer.curMusic.musicFormElementsUI.forEach(function (musicFormElement) {
            musicFormElement.updateStatus(TIMAAT.VideoPlayer.medium.currentTime, viaTimeUpdate);
          });
        }
        if (TIMAAT.VideoPlayer.curMusic.musicChangeInTempoElementsUI != null) {
          TIMAAT.VideoPlayer.curMusic.musicChangeInTempoElementsUI.forEach(function (musicChangeInTempoElement) {
            musicChangeInTempoElement.updateStatus(TIMAAT.VideoPlayer.medium.currentTime, viaTimeUpdate);
          });
        }
        if (TIMAAT.VideoPlayer.curMusic.musicArticulationElementsUI != null) {
          TIMAAT.VideoPlayer.curMusic.musicArticulationElementsUI.forEach(function (musicArticulationElement) {
            musicArticulationElement.updateStatus(TIMAAT.VideoPlayer.medium.currentTime, viaTimeUpdate);
          });
        }
        if (TIMAAT.VideoPlayer.curMusic.musicDynamicsElementsUI != null) {
          TIMAAT.VideoPlayer.curMusic.musicDynamicsElementsUI.forEach(function (musicDynamicsElement) {
            musicDynamicsElement.updateStatus(TIMAAT.VideoPlayer.medium.currentTime, viaTimeUpdate);
          });
        }
        if (TIMAAT.VideoPlayer.curMusic.musicTextSettingElementsUI != null) {
          TIMAAT.VideoPlayer.curMusic.musicTextSettingElementsUI.forEach(function (musicTextSettingElement) {
            musicTextSettingElement.updateStatus(TIMAAT.VideoPlayer.medium.currentTime, viaTimeUpdate);
          });
        }
      }
    },

    updateUI: function () {
      // console.log("TCL: updateUI: function()");
      var hasChanges = false;
      if (this.curAnnotation && this.curAnnotation.hasChanges()) hasChanges = true;
      TIMAAT.VideoPlayer.savePolygonCtrl.setEnabled(hasChanges);
      TIMAAT.VideoPlayer.animCtrl.updateUI();
      if (this.curAnnotation && this.curAnnotation.isAnimation() && $('#timelineVisualLayer').is(':checked'))
        $('#timelineKeyframePane').show();
      else $('#timelineKeyframePane').hide();
      let enabled = this.curAnnotation && this.curAnnotation.isActive() && this.curAnnotation.isOnKeyframe();
      if (TIMAAT.VideoPlayer.duration == 0 && this.curAnnotation) enabled = true; // switch for non time-based media (images)
      TIMAAT.VideoPlayer.editShapesCtrl.setEnabled(enabled);
      // $('.repeatButton').prop('disabled', TIMAAT.VideoPlayer.curAnnotation == null );
    },

    refreshTimelineElementsStructure: async function () {
      // console.log("TCL: refreshTimelineElementsStructure:function()");
      TIMAAT.VideoPlayer.clearTimelineElementsStructure();
      // Refresh current list data (required mainly for delete operations)
      if (TIMAAT.VideoPlayer.curAnalysisList) {
        TIMAAT.VideoPlayer.curAnalysisList = await TIMAAT.AnalysisListService.getAnalysisList(TIMAAT.VideoPlayer.curAnalysisList.id);
      }
      if (TIMAAT.VideoPlayer.curMusic) {
        TIMAAT.VideoPlayer.curMusic = await TIMAAT.MusicService.getMusic(TIMAAT.VideoPlayer.curMusic.id);
      }
      TIMAAT.VideoPlayer.createTimelineElementsStructure();
      TIMAAT.VideoPlayer.sortTimelineElementsStructure();
      TIMAAT.VideoPlayer.showTimelineElementsStructure();
      TIMAAT.VideoPlayer.updateListUI();
      TIMAAT.VideoPlayer.sortListUI();
      console.log("refreshed ")
    },

    clearTimelineElementsStructure: function () {
      // console.log("TCL: clearTimelineElementsStructure:function()");
      if (TIMAAT.VideoPlayer.curAnalysisList) {
        if (TIMAAT.VideoPlayer.curAnalysisList.analysisSegmentsUI) {
          TIMAAT.VideoPlayer.curAnalysisList.analysisSegmentsUI.forEach(function (segment) {
            segment.removeUI();
          });
        }
        if (TIMAAT.VideoPlayer.curAnalysisList.analysisSequencesUI) {
          TIMAAT.VideoPlayer.curAnalysisList.analysisSequencesUI.forEach(function (sequence) {
            sequence.removeUI();
          });
        }
        if (TIMAAT.VideoPlayer.curAnalysisList.analysisTakesUI) {
          TIMAAT.VideoPlayer.curAnalysisList.analysisTakesUI.forEach(function (take) {
            take.removeUI();
          });
        }
        if (TIMAAT.VideoPlayer.curAnalysisList.analysisScenesUI) {
          TIMAAT.VideoPlayer.curAnalysisList.analysisScenesUI.forEach(function (scene) {
            scene.removeUI();
          });
        }
        if (TIMAAT.VideoPlayer.curAnalysisList.analysisActionsUI) {
          TIMAAT.VideoPlayer.curAnalysisList.analysisActionsUI.forEach(function (action) {
            action.removeUI();
          });
        }
      }
      if (TIMAAT.VideoPlayer.curMusic) {
        if (TIMAAT.VideoPlayer.curMusic.musicFormElementsUI) {
          TIMAAT.VideoPlayer.curMusic.musicFormElementsUI.forEach(function (musicFormElement) {
            musicFormElement.removeUI();
          });
        }
        if (TIMAAT.VideoPlayer.curMusic.musicChangeInTempoElementsUI) {
          TIMAAT.VideoPlayer.curMusic.musicChangeInTempoElementsUI.forEach(function (musicChangeInTempoElement) {
            musicChangeInTempoElement.removeUI();
          });
        }
        if (TIMAAT.VideoPlayer.curMusic.musicArticulationElementsUI) {
          TIMAAT.VideoPlayer.curMusic.musicArticulationElementsUI.forEach(function (musicArticulationElement) {
            musicArticulationElement.removeUI();
          });
        }
        if (TIMAAT.VideoPlayer.curMusic.musicDynamicsElementsUI) {
          TIMAAT.VideoPlayer.curMusic.musicDynamicsElementsUI.forEach(function (musicDynamicsElement) {
            musicDynamicsElement.removeUI();
          });
        }
        if (TIMAAT.VideoPlayer.curMusic.musicTextSettingElementsUI) {
          TIMAAT.VideoPlayer.curMusic.musicTextSettingElementsUI.forEach(function (musicTextSettingElement) {
            musicTextSettingElement.removeUI();
          });
        }
      }
    },

    createTimelineElementsStructure: function () {
      // console.log("TCL: createTimelineElementsStructure:function()");
      // setup segment model
      if (TIMAAT.VideoPlayer.curAnalysisList) {
        TIMAAT.VideoPlayer.curAnalysisList.analysisSegmentsUI = Array();
        TIMAAT.VideoPlayer.curAnalysisList.analysisSequencesUI = Array();
        TIMAAT.VideoPlayer.curAnalysisList.analysisTakesUI = Array();
        TIMAAT.VideoPlayer.curAnalysisList.analysisScenesUI = Array();
        TIMAAT.VideoPlayer.curAnalysisList.analysisActionsUI = Array();
        TIMAAT.VideoPlayer.curAnalysisList.analysisSegments.forEach(function (segment) {
          TIMAAT.VideoPlayer.curAnalysisList.analysisSegmentsUI.push(new TIMAAT.AnalysisSegment(segment));
          segment.analysisSequences.forEach(function (sequence) {
            sequence.segmentId = segment.id;
            TIMAAT.VideoPlayer.curAnalysisList.analysisSequencesUI.push(new TIMAAT.AnalysisSequence(sequence));
            sequence.analysisTakes.forEach(function (take) {
              take.sequenceId = sequence.id;
              TIMAAT.VideoPlayer.curAnalysisList.analysisTakesUI.push(new TIMAAT.AnalysisTake(take));
            });
          });
          segment.analysisScenes.forEach(function (scene) {
            scene.segmentId = segment.id;
            TIMAAT.VideoPlayer.curAnalysisList.analysisScenesUI.push(new TIMAAT.AnalysisScene(scene));
            scene.analysisActions.forEach(function (action) {
              action.sceneId = scene.id;
              TIMAAT.VideoPlayer.curAnalysisList.analysisActionsUI.push(new TIMAAT.AnalysisAction(action));
            });
          });
        });
      }
      if (TIMAAT.VideoPlayer.curMusic) {
        TIMAAT.VideoPlayer.curMusic.musicFormElementsUI = Array();
        TIMAAT.VideoPlayer.curMusic.musicChangeInTempoElementsUI = Array();
        TIMAAT.VideoPlayer.curMusic.musicArticulationElementsUI = Array();
        TIMAAT.VideoPlayer.curMusic.musicDynamicsElementsUI = Array();
        TIMAAT.VideoPlayer.curMusic.musicTextSettingElementsUI = Array();
        TIMAAT.VideoPlayer.curMusic.musicFormElementList.forEach(function (musicFormElement) {
          TIMAAT.VideoPlayer.curMusic.musicFormElementsUI.push(new TIMAAT.MusicFormElement(musicFormElement));
        });
        TIMAAT.VideoPlayer.curMusic.musicChangeInTempoElementList.forEach(function (musicChangeInTempoElement) {
          TIMAAT.VideoPlayer.curMusic.musicChangeInTempoElementsUI.push(new TIMAAT.MusicChangeInTempoElement(musicChangeInTempoElement));
        });
        TIMAAT.VideoPlayer.curMusic.musicArticulationElementList.forEach(function (musicArticulationElement) {
          TIMAAT.VideoPlayer.curMusic.musicArticulationElementsUI.push(new TIMAAT.MusicArticulationElement(musicArticulationElement));
        });
        TIMAAT.VideoPlayer.curMusic.musicDynamicsElementList.forEach(function (musicDynamicsElement) {
          TIMAAT.VideoPlayer.curMusic.musicDynamicsElementsUI.push(new TIMAAT.MusicDynamicsElement(musicDynamicsElement));
        });
        TIMAAT.VideoPlayer.curMusic.musicTextSettingElementList.forEach(function (musicTextSettingElement) {
          TIMAAT.VideoPlayer.curMusic.musicTextSettingElementsUI.push(new TIMAAT.MusicTextSettingElement(musicTextSettingElement));
        });
      }
    },

    sortTimelineElementsStructure: function () {
      // console.log("TCL: sortTimelineElementsStructure:function()");
      if (this.curAnalysisList) {
        this.sort(this.curAnalysisList.analysisSegmentsUI);
        this.sort(this.curAnalysisList.analysisSequencesUI);
        this.sort(this.curAnalysisList.analysisTakesUI);
        this.sort(this.curAnalysisList.analysisScenesUI);
        this.sort(this.curAnalysisList.analysisActionsUI);
      }
      if (this.curMusic) {
        this.sort(this.curMusic.musicFormElementsUI);
        this.sort(this.curMusic.musicChangeInTempoElementsUI);
        this.sort(this.curMusic.musicArticulationElementsUI);
        this.sort(this.curMusic.musicDynamicsElementsUI);
        this.sort(this.curMusic.musicTextSettingElementsUI);
      }
    },

    showTimelineElementsStructure: function () {
      // console.log("TCL: showTimelineElementsStructure:function()");
      // build annotation list from model
      $('.js-timeline__section-sub-lane').hide();
      if (this.curAnalysisList != null && this.curAnalysisList.analysisSegmentsUI != null && this.curAnalysisList.analysisSegmentsUI.length > 0) {
        this.curAnalysisList.analysisSegmentsUI.forEach(function (segment) {
          segment.addUI();
        });
        $('#timelinePaneSegment').show();
        if (this.curAnalysisList.analysisSequencesUI != null && this.curAnalysisList.analysisSequencesUI.length > 0) {
          this.curAnalysisList.analysisSequencesUI.forEach(function (sequence) {
            sequence.addUI();
          });
          $('#timelinePaneSequence').show();
          if (this.curAnalysisList.analysisTakesUI != null && this.curAnalysisList.analysisTakesUI.length > 0) {
            this.curAnalysisList.analysisTakesUI.forEach(function (take) {
              take.addUI();
            });
            $('#timelinePaneTake').show();
          }
        }
        if (this.curAnalysisList.analysisScenesUI != null && this.curAnalysisList.analysisScenesUI.length > 0) {
          this.curAnalysisList.analysisScenesUI.forEach(function (scene) {
            scene.addUI();
          });
          $('#timelinePaneScene').show();
          if (this.curAnalysisList.analysisActionsUI != null && this.curAnalysisList.analysisActionsUI.length > 0) {
            this.curAnalysisList.analysisActionsUI.forEach(function (action) {
              action.addUI();
            });
            $('#timelinePaneAction').show();
          }
        }
      }
      if (this.curMusic != null) {
        if (this.curMusic.musicFormElementsUI != null) {
          this.curMusic.musicFormElementsUI.forEach(function (musicFormElement) {
            musicFormElement.addUI();
          });
          $('#timelinePaneMusicFormElement').show();
        }
        if (this.curMusic.musicChangeInTempoElementsUI != null) {
          this.curMusic.musicChangeInTempoElementsUI.forEach(function (musicChangeInTempoElement) {
            musicChangeInTempoElement.addUI();
          });
          $('#timelinePaneMusicChangeInTempo').show();
        }
        if (this.curMusic.musicArticulationElementsUI != null) {
          this.curMusic.musicArticulationElementsUI.forEach(function (musicArticulationElement) {
            musicArticulationElement.addUI();
          });
          $('#timelinePaneMusicArticulation').show();
        }
        if (this.curMusic.musicDynamicsElementsUI != null) {
          this.curMusic.musicDynamicsElementsUI.forEach(function (musicDynamicsElement) {
            musicDynamicsElement.addUI();
          });
          $('#timelinePaneMusicDynamics').show();
        }
        if (this.curMusic.musicTextSettingElementsUI != null) {
          this.curMusic.musicTextSettingElementsUI.forEach(function (musicTextSettingElement) {
            musicTextSettingElement.addUI();
          });
          $('#timelinePaneMusicTextSetting').show();
        }
      }
    },

    isPlaying: function () {
      if (!this.medium || !this.medium.currentTime) return false;
      // return this.medium.currentTime > 0 && !this.medium.paused && !this.medium.ended && this.medium.readyState > this.medium.HAVE_CURRENT_DATA; //* this.medium.HAVE_CURRENT_DATA == 2
      return this.medium.currentTime > 0 && !this.medium.paused && !this.medium.ended; //! readyState is 1 when clicking a segment while time is not within segment time interval, ignoring to pause
    },

    pause: function () {
      if (!this.medium || this.mediaType != 'video' && this.mediaType != 'audio')
        return;
      if (this.isPlaying()) {
        this.medium.pause();
      }
      $('.toggle-play-button__icon').removeClass('fa-pause').addClass('fa-play');
      $('.togglePlayButton').prop('title', 'Play (space)');
    },

    play: function () {
      if (!this.medium || this.mediaType != 'video' && this.mediaType != 'audio')
        return;
      if (!this.isPlaying()) {
        this.medium.play();
      }
      $('.toggle-play-button__icon').removeClass('fa-play').addClass('fa-pause');
      $('.togglePlayButton').prop('title', 'Pause (space)');
    },

    jumpTo: function (timeInMilliseconds) {
      // console.log("TCL: jumpTo() -> timeInMilliseconds: ", timeInMilliseconds);
      if (!this.medium || this.mediaType != 'video' && this.mediaType != 'audio') return;
      this.medium.currentTime = Math.floor(timeInMilliseconds) / 1000;
      // this.updateListUI(); // obsolete as updateListUI() is called within on(timeupdate), which is also called upon clicking within the time slider
    },

    jumpVisible: function (startInMilliseconds, endInMilliseconds) {
      // console.log("TCL: jumpVisible: function(startInSeconds, endInSeconds)", startInSeconds, endInSeconds);
      if (!this.medium || this.mediaType != 'video' && this.mediaType != 'audio') return;
      let curTime = this.medium.currentTime * 1000;
      if (curTime < startInMilliseconds || curTime >= endInMilliseconds) this.medium.currentTime = startInMilliseconds / 1000;
      // this.updateListUI(); // obsolete as on timeupdate is called afterward
    },

    initEmptyAnalysisListMenu: function () {
      $('#analysisChooser').append('<option>No analyses available or accessible. You can create a new one.</option>');
      $('#analysisChooser').addClass("item--disabled");
      $('.addAnnotation').addClass("item--disabled");

      $('#analysisSegmentOptions').addClass("item--disabled");
      $('#analysisSegmentOptions').removeClass("dropdown-toggle");
      $('.addSegmentElement').addClass("item--disabled");
      $('.addSegmentElement').removeAttr('onclick');
      $('#analysisPublication').addClass("item--disabled");
      $('#analysisPublication').removeAttr('onclick');
      $('#analysisEdit').addClass("item--disabled");
      $('#analysisEdit').removeAttr('onclick');
      $('#analysisManage').addClass("item--disabled");
      $('#analysisManage').removeAttr('onclick');
      $('#analysisDelete').addClass("item--disabled");
      $('#analysisDelete').removeAttr('onclick');
      $('#analysisOfflinePublication').addClass("item--disabled");
      $('#analysisOfflinePublication').removeAttr('onclick');
      // $('#videoPlayerAnnotationQuickAddButton').prop('disabled', true);
      // $('#videoPlayerAnnotationQuickAddButton').attr('disabled');
      // $('#videoPlayerAnnotationQuickAddButton').addClass('annotation-controls__button--disabled');
      $('.leaflet-control-container').hide();
    },

    initAnalysisListMenu: function () {
      // console.log("initAnalysisListMenu");
      if (this.currentPermissionLevel && this.currentPermissionLevel >= 2) {
        $('#analysisChooser').removeClass("item--disabled");
        $('.addSegment').removeClass("item--disabled");
        if (this.mediaType != 'video') $('.addSegment').addClass("item--disabled");
        $('.addSegment').attr('onclick', 'TIMAAT.VideoPlayer.addAnalysisSegmentElement("segment")');
        if (this.mediaType != 'video') $('.addSegment').attr('onclick', '');
        $('.addAnnotation').removeClass("item--disabled");

        $('#analysisSegmentOptions').removeClass("item--disabled");
        $('#analysisSegmentOptions').addClass("dropdown-toggle");
        $('#analysisEdit').removeClass("item--disabled");
        $('#analysisEdit').attr('onclick', 'TIMAAT.VideoPlayer.editAnalysisList()');
        if (this.currentPermissionLevel >= 3) {
          $('#analysisManage').removeClass("item--disabled");
          $('#analysisManage').attr('onclick', 'TIMAAT.VideoPlayer.manageAnalysisList()');
        }
        $('#analysisDelete').removeClass("item--disabled");
        $('#analysisDelete').attr('onclick', 'TIMAAT.VideoPlayer.removeAnalysisList()');
        $('#analysisPublication').removeClass("item--disabled");
        $('#analysisPublication').attr('onclick', 'TIMAAT.VideoPlayer.managePublication()');
        $('#analysisOfflinePublication').removeClass("item--disabled");
        $('#analysisOfflinePublication').attr('onclick', 'TIMAAT.VideoPlayer.offLinePublication()');
        // $('#videoPlayerAnnotationQuickAddButton').prop('disabled', false);
        // $('#videoPlayerAnnotationQuickAddButton').removeAttr('disabled');
        // $('#videoPlayerAnnotationQuickAddButton').removeClass('annotation-controls__button--disabled');
      }
      $('.leaflet-control-container').show();
    },

    _updatePublicationSettings: function () {
      let dialog = $('#analysisListPublicationModal');
      let enabled = $('#publishAnalysisSwitch').prop('checked');
      let restricted = $('#publicationProtectedSwitch').prop('checked');
      let username = (dialog.find('.username').val() && restricted) ? dialog.find('.username').val() : '';
      let password = (dialog.find('.password').val() && restricted) ? dialog.find('.password').val() : '';
      $('#publicationSettingsSubmitButton').prop('disabled', true);
      $('#publicationSettingsSubmitButton i.login-spinner').removeClass('d-none');
      if (enabled) {
        let publication = (TIMAAT.VideoPlayer.publication) ? TIMAAT.VideoPlayer.publication : {id: 0};
        publication.access = (restricted) ? 'protected' : 'public';
        publication.mediaCollectionAnalysisList = null;
        publication.ownerId = TIMAAT.Service.session.id;
        publication.settings = null;
        publication.slug = TIMAAT.Util.createUUIDv4();
        publication.mediumAnalysisListId = TIMAAT.VideoPlayer.curAnalysisList.id;
        publication.title = dialog.find('.publicationTitle').val();
        publication.credentials = JSON.stringify({
          scheme: 'password',
          user: username,
          password: password,
        });
        if (TIMAAT.VideoPlayer.publication) {
          TIMAAT.PublicationService.updateSinglePublication(publication).then(publication => {
            // console.log("TCL: TIMAAT.VideoPlayer.viewer.on -> publication", publication);
            TIMAAT.VideoPlayer.publication = publication;
            TIMAAT.VideoPlayer._setupPublicationDialog(publication != null, publication != null && publication.access == 'protected');
            $('#publicationSettingsSubmitButton').prop('disabled', false);
            $('#publicationSettingsSubmitButton i.login-spinner').addClass('d-none');
            dialog.find('.save-info').show().delay(1000).fadeOut();
          }).catch(e => {
            $('#publicationSettingsSubmitButton').prop('disabled', false);
            $('#publicationSettingsSubmitButton i.login-spinner').addClass('d-none');
          })
        } else {
          TIMAAT.PublicationService.createSinglePublication(publication).then(publication => {
            // console.log("TCL: TIMAAT.VideoPlayer.viewer.on -> publication", publication);
            TIMAAT.VideoPlayer.publication = publication;
            TIMAAT.VideoPlayer._setupPublicationDialog(publication != null, publication != null && publication.access == 'protected');
            $('#publicationSettingsSubmitButton').prop('disabled', false);
            $('#publicationSettingsSubmitButton i.login-spinner').addClass('d-none');
            dialog.find('.save-info').show().delay(1000).fadeOut();
          }).catch(e => {
            $('#publicationSettingsSubmitButton').prop('disabled', false);
            $('#publicationSettingsSubmitButton i.login-spinner').addClass('d-none');
          })
        }
      } else {
        TIMAAT.PublicationService.deleteSinglePublication(TIMAAT.VideoPlayer.curAnalysisList.id).then(status => {
          TIMAAT.VideoPlayer.publication = null;
          TIMAAT.VideoPlayer._setupPublicationDialog(false, false);
          $('#publicationSettingsSubmitButton').prop('disabled', false);
          $('#publicationSettingsSubmitButton i.login-spinner').addClass('d-none');
          dialog.find('.save-info').show().delay(1000).fadeOut();
        }).catch(e => {
          $('#publicationSettingsSubmitButton').prop('disabled', false);
          $('#publicationSettingsSubmitButton i.login-spinner').addClass('d-none');
        })
      }

    },

    _setupPublicationDialog: function (enabled, restricted) {
      $('#publishAnalysisSwitch').prop('checked', enabled);
      $('#publicationProtectedSwitch').prop('checked', restricted);
      let credentials = {};
      try {
        credentials = JSON.parse(TIMAAT.VideoPlayer.publication.credentials);
      } catch (e) {
        credentials = {};
      }
      let dialog = $('#analysisListPublicationModal');
      let title = (TIMAAT.VideoPlayer.publication) ? TIMAAT.VideoPlayer.publication.title : '';
      let username = (credentials.user && enabled) ? credentials.user : '';
      let password = (credentials.password && enabled) ? credentials.password : '';
      let url = (TIMAAT.VideoPlayer.publication) ? window.location.protocol + '//' + window.location.host + window.location.pathname + 'publication/' + TIMAAT.VideoPlayer.publication.slug + '/' : '';
      dialog.find('.icon--protected').removeClass('fa-lock').removeClass('fa-lock-open');
      if (restricted) dialog.find('.icon--protected').addClass('fa-lock'); else dialog.find('.icon--protected').addClass('fa-lock-open');

      dialog.find('.publicationTitle').prop('disabled', !enabled);
      dialog.find('#publicationProtectedSwitch').prop('disabled', !enabled);
      dialog.find('.username').prop('disabled', !enabled || !restricted);
      dialog.find('.username').val(username);
      dialog.find('.password').prop('disabled', !enabled || !restricted);
      dialog.find('.password').val(password);
      dialog.find('.password').attr('type', 'password');
      $('#publicationSettingsSubmitButton').prop('disabled', enabled && restricted && username == '' && password == '');

      if (enabled) {
        dialog.find('.publicationTitle').val(title);
        if (url.length > 0) url = '<a href="' + url + '" target="_blank">' + url + '</a>';
        else url = '- Publication link will be available after saving -';
        dialog.find('.js-analysis-list-menu__publication--url').html(url);
      } else {
        dialog.find('.publicationTitle').val('');
        dialog.find('.js-analysis-list-menu__publication--url').html('- Medium not published -');
      }
    },

    _updateAnimations: function () {
      if (!TIMAAT.VideoPlayer.medium || TIMAAT.VideoPlayer.medium.paused || !TIMAAT.VideoPlayer.annotationList || !TIMAAT.VideoPlayer.curAnalysisList) return;

      // repeat video section if control activated
      if (TIMAAT.VideoPlayer.repeatMode === "ANNOTATION") {
        if (TIMAAT.VideoPlayer.curAnnotation) {
          // repeat annotation
          if (TIMAAT.VideoPlayer.medium.currentTime < TIMAAT.VideoPlayer.curAnnotation.startTime / 1000
              || TIMAAT.VideoPlayer.medium.currentTime > TIMAAT.VideoPlayer.curAnnotation.endTime / 1000)
            TIMAAT.VideoPlayer.jumpTo(TIMAAT.VideoPlayer.curAnnotation.startTime);
        } else {
          // repeat segment //! TODO repeat segment currently not working
          let curSegment = null;
          for (let index = TIMAAT.VideoPlayer.curAnalysisList.analysisSegmentsUI.length - 1; index >= 0; index--) {
            let segment = TIMAAT.VideoPlayer.curAnalysisList.analysisSegmentsUI[index];
            if (segment.active) {
              curSegment = segment;
              break;
            }
          }

          if (curSegment) {
            if (TIMAAT.VideoPlayer.medium.currentTime < curSegment.model.startTime / 1000
                || TIMAAT.VideoPlayer.medium.currentTime > curSegment.model.endTime / 1000)
              TIMAAT.VideoPlayer.jumpTo(curSegment.model.startTime);
          }
        }
        // TODO: repeat music structure element
      } else if (TIMAAT.VideoPlayer.repeatMode === "SELECTION") {
        if (TIMAAT.VideoPlayer.currentTemporaryWaveformMarker) {
          const startTime = TIMAAT.VideoPlayer.currentTemporaryWaveformMarker.startXPercentage * TIMAAT.VideoPlayer.duration
          const endTime = TIMAAT.VideoPlayer.currentTemporaryWaveformMarker.endXPercentage * TIMAAT.VideoPlayer.duration
          const currentTime = TIMAAT.VideoPlayer.medium.currentTime * 1000

          if (currentTime < startTime || currentTime > endTime)
            TIMAAT.VideoPlayer.jumpTo(startTime);
        }
      }

      for (let annotation of TIMAAT.VideoPlayer.annotationList)
        if (TIMAAT.VideoPlayer.duration > 0) annotation.updateStatus(TIMAAT.VideoPlayer.medium.currentTime);

      // update timeline
      TIMAAT.VideoPlayer.timeline.updateIndicator();
    },

    _analysisListAdded: function (analysisList) {
      // console.log("TCL: _analysisListAdded: function(analysisList) - analysisList", analysisList);
      var wasEmpty = TIMAAT.VideoPlayer.model.analysisLists.length == 0;
      TIMAAT.VideoPlayer.model.analysisLists.push(analysisList);

      analysisList.ui = $('<option value="' + analysisList.id + '">' + TIMAAT.Util.getDefaultTranslation(analysisList, 'mediumAnalysisListTranslations', 'title') + '</option>');
      analysisList.ui.data('list', analysisList);

      // update UI
      if (wasEmpty) $('#analysisChooser').empty();
      $('#analysisChooser').append(analysisList.ui);
      $('#analysisChooser').val(analysisList.id);
      TIMAAT.VideoPlayer.currentPermissionLevel = 4; //* creator is admin of new list
      $('#analysisChooser').trigger('change');
      // TIMAAT.VideoPlayer.inspector.open('inspectorMetadata');
      TIMAAT.VideoPlayer.inspector.close();
      TIMAAT.VideoPlayer.initAnalysisListMenu();
    },

    _analysisListRemoved: async function (analysisList) {
      // console.log("TCL: _analysisListRemoved:function -> analysisList", analysisList);
      // sync to server
      TIMAAT.AnalysisListService.removeAnalysisList(analysisList);

      // remove from model lists
      var index = TIMAAT.VideoPlayer.model.analysisLists.findIndex(({id}) => id === analysisList.id);
      if (index > -1) TIMAAT.VideoPlayer.model.analysisLists.splice(index, 1);
      // update UI list view
      $('#analysisChooser').find('[value="' + analysisList.id + '"]').remove();
      $('#analysisChooser').trigger('change');
      if (TIMAAT.VideoPlayer.model.analysisLists.length == 0) {
        await TIMAAT.VideoPlayer.setupAnalysisList(null);
        TIMAAT.VideoPlayer.initEmptyAnalysisListMenu();
        // TIMAAT.URLHistory.setURL(null, 'Video Player', '#analysis');
      }
      // update annotation UI
    },

    _annotationAdded: function (annotation, openInspector = true) {
      // console.log("TCL: _annotationAdded: function(annotation): ", annotation);
      // console.log("TCL: annotation", annotation);
      TIMAAT.VideoPlayer.annotationList.push(annotation);
      TIMAAT.VideoPlayer.curAnalysisList.annotations.push(annotation.model);
      annotation.updateUI();
      if (TIMAAT.VideoPlayer.duration > 0) annotation.updateStatus(TIMAAT.VideoPlayer.medium.currentTime);
      TIMAAT.VideoPlayer.updateListUI();
      TIMAAT.VideoPlayer.sortListUI();
      // if ( openInspector ) TIMAAT.VideoPlayer.selectAnnotation(annotation);
      TIMAAT.VideoPlayer.selectAnnotation(annotation);
      annotation.setActive(true);
      TIMAAT.VideoPlayer.editShapesCtrl.setEnabled(true);
    },

    _annotationRemoved: function (annotation) {
      // console.log("TCL: _annotationRemoved: function(annotation)");
      // console.log("TCL: annotation", annotation);
      // sync to server
      TIMAAT.AnnotationService.removeAnnotation(annotation);
      var index = TIMAAT.VideoPlayer.annotationList.indexOf(annotation);
      if (index > -1) TIMAAT.VideoPlayer.annotationList.splice(index, 1);
      // remove from model list
      var anno = TIMAAT.VideoPlayer.curAnalysisList.annotations.find(x => x.id === annotation.model.id);
      index = TIMAAT.VideoPlayer.curAnalysisList.annotations.indexOf(anno);
      if (index > -1) TIMAAT.VideoPlayer.curAnalysisList.annotations.splice(index, 1);

      // update UI list view
      annotation.remove();
      // console.log("TCL: $ -> TIMAAT.VideoPlayer.updateListUI()");
      TIMAAT.VideoPlayer.updateListUI();
      TIMAAT.VideoPlayer.sortListUI();
      TIMAAT.VideoPlayer.selectAnnotation(null);
    },

    _segmentAdded: function (segment, openInspector = true) {
      // console.log("TCL: _segmentAdded: function(segment): ", segment);
      if (!TIMAAT.VideoPlayer.curAnalysisList.analysisSegmentsUI) {
        TIMAAT.VideoPlayer.curAnalysisList.analysisSegmentsUI = [];
      }
      TIMAAT.VideoPlayer.curAnalysisList.analysisSegmentsUI.push(segment);
      TIMAAT.VideoPlayer.sort(TIMAAT.VideoPlayer.curAnalysisList.analysisSegmentsUI);
      TIMAAT.VideoPlayer.jumpTo(segment.model.startTime);
      TIMAAT.VideoPlayer.selectedElementType = 'segment';
      segment.addUI();
      TIMAAT.VideoPlayer.curSegment = segment;
      TIMAAT.VideoPlayer.curSegment.timelineView[0].classList.replace('bg-info', 'bg-primary');
      TIMAAT.VideoPlayer.curSegment.timelineView[0].classList.add('bg-primary');

      if (openInspector) {
        TIMAAT.VideoPlayer.inspector.setItem(segment, 'segment');
        TIMAAT.VideoPlayer.inspector.open('inspectorMetadata');
      }

      // update UI
      $('#timelinePaneSegment').show();
      // console.log("TCL: $ -> TIMAAT.VideoPlayer.updateListUI()");
      TIMAAT.VideoPlayer.updateListUI();
      TIMAAT.VideoPlayer.sortListUI();
    },

    _sequenceAdded: function (sequence, openInspector = true) {
      // console.log("TCL: _sequenceAdded: function(sequence)", sequence);
      if (!TIMAAT.VideoPlayer.curAnalysisList.analysisSequencesUI) {
        TIMAAT.VideoPlayer.curAnalysisList.analysisSequencesUI = [];
      }
      TIMAAT.VideoPlayer.curAnalysisList.analysisSequencesUI.push(sequence);
      TIMAAT.VideoPlayer.sort(TIMAAT.VideoPlayer.curAnalysisList.analysisSequencesUI);
      TIMAAT.VideoPlayer.jumpTo(sequence.model.startTime);
      TIMAAT.VideoPlayer.selectedElementType = 'sequence';
      sequence.addUI();
      TIMAAT.VideoPlayer.curSequence = sequence;
      TIMAAT.VideoPlayer.curSequence.timelineView[0].classList.replace('bg-info', 'bg-primary');
      TIMAAT.VideoPlayer.curSequence.timelineView[0].classList.add('bg-primary');

      if (openInspector) {
        // TIMAAT.VideoPlayer.selectAnnotation(null);
        TIMAAT.VideoPlayer.inspector.setItem(sequence, 'sequence');
        TIMAAT.VideoPlayer.inspector.open('inspectorMetadata');
      }

      // update UI
      $('#timelinePaneSequence').show();
      // console.log("TCL: $ -> TIMAAT.VideoPlayer.updateListUI()");
      TIMAAT.VideoPlayer.updateListUI();
      // TIMAAT.VideoPlayer.sortListUI();
    },

    _takeAdded: function (take, openInspector = true) {
      // console.log("TCL: _takeAdded: function(take)", take);
      if (!TIMAAT.VideoPlayer.curAnalysisList.analysisTakesUI) {
        TIMAAT.VideoPlayer.curAnalysisList.analysisTakesUI = [];
      }
      TIMAAT.VideoPlayer.curAnalysisList.analysisTakesUI.push(take);
      TIMAAT.VideoPlayer.sort(TIMAAT.VideoPlayer.curAnalysisList.analysisTakesUI);
      TIMAAT.VideoPlayer.jumpTo(take.model.startTime);
      TIMAAT.VideoPlayer.selectedElementType = 'take';
      take.addUI();
      TIMAAT.VideoPlayer.curTake = take;
      TIMAAT.VideoPlayer.curTake.timelineView[0].classList.replace('bg-info', 'bg-primary');
      TIMAAT.VideoPlayer.curTake.timelineView[0].classList.add('bg-primary');

      if (openInspector) {
        // TIMAAT.VideoPlayer.selectAnnotation(null);
        TIMAAT.VideoPlayer.inspector.setItem(take, 'take');

        TIMAAT.VideoPlayer.inspector.open('inspectorMetadata');
      }

      // update UI
      $('#timelinePaneTake').show();
      // console.log("TCL: $ -> TIMAAT.VideoPlayer.updateListUI()");
      TIMAAT.VideoPlayer.updateListUI();
      // TIMAAT.VideoPlayer.sortListUI();
    },

    _sceneAdded: function (scene, openInspector = true) {
      // console.log("TCL: _sceneAdded: function(scene)", scene);
      if (!TIMAAT.VideoPlayer.curAnalysisList.analysisScenesUI) {
        TIMAAT.VideoPlayer.curAnalysisList.analysisScenesUI = [];
      }
      TIMAAT.VideoPlayer.curAnalysisList.analysisScenesUI.push(scene);
      TIMAAT.VideoPlayer.sort(TIMAAT.VideoPlayer.curAnalysisList.analysisScenesUI);
      TIMAAT.VideoPlayer.jumpTo(scene.model.startTime);
      TIMAAT.VideoPlayer.selectedElementType = 'scene';
      scene.addUI();
      TIMAAT.VideoPlayer.curScene = scene;
      TIMAAT.VideoPlayer.curScene.timelineView[0].classList.replace('bg-info', 'bg-primary');
      TIMAAT.VideoPlayer.curScene.timelineView[0].classList.add('bg-primary');

      if (openInspector) {
        // TIMAAT.VideoPlayer.selectAnnotation(null);
        TIMAAT.VideoPlayer.inspector.setItem(scene, 'scene');
        TIMAAT.VideoPlayer.inspector.open('inspectorMetadata');
      }

      // update UI
      $('#timelinePaneScene').show();
      // console.log("TCL: $ -> TIMAAT.VideoPlayer.updateListUI()");
      TIMAAT.VideoPlayer.updateListUI();
      // TIMAAT.VideoPlayer.sortListUI();
    },

    _actionAdded: function (action, openInspector = true) {
      // console.log("TCL: _actionAdded: function(action)", action);
      if (!TIMAAT.VideoPlayer.curAnalysisList.analysisActionsUI) {
        TIMAAT.VideoPlayer.curAnalysisList.analysisActionsUI = [];
      }
      TIMAAT.VideoPlayer.curAnalysisList.analysisActionsUI.push(action);
      TIMAAT.VideoPlayer.sort(TIMAAT.VideoPlayer.curAnalysisList.analysisActionsUI);
      TIMAAT.VideoPlayer.jumpTo(action.model.startTime);
      TIMAAT.VideoPlayer.selectedElementType = 'action';
      action.addUI();
      TIMAAT.VideoPlayer.curAction = action;
      TIMAAT.VideoPlayer.curAction.timelineView[0].classList.replace('bg-info', 'bg-primary');
      TIMAAT.VideoPlayer.curAction.timelineView[0].classList.add('bg-primary');

      if (openInspector) {
        // TIMAAT.VideoPlayer.selectAnnotation(null);
        TIMAAT.VideoPlayer.inspector.setItem(action, 'action');
        TIMAAT.VideoPlayer.inspector.open('inspectorMetadata');
      }

      // update UI
      $('#timelinePaneAction').show();
      // console.log("TCL: $ -> TIMAAT.VideoPlayer.updateListUI()");
      TIMAAT.VideoPlayer.updateListUI();
      // TIMAAT.VideoPlayer.sortListUI();
    },

    _musicFormElementAdded: function (musicFormElement, openInspector = true) {
      // console.log("TCL: _musicFormElementAdded: function(musicFormElement): ", musicFormElement);
      // TODO refactor for persistent music form data display independent from analysislist
      if (!TIMAAT.VideoPlayer.curMusic.musicFormElementsUI) {
        TIMAAT.VideoPlayer.curMusic.musicFormElementsUI = [];
      }
      TIMAAT.VideoPlayer.curMusic.musicFormElementsUI.push(musicFormElement);
      TIMAAT.VideoPlayer.sort(TIMAAT.VideoPlayer.curMusic.musicFormElementsUI);
      TIMAAT.VideoPlayer.jumpTo(musicFormElement.model.startTime);
      TIMAAT.VideoPlayer.selectedElementType = 'musicFormElement';
      musicFormElement.addUI();
      TIMAAT.VideoPlayer.curMusicFormElement = musicFormElement;

      if (openInspector) {
        TIMAAT.VideoPlayer.inspector.setItem(musicFormElement, 'musicFormElement');
        TIMAAT.VideoPlayer.inspector.open('inspectorMetadata');
      }

      // update UI
      $('#timelinePaneMusicFormElement').show();
      TIMAAT.VideoPlayer.updateListUI();
      TIMAAT.VideoPlayer.sortListUI();
    },

    _musicChangeInTempoElementAdded: function (musicChangeInTempoElement, openInspector = true) {
      // console.log("TCL: _musicChangeInTempoElementAdded: function(musicChangeInTempoElement): ", musicChangeInTempoElement);
      // TODO refactor for persistent music form data display independent from analysislist
      if (!TIMAAT.VideoPlayer.curMusic.musicChangeInTempoElementsUI) {
        TIMAAT.VideoPlayer.curMusic.musicChangeInTempoElementsUI = [];
      }
      TIMAAT.VideoPlayer.curMusic.musicChangeInTempoElementsUI.push(musicChangeInTempoElement);
      TIMAAT.VideoPlayer.sort(TIMAAT.VideoPlayer.curMusic.musicChangeInTempoElementsUI);
      TIMAAT.VideoPlayer.jumpTo(musicChangeInTempoElement.model.startTime);
      TIMAAT.VideoPlayer.selectedElementType = 'musicChangeInTempoElement';
      musicChangeInTempoElement.addUI();
      TIMAAT.VideoPlayer.curMusicChangeInTempoElement = musicChangeInTempoElement;

      if (openInspector) {
        TIMAAT.VideoPlayer.inspector.setItem(musicChangeInTempoElement, 'musicChangeInTempoElement');
        TIMAAT.VideoPlayer.inspector.open('inspectorMetadata');
      }

      // update UI
      $('#timelinePaneMusicChangeInTempo').show();
      TIMAAT.VideoPlayer.updateListUI();
      TIMAAT.VideoPlayer.sortListUI();
    },

    _musicArticulationElementAdded: function (musicArticulationElement, openInspector = true) {
      // console.log("TCL: _musicArticulationElementAdded: function(musicArticulationElement): ", musicArticulationElement);
      // TODO refactor for persistent music form data display independent from analysislist
      if (!TIMAAT.VideoPlayer.curMusic.musicArticulationElementsUI) {
        TIMAAT.VideoPlayer.curMusic.musicArticulationElementsUI = [];
      }
      TIMAAT.VideoPlayer.curMusic.musicArticulationElementsUI.push(musicArticulationElement);
      TIMAAT.VideoPlayer.sort(TIMAAT.VideoPlayer.curMusic.musicArticulationElementsUI);
      TIMAAT.VideoPlayer.jumpTo(musicArticulationElement.model.startTime);
      TIMAAT.VideoPlayer.selectedElementType = 'musicArticulationElement';
      musicArticulationElement.addUI();
      TIMAAT.VideoPlayer.curMusicArticulationElement = musicArticulationElement;

      if (openInspector) {
        TIMAAT.VideoPlayer.inspector.setItem(musicArticulationElement, 'musicArticulationElement');
        TIMAAT.VideoPlayer.inspector.open('inspectorMetadata');
      }

      // update UI
      $('#timelinePaneMusicArticulation').show();
      TIMAAT.VideoPlayer.updateListUI();
      TIMAAT.VideoPlayer.sortListUI();
    },

    _musicDynamicsElementAdded: function (musicDynamicsElement, openInspector = true) {
      // console.log("TCL: _musicDynamicsElementAdded: function(musicDynamicsElement): ", musicDynamicsElement);
      // TODO refactor for persistent music form data display independent from analysislist
      if (!TIMAAT.VideoPlayer.curMusic.musicDynamicsElementsUI) {
        TIMAAT.VideoPlayer.curMusic.musicDynamicsElementsUI = [];
      }
      TIMAAT.VideoPlayer.curMusic.musicDynamicsElementsUI.push(musicDynamicsElement);
      TIMAAT.VideoPlayer.sort(TIMAAT.VideoPlayer.curMusic.musicDynamicsElementsUI);
      TIMAAT.VideoPlayer.jumpTo(musicDynamicsElement.model.startTime);
      TIMAAT.VideoPlayer.selectedElementType = 'musicDynamicsElement';
      musicDynamicsElement.addUI();
      TIMAAT.VideoPlayer.curMusicDynamicsElement = musicDynamicsElement;

      if (openInspector) {
        TIMAAT.VideoPlayer.inspector.setItem(musicDynamicsElement, 'musicDynamicsElement');
        TIMAAT.VideoPlayer.inspector.open('inspectorMetadata');
      }

      // update UI
      $('#timelinePaneMusicDynamics').show();
      TIMAAT.VideoPlayer.updateListUI();
      TIMAAT.VideoPlayer.sortListUI();
    },

    _musicTextSettingElementAdded: function (musicTextSettingElement, openInspector = true) {
      // console.log("TCL: _musicTextSettingElementAdded: function(musicTextSettingElement): ", musicTextSettingElement);
      // TODO refactor for persistent music form data display independent from analysislist
      if (!TIMAAT.VideoPlayer.curMusic.musicTextSettingElementsUI) {
        TIMAAT.VideoPlayer.curMusic.musicTextSettingElementsUI = [];
      }
      TIMAAT.VideoPlayer.curMusic.musicTextSettingElementsUI.push(musicTextSettingElement);
      TIMAAT.VideoPlayer.sort(TIMAAT.VideoPlayer.curMusic.musicTextSettingElementsUI);
      TIMAAT.VideoPlayer.jumpTo(musicTextSettingElement.model.startTime);
      TIMAAT.VideoPlayer.selectedElementType = 'musicTextSettingElement';
      musicTextSettingElement.addUI();
      TIMAAT.VideoPlayer.curMusicTextSettingElement = musicTextSettingElement;

      if (openInspector) {
        TIMAAT.VideoPlayer.inspector.setItem(musicTextSettingElement, 'musicTextSettingElement');
        TIMAAT.VideoPlayer.inspector.open('inspectorMetadata');
      }

      // update UI
      $('#timelinePaneMusicTextSetting').show();
      TIMAAT.VideoPlayer.updateListUI();
      TIMAAT.VideoPlayer.sortListUI();
    },

  }

}, window));
