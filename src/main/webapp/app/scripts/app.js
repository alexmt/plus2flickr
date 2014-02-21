'use strict';

angular.module('webApp', [
    'controllers',
    'ngRoute',
    'directive.google-plus-signin'
  ]).config(function ($routeProvider) {
    $routeProvider
      .when('/', {
        templateUrl: 'views/main.html',
        controller: 'MainCtrl'
      })
      .otherwise({
        redirectTo: '/'
      });
  });