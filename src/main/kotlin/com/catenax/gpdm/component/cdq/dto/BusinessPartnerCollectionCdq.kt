package com.catenax.gpdm.component.cdq.dto

data class BusinessPartnerCollectionCdq(
    val limit: Int,
    val startAfter: String? = null,
    val nextStartAfter: String? = null,
    val total: Int,
    val values: Collection<BusinessPartnerCdq>
)
