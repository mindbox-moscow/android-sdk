package cloud.mindbox.mobile_sdk.models.operation.response

import com.google.gson.annotations.SerializedName

open class ProductListItemResponse constructor(
    @SerializedName("count") val count: Double? = null,
    @SerializedName("product") val product: ProductResponse? = null,
    @SerializedName("productGroup") val productGroup: ProductGroupResponse? = null,
    @SerializedName("pricePerItem") val pricePerItem: Double? = null,
    @SerializedName("priceOfLine") val priceOfLine: Double? = null
)