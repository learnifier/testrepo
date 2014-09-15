

require(['jquery'], function() {
   
    var orgMats;
    var purchMats;

    require(['handlebars'], function(Handlebars) {

        Handlebars.registerHelper('if_eq', function(a, b, opts) {
            if(a == b) 
                return opts.fn(this);
            else
                return opts.inverse(this);
        });
      
        Handlebars.registerPartial('video', $('#video-partial').html());
        Handlebars.registerPartial('audio', $('#audio-partial').html());
        Handlebars.registerPartial('file', $('#file-partial').html());
        Handlebars.registerPartial('general', $('#general-partial').html());
      
        $.when(
            $.ajax({
                url: listOrgMatsUrl,
                success: function(data) {
                    orgMats = data;

                    $.each(orgMats.aaData, function(index, object) {
                        object.materialType = 'added';
                    });
                }
            }),
            
            $.ajax({
                url: listPurchasedMatsUrl,
                success: function(data) {
                    purchMats = data;


                    $.each(purchMats.aaData, function(index, object) {
                        object.materialType = 'purchased';
                    });

                }
            })

        ).then(function() {
            log('purchMats ', purchMats);
            log('orgMats', orgMats);

            var source = $('#grid-entry-template').html();

            var template = Handlebars.compile(source);

            var allMats = purchMats;

            $.each(orgMats.aaData, function(index, object) {
                allMats.aaData.push(object);
            });

            var context = allMats;

            var output = template(context);

            $('#materials-grid').html(output);

            initIsotope();
        });

    });



    var initIsotope = function() {

        require(['jquery', 'isotope', 'jquery.bridget'], function($, Isotope) {

            $.bridget('isotope', Isotope);

            var filterFns = {
                all: function() {
                    return true;
                },
                added: function() {
                    var materialType = $(this).attr('data-materialtype');

                    return materialType == 'added';

                },
                purchased: function() {

                    var materialType = $(this).attr('data-materialtype');

                    return materialType == 'purchased';
                },
                video: function() {
                    var videofilter = $(this).attr('data-type');
                    return videofilter == 'vimeo2video' || videofilter == 'vimeovideo' || videofilter == 'brightcovevideo';
                },
                audio: function() {
                    var audiofilter = $(this).attr('data-type');
                    return audiofilter == 'audioplayer';
                },
                docs: function() {
                    var docsfilter = $(this).attr('data-type');
                    return docsfilter == 'pdf' || docsfilter == 'genericfile';
                }
            };

            var $iso = $('#materials-grid').isotope({
                itemSelector: '.grid-entry',
                layoutMode: 'masonry'
            });

            $('#masonry').click(function() {
                $iso.isotope({'layoutMode': 'masonry'});

            });

            $('#fitrows').click(function() {
                $iso.isotope({'layoutMode': 'fitRows'});

            });

            $('.filter', '#materials-grid-filter').click(function() {

                var layout;

                if ($(this).attr('data-filter') == 'all') {
                    layout = 'masonry';
                } else {
                    layout = 'fitRows';
                }


                var filterValue = filterFns[$(this).attr('data-filter')];
                $iso.isotope({
                    filter: filterValue,
                    layoutMode: layout
                });
            });


        });

    };
    
    $('#materials-grid ').on('click', '.body' , function() {
        $(this).toggleClass('hover');
    });
    
    $('#materials-grid ').on('click', '.settings' , function() {
        $(this).find('.settings-actions').toggle();
    });
    
    $('#materials-grid').on('mouseleave', '.body', function() {
       $(this).find('.settings-actions').hide(); 
    });
    
    
    
});