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

  TIMAAT.Settings = {

    init: function () {
      this.initSettings();
      TIMAAT.UI.displayComponent('settings', 'settingsBugfixesTab', null);
    },

    initSettingsComponent: function () {
      TIMAAT.UI.showComponent('settings');
      $('#settingsAccountCreationTab').trigger('click');
    },

    initSettings: function () {
      // nav-bar functionality
      $('#settingsBugfixesTab').on('click', function (event) {
        TIMAAT.Settings.loadSettingsBugfixes();
        TIMAAT.UI.displayComponent('settings', 'settingsBugfixesTab', 'settingsBugfixes');
        TIMAAT.URLHistory.setURL(null, 'Settings', '#settings/bugfixes');
      });

      $('#settingsAccountCreationTab').on('click', function (event) {
        TIMAAT.Settings.loadSettingsAccountCreation();
        TIMAAT.UI.displayComponent('settings', 'settingsAccountCreationTab', 'settingsAccountCreation');
        TIMAAT.URLHistory.setURL(null, 'Settings', '#settings/accountCreation');
      });

      $('#settingsTranscriptionsTab').on('click', function (event) {
        TIMAAT.Settings.loadSettingsAccountCreation();
        TIMAAT.UI.displayComponent('settings', 'settingsTranscriptionsTab', 'settingsTranscriptions');
        TIMAAT.URLHistory.setURL(null, 'Settings', '#settings/transcriptions');
      });


      $('#fixDurationButton').on('click', function (event) {
        if (TIMAAT.Service.session.displayName == "admin") {
          // console.log("length fix button clicked");
          TIMAAT.Settings.fixLength();
        }
      });

      $('#fixNoPermissionSetButton').on('click', function (event) {
        if (TIMAAT.Service.session.displayName == "admin") {
          // console.log("add missing permissions");
          TIMAAT.Settings.fixPermissions();
        }
      });

      $('#fixKeyframeTimeSecondsToMillisecondsButton').on('click', function (event) {
        if (TIMAAT.Service.session.displayName == "admin") {
          // console.log("fix annotation keyframe timestamps from s to ms");
          TIMAAT.Settings.fixKeyframeTimes();
        }
      });

      $('#createRandomPassword').on('click', function (event) {
        let randomPasswordString = '';
        let passwordCharacters = '1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
        let passwordCharactersLength = passwordCharacters.length;
        let i = 0;
        for (; i < 20; i++) {  // generated random password is of length 20
          randomPasswordString += passwordCharacters.charAt(Math.floor(Math.random() * passwordCharactersLength));
        }
        $('#newAccountPassword').val(randomPasswordString);
      });

      $('#createAccountSubmitButton').on('click', async function (event) {
        event.preventDefault();
        if (TIMAAT.Service.session.displayName != "admin") return;
        if (!$('#createNewAccountForm').valid()) return;
        let newAccountUsername = $('#newAccountUsername').val();
        let newAccountDisplayName = $('#newAccountDisplayName').val();
        let newAccountPassword = $('#newAccountPassword').val();
        let isLoginNameInUse = await TIMAAT.Settings.isLoginNameInUse(newAccountUsername);
        if (isLoginNameInUse) {
          $('#newAccountUsername').addClass('is-invalid');
          $('#duplicateUsername').addClass('invalid-feedback');
          $('#duplicateUsername').html('This name already exists. Please choose another.');
          $('#duplicateUsername').show();
          return;
        }
        let isDisplayNameInUse = await TIMAAT.Settings.isDisplayNameInUse(newAccountDisplayName);
        if (isDisplayNameInUse) {
          $('#newAccountDisplayName').addClass('is-invalid');
          $('#duplicateDisplayName').addClass('invalid-feedback');
          $('#duplicateDisplayName').html('This name already exists. Please choose another.');
          $('#duplicateDisplayName').show();
          return;
        }
        let newAccountPasswordHash = TIMAAT.Util.getArgonHash(newAccountPassword, newAccountUsername + TIMAAT.Service.clientSalt);
        let newUserPasswordDataSet = {
          id: 0,
          userPasswordHashType: {
            id: 1
          },
          salt: 'salzigessalt',
          keyStretchingIterations: 8,
          stretchedHashEncrypted: newAccountPasswordHash
        };
        let userPasswordId = await TIMAAT.Service.createUserPassword(newUserPasswordDataSet);
        let userAccountDataSet = {
          id: 0,
          userPassword: {
            id: userPasswordId
          },
          userAccountStatus: 'active',
          accountName: newAccountUsername,
          displayName: newAccountDisplayName,
          createdAt: Date.now(),
          recoveryEmailEncrypted: 'foo@bar.de',
          contentAccessRights: null,
          userSettingsWebInterface: null
        };
        await TIMAAT.Service.createUserAccount(userAccountDataSet);
        $('#createNewAccountForm').trigger('reset');
        let modal = $('#accountCreatedModal');
        modal.find('.modal-body').html(`
          Login details for <b>` + newAccountDisplayName + `</b>:
          <br>
          <br>
          <b>Username:</b> ` + newAccountUsername + `<br>
          <b>Password:</b> ` + newAccountPassword + `<br>
          <hr>
          Please send these credentials to the corresponding person and urge them to change the initial password as soon as they log in for the first time.
          <br>
          <div class="alert-danger"><b>ATTENTION: Once you close this window, you cannot retrieve the password anymore. Make sure you have stored the credentials before closing!</b>
          </div>
        `);
        modal.modal({backdrop: 'static', keyboard: false}, 'show');

      });

      $('#accountCreatedModal').on('hidden.bs.modal', function (event) {
        $('#createNewAccountForm').trigger('reset');
        $('#createRandomPassword').trigger('click');
      });

      $('#newAccountUsername').on('change input', function (event) {
        $('#duplicateUsername').hide();
        $('#duplicateUsername').removeClass('invalid-feedback');
        $('#newAccountUsername').removeClass('is-invalid');
      });

      $('#newAccountDisplayName').on('change input', function (event) {
        $('#duplicateDisplayName').hide();
        $('#duplicateDisplayName').removeClass('invalid-feedback');
        $('#newAccountDisplayName').removeClass('is-invalid');
      });
    },

    loadSettingsBugfixes: function () {
      // TIMAAT.UI.displayComponent('settings', 'settingsBugfixesTab', null);
      // TIMAAT.UI.addSelectedClassToSelectedItem('settings', null);
      // TIMAAT.UI.subNavTab = 'dataSheet';
    },

    loadSettingsAccountCreation: function () {
      $('#createRandomPassword').trigger('click');
    },

    isLoginNameInUse: async function (loginName) {
      let isInUse = await TIMAAT.Service.loginNameExists(loginName);
      return isInUse;
    },

    isDisplayNameInUse: async function (displayName) {
      let isInUse = await TIMAAT.Service.displayNameExists(displayName);
      return isInUse;
    },

    fixLength: async function () {
      return new Promise(resolve => {
        $.ajax({
          url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/medium/fileLengthFix" + '?authToken=' + TIMAAT.Service.session.token,
          type: "PATCH",
          contentType: "application/json; charset=utf-8",
          beforeSend: function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
        }).done(function (data) {
          resolve(data);
        }).fail(function (error) {
          console.error("ERROR: ", error);
          console.error("ERROR responseText: ", error.responseText);
        });
      }).catch((error) => {
        console.error("ERROR: ", error);
      });
    },

    fixPermissions: async function () {
      // console.log("fix permissions");
      return new Promise(resolve => {
        $.ajax({
          url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/user/permissionFix" + '?authToken=' + TIMAAT.Service.session.token,
          type: "PATCH",
          contentType: "application/json; charset=utf-8",
          beforeSend: function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
        }).done(function (data) {
          resolve(data);
        }).fail(function (error) {
          console.error("ERROR: ", error);
          console.error("ERROR responseText: ", error.responseText);
        });
      }).catch((error) => {
        console.error("ERROR: ", error);
      });
    },

    fixAnnotationUUIDs: async function () {
      // console.log("fix annotation uuids");
      return new Promise(resolve => {
        $.ajax({
          url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/annotation/uuidFix" + '?authToken=' + TIMAAT.Service.session.token,
          type: "PATCH",
          contentType: "application/json; charset=utf-8",
          beforeSend: function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
        }).done(function (data) {
          resolve(data);
        }).fail(function (error) {
          console.error("ERROR: ", error);
          console.error("ERROR responseText: ", error.responseText);
        });
      }).catch((error) => {
        console.error("ERROR: ", error);
      });
    },

    fixKeyframeTimes: async function () {
      // console.log("fix keyframe timestamps");
      let selectorSvgList = await this.getSelectorSvgData();
      // console.log("selectorSvgList", selectorSvgList);
      let svgDataList = [];
      selectorSvgList.forEach(function (selectorSvg) {
        svgDataList.push(JSON.parse(selectorSvg.svgData));
      });
      // console.log("svgDataList: ", svgDataList);
      let i = selectorSvgList.length - 1;
      for (; i >= 0; i--) {
        if (svgDataList[i].keyframes && svgDataList[i].keyframes.length > 1) { // fix time data s -> ms
          svgDataList[i].keyframes.forEach(function (keyframe) {
            // console.log("TCL: fix value from ", keyframe.time);
            if (keyframe.time < 50) {
              keyframe.time *= 1000;
            }
            keyframe.time = Math.floor(keyframe.time);
            // console.log("TCL: to ", keyframe.time);
          });
        } else { // reduce list to entries which have animation data
          selectorSvgList.splice(i, 1);
          svgDataList.splice(i, 1);
        }
      }
      // console.log("svgDataList: ", svgDataList);
      i = 0;
      for (; i < selectorSvgList.length; i++) {
        selectorSvgList[i].svgData = JSON.stringify(svgDataList[i]);

      }
      // console.log("selectorSvgList", selectorSvgList);
      await this.setFixedSelectorSvgData(selectorSvgList);
    },

    getSelectorSvgData: async function () {
      return new Promise(resolve => {
        $.ajax({
          url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/analysisList/keyframeFix" + '?authToken=' + TIMAAT.Service.session.token,
          type: "GET",
          dataType: 'json',
          contentType: "application/json; charset=utf-8",
          beforeSend: function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
        }).done(function (data) {
          resolve(data);
        }).fail(function (error) {
          console.error("ERROR: ", error);
          console.error("ERROR responseText: ", error.responseText);
        });
      }).catch((error) => {
        console.error("ERROR: ", error);
      });
    },

    setFixedSelectorSvgData: async function (model) {
      return new Promise(resolve => {
        $.ajax({
          url: window.location.protocol + '//' + window.location.host + "/TIMAAT/api/analysisList/keyframeFix" + '?authToken=' + TIMAAT.Service.session.token,
          type: "PATCH",
          data: JSON.stringify(model),
          contentType: "application/json; charset=utf-8",
          beforeSend: function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + TIMAAT.Service.token);
          },
        }).done(function (data) {
          resolve(data);
        }).fail(function (error) {
          console.error("ERROR: ", error);
          console.error("ERROR responseText: ", error.responseText);
        });
      }).catch((error) => {
        console.error("ERROR: ", error);
      });
    },

  }

}, window));
