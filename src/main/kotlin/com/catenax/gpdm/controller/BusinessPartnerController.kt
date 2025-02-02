package com.catenax.gpdm.controller

import com.catenax.gpdm.component.elastic.SearchService
import com.catenax.gpdm.component.elastic.impl.doc.SuggestionType
import com.catenax.gpdm.config.BpnConfigProperties
import com.catenax.gpdm.dto.request.*
import com.catenax.gpdm.dto.response.BusinessPartnerResponse
import com.catenax.gpdm.dto.response.BusinessPartnerSearchResponse
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.dto.response.SuggestionResponse
import com.catenax.gpdm.service.BusinessPartnerBuildService
import com.catenax.gpdm.service.BusinessPartnerFetchService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springdoc.api.annotations.ParameterObject
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/catena/business-partner")
class BusinessPartnerController(
    val businessPartnerFetchService: BusinessPartnerFetchService,
    val businessPartnerBuildService: BusinessPartnerBuildService,
    val searchService: SearchService,
    val bpnConfigProperties: BpnConfigProperties
) {

    @Operation(summary = "Get page of business partners matching the search criteria",
    description = "This endpoint tries to find matches among all existing business partners, " +
            "filtering out partners which entirely do not match and ranking the remaining partners according to the accuracy of the match. " +
            "The match of a partner is better the higher its relevancy score.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Page of business partners matching the search criteria, may be empty"),
        ApiResponse(responseCode = "400", description = "On malformed search or pagination request", content = [Content()])
    ])
    @GetMapping
    fun getBusinessPartners(
        @ParameterObject bpSearchRequest: BusinessPartnerPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageResponse<BusinessPartnerSearchResponse> {
        return searchService.searchBusinessPartners(
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest),
            paginationRequest
        )
    }

    @Operation(summary = "Get business partner by identifier",
    description = "This endpoint tries to find a business partner by the specified identifier. " +
            "The identifier value is case insensitively compared but needs to be given exactly. " +
            "By default the value given is interpreted as a BPN. " +
            "By specifying the technical key of another identifier type" +
            "the value is matched against the identifiers of that given type.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Found business partner with specified identifier"),
        ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
        ApiResponse(responseCode = "404", description = "No business partner found under specified identifier or specified identifier type not found", content = [Content()])
    ])
    @GetMapping("/{idValue}")
    fun getBusinessPartner(
        @Parameter(description = "Identifier value")@PathVariable idValue: String,
        @Parameter(description = "Type of identifier to use, defaults to BPN when omitted", schema = Schema(defaultValue = "BPN"))
        @RequestParam
        idType: String?
    ): BusinessPartnerResponse {
        val actualType = idType ?: bpnConfigProperties.id
        return if (actualType == bpnConfigProperties.id) businessPartnerFetchService.findPartner(idValue)
        else businessPartnerFetchService.findPartnerByIdentifier(actualType, idValue)
    }

    @Operation(summary = "Create new business partner record",
    description = "Endpoint to create new business partner records directly in the system. Currently for test purposes only.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "New business partner record successfully created"),
        ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Metadata referenced by technical key not found", content = [Content()])
    ])
    @PostMapping
    fun createBusinessPartners(
        @RequestBody
        businessPartners: Collection<BusinessPartnerRequest>): Collection<BusinessPartnerResponse> {
        return businessPartnerBuildService.upsertBusinessPartners(businessPartners)
    }

    @Operation(
        summary = "Find best matches for given text in business partner names",
        description = "Performs search on business partner names in order to find the best matches for the given text. " +
                "By specifying further request parameters the set of business partners to search in can be restricted. " +
                "If no text is given, the endpoint lists possible names in the search set.",
        responses = [ApiResponse(responseCode = "200", description = "Best matches found, may be empty")]
    )
    @GetMapping("/name")
    fun getNameSuggestion(
        @Parameter(description = "Show names best matching this text") text: String?,
        @ParameterObject bpSearchRequest: BusinessPartnerPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        return searchService.getSuggestion(
            SuggestionType.NAME,
            text,
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest),
            pageRequest
        )
    }

    @Operation(
        summary = "Find best matches for given text in business partner legal forms",
        description = "Performs search on legal form names in order to find the best matches for the given text. " +
                "By specifying further request parameters the set of business partners to search in can be restricted. " +
                "If no text is given, the endpoint lists possible legal form names in the search set.",
        responses = [ApiResponse(responseCode = "200", description = "Best matches found, may be empty")]
    )
    @GetMapping("/legal-form")
    fun getLegalFormSuggestion(
        @Parameter(description = "Show legal form names best matching this text") text: String?,
        @ParameterObject bpSearchRequest: BusinessPartnerPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        return searchService.getSuggestion(
            SuggestionType.LEGAL_FORM,
            text,
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest),
            pageRequest
        )
    }

    @Operation(
        summary = "Find best matches for given text in business statuses",
        description = "Performs search on business status denotations in order to find the best matches for the given text. " +
                "By specifying further request parameters the set of business partners to search in can be restricted. " +
                "If no text is given, the endpoint lists possible business status denotations in the search set.",
        responses = [ApiResponse(responseCode = "200", description = "Best matches found, may be empty")]
    )
    @GetMapping("/status")
    fun getStatusSuggestion(
        @Parameter(description = "Show business status denotations best matching this text") text: String?,
        @ParameterObject bpSearchRequest: BusinessPartnerPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        return searchService.getSuggestion(
            SuggestionType.STATUS,
            text,
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest),
            pageRequest
        )
    }

    @Operation(
        summary = "Find best matches for given text in business partner classifications",
        description = "Performs search on business partner classifications in order to find the best matches for the given text. " +
                "By specifying further request parameters the set of business partners to search in can be restricted. " +
                "If no text is given, the endpoint lists possible business partner classifications in the search set.",
        responses = [ApiResponse(responseCode = "200", description = "Best matches found, may be empty")]
    )
    @GetMapping("/classification")
    fun getClassificationSuggestion(
        @Parameter(description = "Show business partner classifications best matching this text") text: String?,
        @ParameterObject bpSearchRequest: BusinessPartnerPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        return searchService.getSuggestion(
            SuggestionType.CLASSIFICATION,
            text,
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest),
            pageRequest
        )
    }

    @Operation(
        summary = "Find best matches for given text in administrative areas",
        description = "Performs search on administrative area names in order to find the best matches for the given text. " +
                "By specifying further request parameters the set of business partners to search in can be restricted. " +
                "If no text is given, the endpoint lists possible administrative area names in the search set.",
        responses = [ApiResponse(responseCode = "200", description = "Best matches found, may be empty")]
    )
    @GetMapping("/address/administrative-area")
    fun getAdminAreaSuggestion(
        @Parameter(description = "Show administrative area names best matching this text") text: String?,
        @ParameterObject bpSearchRequest: BusinessPartnerPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        return searchService.getSuggestion(
            SuggestionType.ADMIN_AREA,
            text,
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest),
            pageRequest
        )
    }

    @Operation(
        summary = "Find best matches for given text in postcodes",
        description = "Performs search on postcode values in order to find the best matches for the given text. " +
                "By specifying further request parameters the set of business partners to search in can be restricted. " +
                "If no text is given, the endpoint lists possible postcode values in the search set.",
        responses = [ApiResponse(responseCode = "200", description = "Best matches found, may be empty")]
    )
    @GetMapping("/address/postcode")
    fun getPostcodeSuggestion(
        @Parameter(description = "Show postcodes best matching this text") text: String?,
        @ParameterObject bpSearchRequest: BusinessPartnerPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        return searchService.getSuggestion(
            SuggestionType.POSTCODE,
            text,
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest),
            pageRequest
        )
    }

    @Operation(
        summary = "Find best matches for given text in localities",
        description = "Performs search on locality denotations in order to find the best matches for the given text. " +
                "By specifying further request parameters the set of business partners to search in can be restricted. " +
                "If no text is given, the endpoint lists possible locality names in the search set.",
        responses = [ApiResponse(responseCode = "200", description = "Best matches found, may be empty")]
    )
    @GetMapping("/address/locality")
    fun getLocalitySuggestion(
        @Parameter(description = "Show locality names this text") text: String?,
        @ParameterObject bpSearchRequest: BusinessPartnerPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        return searchService.getSuggestion(
            SuggestionType.LOCALITY,
            text,
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest),
            pageRequest
        )
    }

    @Operation(
        summary = "Find best matches for given text in thoroughfares",
        description = "Performs search on thoroughfare denotations in order to find the best matches for the given text. " +
                "By specifying further request parameters the set of business partners to search in can be restricted. " +
                "If no text is given, the endpoint lists possible thoroughfare names in the search set.",
        responses = [ApiResponse(responseCode = "200", description = "Best matches found, may be empty")]
    )
    @GetMapping("/address/thoroughfare")
    fun getThoroughfareSuggestion(
        @Parameter(description = "Show thoroughfare names best matching this text") text: String?,
        @ParameterObject bpSearchRequest: BusinessPartnerPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        return searchService.getSuggestion(
            SuggestionType.THOROUGHFARE,
            text,
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest),
            pageRequest
        )
    }

    @Operation(
        summary = "Find best matches for given text in premises",
        description = "Performs search on premise denotations in order to find the best matches for the given text. " +
                "By specifying further request parameters the set of business partners to search in can be restricted. " +
                "If no text is given, the endpoint lists possible premise names in the search set.",
        responses = [ApiResponse(responseCode = "200", description = "Best matches found, may be empty")]
    )
    @GetMapping("/address/premise")
    fun getPremiseSuggestion(
        @Parameter(description = "Show premise names best matching this text") text: String?,
        @ParameterObject bpSearchRequest: BusinessPartnerPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        return searchService.getSuggestion(
            SuggestionType.PREMISE,
            text,
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest),
            pageRequest
        )
    }

    @Operation(
        summary = "Find best matches for given text in postal delivery points",
        description = "Performs search on postal delivery point denotations in order to find the best matches for the given text. " +
                "By specifying further request parameters the set of business partners to search in can be restricted. " +
                "If no text is given, the endpoint lists possible postal delivery point names in the search set.",
        responses = [ApiResponse(responseCode = "200", description = "Best matches found, may be empty")]
    )
    @GetMapping("/address/postal-delivery-point")
    fun getPostalDeliverPointSuggestion(
        @Parameter(description = "Show postal delivery point names best matching this text") text: String?,
        @ParameterObject bpSearchRequest: BusinessPartnerPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        return searchService.getSuggestion(
            SuggestionType.POSTAL_DELIVERY_POINT,
            text,
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest),
            pageRequest
        )
    }

}