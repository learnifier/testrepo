define([], function () {
    $('#myAffix').affix({
        offset: {
            top: 341,
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

        });



    });

});
