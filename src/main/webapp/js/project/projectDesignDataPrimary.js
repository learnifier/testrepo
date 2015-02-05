define(['async!//maps.googleapis.com/maps/api/js?v=3.exp&sensor=false&libraries=places', 'jquery.validate'], function () {
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

                //log(elementId);
                //log(mapsUrl);
                //log($(elementId).closest('.details').find('.locUrl input'));

                $('#' + elementId).next('label.error').remove();

                $('#' + elementId).closest('.details').find('.locUrl input').val(mapsUrl);

            });

        }

        function createMapsUrl(place) {

            var street, number, town = '';

            if (place.address_components) {
                street = (place.address_components[1] && place.address_components[1].short_name || '');
                number = (place.address_components[0] && place.address_components[0].short_name || '');
                town = (place.address_components[2] && place.address_components[2].short_name || '');
            }

            var long, lat;
            if (place.geometry.location) {
                lat = (place.geometry.location.D).toString().substring(0, 9);
                long = (place.geometry.location.k).toString().substring(0, 9);
            }

            var mapsBaseUrl = 'http://www.google.com/maps/place/',
                    mapsUrl = mapsBaseUrl + encodeURIComponent(street) + ',' + encodeURIComponent(number) + ',' + encodeURIComponent(town) + '/@' + long + ',' + lat + ',17z';
            return mapsUrl;

        }


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

});