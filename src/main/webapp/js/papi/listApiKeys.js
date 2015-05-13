/* 
 * (c) Dabox AB 2015 All Rights Reserved
 */
define([], function () {
    "use strict";
    var exports = {};

    require(['handlebars', 'dabox-common'], function (Handlebars) {

        $.getJSON(apiWebUrlSource).done(function (data) {

            var source = $('#settings-template').html();
            var template = Handlebars.compile(source);
            var context = data;

            $('#list-settings').html(template(context));
        }).fail(cocobox.internal.ajaxErrorHandler);


        $("#list-settings").on('click', '.delBtnPos', function () {

            var clickedElement = $(this);
            var element = $(this).data('id');

            cocobox.confirmationDialog("Delete material",
                    "Do you want to permanently delete this key?",
                    function () {
                        cocobox.ajaxPost(deleteKeyUrl + '/' + element);
                        $(clickedElement).parent().remove();
                    }
            );
        });

        $("#list-settings").on('click', '.showBtnPos', function () {

            var element = $(this).data('id');

            $.getJSON(showKeyUrl + '?publicKey=' + element).done(function (data) {

                var secretApiKey = data.secretKey;

                cocobox.infoDialog("Secret API KEY",
                        "Your secret key is: " + secretApiKey
                        );

            }).fail(cocobox.internal.ajaxErrorHandler);

        });

        $(".clearALl").on('click', '.addBtn', function () {

            $.getJSON(createKeyUrl).done(function (data) {

                cocobox.infoDialog("A new key has been created",
                        "Your PUBLIC key is: " + data.publicKey + "<br/>" + "Your SECRET key is: " + data.secretKey);


                $.getJSON(apiWebUrlSource).done(function (data) {

                    var source = $('#settings-template').html();
                    var template = Handlebars.compile(source);
                    var context = data;

                    $('#list-settings').html(template(context));
                }).fail(cocobox.internal.ajaxErrorHandler);


            }).fail(cocobox.internal.ajaxErrorHandler);

        });


    });

    return exports;
});

