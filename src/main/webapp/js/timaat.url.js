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

    TIMAAT.URLHistory = {

        init: function () {

            // $(document).ready(function() {
            //   if ()
            //   window.location.hash = '';
            // });

            $(window).on('popstate', async function () {
                // console.log("TCL ~ $ ~ popstate");
                if (TIMAAT.Service.session) {
                    //* get url hash and traverse through each segment to determine what to load and display
                    let currentUrlHash = window.location.hash;
                    await TIMAAT.URLHistory.setupView(currentUrlHash);
                }
            });

            $(window).on('hashchange', async function (e) {
                // console.log("TCL ~ $ ~ hashchange");
                // TODO handling of deleted entries when using browser back button

                //* get url hash and traverse through each segment to determine what to load and display
                // let currentUrlHash = window.location.hash;
                // await TIMAAT.URLHistory.setupView(currentUrlHash);
                // history.pushState(null, document.title, window.location.href);
            });
        },

        setURL: function (data, title, hash) {
            // console.log("TCL ~ data, title, hash", data, title, hash);
            let currentURL = new URL(document.URL);
            if (currentURL.hash != hash) {
                currentURL.hash = hash;
                let newURL = currentURL.href;
                history.pushState(data, title, newURL);
                document.title = `TIMAAT | ${title}`;
            }
        },

        setupView: async function (currentUrlHash) {
            currentUrlHash = currentUrlHash.slice(1); // remove #
            // separate url hash into its path segments
            const pathSegments = currentUrlHash.split('/');
            if (pathSegments.length >= 1) {
                switch (pathSegments[0]) {
                    // case '':
                    //   // open login page
                    // break;
                    case 'medium': // #medium...
                        // make sure media list is loaded
                        // if (!TIMAAT.MediumDatasets.mediaLoaded) {
                        //   TIMAAT.MediumDatasets.setMediumList();
                        //   TIMAAT.MediumDatasets.mediaLoaded = true;
                        // }
                        // show media component
                        TIMAAT.UI.showComponent('media');
                        // show corresponding medium form
                        if (pathSegments.length >= 2 && !isNaN(pathSegments[1])) { // path segment is id of current medium
                            $('#mediumDatasetsAllMediaList tr[id=' + pathSegments[1] + ']').trigger('click');
                            let medium = {};
                            medium.model = await TIMAAT.MediumService.getMedium(pathSegments[1]);
                            let type = medium.model.mediaType.mediaTypeTranslations[0].type;
                            TIMAAT.UI.clearLastSelection('medium');
                            TIMAAT.UI.selectedMediumId = pathSegments[1];
                            TIMAAT.UI.refreshDataTable('medium');
                            $('#mediumFormMetadata').data('type', type);
                            $('#mediumFormMetadata').data('medium', medium);
                            if (pathSegments.length == 2) { //* #medium/:id     (default view, show data sheet)
                                TIMAAT.UI.displayComponent('medium', 'mediumTab', 'mediumDataTable', 'mediumMetadataTab', 'mediumFormMetadata');
                                TIMAAT.UI.displayDataSetContent('dataSheet', medium, 'medium');
                            } else { // other medium form than data sheet
                                switch (pathSegments[2]) {
                                    case 'preview': //* #medium/:id/preview
                                        TIMAAT.UI.displayComponent('medium', 'mediumTab', 'mediumDataTable', 'mediumPreviewTab', 'mediumFormPreview');
                                        TIMAAT.UI.displayDataSetContent('preview', medium, 'medium');
                                        break;
                                    case 'titles': //* #medium/:id/titles
                                        TIMAAT.UI.displayComponent('medium', 'mediumTab', 'mediumDataTable', 'mediumTitlesTab', 'mediumFormTitles');
                                        TIMAAT.UI.displayDataSetContent('titles', medium, 'medium');
                                        break;
                                    case 'languages': //* #medium/:id/languages
                                        TIMAAT.UI.displayComponent('medium', 'mediumTab', 'mediumDataTable', 'mediumLanguageTracksTab', 'mediumFormLanguageTracks');
                                        TIMAAT.UI.displayDataSetContent('languageTracks', medium, 'medium');
                                        break;
                                    case 'actorsWithRoles': //* #medium/:id/actorsWithRoles
                                        TIMAAT.UI.displayComponent('medium', 'mediumTab', 'mediumDataTable', 'mediumActorWithRolesTab', 'mediumFormActorWithRoles');
                                        TIMAAT.UI.displayDataSetContent('actorWithRoles', medium, 'medium');
                                        break;
                                    case 'music': //* #medium/:id/music
                                        TIMAAT.UI.displayComponent('medium', 'mediumTab', 'mediumDataTable', 'mediumMusicTab', 'mediumFormMusic');
                                        TIMAAT.UI.displayDataSetContent('music', medium, 'medium')
                                        break
                                    case 'transcriptions': //* #medium/:id/transcriptions
                                        TIMAAT.UI.displayComponent('medium', 'mediumTab', 'mediumDataTable', 'mediumTranscriptionTab', 'mediumFormTranscriptions');
                                        TIMAAT.UI.displayDataSetContent('transcription', medium, 'medium')
                                        break
                                    default:
                                        this.redirectToDefaultView();
                                        break;
                                }
                            }
                        } else {
                            switch (pathSegments[1]) {
                                case 'list':
                                    if (!TIMAAT.MediumDatasets.dataTableAllMediaList) {
                                        await TIMAAT.MediumDatasets.setupAllMediaDataTable();
                                    }
                                    TIMAAT.MediumDatasets.dataTableAllMediaList.ajax.url('/TIMAAT/api/medium/list')
                                    TIMAAT.MediumDatasets.dataTableAllMediaList.ajax.reload();
                                    TIMAAT.UI.displayComponent('medium', null, 'mediumDataTable');
                                    $('#mediumTab').addClass('active');
                                    TIMAAT.UI.displayDataSetContentArea('mediumAllMedia');
                                    $('#mediumDatasetsAllMedia').addClass('list-group-item--is-active');
                                    break;
                                case 'audio': // #medium/audio ...
                                case 'document': // #medium/document ...
                                case 'image': // #medium/image ...
                                case 'software': // #medium/software ...
                                case 'text': // #medium/text ...
                                case 'video': // #medium/video ...
                                case 'videogame': // #medium/videogame ...
                                    if (pathSegments.length >= 3 && !isNaN(pathSegments[2])) { // path segment is id of current medium
                                        $('#mediumDatasets' + pathSegments[1] + 'List tr[id=' + pathSegments[2] + ']').trigger('click');
                                        let medium = {};
                                        medium.model = await TIMAAT.MediumService.getMedium(pathSegments[2]);
                                        let type = medium.model.mediaType.mediaTypeTranslations[0].type;
                                        TIMAAT.UI.clearLastSelection(type);
                                        TIMAAT.UI.selectedMediumId = pathSegments[2];
                                        TIMAAT.UI.refreshDataTable(type);
                                        $('#mediumFormMetadata').data('type', type);
                                        $('#mediumFormMetadata').data('medium', medium);
                                        if (pathSegments.length == 3) { //* #medium/:type/:id     (default view, show data sheet)
                                            TIMAAT.UI.displayComponent('medium', type + 'Tab', type + 'DataTable', 'mediumMetadataTab', 'mediumFormMetadata');
                                            TIMAAT.UI.displayDataSetContent('dataSheet', medium, 'medium');
                                            $('#mediumDatasetsAll' + type + 's').addClass('active');
                                        } else { // other medium form than data sheet
                                            switch (pathSegments[3]) {
                                                case 'preview': //* #medium/:type/:id/preview
                                                    TIMAAT.UI.displayComponent('medium', type + 'Tab', type + 'DataTable', 'mediumPreviewTab', 'mediumFormPreview');
                                                    TIMAAT.UI.displayDataSetContent('preview', medium, 'medium');
                                                    break;
                                                case 'titles': //* #medium/:type/:id/titles
                                                    TIMAAT.UI.displayComponent('medium', type + 'Tab', type + 'DataTable', 'mediumTitlesTab', 'mediumFormTitles');
                                                    TIMAAT.UI.displayDataSetContent('titles', medium, 'medium');
                                                    break;
                                                case 'languages': //* #medium/:type/:id/languages
                                                    TIMAAT.UI.displayComponent('medium', type + 'Tab', type + 'DataTable', 'mediumLanguageTracksTab', 'mediumFormLanguageTracks');
                                                    TIMAAT.UI.displayDataSetContent('languageTracks', medium, 'medium');
                                                    break;
                                                case 'actorsWithRoles': //* #medium/:type/:id/actorsWithRoles
                                                    TIMAAT.UI.displayComponent('medium', type + 'Tab', type + 'DataTable', 'mediumActorWithRolesTab', 'mediumFormActorWithRoles');
                                                    TIMAAT.UI.displayDataSetContent('actorWithRoles', medium, 'medium');
                                                    break;
                                                case 'music': //* #medium/:type/:id/music
                                                    TIMAAT.UI.displayComponent('medium', type + 'Tab', type + 'DataTable', 'mediumMusicTab', 'mediumFormMusic');
                                                    TIMAAT.UI.displayDataSetContent('music', medium, 'medium');
                                                    break
                                                case 'transcriptions': //* #medium/:type/:id/transcriptions
                                                    TIMAAT.UI.displayComponent('medium', 'mediumTab', 'mediumDataTable', 'mediumTranscriptionTab', 'mediumFormTranscriptions');
                                                    TIMAAT.UI.displayDataSetContent('transcription', medium, 'medium')
                                                    break
                                                default:
                                                    this.redirectToDefaultView();
                                                    break;
                                            }
                                        }
                                    } else if (pathSegments.length >= 3 && pathSegments[2] == 'list') { //* .../list
                                        switch (pathSegments[1]) {
                                            case 'audio': // #medium/audio ...
                                                if (!TIMAAT.MediumDatasets.dataTableAllAudiosList) {
                                                    await TIMAAT.MediumDatasets.setupAllAudiosDataTable();
                                                }
                                                TIMAAT.MediumDatasets.dataTableAllAudiosList.ajax.url('/TIMAAT/api/medium/audio/list')
                                                TIMAAT.MediumDatasets.dataTableAllAudiosList.ajax.reload();
                                                TIMAAT.UI.displayComponent('medium', null, 'audioDataTable');
                                                $('#' + pathSegments[1] + 'Tab').addClass('active');
                                                TIMAAT.UI.displayDataSetContentArea('mediumAllAudios');
                                                $('#mediumDatasetsAllAudios').addClass('active');
                                                break;
                                            case 'document': // #medium/document ...
                                                if (!TIMAAT.MediumDatasets.dataTableAllDocumentsList) {
                                                    await TIMAAT.MediumDatasets.setupAllDocumentsDataTable();
                                                }
                                                TIMAAT.MediumDatasets.dataTableAllDocumentsList.ajax.url('/TIMAAT/api/medium/document/list')
                                                TIMAAT.MediumDatasets.dataTableAllDocumentsList.ajax.reload();
                                                TIMAAT.UI.displayComponent('medium', null, 'documentDataTable');
                                                $('#' + pathSegments[1] + 'Tab').addClass('active');
                                                TIMAAT.UI.displayDataSetContentArea('mediumAllDocuments');
                                                $('#mediumDatasetsAllDocuments').addClass('active');
                                                break;
                                            case 'image': // #medium/image ...
                                                if (!TIMAAT.MediumDatasets.dataTableAllImagesList) {
                                                    await TIMAAT.MediumDatasets.setupAllImagesDataTable();
                                                }
                                                TIMAAT.MediumDatasets.dataTableAllImagesList.ajax.url('/TIMAAT/api/medium/image/list')
                                                TIMAAT.MediumDatasets.dataTableAllImagesList.ajax.reload();
                                                TIMAAT.UI.displayComponent('medium', null, 'imageDataTable');
                                                $('#' + pathSegments[1] + 'Tab').addClass('active');
                                                TIMAAT.UI.displayDataSetContentArea('mediumAllImages');
                                                $('#mediumDatasetsAllImages').addClass('active');
                                                break;
                                            case 'software': // #medium/software ...
                                                if (!TIMAAT.MediumDatasets.dataTableAllSoftwaresList) {
                                                    await TIMAAT.MediumDatasets.setupAllSoftwaresDataTable();
                                                }
                                                TIMAAT.MediumDatasets.dataTableAllSoftwaresList.ajax.url('/TIMAAT/api/medium/software/lList')
                                                TIMAAT.MediumDatasets.dataTableAllSoftwaresList.ajax.reload();
                                                TIMAAT.UI.displayComponent('medium', null, 'softwareDataTable');
                                                $('#' + pathSegments[1] + 'Tab').addClass('active');
                                                TIMAAT.UI.displayDataSetContentArea('mediumAllSoftwares');
                                                $('#mediumDatasetsAllSoftwares').addClass('active');
                                                break;
                                            case 'text': // #medium/text ...
                                                if (!TIMAAT.MediumDatasets.dataTableAllTextsList) {
                                                    await TIMAAT.MediumDatasets.setupAllTextsDataTable();
                                                }
                                                TIMAAT.MediumDatasets.dataTableAllTextsList.ajax.url('/TIMAAT/api/medium/text/list')
                                                TIMAAT.MediumDatasets.dataTableAllTextsList.ajax.reload();
                                                TIMAAT.UI.displayComponent('medium', null, 'textDataTable');
                                                $('#' + pathSegments[1] + 'Tab').addClass('active');
                                                TIMAAT.UI.displayDataSetContentArea('mediumAllTexts');
                                                $('#mediumDatasetsAllTexts').addClass('active');
                                                break;
                                            case 'video': // #medium/video ...
                                                if (!TIMAAT.MediumDatasets.dataTableAllVideosList) {
                                                    await TIMAAT.MediumDatasets.setupAllVideosDataTable();
                                                }
                                                TIMAAT.MediumDatasets.dataTableAllVideosList.ajax.url('/TIMAAT/api/medium/video/list')
                                                TIMAAT.MediumDatasets.dataTableAllVideosList.ajax.reload();
                                                TIMAAT.UI.displayComponent('medium', null, 'videoDataTable');
                                                $('#' + pathSegments[1] + 'Tab').addClass('active');
                                                TIMAAT.UI.displayDataSetContentArea('mediumAllVideos');
                                                $('#mediumDatasetsAllVideos').addClass('active');
                                                break;
                                            case 'videogame': // #medium/videogame ...
                                                if (!TIMAAT.MediumDatasets.dataTableAllVideogamesList) {
                                                    await TIMAAT.MediumDatasets.setupAllVideogamesDataTable();
                                                }
                                                TIMAAT.MediumDatasets.dataTableAllVideogamesList.ajax.url('/TIMAAT/api/medium/videogame/list')
                                                TIMAAT.MediumDatasets.dataTableAllVideogamesList.ajax.reload();
                                                TIMAAT.UI.displayComponent('medium', null, 'videogameDataTable');
                                                $('#' + pathSegments[1] + 'Tab').addClass('active');
                                                TIMAAT.UI.displayDataSetContentArea('mediumAllVideogames');
                                                $('#mediumDatasetsAllVideogames').addClass('active');
                                                break;
                                        }
                                    } else {
                                        this.redirectToDefaultView();
                                    }
                                    break;
                                default:
                                    this.redirectToDefaultView();
                                    break;
                            }
                        }
                        break;
                    case 'mediumCollection': // #mediumCollection...
                        // TODO make sure media collections are loaded
                        // show media collection component
                        TIMAAT.UI.showComponent('media');
                        // show corresponding medium collection form
                        if (pathSegments.length >= 2 && !isNaN(pathSegments[1])) { // path segment is id of current medium collection
                            $('#mediumCollectionDatasetsMediumCollectionList tr[id=' + pathSegments[1] + ']').trigger('click');
                            let mediumCollection = {};
                            mediumCollection.model = await TIMAAT.MediumCollectionService.getMediumCollection(pathSegments[1]);
                            let type = mediumCollection.model.mediaCollectionType.mediaCollectionTypeTranslations[0].type;
                            TIMAAT.MediumCollectionDatasets.currentPermissionLevel = await TIMAAT.MediumCollectionService.getMediumCollectionPermissionLevel(mediumCollection.model.id);
                            // TIMAAT.UI.clearLastSelection('mediumCollection');
                            TIMAAT.UI.selectedMediumCollectionId = pathSegments[1];
                            TIMAAT.UI.refreshDataTable('mediumCollection');
                            $('#mediumCollectionFormMetadata').data('mediumCollection', mediumCollection);
                            $('#mediumCollectionFormMetadata').data('type', type);
                            if (pathSegments.length == 2) { //* #mediumCollection/:id    (default view, show data sheet)
                                TIMAAT.UI.displayComponent('mediumCollection', 'mediumCollectionTab', 'mediumCollectionDataTable', 'mediumCollectionMetadataTab', 'mediumCollectionFormMetadata');
                                TIMAAT.UI.displayDataSetContent('dataSheet', mediumCollection, 'mediumCollection');
                            } else { // other mediumCollection form than data sheet
                                switch (pathSegments[2]) {
                                    case 'items': //* #mediumCollection/:id/items
                                        if (!TIMAAT.MediumCollectionDatasets.dataTableMediaCollectionItemList) {
                                            await TIMAAT.MediumCollectionDatasets.setupMediumCollectionItemListDataTable(mediumCollection.model.id);
                                            TIMAAT.MediumCollectionDatasets.setMediumCollectionItemList();
                                        }
                                        TIMAAT.UI.displayComponent('mediumCollection', 'mediumCollectionTab', 'mediumCollectionDataTable', 'mediumCollectionItemsTab', 'mediumCollectionMediaItems');
                                        TIMAAT.UI.displayDataSetContent('items', mediumCollection, 'mediumCollection');
                                        TIMAAT.MediumCollectionDatasets.dataTableMediaCollectionItemList.ajax.url('/TIMAAT/api/mediumCollection/' + mediumCollection.model.id + '/hasMediaList')
                                        TIMAAT.MediumCollectionDatasets.dataTableMediaCollectionItemList.ajax.reload().columns.adjust();
                                        // TIMAAT.MediumCollectionDatasets.clearLastItemSelection();
                                        break;
                                    // case 'publication': //* #mediumCollection/:id/publication
                                    // TIMAAT.UI.displayComponent('mediumCollection', 'mediumCollectionTab', 'mediumCollectionDataTable', 'mediumCollectionPublicationTab', 'mediumCollectionPublication');
                                    // TIMAAT.UI.displayDataSetContent('publication', mediumCollection, 'mediumCollection');
                                    // break;
                                    default:
                                        this.redirectToDefaultView();
                                        break;
                                }
                            }
                        } else {
                            switch (pathSegments[1]) {
                                case 'list': //* #mediumCollection/list
                                    TIMAAT.MediumCollectionDatasets.loadMediaCollections();
                                    TIMAAT.UI.displayComponent('mediumCollection', 'mediumCollectionTab', 'mediumCollectionDataTable');
                                    break;
                                default:
                                    this.redirectToDefaultView();
                                    break;
                            }
                        }
                        break;
                    case 'categories':
                        TIMAAT.UI.showComponent('categories');
                        if (pathSegments.length >= 2) {
                            switch (pathSegments[1]) {
                                case 'categories':
                                    TIMAAT.Categories.updateItemTypeSelection("category");
                                    break
                                case 'categorySets':
                                    TIMAAT.Categories.updateItemTypeSelection("categorySet");
                                    break
                            }
                        } else {

                        }
                        break
                    case 'actor': // #actor...
                        // make sure actor list is loaded
                        // if (!TIMAAT.ActorDatasets.actorsLoaded) {
                        //   TIMAAT.ActorDatasets.actorsLoaded = true;
                        // }
                        // show actors component
                        TIMAAT.UI.showComponent('actors');
                        // show corresponding actor form
                        if (pathSegments.length >= 2 && !isNaN(pathSegments[1])) { // path segment is id of current actor
                            $('#actorDatasetsActorList tr[id=' + pathSegments[1] + ']').trigger('click');
                            let actor = {};
                            actor.model = await TIMAAT.ActorService.getActor(pathSegments[1]);
                            let type = actor.model.actorType.actorTypeTranslations[0].type;
                            TIMAAT.UI.clearLastSelection('actor');
                            TIMAAT.UI.selectedActorId = pathSegments[1];
                            TIMAAT.UI.refreshDataTable('actor');
                            $('#actorFormMetadata').data('type', type);
                            $('#actorFormMetadata').data('actor', actor);
                            if (pathSegments.length == 2) { //* #actor/:id     (default view, show data sheet)
                                TIMAAT.UI.displayComponent('actor', 'actorTab', 'actorDataTable', 'actorTabMetadata', 'actorFormMetadata');
                                TIMAAT.UI.displayDataSetContent('dataSheet', actor, 'actor');
                            } else { // other actor form than data sheet
                                switch (pathSegments[2]) {
                                    case 'names': //* #actor/:id/preview
                                        TIMAAT.UI.displayComponent('actor', 'actorTab', 'actorDataTable', 'actorTabNames', 'actorFormNames');
                                        TIMAAT.UI.displayDataSetContent('names', actor, 'actor');
                                        break;
                                    case 'addresses': //* #actor/:id/titles
                                        TIMAAT.UI.displayComponent('actor', 'actorTab', 'actorDataTable', 'actorTabAddresses', 'actorFormAddresses');
                                        TIMAAT.UI.displayDataSetContent('addresses', actor, 'actor');
                                        break;
                                    case 'emails': //* #actor/:id/languages
                                        TIMAAT.UI.displayComponent('actor', 'actorTab', 'actorDataTable', 'actorTabEmailAddresses', 'actorFormEmailAddresses');
                                        TIMAAT.UI.displayDataSetContent('emails', actor, 'actor');
                                        break;
                                    case 'phoneNumbers': //* #actor/:id/actorsWithRoles
                                        TIMAAT.UI.displayComponent('actor', 'actorTab', 'actorDataTable', 'actorTabPhoneNumbers', 'actorFormPhoneNumbers');
                                        TIMAAT.UI.displayDataSetContent('phoneNumbers', actor, 'actor');
                                        break;
                                    case 'memberOfCollectives': //* #actor/:id/preview
                                        TIMAAT.UI.displayComponent('actor', 'actorTab', 'actorDataTable', 'actorTabMemberOfCollectives', 'actorFormMemberOfCollectives');
                                        TIMAAT.UI.displayDataSetContent('memberOfCollectives', actor, 'actor');
                                        break;
                                    case 'roles': //* #actor/:id/titles
                                        TIMAAT.UI.displayComponent('actor', 'actorTab', 'actorDataTable', 'actorTabRoles', 'actorFormRoles');
                                        TIMAAT.UI.displayDataSetContent('roles', actor, 'actor');
                                        break;
                                    case 'rolesInMedia': //* #actor/:id/languages
                                        TIMAAT.UI.displayComponent('actor', 'actorTab', 'actorDataTable', 'actorTabRoleInMedium', 'actorFormActorRoleInMedium');
                                        TIMAAT.UI.displayDataSetContent('rolesInMedia', actor, 'actor');
                                        break;
                                    default:
                                        this.redirectToDefaultView();
                                        break;
                                }
                            }
                        } else {
                            switch (pathSegments[1]) {
                                case 'list': //* #actor/list
                                    TIMAAT.ActorDatasets.loadActors();
                                    TIMAAT.UI.displayComponent('actor', 'actorTab', 'actorDataTable');
                                    break;
                                case 'person': // #actor/person ...
                                case 'collective': // #actor/collective ...
                                    // console.log("TCL: switch(pathSegments[1])", pathSegments[1]);
                                    if (pathSegments.length >= 3 && !isNaN(pathSegments[2])) { // path segment is id of current actor
                                        $('#actorDatasets' + pathSegments[1] + 'List tr[id=' + pathSegments[2] + ']').trigger('click');
                                        let actor = {};
                                        actor.model = await TIMAAT.ActorService.getActor(pathSegments[2]);
                                        let type = actor.model.actorType.actorTypeTranslations[0].type;
                                        $('#actorFormMetadata').data('actor', actor);
                                        $('#actorFormMetadata').data('type', type);
                                        TIMAAT.UI.clearLastSelection(type);
                                        TIMAAT.UI.selectedActorId = pathSegments[2];
                                        TIMAAT.UI.refreshDataTable(type);
                                        if (pathSegments.length == 3) { //* #actor/:type/:id     (default view, show data sheet)
                                            TIMAAT.UI.displayComponent('actor', type + 'Tab', type + 'DataTable', 'actorTabMetadata', 'actorFormMetadata');
                                            TIMAAT.UI.displayDataSetContent('dataSheet', actor, 'actor');
                                        } else { // other actor form than data sheet
                                            switch (pathSegments[3]) {
                                                case 'names': //* #actor/:type/:id/preview
                                                    TIMAAT.UI.displayComponent('actor', type + 'Tab', type + 'DataTable', 'actorTabNames', 'actorFormNames');
                                                    TIMAAT.UI.displayDataSetContent('names', actor, 'actor');
                                                    break;
                                                case 'addresses': //* #actor/:type/:id/titles
                                                    TIMAAT.UI.displayComponent('actor', type + 'Tab', type + 'DataTable', 'actorTabAddresses', 'actorFormAddresses');
                                                    TIMAAT.UI.displayDataSetContent('addresses', actor, 'actor');
                                                    break;
                                                case 'emails': //* #actor/:type/:id/languages
                                                    TIMAAT.UI.displayComponent('actor', type + 'Tab', type + 'DataTable', 'actorTabEmailAddresses', 'actorFormEmailAddresses');
                                                    TIMAAT.UI.displayDataSetContent('emails', actor, 'actor');
                                                    break;
                                                case 'phoneNumbers': //* #actor/:type/:id/actorsWithRoles
                                                    TIMAAT.UI.displayComponent('actor', type + 'Tab', type + 'DataTable', 'actorTabPhoneNumbers', 'actorFormPhoneNumbers');
                                                    TIMAAT.UI.displayDataSetContent('phoneNumbers', actor, 'actor');
                                                    break;
                                                case 'memberOfCollectives': //* #actor/:type/:id/preview
                                                    TIMAAT.UI.displayComponent('actor', type + 'Tab', type + 'DataTable', 'actorTabMemberOfCollectives', 'actorFormMemberOfCollectives');
                                                    TIMAAT.UI.displayDataSetContent('memberOfCollectives', actor, 'actor');
                                                    break;
                                                case 'roles': //* #actor/:type/:id/titles
                                                    TIMAAT.UI.displayComponent('actor', type + 'Tab', type + 'DataTable', 'actorTabRoles', 'actorFormRoles');
                                                    TIMAAT.UI.displayDataSetContent('roles', actor, 'actor');
                                                    break;
                                                case 'rolesInMedia': //* #actor/:type/:id/languages
                                                    TIMAAT.UI.displayComponent('actor', type + 'Tab', type + 'DataTable', 'actorTabRoleInMedium', 'actorFormActorRoleInMedium');
                                                    TIMAAT.UI.displayDataSetContent('rolesInMedia', actor, 'actor');
                                                    break;
                                                default:
                                                    this.redirectToDefaultView();
                                                    break;
                                            }
                                        }
                                    } else if (pathSegments.length >= 3 && pathSegments[2] == 'list') { //* .../list
                                        TIMAAT.UI.clearLastSelection(pathSegments[1]);
                                        TIMAAT.ActorDatasets.loadActorSubtype(pathSegments[1]);
                                        TIMAAT.UI.displayComponent('actor', pathSegments[1] + 'Tab', pathSegments[1] + 'DataTable');
                                    } else {
                                        this.redirectToDefaultView();
                                    }
                                    break;
                                default:
                                    this.redirectToDefaultView();
                                    break;
                            }
                        }
                        break;
                    case 'event': // #event...
                        // make sure event list is loaded
                        // if (!TIMAAT.EventDatasets.eventsLoaded) {
                        //   TIMAAT.EventDatasets.setEventList();
                        //   TIMAAT.EventDatasets.eventsLoaded = true;
                        // }
                        // show events component
                        TIMAAT.UI.showComponent('events');
                        // show corresponding event form
                        if (pathSegments.length >= 2 && !isNaN(pathSegments[1])) { // path segment is id of current event
                            $('#eventDatasetsEventList tr[id=' + pathSegments[1] + ']').trigger('click');
                            let event = {};
                            event.model = await TIMAAT.EventService.getEvent(pathSegments[1]);
                            // let type = event.model.eventType.eventTypeTranslations[0].type;
                            TIMAAT.UI.clearLastSelection('event');
                            TIMAAT.UI.selectedEventId = pathSegments[1];
                            TIMAAT.UI.refreshDataTable('event');
                            // $('#eventFormMetadata').data('type', type);
                            $('#eventFormMetadata').data('event', event);
                            if (pathSegments.length == 2) { //* #event/:id     (default view, show data sheet)
                                TIMAAT.UI.displayComponent('event', 'eventTab', 'eventDataTable', 'eventTabMetadata', 'eventFormMetadata');
                                TIMAAT.UI.displayDataSetContent('dataSheet', event, 'event');
                            } else { // other event form than data sheet
                                switch (pathSegments[2]) {
                                    //* no further data tabs currently available
                                    default:
                                        this.redirectToDefaultView();
                                        break;
                                }
                            }
                        } else {
                            switch (pathSegments[1]) {
                                case 'list': //* #event/list
                                    TIMAAT.EventDatasets.loadEvents();
                                    TIMAAT.UI.displayComponent('event', 'eventTab', 'eventDataTable');
                                    break;
                                default:
                                    this.redirectToDefaultView();
                                    break;
                            }
                        }
                        break;
                    case 'music': // #music...
                        // show music component
                        TIMAAT.UI.showComponent('music');
                        // show corresponding music form
                        if (pathSegments.length >= 2 && !isNaN(pathSegments[1])) { // path segment is id of current music
                            $('#musicDatasetsMusicList tr[id=' + pathSegments[1] + ']').trigger('click');
                            let music = {};
                            music.model = await TIMAAT.MusicService.getMusic(pathSegments[1]);
                            let type = music.model.musicType.musicTypeTranslations[0].type;
                            TIMAAT.UI.clearLastSelection('music');
                            TIMAAT.UI.selectedMusicId = pathSegments[1];
                            TIMAAT.UI.refreshDataTable('music');
                            $('#musicFormMetadata').data('type', type);
                            $('#musicFormMetadata').data('music', music);
                            if (pathSegments.length == 2) { //* #music/:id (default view, show data sheet)
                                TIMAAT.UI.displayComponent('music', 'musicTab', 'musicDataTable', 'musicTabMetadata', 'musicFormMetadata');
                                TIMAAT.UI.displayDataSetContent('dataSheet', music, 'music');
                            } else { // other music form than data sheet
                                switch (pathSegments[2]) {
                                    case 'preview': //* #music/:id/preview
                                        TIMAAT.UI.displayComponent('music', 'musicTab', 'musicDataTable', 'musicTabPreview', 'musicFormPreview');
                                        TIMAAT.UI.displayDataSetContent('preview', music, 'music');
                                        break;
                                    case 'titles': //* #music/:id/titles
                                        TIMAAT.UI.displayComponent('music', 'musicTab', 'musicDataTable', 'musicTabTitles', 'musicFormTitles');
                                        TIMAAT.UI.displayDataSetContent('titles', music, 'music');
                                        break;
                                    case 'actorsWithRoles': //* #music/:id/actorsWithRoles
                                        TIMAAT.UI.displayComponent('music', 'musicTab', 'musicDataTable', 'musicTabActorWithRoles', 'musicFormActorWithRoles');
                                        TIMAAT.UI.displayDataSetContent('actorWithRoles', music, 'music');
                                        break;
                                    case 'mediumHasMusicList': //* #music/:id/mediumHasMusicList
                                        TIMAAT.UI.displayComponent('music', 'musicTab', 'musicDataTable', 'musicTabMediumHasMusicList', 'musicFormMediumHasMusicList');
                                        TIMAAT.UI.displayDataSetContent('mediumHasMusicList', music, 'music');
                                        break;
                                    case 'transcriptions': //* #music/:id/transcriptions
                                        TIMAAT.UI.displayComponent('music', 'musicTab', type + 'DataTable', 'musicTabTranscription', 'transcriptions');
                                        TIMAAT.UI.displayDataSetContent('transcriptions', music, 'music');
                                        break;
                                    default:
                                        this.redirectToDefaultView();
                                        break;
                                }
                            }
                        } else {
                            switch (pathSegments[1]) {
                                case 'list': //* #music/list
                                    TIMAAT.MusicDatasets.loadMusicList();
                                    TIMAAT.UI.displayComponent('music', 'musicTab', 'musicDataTable');
                                    break;
                                case 'nashid':
                                case 'churchMusic':
                                    if (pathSegments.length >= 3 && !isNaN(pathSegments[2])) { // path segment is id of current music
                                        $('#musicDatasets' + pathSegments[1] + 'List tr[id=' + pathSegments[2] + ']').trigger('click');
                                        let music = {};
                                        music.model = await TIMAAT.MusicService.getMusic(pathSegments[2]);
                                        let type = music.model.musicType.musicTypeTranslations[0].type;
                                        TIMAAT.UI.clearLastSelection(type);
                                        TIMAAT.UI.selectedMusicId = pathSegments[2];
                                        TIMAAT.UI.refreshDataTable(type);
                                        $('#musicFormMetadata').data('type', type);
                                        $('#musicFormMetadata').data('music', music);
                                        if (pathSegments.length == 3) { //* #music/:type/:id     (default view, show data sheet)
                                            TIMAAT.UI.displayComponent('music', type + 'Tab', type + 'DataTable', 'musicTabMetadata', 'musicFormMetadata');
                                            TIMAAT.UI.displayDataSetContent('dataSheet', music, 'music');
                                        } else { // other music form than data sheet
                                            switch (pathSegments[3]) {
                                                case 'preview': //* #music/:type/:id/preview
                                                    TIMAAT.UI.displayComponent('music', type + 'Tab', type + 'DataTable', 'musicTabPreview', 'musicFormPreview');
                                                    TIMAAT.UI.displayDataSetContent('preview', music, 'music');
                                                    break;
                                                case 'titles': //* #music/:type/:id/titles
                                                    TIMAAT.UI.displayComponent('music', type + 'Tab', type + 'DataTable', 'musicTabTitles', 'musicFormTitles');
                                                    TIMAAT.UI.displayDataSetContent('titles', music, 'music');
                                                    break;
                                                case 'actorsWithRoles': //* #music/:type/:id/actorsWithRoles
                                                    TIMAAT.UI.displayComponent('music', type + 'Tab', type + 'DataTable', 'musicTabActorWithRoles', 'musicFormActorWithRoles');
                                                    TIMAAT.UI.displayDataSetContent('actorWithRoles', music, 'music');
                                                    break;
                                                case 'mediumHasMusicList': //* #music/:type/:id/mediumHasMusicList
                                                    TIMAAT.UI.displayComponent('music', type + 'Tab', type + 'DataTable', 'musicTabMediumHasMusicList', 'musicFormMediumHasMusicList');
                                                    TIMAAT.UI.displayDataSetContent('mediumHasMusicList', music, 'music');
                                                    break;
                                                case 'transcriptions': //* #music/:type/:id/transcriptions
                                                    TIMAAT.UI.displayComponent('music', type + 'Tab', type + 'DataTable', 'musicTabTranscription', 'transcriptions');
                                                    TIMAAT.UI.displayDataSetContent('transcriptions', music, 'music');
                                                    break;
                                                default:
                                                    this.redirectToDefaultView();
                                                    break;
                                            }
                                        }
                                    } else if (pathSegments.length >= 3 && pathSegments[2] == 'list') { //* .../list
                                        TIMAAT.UI.clearLastSelection(pathSegments[1]);
                                        TIMAAT.MusicDatasets.loadMusicSubtype(pathSegments[1]);
                                        TIMAAT.UI.displayComponent('music', pathSegments[1] + 'Tab', pathSegments[1] + 'DataTable');
                                    } else {
                                        this.redirectToDefaultView();
                                    }
                                    break;
                                default:
                                    this.redirectToDefaultView();
                                    break;
                            }
                        }
                        break;
                    case 'role': // #role...
                        // make sure role list is loaded
                        // if (!TIMAAT.Lists.listsLoaded) {
                        //   TIMAAT.Lists.setLists();
                        //   TIMAAT.Lists.listsLoaded = true;
                        // }
                        // show role component
                        TIMAAT.UI.showComponent('lists');
                        // show corresponding role form
                        if (pathSegments.length >= 2 && !isNaN(pathSegments[1])) { // path segment is id of current role
                            $('#roleList tr[id=' + pathSegments[1] + ']').trigger('click');
                            let role = {};
                            role.model = await TIMAAT.RoleService.getRole(pathSegments[1]);
                            TIMAAT.UI.clearLastSelection('role');
                            TIMAAT.UI.selectedRoleId = pathSegments[1];
                            TIMAAT.UI.refreshDataTable('role');
                            $('#listsTabMetadata').data('type', 'role');
                            $('#roleFormMetadata').data('role', role);
                            if (pathSegments.length == 2) { //* #role/:id     (default view, show data sheet)
                                TIMAAT.UI.displayComponent('role', 'roleTab', 'roleDataTable', 'listsTabMetadata', 'roleFormMetadata');
                                TIMAAT.UI.displayDataSetContent('dataSheet', role, 'role');
                            } else { // other role form than data sheet
                                switch (pathSegments[2]) {
                                    //* no further data tabs currently available
                                    default:
                                        this.redirectToDefaultView();
                                        break;
                                }
                            }
                        } else {
                            switch (pathSegments[1]) {
                                case 'list': //* #role/list
                                    TIMAAT.RoleLists.loadRoles();
                                    TIMAAT.UI.displayComponent('role', 'roleTab', 'roleDataTable');
                                    break;
                                default:
                                    this.redirectToDefaultView();
                                    break;
                            }
                        }
                        break;
                    case 'roleGroup': // #roleGroup...
                        // make sure role group list is loaded
                        // if (!TIMAAT.Lists.listsLoaded) {
                        //   TIMAAT.Lists.setLists();
                        //   TIMAAT.Lists.listsLoaded = true;
                        // }
                        // show role group component
                        TIMAAT.UI.showComponent('lists');
                        // show corresponding role group form
                        if (pathSegments.length >= 2 && !isNaN(pathSegments[1])) { // path segment is id of current role group
                            $('#roleGroupList tr[id=' + pathSegments[1] + ']').trigger('click');
                            let roleGroup = {};
                            roleGroup.model = await TIMAAT.RoleService.getRoleGroup(pathSegments[1]);
                            TIMAAT.UI.clearLastSelection('roleGroup');
                            TIMAAT.UI.selectedRoleGroupId = pathSegments[1];
                            TIMAAT.UI.refreshDataTable('roleGroup');
                            $('#listsTabMetadata').data('type', 'roleGroup');
                            $('#roleGroupFormMetadata').data('roleGroup', roleGroup);
                            if (pathSegments.length == 2) { //* #roleGroup/:id     (default view, show data sheet)
                                TIMAAT.UI.displayComponent('roleGroup', 'roleGroupTab', 'roleGroupDataTable', 'listsTabMetadata', 'roleGroupFormMetadata');
                                TIMAAT.UI.displayDataSetContent('dataSheet', roleGroup, 'roleGroup');
                            } else { // other role group form than data sheet
                                switch (pathSegments[2]) {
                                    //* no further data tabs currently available
                                    default:
                                        this.redirectToDefaultView();
                                        break;
                                }
                            }
                        } else {
                            switch (pathSegments[1]) {
                                case 'list': //* #roleGroup/list
                                    TIMAAT.RoleLists.loadRoleGroups();
                                    TIMAAT.UI.displayComponent('roleGroup', 'roleGroupTab', 'roleGroupDataTable');
                                    break;
                                default:
                                    this.redirectToDefaultView();
                                    break;
                            }
                        }
                        break;
                    case 'language': // #language...
                        // make sure language list is loaded
                        // if (!TIMAAT.Lists.listsLoaded) {
                        //   TIMAAT.Lists.setLists();
                        //   TIMAAT.Lists.listsLoaded = true;
                        // }
                        // show language component
                        TIMAAT.UI.showComponent('lists');
                        // show corresponding language form
                        if (pathSegments.length >= 2 && !isNaN(pathSegments[1])) { // path segment is id of current language
                            $('#languageList tr[id=' + pathSegments[1] + ']').trigger('click');
                            let language = {};
                            language.model = await TIMAAT.LanguageService.getLanguage(pathSegments[1]);
                            TIMAAT.UI.clearLastSelection('language');
                            TIMAAT.UI.selectedLanguageId = pathSegments[1];
                            TIMAAT.UI.refreshDataTable('language');
                            $('#listsTabMetadata').data('type', 'language');
                            $('#languageFormMetadata').data('language', language);
                            if (pathSegments.length == 2) { //* #language/:id     (default view, show data sheet)
                                TIMAAT.UI.displayComponent('language', 'languageTab', 'languageDataTable', 'listsTabMetadata', 'languageFormMetadata');
                                TIMAAT.UI.displayDataSetContent('dataSheet', language, 'language');
                            } else { // other language form than data sheet
                                this.redirectToDefaultView();
                                // switch (pathSegments[2]) {
                                //   //* no further data tabs currently available
                                // }
                            }
                        } else {
                            switch (pathSegments[1]) {
                                case 'list': //* #language/list
                                    TIMAAT.LanguageLists.loadLanguages();
                                    TIMAAT.UI.displayComponent('language', 'languageTab', 'languageDataTable');
                                    break;
                                default:
                                    this.redirectToDefaultView();
                                    break;
                            }
                        }
                        break;
                    case 'settings': // #settings...
                        if (TIMAAT.Service.session && TIMAAT.Service.session.displayName == "admin") {
                            $('.adminAccessOnly').show();
                            TIMAAT.UI.showComponent('settings');
                            if (pathSegments[1] == 'bugfixes') {
                                TIMAAT.UI.displayComponent('settings', 'settingsBugfixesTab', 'settingsBugfixes');
                            } else {
                                TIMAAT.UI.displayComponent('settings', 'settingsAccountCreationTab', 'settingsAccountCreation');
                                TIMAAT.Settings.loadSettingsAccountCreation();
                            }
                        } else {
                            $('.adminAccessOnly').hide();
                            this.redirectToDefaultView();
                        }
                        break;
                    case 'analysis': // #analysis...
                        // make sure analysis list is loaded
                        // if (!TIMAAT.VideoPlayer.curAnalysisList) {
                        //   TIMAAT.VideoPlayer.
                        //   TIMAAT.MediumDatasets.mediaLoaded = true;
                        // }
                        // show video player component
                        TIMAAT.UI.showComponent('videoPlayer');
                        // show corresponding video and analysis lists
                        if (pathSegments.length >= 2 && !isNaN(pathSegments[1])) { // path segment is id of current analysis
                            // console.log("TCL: setupView:function -> pathSegments[1]", pathSegments[1]);
                            //* determine analysis list and corresponding medium via hashPath
                            let analysisList = await TIMAAT.AnalysisListService.getAnalysisList(pathSegments[1]);
                            let medium = await TIMAAT.MediumService.getMedium(analysisList.mediumID);
                            //* load necessary data to display UI
                            TIMAAT.VideoPlayer.setupMedium(medium);
                            let analysisLists = await TIMAAT.AnalysisListService.getMediumAnalysisLists(medium.id);
                            //* setup UI
                            await TIMAAT.VideoPlayer.setupMediumAnalysisLists(analysisLists);
                            await TIMAAT.VideoPlayer.loadAnalysisList(analysisList.id);
                            $('#analysisChooser').val(analysisList.id);
                            if (pathSegments.length == 2) { //* #analysis/:id     (default view, show analysis list)
                            } else { //* as long as no substructure is accessible via url
                                this.redirectToDefaultView();
                            }
                            // TODO annotation and segment data accessibility by url
                            // else { // element in analysis list selected
                            //   switch (pathSegments[2]) {
                            //     case 'annotation': // #analysis/:analysisId/annotation/:annotationId
                            //       console.log("TCL: setupView:function -> pathSegments[3] - Annotation: ", pathSegments[3]);
                            //     $('#annotation-'+pathSegments[3]).trigger('click');
                            //     break;
                            //     case 'segment': // #analysis/:analysisId/segment/:segmentId
                            //       console.log("TCL: setupView:function -> pathSegments[3] - Segment: ", pathSegments[3]);
                            //       $('#segment-'+pathSegments[3]).trigger('click');
                            //     break;
                            //     case 'scene': // #analysis/:analysisId/scene/:sceneId
                            //       $('#scene-'+pathSegments[3]).trigger('click');
                            //     break;
                            //     case 'action': // #analysis/:analysisId/action/:actionId
                            //       $('#action-'+pathSegments[3]).trigger('click');
                            //     break;
                            //     case 'take': // #analysis/:analysisId/take/:takeId
                            //       $('#take-'+pathSegments[3]).trigger('click');
                            //     break;
                            //   }
                            // }
                        } else {
                            this.redirectToDefaultView();
                        }
                        break;
                    default:
                        this.redirectToDefaultView();
                        break;
                }
            }
        },

        redirectToDefaultView: function () {
            // console.log("TCL: redirectToDefaultView", );
            // redirect if invalid url path is entered
            TIMAAT.URLHistory.setupView('#medium/list');
            TIMAAT.URLHistory.setURL(null, 'Media Library', '#medium/list');
        },

    }

}, window));