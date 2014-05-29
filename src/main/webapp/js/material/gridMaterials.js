
require(['jquery'], function() {


    /*
     
     listMaterialsProducts.listPurchasedMatsUrl = "${helper.urlFor('OrgMaterialJsonModule','listPurchasedMats',[org.id])}";
     listMaterialsProducts.newOrgMatUrl = "${helper.urlFor('material.ProductMaterialJsonModule','newProdLink',[org.id])}";
     listMaterialsProducts.deleteOrgMatLinkUrl = "${helper.urlFor('material.ProductMaterialJsonModule','deleteLink',[org.id])}";
     listMaterialsProducts.contextPath = "${contextPath}";
     listMaterialsProducts.sLang = "Search purchased materials";
     
     listMaterialsOrgMats.listOrgMatsUrl = "${helper.urlFor('OrgMaterialJsonModule','listOrgMats',[org.id])}";
     listMaterialsOrgMats.listPurchasedMatsUrl = "${helper.urlFor('OrgMaterialJsonModule','listPurchasedMats',[org.id])}";
     listMaterialsOrgMats.createOrgMatUrl =  "${helper.urlFor('CpMainModule','newMaterial',[org.id])}";
     listMaterialsOrgMats.editOrgMatUrl = "${helper.urlFor('material.MaterialModule','edit',[org.id])}";
     listMaterialsOrgMats.deleteOrgMatLinkUrl = "${helper.urlFor('OrgMaterialJsonModule','deleteOrgMatLink')}";
     listMaterialsOrgMats.deleteOrgMatUrl = "${helper.urlFor('OrgMaterialJsonModule','deleteOrgMat')}"
     listMaterialsOrgMats.contextPath = "${contextPath}";
     listMaterialsOrgMats.sLang = "Search added materials";
     
     */


    var orgMats;
    var purchMats;

    require(['handlebars'], function() {


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
        log('clicked grid-entry ' , this );
        $(this).toggleClass('hover');
    });
    
    
    $('#materials-grid').on('click', '.info-hover' , function() {
        $(this).parent().removeClass('hover');
    });
    

});