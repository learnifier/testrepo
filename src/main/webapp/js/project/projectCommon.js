/* global cocobox */

define(['dabox-common'], function () {

    $("#deletePrjBtn").click(function() {
        var btn = this;
        cocobox.confirmationDialog("Delete project?", "Do you want to delete this project?", function() {
            //Do delete here

            var url = $(btn).parents('form').first().prop("action");

            var dlg = cocobox.longOp();

            var op = $.ajax({
                type: "POST",
                url: url,
                data: {}
            });

            op.fail(dlg.abort)
                    .fail(cocobox.internal.ajaxError)
                    .done(function (data) {
                        if (data.location) {
                            window.location = data.location;
                        } else {
                            dlg.abort();
                            cocobox.infoDialog('Delete project', 'You need to delete all participants to be able to delete the project.');
                        }
                    });


        });
    });

    $('#myAffix').affix({
        offset: {
            top: 346,
            bottom: function () {
                return (this.bottom = $('.footer').outerHeight(true));
            }
        }
    });

    $(function () {

        $(".header-icon-button").click(function () {

            if ($('#pagewrapper').hasClass("sidebar-maximized"))
            {
                $("#myAffix").removeClass("moveAffix");
            }
            else
            {
                $("#myAffix").addClass("moveAffix");

            }

        });
        $(window).scroll(function () {
            if ($('#pagewrapper').hasClass("sidebar-maximized"))
            {
                $("#myAffix").addClass("moveAffix");
            }
            else
            {

                $("#myAffix").removeClass("moveAffix");
            }

            //Remove affix if tablet/phone on page load
        if ($(window).width() < 768) {
            $('#myAffix').removeClass('affix-top');
            $('#myAffix').removeClass('affix');
            $('#myAffix').removeClass('moveAffix');
        }

        });

        $( window ).resize(function() {
         //Remove affix if tablet/phone
            if ($(window).width() < 768) {
                $('#myAffix').removeClass('affix-top');
                $('#myAffix').removeClass('affix');
                $('#myAffix').removeClass('moveAffix');
            }
        });

    });

});
