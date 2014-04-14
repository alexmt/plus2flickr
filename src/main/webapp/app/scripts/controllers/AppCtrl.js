'use strict';

angular.module('controllers')
  .controller('AppCtrl', ['$scope', '$rootScope', '$location', '$window', 'App', 'Google', 'User',
    function ($scope, $rootScope, $location, $window, App, Google, User) {

      $scope.signInToFlickr = function () {
        $window.location.href = 'services/user/flickr/authorize';
      };

      $scope.signInToGoogle = function () {
        App.googleAppSettings(function (settings) {
          Google.authorize(settings).then(function (code) {
            User.authorizeGoogleAccount({ code: code }, function (response) {
              $scope.userInfo = response.data;
              $location.path('/home/google/albums');
            });
          });
        });
      };

      User.info(function(userInfo) {
        $scope.userInfo = userInfo;
        if (userInfo && userInfo.isAnonymous) {
          $location.path("/login");
        }
      });
    }]);