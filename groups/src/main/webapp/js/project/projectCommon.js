define([], function () {
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
