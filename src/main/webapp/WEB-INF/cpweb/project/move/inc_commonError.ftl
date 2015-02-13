[#ftl strip_text="true" /]


[#function errorMessage]
    [#if result.moveError.type == "same_project"]
    [#return "You selected the same project" /]
    [#elseif result.moveError.type == "invalid_source_project"]
    [#return "There is an error related to the project that the participant belongs to" /]
    [#elseif result.moveError.type == "invalid_target_project"]
    [#return "There is an error related to the selected target project" /]
    [#elseif result.moveError.type == "product_constraint"]
    [#return "It is not possible to move the participant due to one or more products having progress that can't be moved" /]
    [#elseif result.moveError.type == "participation_error"]
    [#return "There is a problem with the selected participant that blocks the move operation" /]
    [#elseif result.moveError.type == "invalid_participation"]
    [#return "There is a problem with the selected participant that blocks the move operation" /]
    [#else]
    [#return "An unknown error occured: " +  result.moveError.cause!result.moveError.type /]
    [/#if]
[/#function]


[#macro productErrors]
<ul class="list-group">
    [#list result.productErrors as productError]
        [#assign peMaterial = materials[productError.productId] /]
        <li class="list-group-item">${peMaterial.title?xml} [@common.materialProductId material=peMaterial suffix=") " /]- ${productErrorMessage(productError)?xml} </li>
    [/#list]
</ul>
[/#macro]

[#function productErrorMessage productError]
    [#local type = productError.type /]
    [#local integrationMsg = productError.message!"" /]
    [#if type == "creditAllocationDenied"]
    [#return "Insufficient credits available" /]
    [#elseif type == "creditDeallocationDenied"]
    [#return "Unable to release credits" /]
    [#elseif type == "integrationDeleteDenied"]
    [#return "Cannot move learner due to recorded progress" /]
    [#elseif type == "integrationMoveDenied"]
    [#return "Cannot move learner. " + integrationMsg /]
    [#elseif type == "integrationAddDenied"]
    [#return "Not possible to add this product" /]
    [#elseif type == "productMissing"]
    [#return "Product not found" /]
    [#elseif type == "rollback"]
    [#return "Move failed but rollback unsuccessful as well" /]
    [#else]
    [#return "Unknown error " + type /]
    [/#if]
[/#function]


