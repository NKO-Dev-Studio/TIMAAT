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

    TIMAAT.MediumService = {

        listMediumTypes(callback) {
            jQuery.ajax({
                url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/mediaType/list",
                type: "GET",
                contentType: "application/json; charset=utf-8",
                dataType: "json",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                },
            }).done(function (data) {
                callback(data);
            })
                .fail(function (error) {
                    console.error("ERROR responseText: ", error.responseText);
                    console.error("ERROR: ", error);
                });
        },

        // listMedia(callback) {
        // 	jQuery.ajax({
        // 		url:window.location.protocol+'//'+window.location.host+"/TIMAAT/api/medium/list",
        // 		type:"GET",
        // 		contentType:"application/json; charset=utf-8",
        // 		dataType:"json",
        // 		beforeSend: function (xhr) {
        // 			xhr.setRequestHeader('Authorization', 'Bearer '+TIMAAT.Service.token);
        // 		},
        // 	}).done(function(data) {
        // 		// console.log("TCL: listMedia -> data", data);
        // 		callback(data);
        // 	}).fail(function(error) {
        // 		console.error("ERROR responseText: ", error.responseText);
        // 		console.error("ERROR: ", error);
        // 	});
        // },

        async getMediaDatasetsTotal() {
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/total",
                    type: "GET",
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    // console.log("TCL: getMediaDatasetsTotal -> data", data);
                    resolve(data);
                }).fail(function (error) {
                    console.error("ERROR responseText: ", error.responseText);
                    console.error("ERROR: ", error);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        listMediumSubtype(mediumSubtype, callback) {
            // console.log("TCL: listMediumSubtype", mediumSubtype);
            jQuery.ajax({
                url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + mediumSubtype + "/list",
                type: "GET",
                // data: JSON.stringify(mediaType),
                contentType: "application/json; charset=utf-8",
                dataType: "json",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                },
            }).done(function (data) {
                // console.log("TCL: listMediumSubtype -> data", data);
                callback(data);
            })
                .fail(function (error) {
                    console.error("ERROR responseText: ", error.responseText);
                    console.error("ERROR: ", error);
                });
        },

        async getActorList(mediumId) {
            // console.log("TCL: getActorHasRoleList -> mediumId", mediumId);
            return new Promise(resolve => {
                jQuery.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + mediumId + "/hasActorList/",
                    type: "GET",
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    // console.log("TCL: getActorRolesList -> data", data);
                    resolve(data);
                })
                    .fail(function (error) {
                        console.error("ERROR responseText: ", error.responseText);
                        console.error("ERROR: ", error);
                    });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async getActorHasRoleList(mediumId, actorId) {
            // console.log("TCL: getActorHasRoleList -> mediumId, actorId", mediumId, actorId);
            return new Promise(resolve => {
                jQuery.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + mediumId + "/hasActor/" + actorId + "/withRoleList/",
                    type: "GET",
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    // console.log("TCL: getActorHasRoleList -> data", data);
                    resolve(data);
                })
                    .fail(function (error) {
                        console.error("ERROR responseText: ", error.responseText);
                        console.error("ERROR: ", error);
                    });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async getMedium(id) {
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + id,
                    type: "GET",
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    // console.log("TCL: getMedium -> data", data);
                    resolve(data);
                }).fail(function (error) {
                    console.error("ERROR responseText: ", error.responseText);
                    console.error("ERROR: ", error);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async getMediumDisplayTitle(id) {
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + id + "/title",
                    type: "GET",
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    // console.log("TCL: getMediumTitle -> data", data);
                    resolve(data);
                }).fail(function (error) {
                    console.error("ERROR responseText: ", error.responseText);
                    console.error("ERROR: ", error);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async getViewToken(id) {
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + id + "/viewToken",
                    type: "GET",
                    contentType: "application/json; charset=utf-8",
                    dataType: "text",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    // console.log("TCL: getViewToken -> data", data);
                    resolve(data);
                }).fail(function (error) {
                    console.error("ERROR responseText: ", error.responseText);
                    console.error("ERROR: ", error);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async getCategorySetList(mediumId) {
            // console.log("TCL: getCategorySetList -> mediumId", mediumId);
            return new Promise(resolve => {
                jQuery.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + mediumId + "/categorySet/list/",
                    type: "GET",
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    // console.log("TCL: getCategorySetList -> data", data);
                    resolve(data);
                })
                    .fail(function (error) {
                        console.error("ERROR responseText: ", error.responseText);
                        console.error("ERROR: ", error);
                    });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async getSelectedCategories(mediumId) {
            // console.log("TCL: getSelectedCategories -> mediumId", mediumId);
            return new Promise(resolve => {
                jQuery.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + mediumId + "/category/list/",
                    type: "GET",
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    // console.log("TCL: getSelectedCategories -> data", data);
                    resolve(data);
                })
                    .fail(function (error) {
                        console.error("ERROR responseText: ", error.responseText);
                        console.error("ERROR: ", error);
                    });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async getTagList(mediumId) {
            // console.log("TCL: getTagList -> for mediumId", mediumId);
            return new Promise(resolve => {
                jQuery.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + mediumId + "/hasTagList/",
                    type: "GET",
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    // console.log("TCL: getTagList -> data", data);
                    resolve(data);
                })
                    .fail(function (error) {
                        console.error("ERROR responseText: ", error.responseText);
                        console.error("ERROR: ", error);
                    });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async createMedium(mediumModel) {
            // console.log("TCL: async createMedium -> mediumModel", mediumModel);
            var newMediumModel = {
                id: 0,
                remark: mediumModel.remark,
                releaseDate: mediumModel.releaseDate,
                recordingStartDate: mediumModel.recordingStartDate,
                recordingEndDate: mediumModel.recordingEndDate,
                copyright: mediumModel.copyright,
                mediaType: {
                    id: mediumModel.mediaType.id,
                },
                // work: {
                // 	id: mediumModel.work.id,
                // },
                displayTitle: {
                    id: mediumModel.displayTitle.id,
                },
            };
            // console.log("TCL: createMedium -> newMediumModel", newMediumModel);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/0",
                    type: "POST",
                    data: JSON.stringify(newMediumModel),
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (mediumData) {
                    // console.log("TCL: createMedium -> returning mediumData", mediumData);
                    resolve(mediumData);
                }).fail(function (error) {
                    console.error("ERROR responseText:", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        // not yet needed (no translation data or translation table available at the moment)
        // async createMediumTranslation(model, modelTranslation) {
        // 	// console.log("TCL: createMediumTranslation -> model, modelTranslation", model, modelTranslation);
        // 	return new Promise(resolve => {
        // 		$.ajax({
        // 			url:window.location.protocol+'//'+window.location.host+"/TIMAAT/api/medium/"+model.id+"/translation/"+modelTranslation.id,
        // 			type:"POST",
        // 			data: JSON.stringify(modelTranslation),
        // 			contentType:"application/json; charset=utf-8",
        // 			dataType:"json",
        // 			beforeSend: function (xhr) {
        // 				xhr.setRequestHeader('Authorization', 'Bearer '+TIMAAT.Service.token);
        // 			},
        // 		}).done(function(translationData) {
        // 			// console.log("TCL: createMediumTranslation -> translationData", translationData);
        // 			resolve(translationData);
        // 		}).fail(function(error) {
        // 			console.error("ERROR responseText:", error.responseText);
        // 		});
        // 	}).catch((error) => {
        // 		console.error("ERROR: ", error);
        // 	});
        // },

        async createMediumSubtype(mediumSubtype, mediumModel, subtypeModel) {
            // console.log("TCL: createMediumSubtype -> mediumSubtype, mediumModel, subtypeModel", mediumSubtype, mediumModel, subtypeModel);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + mediumSubtype + "/" + mediumModel.id,
                    type: "POST",
                    data: JSON.stringify(subtypeModel),
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (subtypeData) {
                    // console.log("TCL: createMediumSubtype - returning subtypeData", subtypeData);
                    resolve(subtypeData);
                }).fail(function (error) {
                    console.error("ERROR responseText:", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async createTitle(title) {
            // console.log("TCL: async createTitle -> title", title);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/title/" + title.id,
                    type: "POST",
                    data: JSON.stringify(title),
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (titleData) {
                    // console.log("TCL: createTitle -> titleData", titleData);
                    resolve(titleData);
                }).fail(function (error) {
                    console.error("ERROR responseText:", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async addTitle(mediumId, title) {
            // console.log("TCL: addTitle -> mediumId, title", mediumId, title);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + mediumId + "/title/" + title.id,
                    type: "POST",
                    data: JSON.stringify(title),
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (titleData) {
                    // console.log("TCL: addTitle -> titleData", titleData);
                    resolve(titleData);
                }).fail(function (error) {
                    console.error("ERROR responseText:", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async addLanguageTrack(track) {
            // console.log("TCL: async addLanguageTrack -> track", track);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + track.mediumId + "/languageTrack/" + track.mediumLanguageTypeId + "/" + track.languageId,
                    type: "POST",
                    // data: JSON.stringify(track),
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    // console.log("TCL: addTrack -> data", data);
                    resolve(data);
                }).fail(function (error) {
                    console.error("ERROR: ", error);
                    console.error("ajax call fail - error: ", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async addMediumHasActorWithRoles(mediumId, actorId, roleId) {
            // console.log("TCL: addMediumHasActorWithRoles -> mediumId, actorId, roleIds", mediumId, actorId, roleId);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + mediumId + "/hasActor/" + actorId + "/withRole/" + roleId,
                    type: "POST",
                    // data: JSON.stringify(roleIds),
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    // console.log("TCL: addMediumHasActorWithRoles - done -> data", data);
                    resolve(data);
                }).fail(function (error) {
                    console.error("ERROR: ", error);
                    console.error("ajax call fail - error: ", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async addTag(mediumId, tagId) {
            // console.log("TCL: addTag -> mediumId, tagId", mediumId, tagId);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + mediumId + "/tag/" + tagId,
                    type: "POST",
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    resolve(data);
                })
                    .fail(function (error) {
                        console.error("ERROR: ", error);
                        console.error("ERROR responseText:", error.responseText);
                    });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async addCategorySet(mediumId, categorySetId) {
            // console.log("TCL: addCategorySet -> mediumId, categorySetId", mediumId, categorySetId);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + mediumId + "/categorySet/" + categorySetId,
                    type: "POST",
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    resolve(data);
                }).fail(function (error) {
                    console.error("ERROR: ", error);
                    console.error("ERROR responseText:", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async removeCategorySet(mediumId, categorySetId) {
            // console.log("TCL: removeCategorySet -> mediumId, categorySetName", mediumId, categorySetName);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + mediumId + "/categorySet/" + categorySetId,
                    type: "DELETE",
                    contentType: "application/json; charset=utf-8",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    // console.log("TCL: removeCategorySet -> data", data);
                    resolve(data);
                })
                    .fail(function (error) {
                        console.error("ERROR: ", error);
                        console.error("ERROR responseText:", error.responseText);
                    });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async addCategory(mediumId, categoryId) {
            // console.log("TCL: addCategory -> mediumId, categoryId", mediumId, categoryId);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + mediumId + "/category/" + categoryId,
                    type: "POST",
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    resolve(data);
                }).fail(function (error) {
                    console.error("ERROR: ", error);
                    console.error("ERROR responseText:", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async removeCategory(mediumId, categoryId) {
            // console.log("TCL: removeCategory -> mediumId, categoryId", mediumId, categoryId);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + mediumId + "/category/" + categoryId,
                    type: "DELETE",
                    contentType: "application/json; charset=utf-8",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    resolve(data);
                })
                    .fail(function (error) {
                        console.error("ERROR: ", error);
                        console.error("ERROR responseText:", error.responseText);
                    });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async createSource(source) {
            // console.log("TCL: async createSource -> source", source);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/source/" + source.id,
                    type: "POST",
                    data: JSON.stringify(source),
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (sourceData) {
                    // console.log("TCL: createSource -> sourceData", sourceData);
                    resolve(sourceData);
                }).fail(function (error) {
                    console.error("ERROR responseText:", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async updateMedium(mediumModel) {
            // console.log("TCL: MediumService: async updateMedium -> mediumModel", mediumModel);
            let model = mediumModel;
            delete model.ui;
            // console.log("TCL: updateMedium -> tempMediumModel", tempMediumModel);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + mediumModel.id,
                    type: "PATCH",
                    data: JSON.stringify(model),
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (updateData) {
                    // console.log("TCL: async updateMedium -> returning updateData", updateData);
                    resolve(updateData);
                }).fail(function (error) {
                    console.error("ERROR: ", error);
                    console.error("ERROR responseText:", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },
        async updateMediumThumbnail(mediumId, thumbnailPositionMs) {
            const payload = {
                thumbnailPositionMs
            }

            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/video/" + mediumId + "/thumbnail",
                    type: "POST",
                    data: JSON.stringify(payload),
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (updateData) {
                    resolve(updateData);
                }).fail(function (error) {
                    console.error("ERROR: ", error);
                    console.error("ERROR responseText:", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async getPreviewImageBlobForMediumVideoAtPlaybackPosition(mediumId, positionMs, mediumToken) {
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/video/" + mediumId + "/thumbnail?token=" + mediumToken + "&time=" + positionMs,
                    type: "GET",
                    xhrFields: {responseType: 'blob'},
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    // console.log("TCL: getSelectedCategories -> data", data);
                    resolve(data);
                })
                    .fail(function (error) {
                        console.error("ERROR responseText: ", error.responseText);
                        console.error("ERROR: ", error);
                    });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async updateMediumHasMusicList(mediumId, mediumHasMusicList) {
            const payload = {
                mediumHasMusicListEntries: mediumHasMusicList
            }

            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + mediumId + "/mediumHasMusicList",
                    type: "PUT",
                    data: JSON.stringify(payload),
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (updateData) {
                    resolve(updateData);
                }).fail(function (error) {
                    console.error("ERROR: ", error);
                    console.error("ERROR responseText:", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async updateMediumSubtype(mediumSubtype, subtypeModel) {
            // console.log("TCL: updateMediumSubtype -> mediumSubtype, subtypeModel", mediumSubtype, subtypeModel);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + mediumSubtype + "/" + subtypeModel.mediumId,
                    type: "PATCH",
                    data: JSON.stringify(subtypeModel),
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (updateData) {
                    // console.log("TCL: async updateMediumSubtype -> returning updateData", updateData);
                    resolve(updateData);
                }).fail(function (error) {
                    console.error("ERROR: ", error);
                    console.error("ERROR responseText:", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async updateFileStatus(medium, type) {
            // console.log("TCL: updateFileStatus(medium, type)", type);
            // medium.poll = window.setInterval(function() {
            // if ( medium.ui && !medium.ui.is(':visible') ) return;
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + type + "/" + medium.id + '/status',
                    type: "GET",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    // console.log("TCL: updateFileStatus -> data: ", data);
                    // if ( medium.fileStatus && medium.fileStatus == data ) return;
                    // medium.fileStatus = data;

                    // TIMAAT.MediumDatasets.setMediumStatus(medium);

                    // if (medium.fileStatus == 'unavailable' || medium.fileStatus == 'ready')
                    // 	window.clearInterval(medium.poll);
                    // return(data);
                    resolve(data);
                }).fail(function (error) {
                    // TODO handle error
                    // window.clearInterval(medium.poll);
                    // medium.ui.find('.js-medium-file-status').html('<i class="fas fa-eye-slash"></i> not available');
                    console.error("ERROR: ", error);
                    console.error("ERROR responseText: ", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
                console.error(error.responseText);
            });
            // }, Math.round(30000+(Math.random()*15000)));
        },
        async updateViewToken(medium) {
            // console.log("TCL: updateViewToken(medium)", medium);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + medium.id + '/viewToken',
                    type: "GET",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    // console.log("TCL: updateViewToken -> data: ", data);
                    resolve(data);
                }).fail(function (error) {
                    console.error("ERROR: ", error);
                    console.error("ERROR responseText: ", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
                console.error(error.responseText);
            });
        },

        async updateTitle(title) {
            // console.log("TCL: MediumService: async updateTitle -> title", title);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/title/" + title.id,
                    type: "PATCH",
                    data: JSON.stringify(title),
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (updateData) {
                    // console.log("TCL: async updateTitle -> updateData", updateData);
                    resolve(updateData);
                }).fail(function (error) {
                    console.error("ERROR: ", error);
                    console.error("ERROR responseText:", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async updateSource(source) {
            // console.log("TCL: async updateSource -> source", source);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/source/" + source.id,
                    type: "PATCH",
                    data: JSON.stringify(source),
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (updateData) {
                    // console.log("TCL: async updateSource -> updateData", updateData);
                    resolve(updateData);
                }).fail(function (error) {
                    console.error("ERROR: ", error);
                    console.error("ERROR responseText:", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async removeMedium(medium) {
            // console.log("TCL: removeMedium -> medium", medium);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + medium.model.id,
                    type: "DELETE",
                    contentType: "application/json; charset=utf-8",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    resolve(data);
                })
                    .fail(function (error) {
                        console.error("ERROR: ", error);
                        console.error("ERROR responseText:", error.responseText);
                    });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        removeTitle(title) {
            // console.log("TCL: removeTitle -> title ", title);
            $.ajax({
                url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/title/" + title.id,
                type: "DELETE",
                contentType: "application/json; charset=utf-8",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                },
            }).done(function (data) {
            })
                .fail(function (error) {
                    console.error("ERROR: ", error);
                    console.error("ERROR responseText:", error.responseText);
                });
        },

        async removeLanguageTrack(track) {
            // console.log("TCL: removeLanguageTrack -> track ", track);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + track.id.mediumId + "/languageTrack/" + track.id.mediumLanguageTypeId + "/" + track.id.languageId,
                    type: "DELETE",
                    contentType: "application/json; charset=utf-8",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    // console.log("TCL: removed language track from medium_has_language.");
                    resolve(data);
                }).fail(function (error) {
                    console.error("ERROR: ", error);
                    console.error("ERROR responseText:", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async removeActorFromMediumHasActorWithRoles(mediumId, actorId) {
            // console.log("TCL: removeActorFromMediumHasActorWithRoles -> mediumId, actorId", mediumId, actorId);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + mediumId + "/hasActor/" + actorId,
                    type: "DELETE",
                    contentType: "application/json; charset=utf-8",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    resolve(data);
                }).fail(function (error) {
                    console.error("ERROR: ", error);
                    console.error("ERROR responseText:", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async removeRoleFromMediumHasActorWithRoles(mediumId, actorId, roleId) {
            // console.log("TCL: removeRoleFromMediumHasActorWithRoles -> mediumId, actorId, roleId", mediumId, actorId, roleId);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + mediumId + "/hasActor/" + actorId + "/withRole/" + roleId,
                    type: "DELETE",
                    contentType: "application/json; charset=utf-8",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    resolve(data);
                }).fail(function (error) {
                    console.error("ERROR: ", error);
                    console.error("ERROR responseText:", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async removeTag(mediumId, tagId) {
            // console.log("TCL: removeTag -> mediumId, tagName", mediumId, tagName);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + mediumId + "/tag/" + tagId,
                    type: "DELETE",
                    contentType: "application/json; charset=utf-8",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    resolve(data);
                })
                    .fail(function (error) {
                        console.error("ERROR: ", error);
                        console.error("ERROR responseText:", error.responseText);
                    });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async createAudioPostProduction() {
            // console.log("TCL: createAudioPostProduction");
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/audioPostProduction/0",
                    type: "POST",
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    // console.log("TCL: createAudioPostProduction -> data", data);
                    resolve(data);
                }).fail(function (error) {
                    console.error("ERROR: ", error);
                    console.error("ERROR responseText:", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async deleteAudioPostProduction(id) {
            // console.log("TCL: deleteAudioPostProduction -> id", id);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/audioPostProduction/" + id,
                    type: "DELETE",
                    contentType: "application/json; charset=utf-8",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    // console.log("TCL: deleteAudioPostProduction -> data", data);
                    resolve(data);
                }).fail(function (error) {
                    console.error("ERROR: ", error);
                    console.error("ERROR responseText:", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async createAudioPostProductionTranslation(model) {
            // console.log("TCL: createAudioPostProductionTranslation -> model", model);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/audioPostProduction/" + model.audioPostProduction.id + "/translation",
                    type: "POST",
                    data: JSON.stringify(model),
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    // console.log("TCL: createAudioPostProductionTranslation -> data", data);
                    resolve(data);
                }).fail(function (error) {
                    console.error("ERROR: ", error);
                    console.error("ERROR responseText:", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },

        async updateAudioPostProductionTranslation(model) {
            // console.log("TCL: updateAudioPostProductionTranslation -> model", model);
            return new Promise(resolve => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/audioPostProduction/" + model.id + "/translation",
                    type: "PATCH",
                    data: JSON.stringify(model),
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    // console.log("TCL: createAudioPostProductionTranslation -> data", data);
                    resolve(data);
                }).fail(function (error) {
                    console.error("ERROR: ", error);
                    console.error("ERROR responseText:", error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        },
        async createMediumTranscription(mediumId, engineIdentifier, modelIdentifier) {
            const createTranscriptionRequestPayload = {
                engineIdentifier,
                modelIdentifier
            }
            return new Promise((resolve, reject) => {
                $.ajax({
                    url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/" + mediumId + "/transcriptions",
                    type: "POST",
                    data: JSON.stringify(createTranscriptionRequestPayload),
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
                    },
                }).done(function (data) {
                    console.log(data)
                    resolve(data);
                }).fail(function (error) {
                    console.error("ERROR: ", error);
                    console.error("ERROR responseText:", error.responseText);
                    reject(error.responseText);
                });
            }).catch((error) => {
                console.error("ERROR: ", error);
            });
        }
    }
}, window));
