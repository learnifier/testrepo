define([ccbPage.googleMapsEnabled ? 'async!//maps.googleapis.com/maps/api/js?v=3.exp&libraries=places' : 'jquery',
        'jquery.validate'], function () {
    "use strict";

    $(document).ready(function () {
        function initialize(elementId) {

            var input = document.getElementById(elementId);

            var autocomplete = new google.maps.places.Autocomplete(input);

            var mapsUrl;

            google.maps.event.addListener(autocomplete, 'place_changed', function () {
                var place = autocomplete.getPlace();
                if (!place.geometry) {
                    return;
                }

                //log('place', place);
                mapsUrl = createMapsUrl(place);

                console.log(mapsUrl);
                //log($(elementId).closest('.details').find('.locUrl input'));

                $('#' + elementId).next('label.error').remove();

                $('#' + elementId).closest('.details').find('.locUrl input').val(mapsUrl);

            });

        }

        function createMapsUrl(place) {

            var longLat;
            if(place.geometry.location) {
                longLat = place.geometry.location.toUrlValue();
            }

            var mapsBaseUrl = 'http://www.google.com/maps/place/',
            mapsUrl = mapsBaseUrl + encodeURIComponent(place.name) + '/@' + longLat + ',17z';
            return mapsUrl;

        }


        if (ccbPage.googleMapsEnabled) {
            $('input[type=locUrlExtra]').focus(function () {
                initialize(this.id);
            });

            $('input[type=locUrlExtra]').blur(function () {

                if ($(this).val() == '') {
                    $(this).closest('.details').find('.locUrl input').val('');
                }
            });

            $('input[type=locUrlExtra]').keydown(function (event) {
                if (event.keyCode == 13) {
                    event.preventDefault();
                    return false;
                }
            });
        } else {
            $('input[type=locUrl]').change(function() {
                $(this).closest('.details').find('.locUrlExtra input').val($(this).val());
            })
        }

    });


    $.validator.addMethod("place", function (value, element) {
        //log('element is',  element);
        return (!(($(element).closest('.details').find('.locUrlExtra input').val().length > 0) && ($(element).closest('.details').find('.locUrl input').val().length == 0)));

    }, "Please, choose a Google Place.");




    var validator = $("form[name=createProjectGeneral]").validate({
        errorClass: "error bg-danger",
        errorPlacement: function (error, element) {
            if (element.hasClass('hasDatepicker')) {
                element.closest("li").find('span').html(error);
            } else {
                error.insertAfter(element);
            }
        }
    });

    $('input[type=locUrlExtra]').rules('add', {
        place: true,
        required: false
    });

    validator.element("input[type=locUrlExtra]");

    require(['cocobox-datetime'], function (dt) {
        $(function () {

            var linkFn = function () {
                $("input[data-ccbfieldname=starts]").each(function () {
                    var name = $(this).attr("id");
                    var newName = name.replace(/starts$/, "ends");

                    dt.linkToField("#" + name, "#" + newName);
                });
            };

            linkFn();

            $(window).on("dwsfu:validation:onready", linkFn);
            $(window).on("dwsfu:validation:js", linkFn);

        });
    });

});