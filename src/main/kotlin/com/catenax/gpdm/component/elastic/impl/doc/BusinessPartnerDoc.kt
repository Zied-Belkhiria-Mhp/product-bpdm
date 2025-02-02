package com.catenax.gpdm.component.elastic.impl.doc

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

@Document(indexName = "business-partner", createIndex = true)
data class BusinessPartnerDoc(
    @Id
    val bpn: String,
    @Field(type = FieldType.Nested)
    val names: Collection<TextDoc>,
    @Field(type = FieldType.Nested)
    val legalForm: TextDoc?,
    @Field(type = FieldType.Nested)
    val status: TextDoc?,
    @Field(type = FieldType.Object)
    val addresses: Collection<AddressDoc>,
    @Field(type = FieldType.Nested)
    val classifications: Collection<TextDoc>,
)
