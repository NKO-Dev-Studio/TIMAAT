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
    if(typeof window !== 'undefined' && window.TIMAAT){
        factory(window.TIMAAT);
    }

}(function (TIMAAT) {

	TIMAAT.UI = {
		component                     : null,
		selectedActorId               : null,
		selectedEventId               : null,
		selectedMediumCollectionId    : null,
		selectedMediumCollectionItemId: null,
		selectedMediumId              : null,
		selectedMusicId               : null,
		selectedCategoryId            : null,
		selectedCategorySetId         : null,
		selectedLanguageId            : null,
		selectedRoleGroupId           : null,
		selectedRoleId                : null,
		subNavTab                     : 'dataSheet',

		init: function() {
			// console.log("TCL: UI: init: function()");
			$('[data-toggle="popover"]').popover();

			this.templates = {
					notification: `<div class="toast mb-1" role="alert" aria-live="assertive" aria-atomic="true">
						<div class="toast-header">
							<span class="notification__action-color badge">
								<i class="notificationAction fas fa-fw"></i>
							</span>
							&nbsp;<strong class="notificationUser mr-auto">(unknown)</strong>
							<small class="notificationTime text-muted">jetzt</small>
							<button type="button" class="ml-2 mb-1 close" data-dismiss="toast" aria-label="Close"><span aria-hidden="true">&times;</span></button>
						</div>
						<div class="notificationMessage toast-body"></div>
					</div>`,
			}

			// init components
			TIMAAT.EntityUpdate.init()
			TIMAAT.Datasets.init();
			TIMAAT.Lists.init();
            TIMAAT.Categories.init();
			TIMAAT.UploadManager.init();
			TIMAAT.VideoPlayer.init();
			TIMAAT.Settings.init();
			TIMAAT.UserSettings.init();
			TIMAAT.URLHistory.init();

			$('#logInUserName').on('keyup', function (e) { if (e.keyCode == 13) jQuery('#loginSubmitButton').click(); });
			$('#logInPassword').on('keyup', function (e) { if (e.keyCode == 13) jQuery('#loginSubmitButton').click(); });
			$('#loginSubmitButton').on('click', TIMAAT.UI.processLogin);
			// console.log("TCL ~ TIMAAT.Service.state", TIMAAT.Service.state);
			// console.log("TCL ~ TIMAAT.Service.token", TIMAAT.Service.token);
			if ( TIMAAT.Service.state != 1 ) {
				$('#wrapper').addClass('body__wrapper--blur');
				$('#loginModal').modal('show');
			}

			// window.addEventListener('load', function (event) {
			// 	console.log("TCL ~ checkSession");
			// 	if (TIMAAT.Service.session == null) {
			// 		this.processLogin();
			// 	} else {
			//    $('#wrapper').removeClass('body__wrapper--blur');
			// 		$('#loginModal').modal('hide');
			// 		$('#userInfo').html(event.displayName);
			// 	}
			// });

			// init tag popover functionality
			$(document).on('click', function (e) {
				$('[data-toggle="popover"],[data-original-title]').each(function () {
					if (!$(this).is(e.target) && $(this).has(e.target).length === 0 && $('.popover').has(e.target).length === 0) {
						(($(this).popover('hide').data('bs.popover')||{}).inState||{}).click = false
					}
				});
			});

			// init user log popover functionality
			$('#userLogList').popover({
				placement: 'bottom',
				title: '<i class="fas fa-user"></i> editing log',
				trigger: 'click',
				html: true,
				content: '<div class="userLogDetails">Loading ...</div>',
				container: 'body',
				boundary: 'viewport',
			});

			$('#userLogList').on('inserted.bs.popover', function () {
				TIMAAT.Service.getUserLog(TIMAAT.Service.session.id, 12, function(log) {
					var html = '';
					if ( log.length == 0 ) html = "No data available";
				$(log).each(function(index, entry) {
					var icon = 'fas fa-info';
					switch (entry.userLogEventType.type) {
					case 'login':
						icon = 'fas fa-sign-in-alt';
						break;
					case 'userCreated':
					case 'mediumCreated':
					case 'analysisListCreated':
					case 'annotationCreated':
						icon = 'fas fa-plus-square';
						break;
					case 'mediumEdited':
					case 'analysisListEdited':
					case 'analysisListDeleted':
						icon = 'fas fa-edit';
						break;
					case 'mediumDeleted':
					case 'analysisListDeleted':
					case 'annotationDeleted':
						icon = 'fas fa-trash-alt';
						break;
					}

					html += '<b><i class="'+icon+'"></i> '+TIMAAT.Util.formatLogType(entry.userLogEventType.type)
								+'</b><br>'+TIMAAT.Util.formatDate(entry.dateTime)+'<br>';
				});
				$('.userLogDetails').html(html);

				$(log).each(function(index, entry) {TIMAAT.Service.idCache.set(entry.userAccount.id, entry.userAccount.displayName);});
				//	$('.userLogDetails').find('.userId').each(function(index,item) {TIMAAT.Util.resolveUserID(item,"ich")});
				});
			});

			$.datetimepicker.setDateFormatter('moment');
		},

		hidePopups: function() {
			// console.log("TCL: hidePopups: function()");
			$('[data-toggle="popover"],[data-original-title]').each(function () {
				(($(this).popover('hide').data('bs.popover')||{}).inState||{}).click = false
			});
		},

		showComponent: function(component) {
			// console.log("TCL: showComponent: function(component)");
			// console.log("TCL: component", component);
			TIMAAT.UI.component = component;
			TIMAAT.VideoPlayer.pause(); // TODO refactor
			$('#mediumVideoPreview').get(0).pause();
			$('.component').hide();
			$('.js-sidebar__li').removeClass('bg-info');
			$('.js-sidebar__a').removeClass('selected');
			switch (component) {
				case 'videoPlayer':
					$('#componentVideoPlayer').show();
				break;
				case 'lists':
					$('#componentLists').show();
				break;
                case 'categories':
                    $('#componentCategories').show();
                break;
				case 'media':
					$('#componentMedia').show();
				break;
				case 'actors':
					$('#componentActors').show();
				break;
				case 'locations':
					$('#componentLocations').show();
				break;
				case 'events':
					$('#componentEvents').show();
				break;
				case 'music':
					$('#componentMusic').show();
				break;
				case 'settings':
					$('#componentSettings').show();
				break;
			}
			if (component == 'media') {
				$('#mediumPreviewDataTab').removeClass('annotationMode');
				$('#mediumDatasetsMediumTabsContainer').append($('#mediumDatasetsMediumTabs'));
				$('#mediumModalsContainer').append($('#mediumModals'));
				TIMAAT.MediumDatasets.container = 'media';
			}
			$('.sidebarTab'+component).addClass('bg-info');
			$('.sidebarTab'+component+' a').addClass('selected');
		},

		receiveNotification(notificationEvent) {
			try {
				// console.log("notification event", notificationEvent);
				let notification = JSON.parse(notificationEvent.data);
				// console.log("notification data", notification);
				// TODO refactor
				// only show if notification is for current list
				if ( TIMAAT.VideoPlayer.curAnalysisList && TIMAAT.VideoPlayer.curAnalysisList.id == notification.dataID ) {
					TIMAAT.UI.showNotification( notification );
					// trigger local event and action
					// console.log("trigger ",notification.message+'.notification.TIMAAT');
					$(document).trigger(notification.message+'.notification.TIMAAT', notification);
				}
			} catch(error) {
				console.error("ERROR: ", error);
			}
		},

		sendNotification(type, dataID=0, data=null) {
			if ( !this.notificationSocket || this.notificationSocket.readyState == WebSocket.CLOSED ) {
				// init websocket
				let protocol = ( location.protocol == 'http:') ? 'ws://' : 'wss://';
				this.notificationSocket = new WebSocket(protocol+location.host+location.pathname+'api/notification');
				let request = type;
				let storedID = dataID;
				this.notificationSocket.onopen = function() {
					// send notification request to server
					TIMAAT.UI.notificationSocket.send( JSON.stringify({token:TIMAAT.Service.token,request:request,dataID:storedID}) );
				}
				this.notificationSocket.onmessage = TIMAAT.UI.receiveNotification;
				// send notification request to server
			} else {
				this.notificationSocket.onopen = function() {
					TIMAAT.UI.notificationSocket.send(JSON.stringify({token:TIMAAT.Service.token,request:type,dataID:dataID}));
				}
			}
		},

		showNotification(notification) {
			if (!notification) return;
			let ui = $(this.templates.notification);
			let color = 'badge-info';
			let action = 'fa-question';
			let user = notification.username;
			let message = 'hat eine unbekannte Aktion durchgeführt';

			// parse and style notification
			switch (notification.message) {
				case 'subscribe-list':
					color = 'badge-primary';
					action = 'fa-eye';
					message = 'current analysis opened'
					break;
				case 'unsubscribe-list':
					color = 'badge-secondary';
					action = 'fa-eye';
					message = 'current analysis closed'
					break;
				case 'addSegment':
					color = 'badge-success';
					action = 'fa-plus';
					message = 'Segment <strong>"'+notification.data.analysisSegmentTranslations[0].title+'"</strong> added';
					break;
				case 'editSegment':
					color = 'badge-warning';
					action = 'fa-edit';
					message = 'Segment <strong>"'+notification.data.analysisSegmentTranslations[0].title+'"</strong> edited';
					break;
				case 'removeSegment':
					color = 'badge-danger';
					action = 'fa-trash-alt';
					message = 'Segment <strong>"'+notification.data.analysisSegmentTranslations[0].title+'"</strong> deleted';
					break;
				case 'addAnnotation':
					color = 'badge-success';
					action = 'fa-plus';
					message = 'Annotation <strong>"'+notification.data.title+'"</strong> added';
					break;
				case 'editAnnotation':
					color = 'badge-warning';
					action = 'fa-edit';
					message = 'Annotation <strong>"'+notification.data.title+'"</strong> edited';
					break;
				case 'removeAnnotation':
					color = 'badge-danger';
					action = 'fa-trash-alt';
					message = 'Annotation <strong>"'+notification.data.title+'"</strong> deleted';
					break;
				case 'list-subscribers':
					color = 'badge-primary';
					action = 'fa-user-friends';
					user = 'Editors';
					message = '';
					for (let subscriber of notification.data) message += ', '+subscriber;
					message = '<strong>'+message.substring(2)+'</strong> ';
					message += (notification.data.length == 1) ? 'edited ' : 'edits ';
					message += 'the analysis, too';
					break;
				default:
					message = 'unknown action: '+notification.message;
					break;
			}
			ui.find('.notification__action-color').addClass(color);
			ui.find('.notificationAction').addClass(action);
			ui.find('.notificationUser').text(user);
			ui.find('.notificationMessage').html(message);
			ui.find('.notificationTime').text(TIMAAT.Util.getFuzzyDate(notification.timestamp));

			$('#notificationPane').append(ui);
			// display notification
			ui.toast({delay:4000})
			.on('hidden.bs.toast',function(){$(this).toast('dispose').remove();})
			.toast('show');
		},

		setWaiting: function(waiting) {
			// console.log("TCL: setWaiting: function(waiting)");
			// console.log("TCL: waiting", waiting);
			if (waiting) $('#UiWaitingModal').modal('show');
			else $('#UiWaitingModal').modal('hide');
		},

		setLoginEnabled: function(enabled) {
			$('#logInUserName').prop('disabled', !enabled);
			$('#logInPassword').prop('disabled', !enabled);
			$('#loginSubmitButton').prop('disabled', !enabled);
			if ( enabled ) $('#loginSubmitButton i.login-spinner').addClass('d-none');
			else $('#loginSubmitButton i.login-spinner').removeClass('d-none');
		},

		processLogin: function() {
			// console.log("TCL: processLogin: function()");
			var user = jQuery('#logInUserName').val();
			var pass = jQuery('#logInPassword').val();
			if ( user.length > 0 && pass.length > 0 ) {
				TIMAAT.UI.setLoginEnabled(false);

				setTimeout(function() {
					var hash = TIMAAT.Util.getArgonHash(pass, user + TIMAAT.Service.clientSalt); // TODO refactor
					var credentials = {
						username: user,
						password: hash
					}

					$.ajax({
						// url:window.location.protocol+'//'+window.location.host+"/TIMAAT/api/authenticate",
						url        : "api/authenticate",
						type       : "POST",
						data       : JSON.stringify(credentials),
						contentType: "application/json; charset=utf-8",
						dataType   : "json",
					}).done(async function(e) {
						TIMAAT.UI.setLoginEnabled(true);
						TIMAAT.Service.session = e;
            // console.log("TCL ~ setTimeout ~ TIMAAT.Service.session", TIMAAT.Service.session);
						TIMAAT.Service.token = e.token;
            // console.log("TCL ~ setTimeout ~ TIMAAT.Service.token", TIMAAT.Service.token);
						$('#wrapper').removeClass('body__wrapper--blur');
						$('#loginModal').modal('hide');
						// $('#userInfo').html(e.accountName);
						$('#userInfo').html(e.displayName);

						TIMAAT.EntityUpdate.initEntityUpdate()
						TIMAAT.MediumDatasets.loadRequiredRoleIds();
						TIMAAT.Datasets.load();
						TIMAAT.Datasets.loadDataTables();
						TIMAAT.Lists.load();
						TIMAAT.Lists.loadDataTables();

						// define landing page
						if (window.location.hash) {
							await TIMAAT.URLHistory.setupView(window.location.hash);
						} else {
							TIMAAT.MediumDatasets.initMediaComponent()
						}

						// restrict access to settings tab to admin user only
						if (TIMAAT.Service.session.displayName == "admin") {
							$('.adminAccessOnly').show();
						} else {
							$('.adminAccessOnly').hide();
						}
					}).fail(function(error) {
						TIMAAT.UI.setLoginEnabled(true);
						console.error("process login ERROR: ", error);
						jQuery('#loginMessage').fadeIn();
					});
				}, 50);
			}
		},

    // display all areas of the media component
		displayComponent: function(type, navBarLinkId, dataTableCardClass, navTabLinkId=null, contentId=null, subtype=null) {
	    // console.log("TCL: displayComponent - type, navBarLinkId, dataTableCardClass, navTabLinkId, contentId: , subtype: ", type, navBarLinkId, dataTableCardClass, navTabLinkId, contentId, subtype);
			this.displayNavBarContainer(type);
			this.setNavBarActiveLink(navBarLinkId);
			this.setAndDisplayDataTableActiveActiveCard(dataTableCardClass);
			this.displayDataSetContentContainer(navTabLinkId, contentId, type, subtype);
		},

		// display top nav bar row
		displayNavBarContainer: function(type) {
			switch(type) {
				case 'category':
				case 'categorySet':
				case 'role':
				case 'roleGroup':
				case 'language':
					$('#listTabs').show();
				break;
				case 'actor':
					$('#actorTabs').show();
				break;
				case 'event':
					$('#eventTabs').show();
				break;
				case 'medium':
				case 'mediumCollection':
					$('#mediumTabs').show();
				break;
				case 'music':
					$('#musicTabs').show();
				break;
				case 'settings':
					$('#settingsTabs').show();
				break;
			}
		},

		setNavBarActiveLink: function(navBarLinkId) {
			$('.navBarLinkTop').removeClass('active');
			if (navBarLinkId) {
				$('#'+navBarLinkId).addClass('active');
			}
		},

		// display left column (data table list) and show corresponding data
		setAndDisplayDataTableActiveActiveCard: function(dataTableCardId) {
			$('.dataTables').hide();
			if (dataTableCardId) {
				$('#'+dataTableCardId).show();
			}
		},

		// display right column (dataset content)
		displayDataSetContentContainer: function(navTabLinkId=null, contentId=null, type=null, subtype=null) {
    	// console.log("TCL ~ navTabLinkId, contentId, type, subtype: ", navTabLinkId, contentId, type, subtype);
			$('.nav-tabs').hide();
      // $('.data-tabs').hide();
			if (navTabLinkId) {
				this.displayAndSetDataSetContentNavTab(navTabLinkId, type, subtype);
				if (subtype && (subtype == 'document' || subtype == 'software' || subtype == 'text' || subtype == 'videogame')) {
					contentId = 'mediumFormMetadata'; // when in preview form and switching to medium without preview
					this.subNavTab = 'dataSheet';
				}
			}
			this.displayDataSetContentArea(contentId);
		},

		hideDataSetContentContainer: function() {
    	// console.log("TCL: hideDataSetContentContainer");
			this.displayDataSetContentContainer(null, null, null);
		},

		// display dataset content tab bar row
		displayAndSetDataSetContentNavTab: function(navTabLinkId, type=null, subtype=null) {
    	// console.log("TCL: navTabLinkId, type=null: ", navTabLinkId, type);
      switch(type) {
				case 'category':
				case 'categorySet':
				case 'role':
				case 'roleGroup':
				case 'language':
					$('#listsNavTabs').show();
					// $('.listDataTabs').show(); // TODO check if data-tabs is necessary
				break;
				case 'actor':
					$('#actorNavigationTabs').show();
				break;
				case 'event':
					$('#eventNavTabs').show();
				break;
				case 'medium':
					$('#mediumNavTabs').show();
					switch(subtype) {
						case 'document':
						case 'software':
						case 'text':
						case 'videogame':
							$('#mediumPreviewDataTab').hide();
						break;
						case 'audio':
						case 'image':
						case 'video':
							$('#mediumPreviewDataTab').show();
						break;
					}
					if ($('#mediumPreviewDataTab').hasClass('annotationMode')) {
						$('#mediumPreviewDataTab').hide();
					}

				break;
				case 'mediumCollection':
					$('#mediumCollectionNavTabs').show();
				break;
				case 'music':
					$('#musicNavTabs').show();
				break;
			}
			this.setDataSetContentActiveNavTab(navTabLinkId);
		},

		setDataSetContentActiveNavTab: function(navTabLinkId) {
			$('.navBarLinkContent').removeClass('active');
			if (navTabLinkId) {
				$('#'+navTabLinkId).addClass('active');
			}
		},

		// display dataset content data
		displayDataSetContentArea: function(contentHash) {
			$('.datasetContent').hide();
			if (contentHash) {
				$('#'+contentHash).show();
			}
		},

		displayDataSetContent: function(form, data, type, mode = 'show') {
    	// console.log("TCL: displayDataSetContent: form, data, type, mode - ", form, data, type, mode);
			this.subNavTab = form;
      switch(type) {
        case 'category':
          switch(form) {
            case 'dataSheet':
              this.setDataSetContentActiveNavTab('listsTabMetadata');
              TIMAAT.CategoryLists.categoryFormDataSheet(mode, data);
            break;
          }
        break;
        case 'categorySet':
          switch(form) {
            case 'dataSheet':
              this.setDataSetContentActiveNavTab('listsTabMetadata');
              TIMAAT.CategoryLists.categorySetFormDataSheet(mode, data);
            break;
          }
        break;
        case 'role':
          switch(form) {
            case 'dataSheet':
              this.setDataSetContentActiveNavTab('listsTabMetadata');
              TIMAAT.RoleLists.roleFormDataSheet(mode, data);
            break;
          }
        break;
        case 'roleGroup':
          switch(form) {
            case 'dataSheet':
              this.setDataSetContentActiveNavTab('listsTabMetadata');
              TIMAAT.RoleLists.roleGroupFormDataSheet(mode, data);
            break;
          }
        break;
        case 'language':
          switch(form) {
            case 'dataSheet':
              this.setDataSetContentActiveNavTab('listsTabMetadata');
              TIMAAT.LanguageLists.languageFormDataSheet(mode, data);
            break;
          }
        break;
        case 'medium':
					var subtype = data.model.mediaType.mediaTypeTranslations[0].type;
					TIMAAT.UI.addSelectedClassToSelectedItem(subtype, data.model.id);
					switch(form) {
						case 'preview':
							this.setDataSetContentActiveNavTab('mediumPreviewTab');
							TIMAAT.MediumDatasets.mediumFormPreview(subtype, data);
						break;
						case 'dataSheet':
							this.setDataSetContentActiveNavTab('mediumMetadataTab');
							TIMAAT.MediumDatasets.mediumFormDataSheet(mode, subtype, data);
						break;
						case 'titles':
							this.setDataSetContentActiveNavTab('mediumTitlesTab');
							TIMAAT.MediumDatasets.mediumFormTitles(mode, data);
						break;
						case 'languageTracks':
							this.setDataSetContentActiveNavTab('mediumLanguageTracksTab');
							TIMAAT.MediumDatasets.mediumFormLanguageTracks(mode, data);
						break;
						case 'actorWithRoles':
							this.setDataSetContentActiveNavTab('mediumActorWithRolesTab');
							TIMAAT.MediumDatasets.mediumFormActorRoles(mode, data);
                        break;
                        case 'music':
                            this.setDataSetContentActiveNavTab('mediumMusicTab');
                            TIMAAT.MediumDatasets.mediumFormMusic(mode, data)
						break;
						case 'transcriptions':
							this.setDataSetContentActiveNavTab('mediumTranscriptionTab');
							TIMAAT.MediumDatasets.mediumFormTranscriptions(mode, data);
						break;
					}
        break;
        case 'mediumCollection':
					var subtype = type = data.model.mediaCollectionType.mediaCollectionTypeTranslations[0].type;
					TIMAAT.UI.addSelectedClassToSelectedItem(subtype, data.model.id);
          switch(form) {
            case 'dataSheet':
              this.setDataSetContentActiveNavTab('mediumCollectionMetadataTab');
              TIMAAT.MediumCollectionDatasets.mediumCollectionFormDataSheet(mode, subtype, data);
            break;
            case 'items':
              this.setDataSetContentActiveNavTab('mediumCollectionItemsTab');
              TIMAAT.MediumCollectionDatasets.mediumCollectionFormItems(mode, data);
            break;
            case 'publication':
              this.setDataSetContentActiveNavTab('mediumCollectionPublicationTab');
              TIMAAT.MediumCollectionDatasets.mediumCollectionFormPublication(data);
            break;
						case 'allMedia':
							// this.setDataSetContentActiveNavTab('');
							// TIMAAT.MediumCollectionDatasets.mediumCollectionFormAllMedia();
						break;
          }
        break;
				case 'music':
					var subtype = data.model.musicType.musicTypeTranslations[0].type;
					switch(form) {
						case 'preview':
							this.setDataSetContentActiveNavTab('musicTabPreview');
							TIMAAT.MusicDatasets.musicFormPreview(subtype, data);
						break;
						case 'dataSheet':
							this.setDataSetContentActiveNavTab('musicTabMetadata');
							TIMAAT.MusicDatasets.musicFormDataSheet(mode, subtype, data);
						break;
						case 'titles':
							this.setDataSetContentActiveNavTab('musicTabTitles');
							TIMAAT.MusicDatasets.musicFormTitles(mode, data);
						break;
						case 'actorWithRoles':
							this.setDataSetContentActiveNavTab('musicTabActorWithRoles');
							TIMAAT.MusicDatasets.musicFormActorRoles(mode, data);
						break;
						case 'mediumHasMusicList':
							this.setDataSetContentActiveNavTab('musicTabMediumHasMusicList');
							TIMAAT.MusicDatasets.musicFormMediumHasMusicList(mode, data);
						break;
						case 'transcriptions':
							this.setDataSetContentActiveNavTab('musicTabTranscription');
							TIMAAT.MusicDatasets.musicFormTranscriptions(mode, data);
					}
				break;
				case 'event':
					switch(form) {
						case 'dataSheet':
							this.setDataSetContentActiveNavTab('eventTabMetadata');
							TIMAAT.EventDatasets.eventFormDataSheet(mode, data);
						break;
					}
				break;
				case 'actor':
					var subtype = data.model.actorType.actorTypeTranslations[0].type;
					TIMAAT.UI.addSelectedClassToSelectedItem(subtype, data.model.id);
					switch(form) {
						case 'dataSheet':
							this.setDataSetContentActiveNavTab('actorTabMetadata');
							TIMAAT.ActorDatasets.actorFormDataSheet(mode, subtype, data);
						break;
						case 'names':
							this.setDataSetContentActiveNavTab('actorTabNames');
							TIMAAT.ActorDatasets.actorFormNames(mode, data);
						break;
						case 'addresses':
							this.setDataSetContentActiveNavTab('actorTabAddresses');
							TIMAAT.ActorDatasets.actorFormAddresses(mode, data);
						break;
						case 'emails':
							this.setDataSetContentActiveNavTab('actorTabEmailAddresses');
							TIMAAT.ActorDatasets.actorFormEmailAddresses(mode, data);
						break;
						case 'phoneNumbers':
							this.setDataSetContentActiveNavTab('actorTabPhoneNumbers');
							TIMAAT.ActorDatasets.actorFormPhoneNumbers(mode, data);
						break;
						case 'memberOfCollectives':
							this.setDataSetContentActiveNavTab('actorTabMemberOfCollectives');
							TIMAAT.ActorDatasets.actorFormMemberOfCollectives(mode, subtype, data);
						break;
						case 'actorRelationships':
							this.setDataSetContentActiveNavTab('actorTabActorRelationships');
							TIMAAT.ActorDatasets.actorFormActorRelationships(mode, data);
						break;
						case 'locatedInCountry':
							this.setDataSetContentActiveNavTab('actorTabLocatedInCountry');
							TIMAAT.ActorDatasets.actorFormLocatedInCountry(mode, data);
						break;
						case 'roles':
							this.setDataSetContentActiveNavTab('actorTabRoles');
							TIMAAT.ActorDatasets.actorFormRoles(mode, data);
						break;
						case 'rolesInMedia':
							this.setDataSetContentActiveNavTab('actorTabRoleInMedium');
							TIMAAT.ActorDatasets.actorFormRoleMedium(mode, data);
						break;
					}
				break;
      }
		},

    addSelectedClassToSelectedItem: function(type, id) {
			// console.log("TCL: addSelectedClassToSelectedItem: type, id: ", type, id);
			var table;
			switch(type) {
				case 'category':
					table = TIMAAT.CategoryLists.dataTableCategories;
          // remove selection from old row
          if (this.selectedCategoryId && this.selectedCategoryId != id) {
            $(table.row('#'+this.selectedCategoryId).node()).removeClass('selected');
          }
          // add selection to new row
          if (id) {
            $(table.row('#'+id).node()).addClass('selected');
						this.selectedCategoryId = id;
          }
				break;
				case 'categorySet':
					table = TIMAAT.CategoryLists.dataTableCategorySets;
          // remove selection from old row
          if (this.selectedCategorySetId && this.selectedCategorySetId != id) {
            $(table.row('#'+this.selectedCategorySetId).node()).removeClass('selected');
          }
          // add selection to new row
          if (id) {
            $(table.row('#'+id).node()).addClass('selected');
						this.selectedCategorySetId = id;
          }
				break;
        case 'role':
					table = TIMAAT.RoleLists.dataTableRoles;
          // remove selection from old row
          if (this.selectedRoleId && this.selectedRoleId != id) {
            $(table.row('#'+this.selectedRoleId).node()).removeClass('selected');
          }
          // add selection to new row
          if (id) {
            $(table.row('#'+id).node()).addClass('selected');
						this.selectedRoleId = id;
          }
				break;
				case 'roleGroup':
					table = TIMAAT.RoleLists.dataTableRoleGroups;
          // remove selection from old row
          if (this.selectedRoleGroupId && this.selectedRoleGroupId != id) {
            $(table.row('#'+this.selectedRoleGroupId).node()).removeClass('selected');
          }
          // add selection to new row
          if (id) {
            $(table.row('#'+id).node()).addClass('selected');
						this.selectedRoleGroupId = id;
          }
				break;
        case 'language':
					table = TIMAAT.LanguageLists.dataTableLanguages;
          // remove selection from old row
          if (this.selectedLanguageId && this.selectedLanguageId != id) {
            $(table.row('#'+this.selectedLanguageId).node()).removeClass('selected');
          }
          // add selection to new row
          if (id) {
            $(table.row('#'+id).node()).addClass('selected');
						this.selectedLanguageId = id;
          }
				break;
				case 'actor':
					table = TIMAAT.ActorDatasets.dataTableActor;
					if (this.selectedActorId && this.selectedActorId != id) {
						$(table.row('#'+this.selectedActorId).node()).removeClass('selected');
					}
					// add selection to new row
					if (id) {
						$(table.row('#'+id).node()).addClass('selected');
						this.selectedActorId = id;
					}
				break;
				case 'person':
					table = TIMAAT.ActorDatasets.dataTablePerson;
					if (this.selectedActorId && this.selectedActorId != id) {
						$(table.row('#'+this.selectedActorId).node()).removeClass('selected');
					}
					// add selection to new row
					if (id) {
						$(table.row('#'+id).node()).addClass('selected');
						this.addSelectedClassToSelectedItem('actor', id);
					}
				break;
				case 'collective':
					table = TIMAAT.ActorDatasets.dataTableCollective;
					if (this.selectedActorId && this.selectedActorId != id) {
						$(table.row('#'+this.selectedActorId).node()).removeClass('selected');
					}
					// add selection to new row
					if (id) {
						$(table.row('#'+id).node()).addClass('selected');
						this.addSelectedClassToSelectedItem('actor', id);
					}
				break;
				case 'event':
					if (this.selectedEventId && this.selectedEventId != id) {
						$(TIMAAT.EventDatasets.dataTableEvent.row('#'+this.selectedEventId).node()).removeClass('selected');
					}
					// add selection to new row
					if (id) {
						$(TIMAAT.EventDatasets.dataTableEvent.row('#'+id).node()).addClass('selected');
						this.selectedEventId = id;
					}
				break;
				case 'music':
					if (this.selectedMusicId && this.selectedMusicId != id) {
						$(TIMAAT.MusicDatasets.dataTableMusic.row('#'+this.selectedMusicId).node()).removeClass('selected');
					}
					// add selection to new row
					if (id) {
						$(TIMAAT.MusicDatasets.dataTableMusic.row('#'+id).node()).addClass('selected');
						this.selectedMusicId = id;
					}
				break;
				case 'nashid':
					if (this.selectedMusicId && this.selectedMusicId != id) {
						$(TIMAAT.MusicDatasets.dataTableNashid.row('#'+this.selectedMusicId).node()).removeClass('selected');
					}
					// add selection to new row
					if (id) {
						$(TIMAAT.MusicDatasets.dataTableNashid.row('#'+id).node()).addClass('selected');
						this.addSelectedClassToSelectedItem('music', id);
					}
				break;
				case 'churchMusic':
					if (this.selectedMusicId && this.selectedMusicId != id) {
						$(TIMAAT.MusicDatasets.dataTableChurchMusic.row('#'+this.selectedMusicId).node()).removeClass('selected');
					}
					// add selection to new row
					if (id) {
						$(TIMAAT.MusicDatasets.dataTableChurchMusic.row('#'+id).node()).addClass('selected');
						this.addSelectedClassToSelectedItem('music', id);
					}
				break;
				case 'medium':
					if (this.selectedMediumId && this.selectedMediumId != id) {
						$(TIMAAT.MediumDatasets.dataTableMedia.row('#'+this.selectedMediumId).node()).removeClass('selected');
					}
					if (id) {
						$(TIMAAT.MediumDatasets.dataTableMedia.row('#'+id).node()).addClass('selected');
						this.selectedMediumId = id;
					}
					break;
				case 'audio':
					if (this.selectedMediumId && this.selectedMediumId != id) {
						$(TIMAAT.MediumDatasets.dataTableAudio.row('#'+this.selectedMediumId).node()).removeClass('selected');
					}
					// add selection to new row
					if (id) {
						$(TIMAAT.MediumDatasets.dataTableAudio.row('#'+id).node()).addClass('selected');
						this.addSelectedClassToSelectedItem('medium', id);
					}
				break;
				case 'document':
					if (this.selectedMediumId && this.selectedMediumId != id) {
						$(TIMAAT.MediumDatasets.dataTableDocument.row('#'+this.selectedMediumId).node()).removeClass('selected');
					}
					// add selection to new row
					if (id) {
						$(TIMAAT.MediumDatasets.dataTableDocument.row('#'+id).node()).addClass('selected');
						this.addSelectedClassToSelectedItem('medium', id);
					}
				break;
				case 'image':
					if (this.selectedMediumId && this.selectedMediumId != id) {
						$(TIMAAT.MediumDatasets.dataTableImage.row('#'+this.selectedMediumId).node()).removeClass('selected');
					}
					// add selection to new row
					if (id) {
						$(TIMAAT.MediumDatasets.dataTableImage.row('#'+id).node()).addClass('selected');
						this.addSelectedClassToSelectedItem('medium', id);
					}
				break;
				case 'software':
					if (this.selectedMediumId && this.selectedMediumId != id) {
						$(TIMAAT.MediumDatasets.dataTableSoftware.row('#'+this.selectedMediumId).node()).removeClass('selected');
					}
					// add selection to new row
					if (id) {
						$(TIMAAT.MediumDatasets.dataTableSoftware.row('#'+id).node()).addClass('selected');
						this.addSelectedClassToSelectedItem('medium', id);
					}
				break;
				case 'text':
					if (this.selectedMediumId && this.selectedMediumId != id) {
						$(TIMAAT.MediumDatasets.dataTableText.row('#'+this.selectedMediumId).node()).removeClass('selected');
					}
					// add selection to new row
					if (id) {
						$(TIMAAT.MediumDatasets.dataTableText.row('#'+id).node()).addClass('selected');
						this.addSelectedClassToSelectedItem('medium', id);
					}
				break;
				case 'video':
					if (this.selectedMediumId && this.selectedMediumId != id) {
						$(TIMAAT.MediumDatasets.dataTableVideo.row('#'+this.selectedMediumId).node()).removeClass('selected');
					}
					// add selection to new row
					if (id) {
						$(TIMAAT.MediumDatasets.dataTableVideo.row('#'+id).node()).addClass('selected');
						this.addSelectedClassToSelectedItem('medium', id);
					}
				break;
				case 'videogame':
					if (this.selectedMediumId && this.selectedMediumId != id) {
						$(TIMAAT.MediumDatasets.dataTableVideogame.row('#'+this.selectedMediumId).node()).removeClass('selected');
					}
					// add selection to new row
					if (id) {
						$(TIMAAT.MediumDatasets.dataTableVideogame.row('#'+id).node()).addClass('selected');
						this.addSelectedClassToSelectedItem('medium', id);
					}
				break;
				case 'mediumCollection':
					if (this.selectedMediumCollectionId && this.selectedMediumCollectionId != id) {
						$(TIMAAT.MediumCollectionDatasets.dataTableMediaCollectionList.row('#'+this.selectedMediumCollectionId).node()).removeClass('selected');
					}
					// add selection to new row
					if (id) {
						$(TIMAAT.MediumCollectionDatasets.dataTableMediaCollectionList.row('#'+id).node()).addClass('selected');
						this.selectedMediumCollectionId = id;
					}
				break;
				case 'mediumCollectionItem':
					// if (this.selectedMediumCollectionItemId && this.selectedMediumCollectionItemId != id) {
					// 	$(TIMAAT.MediumCollectionDatasets.dataTableMediaCollectionItemList.row('#'+this.selectedMediumCollectionItemId).node()).removeClass('selected');
					// }
					// add selection to new row
					if (id) {
						// $(TIMAAT.MediumCollectionDatasets.dataTableMediaCollectionList.row('#'+id).node()).addClass('selected');
						this.selectedMediumCollectionItemId = id;
					}
				break;
			}
		},

    clearLastSelection: function (type) {
      // console.log("TCL: clearLastSelection - type: ", type);
			let i = 0;
			switch (type) {
				case 'category':
					if (!TIMAAT.CategoryLists.categories) return;
					for (; i < TIMAAT.CategoryLists.categories.length; i++) {
						$(TIMAAT.CategoryLists.dataTableCategories.row('#'+TIMAAT.CategoryLists.categories[i].model.id).node()).removeClass('selected');
					}
          $(TIMAAT.CategoryLists.dataTableCategories.row('#'+this.selectedCategoryId).node()).removeClass('selected');
          this.selectedCategoryId = null;
				break;
				case 'categorySet':
					if (!TIMAAT.CategoryLists.categorySets) return;
					for (; i < TIMAAT.CategoryLists.categorySets.length; i++) {
						$(TIMAAT.CategoryLists.dataTableCategorySets.row('#'+TIMAAT.CategoryLists.categorySets[i].model.id).node()).removeClass('selected');
					}
          $(TIMAAT.CategoryLists.dataTableCategorySets.row('#'+this.selectedCategorySetId).node()).removeClass('selected');
          this.selectedCategorySetId = null;
        break;
        case 'role':
					if (!TIMAAT.RoleLists.roles) return;
					for (; i < TIMAAT.RoleLists.roles.length; i++) {
						$(TIMAAT.RoleLists.dataTableRoles.row('#'+TIMAAT.RoleLists.roles[i].model.id).node()).removeClass('selected');
					}
          $(TIMAAT.RoleLists.dataTableRoles.row('#'+this.selectedRoleId).node()).removeClass('selected');
          this.selectedRoleId = null;
				break;
				case 'roleGroup':
					if (!TIMAAT.RoleLists.roleGroups) return;
					for (; i < TIMAAT.RoleLists.roleGroups.length; i++) {
						$(TIMAAT.RoleLists.dataTableRoleGroups.row('#'+TIMAAT.RoleLists.roleGroups[i].model.id).node()).removeClass('selected');
					}
          $(TIMAAT.RoleLists.dataTableRoleGroups.row('#'+this.selectedRoleGroupId).node()).removeClass('selected');
          this.selectedRoleGroupId = null;
        break;
        case 'language':
					if (!TIMAAT.LanguageLists.languages) return;
					for (; i < TIMAAT.LanguageLists.languages.length; i++) {
						$(TIMAAT.LanguageLists.dataTableLanguages.row('#'+TIMAAT.LanguageLists.languages[i].model.id).node()).removeClass('selected');
					}
          $(TIMAAT.LanguageLists.dataTableLanguages.row('#'+this.selectedLanguageId).node()).removeClass('selected');
          this.selectedLanguageId = null;
				break;
				case 'actor':
					if (!TIMAAT.ActorDatasets.actors) return;
					for (; i < TIMAAT.ActorDatasets.actors.length; i++) {
						$(TIMAAT.ActorDatasets.dataTableActor.row('#'+TIMAAT.ActorDatasets.actors[i].model.id).node()).removeClass('selected');
					}
					$(TIMAAT.ActorDatasets.dataTableActor.row('#'+this.selectedActorId).node()).removeClass('selected');
					this.selectedActorId = null;
				break;
				case 'person':
					if (!TIMAAT.ActorDatasets.persons) return;
					for (; i < TIMAAT.ActorDatasets.persons.length; i++) {
						$(TIMAAT.ActorDatasets.dataTablePerson.row('#'+TIMAAT.ActorDatasets.persons[i].model.id).node()).removeClass('selected');
					}
					$(TIMAAT.ActorDatasets.dataTablePerson.row('#'+this.selectedActorId).node()).removeClass('selected');
					// this.clearLastSelection('actor');
					this.selectedActorId = null;
				break;
				case 'collective':
					if (!TIMAAT.ActorDatasets.collectives) return;
					for (; i < TIMAAT.ActorDatasets.collectives.length; i++) {
						$(TIMAAT.ActorDatasets.dataTableCollective.row('#'+TIMAAT.ActorDatasets.collectives[i].model.id).node()).removeClass('selected');
					}
					$(TIMAAT.ActorDatasets.dataTableCollective.row('#'+this.selectedActorId).node()).removeClass('selected');
					// this.clearLastSelection('actor');
					this.selectedActorId = null;
				break;
				case 'event':
					if (!TIMAAT.EventDatasets.events) return;
					for (; i < TIMAAT.EventDatasets.events.length; i++) {
						$(TIMAAT.EventDatasets.dataTableEvent.row('#'+TIMAAT.EventDatasets.events[i].model.id).node()).removeClass('selected');
					}
					$(TIMAAT.EventDatasets.dataTableEvent.row('#'+this.selectedEventId).node()).removeClass('selected');
					this.selectedEventId = null;
				break;
				case 'music':
					if (!TIMAAT.MusicDatasets.musicList) return;
					for (; i < TIMAAT.MusicDatasets.musicList.length; i++) {
						$(TIMAAT.MusicDatasets.dataTableMusic.row('#'+TIMAAT.MusicDatasets.musicList[i].model.id).node()).removeClass('selected');
					}
					$(TIMAAT.MusicDatasets.dataTableMusic.row('#'+this.selectedMusicId).node()).removeClass('selected');
					this.selectedMusicId = null;
				break;
				case 'nashid':
					if (!TIMAAT.MusicDatasets.nashidList) return;
					for (; i < TIMAAT.MusicDatasets.nashidList.length; i++) {
						$(TIMAAT.MusicDatasets.dataTableNashid.row('#'+TIMAAT.MusicDatasets.nashidList[i].model.id).node()).removeClass('selected');
					}
					$(TIMAAT.MusicDatasets.dataTableNashid.row('#'+this.selectedMusicId).node()).removeClass('selected');
					// this.clearLastSelection('music');
					this.selectedMusicId = null;
				break;
				case 'churchMusic':
					if (!TIMAAT.MusicDatasets.churchMusicList) return;
					for (; i < TIMAAT.MusicDatasets.churchMusicList.length; i++) {
						$(TIMAAT.MusicDatasets.dataTableChurchMusic.row('#'+TIMAAT.MusicDatasets.churchMusicList[i].model.id).node()).removeClass('selected');
					}
					$(TIMAAT.MusicDatasets.dataTableChurchMusic.row('#'+this.selectedMusicId).node()).removeClass('selected');
					// this.clearLastSelection('music');
					this.selectedMusicId = null;
				break;
				case 'mediumCollection':
					if (!TIMAAT.MediumCollectionDatasets.mediaCollectionList) return;
					for (; i < TIMAAT.MediumCollectionDatasets.mediaCollectionList.length; i++) {
						$(TIMAAT.MediumCollectionDatasets.dataTableMediaCollectionList.row('#'+TIMAAT.MediumCollectionDatasets.mediaCollectionList[i].model.id).node()).removeClass('selected');
					}
					$(TIMAAT.MediumCollectionDatasets.dataTableMediaCollectionList.row('#'+this.selectedMediumCollectionId).node()).removeClass('selected');
					this.selectedMediumCollectionId = null;
				break;
				case 'mediumCollectionItem':
					// if (!TIMAAT.MediumCollectionDatasets.mediaCollectionItemList) return;
					// for (; i < TIMAAT.MediumCollectionDatasets.mediaCollectionItemList.length; i++) {
					// 	$(TIMAAT.MediumCollectionDatasets.dataTableMediaCollectionItemList.row('#'+TIMAAT.MediumCollectionDatasets.mediaCollectionItemList[i].model.id).node()).removeClass('selected');
					// }
					// $(TIMAAT.MediumCollectionDatasets.dataTableMediaCollectionItemList.row('#'+this.selectedMediumCollectionItemId).node()).removeClass('selected');
					this.selectedMediumCollectionItemId = null;
				break;
				case 'medium':
					if (!TIMAAT.MediumDatasets.media) return;
					for (; i < TIMAAT.MediumDatasets.media.length; i++) {
						$(TIMAAT.MediumDatasets.dataTableMedia.row('#'+TIMAAT.MediumDatasets.media[i].model.id).node()).removeClass('selected');
					}
					$(TIMAAT.MediumDatasets.dataTableMedia.row('#'+this.selectedMediumId).node()).removeClass('selected');
					this.selectedMediumId = null;
				break;
				case 'audio':
					if (!TIMAAT.MediumDatasets.audios) return;
					for (; i < TIMAAT.MediumDatasets.audios.length; i++) {
						$(TIMAAT.MediumDatasets.dataTableAudio.row('#'+TIMAAT.MediumDatasets.audios[i].model.id).node()).removeClass('selected');
					}
					$(TIMAAT.MediumDatasets.dataTableAudio.row('#'+this.selectedMediumId).node()).removeClass('selected');
					// this.clearLastSelection('medium');
					this.selectedMediumId = null;
				break;
				case 'document':
					if (!TIMAAT.MediumDatasets.documents) return;
					for (; i < TIMAAT.MediumDatasets.documents.length; i++) {
						$(TIMAAT.MediumDatasets.dataTableDocument.row('#'+TIMAAT.MediumDatasets.documents[i].model.id).node()).removeClass('selected');
					}
					$(TIMAAT.MediumDatasets.dataTableDocument.row('#'+this.selectedMediumId).node()).removeClass('selected');
					// this.clearLastSelection('medium');
					this.selectedMediumId = null;
				break;
				case 'image':
					if (!TIMAAT.MediumDatasets.images) return;
					for (; i < TIMAAT.MediumDatasets.images.length; i++) {
						$(TIMAAT.MediumDatasets.dataTableImage.row('#'+TIMAAT.MediumDatasets.images[i].model.id).node()).removeClass('selected');
					}
					$(TIMAAT.MediumDatasets.dataTableImage.row('#'+this.selectedMediumId).node()).removeClass('selected');
					// this.clearLastSelection('medium');
					this.selectedMediumId = null;
				break;
				case 'software':
					if (!TIMAAT.MediumDatasets.softwares) return;
					for (; i < TIMAAT.MediumDatasets.softwares.length; i++) {
						$(TIMAAT.MediumDatasets.dataTableSoftware.row('#'+TIMAAT.MediumDatasets.softwares[i].model.id).node()).removeClass('selected');
					}
					$(TIMAAT.MediumDatasets.dataTableSoftware.row('#'+this.selectedMediumId).node()).removeClass('selected');
					// this.clearLastSelection('medium');
					this.selectedMediumId = null;
				break;
				case 'text':
					if (!TIMAAT.MediumDatasets.texts) return;
					for (; i < TIMAAT.MediumDatasets.texts.length; i++) {
						$(TIMAAT.MediumDatasets.dataTableText.row('#'+TIMAAT.MediumDatasets.texts[i].model.id).node()).removeClass('selected');
					}
					$(TIMAAT.MediumDatasets.dataTableText.row('#'+this.selectedMediumId).node()).removeClass('selected');
					// this.clearLastSelection('medium');
					this.selectedMediumId = null;
				break;
				case 'video':
					if (!TIMAAT.MediumDatasets.videos) return;
					for (; i < TIMAAT.MediumDatasets.videos.length; i++) {
						$(TIMAAT.MediumDatasets.dataTableVideo.row('#'+TIMAAT.MediumDatasets.videos[i].model.id).node()).removeClass('selected');
					}
					$(TIMAAT.MediumDatasets.dataTableVideo.row('#'+this.selectedMediumId).node()).removeClass('selected');
					// this.clearLastSelection('medium');
					this.selectedMediumId = null;
				break;
				case 'videogame':
					if (!TIMAAT.MediumDatasets.videogames) return;
					for (; i < TIMAAT.MediumDatasets.videogames.length; i++) {
						$(TIMAAT.MediumDatasets.dataTableVideogame.row('#'+TIMAAT.MediumDatasets.videogames[i].model.id).node()).removeClass('selected');
					}
					$(TIMAAT.MediumDatasets.dataTableVideogame.row('#'+this.selectedMediumId).node()).removeClass('selected');
					// this.clearLastSelection('medium');
					this.selectedMediumId = null;
				break;
			}
		},

		refreshDataTable: async function(type, id=null){
      // console.log("TCL: refreshDataTable:function -> type: ", type);
			switch(type) {
				case 'mediumCollection':
					if (TIMAAT.MediumCollectionDatasets.dataTableMediaCollectionList) {
						TIMAAT.MediumCollectionDatasets.dataTableMediaCollectionList.ajax.reload(null, false);
					}
					if (TIMAAT.MediumCollectionDatasets.dataTableMediaCollectionItemList) {
						TIMAAT.MediumCollectionDatasets.dataTableMediaCollectionItemList.ajax.reload(null, false);
					}
				break;
				case 'medium':
					if (TIMAAT.MediumDatasets.dataTableMedia) {
						TIMAAT.MediumDatasets.dataTableMedia.ajax.reload(null, false);
					}
				break;
				case 'audio':
					if (TIMAAT.MediumDatasets.dataTableAudio) {
						TIMAAT.MediumDatasets.dataTableAudio.ajax.reload(null, false);
					}
					// await this.refreshDataTable('medium');
				break;
				case 'document':
					if (TIMAAT.MediumDatasets.dataTableDocument) {
						TIMAAT.MediumDatasets.dataTableDocument.ajax.reload(null, false);
					}
					// await this.refreshDataTable('medium');
				break;
				case 'image':
					if (TIMAAT.MediumDatasets.dataTableImage) {
						TIMAAT.MediumDatasets.dataTableImage.ajax.reload(null, false);
					}
					// await this.refreshDataTable('medium');
				break;
				case 'software':
					if (TIMAAT.MediumDatasets.dataTableSoftware) {
						TIMAAT.MediumDatasets.dataTableSoftware.ajax.reload(null, false);
					}
					// await this.refreshDataTable('medium');
				break;
				case 'text':
					if (TIMAAT.MediumDatasets.dataTableText) {
						TIMAAT.MediumDatasets.dataTableText.ajax.reload(null, false);
					}
					// await this.refreshDataTable('medium');
				break;
				case 'video':
					if (TIMAAT.MediumDatasets.dataTableVideo) {
						TIMAAT.MediumDatasets.dataTableVideo.ajax.reload(null, false);
					}
					// await this.refreshDataTable('medium');
				break;
				case 'videogame':
					if (TIMAAT.MediumDatasets.dataTableVideogame) {
						TIMAAT.MediumDatasets.dataTableVideogame.ajax.reload(null, false);
					}
					// await this.refreshDataTable('medium');
				break;
				case 'language':
					if (TIMAAT.LanguageLists.dataTableLanguages) {
						TIMAAT.LanguageLists.dataTableLanguages.ajax.reload(null, false);
					}
				break;
				case 'event':
					if (TIMAAT.EventDatasets.dataTableEvent) {
						TIMAAT.EventDatasets.dataTableEvent.ajax.reload(null, false);
					}
				break;
				case 'music':
					if (TIMAAT.MusicDatasets.dataTableMusic) {
						TIMAAT.MusicDatasets.dataTableMusic.ajax.reload(null, false);
					}
				break;
				case 'nashid':
					if (TIMAAT.MusicDatasets.dataTableNashid) {
						TIMAAT.MusicDatasets.dataTableNashid.ajax.reload(null, false);
					}
					// await this.refreshDataTable('music');
				break;
				case 'churchMusic':
					if (TIMAAT.MusicDatasets.dataTableChurchMusic) {
						TIMAAT.MusicDatasets.dataTableChurchMusic.ajax.reload(null, false);
					}
					// await this.refreshDataTable('music');
				break;
				case 'category':
          if (TIMAAT.CategoryLists.dataTableCategories) {
            TIMAAT.CategoryLists.dataTableCategories.ajax.reload(null, false);
          }
        break;
        case 'categorySet':
          if (TIMAAT.CategoryLists.dataTableCategorySets) {
            TIMAAT.CategoryLists.dataTableCategorySets.ajax.reload(null, false);
          }
        break;
				case 'role':
          if (TIMAAT.RoleLists.dataTableRoles) {
            TIMAAT.RoleLists.dataTableRoles.ajax.reload(null, false);
          }
        break;
        case 'roleGroup':
          if (TIMAAT.RoleLists.dataTableRoleGroups) {
            TIMAAT.RoleLists.dataTableRoleGroups.ajax.reload(null, false);
          }
        break;
				case 'actor':
					if (TIMAAT.ActorDatasets.dataTableActor) {
						TIMAAT.ActorDatasets.dataTableActor.ajax.reload(null, false);
					}
				break;
				case 'person':
					if (TIMAAT.ActorDatasets.dataTablePerson) {
						TIMAAT.ActorDatasets.dataTablePerson.ajax.reload(null, false);
					}
					// await this.refreshDataTable('actor');
				break;
				case 'collective':
					if (TIMAAT.ActorDatasets.dataTableCollective) {
						TIMAAT.ActorDatasets.dataTableCollective.ajax.reload(null, false);
					}
					// await this.refreshDataTable('actor');
				break;
				case 'tag':
					if (TIMAAT.TagLists.dataTableTags) {
						TIMAAT.TagLists.dataTableTags.ajax.reload(null, false);
					}
				break;
			}
		},
	}

}, window));
